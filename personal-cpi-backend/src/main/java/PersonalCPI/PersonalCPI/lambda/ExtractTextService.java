package PersonalCPI.PersonalCPI.lambda;

import PersonalCPI.PersonalCPI.dto.ExtractedReceipt;
import PersonalCPI.PersonalCPI.dto.ExtractedReceiptItem;
import PersonalCPI.PersonalCPI.dto.ReceiptWithItems;
import PersonalCPI.PersonalCPI.model.Receipt;
import PersonalCPI.PersonalCPI.model.ReceiptItem;
import PersonalCPI.PersonalCPI.util.ReceiptDataParser;
import PersonalCPI.PersonalCPI.util.S3KeyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for extracting receipt data from images using AWS Textract
 * and persisting to database.
 */

public class ExtractTextService {
    private static final Logger logger = LoggerFactory.getLogger(ExtractTextService.class);
    
    private final TextractClient textractClient;
    private final double highConfidence;
    private final double mediumConfidence;
    private static final Long DEFAULT_CATEGORY_ID = 8L; // "Other Goods and Services" as default

    // Constructor for ExtractTextService with dependency injection.
    public ExtractTextService(TextractClient textractClient) {
        this.textractClient = textractClient;
        this.highConfidence = 90.0;
        this.mediumConfidence = 75.0;
    }

    /**
     * Extracts receipt data from S3 image, processes it, and saves to database.
     */
    public ReceiptWithItems extractReceiptData(String bucketName, String objectKey) {
        logger.info("Starting receipt extraction for s3://{}/{}", bucketName, objectKey);

        try {
            // Build Textract document reference
            Document document = Document.builder()
                .s3Object(S3Object.builder()
                    .bucket(bucketName)
                    .name(objectKey)
                    .build())
                .build();

            // Create Textract request
            AnalyzeExpenseRequest request = AnalyzeExpenseRequest.builder()
                .document(document)
                .build();

            // Perform OCR
            AnalyzeExpenseResponse response = textractClient.analyzeExpense(request);
            
            if (response.expenseDocuments() == null || response.expenseDocuments().isEmpty()) {
                throw new RuntimeException("No expense documents found in Textract response");
            }

            // Extract fields from the first expense document
            ExtractedReceipt extractedReceipt = null;
            for (ExpenseDocument expenseDocument : response.expenseDocuments()) {
                extractedReceipt = extractFields(expenseDocument);
                break;
            }

            if (extractedReceipt == null) {
                throw new RuntimeException("Failed to extract receipt fields");
            }

            // Map to domain models
            ReceiptWithItems receiptWithItems = mapToReceiptWithItems(extractedReceipt, objectKey);
            
            logger.info("Extracted receipt - Store: {}, Amount: {}, Items: {}",
                receiptWithItems.getReceipt().getStoreName(),
                receiptWithItems.getReceipt().getAmount(),
                receiptWithItems.getItemCount());

            return receiptWithItems;

        } catch (TextractException e) {
            logger.error("Textract service error for s3://{}/{}: {}", bucketName, objectKey, e.getMessage());
            throw new RuntimeException("Textract service error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error during receipt extraction for s3://{}/{}: {}", bucketName, objectKey, e.getMessage());
            throw new RuntimeException("Receipt extraction failed: " + e.getMessage(), e);
        }
    }

    /**
     * Maps ExtractedReceipt DTO to Receipt and ReceiptItem entities.
     */ 
    private ReceiptWithItems mapToReceiptWithItems(ExtractedReceipt extractedReceipt, String objectKey) {
        Receipt receipt = new Receipt();

        // Set vendor/store name
        receipt.setStoreName(extractedReceipt.getVendorName() != null ? extractedReceipt.getVendorName() : "");

        // Parse and set purchase date
        LocalDate purchaseDate = ReceiptDataParser.parseDate(extractedReceipt.getReceiptDate())
            .orElse(LocalDate.now());
        receipt.setPurchaseDate(purchaseDate);

        // Parse and set amount
        BigDecimal amount = ReceiptDataParser.parseAmount(extractedReceipt.getAmountDue())
            .orElse(BigDecimal.ZERO);
        receipt.setAmount(amount);

        // Extract userId from S3 key
        Long userId = S3KeyParser.extractUserId(objectKey);
        receipt.setUserId(userId);

        // Set metadata
        receipt.setCreatedAt(LocalDateTime.now());
        receipt.setImageKey(objectKey);
        receipt.setCategoryId(DEFAULT_CATEGORY_ID);

        // Map receipt items
        List<ReceiptItem> receiptItems = new ArrayList<>();
        if (extractedReceipt.getItems() != null && !extractedReceipt.getItems().isEmpty()) {
            for (ExtractedReceiptItem extractedItem : extractedReceipt.getItems()) {
                ReceiptItem item = mapToReceiptItem(extractedItem);
                if (item != null) {
                    receiptItems.add(item);
                }
            }
        }

        logger.debug("Mapped receipt with {} items. RequiresManualReview: {}", 
            receiptItems.size(), extractedReceipt.isRequiresManualReview());

        return new ReceiptWithItems(receipt, receiptItems);
    }

