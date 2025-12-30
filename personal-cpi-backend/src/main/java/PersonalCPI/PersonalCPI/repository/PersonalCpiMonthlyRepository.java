package PersonalCPI.PersonalCPI.repository;

import PersonalCPI.PersonalCPI.model.PersonalCpiMonthly;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PersonalCpiMonthly entity.
 * Provides data access methods for personal CPI calculations.
 */
@Repository
public interface PersonalCpiMonthlyRepository extends JpaRepository<PersonalCpiMonthly, Long> {
    
    /**
     * Find all CPI data for a user in a specific month
     */
    List<PersonalCpiMonthly> findByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);
    
    /**
     * Find CPI data for a specific category in a specific month
     */
    Optional<PersonalCpiMonthly> findByUserIdAndYearAndMonthAndCategoryId(
            Long userId, Integer year, Integer month, Long categoryId);
    
    /**
     * Find overall CPI (categoryId is null) for a specific month
     */
    Optional<PersonalCpiMonthly> findByUserIdAndYearAndMonthAndCategoryIdIsNull(
            Long userId, Integer year, Integer month);
    
    /**
     * Find all CPI data for a user in a specific year, ordered by month descending
     */
    List<PersonalCpiMonthly> findByUserIdAndYearOrderByMonthDesc(Long userId, Integer year);
    
    /**
     * Find recent CPI data for a user, ordered by year and month descending
     */
    List<PersonalCpiMonthly> findByUserIdOrderByYearDescMonthDesc(Long userId, Pageable pageable);
    
    /**
     * Delete all CPI data for a specific month
     */
    void deleteByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);
    
    /**
     * Find all months with data for a user in a date range
     */
    @Query("SELECT DISTINCT p FROM PersonalCpiMonthly p " +
           "WHERE p.userId = :userId " +
           "AND p.categoryId IS NULL " +
           "AND ((p.year = :startYear AND p.month >= :startMonth) OR p.year > :startYear) " +
           "AND ((p.year = :endYear AND p.month <= :endMonth) OR p.year < :endYear) " +
           "ORDER BY p.year DESC, p.month DESC")
    List<PersonalCpiMonthly> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startYear") Integer startYear,
            @Param("startMonth") Integer startMonth,
            @Param("endYear") Integer endYear,
            @Param("endMonth") Integer endMonth);
}
