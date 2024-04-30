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

#### Approach I - When I used _solely_ ExecutorService



#### What exactly was the lacuna here?

 So, the ExecutorService we've been using here does enable concurrent execution. --> It allows multiple threads to run in parallel.  

 If we look at the methods that're provided by ExecutorService ex. submit() & invokeAll() deal with Future objects 
(Using Future.get() makes the submitting thread to wait/block, until we're through with our computation.)

 </br>
 
> **Red Flag** 🚩:-
>
>  It only manages the submission of these tasks asynchronously.
> The retrieval of results --> that's when we use future.get(), forces the calling thread to wait --> It's a BLOCKING operation
>
> => A partially asynchronous workflow 

</br>

### How did we overcome this challenge then?

We had to quickly transform our approach.      

📌 **We anyhow wanted to get a fully asynchronuos workflow for sending data to Kinesis**. 

 </br>
 
> We could not compromise on the efficiency / responsiveness of the application, as it was crucial for maintaining high levels of throughput we were looking at.

</br>

**_Differentiator_** => Integrated `CompletableFuture` with `ExecutorService`

</br>   

> Okay, so what I mean here, is that CombineFuture extends our concept of Future here, CombineFuture does support dependent operations --> like the actions that trigger upon future's completion.  But without blocking the thread (example, thenApply())

</br>   

I'll use `thenApply()` here --> It'll enable us to transform the result without blocking the main thread.                   

</br>

> I can now chain tasks, and manage them sequentially. My entire workflow is now fully asynchronous. =  ↪️ Operational efficient because we've now improved throughput 👍 👍

</br>

### C &rarr; A dynamically sized thread pool 

</br>

> _Why did we go with such a heuristic? Why was the factor --> 2 * Number of CPU cores? Why was it not a static thread pool?_  I'll answer these questions here

</br>

A couple of reasons here:-

Reason 1 🔀 **Ours is a hybrid workload. It's a mix of CPU-bound & I/O-bound threads**                 
(Check out the code -- it involves both computations plus sending data to Kinesis)

***In such a scenario, **I'll advise to go with a factor of 2***

            ***( 2 * the number of available cores)***         

**--> We're actively engaging all the cores, without overwhelming the system.**              
So, **each CPU would now have two threads to work on:- the CPU-bound, and the I/O Bound ones.** Once the I/O bound threads wait for the operations to complete, the cpu could then proceed with the computational operations. 👍

</br>

Reason 2 🔀 Irrespective of the environment we've deployed the application on, **our application can quickly adapt to a variety of machines/ VMs**, making our application responsive and scalable from the get-go

</br>

Reason 3 🔀 **We had to save on the infra-costs as well**, We're working on the cloud, wherein we'd be charged based on the number of running threads. **We do not want too many threads competing for CPU Time --> (We do not want too much context-switching)** Neither do we want too less threads means we aren't performant enough. 

We're cognizant of the resources we're using --> **There should neither be under-utilisation or over-allocation.** ✔️ 🏁


 #### = Resource efficiency + Performance Optimisation 👍✅

</br>

--

### D &rarr; We've implemented a _Retry + Progressive backoff mechanism_ 

1 &rarr; We were adamant on implementing some sort of error handling mechanisms:-

</br>

> _Point 1_ --> Something that assures us that **despite of temporary setbacks or transient errors, our application will still be well-equipped to run reliably**

</br>

  ➡️ **That'll help us maintain a good level of Operational stability + Service continuity 👍**

</br>

>
> </br>
>
> _Point 2_ --> **Plus a backoff mechanism in place, that progressively increases the time interval between two successive retries**                                 
> We're basically achieving 3 things here:-  
>
>     A - We're minimizing system workload, we aren't overwhelming our resources                                
>     B - We're making our application stable --> We'll limit the number of retries allowed, so, even in face of errors, our application would operate reliably (we do not want it to enter into a loop of infinite failures) 
>     C - We end up improvising the data consistency and processing, handling errors GRACEFULLY ➡️ We're giving errors more time to resolve, by increasing the time interval between two subsequent retries

</br>

More so, **it's a predictable system behaviour, We have a well-defined retry policy with exponential backoff.**

#### = Strong Availability + Reliability ✅ 👍

</br>

## Data transformation layer for Workflow #1:-

Services we've utilised :- **Kinesis Data Firehose + Glue**

</br>

### What was our rationale behind using Firehose plus Glue?
                      
**--> We've used glue as a central metadata repository** through data catalog. 
➡️ Athena can then use this schema information for quering data in s3 

</br>

> Had we used firehose by itself, it would just aid in loading streaming data into S3. &rarr; **The definitions we've stored in glue _actually_ enhance Athena's querying capabilities**

 </br>
 
 _I've shared the Table Definition above, firehose references this definition in glue_

</br>

## Data Transformations using lambda

### Considerations before processing our data using lambda

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

</br>


## Non-functional design decisions for the transformation layer

</br>

> I'll go component wise here, as to how we'd optimize on the non-functional aspects of this layer

</br>

### Kinesis data Firehose - Enhancements from a design standpoint

### --> We'd optimize on the configurations of buffer size and buffer interval

</br>

> This is quite use-case specific. And involves a latency - throughput tradeoff. However, will be diving deep for better clarity

</br>

If we opt for a larger buffer size, it'll delay our delivery rates into S3 (; slightly)            
But would be more cost-effective; we're cutting down on the transmission costs👍

</br>

### **What exactly was my rationale behind this?**

🔆 **When we allow data to accumulate in batches before delivery, we're reducing the number of PUT requests to S3** 

</br>

This means --> 
                **Buffer Size ∝ Latency in delivery  ∝ 1 / costs we'll incur**

</br>

🔆 **We're also reducing on my "per-operation overhead".** (There'll always be some operational overhead, like disk writes, network calls while data transmission...)

