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

**We've utilised Kinesis Data Streams** for the ingestion layer --> C**aptures and stores real-time streaming data**

</br>

Sharing the kinesis producer's workflow, we've used:-

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
          


## What design considerations have we opted for?
</br>

>  **We had to ensure we've got a fairly good level of scalability, fault tolerance and reliability**

</br>

### A --> Opted for the _On-demand capacity mode_ for KDS**
           
We wanted our data stream to scale automatically when there're variations in the workload.

> **We do not need to manually handle shard capacities, It'll automatically scale based on the influx of data** 👍


## B --> Improvised on thread management for increasing our solution's throughput 


### Our original approach - When we used only `ExecutorService`:-

Point 1 :-Tasks submitted to the  `ExecutorService` operate asynchronously.                
Point 2:- It immediately returns the `future` object. That's something we use for monitoring the task's status & retrieving results later. 


> Potential Red Flag 🚩:- **`Future.get()` forces the calling thread to wait, until the task completes.** 

</br>

### How did we overcome this challenge?

#### Combining **`ExecutorService` + `CompletableFuture`** 💡


What did we achieve ?      
**My entire workflow is now fully asynchronous. => Operational Efficiency => Improved throughput**

</br>

---

## Data Transformation Layer for this architecture

Here, I'd be using _**Kinesis Data Firehose**_, in conjuction with _**AWS Glue**_. 

</br>

### _Why did I use Firehose & Glue?_ 

</br>

&#8594; We'd be using **_KDF for capturing and loading streaming data_** reliably into Data Stores / Analytical Tools (In our case, S3 would be our Data Store).

> _KDF can help with only minimal processing_

**_Rationale behind using Glue:-_**  

As a central Metadata Repository through Data Catalog. The Schema Definitions it stores, enhances querying capabilities in Athena. **_Athena can use the Schema Information from the Data Catalog for querying data stored in S3._**
(I've shared the Table Definition above, Firehose references this definition in Glue)

## Lambda? For enriching & transforming the data...

&#8594; Designed to processes streaming data, focusing on data transformation and standardisation. Sets up logging for monitoring, **_converts pickupDate and dropoffDate fields to ISO 8601 format._** Having decoded the records from base-64, it **_inserts the source 'NYCTAXI' column._**
Function has been designed to handle errors, generating responses for each processed record, and manages batch processing as well.

## Design Considerations yet again

### _**Converting the source record format:-**_

 In the scope of our project, **Kinesis Data Firehose** has been leveraged for both data delivery into S3 and preliminary data transformation.
 **conversion from JSON to Parquet format**. 
 
 Couple of Reasons here-  
 **a) Significantly reduces Storage Costs**.  
 **b) Parquet's columnar structure** allows for more efficient data querying in Athena.
  
### _Optimising the Buffer Size & Interval:-_

 I've opted to **_maximize the Buffer Interval time_** for data delivery into **_Amazon S3_**. 

**_Rationale behind this:-_**  
By allowing Data to accumulate in large batches before delivery, we're **_reducing the number of PUT requests to S3_**, thereby reducing transaction costs. This also results in **_improvising the throughput_** through batching and subsequent storage. Something around **_300-600 seconds_** would be a good number to start with.

Buffer Size has been maximised, Costs would be lowered, but **_at the cost of a higher latency_**. 

Cranking up the Buffer Interval to **_900 seconds_** (max possible) would be a relative choice.  
***Point to Note:-- We need to strike balance between the **timely availability of data versus the operational costs** incurred.****

### _Snappy Compression 'n' Encryption for S3 -_

&#8594; I've utilized **_Snappy compression_** for source records, which leads to faster transmission and cost savings in storage. I'm prioritising **_high speed over a higher compression ratio*_*.
 
&#8594; **_Encryption_** is implemented through **_AWS-owned keys_** for security and confidentiality of data as it moves through the Firehose stream, particularly crucial when converting data formats like JSON to Parquet.

</br>

## **Stream Processing & Visualisation**

The **key service we've used here is Kinesis Data Analytics (KDA)**

</br>

### **How does this workflow look like?**

⚙️ This is **Workflow #2**     

As we've mentioned, data is ingested through KDS in the form of JSON Blobs. 

We'll use a **Flink Application** deployed on  **Kinesis Data Analytics**. 

</br>

> **I used Flink over SQL.** Why? Because Flink excels at **extracting real-time insights from streaming data**.
>    
>So when my equation is = **Huge Volumes + Huge Velocity**, the answer has to be Flink    
> Also, **whenever we're encountered with eventful processing**, some complex computations, Flink wins over SQL

</br>

Plus OpenSearch for some serach and analytics
OpenSearch is a really powerful **Visualiser**, it's designed to work on **Streaming data**, and the high level of **scalability** that comes with it. It's used for **searching, storing and analysing** Streaming data, Log Data. It's a Search and Analytics Engine, synonymous to **Historical Data Analysis**, and Visualisation.

## _The Flink Application Codebase_

-  We've defined the Kinesis Connector, that enables the Flink App to read code from the Stream, and the OpenSearch Connector that enables writing processed storage in OpenSearch Connector, for storage and analysis
  
- Created **_`_taxi_trips_`_** table, which is linked to the Kinesis stream. This is not a real table --- This is virtually created in the Flink Ebvironment, It maps to the structure of KDS, facilitating its processing
  
- **_The `trip_statistics` table in OpenSearch._** This table would basically set up to store aggregated data in OS, like trip counts, and averageduration
  
- A **_series of analytical queries to extract insights_** such as average trip duration, distance, peak travel times, and frequent locations.
  
- Have performed Future Analysis and Visualisation through an **_aggregation query to insert summarized data_** into the `trip_statistics` table.

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








    

