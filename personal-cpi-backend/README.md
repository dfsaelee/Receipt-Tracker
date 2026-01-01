# Receipt Tracker Backend

REST API for receipt tracking with image storage, CPI analytics, and spending insights.

## Quick Start

```bash
# Install dependencies
./gradlew build

# Set environment variables (see Environment Variables section)
cp .env.example .env

# Run application
./gradlew bootRun
```

## Database Setup

### 1. Create Supabase Project
Create a new project at [supabase.com](https://supabase.com)

### 2. Run Schema Script
Execute the `schema.sql` file in your Supabase SQL Editor:
```bash
# Copy contents of schema.sql and run in Supabase SQL Editor
# This creates all tables, indexes, and seed data
```

The schema includes:
- **Users** (managed by Supabase Auth)
- **Categories** (8 predefined spending categories)
- **Receipts** (user receipts with S3 image storage)
- **Receipt Items** (line items for each receipt)
- **Personal CPI Monthly** (calculated user inflation data)
- **Official CPI Data** (BLS government inflation data)

## AWS Setup

### S3 Bucket for Receipt Images
```bash
# Create S3 bucket
aws s3 mb s3://personal-cpi-receipts --region us-east-1

# Enable CORS (optional, for direct browser uploads)
aws s3api put-bucket-cors --bucket personal-cpi-receipts --cors-configuration file://cors.json
```

### Lambda Function (Optional - for receipt OCR)
```bash
# Build JAR
./gradlew clean shadowJar

# Deploy using SAM
sam build
sam deploy --guided
```

The Lambda function uses AWS Textract to extract receipt data from uploaded images.

### SQS Queue (Optional - for async processing)
Created automatically via `template.yaml`:
- **receipt-processing-queue** - Main queue for receipt processing
- **receipt-processing-dlq** - Dead letter queue for failed messages

## Environment Variables

Create a `.env` file with:

```properties
# Supabase
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=your-anon-key
SUPABASE_JWT_SECRET=your-jwt-secret

# AWS
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
S3_BUCKET_NAME=personal-cpi-receipts

# BLS API (for official CPI data)
BLS_API_KEY=your-bls-api-key

# Application
SPRING_PROFILES_ACTIVE=dev
```

## API Endpoints

All endpoints require JWT authentication via `Authorization: Bearer <token>` header (except auth endpoints).

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login and get JWT token |

### Receipts

| Method | Endpoint | Description | Query Params |
|--------|----------|-------------|--------------|
| POST | `/api/receipts` | Create receipt with optional items | - |
| GET | `/api/receipts/all` | Get all user receipts (paginated) | `page`, `size`, `sortBy`, `sortDir` |
| GET | `/api/receipts/{id}` | Get receipt by ID | - |
| GET | `/api/receipts/date-range` | Filter receipts by date range | `startDate`, `endDate`, `page`, `size` |
| GET | `/api/receipts/category/{categoryId}` | Filter receipts by category | `page`, `size` |
| GET | `/api/receipts/recent` | Get 10 most recent receipts | - |
| PUT | `/api/receipts/{id}` | Update receipt | - |
| DELETE | `/api/receipts/{id}` | Delete receipt (cascades to items & S3) | - |
| PATCH | `/api/receipts/{id}/category` | Update category only | - |
| GET | `/api/receipts/summary/current-month` | Current month spending by category | - |
| GET | `/api/receipts/summary/period` | Period spending summary | `startDate`, `endDate` |
| GET | `/api/receipts/summary/monthly` | Monthly spending breakdown | - |
| GET | `/api/receipts/total` | Total spending for user | - |

### Receipt Images (S3)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/receipts/upload` | Upload receipt image (multipart/form-data) |
| GET | `/api/receipts/receipt-image` | Get presigned URL for image (param: `key`) |

### Receipt Items

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/receipt-items/receipt/{receiptId}` | Get all items for receipt |
| GET | `/api/receipt-items/{itemId}` | Get item by ID |
| POST | `/api/receipt-items` | Create single item |
| POST | `/api/receipt-items/receipt/{receiptId}/batch` | Create multiple items |
| PUT | `/api/receipt-items/{itemId}` | Update item |
| DELETE | `/api/receipt-items/{itemId}` | Delete item |
| DELETE | `/api/receipt-items/receipt/{receiptId}` | Delete all items for receipt |

### Personal CPI

| Method | Endpoint | Description | Query Params |
|--------|----------|-------------|--------------|
| GET | `/api/cpi/personal` | Get personal CPI for month | `year`, `month` (defaults to current) |
| GET | `/api/cpi/comparison` | Compare personal vs official CPI | `year`, `month` (defaults to current) |
| GET | `/api/cpi/comparison/timeseries` | Time series comparison | `startDate`, `endDate` |
| POST | `/api/cpi/calculate` | Trigger CPI calculation | `year`, `month` (defaults to current) |
| GET | `/api/cpi/official` | Get official CPI data | `year`, `month`, `categoryId` (optional) |

### Admin

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/admin/cpi/fetch` | Fetch latest BLS CPI data (24 months) |

## Request/Response Examples

### Create Receipt with Items
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

**Response:**
```json
{
  "receiptId": 1,
  "storeName": "Walmart",
  "purchaseDate": "2025-12-28",
  "categoryId": 1,
  "categoryName": "Food and Beverages",
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

### Upload Receipt Image
```bash
POST /api/receipts/upload
Content-Type: multipart/form-data
Field: file

# Response
{
  "key": "receipts/123/1735441234567_receipt.jpg",
  "message": "File uploaded successfully"
}
```

### Get Personal CPI Comparison
```bash
GET /api/cpi/comparison?year=2024&month=12

# Response
{
  "year": 2024,
  "month": 12,
  "personalCpi": [
    {
      "categoryId": 1,
      "categoryName": "Food and Beverages",
      "totalSpending": 450.00,
      "momChangePercent": 5.2,
      "yoyChangePercent": 12.3
    }
  ],
  "officialCpi": [
    {
      "categoryId": 1,
      "categoryName": "Food and Beverages",
      "indexValue": 315.162,
      "momChangePercent": 0.3,
      "yoyChangePercent": 2.7
    }
  ]
}
```

## Categories

| ID | Name | BLS Series ID |
|----|------|---------------|
| 1 | Food and Beverages | CUUR0000SAF |
| 2 | Housing | CUUR0000SAH |
| 3 | Apparel | CUUR0000SAA |
| 4 | Transportation | CUUR0000SAT |
| 5 | Medical Care | CUUR0000SAM |
| 6 | Recreation | CUUR0000SAR |
| 7 | Education and Communication | CUUR0000SAE |
| 8 | Other Goods and Services | CUUR0000SAG |

## Error Responses

| Status | Example Response |
|--------|------------------|
| 400 | `{ "error": "Receipt not found" }` |
| 401 | `{ "error": "User not authenticated" }` |
| 403 | `{ "error": "Access denied to this image" }` |
| 500 | `{ "error": "Failed to create receipt" }` |

## Important Notes

- All receipt responses automatically include items
- Presigned S3 URLs expire after 1 hour; fetch fresh URLs as needed
- Deleting receipts cascades to items and S3 images
- Date format: ISO (YYYY-MM-DD)
- Users can only access their own receipts/items
- Pagination defaults: `page=0`, `size=20`

## Build & Test

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Create JAR
./gradlew shadowJar
```
