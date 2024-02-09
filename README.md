# Streamlined-Real-Time-Data-Processing-with-Amazon-Kinesis

### Introduction
This is a Real-time Streaming data pipeline, wherein we've integrated Data Processing with Data Ingestion, Transformation, Storage, Analysis, and Visualization, creating a robust end-to-end solution. This has been tailored for building scalable, resilient pipelines for real-time Data Processing and Analytics, leveraging AWS services - Kinesis, Lambda, Glue, OpenSearch

----------------------------------
</br>

### Phase I - Data Ingestion


_**AWS Service Used:-**_ 
Kinesis Data Streams

_**Primary Purpose:**-_
Capturing and ingesting huge amounts of streaming data. It's core function revolves around ingesting substantial volumes of Real-time Streaming Data, ranging from website clickstreams to IoT & Telemetry Data.
From a high-level perspective, Producers inject records into the data stream, KDS acts as a conduit, and the consumers process the continuous influx of Data.
</br>

#### **_Architectural Design Considerations I've made:-_**
</br>

_**a) Mechanism used to send Data into the KDS:-**_ 

Leveraging Kinesis Producer Library for constructing our Data Producers. 
The reason I prefer KPL over Kinesis SDK is because it simplifies the process of producing data to the Kinesis streams, while improving efficiency, reliability, and scalability. It aggregates user records to efficiently pack multiple records into a single PUT request, reducing the overhead of PUT operations and improving throughput.

_**b) Capacity Mode:-**_
I've used the On-demand Capacity Mode for KDS, since my Data Stream's throughtput requirements are not yet defined, They're unpredictable and variable.
With the On-Demand Mode, the Data Stream's Capacity scales automatically.


## Setting up the Environment for Kinesis

The CF template defined above comprises the resources we'd need before starting off with our project:-

**_a) VPC:_**
Includes Security Groups for the Cloud9 Instance - for secure Development & Testing Environments.

**_b) Cloud9 Instance:_**
A t3.medium instance configured with Amazon Linux 2 - Serving as an IDE for exploring Kinesis Client Library & code.

**_c) S3 Buckets:_**
Two buckets, one for storing the original Taxi Trip dataset,  another for receiving curated data from the Kinesis Data Firehose.

**_d) Glue Database:_**
A database setup within AWS Glue - Centralized Repository for Kinesis Data Analytics Studio application source & target tables.

**_e) Kinesis Analytics Role:_** 
IAM role with fine-grained permissions for Kinesis Analytics operations - Securing Access & Execution

**_f) Kinesis Analytics Studio Application:_** 
Zeppelin & Flink Runtime Environments - To support interactive analytics and insights generation.

**_g) Lambda Functions:_** 
Includes functions for custom Data Processing and initiating Kinesis Analytics Studio applications.

**_h) OpenSearch Instance:_**
 Analytics engine for Data Exploration.

**_Outputs:_**:
S3 bucket names, Cloud9 environment URL, Lambda function ARN, Glue database name, and more for reference and access.






    

