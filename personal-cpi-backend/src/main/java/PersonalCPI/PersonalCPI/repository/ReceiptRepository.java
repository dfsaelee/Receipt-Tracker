package PersonalCPI.PersonalCPI.repository;

import PersonalCPI.PersonalCPI.model.Receipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    
    // Find earliest receipt for a user
    Optional<Receipt> findTop1ByUserIdOrderByPurchaseDateAsc(Long userId);

    // Optimized queries using JOIN FETCH to prevent N+1 problem
    /**
     * Fetches receipts with items in a single query to avoid the N+1 problem.
     */
    @Query("SELECT DISTINCT r FROM Receipt r " +
           "LEFT JOIN FETCH r.items " +
           "WHERE r.userId = :userId " +
           "ORDER BY r.purchaseDate DESC")
    List<Receipt> findByUserIdWithItems(@Param("userId") Long userId);

    /**
     * Fetch receipts by IDs with associated items.
     * Used for efficient pagination with related entities.
     */
    @Query("SELECT DISTINCT r FROM Receipt r " +
           "LEFT JOIN FETCH r.items " +
           "WHERE r.receiptId IN :receiptIds")
    List<Receipt> findByReceiptIdInWithItems(@Param("receiptIds") List<Long> receiptIds);

    /**
     * Paginated version with JOIN FETCH.
     * Note: We use a subquery approach to avoid pagination issues with JOIN FETCH.
     */
    @Query("SELECT r FROM Receipt r " +
           "WHERE r.userId = :userId " +
           "ORDER BY r.purchaseDate DESC")
    Page<Receipt> findByUserIdOrderByPurchaseDateDesc(Long userId, Pageable pageable);

    /**
     * Paginated date range query
     */
    @Query("SELECT r FROM Receipt r " +
           "WHERE r.userId = :userId " +
           "AND r.purchaseDate BETWEEN :startDate AND :endDate " +
           "ORDER BY r.purchaseDate DESC")
    Page<Receipt> findByUserIdAndPurchaseDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * Paginated category query
     */
    @Query("SELECT r FROM Receipt r " +
           "WHERE r.userId = :userId " +
           "AND r.categoryId = :categoryId " +
           "ORDER BY r.purchaseDate DESC")
    Page<Receipt> findByUserIdAndCategoryId(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            Pageable pageable);
}
