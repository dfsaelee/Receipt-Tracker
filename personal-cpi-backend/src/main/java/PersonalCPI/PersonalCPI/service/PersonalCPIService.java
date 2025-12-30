package PersonalCPI.PersonalCPI.service;

import PersonalCPI.PersonalCPI.model.PersonalCpiMonthly;
import PersonalCPI.PersonalCPI.repository.CategoryRepository;
import PersonalCPI.PersonalCPI.repository.PersonalCpiMonthlyRepository;
import PersonalCPI.PersonalCPI.repository.ReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Service for calculating and managing personal CPI data.
 * Calculates month-over-month and year-over-year inflation based on user spending.
 */
@Service
public class PersonalCPIService {
    
    private final ReceiptRepository receiptRepository;
    private final PersonalCpiMonthlyRepository personalCpiMonthlyRepository;
    private final CategoryRepository categoryRepository;
    
    @Autowired
    public PersonalCPIService(
            ReceiptRepository receiptRepository,
            PersonalCpiMonthlyRepository personalCpiMonthlyRepository,
            CategoryRepository categoryRepository) {
        this.receiptRepository = receiptRepository;
        this.personalCpiMonthlyRepository = personalCpiMonthlyRepository;
        this.categoryRepository = categoryRepository;
    }
    
    /**
     * Calculate and store monthly personal CPI for a user
     * @param userId User ID
     * @param year Year
     * @param month Month (1-12)
     */
    @Transactional
    public void calculateMonthlyPersonalCPI(Long userId, Integer year, Integer month) {
        // Calculate for each category
        List<Long> categoryIds = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
        BigDecimal totalSpending = BigDecimal.ZERO;
        
        for (Long categoryId : categoryIds) {
            BigDecimal categorySpending = calculateCategorySpending(userId, year, month, categoryId);
            totalSpending = totalSpending.add(categorySpending);
        }
        
        // If no spending this month, delete any existing data and return
        if (totalSpending.compareTo(BigDecimal.ZERO) == 0) {
            personalCpiMonthlyRepository.deleteByUserIdAndYearAndMonth(userId, year, month);
            return;
        }
        
        // Delete existing calculations for this month before recalculating
        personalCpiMonthlyRepository.deleteByUserIdAndYearAndMonth(userId, year, month);
        
        // Save category-level data
        for (Long categoryId : categoryIds) {
            BigDecimal categorySpending = calculateCategorySpending(userId, year, month, categoryId);
            
            if (categorySpending.compareTo(BigDecimal.ZERO) > 0) {
                PersonalCpiMonthly categoryData = new PersonalCpiMonthly();
                categoryData.setUserId(userId);
                categoryData.setYear(year);
                categoryData.setMonth(month);
                categoryData.setCategoryId(categoryId);
                categoryData.setTotalSpending(categorySpending);
                
                // Calculate MoM change
                BigDecimal momChange = calculateMomChange(userId, year, month, categoryId, categorySpending);
                categoryData.setMomChangePercent(momChange);
                
                // Calculate YoY change
                BigDecimal yoyChange = calculateYoyChange(userId, year, month, categoryId, categorySpending);
                categoryData.setYoyChangePercent(yoyChange);
                
                personalCpiMonthlyRepository.save(categoryData);
            }
        }
        
        // Save overall (all categories combined)
        PersonalCpiMonthly overallData = new PersonalCpiMonthly();
        overallData.setUserId(userId);
        overallData.setYear(year);
        overallData.setMonth(month);
        overallData.setCategoryId(null);  // NULL for overall
        overallData.setTotalSpending(totalSpending);
        
        // Calculate MoM change for overall
        BigDecimal momChange = calculateMomChange(userId, year, month, null, totalSpending);
        overallData.setMomChangePercent(momChange);
        
        // Calculate YoY change for overall
        BigDecimal yoyChange = calculateYoyChange(userId, year, month, null, totalSpending);
        overallData.setYoyChangePercent(yoyChange);
        
        personalCpiMonthlyRepository.save(overallData);
    }
    
    /**
     * Calculate total spending for a category in a specific month
     */
    private BigDecimal calculateCategorySpending(Long userId, Integer year, Integer month, Long categoryId) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        List<Object[]> results = receiptRepository.getSpendingSummaryByCategory(userId, startDate, endDate);
        
