package PersonalCPI.PersonalCPI.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing official US CPI data from Bureau of Labor Statistics.
 * Used for comparison with personal CPI calculations.
 */
@Entity
@Table(name = "official_cpi_data",
       uniqueConstraints = @UniqueConstraint(columnNames = {"year", "month", "category_id"}))
@Getter
@Setter
public class OfficialCpiData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "year", nullable = false)
    private Integer year;
    
    @Column(name = "month", nullable = false)
    private Integer month;
    
    @Column(name = "category_id")
    private Long categoryId;  // NULL for overall CPI
    
    @Column(name = "index_value", nullable = false, precision = 10, scale = 3)
    private BigDecimal indexValue;
    
    @Column(name = "mom_change_percent", precision = 5, scale = 2)
    private BigDecimal momChangePercent;
    
    @Column(name = "yoy_change_percent", precision = 5, scale = 2)
    private BigDecimal yoyChangePercent;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
