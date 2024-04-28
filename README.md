# Real-Time Streaming Analytics pipeline with Kinesis, Flink and OpenSearch

Core idea:-    

**We've integrated the primary phases -->  data ingestion, processing, storage & visualisation --> into a single cohesive pipeline**.    
and have utilised **Kinesis, Lambda, Glue & OpenSearch.**

</br>

Our point of emphasis:-   

➡️ We've **optimised for non-functional aspects &rarr; scalability and performance throughout.**

</br>


## Project Workflow 

<img src="https://github.com/TanishkaMarrott/Real-Time-Streaming-Analytics-with-Kinesis-Flink-and-OpenSearch/assets/78227704/e18c449f-3511-402f-a324-8d7db755ce38" alt="Diagram for Streaming Analytics" width="75%">


</br>

## Specifics into the data ingestion Layer

**We've utilised Kinesis Data Streams** for capturing and storing real-time streaming data    

> Please make sure to check out this file --> `EnhancedProducer.java`

</br>

### How does the Kinesis producer workflow look like? The setup of the ingestion part of the pipeline

We'll first quickly initialize the producer's configuration                                            
Need to have some crucial parameters like timeouts, maxconnections in place, This will help optimize on kinesis' performance                                            
↓                      
With all the necessary configurations specified, we'll instantiate the producer instance.                                             
↓                                                               
It'll read data from the telemetry CSV file --> standardising the format, making it suitable for streaming                                  
↓                      
We've then set up ExecutorService to manage multiple threads. Increased concurrency has a direct correlation with increased throughput                                      
↓                      
I've discussed this below in much detail. We've utilised CompletableFuture for making my data ingestion process fully asynchronous to the Kinesis stream.      
    ↓                         
For data integrity/ reliability of the submissions, we'll check the responses ➡️ log successful shard IDs / capture error messages for the failed ones                      
↓                      
As a non-functional enhancement, we'll have some graceful shutdown mechanisms in place, We'll ensure all our tasks are completed by shutting down the Executor Service and Kinesis Producer properly ▶️ Cost optimisation by freeing up resources we don't need + Preventing inadvertent data loss                        
↓                      
Will continue to monitor metrics and then optimize on / fine-tune the paramaters/ configurations we've set to balance cost, performance plus reliability.                      

</br>

## What sort of design decisions did we make for the ingestion layer?

### A &rarr; We opted for the _On-demand capacity mode_ for KDS:-
           
--> 📍 Our data stream _must scale automatically_ whenever there're variations in the workload.

</br>

> **We do not need to manually handle shard capacities, it'll automatically scale based on the influx of streaming data** 👍

</br>

### B &rarr; We then optimized on the thread management mechanism

#### Approach I 

When we used only ExecutorService

#### What exactly was the lacuna here?

 So, the ExecutorService we've been using here does enable concurrent execution. --> It allows multiple threads to run in parallel.  

 If we look at the methods that're provided by ExecutorService ex. submit() & invokeAll() deal with Future objects 
