-- Creating 'taxi_trips' Table with Kinesis Stream as Source

%flink.ssql(type=update)
drop table if exists taxi_trips;
CREATE TABLE taxi_trips (
    id STRING,
    vendorId INTEGER, 
    pickupDate STRING, 
    dropoffDate STRING, 
    passengerCount INTEGER, 
    pickupLongitude DOUBLE, 
    pickupLatitude DOUBLE, 
    dropoffLongitude DOUBLE, 
    dropoffLatitude DOUBLE, 
    storeAndFwdFlag STRING, 
    gcDistance DOUBLE, 
    tripDuration INTEGER, 
    googleDistance INTEGER,
    googleDuration INTEGER
) WITH (
    'connector' = 'kinesis',
    'stream' = 'input-stream',
    'aws.region' = 'us-west-2',
    'scan.stream.initpos' = 'LATEST',
    'format' = 'json'
)PARTITIONED BY (
    `year` STRING,
    `month` STRING,
    `day` STRING,
    `hour` STRING
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe'
STORED AS INPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat'
OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
LOCATION 's3://[BUCKET-NAME]/nyctaxitrips/';

-- Creating 'trip_statistics' Table in OpenSearch

%flink.ssql(type=update)
drop TABLE if exists trip_statistics;
CREATE TABLE trip_statistics (
    trip_count BIGINT,
    passenger_count INTEGER,
    total_trip_duration INTEGER,
    average_trip_duration DOUBLE,
    average_trip_distance DOUBLE,
    peak_hour INTEGER,
    trips_during_peak INTEGER
) WITH (
    'connector' = 'elasticsearch-7',
    'hosts' = 'https://search-os-domain-yourdomain.us-west-2.es.amazonaws.com:443',
    'index' = 'trip_statistics',
    'username' = 'admin',
    'password' = 'YourPassword'
);

-- Inserting Aggregated Data into 'trip_statistics'

%flink.ssql(type=update)
INSERT INTO trip_statistics
SELECT 
    COUNT(1) as trip_count, 
    SUM(passengerCount) as passenger_count, 
    SUM(tripDuration) as total_trip_duration,
    AVG(tripDuration) as average_trip_duration, 
    AVG(gcDistance) as average_trip_distance,
    (
        SELECT HOUR(pickupDate) 
        FROM taxi_trips 
        GROUP BY HOUR(pickupDate)
        ORDER BY COUNT(*) DESC
        LIMIT 1
    ) as peak_hour,
    (
        SELECT COUNT(*) 
        FROM taxi_trips 
        GROUP BY HOUR(pickupDate)
        ORDER BY COUNT(*) DESC
        LIMIT 1
    ) as trips_during_peak
FROM taxi_trips
WHERE pickupLatitude <> 0 AND pickupLongitude <> 0 AND dropoffLatitude <> 0 AND dropoffLongitude <> 0;
