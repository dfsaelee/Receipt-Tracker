package PersonalCPI.PersonalCPI.repository;

import PersonalCPI.PersonalCPI.model.OfficialCpiData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for OfficialCpiData entity.
 * Provides data access methods for official BLS CPI data.
 */
@Repository
public interface OfficialCpiDataRepository extends JpaRepository<OfficialCpiData, Long> {
    
    /**
     * Find all CPI data for a specific month (all categories + overall)
     */
    List<OfficialCpiData> findByYearAndMonth(Integer year, Integer month);
    
    /**
     * Find CPI data for a specific category in a specific month
     */
    Optional<OfficialCpiData> findByYearAndMonthAndCategoryId(
            Integer year, Integer month, Long categoryId);
    
    /**
     * Find overall CPI (categoryId is null) for a specific month
     */
    Optional<OfficialCpiData> findByYearAndMonthAndCategoryIdIsNull(Integer year, Integer month);
    
    /**
     * Find the most recent CPI data entry
     */
    Optional<OfficialCpiData> findTopByOrderByYearDescMonthDesc();
    
    /**
     * Find all CPI data for a specific year
     */
    List<OfficialCpiData> findByYearOrderByMonthDesc(Integer year);
    
    /**
     * Check if data exists for a specific month
     */
    boolean existsByYearAndMonth(Integer year, Integer month);
    
    /**
     * Find overall CPI data for a date range
     */
    @Query("SELECT o FROM OfficialCpiData o " +
           "WHERE o.categoryId IS NULL " +
           "AND ((o.year = :startYear AND o.month >= :startMonth) OR o.year > :startYear) " +
           "AND ((o.year = :endYear AND o.month <= :endMonth) OR o.year < :endYear) " +
           "ORDER BY o.year DESC, o.month DESC")
    List<OfficialCpiData> findOverallCpiByDateRange(
            Integer startYear, Integer startMonth, Integer endYear, Integer endMonth);
}
