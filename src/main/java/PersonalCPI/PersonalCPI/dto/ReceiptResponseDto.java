package PersonalCPI.PersonalCPI.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class ReceiptResponseDto {
    private Long receiptId;
    private String storeName;
    private LocalDate purchaseDate;
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public ReceiptResponseDto(Long receiptId, String storeName, LocalDate purchaseDate, Long categoryId, String categoryName, BigDecimal amount, LocalDateTime createdAt) {
        this.receiptId = receiptId;
        this.storeName = storeName;
        this.purchaseDate = purchaseDate;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.amount = amount;
        this.createdAt = createdAt;
    }
}
