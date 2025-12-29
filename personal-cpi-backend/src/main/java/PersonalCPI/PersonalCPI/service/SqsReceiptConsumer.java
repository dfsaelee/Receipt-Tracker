package PersonalCPI.PersonalCPI.service;

import PersonalCPI.PersonalCPI.dto.ReceiptMessage;
import PersonalCPI.PersonalCPI.model.Receipt;
import PersonalCPI.PersonalCPI.model.ReceiptItem;
import PersonalCPI.PersonalCPI.repository.ReceiptItemRepository;
import PersonalCPI.PersonalCPI.repository.ReceiptRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

/**
 * Service that polls SQS queue for receipt messages and saves them to database.
 * Runs on a scheduled interval to process messages from Lambda.
 */
@Service
public class SqsReceiptConsumer {
    private static final Logger logger = LoggerFactory.getLogger(SqsReceiptConsumer.class);
    
    private final SqsClient sqsClient;
    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${aws.sqs.receipt-queue-url}")
    private String queueUrl;
    
    public SqsReceiptConsumer(SqsClient sqsClient, 
                             ReceiptRepository receiptRepository,
                             ReceiptItemRepository receiptItemRepository) {
        this.sqsClient = sqsClient;
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.objectMapper = new ObjectMapper();
        // Configure to use BigDecimal for floating point numbers to preserve precision
        this.objectMapper.enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }
    
    /**
     * Polls SQS queue every 10 seconds for new receipt messages.
     * Processes up to 10 messages per poll.
     */
    @Scheduled(fixedDelay = 10000)  // Poll every 10 seconds
    public void processQueue() {
        try {
            // Receive messages from queue
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(5)  // Long polling
                .build();
            
            ReceiveMessageResponse response = sqsClient.receiveMessage(receiveRequest);
            List<Message> messages = response.messages();
            
            if (messages.isEmpty()) {
                return;  // No messages to process
            }
            
            logger.info("Received {} messages from SQS queue", messages.size());
            
            for (Message message : messages) {
                try {
                    processMessage(message);
                    
                    // Delete message from queue after successful processing
                    DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build();
                    sqsClient.deleteMessage(deleteRequest);
                    
                    logger.info("Successfully processed and deleted message: {}", message.messageId());
                    
                } catch (Exception e) {
                    logger.error("Failed to process message {}: {}", message.messageId(), e.getMessage(), e);
                    // Message will be retried (up to maxReceiveCount=3) then moved to DLQ
                }
            }
            
        } catch (Exception e) {
            logger.error("Error polling SQS queue: {}", e.getMessage(), e);
        }
    }
    
    private void processMessage(Message message) throws Exception {
        // Deserialize message body
        ReceiptMessage receiptMessage = objectMapper.readValue(message.body(), ReceiptMessage.class);
        
        logger.info("Processing receipt: Store={}, Amount={}", 
            receiptMessage.getStoreName(), receiptMessage.getAmount());
        
        // Create Receipt entity
        Receipt receipt = new Receipt();
        receipt.setUserId(receiptMessage.getUserId());
        receipt.setStoreName(receiptMessage.getStoreName());
        receipt.setAmount(receiptMessage.getAmount());
        receipt.setPurchaseDate(receiptMessage.getPurchaseDate() != null ? 
            new java.sql.Date(receiptMessage.getPurchaseDate().getTime()).toLocalDate() : null);
        receipt.setImageKey(receiptMessage.getImageKey());
        receipt.setCategoryId(8L);  // Default category, can be updated later
        
        // Save receipt
        Receipt savedReceipt = receiptRepository.save(receipt);
        logger.info("Saved receipt with ID: {}", savedReceipt.getReceiptId());
        
        // Save receipt items
        if (receiptMessage.getItems() != null && !receiptMessage.getItems().isEmpty()) {
            for (ReceiptMessage.ReceiptItemMessage itemMessage : receiptMessage.getItems()) {
                ReceiptItem item = new ReceiptItem();
                item.setReceiptId(savedReceipt.getReceiptId());
                item.setItemName(itemMessage.getItemName());
                item.setQuantity(itemMessage.getQuantity());
                item.setUnitPrice(itemMessage.getPrice());  // price from message goes to unitPrice
                
                receiptItemRepository.save(item);
            }
            logger.info("Saved {} receipt items", receiptMessage.getItems().size());
        }
    }
}
