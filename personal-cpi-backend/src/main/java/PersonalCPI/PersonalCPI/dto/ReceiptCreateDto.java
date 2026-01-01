package PersonalCPI.PersonalCPI.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ReceiptCreateDto {
    private Long receiptId;
    
    @NotBlank(message = "Store name is required")
    @Size(max = 255, message = "Store name must not exceed 255 characters")
    private String storeName;
    
    @PastOrPresent(message = "Purchase date cannot be in the future")
    private LocalDate purchaseDate;
    
    @Min(value = 1, message = "Category ID must be between 1 and 8")
    @Max(value = 8, message = "Category ID must be between 1 and 8")
    private Long categoryId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @DecimalMax(value = "999999.99", message = "Amount must not exceed 999999.99")
    @Digits(integer = 6, fraction = 2, message = "Amount must have at most 6 digits and 2 decimal places")
    private BigDecimal amount;

    @Size(max = 500, message = "Image key must not exceed 500 characters")
    private String imageKey;
    
    private List<ReceiptItemDto> items;
}
