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

### The Producer Codebase - Gist
1) The Java program attached above serves as the **Kinesis Producer**, responsible for **publishing records to the Kinesis Data Stream**. It imports necessary libraries from the AWS SDK for Java, including the **Kinesis Producer Library**.

2) Initially, it **reads and parses NYC Taxi Telemetry Data** from a CSV file, & retrieves a list of **Trip Objects**.
 Next, the code configures the Kinesis Producer with specific parameters, fine-tuning **Configuration Settings** such as **Record Buffer Time**, **Maximum Connections**, **Request Timeout**, and **Record TTL**. It then creates a **Kinesis Producer instance** based on the provided configuration.

4) To enhance scalability and throughput, especially in the context of real-time streaming data, the code incorporates **parallelism** by utilizing multiple worker threads. This is achieved by employing an _**ExecutorService**_ with a **fixed thread pool size**. 
This enables the code to effectively distribute the workload across multiple threads, increasing overall throughput and responsiveness to incoming data.
   
5) Here, we have used _**CompletableFuture**_ in conjunction with the ExecutorService, for **truly non-blocking asynchronous processing**. 
 _**ExecutorService**_ helps us with configuring the threads, _**CompletableFuture**_ helps us in defining and managing the tasks to be executed on these threads,

6) Finally, the code prints **shard IDs for successful puts** and **attempts for failures**.
</br>

### Strategy for Effective Thread Management

**The Pain-Point:-** 

Submitting a task to the _ExecutorService_ is asynchronous. However, upon submitting the task, it returns a _Future_ object immediately, which helps in tracking the status and retrieving the result at a later point.
_**Future.get() forces the calling thread to wait. this makes the solution only partially asynchronous. Not recommended**_

***Our Solution:-***

_ExecutorService_ + _CompletableFuture_

We used a combination of both, since CompletableFuture provides non-blocking methods like thenApply() and thenCombine() to handle asynchronous task results. These execute post the completion of Future. My entire workflow is now fully asynchronous. It allows chaining and managing tasks efficiently without blocking the calling thread.

## Data Transformation

Here, we'd be using _**Kinesis Data Firehose**_, in conjuction with _**AWS Glue**_.

#### Why Firehose + Glue? 

Kinesis Firehose is excellent at capturing and loading streaming data reliably into Data Stores / Analytical Tools (In our case, S3 would be our Data Store).
It's fully managed, and scales automatically to match the throughput of incoming data.
It can help with minimal processing -> For instance, it can handle simpler transformations involving conversion of Data Formats, or simple processing through Lambda.

#### Role of AWS Glue

As a central Metadata Repository through Data Catalog. The Schema Definitions it stores, enhances querying capabilities in Athena. **Athena can use the Schema Information from the Data Catalog for querying data stored in S3.**
(I've shared the Table Definition above)

<ins>**_Key Design Considerations I've made_**</ins> 

1) _**Data Format Transformation**_ - 
In the scope of our project, Kinesis Data Firehose has been leveraged for both data ingestion into S3 and preliminary data transformation. A key aspect of this is conversion from JSON to Parquet format.

_Rationale Behind Data Format Conversion:_
- Storage optimization
- Parquet is an efficient, compressed, columnar storage format --> significantly reduces storage space requirements.

_Advantages in Analytical Context:_
When it comes to analytical tools, especially those like Amazon Athena, they're optimized for columnar storage, Parquet's structure allows for more efficient data querying operations --> faster insights and improved overall performance in data analytics workflow.








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






    

