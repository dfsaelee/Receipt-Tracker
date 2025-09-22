package PersonalCPI.PersonalCPI.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SpendingSummaryDto {
    private Long categoryId;
    private String categoryName;
    private BigDecimal totalAmount;
    private int receiptCount;

    public SpendingSummaryDto(Long categoryId, String categoryName, BigDecimal totalAmount, int receiptCount) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.totalAmount = totalAmount;
        this.receiptCount = receiptCount;
    }
}
