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
        TransactionRequest trans = objectMapper.convertValue(transaction, TransactionRequest.class);

        Double prevLatitude = trans.getLatitude();
        Double prevLongitude = trans.getLongitude();
        Double latitude = userData.getLatitude();
        Double longitude = userData.getLongitude();
        double distance;

        if(prevLatitude != null && prevLongitude != null && latitude != null && longitude != null){
            distance = calculateDistance(latitude,longitude,prevLatitude,prevLongitude);
            Duration duration = Duration.between(LocalDateTime.now(),trans.getTime());
            if(distance > 200 && duration.getSeconds() < 1000 ){
                return false;
            }else{
                return true;
            }
        }else{
            return true; // this will be changed, this is not a solution
        }
    }
    public double calculateDistance(double startLat, double startLong, double endLat, double endLong) {
        double EARTH_RADIUS = 6378137.0;
        double dLat = Math.toRadians((endLat - startLat));
        double dLong = Math.toRadians((endLong - startLong));

        startLat = Math.toRadians(startLat);
        endLat = Math.toRadians(endLat);

        double a = haversine(dLat) + Math.cos(startLat) * Math.cos(endLat) * haversine(dLong);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }
    double haversine(double val) {
        return Math.pow(Math.sin(val / 2), 2);
    }
    @KafkaListener(topics = "transactions", groupId = "in-transactions", containerFactory = "factory")
    public void transactionPipeline(@Payload TransactionRequest userData){
        userData.setResult("Processing your transaction");
        kafkaTemplate.send("out-transactions", userData.getId(), userData);
        System.out.println(userData);
        //getTransactions(userData);//retrieve users transactions as a list
        List<TransactionRequest> transactions = getTransactions(userData);

        if(!checkTimestamps(userData, transactions)){
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
