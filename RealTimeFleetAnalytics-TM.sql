-- Dropping existing tables if they exist and creating new tables with enhancements

%flink.ssql(type=update)
-- Remove the existing 'taxi_trips' table
DROP TABLE IF EXISTS taxi_trips;

-- Create 'taxi_trips' table with data streamed from Kinesis
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
    googleDuration INTEGER,
    fareAmount DOUBLE  -- Assuming fare amount is available for revenue calculations
) WITH (
    'connector' = 'kinesis',
    'stream' = 'input-stream',
    'aws.region' = 'us-west-2',
    'scan.stream.initpos' = 'LATEST',
    'format' = 'json'
);

%flink.ssql(type=update)
-- Remove the existing 'trip_statistics' table
DROP TABLE IF EXISTS trip_statistics;

-- Create 'trip_statistics' table in OpenSearch to store aggregated trip data
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

-- Aggregating and inserting data into 'trip_statistics'

%flink.ssql(type=update)
-- Insert aggregated data from 'taxi_trips' into 'trip_statistics'
INSERT INTO trip_statistics
SELECT 
    COUNT(1) AS trip_count, 
    SUM(passengerCount) AS passenger_count, 
    SUM(tripDuration) AS total_trip_duration,
    AVG(tripDuration) AS average_trip_duration, 
    AVG(gcDistance) AS average_trip_distance,
    (
        SELECT HOUR(pickupDate) 
        FROM taxi_trips 
        GROUP BY HOUR(pickupDate)
        ORDER BY COUNT(*) DESC
        LIMIT 1
    ) AS peak_hour,
    (
        SELECT COUNT(*) 
        FROM taxi_trips 
        WHERE HOUR(pickupDate) = peak_hour
    ) AS trips_during_peak
FROM taxi_trips
WHERE pickupLatitude <> 0 AND pickupLongitude <> 0 AND dropoffLatitude <> 0 AND dropoffLongitude <> 0;

-- Additional Functional Queries

%flink.ssql(type=update)
-- Calculate total revenue per vendor
SELECT vendorId, SUM(fareAmount) AS total_revenue
FROM taxi_trips
GROUP BY vendorId;

%flink.ssql(type=update)
-- Find top 10 most visited drop-off locations
SELECT dropoffLatitude, dropoffLongitude, COUNT(*) AS visit_count
FROM taxi_trips
GROUP BY dropoffLatitude, dropoffLongitude
ORDER BY visit_count DESC
LIMIT 10;

%flink.ssql(type=update)
-- Analyze average trip duration changes over time
SELECT DATE(pickupDate) AS trip_date, AVG(tripDuration) AS average_duration
FROM taxi_trips
GROUP BY DATE(pickupDate)
ORDER BY trip_date;

%flink.ssql(type=update)
-- Determine peak travel times by weekday and hour
SELECT DAYOFWEEK(pickupDate) AS weekday, HOUR(pickupDate) AS hour, COUNT(*) AS trip_count
FROM taxi_trips
GROUP BY DAYOFWEEK(pickupDate), HOUR(pickupDate)
ORDER BY trip_count DESC;

