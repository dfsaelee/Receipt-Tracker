package PersonalCPI.PersonalCPI.dto;

import java.math.BigDecimal;

/**
 * DTO for Official CPI response
 */
public class OfficialCpiDto {
    private BigDecimal indexValue;
    private BigDecimal momChangePercent;
    private BigDecimal yoyChangePercent;
    
    public OfficialCpiDto() {}
    
    public OfficialCpiDto(BigDecimal indexValue, BigDecimal momChangePercent, BigDecimal yoyChangePercent) {
        this.indexValue = indexValue;
        this.momChangePercent = momChangePercent;
        this.yoyChangePercent = yoyChangePercent;
    }
    
    // Getters and setters
    public BigDecimal getIndexValue() { return indexValue; }
    public void setIndexValue(BigDecimal indexValue) { this.indexValue = indexValue; }
    
    public BigDecimal getMomChangePercent() { return momChangePercent; }
    public void setMomChangePercent(BigDecimal momChangePercent) { this.momChangePercent = momChangePercent; }
    
    public BigDecimal getYoyChangePercent() { return yoyChangePercent; }
    public void setYoyChangePercent(BigDecimal yoyChangePercent) { this.yoyChangePercent = yoyChangePercent; }
}
