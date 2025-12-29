# Receipt Tracker Backend API

REST API for receipt tracking with image storage, item management, and spending analytics.

## Authentication

All endpoints require JWT authentication in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

## API Endpoints

### Receipt Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/receipts` | Create receipt with optional items |
| GET | `/api/receipts/all` | Get all receipts with items |
| GET | `/api/receipts/{id}` | Get receipt by ID |
| GET | `/api/receipts/date-range` | Filter by date range (params: startDate, endDate) |
| GET | `/api/receipts/category/{categoryId}` | Filter by category |
| GET | `/api/receipts/recent` | Get 10 most recent receipts |
| PUT | `/api/receipts/{id}` | Update receipt |
| DELETE | `/api/receipts/{id}` | Delete receipt (cascades to items and S3 image) |
| PATCH | `/api/receipts/{id}/category` | Update category only |
| GET | `/api/receipts/summary/current-month` | Current month spending by category |
| GET | `/api/receipts/summary/period` | Period spending (params: startDate, endDate) |
| GET | `/api/receipts/summary/monthly` | Monthly spending breakdown |
| GET | `/api/receipts/total` | Total spending |

### Receipt Item Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/receipt-items/receipt/{receiptId}` | Get all items for receipt |
| GET | `/api/receipt-items/{itemId}` | Get item by ID |
| POST | `/api/receipt-items` | Create single item |
| POST | `/api/receipt-items/receipt/{receiptId}/batch` | Create multiple items |
| PUT | `/api/receipt-items/{itemId}` | Update item |
| DELETE | `/api/receipt-items/{itemId}` | Delete item |
| DELETE | `/api/receipt-items/receipt/{receiptId}` | Delete all items for receipt |

### S3 Image Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/receipts/upload` | Upload image (multipart/form-data) |
| GET | `/api/receipts/receipt-image` | Get presigned URL (param: key) |

## Request/Response Examples

### Create Receipt
```json
POST /api/receipts
{
  "storeName": "Walmart",
  "purchaseDate": "2025-12-28",
  "categoryId": 1,
  "amount": 45.99,
  "imageKey": "receipts/123/1735441234567_receipt.jpg",
  "items": [
    { "itemName": "Milk", "quantity": 2, "unitPrice": 3.99 },
    { "itemName": "Bread", "quantity": 1, "unitPrice": 2.49 }
  ]
}
```

Response:
```json
{
  "receiptId": 1,
  "storeName": "Walmart",
  "purchaseDate": "2025-12-28",
  "categoryId": 1,
  "categoryName": "Groceries",
  "amount": 45.99,
  "createdAt": "2025-12-28T20:40:44",
  "imageKey": "receipts/123/1735441234567_receipt.jpg",
  "imageUrl": "https://s3.amazonaws.com/...",
  "items": [
    {
      "receiptItemId": 1,
      "receiptId": 1,
      "itemName": "Milk",
      "quantity": 2,
      "unitPrice": 3.99
    }
  ]
}
```

### Upload Image
```bash
POST /api/receipts/upload
Content-Type: multipart/form-data
Field: file

Response:
{
  "key": "receipts/123/1735441234567_receipt.jpg",
  "message": "File uploaded successfully"
}
```

### Update Category
```json
PATCH /api/receipts/{id}/category
{
  "categoryId": 2
}
```

## Data Models

### Receipt
```typescript
{
  receiptId: number;
  storeName: string;
  purchaseDate: string;        // ISO date (YYYY-MM-DD)
  categoryId: number;           // 1-8
  categoryName: string;
  amount: number;
  createdAt: string;            // ISO datetime
  imageKey?: string;
  imageUrl?: string;            // Presigned URL (temporary)
  items: ReceiptItem[];
}
```

### ReceiptItem
```typescript
{
  receiptItemId: number;
  receiptId: number;
  itemName: string;
  quantity: number;
  unitPrice: number;
}
```

## Error Responses

| Status | Response |
|--------|----------|
| 400 | `{ "error": "Receipt not found" }` |
| 401 | `{ "error": "User not authenticated" }` |
| 403 | `{ "error": "Access denied to this image" }` |
| 500 | `{ "error": "Failed to create receipt" }` |

## Important Notes

- All receipt responses automatically include items
- Presigned image URLs expire; use fresh URLs from latest fetch
- Deleting receipts cascades to items and S3 images
- Category IDs valid range: 1-8
- Date format: ISO (YYYY-MM-DD)
- Users can only access their own receipts/items

## Development

### Build and Test
```bash
./gradlew build
./gradlew test
```

### LocalStack Lambda Development

#### Start LocalStack
```bash
cd localstack
docker-compose up -d
cd ..
```

#### Deploy Lambda
```bash
export AWS_DEFAULT_REGION=us-east-1

# Build JAR
./gradlew clean shadowJar

# Create S3 bucket
aws s3 mb s3://lambda-deployments --endpoint-url=http://localhost:4566

# Upload JAR
aws s3 cp build/libs/personal-cpi-backend-0.0.1-SNAPSHOT-all.jar \
  s3://lambda-deployments/function.jar --endpoint-url=http://localhost:4566

# Create Lambda function
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

#### Invoke Lambda
```bash
aws lambda invoke \
  --endpoint-url=http://localhost:4566 \
  --function-name my-java-lambda \
  --cli-binary-format raw-in-base64-out \
  --payload file://localstack/s3-event.json \
  response.json

cat response.json
```

#### Update Lambda (after code changes)
```bash
./gradlew clean shadowJar

aws s3 cp build/libs/personal-cpi-backend-0.0.1-SNAPSHOT-all.jar \
  s3://lambda-deployments/function.jar --endpoint-url=http://localhost:4566

aws lambda update-function-code \
  --endpoint-url=http://localhost:4566 \
  --function-name my-java-lambda \
  --s3-bucket lambda-deployments \
  --s3-key function.jar
```

#### View Logs
```bash
# Lambda logs
aws logs tail /aws/lambda/my-java-lambda --endpoint-url=http://localhost:4566 --follow

# Container logs
cd localstack && docker-compose logs -f localstack
```

#### Stop LocalStack
```bash
cd localstack
docker-compose down      # Stop
docker-compose down -v   # Stop and clean volumes
```

## Architecture

### SOLID Principles
- Single Responsibility: Separate controllers/services for receipts and items
- Open/Closed: Services extensible without modification
- Dependency Inversion: Services depend on repository interfaces

### Transaction Management
- Create/update/delete operations use `@Transactional`
- Items created/updated atomically with receipts
- Rollback on errors ensures data consistency

## Recent Changes

### Receipt Items System
- Added complete CRUD for receipt items
- New endpoints: `/api/receipt-items/*`
- Batch operations support
- Items automatically included in receipt responses

### Enhanced Receipt Endpoints
- Create receipts with items in single request
- Update receipts replaces items transactionally
- Delete cascades to items and S3 images

### Files Added
- `ReceiptItemDto.java` - DTO for items
- `ReceiptItemService.java` - Business logic
- `ReceiptItemController.java` - REST endpoints

### Files Modified
- `ReceiptResponseDto.java` - Added items list
- `ReceiptCreateDto.java` - Added optional items
- `ReceiptService.java` - Enhanced with item support
