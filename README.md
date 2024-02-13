<img width="957" alt="image" src="https://github.com/TanishkaMarrott/Streamlined-Real-Time-Data-Processing-with-Amazon-Kinesis/assets/78227704/14696afe-5ff8-4771-aa51-37c69dc2be2d"># Streamlined-Real-Time-Data-Processing-with-Amazon-Kinesis
----

## Setting the context
This Real-time Streaming pipeline integrates Data Processing with Data Ingestion, Transformation, Storage, Analysis, and Visualization, creating a robust end-to-end solution. Services leveraged include  Kinesis, Lambda, Glue, OpenSearch.

There's been a strong emphasis on Design Considerations that align with the overarching Architectural Design, I've prioritized scalability, fault tolerance, security, and performance optimization across all system layers.

---
## Project Workflow

### <ins>Data Ingestion - Specifics</ins>


_**AWS Service Utilised-**_ 
Kinesis Data Streams

_**Primary Objective-**_
Capturing & ingesting extensive streams of real-time data, serving as a pivotal bridge between data producers and consumers.

</br>


--> <ins>**_Key Design Considerations I've Made_**</ins> 

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


<ins>***Kinesis Producer Codebase - What does it actually do?***</ins>

- The Java program attached above is actually the Kinesis Producer Code, aka it publishes records to the Kinesis Data Stream. It imports the necessary libraries from the AWS SDK for Java, including the Kinesis Producer Library.
- It'll read and parse the CSV file comprising NYC Taxi Telemetry Data, & retrieve a list of Trip Objects
- Next, I've configured the Kinesis Producer with specific parameters, fine-tuned Configuration Settings - Record Buffer Time, Maximum Connections, Request Timeout, and Record TTL. And then created a Kinesis producer instance based on the provided configuration. (More on this subsequently)
- Iterates over the list of trips & converts each trip into JSON format, Wraps the JSON data into a ByteBuffer and asynchronously adds the user record to the specified Kinesis data stream.
- The request handling is asynchronous, and hence the code collects futures for each put operation (Since this is an asynchronous computation), 
  This means that the program continues to execute without waiting for each put operation to complete. Instead, it adds each put operation's future to a list for later analysis. This approach improves efficiency by allowing the program to continue processing data while concurrently handling the results of the put operations.

- _Here, future refers to a Java object, representing the result of an asynchronous computation. When you submit a task for execution asynchronously, you receive a Future object immediately, which you can use to track the status and retrieve the result of the computation later_

- Lastly, it prints shard IDs for successful puts and attempts for failures.

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






    

