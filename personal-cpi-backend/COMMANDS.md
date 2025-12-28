# LocalStack Lambda Commands Reference

Quick reference for all LocalStack and Lambda commands. Copy and paste as needed.

---

## 1. Start LocalStack

```bash
cd localstack
docker-compose up -d
cd ..

# Wait and check health (optional)
curl http://localhost:4566/_localstack/health

# Or check if container is running
docker ps --filter "name=localstack"
```

---

## 2. Deploy Lambda Function

```bash
# Set environment variable
export AWS_DEFAULT_REGION=us-east-1

# Build the JAR
./gradlew clean shadowJar

# Create S3 bucket in LocalStack
aws s3 mb s3://lambda-deployments --endpoint-url=http://localhost:4566

# Upload JAR to S3
aws s3 cp build/libs/personal-cpi-backend-0.0.1-SNAPSHOT-all.jar s3://lambda-deployments/function.jar --endpoint-url=http://localhost:4566

# Create Lambda function
# Note: arn:aws:iam::000000000000:role/dummy is a placeholder for your actual IAM role ARN
aws lambda create-function \
  --endpoint-url=http://localhost:4566 \
  --function-name my-java-lambda \
  --runtime java21 \
  --handler PersonalCPI.PersonalCPI.lambda.ReceiptLambdaHandler::handleRequest \
  --role arn:aws:iam::000000000000:role/dummy \
  --code S3Bucket=lambda-deployments,S3Key=function.jar \
  --timeout 60 \
  --memory-size 512
```

---

## 3. Invoke Lambda Function

```bash
# Set environment variable
export AWS_DEFAULT_REGION=us-east-1

# Invoke the Lambda (s3-event.json already exists in localstack/ folder)
aws lambda invoke \
  --endpoint-url=http://localhost:4566 \
  --function-name my-java-lambda \
  --cli-binary-format raw-in-base64-out \
  --payload file://localstack/s3-event.json \
  response.json

# View response
cat response.json
```

---

## 4. View Lambda Logs

```bash
# View Lambda logs in real-time
aws logs tail /aws/lambda/my-java-lambda --endpoint-url=http://localhost:4566 --follow

# Or view LocalStack container logs
cd localstack
docker-compose logs -f localstack
```

---

## 5. Update Lambda Function (After Code Changes)

```bash
# Rebuild JAR
./gradlew clean shadowJar

# Upload new JAR to S3
aws s3 cp build/libs/personal-cpi-backend-0.0.1-SNAPSHOT-all.jar s3://lambda-deployments/function.jar --endpoint-url=http://localhost:4566

# Update Lambda function code
aws lambda update-function-code \
  --endpoint-url=http://localhost:4566 \
  --function-name my-java-lambda \
  --s3-bucket lambda-deployments \
  --s3-key function.jar
```

---

## 6. Delete Lambda Function

```bash
# Delete the Lambda function
aws lambda delete-function \
  --endpoint-url=http://localhost:4566 \
  --function-name my-java-lambda

# Delete S3 bucket contents and bucket
aws s3 rm s3://lambda-deployments --recursive --endpoint-url=http://localhost:4566
aws s3 rb s3://lambda-deployments --endpoint-url=http://localhost:4566
```

---

## 7. Stop LocalStack

```bash
# Stop and remove containers
cd localstack
docker-compose down

# Stop and remove containers + volumes (clean slate)
docker-compose down -v
```

---

## Quick Workflow

### First Time Setup
```bash
# 1. Start LocalStack
cd localstack
docker-compose up -d
cd ..

# 2. Deploy Lambda (run all commands from section 2)
```

### Daily Development
```bash
# Start LocalStack (if not running)
cd localstack && docker-compose up -d && cd ..

# Make code changes...

# Rebuild and update Lambda (run commands from section 5)

# Test Lambda (run commands from section 3)
```

### Cleanup
```bash
# Stop LocalStack
cd localstack && docker-compose down
```

---

## Troubleshooting Commands

```powershell
# Check LocalStack health
curl http://localhost:4566/_localstack/health

# List all Lambda functions
aws lambda list-functions --endpoint-url=http://localhost:4566

# Get Lambda function details
aws lambda get-function --endpoint-url=http://localhost:4566 --function-name my-java-lambda

# List S3 buckets
aws s3 ls --endpoint-url=http://localhost:4566

# List files in S3 bucket
aws s3 ls s3://lambda-deployments --endpoint-url=http://localhost:4566

# Check Docker containers
docker ps -a

# View LocalStack logs
docker logs localstack

# Restart LocalStack
docker-compose restart
```
