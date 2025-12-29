package PersonalCPI.PersonalCPI.lambda;

import PersonalCPI.PersonalCPI.dto.ReceiptMessage;
import PersonalCPI.PersonalCPI.dto.ReceiptWithItems;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.textract.TextractClient;

import java.util.stream.Collectors;

/**
 * AWS Lambda handler for processing receipt images uploaded to S3.
 * Extracts receipt data using Textract and sends to SQS queue.
 */
public class ReceiptLambdaHandler implements RequestHandler<S3Event, Boolean> {
    private final ExtractTextService extractTextService;
    private final SqsClient sqsClient;
    private final String queueUrl;
    private final ObjectMapper objectMapper;

    /**
     * No-arg constructor for AWS Lambda.
     */
    public ReceiptLambdaHandler() {
        // Get region from Lambda environment
        String regionStr = System.getenv("AWS_REGION");
        Region region = regionStr != null ? Region.of(regionStr) : Region.US_WEST_1;
        System.out.println("Using region: " + region);

        // Create Textract client
        TextractClient textractClient = TextractClient.builder()
            .region(region)
            .build();

        // Create SQS client
        this.sqsClient = SqsClient.builder()
            .region(region)
            .build();

        // Get queue URL from environment
        this.queueUrl = System.getenv("RECEIPT_QUEUE_URL");
        if (this.queueUrl == null || this.queueUrl.isEmpty()) {
            throw new IllegalStateException("RECEIPT_QUEUE_URL environment variable is not set");
        }
        // Create service without database repositories
        this.extractTextService = new ExtractTextService(textractClient);

        // JSON mapper for SQS messages
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        System.out.println("ReceiptLambdaHandler initialized successfully!");
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
            try {
                // Extract S3 information
                S3EventNotification.S3Entity s3 = record.getS3();
                if (s3 == null || s3.getBucket() == null || s3.getObject() == null) {
                    logger.log("ERROR: Invalid S3 record - missing bucket or object information");
                    allSuccessful = false;
                    continue;
                }

                String bucketName = s3.getBucket().getName();
                String objectKey = s3.getObject().getKey();

                logger.log(String.format("Processing receipt: s3://%s/%s", bucketName, objectKey));

                // Extract receipt data using Textract
                ReceiptWithItems receiptWithItems = extractTextService.extractReceiptData(bucketName, objectKey);

                logger.log(String.format("Extracted - Store: %s, Amount: %s, Items: %d",
                    receiptWithItems.getReceipt().getStoreName(),
                    receiptWithItems.getReceipt().getAmount(),
                    receiptWithItems.getItemCount()));

                sendToSqs(receiptWithItems, logger);

            } catch (Exception e) {
                logger.log(String.format("ERROR: Failed to process receipt - " + e.getMessage()));
                e.printStackTrace();
                allSuccessful = false;
            }
        }

        return allSuccessful;
    }

    private void sendToSqs(ReceiptWithItems receiptWithItems, LambdaLogger logger) throws JsonProcessingException {
        // Convert to SQS message format
        ReceiptMessage message = new ReceiptMessage(
            receiptWithItems.getReceipt().getStoreName(),
            receiptWithItems.getReceipt().getAmount(),
            receiptWithItems.getReceipt().getPurchaseDate() != null ?
                java.sql.Date.valueOf(receiptWithItems.getReceipt().getPurchaseDate()) : null,
            receiptWithItems.getReceipt().getImageKey(),
            receiptWithItems.getReceipt().getUserId(),
            receiptWithItems.getItems().stream()
                .map(item -> new ReceiptMessage.ReceiptItemMessage(
                    item.getItemName(),
                    item.getQuantity(),
                    item.getUnitPrice()  // Changed from getPrice() to getUnitPrice()
                ))
                .collect(Collectors.toList())
        );

        // Send to SQS
        String messageBody = objectMapper.writeValueAsString(message);
        SendMessageRequest sendRequest = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(messageBody)
            .build();

        sqsClient.sendMessage(sendRequest);

        logger.log(String.format("SUCCESS: Sent receipt to SQS - Store: %s, Amount: %s",
            message.getStoreName(), message.getAmount()));
    }
}
