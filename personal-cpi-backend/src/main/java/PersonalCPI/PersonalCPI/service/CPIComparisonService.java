package PersonalCPI.PersonalCPI.service;

import PersonalCPI.PersonalCPI.model.OfficialCpiData;
import PersonalCPI.PersonalCPI.model.PersonalCpiMonthly;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for comparing personal CPI with official CPI data.
 * Calculates deltas and generates comparison insights.
 */
@Service
public class CPIComparisonService {
    
    private final PersonalCPIService personalCPIService;
    private final OfficialCPIService officialCPIService;
    
    @Autowired
    public CPIComparisonService(
            PersonalCPIService personalCPIService,
            OfficialCPIService officialCPIService) {
        this.personalCPIService = personalCPIService;
        this.officialCPIService = officialCPIService;
    }
    
    /**
     * Compare personal CPI to official CPI for a specific month
     * Returns a comparison object with deltas and insights
     */
    public ComparisonResult compareToOfficialCPI(Long userId, Integer year, Integer month) {
        // Get personal CPI data
        Optional<PersonalCpiMonthly> personalCpi = personalCPIService.getOverallPersonalCPI(userId, year, month);
        
        if (personalCpi.isEmpty()) {
            throw new IllegalArgumentException("No personal CPI data found for " + year + "-" + month);
        }
        
        // Get official CPI data
        Optional<OfficialCpiData> officialCpi = officialCPIService.getOverallOfficialCPI(year, month);
        
        if (officialCpi.isEmpty()) {
            throw new IllegalArgumentException("No official CPI data found for " + year + "-" + month);
        }
        
        return buildComparisonResult(userId, year, month, personalCpi.get(), officialCpi.get());
    }
    
    /**
     * Get time series comparison data for a date range
     */
    public List<ComparisonResult> getComparisonTimeSeries(Long userId, LocalDate startDate, LocalDate endDate) {
        List<ComparisonResult> results = new ArrayList<>();
        
        YearMonth start = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);
        
        YearMonth current = start;
        while (!current.isAfter(end)) {
            try {
                ComparisonResult result = compareToOfficialCPI(
                        userId, current.getYear(), current.getMonthValue());
                results.add(result);
            } catch (IllegalArgumentException e) {
                // Skip months with missing data
            }
            current = current.plusMonths(1);
        }
        
        return results;
    }
    
    /**
     * Build comparison result object
     */
    private ComparisonResult buildComparisonResult(
            Long userId, Integer year, Integer month,
            PersonalCpiMonthly personalCpi, OfficialCpiData officialCpi) {
        
        ComparisonResult result = new ComparisonResult();
        result.setUserId(userId);
        result.setYear(year);
        result.setMonth(month);
        
        // Personal CPI data
        result.setPersonalTotalSpending(personalCpi.getTotalSpending());
        result.setPersonalMomChangePercent(personalCpi.getMomChangePercent());
        result.setPersonalYoyChangePercent(personalCpi.getYoyChangePercent());
        
        // Official CPI data
        result.setOfficialIndexValue(officialCpi.getIndexValue());
        result.setOfficialMomChangePercent(officialCpi.getMomChangePercent());
        result.setOfficialYoyChangePercent(officialCpi.getYoyChangePercent());
        
        // Calculate deltas
        if (personalCpi.getMomChangePercent() != null && officialCpi.getMomChangePercent() != null) {
            BigDecimal deltaMom = personalCpi.getMomChangePercent()
                    .subtract(officialCpi.getMomChangePercent())
                    .setScale(2, RoundingMode.HALF_UP);
            result.setDeltaMom(deltaMom);
        }
        
        if (personalCpi.getYoyChangePercent() != null && officialCpi.getYoyChangePercent() != null) {
            BigDecimal deltaYoy = personalCpi.getYoyChangePercent()
                    .subtract(officialCpi.getYoyChangePercent())
                    .setScale(2, RoundingMode.HALF_UP);
            result.setDeltaYoy(deltaYoy);
        }
        
        // Generate message
        result.setMessage(generateComparisonMessage(result));
        
        return result;
    }
    
    /**
     * Generate human-readable comparison message
     */
    private String generateComparisonMessage(ComparisonResult result) {
        if (result.getDeltaYoy() == null) {
            return "Insufficient data for year-over-year comparison.";
        }
        
        BigDecimal deltaYoy = result.getDeltaYoy();
        
        if (deltaYoy.compareTo(BigDecimal.ZERO) > 0) {
            return String.format("Your personal inflation is running %.2f%% higher than the national average this year.",
                    deltaYoy.abs());
        } else if (deltaYoy.compareTo(BigDecimal.ZERO) < 0) {
            return String.format("Your personal inflation is running %.2f%% lower than the national average this year.",
                    deltaYoy.abs());
        } else {
            return "Your personal inflation matches the national average this year.";
        }
    }
    
    /**
     * Inner class to hold comparison results
     */
    public static class ComparisonResult {
        private Long userId;
        private Integer year;
        private Integer month;
        
        // Personal CPI
        private BigDecimal personalTotalSpending;
        private BigDecimal personalMomChangePercent;
        private BigDecimal personalYoyChangePercent;
        
        // Official CPI
        private BigDecimal officialIndexValue;
        private BigDecimal officialMomChangePercent;
        private BigDecimal officialYoyChangePercent;
        
        // Comparison
        private BigDecimal deltaMom;
        private BigDecimal deltaYoy;
        private String message;
        
        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        
        public Integer getMonth() { return month; }
        public void setMonth(Integer month) { this.month = month; }
        
        public BigDecimal getPersonalTotalSpending() { return personalTotalSpending; }
        public void setPersonalTotalSpending(BigDecimal personalTotalSpending) { 
            this.personalTotalSpending = personalTotalSpending; 
        }
        
        public BigDecimal getPersonalMomChangePercent() { return personalMomChangePercent; }
        public void setPersonalMomChangePercent(BigDecimal personalMomChangePercent) { 
            this.personalMomChangePercent = personalMomChangePercent; 
        }
        
        public BigDecimal getPersonalYoyChangePercent() { return personalYoyChangePercent; }
        public void setPersonalYoyChangePercent(BigDecimal personalYoyChangePercent) { 
            this.personalYoyChangePercent = personalYoyChangePercent; 
        }
        
        public BigDecimal getOfficialIndexValue() { return officialIndexValue; }
        public void setOfficialIndexValue(BigDecimal officialIndexValue) { 
            this.officialIndexValue = officialIndexValue; 
        }
        
        public BigDecimal getOfficialMomChangePercent() { return officialMomChangePercent; }
        public void setOfficialMomChangePercent(BigDecimal officialMomChangePercent) { 
            this.officialMomChangePercent = officialMomChangePercent; 
        }
        
        public BigDecimal getOfficialYoyChangePercent() { return officialYoyChangePercent; }
        public void setOfficialYoyChangePercent(BigDecimal officialYoyChangePercent) { 
            this.officialYoyChangePercent = officialYoyChangePercent; 
        }
        
        public BigDecimal getDeltaMom() { return deltaMom; }
        public void setDeltaMom(BigDecimal deltaMom) { this.deltaMom = deltaMom; }
        
        public BigDecimal getDeltaYoy() { return deltaYoy; }
        public void setDeltaYoy(BigDecimal deltaYoy) { this.deltaYoy = deltaYoy; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
