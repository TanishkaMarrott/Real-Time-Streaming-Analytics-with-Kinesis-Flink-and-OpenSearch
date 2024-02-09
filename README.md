# Streamlined-Real-Time-Data-Processing-with-Amazon-Kinesis

## Introduction
This is a Real-time Streaming data pipeline, wherein we've integrated Data Processing with Data Ingestion, Transformation, Storage, Analysis, and Visualization, creating a robust end-to-end solution. This has been tailored for building scalable, resilient pipelines for real-time data processing and analytics, leveraging AWS services like Kinesis, Lambda, Glue, and OpenSearch.


## Environment Setup
The CF template defined above comprises the resources we'd need before starting off with our project:-

**_VPC:_**

Includes Security Groups for the Cloud9 Instance - for secure Development & Testing Environments.

**_Cloud9 Instance:_**

An t3.medium instance configured with Amazon Linux 2 - Serving as an IDE for exploring Kinesis Client Library & code.

**_S3 Buckets:_**

Two buckets, one for storing the original Taxi Trip dataset,  another for receiving curated data from the Kinesis Data Firehose.

**_Glue Database:_**

A database setup within AWS Glue - Centralized Repository for Kinesis Data Analytics Studio application source & target tables.

Kinesis Analytics Role: 
An IAM role with fine-grained permissions for Kinesis Analytics operations, ensuring secure access and execution.

Kinesis Analytics Studio Application: 
Configured with Zeppelin and Flink runtime environments, supporting interactive analytics and insights generation.

Lambda Functions: 
Includes functions for custom data processing and initiating Kinesis Analytics Studio applications, enhancing automation and efficiency.

OpenSearch Instance: 
Deployed with advanced security features like node-to-node encryption and encryption at rest, providing a powerful analytics engine for data exploration.

Outputs: 
Displays crucial resources like S3 bucket names, Cloud9 environment URL, Lambda function ARN, Glue database name, and more for easy reference and access.


    

