# Streamlined-Real-Time-Data-Processing-with-Amazon-Kinesis

## Project Workflow 
This Real-time Streaming pipeline integrates Data Processing with Data Ingestion, Transformation, Storage, Analysis, and Visualization, creating a robust end-to-end solution. Services leveraged include  Kinesis, Lambda, Glue, OpenSearch.

There's been a strong emphasis on Design Considerations that align with the overarching Architectural Design, I've prioritized scalability, fault tolerance, security, and performance optimization across all system layers.

## The Data Ingestion Layer - Specifics

_**AWS Service Utilised:-**_ 
Kinesis Data Streams

_**Primary Objective:-**_
Capturing & ingesting extensive streams of real-time data, serving as a pivotal bridge between data producers and consumers.


### Key Design Considerations I've made:-

_**a) How do we inject the Data?**_ 

  I've leveraged Kinesis Producer Library for constructing our Data Producers. 
  Quick Breakdown:-
  
  </br>
  

| **Feature** | **Description** |
|-------------|-----------------|
| **Aggregates multiple records into a single PUT request** | Reduces operational overhead & **improves throughput**. |
| **Handles requests asynchronously** | **Decouples** the Data Production Logic from Stream Interaction, ensuring steady data production pace, unaffected by latency in Stream Interactions. |
| **Implements graceful Error Handling** | Defines criteria for retry attempts upon failure, enhancing **resilience and reliability**. |
| **Compresses data** to reduce transmitted data volume | Optimizes **bandwidth usage** and reduces costs. |
| **Collects Metrics** on Data production & Stream Interaction | - Metrics related to **data volume** for capacity estimation. - Stream Interaction metrics, like **latency**, provide insights into the data ingestion process's performance and health. |
 
  </br>

_**b) What about the Capacity Mode?**_

I've opted for the **On-demand Capacity Mode** for Kinesis Data Streams due to the _unpredictable and variable nature_ of my data stream's throughput requirements. 

With this mode, the capacity of the data stream **scales automatically** based on the incoming data volume, ensuring that I **don't need to predefine or manage shard capacities**. This flexibility is crucial for **accommodating sudden spikes** in data ingestion rates or **adjusting to changing application demands**.
  </br>

## The Producer Codebase - In a Gist
In our project, the **Kinesis Producer** is developed in Java, tailored to handle **NYC Taxi Telemetry Data**. 

The code begins by parsing data from a CSV file into Trip Objects, then configures the Kinesis Producer with parameters like **Record Buffer Time** and **Maximum Connections**.

We've incorporated **parallelism and concurrency** by deploying an **ExecutorService** with a fixed thread pool size, optimizing scalability and throughput. The integration of **CompletableFuture** with ExecutorService facilitates a fully **non-blocking asynchronous processing**, enhancing the efficiency of task management across threads.

And finally, **graceful Error handling** is achieved through the output of shard IDs for success and logging of failures, ensuring a **robust Data Ingestion Layer**.
</br>

## Strategy I've leveraged for Effective Thread Management

**The Pain-Point:-** 

Submitting a task to the _ExecutorService_ is asynchronous. However, upon submitting the task, it returns a _Future_ object immediately, which helps in tracking the status and retrieving the result at a later point.
_**Future.get() forces the calling thread to wait. this makes the solution only partially asynchronous. Not recommended**_

**Our Solution:-**

_ExecutorService_ + _CompletableFuture_

I've used a combination of both, since CompletableFuture provides non-blocking methods like thenApply() and thenCombine() to handle asynchronous task results. These execute post the completion of Future. My entire workflow is now fully asynchronous. It allows chaining and managing tasks efficiently without blocking the calling thread.

</br>

## The Data Transformation Layer
Here, I'd be using _**Kinesis Data Firehose**_, in conjuction with _**AWS Glue**_.

### Why Firehose + Glue? 

&#8594; We'd be using KDF for capturing and loading streaming data reliably into Data Stores / Analytical Tools (In our case, S3 would be our Data Store).
It's fully managed, and scales automatically to match the throughput of incoming data. However, tt can help with _minimal _processing

&#8594; Rationale behind using Glue:- As a central Metadata Repository through Data Catalog. The Schema Definitions it stores, enhances querying capabilities in Athena. **Athena can use the Schema Information from the Data Catalog for querying data stored in S3.**
(I've shared the Table Definition above, Firehose references this definition in Glue)
</br>

### Key Design Considerations I've made:-
</br>

**__a) Data Format Transformation:-__** 

&#8594; In the scope of our project, **Kinesis Data Firehose** has been leveraged for both data delivery into S3 and preliminary data transformation. A key aspect of this is **conversion from JSON to Parquet format**. Couple of Reasons here- **a) Significantly reduces Storage Costs**. **b) Parquet's columnar structure** allows for more efficient data querying in Athena.
 
  </br>
  
**__b) Buffer Interval Optimisation:-__**

&#8594; I've opted to **_maximize the Buffer Interval time_** for data delivery into **Amazon S3**. 
**Rationale behind this:-** By allowing Data to accumulate in large batches before delivery, we're **reducing the number of PUT requests to S3**, thereby reducing transaction costs. This also results in **improvising the throughput** through batching and subsequent storage. Something around **300-600 seconds** would be a good number to start with.

&#8594; Buffer Size has been maximised, Costs would be lowered, but at the cost of a **higher latency**. 

&#8594; Cranking up the Buffer Interval to **900 seconds** (max possible) would be a relative choice. ***We need to strike balance between the **timely availability of data versus the operational costs** incurred.****
 
  </br>
  
**__c) S3 Compression and Encryption:-__**

&#8594; I've utilized **Snappy compression** for source records, which leads to faster transmission and cost savings in storage. I'm prioritising **high speed over a higher compression ratio**.
 
&#8594; **Encryption** is implemented through **AWS-owned keys** for security and confidentiality of data as it moves through the Firehose stream, particularly crucial when converting data formats like JSON to Parquet.

**Preliminary Transformation through Lambda:-**


















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






    

