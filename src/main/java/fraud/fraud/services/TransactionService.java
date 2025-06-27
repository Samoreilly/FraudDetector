package fraud.fraud.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import fraud.fraud.DTO.TransactionRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class TransactionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SetupSse setupSse;
    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;

    public TransactionService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper, SetupSse setupSse, KafkaTemplate<String, TransactionRequest>  kafkaTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.setupSse = setupSse;
        this.kafkaTemplate = kafkaTemplate;
    }

    public boolean checkTimestamps(TransactionRequest userData, List<TransactionRequest> validateTimes){
        userData.setResult("Validating your transaction for potential fraud");
        kafkaTemplate.send("out-transactions", userData.getId(), userData);
        if(validateTimes == null || validateTimes.isEmpty()){
            return true;
        }
        Duration duration = Duration.between(validateTimes.getFirst().getTime(),userData.getTime());
        return duration.getSeconds() >= 5;
    }

    public List<TransactionRequest> getTransactions(TransactionRequest userData){ // passes into transaction of type TransactionRequest
        String key = userData.getId();
        Long size = redisTemplate.opsForList().size(key);
        userData.setResult("Checking your previous transactions");
        kafkaTemplate.send("out-transactions", userData.getId(), userData);
        List<Object> transactions = redisTemplate.opsForList().range(userData.getId(),0,-1);//gets userdata by id by looking from start to end of list
        if(transactions == null){
            return null;
        }
        if(size == null || size == 0){
            redisTemplate.expire(userData.getId(), Duration.ofMinutes(10));//set time to live if doesnt exist
        }
        List<TransactionRequest> tx = new ArrayList<>();

        for(Object obj : transactions){
            TransactionRequest trans = objectMapper.convertValue(obj, TransactionRequest.class);

            tx.add(trans);

        }
        System.out.println("777 " + transactions);
        for (TransactionRequest transaction : tx) {

            System.out.println(transaction.getData());
        }

        return tx;
    }

    public boolean checkTransactionByLocation(TransactionRequest userData){
        Object transaction = redisTemplate.opsForList().getFirst(userData.getId());
        if(transaction == null){

            return false;
        }
        userData.setResult("Validating latitude and longitude");
        kafkaTemplate.send("out-transactions", userData.getId(), userData);
        TransactionRequest trans = objectMapper.convertValue(transaction, TransactionRequest.class);

        Double prevLatitude = trans.getLatitude();
        Double prevLongitude = trans.getLongitude();
        Double latitude = userData.getLatitude();
        Double longitude = userData.getLongitude();

        double distanceInMeters = calculateDistance(latitude, longitude,
                prevLatitude, prevLongitude);

        Duration duration = Duration.between(trans.getTime(), userData.getTime());
        long timeDiffSeconds = Math.abs(duration.getSeconds());

        double distanceInKm = distanceInMeters / 1000.0;

        double requiredSpeedKmh = (distanceInKm / timeDiffSeconds) * 3600;

        if(requiredSpeedKmh > 500.0 && timeDiffSeconds > 0){
            userData.setResult("Suspicious location change detected");
            kafkaTemplate.send("out-transactions", userData.getId(), userData);
            System.out.println("Fraud detected - impossible travel speed: " + requiredSpeedKmh + " km/h");
            return false;
        }

        System.out.println("Location validation passed - speed: " + requiredSpeedKmh + " km/h");
        return true;
    }
    public double calculateDistance(double startLat, double startLong, double endLat, double endLong) {
        double EARTH_RADIUS = 6378137.0;
        double lat1Rad = Math.toRadians(startLat);
        double lat2Rad = Math.toRadians(startLong);
        double deltaLatRad = Math.toRadians(endLat - startLat);
        double deltaLonRad = Math.toRadians(endLong - startLong);

        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +//haversine formula to calculate distance between two points on earth in metres
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c; // Distance in meters
    }
    @KafkaListener(topics = "transactions", groupId = "in-transactions", containerFactory = "factory")
    public void transactionPipeline(@Payload TransactionRequest userData){
        userData.setResult("Processing your transaction");
        kafkaTemplate.send("out-transactions", userData.getId(), userData);
        System.out.println(userData);
        //getTransactions(userData);//retrieve users transactions as a list
        List<TransactionRequest> transactions = getTransactions(userData);

        if(!checkTimestamps(userData, transactions) && checkTransactionByLocation(userData)){
            userData.setResult("Too many transactions.. retry again in 5 seconds");
            kafkaTemplate.send("out-transactions",userData.getId(), userData);
            System.out.println("sent too early");
            return;
        }
        saveTransaction(userData);//save transaction
        System.out.println("Cached");

    }
    public void saveTransaction(TransactionRequest userData){
        redisTemplate.opsForList().leftPush(userData.getId(), userData);
        userData.setResult("Caching your transaction");
        kafkaTemplate.send("out-transactions", userData.getId(), userData);

        try {
            userData.setResult("Successful");
            kafkaTemplate.send("out-transactions",userData.getId(), userData);
            System.out.println("Transaction sent successfully");
        } catch (Exception e) {
            System.err.println("Failed to send transaction: " + e.getMessage());
            userData.setResult("Error processing transaction");
            kafkaTemplate.send("out-transactions",userData.getId(), userData);
        }
    }
}
