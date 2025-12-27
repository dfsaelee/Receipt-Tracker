package PersonalCPI.PersonalCPI.lambda;

import PersonalCPI.PersonalCPI.dto.ReceiptWithItems;
import PersonalCPI.PersonalCPI.model.Receipt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptLambdaHandlerTest {

    @Mock
    private ExtractTextService extractTextService;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    private ReceiptLambdaHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ReceiptLambdaHandler(extractTextService);
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    void handleRequest_withValidS3Event_returnsTrue() {
        // Arrange
        S3Event s3Event = createS3Event("test-bucket", "receipts/123/receipt.jpg");
        ReceiptWithItems mockResult = createMockReceipt(1L, "Walmart", "45.99");
        
        when(extractTextService.extractAndSaveReceipt(anyString(), anyString())).thenReturn(mockResult);

        // Act
        Boolean result = handler.handleRequest(s3Event, context);

        // Assert
        assertThat(result).isTrue();
        verify(extractTextService).extractAndSaveReceipt("test-bucket", "receipts/123/receipt.jpg");
    }

    @Test
    void handleRequest_withNullEvent_returnsFalse() {
        // Act
        Boolean result = handler.handleRequest(null, context);

        // Assert
        assertThat(result).isFalse();
        verify(extractTextService, never()).extractAndSaveReceipt(anyString(), anyString());
    }

    @Test
    void handleRequest_withEmptyRecords_returnsFalse() {
        // Arrange
        S3Event s3Event = new S3Event(Collections.emptyList());

        // Act
        Boolean result = handler.handleRequest(s3Event, context);

        // Assert
        assertThat(result).isFalse();
        verify(extractTextService, never()).extractAndSaveReceipt(anyString(), anyString());
    }

    @Test
    void handleRequest_withServiceException_returnsFalse() {
        // Arrange
        S3Event s3Event = createS3Event("test-bucket", "receipts/123/receipt.jpg");
        when(extractTextService.extractAndSaveReceipt(anyString(), anyString()))
            .thenThrow(new RuntimeException("Extraction failed"));

        // Act
        Boolean result = handler.handleRequest(s3Event, context);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void handleRequest_withEmptyBucketName_returnsFalse() {
        // Arrange
        S3Event s3Event = createS3Event("", "receipts/123/receipt.jpg");

        // Act
        Boolean result = handler.handleRequest(s3Event, context);

        // Assert
        assertThat(result).isFalse();
        verify(extractTextService, never()).extractAndSaveReceipt(anyString(), anyString());
    }

    @Test
    void handleRequest_withEmptyObjectKey_returnsFalse() {
        // Arrange
        S3Event s3Event = createS3Event("test-bucket", "");

        // Act
        Boolean result = handler.handleRequest(s3Event, context);

        // Assert
        assertThat(result).isFalse();
        verify(extractTextService, never()).extractAndSaveReceipt(anyString(), anyString());
    }

    // Helper methods
    private S3Event createS3Event(String bucketName, String objectKey) {
        S3EventNotification.S3BucketEntity bucket = 
            new S3EventNotification.S3BucketEntity(bucketName, null, null);
        
        S3EventNotification.S3ObjectEntity object = 
            new S3EventNotification.S3ObjectEntity(objectKey, null, null, null, null);
        
        S3EventNotification.S3Entity s3Entity = 
            new S3EventNotification.S3Entity(null, bucket, object, null);
        
        S3EventNotification.S3EventNotificationRecord record = 
            new S3EventNotification.S3EventNotificationRecord(
                null, null, null, null, null, null, null, s3Entity, null
            );

        return new S3Event(List.of(record));
    }

    private ReceiptWithItems createMockReceipt(Long receiptId, String storeName, String amount) {
        Receipt receipt = new Receipt();
        receipt.setReceiptId(receiptId);
        receipt.setStoreName(storeName);
        receipt.setAmount(new BigDecimal(amount));
        receipt.setPurchaseDate(LocalDate.now());
        receipt.setUserId(123L);

        return new ReceiptWithItems(receipt, new ArrayList<>());
    }
}
