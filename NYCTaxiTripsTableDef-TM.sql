-- Creating an external table named 'nyctaxitrips' to store NYC Taxi trip data

CREATE EXTERNAL TABLE `nyctaxitrips`
(
    `id` string, -- Unique identifier for each trip - Will abbreviate this as UID 
    `vendorId` int, -- Vendor UID
    `pickupDate` string, -- Start Date/Time
    `dropoffDate` string, -- End Date/Time
    `passengerCount` int, -- Number of passengers during the trip
    `pickupLongitude` double, -- Longitude Coords where the trip started
    `pickupLatitude` double, -- Latitudee Coords where the trip started
    `dropoffLongitude` double, -- Longitude where the trip ended
    `dropoffLatitude` double, -- Latitude where the trip ended
    `storeAndFwdFlag` string, -- Was the trip data stored before sending to the server?
    `gcDistance` double, -- Great circle distance of the trip
    `tripDuration` int, -- Trip Duration
    `googleDistance` int, -- Distance of the trip (calculated by Google Maps)
    `googleDuration` int, -- Duration of the trip
    `source` string -- Source of the data entry - This is the CSV in my case
)
PARTITIONED BY ( -- Partitioning the data --> for  query performance
    `year` string, 
    `month` string, 
    `day` string, 
    `hour` string 
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe' -- Using Parquet format for efficient storage and querying
STORED AS INPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat' 
OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
LOCATION 's3://<<BUCKET-NAME>>/nyctaxitrips/' -- Location in S3 where the data is stored
