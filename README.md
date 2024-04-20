# Real-Time Streaming Analytics pipeline with Kinesis, Flink and OpenSearch

Core idea:-    

**We've integrated data ingestion, processing, storage and data visualisation --> into a single cohesive pipeline**.    
and have utilised **Kinesis, Lambda, Glue & OpenSearch.**

</br>

Our point of emphasis:-   

➡️ **We've optimised for non-functional aspects &rarr; scalability and performance throughout.**

</br>

## Project Workflow 

![Real-Time Streaming Analytics with Kinesis (1)](https://github.com/TanishkaMarrott/Real-Time-Streaming-Analytics-with-Kinesis-Flink-and-OpenSearch/assets/78227704/99edb176-7c2f-485d-bae0-b32629942201)

---

## Specifics into the Data Ingestion Layer

**We've utilised Kinesis Data Streams** for capturing and storing real-time streaming data**

</br>

Sharing the kinesis producer's workflow, we've used please check the file `Enhanced_producer.java`

## The KPL Workflow

            We'll first create the kinesis producer configuration               
          - This is where we'll specify the parameters like timeout, maxConnections, etc                 
                                ⬇️     
           We will then initialise a kinesis producer instance with the said configurations    
                                ⬇️     
            Extracts the data from the telemetry csv we've provided     
                                ⬇️     
            Each row in the CSV will then be converted into a `Trip` object    
                                ⬇️    
          
</br>

## What design considerations have we opted for?

</br>

>  **We had to ensure we've got a fairly good level of scalability, fault tolerance and reliability**

</br>

### A --> Opted for the _On-demand capacity mode_ for KDS**
           
We wanted our data stream to scale automatically when there're variations in the workload.

> **We do not need to manually handle shard capacities, It'll automatically scale based on the influx of data** 👍

</br>

### B --> Improvised on thread management for increasing our solution's throughput 


#### Our original approach - When we used only `ExecutorService`:-

Point 1 :- Whenever we're submitting tasks to the  `ExecutorService`, they operate asynchronously.     

Point 2:- It immediately returns the `future` object.                                     
That's something we use for monitoring the task's status & retrieving results later. 

</br>

> Potential Red Flag 🚩:- **`Future.get()` forces the calling thread to wait, until the task completes and it receives the result.** 

</br>

### How did we overcome this challenge?

#### Combining **`ExecutorService` + `CompletableFuture`** 💡


What did we achieve ?      
**My entire workflow is now fully asynchronous. => Operational Efficiency => Improved throughput**

</br>

---

## Data Transformation Layer for this architecture

**We'd be using Kinesis Data Firehose + Glue for this layer**

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

## Design Considerations for the transformation layer

### _A -->**Converting the source record format:-**_

 In the scope of our project, **Kinesis Data Firehose** has been leveraged for both data delivery into S3 and preliminary data transformation.
 **conversion from JSON to Parquet format**. 
 
 Couple of Reasons here-  
 **a) Significantly reduces Storage Costs**.  
 **b) Parquet's columnar structure** allows for more efficient data querying in Athena.

</br>
  
### _Optimising the Buffer Size & Interval:-_

 I've opted to **_maximize the Buffer Interval time_** for data delivery into **_Amazon S3_**. 
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

Plus OpenSearch for some search and analytics ::

</br>

### Why did we incorporate OpenSearch alongside Flink?

Flink has amazing cpabailities ::

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


## Acknowledgements

Really appreciate AWS Workshops for their resources and support in this project's development.
 [AWS Workshops](https://www.aws.training/learningobject/curriculum?id=20685).








    

