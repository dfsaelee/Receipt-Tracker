package PersonalCPI.PersonalCPI.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter

public class MonthlySpendingDto {
    private int year;
    private int month;
    private String monthName;
    private BigDecimal totalAmount;

    public MonthlySpendingDto(int year, int month, BigDecimal totalAmount) {
        this.year = year;
        this.month = month;
        this.totalAmount = totalAmount;
    }

    // Override
    public void setMonth(int month) {
        this.month = month;
        this.monthName = monthNameFromNumber(month); // automatically update monthName
    }
    // Utility method
    private String monthNameFromNumber(int month) {
        String[] months = {"", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        if (month < 1 || month > 12) return "Unknown";
        return months[month];
    }
}
