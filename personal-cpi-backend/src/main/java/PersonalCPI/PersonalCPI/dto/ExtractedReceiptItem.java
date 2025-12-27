package PersonalCPI.PersonalCPI.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExtractedReceiptItem {
    String name;
    String quantity;
    String price;
    String confidence;
}
