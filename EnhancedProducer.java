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
            for (Trip trip : trips) {
                CompletableFuture.runAsync(() -> sendRecord(producer, trip), executor);
            }

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

    private static void logFailure(UserRecordResult result) {
        result.getAttempts().forEach(attempt -> {
            if (isRecoverable(attempt.getErrorCode())) {
                System.err.println("Recoverable error, retrying: " + attempt.getErrorCode());
                // Optionally implement retry logic here or log for manual retry
            } else {
                System.err.println("Non-recoverable error: " + attempt.getErrorCode() + ": " + attempt.getErrorMessage());
            }
        });
    }

    private static boolean isRecoverable(String errorCode) {
        // Define logic to determine if the error is recoverable
        return errorCode.equals("ProvisionedThroughputExceededException") || errorCode.equals("ServiceUnavailable");
    }
}
