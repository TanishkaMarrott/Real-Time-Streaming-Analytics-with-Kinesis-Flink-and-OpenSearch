# Streamlined-Real-Time-Data-Processing-with-Amazon-Kinesis

## Introduction
This is a Real-time Streaming data pipeline, wherein we've integrated Data Processing with Data Ingestion, Transformation, Storage, Analysis, and Visualization, creating a robust end-to-end solution. This has been tailored for building scalable, resilient pipelines for real-time Data Processing and Analytics, leveraging AWS services - Kinesis, Lambda, Glue, OpenSearch

## Environment Setup
The CF template defined above comprises the resources we'd need before starting off with our project:-

**_VPC:_**

Includes Security Groups for the Cloud9 Instance - for secure Development & Testing Environments.

**_Cloud9 Instance:_**

A t3.medium instance configured with Amazon Linux 2 - Serving as an IDE for exploring Kinesis Client Library & code.

**_S3 Buckets:_**

Two buckets, one for storing the original Taxi Trip dataset,  another for receiving curated data from the Kinesis Data Firehose.

**_Glue Database:_**

A database setup within AWS Glue - Centralized Repository for Kinesis Data Analytics Studio application source & target tables.

**_Kinesis Analytics Role:_** 

IAM role with fine-grained permissions for Kinesis Analytics operations - Securing Access & Execution

**_Kinesis Analytics Studio Application:_** 

Zeppelin & Flink Runtime Environments - To support interactive analytics and insights generation.

**_Lambda Functions:_** 

Includes functions for custom Data Processing and initiating Kinesis Analytics Studio applications.

**_OpenSearch Instance:_**

 Analytics engine for Data Exploration.

**_Outputs_**:
S3 bucket names, Cloud9 environment URL, Lambda function ARN, Glue database name, and more for reference and access.


    

