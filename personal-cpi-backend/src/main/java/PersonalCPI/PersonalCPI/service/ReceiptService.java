package PersonalCPI.PersonalCPI.service;

import PersonalCPI.PersonalCPI.dto.MonthlySpendingDto;
import PersonalCPI.PersonalCPI.dto.ReceiptCreateDto;
import PersonalCPI.PersonalCPI.dto.ReceiptItemDto;
import PersonalCPI.PersonalCPI.dto.ReceiptResponseDto;
import PersonalCPI.PersonalCPI.dto.SpendingSummaryDto;
import PersonalCPI.PersonalCPI.model.Category;
import PersonalCPI.PersonalCPI.model.Receipt;
import PersonalCPI.PersonalCPI.model.ReceiptItem;
import PersonalCPI.PersonalCPI.repository.CategoryRepository;
import PersonalCPI.PersonalCPI.repository.ReceiptItemRepository;
import PersonalCPI.PersonalCPI.repository.ReceiptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReceiptService {
    private static final Logger logger = LoggerFactory.getLogger(ReceiptService.class);
    
    private final ReceiptRepository receiptRepository;
    private final CategoryRepository categoryRepository;
    private final S3Service s3Service;
    private final ReceiptItemRepository receiptItemRepository;
    private final SecurityEventLogger securityLogger;

    @Autowired
    public ReceiptService(ReceiptRepository receiptRepository, CategoryRepository categoryRepository, 
                         S3Service s3Service, ReceiptItemRepository receiptItemRepository,
                         SecurityEventLogger securityLogger) {
        this.receiptRepository = receiptRepository;
        this.categoryRepository = categoryRepository;
        this.s3Service = s3Service;
        this.receiptItemRepository = receiptItemRepository;
        this.securityLogger = securityLogger;
    }

    // Crud
    // Create a new Receipt for authenticated user
    @Transactional
    public ReceiptResponseDto createReceipt(Long userId, ReceiptCreateDto createDto) {
        if (createDto.getCategoryId() != null) {
            Optional<Category> category = categoryRepository.findById(createDto.getCategoryId());
            if (category.isEmpty()) {
                throw new IllegalArgumentException("Category not found with ID: " + createDto);
            }
        }
        Receipt receipt = new Receipt(
                userId,
                createDto.getStoreName(),
                createDto.getPurchaseDate(),
                createDto.getCategoryId(),
                createDto.getAmount(),
                createDto.getImageKey()
        );

        Receipt savedReceipt = receiptRepository.save(receipt);
        
        // Create receipt items if provided
        if (createDto.getItems() != null && !createDto.getItems().isEmpty()) {
            List<ReceiptItem> items = createDto.getItems().stream()
                    .map(itemDto -> {
                        ReceiptItem item = new ReceiptItem();
                        item.setReceiptId(savedReceipt.getReceiptId());
                        item.setItemName(itemDto.getItemName());
                        item.setQuantity(itemDto.getQuantity());
                        item.setUnitPrice(itemDto.getUnitPrice());
                        return item;
                    })
                    .collect(Collectors.toList());
            receiptItemRepository.saveAll(items);
        }
        
        return convertToResponseDto(savedReceipt);
    }

    /**
     * Get all receipts for a user.
     * Loads receipts with associated items in a single query.
     */
    @Transactional(readOnly = true)
    public List<ReceiptResponseDto> getUserReceipts(Long userId) {
        List<Receipt> receipts = receiptRepository.findByUserIdWithItems(userId);
        return receipts.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Get paginated receipts for a user.
     * Uses two-query approach for efficient pagination with related entities.
     */
    @Transactional(readOnly = true)
    public Page<ReceiptResponseDto> getUserReceipts(Long userId, Pageable pageable) {
        // Get paginated receipts
        Page<Receipt> receiptPage = receiptRepository.findByUserIdOrderByPurchaseDateDesc(userId, pageable);
        
        // If we have receipts, fetch them with items using JOIN FETCH
        if (!receiptPage.isEmpty()) {
            List<Long> receiptIds = receiptPage.getContent().stream()
                    .map(Receipt::getReceiptId)
                    .collect(Collectors.toList());
            
            // Fetch receipts with items
            List<Receipt> receiptsWithItems = receiptRepository.findByReceiptIdInWithItems(receiptIds);
            
            // Convert to DTOs
            Map<Long, Receipt> receiptMap = receiptsWithItems.stream()
                    .collect(Collectors.toMap(Receipt::getReceiptId, r -> r));
            
            List<ReceiptResponseDto> dtos = receiptPage.getContent().stream()
                    .map(r -> convertToResponseDto(receiptMap.getOrDefault(r.getReceiptId(), r)))
                    .collect(Collectors.toList());
            
            return new PageImpl<>(dtos, pageable, receiptPage.getTotalElements());
        }
        
        return receiptPage.map(this::convertToResponseDto);
    }

    /**
     * Get receipts by date range (non-paginated)
     */
    @Transactional(readOnly = true)
    public List<ReceiptResponseDto> getUserReceiptsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Receipt> receipts = receiptRepository.findByUserIdAndPurchaseDateBetween(userId, startDate, endDate);
        return receipts.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Get receipts by date range (PAGINATED)
     */
    @Transactional(readOnly = true)
    public Page<ReceiptResponseDto> getUserReceiptsByDateRange(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // Validate date range
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        
        // Prevent querying too large date ranges (> 2 years)
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 730) {
            throw new IllegalArgumentException("Date range too large (maximum 2 years allowed)");
        }
        
        Page<Receipt> receipts = receiptRepository.findByUserIdAndPurchaseDateBetween(userId, startDate, endDate, pageable);
        return receipts.map(this::convertToResponseDto);
    }

    /**
     * Get receipts by category (non-paginated)
     */
    @Transactional(readOnly = true)
    public List<ReceiptResponseDto> getUserReceiptsByCategory(Long userId, Long categoryId) {
        List<Receipt> receipts = receiptRepository.findByUserIdAndCategoryId(userId, categoryId);
        return receipts.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Get receipts by category (PAGINATED)
     */
    @Transactional(readOnly = true)
    public Page<ReceiptResponseDto> getUserReceiptsByCategory(Long userId, Long categoryId, Pageable pageable) {
        Page<Receipt> receipts = receiptRepository.findByUserIdAndCategoryId(userId, categoryId, pageable);
        return receipts.map(this::convertToResponseDto);
    }

    @Transactional(readOnly = true)
    public ReceiptResponseDto getReceiptById(Long userId, Long receiptId) {
        Optional<Receipt> receipt = receiptRepository.findById(receiptId);

        if (receipt.isEmpty()) {
            throw new IllegalArgumentException("Receipt not found");
        }

        if (!receipt.get().getUserId().equals(userId)) {
            securityLogger.logAccessDenied(userId, receiptId, "Receipt");
            throw new IllegalArgumentException("Receipt does not belong to user");
        }

        return convertToResponseDto(receipt.get());
    }

    @Transactional
    public ReceiptResponseDto updateReceipt(Long userId, Long receiptId, ReceiptCreateDto updateDto) {
        Optional<Receipt> existingReceipt = receiptRepository.findById(receiptId);

        if (existingReceipt.isEmpty()) {
            throw new IllegalArgumentException("Receipt not found");
        }

        Receipt receipt = existingReceipt.get();
        if (!receipt.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Receipt does not belong to user");
        }

        // Validate category exists if provided
        if (updateDto.getCategoryId() != null) {
            Optional<Category> category = categoryRepository.findById(updateDto.getCategoryId());
            if (category.isEmpty()) {
                throw new IllegalArgumentException("Category not found with ID: " + updateDto.getCategoryId());
            }
        }

        // Update fields
        receipt.setStoreName(updateDto.getStoreName());
        receipt.setPurchaseDate(updateDto.getPurchaseDate());
        receipt.setCategoryId(updateDto.getCategoryId());
        receipt.setAmount(updateDto.getAmount());
        receipt.setImageKey(updateDto.getImageKey());

        Receipt savedReceipt = receiptRepository.save(receipt);
        
        // Update receipt items if provided
        if (updateDto.getItems() != null) {
            // Delete existing items
            receiptItemRepository.deleteByReceiptId(receiptId);
            
            // Create new items
            if (!updateDto.getItems().isEmpty()) {
                List<ReceiptItem> items = updateDto.getItems().stream()
                        .map(itemDto -> {
                            ReceiptItem item = new ReceiptItem();
                            item.setReceiptId(receiptId);
                            item.setItemName(itemDto.getItemName());
                            item.setQuantity(itemDto.getQuantity());
                            item.setUnitPrice(itemDto.getUnitPrice());
                            return item;
                        })
                        .collect(Collectors.toList());
                receiptItemRepository.saveAll(items);
            }
        }
        
        return convertToResponseDto(savedReceipt);
    }

    @Transactional
    public void deleteReceipt(Long userId, Long receiptId) {
        Optional<Receipt> receipt = receiptRepository.findById(receiptId);

        if (receipt.isEmpty()) {
            throw new IllegalArgumentException("Receipt not found");
        }

        if (!receipt.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Receipt does not belong to user");
        }

        // Delete associated S3 image if it exists
        String imageKey = receipt.get().getImageKey();
        if (imageKey != null && !imageKey.isEmpty()) {
            try {
                s3Service.deleteObject(imageKey);
                logger.info("Deleted S3 object: {}", imageKey);
            } catch (Exception e) {
                // Log error but don't fail the deletion
                logger.error("Failed to delete S3 object: {} - {}", imageKey, e.getMessage());
            }
        }

        // Delete associated receipt items
        receiptItemRepository.deleteByReceiptId(receiptId);

        receiptRepository.deleteById(receiptId);
    }

    // Update receipt category
    @Transactional
    public Receipt updateReceiptCategory(Long userId, Long receiptId, Long categoryId) {
        // Validate category ID (1-8)
        if (categoryId < 1 || categoryId > 8) {
            throw new IllegalArgumentException("Invalid category ID. Must be between 1 and 8.");
        }
        
        // Find receipt
        Optional<Receipt> optionalReceipt = receiptRepository.findById(receiptId);
        if (optionalReceipt.isEmpty()) {
            throw new IllegalArgumentException("Receipt not found");
        }
        
        Receipt receipt = optionalReceipt.get();
        
        // Verify ownership
        if (!receipt.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Receipt does not belong to user");
        }
        
        // Update category
        receipt.setCategoryId(categoryId);
        
        return receiptRepository.save(receipt);
    }

    @Transactional(readOnly = true)
    public List<SpendingSummaryDto> getCurrentMonthSpendingByCategory(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();

        return getSpendingByCategoryForPeriod(userId, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<SpendingSummaryDto> getSpendingByCategoryForPeriod(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = receiptRepository.getSpendingSummaryByCategory(userId, startDate, endDate);

        // Get all categories for lookup
        Map<Long, String> categoryMap = categoryRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Category::getCategoryId, Category::getName));

        return results.stream()
                .map(result -> {
                    Long categoryId = (Long) result[0];
                    BigDecimal totalAmount = (BigDecimal) result[1];
                    String categoryName = categoryMap.getOrDefault(categoryId, "Unknown");

                    return new SpendingSummaryDto(categoryId, categoryName, totalAmount, 0);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get monthly spending summary for a user
     */
    @Transactional(readOnly = true)
    public List<MonthlySpendingDto> getMonthlySpendingSummary(Long userId) {
        List<Object[]> results = receiptRepository.getMonthlySpendingSummary(userId);

        return results.stream()
                .map(result -> new MonthlySpendingDto(
                        (Integer) result[0], // year
                        (Integer) result[1], // month
                        (BigDecimal) result[2] // total
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get total spending for a user
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalSpending(Long userId) {
        return receiptRepository.getTotalSpendingByUser(userId);
    }

    /**
     * Get recent receipts for a user
     */
    @Transactional(readOnly = true)
    public List<ReceiptResponseDto> getRecentReceipts(Long userId) {
        List<Receipt> receipts = receiptRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
        return receipts.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert Receipt entity to ReceiptResponseDto.
     */
    private ReceiptResponseDto convertToResponseDto(Receipt receipt) {
        String categoryName = null;
        if (receipt.getCategoryId() != null) {
            Optional<Category> category = categoryRepository.findById(receipt.getCategoryId());
            categoryName = category.map(Category::getName).orElse("Unknown");
        }

        // Generate presigned URL if imageKey exists
        String imageUrl = null;
        if (receipt.getImageKey() != null && !receipt.getImageKey().isEmpty()) {
            try {
                imageUrl = s3Service.createPresignedGetUrl(receipt.getImageKey());
            } catch (Exception e) {
                // Log error but don't fail the request
                logger.error("Failed to generate presigned URL for key: {}", receipt.getImageKey());
            }
        }

        // Load receipt items
        List<ReceiptItemDto> items;
        
        // Use items if already loaded, otherwise fetch from repository
        if (receipt.getItems() != null && !receipt.getItems().isEmpty()) {
            try {
                items = receipt.getItems().stream()
                        .map(item -> new ReceiptItemDto(
                                item.getReceiptItemId(),
                                item.getReceiptId(),
                                item.getItemName(),
                                item.getQuantity(),
                                item.getUnitPrice()
                        ))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                // Fallback to repository query
                items = receiptItemRepository.findByReceiptId(receipt.getReceiptId())
                        .stream()
                        .map(item -> new ReceiptItemDto(
                                item.getReceiptItemId(),
                                item.getReceiptId(),
                                item.getItemName(),
                                item.getQuantity(),
                                item.getUnitPrice()
                        ))
                        .collect(Collectors.toList());
            }
        } else {
            // Items not loaded, fetch from repository
            items = receiptItemRepository.findByReceiptId(receipt.getReceiptId())
                    .stream()
                    .map(item -> new ReceiptItemDto(
                            item.getReceiptItemId(),
                            item.getReceiptId(),
                            item.getItemName(),
                            item.getQuantity(),
                            item.getUnitPrice()
                    ))
                    .collect(Collectors.toList());
        }

        return new ReceiptResponseDto(
                receipt.getReceiptId(),
                receipt.getStoreName(),
                receipt.getPurchaseDate(),
                receipt.getCategoryId(),
                categoryName,
                receipt.getAmount(),
                receipt.getCreatedAt(),
                receipt.getImageKey(),
                imageUrl,
                items
        );
    }
}