        for (Object[] result : results) {
            Long resultCategoryId = (Long) result[0];
            if (resultCategoryId.equals(categoryId)) {
                return (BigDecimal) result[1];
            }
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate Month-over-Month percentage change
     */
    private BigDecimal calculateMomChange(Long userId, Integer year, Integer month, 
                                          Long categoryId, BigDecimal currentSpending) {
        // Get previous month
        YearMonth currentYearMonth = YearMonth.of(year, month);
        YearMonth previousYearMonth = currentYearMonth.minusMonths(1);
        
        Optional<PersonalCpiMonthly> previousData;
        if (categoryId == null) {
            previousData = personalCpiMonthlyRepository.findByUserIdAndYearAndMonthAndCategoryIdIsNull(
                    userId, previousYearMonth.getYear(), previousYearMonth.getMonthValue());
        } else {
            previousData = personalCpiMonthlyRepository.findByUserIdAndYearAndMonthAndCategoryId(
                    userId, previousYearMonth.getYear(), previousYearMonth.getMonthValue(), categoryId);
        }
        
        if (previousData.isPresent() && previousData.get().getTotalSpending().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal previousSpending = previousData.get().getTotalSpending();
            BigDecimal change = currentSpending.subtract(previousSpending);
            return change.divide(previousSpending, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        
        return null;  // No previous data
    }
    
    /**
     * Calculate Year-over-Year percentage change
     */
    private BigDecimal calculateYoyChange(Long userId, Integer year, Integer month, 
                                          Long categoryId, BigDecimal currentSpending) {
        Integer previousYear = year - 1;
        
        Optional<PersonalCpiMonthly> previousYearData;
        if (categoryId == null) {
            previousYearData = personalCpiMonthlyRepository.findByUserIdAndYearAndMonthAndCategoryIdIsNull(
                    userId, previousYear, month);
        } else {
            previousYearData = personalCpiMonthlyRepository.findByUserIdAndYearAndMonthAndCategoryId(
                    userId, previousYear, month, categoryId);
        }
        
        if (previousYearData.isPresent() && previousYearData.get().getTotalSpending().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal previousSpending = previousYearData.get().getTotalSpending();
            BigDecimal change = currentSpending.subtract(previousSpending);
            return change.divide(previousSpending, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        
        return null;  // No previous year data
    }
    
    /**
     * Get personal CPI summary for a specific month
     */
    @Transactional(readOnly = true)
    public List<PersonalCpiMonthly> getPersonalCPISummary(Long userId, Integer year, Integer month) {
        return personalCpiMonthlyRepository.findByUserIdAndYearAndMonth(userId, year, month);
    }
    
    /**
     * Get overall personal CPI for a specific month
     */
    @Transactional(readOnly = true)
    public Optional<PersonalCpiMonthly> getOverallPersonalCPI(Long userId, Integer year, Integer month) {
        return personalCpiMonthlyRepository.findByUserIdAndYearAndMonthAndCategoryIdIsNull(userId, year, month);
    }
    
    /**
     * Recalculate all personal CPI data for a user
     * Limited to last 24 months for performance
     * Useful for data corrections or initial setup
     */
    @Transactional
    public void recalculateAllForUser(Long userId) {
        // Find the earliest receipt date for this user
        Optional<LocalDate> earliestDateOpt = receiptRepository.findTop1ByUserIdOrderByPurchaseDateAsc(userId)
                .map(receipt -> receipt.getPurchaseDate());
        
        // If no receipts exist, nothing to calculate
        if (earliestDateOpt.isEmpty()) {
            System.out.println("No receipts found for user " + userId + ", skipping CPI calculation");
            return;
        }
        
        LocalDate earliestDate = earliestDateOpt.get();
        
        // Limit to last 24 months for performance
        LocalDate twoYearsAgo = LocalDate.now().minusYears(2);
        if (earliestDate.isBefore(twoYearsAgo)) {
            System.out.println("Limiting calculation to last 24 months (from " + twoYearsAgo + ") instead of " + earliestDate);
            earliestDate = twoYearsAgo;
        }
        
        YearMonth startYearMonth = YearMonth.from(earliestDate);
        YearMonth endYearMonth = YearMonth.now();
        
        // Calculate total months to process
        long monthsToProcess = java.time.temporal.ChronoUnit.MONTHS.between(startYearMonth, endYearMonth) + 1;
        System.out.println("Calculating CPI for user " + userId + " from " + startYearMonth + " to " + endYearMonth + " (" + monthsToProcess + " months)");
        
        // Calculate for each month
        YearMonth current = startYearMonth;
        int processedCount = 0;
        while (!current.isAfter(endYearMonth)) {
            calculateMonthlyPersonalCPI(userId, current.getYear(), current.getMonthValue());
            processedCount++;
            
            // Log progress every 6 months
            if (processedCount % 6 == 0) {
                System.out.println("Progress: " + processedCount + "/" + monthsToProcess + " months processed");
            }
            
            current = current.plusMonths(1);
        }
        
        System.out.println("CPI calculation complete for user " + userId + ": " + processedCount + " months processed");
    }
    
    /**
     * Asynchronously recalculate all personal CPI data for a user
     * Runs in background thread, returns immediately
     */
    @Async
    @Transactional
    public void recalculateAllForUserAsync(Long userId) {
        System.out.println("Starting async CPI calculation for user " + userId);
        recalculateAllForUser(userId);
        System.out.println("Async CPI calculation completed for user " + userId);
    }
}
