-- mimiced from https://www.youtube.com/watch?v=SAdRcCMSjL0

-- supabasedatabasepass!2020visionCode

CREATE TABLE users (

    user\_id SERIAL PRIMARY KEY,

    username VARCHAR(50) UNIQUE NOT NULL,

    email VARCHAR(100) UNIQUE NOT NULL,

    password\_hash VARCHAR(200) NOT NULL,

    role VARCHAR(20) DEFAULT 'USER', -- could be USER / ADMIN

    enabled BOOLEAN DEFAULT TRUE,

    created\_at TIMESTAMP DEFAULT NOW()

);



CREATE TABLE categories (

    category\_id SERIAL PRIMARY KEY,

    name VARCHAR(50) NOT NULL UNIQUE,

    description TEXT

    );



INSERT INTO categories (name, description) VALUES

('Food and Beverages', 'Groceries, restaurants, alcohol, etc.'),

('Housing', 'Rent, utilities, furnishings, etc.'),

('Apparel', 'Clothing and footwear'),

('Transportation', 'Gas, car payments, public transport'),

('Medical Care', 'Healthcare, drugs, insurance'),

('Recreation', 'Entertainment, hobbies, pets'),

('Education and Communication', 'Tuition, books, internet, phone'),

('Other Goods and Services', 'Personal care, insurance, misc');



CREATE TABLE receipts (

    receipt\_id SERIAL PRIMARY KEY,

    user\_id INT NOT NULL REFERENCES users(user\_id) on DELETE CASCADE, -- reference by users

    store\_name VARCHAR(100),

    purchase\_date DATE DEFAULT GETDATE(),

    total\_amount NUMERIC(10,2),

    created\_at TIMESTAMP DEFAULT NOW()

);



CREATE TABLE receipt\_items (

    item\_id SERIAL PRIMARY KEY,

    receipt\_id = INT NOT NULL REFERENCES receipts(receipt\_id) on DELETE CASCADE,

    category\_id = INT NOT NULL REFERENCES categories(category\_id),

    item\_name VARCHAR(100),

    unit\_price NUMERIC(10, 2) NOT NULL,

    quantity NUMERIC(10, 2) DEFAULT 1,

    total\_price NUMERIC(10, 2) GENERATED ALWAYS AS (quantity \* unit\_price) STORED

);











// real

-- WARNING: This schema is for context only and is not meant to be run.

-- Table order and constraints may not be valid for execution.



CREATE TABLE public.categories (

  category\_id bigint NOT NULL DEFAULT nextval('categories\_category\_id\_seq'::regclass),

  name character varying NOT NULL UNIQUE,

  description text,

  CONSTRAINT categories\_pkey PRIMARY KEY (category\_id)

);

CREATE TABLE public.receipts (

  receipt\_id bigint GENERATED ALWAYS AS IDENTITY NOT NULL UNIQUE,

  user\_id bigint NOT NULL,

  store\_name character varying DEFAULT ''::character varying,

  purchase\_date date DEFAULT CURRENT\_DATE,

  category\_id bigint,

  amount numeric NOT NULL,

  created\_at timestamp with time zone DEFAULT now(),

  CONSTRAINT receipts\_pkey PRIMARY KEY (receipt\_id),

  CONSTRAINT receipts\_category\_id\_fkey FOREIGN KEY (category\_id) REFERENCES public.categories(category\_id),

  CONSTRAINT receipts\_user\_id\_fkey FOREIGN KEY (user\_id) REFERENCES public.users(id)

);

CREATE TABLE public.users (

  id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,

  username character varying,

  email character varying,

  password character varying,

  enabled boolean,

  verification\_code character varying,

  verification\_expiration timestamp with time zone,

  CONSTRAINT users\_pkey PRIMARY KEY (id)

);





;

docker build -t personalcpi-app .

docker run --env-file ./PersonalCPI/.env -p 8080:8080 personalcpi-app



localstack:

pip install localstack

\# Refresh PATH (if needed in current session)

