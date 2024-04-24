# Real-Time Streaming Analytics pipeline with Kinesis, Flink and OpenSearch

Core idea:-    

**We've integrated the primary phases -->  data ingestion, processing, storage & visualisation --> into a single cohesive pipeline**.    
and have utilised **Kinesis, Lambda, Glue & OpenSearch.**

</br>

Our point of emphasis:-   

➡️ We've **optimised for non-functional aspects &rarr; scalability and performance throughout.**

</br>


## Project Workflow 

![Real-Time Streaming Analytics with Kinesis (1)](https://github.com/TanishkaMarrott/Real-Time-Streaming-Analytics-with-Kinesis-Flink-and-OpenSearch/assets/78227704/99edb176-7c2f-485d-bae0-b32629942201)


</br>

## Specifics into the data ingestion Layer

**We've utilised Kinesis Data Streams** for capturing and storing real-time streaming data    

> Please make sure to check out this file --> `EnhancedProducer.java`


</br>

       
## What were our design considerations, while setting up the ingestion pipeline?

### A --> Opted for the on-demand capacity mode for KDS
           
Our data stream must scale automatically whenever there're variations in the workload.

> **We do NOT need to manually handle shard capacities, It'll automatically scale based on the influx of data** 👍

</br>

### B --> We had to improvise on our thread management for amping up the throughput 

#### Our original approach - Wherein we've used only `ExecutorService`:-

_Point 1 :-_ Whenever we're submitting tasks to the  `ExecutorService`, they operate asynchronously.     

_Point 2:-_ It immediately returns the `future` object.                                     
(That's something we use for monitoring the task's status & retrieving results later. )

</br>

> **Potential Red Flag** 🚩:- **`Future.get()` forces the calling thread to wait, until the task completes(till it retrieves the result).** 

</br>

### How did we overcome this challenge?

We had to quickly tranform our approach. It was very crucial to have a fully asynchronuos workflow for sending data to Kinesis :

**--> _Integrated `CompletableFuture` + `ExecutorService`**_**
            
PLEASE NOTE:- `ExecutorService` will only be responsible for managing the thread pool --> This only takes care of the concurrency aspect                  
**We've now combined `CompletableFuture`,  means we're not blocking any threads, we can perform other operations without waiting for task completion**

</br>

> What did we achieve ?
>     
> **My entire workflow is now fully asynchronous. ↪️ Operational Efficiency because we've now improved throughput**

</br>

### C --> 

A couple of reasons here:-

--> **Ours is more of a hybrid workload, It's a mix of CPU-Bound and I/O bound threads.** (It involves both computations as well as sending data to Kinesis)
In such a scenario, **I'll advise to go with a factor of 2 (2 * the number of available cores)**

#### Why did we go with such a heuristic? (2 * the number of CPU Cores)

</br>

_Simple Answer:-_
**A balanced resource utilisation** 💡
</br>



1 -->  **We're actively engaging all the CPU cores, without overwhelming the system. Each CPU would have two threads to work on, the CPU-bound, and the I/O Bound .**
Once the I/O bound threads wait for the operations to complete, the cpu could then proceed with the computational operations.

2 --> We're cognizant of the resources we're using --> There should neither be underutilisation or over-allocation.

</br>

> So, **irrespective of our environments, our application can quickly adapt to machines, making our application responsive and scalable from the get-go**
>
> This is **one of my strategy I often use whenever we're trying to optimize the software architecture itself to make it way more resource efficient plus scalable 👍.**


3 --> **We had to save on the infra-costs as well**, We're working on the cloud, wherein we'd be charged based on the number of running threads. **We do not want too many threads competing for CPU Time --> (We do not want too much context-switching)** Neither do we want too less threads means we aren't performant enough

➡️ Resource efficient + Performance optimised 👍

</br>

### D --> We've implemented a retry + progressive backoff mechanism 


> 1 --> **We wanted to implement some sort of error handling** mechanisms. 
>
> Point 1 --> It **helps me with the application's availability**. We'll be confident that **despite of temporary setbacks, our application will be well-equipped to run reliably** without any significant downtime
>
> Point 2 --> 

**What did we achieve ? Strong availability + reliability** ✅

</br>

Strategic retries => 1 -> We'll re-attempt an operation multiple times, in case we're facing any transient errors. (That's particularly due to congestion issues / rate limits, timeout)    
      a) We should have a retry policy --> What kind of criteria demands for a retry    
      ➡️ The type of the error? Is it recoverable
      ➡️ The number of attempts we've made - Or the max number of retry attempts

Exponential backoffs => 



### The Producer Workflow

**_We'll first create the Kinesis producer configuration:_**  
This is where we'll specify the parameters like timeout, maxConnections, etc.

⬇️

We will then initialize a Kinesis producer instance with the said configurations.

⬇️

Extract the data from the telemetry CSV we've provided.

⬇️

Each row in the CSV will then be converted into a `Trip` object.

⬇️

We'll then set up an Executor Service.  
Helps us in sending data concurrently through multiple threads, improving throughput.

⬇️

Serializing our trip objects:  
Meaning we'll convert them into JSON and send to a ByteBuffer.

⬇️

We will then send to our stream asynchronously using CompletableFuture.

⬇️

Check if our submission was successful and log shard ID or error, as may be the case.

⬇️

We'll shut down the Executor Service and Kinesis Producer gracefully,  
while ensuring that all our tasks are completed without abrupt termination.

⬇️

End

---

This format provides a straightforward and detailed step-by-step guide through your data processing pipeline, ensuring clarity and facilitating easy tracking of each stage in the process.

--

## Data Transformation Layer for this architecture

Service we've utilised :- **Kinesis Data Firehose + Glue**

</br>

### Why did we use Firehose & Glue? 

</br>

&#8594; We'd be using **KDF for capturing and loading streaming data** reliably into S3

> _KDF can help with only minimal processing_

**_Rationale behind using Glue:-_**

As a central Metadata Repository through Data Catalog. The Schema Definitions it stores, enhances querying capabilities in Athena. **_Athena can use the Schema Information from the Data Catalog for querying data stored in S3._**
(I've shared the Table Definition above, Firehose references this definition in Glue)

</br>

## Lambda? For enriching & transforming the data...

&#8594; Designed to processes streaming data, focusing on data transformation and standardisation. Sets up logging for monitoring, **_converts pickupDate and dropoffDate fields to ISO 8601 format._** Having decoded the records from base-64, it **_inserts the source 'NYCTAXI' column._**
Function has been designed to handle errors, generating responses for each processed record, and manages batch processing as well.

</br>

## Design decisions we've made in the transformation layer

### _A -->**Converting the source record format:-**_

 We've used **Kinesis Data Firehose** for  data delivery into S3 & some initial data transformation.

 #### Why did we convert the format from JSON to Parquet? 
 
 A couple of reasons here:-
 
 **--> It helps us reduce storage costs significantly**.  
 
 **--> We were looking for an efficient kind of query mechanism for Athena**. And **Parquet's columnar structure** works very well.

</br>
  
### _B --> Optimising the buffer size & interval:-_

**We had to maximize the buffer interval time for data delivery into S3.**

</br>

**_Rationale behind this:-_**  
By allowing Data to accumulate in large batches before delivery, we're **_reducing the number of PUT requests to S3_**, thereby reducing transaction costs. This also results in **_improvising the throughput_** through batching and subsequent storage. Something around **_300-600 seconds_** would be a good number to start with.

Buffer Size has been maximised, Costs would be lowered, but **_at the cost of a higher latency_**. 

Cranking up the Buffer Interval to **_900 seconds_** (max possible) would be a relative choice.  
***Point to Note:-- We need to strike balance between the **timely availability of data versus the operational costs** incurred.****

</br>

### _Snappy Compression 'n' Encryption for S3 -_

&#8594; I've utilized **_Snappy compression_** for source records, which leads to faster transmission and cost savings in storage. I'm prioritising **_high speed over a higher compression ratio*_*.
 
&#8594; **_Encryption_** is implemented through **_AWS-owned keys_** for security and confidentiality of data as it moves through the Firehose stream, particularly crucial when converting data formats like JSON to Parquet.

</br>

## ***Workflow #2:- Stream Processing & Visualisation**

The **key service we've used here is Kinesis Data Analytics (KDA)**

</br>

⚙️ This is **Workflow #2**     

As we've mentioned, data is ingested through KDS in the form of JSON Blobs. 

We'll use a **Flink Application** deployed on  **Kinesis Data Analytics**. 

</br>

> **I used Flink over SQL.** Why? Because Flink excels at **extracting real-time insights from streaming data**.
>    
>So when my equation is = **Huge Volumes + Huge Velocity**, the answer has to be Flink    
> Also, **whenever we're encountered with eventful processing**, some complex computations, Flink wins over SQL

</br>

### Why did we incorporate OpenSearch alongside Flink?

Flink is awesome for _real-time data processing_

> ➡️ **this means it'll help us in performing some complex computations, _as data flows through the system_**

However, once we're done with processing, **OpenSearch will be our search and analytics engine                                                 
--> It helps us in _actually extracting useful insights from the processed data + some data visualisation capabilities_**

</br>

## How does the workflow actually look like?

 **We've defined a Kinesis Connector for Flink** to read from the Stream            
   &nbsp;    **↓**            
**And an OpenSearch Connector** to write processed data to OpenSearch            
   &nbsp;     **↓**                        
**We've then created the taxi_trips table in Flink** and **then linked it to the Kinesis stream** --> _virtual data mapping_                        
   &nbsp;     ↓            
**Our trip_statistics table in OpenSearch will store aggregated data** --> trip counts and average duration            
   &nbsp;     ↓                        
**We'll then execute some analytical queries** --> Insights into some critical metrics                                    
   &nbsp;     ↓            
Finally, **some data aggregation & visualization with summarized data**            


</br>

## Wrapping it Up

Thank you so much for accompanying me on my journey.

I'll quickly summarise all that we've done:-

### **Workflow - 1 :- Data Ingestion to Storage**

   Ingested data via Kinesis Data Streams     
         ⬇      
   Transferred to S3 via Firehose    
         ⬇        
    Managed Schema in Glue       
         ⬇      
   Enriched & Standardised data via Lambda     
         ⬇    
   Stored in S3

 </br>
 
### **Workflow 2:- Stream Processing and Visualization**    

Flink Application on KDA Studio for real-time processing    
                ⬇     
Aggregating the data     
                ⬇    
S3 as Durable Data Store   
                ⬇   
Visualising the data with OpenSearch     



## Acknowledgements & Attributions

Really appreciate AWS Workshops for their resources and support in this project's development.
 [AWS Workshops](https://www.aws.training/learningobject/curriculum?id=20685).








    

