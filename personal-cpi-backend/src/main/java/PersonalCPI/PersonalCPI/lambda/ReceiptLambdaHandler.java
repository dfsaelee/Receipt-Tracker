package PersonalCPI.PersonalCPI.lambda;

import PersonalCPI.PersonalCPI.dto.ReceiptWithItems;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;

/**
 * AWS Lambda handler for processing receipt images uploaded to S3.
 * Simplified version - uses Textract only, no database.
 */
public class ReceiptLambdaHandler implements RequestHandler<S3Event, Boolean> {
    private final ExtractTextService extractTextService;

    /**
     * No-arg constructor for AWS Lambda.
     */
    public ReceiptLambdaHandler() {
        System.out.println("Initializing ReceiptLambdaHandler...");
        
        // Get region from Lambda environment (AWS sets this automatically)
        String regionStr = System.getenv("AWS_REGION");
        Region region = regionStr != null ? Region.of(regionStr) : Region.US_WEST_1;
        System.out.println("Using region: " + region);
        
        // Create Textract client
        TextractClient textractClient = TextractClient.builder()
            .region(region)
            .build();
        
        // Create service without database repositories (for now)
        this.extractTextService = new ExtractTextService(textractClient, null, null);
        
        System.out.println("ReceiptLambdaHandler initialized successfully!");
    }

    /**
     * Constructor for testing with dependency injection.
     */
    public ReceiptLambdaHandler(ExtractTextService extractTextService) {
        this.extractTextService = extractTextService;
    }

    @Override
    public Boolean handleRequest(S3Event s3Event, Context context) {
        LambdaLogger logger = context.getLogger();
        
        if (s3Event == null || s3Event.getRecords() == null || s3Event.getRecords().isEmpty()) {
            logger.log("ERROR: Invalid S3 event - no records found");
            return false;
        }

        boolean allSuccessful = true;

        for (S3EventNotification.S3EventNotificationRecord record : s3Event.getRecords()) {
            String bucketName = null;
            String objectKey = null;

            try {
                // Extract S3 information
                S3EventNotification.S3Entity s3 = record.getS3();
                if (s3 == null || s3.getBucket() == null || s3.getObject() == null) {
                    logger.log("ERROR: Invalid S3 record - missing bucket or object information");
                    allSuccessful = false;
                    continue;
                }

                bucketName = s3.getBucket().getName();
                objectKey = s3.getObject().getKey();

                if (bucketName == null || bucketName.isEmpty() || objectKey == null || objectKey.isEmpty()) {
                    logger.log("ERROR: Invalid S3 record - empty bucket name or object key");
                    allSuccessful = false;
                    continue;
                }

                logger.log(String.format("Processing receipt: s3://%s/%s", bucketName, objectKey));

                // Extract receipt data using Textract
                ReceiptWithItems receiptWithItems = extractTextService.extractAndSaveReceipt(bucketName, objectKey);

                logger.log(String.format("SUCCESS: Extracted receipt - Store: %s, Amount: %s, Items: %d",
                    receiptWithItems.getReceipt().getStoreName(),
                    receiptWithItems.getReceipt().getAmount(),
                    receiptWithItems.getItemCount()));

            } catch (Exception e) {
                logger.log(String.format("ERROR: Failed to process s3://%s/%s - %s",
                    bucketName, objectKey, e.getMessage()));
                e.printStackTrace();
                allSuccessful = false;
            }
        }

        return allSuccessful;
    }
}