(Using Future.get() makes the submitting thread to wait/block, until we're through with our computation.)
 
 
 
> **Red Flag** 🚩:-
>
>  It only manages the submission of these tasks asynchronously.
>
> The retrieval of results --> that's when we use future.get(), forces the calling thread to wait --> It's a BLOCKING operation
>
> => A partially asynchronous workflow 

</br>

### How did we overcome this challenge then?

We had to quickly transform our approach.      


******📌 **We anyhow had to get a fully asynchronuos workflow for sending data to Kinesis** ******

**_Differentiator_** => Integrated `CompletableFuture` with `ExecutorService`

How exactly?

`ExecutorService` 👇
 Will only be responsible for managing the thread pool       

****--> This ONLY takes care of the concurrency aspect****     

 `CompletableFuture` object we've used **will now have some non-blocking methods like thenApply() and thenCombine()**.                          
`thenApply()` --> will be used to transform the result when it gets available without blcoking the calling thread.                
`thenCombine()` --> will enable us to combine results of asynchronous operations, without having to keep the thread idle / blocked, while waiting for the result's retrieval.

</br>

> I can now chain tasks, and manage them sequentially. My entire workflow is now fully asynchronous. =  ↪️ Operational efficient because we've now improved throughput 👍 👍

</br>

### C &rarr; A dynamically sized thread pool 

</br>

> #### Why did we go with such a heuristic? (2 * the number of CPU Cores) for the thread pool? Why was it not a static thread pool?

</br>

A couple of reasons here:-

**Ours is more of a hybrid workload. ➡️ It's a mix of CPU-Bound and I/O bound threads.** 
(It involves both computations as well as sending data to Kinesis)
In such a scenario, **I'll advise to go with a factor of 2 (2 * the number of available cores)**

1 &rarr; **We're actively engaging all the CPU cores, without overwhelming the system. Each CPU would have two threads to work on, the CPU-bound, and the I/O Bound .**
Once the I/O bound threads wait for the operations to complete, the cpu could then proceed with the computational operations. 👍

2 &rarr;  We're cognizant of the resources we're using --> **There should neither be under-utilisation or over-allocation.** ✔️ 🏁

</br>

> So, **irrespective of our environments, our application can quickly adapt to machines, making our application responsive and scalable from the get-go**

</br>

3 --> **We had to save on the infra-costs as well**, We're working on the cloud, wherein we'd be charged based on the number of running threads. **We do not want too many threads competing for CPU Time --> (We do not want too much context-switching)** Neither do we want too less threads means we aren't performant enough

➡️ Resource efficient + Performance optimised 👍

</br>

### D --> We've implemented a Retry + Progressive backoff mechanism 


1 &rarr; We were adamant on implementing some sort of error handling mechanisms:-

 _Point 1_ --> Something that assures us that **despite of temporary setbacks or transient errors, our application will still be well-equipped to run reliably**
>
  ➡️ **That'll help us maintain a good level of Operational stability + Service continuity 👍**
>
> </br>
>
> _Point 2_ --> **Plus a backoff mechanism in place, that progressively increases the time interval between two successive retries**                                 
> We're basically achieving 3 things here:-  
>
>     A - We're minimizing system workload, we aren't overwhelming our resources                                
>     B - We're making our application stable --> We'll limit the number of retries allowed, so, even in face of errors, our application would operate reliably (we do not want it to enter into a loop of infinite failures) 
>     C - We end up improvising the data consistency and processing, handling errors GRACEFULLY ➡️ We're giving errors more time to resolve, by increasing the time interval between two subsequent retries


More so, **it's a predictable system behaviour, We have a well-defined retry policy with exponential backoff.**

### = Strong Availability + Reliability ✅ 👍

</br>

## Data transformation layer for this architecture

Services we've utilised :- **Kinesis Data Firehose + Glue**

### What was our rationale behind using firehose plus glue?
                      
**We've used glue as a central metadata repository** through data catalog.               
➡️ **Athena can then use this schema information for quering data in s3**. 

</br>

> Had we used firehose by itself, it would just aid in loading streaming data into S3. &rarr; **The definitions we've stored in glue _actually_ enhance Athena's querying capabilities** 
> I've shared the Table Definition above, firehose references this definition in glue

</br>

## Data Transformations using lambda

### Considerations before processing in lambda

1 => **We had to weigh in the impact on downstream systems**.                                  
This means the processing logic on Lambda shouldn't be too heavy, such that it starts affecting our solution's overall latency. - We don't want bottlenecks.                     
2 => **Plus if our volume of data and frequency of data processing requests is too high, lambda might start getting strained**, especially if we're using a lot of lambda's memory **or getting too close to the 15 minute cap on Lambda's execution.** 🚩         

3 => This also means **we're bumping up our memory allocation and compute costs.** For complex data transformations, and heavy data analytics, **we've got other alternatives that work out better given the use case and viability**                                            

</br>

**Solution 💡:-**                
So, we decided to have light-weight data processing and validation for lambda, offloading complex data processing logic/transformations to Flink in KDA (More on this subsequently):-

</br>

> **Our intent here was to keep the data processing and transformation logic very light-weight.** This would actually align with lambda's stateless model. All of these transformations do not require the state to be retained across operations. None of these demand a stateful context.

</br>

_Pls check out the CF template for details around this data transformation lambda_

--

#### Quickly recapitulating what this lambda would do:-

Lambda is first triggered with a batch of records coming in from KDS -->                 
Then, it'll deserialise the data -->                 
We're then performing some initial data validation / cleansing (Validation checks to remove corrupt/irrelevant data points,) -->                    
Timestamp standardisation to ISO Format -->                    
Plus, some lightweight data enrichment, Adding metadata 'source' to for traceability downstream -->     
logging record's submission, capturing the record ID + metrics/errors -->       
Will then assemble the records to be sent to firehose



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

### _Snappy Compression  Encryption for S3 -_

&#8594; I've utilized **_Snappy compression_** for source records

> Why did we compress the records? Its equal to faster transmission plus cost savings in storage. I'm prioritising **_high speed over a higher compression ratio*_*.
 
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

> ➡️ **This means it'll help us in performing some complex computations, _as data flows through the system_**

However, once we're done with processing, **OpenSearch will be our search and analytics engine                                                 
--> It helps us in _actually extracting useful insights from the processed data + some data visualisation capabilities_** 👍

</br>

## Flink's real-time processing + OpenSearch's data Aggregation and Search 

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

Thank you so much for accompanying me on my journey!

I'll quickly summarise all that we've built today:-

### **Workflow - 1 :- Data Ingestion to Storage**

   Ingested data via Kinesis Data Streams     
         ⬇      
   Transferred to S3 via Firehose    
         ⬇        
    Managed Schema in Glue       
         ⬇      
   Enriched plus standardised data via Lambda     
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








    