$env:Path = \[System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + \[System.Environment]::GetEnvironmentVariable("Path","User")



\# Start LocalStack in detached mode

localstack start -d

localstack --version





Zip it

./gradlew shadowJar (gets build/libs/my-app-all.jar)

Compress-Archive -Path "build\\libs\\personal-cpi-backend-0.0.1-SNAPSHOT-all.jar" -DestinationPath "function.zip" -Force



Configure Buckets

$env:AWS\_DEFAULT\_REGION = "us-east-1"

aws s3 mb s3://lambda-deployments --endpoint-url=http://localhost:4566



\# build jar

./gradlew clean shadowJar

\# Endpoints

aws s3 mb s3://lambda-deployments --endpoint-url=http://localhost:4566

\# upload jar to s3

aws s3 cp build/libs/personal-cpi-backend-0.0.1-SNAPSHOT-all.jar s3://lambda-deployments/function.jar --endpoint-url=http://localhost:4566

\# create lambda

aws lambda create-function --endpoint-url=http://localhost:4566 --function-name my-java-lambda --runtime java21 --handler PersonalCPI.PersonalCPI.lambda.ReceiptLambdaHandler::handleRequest --role arn:aws:iam::000000000000:role/dummy --code S3Bucket=lambda-deployments,S3Key=function.jar --timeout 60 --memory-size 512





\# test lambda invoke

.\\invoke-lambda.ps1



.\\gradlew.bat compileTestJava --console=plain 2>\&1

.\\gradlew.bat test --tests "\*ReceiptDataParserTest" 2>$null

.\\gradlew.bat test --tests "\*ReceiptDataParserTest" 2>$null

.\\gradlew.bat test --console=plain





Stop Local Stack

localstack stop



List Logs:

aws logs tail /aws/lambda/my-java-lambda --endpoint-url=http://localhost:4566 --follow



List Functions:

aws lambda list-functions --endpoint-url=http://localhost:4566



Delete Function

aws lambda delete-function --endpoint-url=http://localhost:4566 --function-name my-java-lambda



Tests Part 2:

\# Run all utility tests

./gradlew test --tests "\*util.\*Test"



\# Run specific test class

./gradlew test --tests "\*S3KeyParserTest"

./gradlew test --tests "\*ReceiptDataParserTest"



\# Clean build and run

./gradlew clean test --tests "\*util.\*Test"





Tests Part 3:

\# Run service tests

./gradlew test --tests "\*ExtractTextServiceTest"



\# Run Lambda handler tests

./gradlew test --tests "\*ReceiptLambdaHandlerTest"



\# Run all Lambda package tests

./gradlew test --tests "\*lambda.\*Test"



\# Run everything

./gradlew test --tests "\*lambda.\*Test" --tests "\*util.\*Test"







SQS:

./gradlew clean shadowJar

sam deploy



\# Get the SQS queue URL from CloudFormation outputs

aws cloudformation describe-stacks `

    --stack-name receipt-processor-stack `

    --region us-west-1 `

    --query "Stacks\[0].Outputs\[?OutputKey=='ReceiptQueueUrl'].OutputValue" `

    --output text



AWS\_SQS\_RECEIPT\_QUEUE\_URL=https://sqs.us-west-1.amazonaws.com/163747920911/receipt-processing-queue add to env





\# upload test receipts

aws s3 cp walmartTest.jpeg s3://personal-cpi-receipts/receipts/502/walmartTest.jpeg

aws s3 cp targetTest.webp s3://personal-cpi-receipts/receipts/502/targetTest.webp



\# Logs

sam logs -n ReceiptProcessorFunction --stack-name receipt-processor-stack --region us-west-1 --tail



\# Check SQS Queue

aws sqs receive-message `

    --queue-url https://sqs.us-west-1.amazonaws.com/163747920911/receipt-processing-queue `

    --region us-west-1 `

    --max-number-of-messages 1



\# Check queue attributes

aws sqs get-queue-attributes `

    --queue-url https://sqs.us-west-1.amazonaws.com/163747920911/receipt-processing-queue `

    --region us-west-1 `

    --attribute-names All



\# View sam logs:

sam logs -n ReceiptProcessorFunction --stack-name receipt-processor-stack --region us-west-1 --start-time "30m ago"



\# Check all receipts

aws s3 ls s3://personal-cpi-receipts/receipts/ --recursive --region us-west-1





\# Check Stuff 

./gradlew build





