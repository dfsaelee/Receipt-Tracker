# AWS SAM Testing Guide

## Prerequisites

1. **AWS SAM CLI** installed
2. **Docker** running (for local testing)
3. **AWS credentials** configured
4. **Build the JAR** first

## Build Steps

### 1. Build the Shadow JAR

```powershell
# Build the fat JAR with all dependencies
./gradlew clean shadowJar

# Verify the JAR was created
ls build/libs/
# Should see: personal-cpi-backend-0.0.1-SNAPSHOT-all.jar
```

### 2. Validate SAM Template

```powershell
sam validate --template template.yaml
```

## Local Testing (with Docker)

### Test with Mock S3 Event

```powershell
# Invoke locally with test event
sam local invoke ReceiptProcessorFunction --event test-event.json

# With environment variables
sam local invoke ReceiptProcessorFunction `
  --event test-event.json `
  --env-vars env.json
```

### Start Local API (if needed later)

```powershell
sam local start-lambda
```

## Deploy to AWS

### 1. Build for Deployment

```powershell
sam build
```

### 2. Deploy (Guided)

```powershell
# First time deployment
sam deploy --guided

# Follow prompts:
# - Stack Name: receipt-processor-stack
# - AWS Region: us-east-1
# - Confirm changes: Y
# - Allow SAM CLI IAM role creation: Y
# - Disable rollback: N
# - Save arguments to config: Y
```

### 3. Deploy (After First Time)

```powershell
# Use saved config
sam deploy
```

## Testing with Real AWS

### 1. Upload a Test Receipt

```powershell
# Get bucket name from outputs
$BUCKET_NAME = aws cloudformation describe-stacks `
  --stack-name receipt-processor-stack `
  --query 'Stacks[0].Outputs[?OutputKey==`ReceiptBucketName`].OutputValue' `
  --output text

# Upload test image
aws s3 cp path/to/receipt.jpg s3://$BUCKET_NAME/receipts/670/test-receipt.jpg
```

### 2. Check Lambda Logs

```powershell
# Get function name
sam logs -n ReceiptProcessorFunction --stack-name receipt-processor-stack --tail

# Or use AWS CLI
aws logs tail /aws/lambda/ReceiptProcessorFunction --follow
```

### 3. Verify Textract was Called

```powershell
# Check CloudWatch Logs for Textract calls
aws logs filter-log-events `
  --log-group-name /aws/lambda/ReceiptProcessorFunction `
  --filter-pattern "Textract"
```

## Test Event Details

The `test-event.json` simulates an S3 ObjectCreated event:
- **Bucket**: `receipt-uploads-123456789012`
- **Key**: `receipts/670/test-receipt.jpg`
- **User ID**: Extracted as `670` from the key

## Environment Variables (Optional)

Create `env.json` for local testing:

```json
{
  "ReceiptProcessorFunction": {
    "SPRING_PROFILES_ACTIVE": "local",
    "AWS_REGION": "us-east-1"
  }
}
```

## Cleanup

```powershell
# Delete the stack
sam delete --stack-name receipt-processor-stack

# Or use CloudFormation
aws cloudformation delete-stack --stack-name receipt-processor-stack
```

## Troubleshooting

### Lambda Timeout
- Increase timeout in `template.yaml` (currently 60s)
- Check CloudWatch Logs for actual execution time

### Textract Permissions
- Verify IAM role has `textract:AnalyzeExpense` permission
- Check CloudWatch Logs for permission errors

### S3 Access
- Ensure Lambda has `s3:GetObject` permission
- Verify bucket name matches in test event

### Build Issues
- Run `./gradlew clean shadowJar` to rebuild
- Check that JAR includes all dependencies
- Verify JAR path in `template.yaml`
