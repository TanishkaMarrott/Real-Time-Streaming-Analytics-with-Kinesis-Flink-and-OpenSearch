# Streamlined-Real-Time-Data-Processing-with-Amazon-Kinesis

## Project Workflow 
This Real-time Streaming pipeline integrates Data Processing with Data Ingestion, Transformation, Storage, Analysis, and Visualization, creating a robust end-to-end solution. Services leveraged include  Kinesis, Lambda, Glue, OpenSearch.

There's been a strong emphasis on Design Considerations that align with the overarching Architectural Design, I've prioritized scalability, fault tolerance, security, and performance optimization across all system layers.

## Data Ingestion - Specifics

_**AWS Service Utilised:-**_ 
Kinesis Data Streams

_**Primary Objective:-**_
Capturing & ingesting extensive streams of real-time data, serving as a pivotal bridge between data producers and consumers.


<rarr> <ins>**_Key Design Considerations I've made_**</ins> 

_**a) Data Injection Mechanism**_ 

  I've leveraged Kinesis Producer Library for constructing our Data Producers. 
  Quick Breakdown:-
  </br>
  

 | Feature                                                           | Description                                                                                                                                                                                  |
|-------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
|  Aggregates multiple records into a single PUT request       | Reduces operational overhead & improves throughput.        |                                                                                                                                  |
| Handles requests asynchronously  | Decouples the Data Production Logic from Stream Interaction, that is, Data production continues at a steady pace, without being affected by the latency introduced by Stream Interactions.          |
|  Implements graceful Error Handling     | Define the criterion for subsequent retry attempts upon failure, for more resilience and reliability.                                                                                 |
|  Compresses data to reduce the amount of data transmitted   | Optimizes bandwidth usage and reducing costs.                                                                                                                                              |
|  Collects Metrics with regards to Data production & Stream Interaction | - Data production metrics, primarily the ones related to volume of data generated, aiding in estimating the capacity to be provisioned. - Stream Interaction metrics (Ex- Latency in processing Data) help in providing insights into the performance and health of the data ingestion process. |
</br>

_**b) Capacity Mode**_

I've opted for the _On-demand Capacity Mode_ for Kinesis Data Streams due to the unpredictable and variable nature of my data stream's throughput requirements. With this mode, the capacity of the data stream scales automatically based on the incoming data volume, ensuring that I don't need to predefine or manage shard capacities.

This flexibility is crucial for accommodating sudden spikes in data ingestion rates or adjusting to changing application demands.

### The Producer Codebase - In a Gist

In our project, the Kinesis Producer is developed in Java, and is tailored to handle NYC Taxi Telemetry Data. The code initiates by parsing data from a CSV file into Trip Objects, then configures the Kinesis Producer with parameters like Record Buffer Time and Maximum Connections. We've incorporated parallelism and concurrency by deploying an ExecutorService with a fixed thread pool size for optimizing scalability and throughput. The integration of CompletableFuture with ExecutorService facilitates a fully non-blocking asynchronous processing, --> the efficiency of task management across threads. And finally graceful Error handling through the output of shard IDs for success and logging of failures. This ensures a robust Data Ingestion Layer.
</br>

### Strategy I've leveraged for Effective Thread Management

**The Pain-Point:-** 

Submitting a task to the _ExecutorService_ is asynchronous. However, upon submitting the task, it returns a _Future_ object immediately, which helps in tracking the status and retrieving the result at a later point.
_**Future.get() forces the calling thread to wait. this makes the solution only partially asynchronous. Not recommended**_

**Our Solution:-**

_ExecutorService_ + _CompletableFuture_

I've used a combination of both, since CompletableFuture provides non-blocking methods like thenApply() and thenCombine() to handle asynchronous task results. These execute post the completion of Future. My entire workflow is now fully asynchronous. It allows chaining and managing tasks efficiently without blocking the calling thread.

</br>

## Data Transformation
Here, I'd be using _**Kinesis Data Firehose**_, in conjuction with _**AWS Glue**_.

#### Why Firehose + Glue? 

Kinesis Firehose is excellent at capturing and loading streaming data reliably into Data Stores / Analytical Tools (In our case, S3 would be our Data Store).
It's fully managed, and scales automatically to match the throughput of incoming data.
It can help with minimal processing -> For instance, it can handle simpler transformations involving conversion of Data Formats, or simple processing through Lambda.

#### Role of AWS Glue

As a central Metadata Repository through Data Catalog. The Schema Definitions it stores, enhances querying capabilities in Athena. **Athena can use the Schema Information from the Data Catalog for querying data stored in S3.**
(I've shared the Table Definition above, Firehose references this definition in Glue)

</br>

<ins>**_Key Design Considerations I've made_**</ins> 

_**a) Data Format Transformation:-**_ 

In the scope of our project, Kinesis Data Firehose has been leveraged for both data delivery into S3 and preliminary data transformation. A key aspect of this is conversion from JSON to Parquet format. Couple of Reasons here- a) Significantly reduces Storage Costs. b) Parquet's columnar structure allows for more efficient data querying in Athena

_**b) Buffer Interval Optimisation:-**_

In our project's configuration of Kinesis Data Firehose, I've opted to _maximize the Buffer Interval time for data delivery into Amazon S3._ 
Rationale behind this:- By allowing Data to accumulate in large batches before delivery, we're reducing the number of PUT requests to S3, thereby reducing transaction costs. This also results in improvising the throughput through batching and subsequent storage. Something around 300-600 would be a good number to start with.

Buffer Size has been maximised, Costs would be lowered, but at the cost of a higher latency. 

Cranking up the Buffer Interval to 900 seconds (max possible) would be a relative choice. We need to strike balance between the timely availability of data versus the operational costs incurred. 
</br>

_**c) S3 Compression and Encryption:-**_

 I've utilized Snappy compression for source records,  which leads to faster transmission and cost savings in storage. I'm prioritising high speed over a higher compression ratio.
 Encryption is implemented through AWS-owned keys for security and confidentiality of data as it moves through the Firehose stream, particularly crucial when converting data formats like JSON to Parquet.















</br>



---


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






    

