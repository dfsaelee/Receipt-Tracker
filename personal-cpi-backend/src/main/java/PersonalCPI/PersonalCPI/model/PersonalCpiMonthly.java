package PersonalCPI.PersonalCPI.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing monthly personal CPI calculations for a user.
 * Pre-calculated data stored for improved query performance.
 */
@Entity
@Table(name = "personal_cpi_monthly", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "year", "month", "category_id"}))
@Getter
@Setter
public class PersonalCpiMonthly {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "year", nullable = false)
    private Integer year;
    
    @Column(name = "month", nullable = false)
    private Integer month;
    
    @Column(name = "category_id")
    private Long categoryId;  // NULL for total across all categories
    
    @Column(name = "total_spending", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalSpending;
    
    @Column(name = "mom_change_percent", precision = 5, scale = 2)
    private BigDecimal momChangePercent;  // Month-over-Month
    
    @Column(name = "yoy_change_percent", precision = 5, scale = 2)
    private BigDecimal yoyChangePercent;  // Year-over-Year
    
    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;
    
    @PrePersist
    protected void onCreate() {
        if (calculatedAt == null) {
            calculatedAt = LocalDateTime.now();
        }
    }
}
