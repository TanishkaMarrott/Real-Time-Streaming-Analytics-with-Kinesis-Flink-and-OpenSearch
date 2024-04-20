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

/**
 * This class demonstrates the use of the AWS Kinesis Producer Library to send streaming data to Kinesis Data Streams.
 * It includes robust error handling and efficient thread management to ensure high throughput and reliability.
 */
public class EnhancedProducer {

    private static final String STREAM_NAME = "input-stream";
    private static final String REGION = "ap-northeast-1";

    public static void main(String[] args) {
        File tripCsv = new File("data/taxi-trips.csv");
        List<Trip> trips = TripReader.readFile(tripCsv);

        KinesisProducerConfiguration config = new KinesisProducerConfiguration()
                .setRecordMaxBufferedTime(3000)
                .setMaxConnections(1)
                .setRequestTimeout(60000)
                .setRegion(REGION)
                .setRecordTtl(30000);

        // Auto-closeable try block ensures proper cleanup of resources.
        try (KinesisProducer producer = new KinesisProducer(config);
             ExecutorService executor = Executors.newFixedThreadPool(5)) {

            for (Trip trip : trips) {
                CompletableFuture.runAsync(() -> sendRecord(producer, trip), executor);
            }

            executor.shutdown();
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException e) {
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
            ByteBuffer data = ByteBuffer.wrap(new JSONObject(trip).toString().getBytes("UTF-8"));
            UserRecordResult result = producer.addUserRecord(STREAM_NAME, trip.getId(), data).get();
            
            if (result.isSuccessful()) {
                System.out.println("Record posted to shard " + result.getShardId());
            } else {
                logFailure(result);
            }
        } catch (Exception e) {
            System.err.println("Failed to send record: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Logs details of failed attempts to put records into the stream.
     * @param result The result of the record put operation
     */
    private static void logFailure(UserRecordResult result) {
        result.getAttempts().forEach(attempt -> 
            System.err.println("Failed attempt: " + attempt.getErrorCode() + ": " + attempt.getErrorMessage()));
    }
}
