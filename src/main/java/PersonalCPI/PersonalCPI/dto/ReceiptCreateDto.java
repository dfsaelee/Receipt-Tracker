package PersonalCPI.PersonalCPI.dto;

import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.misc.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ReceiptCreateDto {
    private Long receiptId;
    private String storeName;
    private LocalDate purchaseDate;
    private Long categoryId;

    @NotNull // deprecated
    private BigDecimal amount;
}
