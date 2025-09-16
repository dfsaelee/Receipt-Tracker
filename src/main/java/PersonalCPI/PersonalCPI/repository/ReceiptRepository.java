package PersonalCPI.PersonalCPI.repository;

import PersonalCPI.PersonalCPI.model.Receipt;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    // Find all receipts for a specific user
    List<Receipt> findByUserIdOrderByPurchaseDateDesc(Long userId);

    // Find receipts by user and date range
    List<Receipt> findByUserIdAndPurchaseDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    // Find receipts by user and category
    List<Receipt> findByUserIdAndCategoryId(Long userId, Long categoryId);

    // Find receipts by user, category, and date range
    List<Receipt> findByUserIdAndCategoryIdAndPurchaseDateBetween(
            Long userId, Long categoryId, LocalDate startDate, LocalDate endDate);

    // Calculate total spending by user
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Receipt r WHERE r.userId = :userId")
    BigDecimal getTotalSpendingByUser(@Param("userId") Long userId);

    // Calculate total spending by user and date range
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Receipt r WHERE r.userId = :userId " +
            "AND r.purchaseDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalSpendingByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Calculate total spending by user and category
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Receipt r WHERE r.userId = :userId " +
            "AND r.categoryId = :categoryId")
    BigDecimal getTotalSpendingByUserAndCategory(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId);

    // Get spending summary by category for a user within date range
    @Query("SELECT r.categoryId, COALESCE(SUM(r.amount), 0) as total " +
            "FROM Receipt r WHERE r.userId = :userId " +
            "AND r.purchaseDate BETWEEN :startDate AND :endDate " +
            "GROUP BY r.categoryId")
    List<Object[]> getSpendingSummaryByCategory(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Get monthly spending summary for a user
    @Query("SELECT YEAR(r.purchaseDate) as year, MONTH(r.purchaseDate) as month, " +
            "COALESCE(SUM(r.amount), 0) as total " +
            "FROM Receipt r WHERE r.userId = :userId " +
            "GROUP BY YEAR(r.purchaseDate), MONTH(r.purchaseDate) " +
            "ORDER BY YEAR(r.purchaseDate) DESC, MONTH(r.purchaseDate) DESC")
    List<Object[]> getMonthlySpendingSummary(@Param("userId") Long userId);

    // Count receipts by user
    long countByUserId(Long userId);

    // Find recent receipts (last N receipts)
    List<Receipt> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
