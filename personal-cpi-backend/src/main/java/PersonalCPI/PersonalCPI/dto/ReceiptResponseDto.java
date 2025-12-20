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
    private String imageKey; // S3 object key
    private String imageUrl; // Presigned URL for image access

    public ReceiptResponseDto(Long receiptId, String storeName, LocalDate purchaseDate, Long categoryId, String categoryName, BigDecimal amount, LocalDateTime createdAt, String imageKey, String imageUrl) {
        this.receiptId = receiptId;
        this.storeName = storeName;
        this.purchaseDate = purchaseDate;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.amount = amount;
        this.createdAt = createdAt;
        this.imageKey = imageKey;
        this.imageUrl = imageUrl;
    }
}
