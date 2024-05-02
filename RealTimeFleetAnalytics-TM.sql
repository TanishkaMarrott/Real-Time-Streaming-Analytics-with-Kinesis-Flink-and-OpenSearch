-- Dropping existing tables if they exist
%flink.ssql(type=update)
DROP TABLE IF EXISTS taxi_trips;

-- Creating a new 'taxi_trips' table with data streamed from Kinesis
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
    source STRING
) WITH (
    'connector' = 'kinesis',
    'stream' = 'input-stream',
    'aws.region' = 'us-west-2',
    'scan.stream.initpos' = 'LATEST',
    'format' = 'json'
);

%flink.ssql(type=update)
DROP TABLE IF EXISTS trip_statistics;

-- Creating 'trip_statistics' table in OpenSearch to store aggregated trip data
CREATE TABLE trip_statistics (
    trip_count BIGINT,
    passenger_count INTEGER,
    total_trip_duration INTEGER,
    average_trip_duration DOUBLE,
    average_trip_distance DOUBLE,
    peak_hour INTEGER,
    trips_during_peak INTEGER,
    masked_average_pickup_longitude DOUBLE,
    masked_average_pickup_latitude DOUBLE,
    anomaly_score DOUBLE
) WITH (
    'connector' = 'elasticsearch-7',
    'hosts' = 'https://search-os-domain-yourdomain.us-west-2.es.amazonaws.com:443',
    'index' = 'trip_statistics',
    'username' = 'admin',
    'password' = 'YourPassword'
);

-- Inserting aggregated data into 'trip_statistics' with anomaly detection and data masking
%flink.ssql(type=update)
INSERT INTO trip_statistics
SELECT 
    COUNT(1) AS trip_count,
    SUM(passengerCount) AS passenger_count,
    SUM(tripDuration) AS total_trip_duration,
    AVG(tripDuration) AS average_trip_duration,
    AVG(gcDistance) AS average_trip_distance,
    ROUND(AVG(pickupLongitude) + RAND() * 0.01, 5) AS masked_average_pickup_longitude,
    ROUND(AVG(pickupLatitude) + RAND() * 0.01, 5) AS masked_average_pickup_latitude,
    CALCULATE_ANOMALY_SCORE(tripDuration, passengerCount) AS anomaly_score,
    (SELECT HOUR(pickupDate) FROM taxi_trips GROUP BY HOUR(pickupDate) ORDER BY COUNT(*) DESC LIMIT 1) AS peak_hour,
    (SELECT COUNT(*) FROM taxi_trips WHERE HOUR(pickupDate) = peak_hour) AS trips_during_peak
FROM taxi_trips
WHERE pickupLatitude <> 0 AND pickupLongitude <> 0 AND dropoffLatitude <> 0 AND dropoffLongitude <> 0;

-- Define a user-defined function for anomaly detection (placeholder, needs actual UDF implementation)
CREATE FUNCTION CALCULATE_ANOMALY_SCORE AS 'com.yourdomain.flinksqlexample.AnomalyScoreCalculator';


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
