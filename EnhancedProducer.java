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

    private static final String STREAM_NAME = "input-stream";
    private static final String REGION = "ap-northeast-1";
    private static final int MAX_RETRIES = 3;  // Define the maximum number of retries

    public static void main(String[] args) {
        File tripCsv = new File("data/taxi-trips.csv");
        List<Trip> trips = TripReader.readFile(tripCsv);

        KinesisProducerConfiguration config = new KinesisProducerConfiguration()
                .setRecordMaxBufferedTime(3000)
                .setMaxConnections(1)
                .setRequestTimeout(60000)
                .setRegion(REGION)
                .setRecordTtl(30000);

        int coreCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(coreCount * 2);

        try (KinesisProducer producer = new KinesisProducer(config)) {
            trips.forEach(trip -> CompletableFuture.runAsync(() -> sendRecord(producer, trip, 0, 100), executor));

            executor.shutdown();
            producer.flushSync();
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Executor service did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static void sendRecord(KinesisProducer producer, Trip trip, int attemptCount, long backoff) {
        try {
            ByteBuffer data = ByteBuffer.wrap(new JSONObject(trip).toString().getBytes("UTF-8"));
            UserRecordResult result = producer.addUserRecord(STREAM_NAME, trip.getId(), data).get();

            if (result.isSuccessful()) {
                System.out.println("Record posted to shard " + result.getShardId());
            } else if (isRecoverableError(result) && attemptCount < MAX_RETRIES) {
                System.err.println("Attempting to retry due to recoverable error... Attempt: " + attemptCount);
                TimeUnit.MILLISECONDS.sleep(backoff);
                sendRecord(producer, trip, attemptCount + 1, backoff * 2);  // Exponential backoff
            } else {
                System.err.println("Failed to send record after " + MAX_RETRIES + " attempts or due to non-recoverable error");
            }
        } catch (Exception e) {
            if (isRecoverable(e.getMessage()) && attemptCount < MAX_RETRIES) {
                System.err.println("Error during send, retrying... Error: " + e.getMessage() + " Attempt: " + attemptCount);
                try {
                    TimeUnit.MILLISECONDS.sleep(backoff);
                    sendRecord(producer, trip, attemptCount + 1, backoff * 2);  // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } else {
                System.err.println("Failed to send record after " + MAX_RETRIES + " attempts. Final Error: " + e.getMessage());
            }
        }
    }

    private static boolean isRecoverableError(UserRecordResult result) {
        // Check attempts to determine if the error is recoverable
        return result.getAttempts().stream()
                     .anyMatch(attempt -> isRecoverable(attempt.getErrorCode()));
    }

    private static boolean isRecoverable(String errorCode) {
        // Define logic to determine if the error is recoverable
        return errorCode.equals("ProvisionedThroughputExceededException") || errorCode.equals("ServiceUnavailable");
    }
}