</br>

>  When we're performing batching, I'm effectively "spreading" this fixed overhead across multiple data items 🙂 👍 

</br>

🔆 Network bandwidth. (We're not wasting unnecessary CPU time on handling I/O operations). Also, we;re cognizant of the API Limits 👍

 </br>
            
> I might also crank up the buffer interval to 900 second for absolutely low costs. But I'd appreciate the tradeoff, and 360 seconds looks like a good start for me.

 </br>
 
 #### Why did we convert the format from JSON to Parquet? 
 
 A couple of reasons here:-
 
 **--> It helps us reduce storage costs significantly**.  
 
 **--> We were looking for an efficient kind of query mechanism for Athena**. And **Parquet's columnar structure** works very well.

</br>
  




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

As we've mentioned, Streaming data is ingested through Kinesis Data Streams, This ingested data would then be processed by a Flink SQL Application deployed on KDA Studio --- that's Kinesis Data Analytics. Post processing, we've sent our processed data to OpenSearch, for visualisation and analytics of historic data.

</br>

> **I used Flink over SQL.** Why? Because Flink excels at **extracting real-time insights from streaming data**.
>    
>So when my equation is = **Huge Volumes + Huge Velocity**, the answer has to be Flink    
> Also, **whenever we're encountered with eventful processing**, some complex computations, Flink wins over SQL

</br>


**As I've mentioned above, all the complex data processing / heavy data transformations have been offloaded to Flink, It's best for running such kind of stateful computations (They require the state to be retained across operations, hence stateful) on the data as its being streamed in.**

So, our complex trans

</br>

### Why did we incorporate OpenSearch alongside Flink?

Flink is awesome for _real-time data processing_

> ➡️ **This means it'll help us in performing some complex computations, _as data flows through the system_**

However, once we're done with processing, **OpenSearch will be our search and analytics engine FOR HISTORIC DATA                                                 
--> It helps us in _ACTUALLY EXTRACTING USEFUL INSIGHTS from the processed data + some data visualisation capabilities_** 👍

</br>

## Design Considerations from a non-functional standpoint to optimize the second worflow:-

I'll go component-wise, starting from KDA






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








    