    /**
     * Maps an ExtractedReceiptItem to a ReceiptItem entity.
     */
    private ReceiptItem mapToReceiptItem(ExtractedReceiptItem extractedItem) {
        if (extractedItem.getName() == null || extractedItem.getName().trim().isEmpty()) {
            logger.warn("Skipping receipt item with no name");
            return null;
        }

        ReceiptItem item = new ReceiptItem();
        item.setItemName(extractedItem.getName());

        // Parse quantity
        Integer quantity = ReceiptDataParser.parseQuantity(extractedItem.getQuantity())
            .orElse(1);
        item.setQuantity(quantity);

        // Parse price
        BigDecimal price = ReceiptDataParser.parseAmount(extractedItem.getPrice())
            .orElse(BigDecimal.ZERO);
        logger.info("Parsed price for '{}': raw='{}' -> parsed={}", 
            extractedItem.getName(), extractedItem.getPrice(), price);
        item.setUnitPrice(price);

        return item;
    }

    /**
     * Extracts primary information from receipt using Textract expense document.
     */
    private ExtractedReceipt extractFields(ExpenseDocument expenseDocument) {
        ExtractedReceipt extractedReceipt = new ExtractedReceipt();
        Map<String, Double> confidenceMap = new HashMap<>();
        boolean requiresReview = false;

        // Extract summary fields (receipt-level data)
        for (ExpenseField expenseField : expenseDocument.summaryFields()) {
            String type = expenseField.type().text();
            String value = expenseField.valueDetection() != null 
                ? expenseField.valueDetection().text() 
                : null;

            Double confidence = expenseField.valueDetection() != null 
                ? (double) expenseField.valueDetection().confidence() 
                : 0.0;

            confidenceMap.put(type, confidence);
            
            // Log all fields for debugging (DEBUG level for production)
            logger.debug("Textract field: {} = '{}' (confidence: {}%)", type, value, String.format("%.2f", confidence));
            
            // Map fields based on type
            if (isConfidenceAcceptable(confidence)) {
                switch (type) {
                    case "VENDOR_NAME" -> extractedReceipt.setVendorName(value);
                    case "AMOUNT_DUE", "TOTAL", "AMOUNT_PAID" -> {
                        // Try multiple amount field names
                        if (extractedReceipt.getAmountDue() == null || extractedReceipt.getAmountDue().isEmpty()) {
                            extractedReceipt.setAmountDue(value);
                            logger.debug("Set amount from field '{}': {}", type, value);
                        }
                    }
                    case "INVOICE_RECEIPT_DATE" -> extractedReceipt.setReceiptDate(value);
                    case "PAYMENT_TERMS" -> extractedReceipt.setPaymentTerms(value);
                }
            } else {
                requiresReview = true;
                logger.warn("Low confidence for field: {} (confidence: {}%)", type, String.format("%.2f", confidence));
            }
        }

        extractedReceipt.setConfidenceByField(confidenceMap);
        extractedReceipt.setRequiresManualReview(requiresReview);

        // Extract line items
        List<ExtractedReceiptItem> items = extractLineItems(expenseDocument);
        extractedReceipt.setItems(items);

        logger.debug("Extracted {} summary fields and {} line items", confidenceMap.size(), items.size());

        return extractedReceipt;
    }

    /**
     * Extracts line item information from the receipt.
     */
    private List<ExtractedReceiptItem> extractLineItems(ExpenseDocument expenseDocument) {
        List<ExtractedReceiptItem> items = new ArrayList<>();

        for (LineItemGroup lineItemGroup : expenseDocument.lineItemGroups()) {
            for (LineItemFields lineItem : lineItemGroup.lineItems()) {
                ExtractedReceiptItem item = new ExtractedReceiptItem();
                double totalConfidence = 0.0;
                int fieldCount = 0;

                for (ExpenseField field : lineItem.lineItemExpenseFields()) {
                    String type = field.type().text();
                    String value = field.valueDetection() != null 
                        ? field.valueDetection().text() 
                        : null;

                    Double confidence = field.valueDetection() != null 
                        ? (double) field.valueDetection().confidence() 
                        : 0.0;

                    totalConfidence += confidence;
                    fieldCount++;

                    if (isConfidenceAcceptable(confidence)) {
                        switch (type) {
                            case "ITEM" -> item.setName(value);
                            case "QUANTITY" -> item.setQuantity(value);
                            case "PRICE" -> {
                                logger.info("Raw Textract PRICE value: '{}' (confidence: {}%)", 
                                    value, String.format("%.2f", confidence));
                                item.setPrice(value);
                            }
                        }
                    }
                }

                // Calculate average confidence for the item
                double avgConfidence = fieldCount > 0 ? totalConfidence / fieldCount : 0.0;
                item.setConfidence(String.format("%.2f", avgConfidence));

                // Only add items with acceptable confidence
                if (avgConfidence >= mediumConfidence && item.getName() != null) {
                    items.add(item);
                } else {
                    logger.debug("Skipping line item due to low confidence or missing name: {}%", 
                        String.format("%.2f", avgConfidence));
                }
            }
        }

        return items;
    }

    private boolean isConfidenceAcceptable(Double confidence) {
        return confidence != null && confidence >= mediumConfidence;
    }

    private boolean isHighConfidence(Double confidence) {
        return confidence != null && confidence >= highConfidence;
    }

    private String getConfidenceLevel(Double confidence) {
        if (confidence == null) return "UNKNOWN";
        if (confidence >= highConfidence) return "HIGH";
        if (confidence >= mediumConfidence) return "MEDIUM";
        return "LOW";
    }
}
