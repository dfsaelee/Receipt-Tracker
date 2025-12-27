package PersonalCPI.PersonalCPI.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ExtractedReceipt {
    private String vendorName;
    private String amountDue;
    private String receiptDate;
    private String paymentTerms;
    private Map<String, Double> confidenceByField;
    private List<ExtractedReceiptItem> items;
    private boolean requiresManualReview;
}
