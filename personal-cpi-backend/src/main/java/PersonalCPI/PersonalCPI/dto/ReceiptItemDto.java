package PersonalCPI.PersonalCPI.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO for ReceiptItem entity.
 * Used for both request and response operations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptItemDto {
    private Long receiptItemId;
    private Long receiptId;
    private String itemName;
    private Integer quantity;
    private BigDecimal unitPrice;
}
