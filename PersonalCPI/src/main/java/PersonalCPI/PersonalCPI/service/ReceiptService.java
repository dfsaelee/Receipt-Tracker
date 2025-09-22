package PersonalCPI.PersonalCPI.service;

import PersonalCPI.PersonalCPI.dto.MonthlySpendingDto;
import PersonalCPI.PersonalCPI.dto.ReceiptCreateDto;
import PersonalCPI.PersonalCPI.dto.ReceiptResponseDto;
import PersonalCPI.PersonalCPI.dto.SpendingSummaryDto;
import PersonalCPI.PersonalCPI.model.Category;
import PersonalCPI.PersonalCPI.model.Receipt;
import PersonalCPI.PersonalCPI.repository.CategoryRepository;
import PersonalCPI.PersonalCPI.repository.ReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReceiptService {
    private final ReceiptRepository receiptRepository;
    private final CategoryRepository categoryRepository;

    @Autowired
    public ReceiptService(ReceiptRepository receiptRepository, CategoryRepository categoryRepository) {
        this.receiptRepository = receiptRepository;
        this.categoryRepository = categoryRepository;
    }

    // Crud
    // Create a new Receipt for authenticated user
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
                createDto.getAmount()
        );

        Receipt savedReceipt = receiptRepository.save(receipt);
        return convertToResponseDto(savedReceipt);
    }

    @Transactional(readOnly = true)
    public List<ReceiptResponseDto> getUserReceipts(Long userId) {
        List<Receipt> receipts = receiptRepository.findByUserIdOrderByPurchaseDateDesc(userId);
        return receipts.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReceiptResponseDto> getUserReceiptsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Receipt> receipts = receiptRepository.findByUserIdAndPurchaseDateBetween(userId, startDate, endDate);
        return receipts.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReceiptResponseDto> getUserReceiptsByCategory(Long userId, Long categoryId) {
        List<Receipt> receipts = receiptRepository.findByUserIdAndCategoryId(userId, categoryId);
        return receipts.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReceiptResponseDto getReceiptById(Long userId, Long receiptId) {
        Optional<Receipt> receipt = receiptRepository.findById(receiptId);

        if (receipt.isEmpty()) {
            throw new IllegalArgumentException("Receipt not found");
        }

        if (!receipt.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Receipt does not belong to user");
        }

        return convertToResponseDto(receipt.get());
    }

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

        Receipt savedReceipt = receiptRepository.save(receipt);
        return convertToResponseDto(savedReceipt);
    }

    public void deleteReceipt(Long userId, Long receiptId) {
        Optional<Receipt> receipt = receiptRepository.findById(receiptId);

        if (receipt.isEmpty()) {
            throw new IllegalArgumentException("Receipt not found");
        }

        if (!receipt.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Receipt does not belong to user");
        }

        receiptRepository.deleteById(receiptId);
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
     * Convert Receipt entity to ReceiptResponseDto
     */
    private ReceiptResponseDto convertToResponseDto(Receipt receipt) {
        String categoryName = null;
        if (receipt.getCategoryId() != null) {
            Optional<Category> category = categoryRepository.findById(receipt.getCategoryId());
            categoryName = category.map(Category::getName).orElse("Unknown");
        }

        return new ReceiptResponseDto(
                receipt.getReceiptId(),
                receipt.getStoreName(),
                receipt.getPurchaseDate(),
                receipt.getCategoryId(),
                categoryName,
                receipt.getAmount(),
                receipt.getCreatedAt()
        );
    }
}