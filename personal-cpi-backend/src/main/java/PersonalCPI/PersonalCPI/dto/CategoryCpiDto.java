package PersonalCPI.PersonalCPI.dto;

import java.math.BigDecimal;

/**
 * DTO for category-level CPI data
 */
public class CategoryCpiDto {
    private Long categoryId;
    private String categoryName;
    private BigDecimal spending;
    private BigDecimal weightPercent;
    private BigDecimal momChangePercent;
    private BigDecimal yoyChangePercent;
    
    public CategoryCpiDto() {}
    
    public CategoryCpiDto(Long categoryId, String categoryName, BigDecimal spending,
                          BigDecimal weightPercent, BigDecimal momChangePercent, BigDecimal yoyChangePercent) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.spending = spending;
        this.weightPercent = weightPercent;
        this.momChangePercent = momChangePercent;
        this.yoyChangePercent = yoyChangePercent;
    }
    
    // Getters and setters
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    
    public BigDecimal getSpending() { return spending; }
    public void setSpending(BigDecimal spending) { this.spending = spending; }
    
    public BigDecimal getWeightPercent() { return weightPercent; }
    public void setWeightPercent(BigDecimal weightPercent) { this.weightPercent = weightPercent; }
    
    public BigDecimal getMomChangePercent() { return momChangePercent; }
    public void setMomChangePercent(BigDecimal momChangePercent) { this.momChangePercent = momChangePercent; }
    
    public BigDecimal getYoyChangePercent() { return yoyChangePercent; }
    public void setYoyChangePercent(BigDecimal yoyChangePercent) { this.yoyChangePercent = yoyChangePercent; }
}
