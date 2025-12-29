package PersonalCPI.PersonalCPI.lambda;

import PersonalCPI.PersonalCPI.dto.ReceiptWithItems;
import PersonalCPI.PersonalCPI.model.Receipt;
import PersonalCPI.PersonalCPI.model.ReceiptItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtractTextServiceTest {

    @Mock
    private TextractClient textractClient;

    private ExtractTextService extractTextService;

    @BeforeEach
    void setUp() {
        extractTextService = new ExtractTextService(textractClient);
    }

    @Test
    void extractReceipt_withValidResponse_extractsReceiptData() {
        // Arrange
        String bucketName = "test-bucket";
        String objectKey = "receipts/123/receipt.jpg";

        AnalyzeExpenseResponse mockResponse = createSimpleMockResponse("Walmart", "2024-12-26", "45.99");
        when(textractClient.analyzeExpense(any(AnalyzeExpenseRequest.class))).thenReturn(mockResponse);

        // Act
        ReceiptWithItems result = extractTextService.extractReceiptData(bucketName, objectKey);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getReceipt()).isNotNull();
        assertThat(result.getReceipt().getStoreName()).isEqualTo("Walmart");
        assertThat(result.getReceipt().getUserId()).isEqualTo(123L);
        assertThat(result.getReceipt().getAmount()).isEqualByComparingTo("45.99");
        verify(textractClient).analyzeExpense(any(AnalyzeExpenseRequest.class));
    }

    @Test
    void extractReceipt_Data_withNoExpenseDocuments_throwsException() {
        // Arrange
        String bucketName = "test-bucket";
        String objectKey = "receipts/123/receipt.jpg";

        AnalyzeExpenseResponse emptyResponse = AnalyzeExpenseResponse.builder()
            .expenseDocuments(new ArrayList<>())
            .build();

        when(textractClient.analyzeExpense(any(AnalyzeExpenseRequest.class))).thenReturn(emptyResponse);

        // Act & Assert
        assertThatThrownBy(() -> extractTextService.extractReceiptData(bucketName, objectKey))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("No expense documents found");
    }

    @Test
    void extractReceipt_Data_withTextractException_throwsRuntimeException() {
        // Arrange
        String bucketName = "test-bucket";
        String objectKey = "receipts/123/receipt.jpg";

        when(textractClient.analyzeExpense(any(AnalyzeExpenseRequest.class)))
            .thenThrow(TextractException.builder().message("Textract error").build());

        // Act & Assert
        assertThatThrownBy(() -> extractTextService.extractReceiptData(bucketName, objectKey))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Textract");
    }

    @Test
    void extractReceipt_Data_withInvalidS3Key_throwsException() {
        // Arrange
        String bucketName = "test-bucket";
        String objectKey = "invalid/key/format.jpg"; // This doesn't match receipts/{userId}/

        AnalyzeExpenseResponse mockResponse = createSimpleMockResponse("Store", "2024-12-26", "10.00");
        when(textractClient.analyzeExpense(any(AnalyzeExpenseRequest.class))).thenReturn(mockResponse);

        // Act & Assert
        // The IllegalArgumentException is wrapped in RuntimeException by the catch block
        assertThatThrownBy(() -> extractTextService.extractReceiptData(bucketName, objectKey))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Invalid S3 key");

        // Textract IS called, but extraction fails during userId parsing
        verify(textractClient).analyzeExpense(any(AnalyzeExpenseRequest.class));
    }

    @Test
    void extractReceipt_Data_setsDefaultCategoryId() {
        // Arrange
        String bucketName = "test-bucket";
        String objectKey = "receipts/789/receipt.jpg";

        AnalyzeExpenseResponse mockResponse = createSimpleMockResponse("Store", "2024-12-26", "25.00");
        when(textractClient.analyzeExpense(any(AnalyzeExpenseRequest.class))).thenReturn(mockResponse);

        // Act
        ReceiptWithItems result = extractTextService.extractReceiptData(bucketName, objectKey);

        // Assert
        assertThat(result.getReceipt().getCategoryId()).isEqualTo(8L);
    }

    @Test
    void extractReceipt_Data_extractsUserIdFromKey() {
        // Arrange
        String bucketName = "test-bucket";
        String objectKey = "receipts/456/receipt.jpg";

        AnalyzeExpenseResponse mockResponse = createSimpleMockResponse("Store", "2024-12-26", "10.00");
        when(textractClient.analyzeExpense(any(AnalyzeExpenseRequest.class))).thenReturn(mockResponse);

        // Act
        ReceiptWithItems result = extractTextService.extractReceiptData(bucketName, objectKey);

        // Assert
        assertThat(result.getReceipt().getUserId()).isEqualTo(456L);
        verify(textractClient).analyzeExpense(any(AnalyzeExpenseRequest.class));
    }

    @Test
    void extractReceipt_Data_withLineItems_extractsItemsCorrectly() {
        // Arrange
        String bucketName = "test-bucket";
        String objectKey = "receipts/999/receipt.jpg";

        AnalyzeExpenseResponse mockResponse = createMockResponseWithLineItems();
        when(textractClient.analyzeExpense(any(AnalyzeExpenseRequest.class))).thenReturn(mockResponse);

        // Act
        ReceiptWithItems result = extractTextService.extractReceiptData(bucketName, objectKey);

        // Assert
        assertThat(result.getItems()).isNotEmpty();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getItemName()).isEqualTo("Milk");
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    // Helper to create mock Textract response
    private AnalyzeExpenseResponse createSimpleMockResponse(String vendor, String date, String amount) {
        List<ExpenseField> fields = new ArrayList<>();
        fields.add(createField("VENDOR_NAME", vendor, 95.0f));
        fields.add(createField("INVOICE_RECEIPT_DATE", date, 90.0f));
        fields.add(createField("AMOUNT_DUE", amount, 92.0f));

        ExpenseDocument doc = ExpenseDocument.builder()
            .summaryFields(fields)
            .lineItemGroups(new ArrayList<>())
            .build();

        return AnalyzeExpenseResponse.builder()
            .expenseDocuments(List.of(doc))
            .build();
    }

    private AnalyzeExpenseResponse createMockResponseWithLineItems() {
        List<ExpenseField> summaryFields = new ArrayList<>();
        summaryFields.add(createField("VENDOR_NAME", "Target", 95.0f));
        summaryFields.add(createField("INVOICE_RECEIPT_DATE", "2024-12-26", 90.0f));
        summaryFields.add(createField("AMOUNT_DUE", "50.00", 92.0f));

        // Create line items
        List<ExpenseField> item1Fields = new ArrayList<>();
        item1Fields.add(createField("ITEM", "Milk", 90.0f));
        item1Fields.add(createField("QUANTITY", "2", 85.0f));
        item1Fields.add(createField("PRICE", "3.99", 88.0f));

        LineItemFields lineItem1 = LineItemFields.builder()
            .lineItemExpenseFields(item1Fields)
            .build();

        LineItemGroup lineItemGroup = LineItemGroup.builder()
            .lineItems(List.of(lineItem1))
            .build();

        ExpenseDocument doc = ExpenseDocument.builder()
            .summaryFields(summaryFields)
            .lineItemGroups(List.of(lineItemGroup))
            .build();

        return AnalyzeExpenseResponse.builder()
            .expenseDocuments(List.of(doc))
            .build();
    }

    private ExpenseField createField(String type, String value, float confidence) {
        return ExpenseField.builder()
            .type(ExpenseType.builder().text(type).build())
            .valueDetection(ExpenseDetection.builder().text(value).confidence(confidence).build())
            .build();
    }
}