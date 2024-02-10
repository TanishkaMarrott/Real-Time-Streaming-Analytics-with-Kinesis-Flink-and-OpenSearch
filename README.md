# Streamlined-Real-Time-Data-Processing-with-Amazon-Kinesis
----

## Setting the context
This Real-time Streaming pipeline integrates Data Processing with Data Ingestion, Transformation, Storage, Analysis, and Visualization, creating a robust end-to-end solution. Services leveraged include  Kinesis, Lambda, Glue, OpenSearch.

There's been a strong emphasis on Design Considerations that align with the overarching Architectural Design, I've prioritized scalability, fault tolerance, security, and performance optimization across all system layers.

---
## Project Workflow

### Data Ingestion - Specifics


_**AWS Service Utilised:-**_ 
Kinesis Data Streams

_**Primary Objective:-**-_
Capturing & ingesting extensive streams of real-time data, serving as a pivotal bridge between data producers and consumers.

#### **_Key Design Considerations made:-_**

_**a) Data Injection Mechanism:-**_ 

 Leveraged Kinesis Producer Library for constructing our Data Producers.

Simplifies the process via - 

1 - _Aggregating multiple records into a single PUT request_  &nbsp; &rarr; &nbsp;  Reduces operational overhead & improves throughput.

2 - _Decoupling the Data Production Logic from Stream Interaction_ &nbsp;  &rarr;  &nbsp; This actually makes Request handling asynchronous, that is, Data production continues at a steady pace, without being affected by the latency introduced by Stream Interactions (Response received on receipt of Data)

3 - _Graceful Error handling via a retry logic_ &nbsp;  &rarr; &nbsp;  We can define the criterion for subsequent retry attempts upon failure, for more resilence and reliability. 

4 - Data Compression to reduce the amount of data transmitted over the network &nbsp; &rarr;  &nbsp; Optimizing bandwidth usage and reducing costs.

5 - Collecting metrics with regards to Data production & Stream Interaction 

&nbsp; &nbsp; &rarr; Data production metrics, primarily the ones related to volume of data generated, number of records per unit time -> Aids in estimating the capacity to be provisioned, 

&nbsp; &nbsp; &rarr; Stream Interaction metrics (Eg. Latency in processing Data) helps in identifying events of performance degradation. Providing insights into the performance and health of the data ingestion process.

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






    

