# Personal CPI Backend - LocalStack Setup

Java Spring Boot backend with AWS Lambda support for receipt processing.

## Project Structure

```
personal-cpi-backend/
├── src/                          # Java source code
│   └── main/java/PersonalCPI/PersonalCPI/
│       └── lambda/
│           └── ReceiptLambdaHandler.java
├── localstack/                   # LocalStack configuration
│   ├── docker-compose.yml       # LocalStack Docker setup
│   └── s3-event.json           # Sample S3 event for testing
├── build.gradle                  # Gradle build configuration
├── README.md                     # Project documentation
└── COMMANDS.md                   # Quick reference commands
```

## Quick Start

### 1. Start LocalStack
```bash
cd localstack
docker-compose up -d
cd ..
```

### 2. Deploy Lambda Function
```bash
# Build JAR
./gradlew clean shadowJar

# Create S3 bucket
aws s3 mb s3://lambda-deployments --endpoint-url=http://localhost:4566 --region us-east-1

# Upload JAR
aws s3 cp build/libs/personal-cpi-backend-0.0.1-SNAPSHOT-all.jar s3://lambda-deployments/function.jar --endpoint-url=http://localhost:4566 --region us-east-1

# Create Lambda function
aws lambda create-function \
  --endpoint-url=http://localhost:4566 \
  --function-name my-java-lambda \
  --runtime java21 \
  --handler PersonalCPI.PersonalCPI.lambda.ReceiptLambdaHandler::handleRequest \
  --role arn:aws:iam::000000000000:role/dummy \
  --code S3Bucket=lambda-deployments,S3Key=function.jar \
  --timeout 60 \
  --memory-size 512 \
  --region us-east-1
```

### 3. Test Lambda Invocation
```bash
aws lambda invoke \
  --endpoint-url=http://localhost:4566 \
  --function-name my-java-lambda \
  --cli-binary-format raw-in-base64-out \
  --payload file://localstack/s3-event.json \
  --region us-east-1 \
  response.json

# View response
cat response.json
```

### 4. Stop LocalStack
```bash
cd localstack
docker-compose down
cd ..
```

## Requirements

- **Docker Desktop** - For running LocalStack
- **Java 21** - Lambda runtime
- **Gradle** - Build tool
- **AWS CLI** - For LocalStack interaction

## LocalStack Configuration

- **Port**: 4566 (LocalStack Gateway)
- **Services**: Lambda, S3, CloudWatch Logs
- **Lambda Timeout**: 60 seconds
- **Lambda Memory**: 512 MB
- **Docker Socket**: Mounted for Lambda execution

## Development Workflow

1. **Make code changes** to `ReceiptLambdaHandler.java`
2. **Rebuild & update Lambda**:
   ```bash
   ./gradlew shadowJar
   aws s3 cp build/libs/personal-cpi-backend-0.0.1-SNAPSHOT-all.jar s3://lambda-deployments/function.jar --endpoint-url=http://localhost:4566 --region us-east-1
   aws lambda update-function-code --endpoint-url=http://localhost:4566 --function-name my-java-lambda --s3-bucket lambda-deployments --s3-key function.jar --region us-east-1
   ```
3. **Test** - Re-run Lambda invocation command from Quick Start

## Troubleshooting

### LocalStack not starting
```bash
cd localstack
docker-compose logs localstack
```

### Lambda timeout errors
- Check LocalStack is running: `docker ps`
- Verify health: `curl http://localhost:4566/_localstack/health`
- Increase timeout in Lambda creation command if needed

### Port conflicts
- Stop old containers: `docker stop localstack-main`
- Clean restart: `cd localstack && docker-compose down -v && docker-compose up -d`

## Database

This project uses **Supabase** for PostgreSQL (not local database).

## Reference

See [COMMANDS.md](COMMANDS.md) for detailed command reference.
