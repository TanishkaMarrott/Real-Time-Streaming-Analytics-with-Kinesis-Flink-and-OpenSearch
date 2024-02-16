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
</br>


### Kinesis Producer Codebase - What does it actually do?

1) The Java program attached above serves as the Kinesis Producer, responsible for publishing records to the Kinesis Data Stream. It imports necessary libraries from the AWS SDK for Java, including the Kinesis Producer Library.

2) Initially, it reads and parses NYC Taxi Telemetry Data from a CSV file, & retrieves a list of Trip Objects.

3) Next, the code configures the Kinesis Producer with specific parameters, fine-tuning Configuration Settings such as Record Buffer Time, Maximum Connections, Request Timeout, and Record TTL. It then creates a Kinesis Producer instance based on the provided configuration.

4) To enhance scalability and throughput, especially in the context of real-time streaming data, the code incorporates parallelism by utilizing multiple worker threads. This is achieved by employing an _ExecutorService_ with a fixed thread pool size. 

6) This enables the code to effectively distribute the workload across multiple threads, increasing overall throughput and responsiveness to incoming data.
   
7) Here, we have used _CompletableFuture_ in conjunction with the ExecutorService, for truly non-blocking asynchronous processing. 

8) _ExecutorService_ helps us with configuring the threads, _ComplatebleFuture_ helps us in defining and managing the tasks to be executed on these threads,

9) Finally, the code prints shard IDs for successful puts and attempts for failures.

### What Strategy did we leverage for Effective Thread Management?

- Submitting a task to the _ExecutorService_ is asynchronous, the task runs independently of the main thread. However, upon submitting the task, it returns a _Future_ object immediately, that would help us in tracking the status and retrieving the result at a later point.

  **Pain-Point:-** The _get()_ method used for retrieving the result of the future object is blocking. The thread that calls _get()_ will be in stalled state until the result is available.

***While the task itself is running asynchronously, retrieving its result via get() does not adhere to asynchronous principles, it forces the calling thread to wait.***
 
 Enter **CompletableFuture**.

- To handle the results of the asynchronous operation without blocking, CompletableFuture provides us with a rich set of methods, such as _thenApply()_, _thenCombine()_, _thenAccept()_, that allow us to specify callback functions to be executed once the future completes.

- These methods help maintain the truly asynchronous nature, by not blocking the calling thread, Instead, it schedules actions to be performed upon completion of the asynchronous task. This also aids in combining, chaining multiple futures, and for more complex workflows.

- Thus, Completable future  provides a way to manage, chain, and react to the completion of these asynchronous tasks, also in a non-blocking manner.

---

## Data Transformation

Here, we'd be using _**Kinesis Data Firehose**_, in conjuction with _**AWS Glue**_.

#### Why Firehose + Glue? 

Kinesis Firehose is excellent at capturing and loading streaming data reliably into Data Stores / Analytical Tools (In our case, S3 would be our Data Store).
It's fully managed, and scales automatically to match the throughput of incoming data.
It can help with minimal processing -> For instance, it can handle simpler transformations involving conversion of Data Formats, or simple processing through Lambda.

#### Utilization of Kinesis Firehose in the Project

In context with our project, we've utilised Kinesis Data Firehose for loading it into S3, and for initial transformation, wherein we've converted the Data format from JSON to Parquet

Reason for Data Format Conversion: 

This conversion is beneficial for optimizing storage (as Parquet is a compressed, columnar format) and for improving querying and analytics efficiency. _**Particularly advantageous when using analytic tools that perform better with columnar storage like Athena.**_

#### Role of AWS Glue in the Project

Glue, on the other hand, is specifically for complex ETL workflows. 

Glue as a Metadata Repository:-

However, in this project, Glue is being used for its significance as a central Metadata Repository through Data Catalog. The Schema Definitions it stores enhances querying capabilities in Athena. Athena can use the Schema Information from the Data Catalog for querying data stored in S3, which streamlines the Analytics process.







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






    

