// Importing necessary classes and packages required for Kinesis Producer Functionality.
// These are essentially the classes within the AWS SDK for Java, for putting records into KDS,
// or interacting with the stream.
import com.amazonaws.services.kinesis.producer.Attempt;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.amazonaws.services.kinesis.producer.UserRecordResult;

import org.json.JSONObject;
import trip.Trip;
import trip.TripReader;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//Kinesis Producer code with Error Analysis Functionality
public class B_Producer_withErrorAnalysis {

    // initialised the Stream Name and its region
    public static String streamName = "input-stream";
    public static String region = "ap-northeast-1";
    
    // Entry point of the program, the main method
    public static void main(String[] args) throws Exception {

        // Reads Telemetry Data from the CSV passed as an argument
        File tripCsv = new File("data/taxi-trips.csv");
        // Parses the CSV File and retrieves a list of Trip Objects
        List<Trip> trips = TripReader.readFile(tripCsv);

        // Configuring the Kinesis Producer, fine-tuning parameters associated with the producer
        KinesisProducerConfiguration config = new KinesisProducerConfiguration()
                .setRecordMaxBufferedTime(3000)
                .setMaxConnections(1)
                .setRequestTimeout(60000)
                .setRegion(region)
                .setRecordTtl(30000);
        
        // Using the try-with-resources statement
        // to instantiate the KinesisProducer ensures that it will be closed & cleaned up properly,
        // for proper resource management
        try (KinesisProducer kinesis = new KinesisProducer(config)) {

            //Configurable thread pool size, ExecutorService to manage a pool of threads.
            ExecutorService executor = Executors.newFixedThreadPool(5); 

            for (Trip trip : trips) {

                // Send data to the Kinesis stream in a non-blocking, asynchronous way
                CompletableFuture.runAsync(() -> {
                    try {
                        ByteBuffer data = ByteBuffer.wrap(new JSONObject(trip).toString().getBytes("UTF-8"));
                        UserRecordResult result = kinesis.addUserRecord(streamName, trip.getId(), data).get();
                        
                        // Prints Shard IDs for successful puts, and attempts for failed ones.
                        if (result.isSuccessful()) {
                            System.out.println("Put record into shard " + result.getShardId());
                        } else {
                            for (Attempt attempt : result.getAttempts()) {
                                System.out.println(attempt);
                            }
                        }
                    } catch (Exception e) {
                        
                        e.printStackTrace();
                    }
                }, executor);
            }
            
            // Shutdown mechanism for the ExecutorService,
            // including a wait for tasks to complete and handling InterruptedException
            executor.shutdown();
            try {
                // Wait for 60 seconds for existing tasks to terminate
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {

                    // Cancel currently executing tasks, if interavl exceeds the 60 second threshold
                    executor.shutdownNow(); 

                    // Wait a while for tasks to respond to being cancelled
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                        System.err.println("Executor service did not terminate");
                }
            } catch (InterruptedException ie) {
                // Cancel if current thread also interrupted
                executor.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        } // KinesisProducer is auto-closed here
    }
}
