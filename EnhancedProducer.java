import com.amazonaws.services.kinesis.producer.Attempt;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.amazonaws.services.kinesis.producer.UserRecordResult;

import org.json.JSONObject;
import trip.Trip;
import trip.TripReader;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EnhancedProducer {

    // Our constants for the stream name and AWS region
    private static final String STREAM_NAME = "input-stream";
    private static final String REGION = "ap-northeast-1";

    public static void main(String[] args) {

        // Setting up the source CSV file containing trip data
        File tripCsv = new File("data/taxi-trips.csv");

        // Parsing the CSV file to create a list of Trip objects
        List<Trip> trips = TripReader.readFile(tripCsv);

        // Configuring the Kinesis Producer with various settings --> non functional aspects
        KinesisProducerConfiguration config = new KinesisProducerConfiguration()
                .setRecordMaxBufferedTime(3000)
                .setMaxConnections(1)
                .setRequestTimeout(60000)
                .setRegion(REGION)
                .setRecordTtl(30000);

        // Initializing KinesisProducer and ExecutorService 
        try (KinesisProducer producer = new KinesisProducer(config);
             ExecutorService executor = Executors.newFixedThreadPool(5)) {

            // Sending each trip record to Kinesis
            for (Trip trip : trips) {
                CompletableFuture.runAsync(() -> sendRecord(producer, trip), executor);
            }

            // Shutting down the ExecutorService gracefully
            executor.shutdown();
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException e) {

            // Handling thread interruption
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Asynchronously sends a record to Kinesis Data Streams and logs the outcome.
     * @param producer The Kinesis producer
     * @param trip The trip data
     */
    private static void sendRecord(KinesisProducer producer, Trip trip) {
        try {

            // Serializing Trip data into JSON and wrapping it into a ByteBuffer
            ByteBuffer data = ByteBuffer.wrap(new JSONObject(trip).toString().getBytes("UTF-8"));
            
            // Submitting the record to Kinesis and wait for the result
            UserRecordResult result = producer.addUserRecord(STREAM_NAME, trip.getId(), data).get();
            
            // Logging the result of the record submission
            if (result.isSuccessful()) {
                System.out.println("Record posted to shard " + result.getShardId());
            } else {
                logFailure(result);
            }
        } catch (Exception e) {

            // Logging and handling exceptions related to record submission
            System.err.println("Failed to send record: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Logging details of failed attempts to put records into the stream.
     * @param result --> The result of the record-put operation
     */
    private static void logFailure(UserRecordResult result) {
        // Iterate through all attempts and log each failed attempt's details
        result.getAttempts().forEach(attempt -> 
            System.err.println("Failed attempt: " + attempt.getErrorCode() + ": " + attempt.getErrorMessage()));
    }
}
