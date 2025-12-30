package PersonalCPI.PersonalCPI.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for Personal CPI response
 */
public class PersonalCpiDto {
    private BigDecimal totalSpending;
    private BigDecimal momChangePercent;
    private BigDecimal yoyChangePercent;
    private List<CategoryCpiDto> categories;
    
    public PersonalCpiDto() {}
    
    public PersonalCpiDto(BigDecimal totalSpending, BigDecimal momChangePercent, 
                          BigDecimal yoyChangePercent, List<CategoryCpiDto> categories) {
        this.totalSpending = totalSpending;
        this.momChangePercent = momChangePercent;
        this.yoyChangePercent = yoyChangePercent;
        this.categories = categories;
    }
    
    // Getters and setters
    public BigDecimal getTotalSpending() { return totalSpending; }
    public void setTotalSpending(BigDecimal totalSpending) { this.totalSpending = totalSpending; }
    
    public BigDecimal getMomChangePercent() { return momChangePercent; }
    public void setMomChangePercent(BigDecimal momChangePercent) { this.momChangePercent = momChangePercent; }
    
    public BigDecimal getYoyChangePercent() { return yoyChangePercent; }
    public void setYoyChangePercent(BigDecimal yoyChangePercent) { this.yoyChangePercent = yoyChangePercent; }
    
    public List<CategoryCpiDto> getCategories() { return categories; }
    public void setCategories(List<CategoryCpiDto> categories) { this.categories = categories; }
}
