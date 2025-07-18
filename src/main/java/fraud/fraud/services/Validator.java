package fraud.fraud.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.interfaces.Handler;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class Validator{
    //these to seperate method

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ValidateTransactions validateTransactions;

    public Validator(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper, KafkaTemplate<String,  TransactionRequest> kafkaTemplate, ValidateTransactions validateTransactions) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.validateTransactions = validateTransactions;
    }
    public boolean checkTimestamps(TransactionRequest userData, List<TransactionRequest> validateTimes){ // restructered - moved from TransactionService.java to get away from 1 god class(transaction service)
        userData.setResult("Validating your transaction for potential fraud");
        kafkaTemplate.send("out-transactions", userData.getId(), userData);
        if(validateTimes == null || validateTimes.isEmpty()){
            return true;
        }
        Duration duration = Duration.between(validateTimes.getFirst().getTime(),userData.getTime());
        return duration.getSeconds() >= 0;
    }
    public boolean checkTransactionByLocation(TransactionRequest userData){                             //...
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

        double distanceInMeters = validateTransactions.calculateDistance(latitude, longitude,
                prevLatitude, prevLongitude);

        Duration duration = Duration.between(trans.getTime(), userData.getTime());
        long timeDiffSeconds = Math.abs(duration.getSeconds());

        double distanceInKm = distanceInMeters / 1000.0;

        double requiredSpeedKmh = (distanceInKm / timeDiffSeconds) * 3600;

        if(requiredSpeedKmh > 500.0 && timeDiffSeconds > 0){
            userData.setResult("Suspicious location change detected");
            kafkaTemplate.send("out-transactions", userData.getId(), userData);
            System.out.println("Fraud detected - impossible travel speed: " + requiredSpeedKmh + " km/h");
            System.out.println("FAILLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLEDDDDDDDDDDDDDDDDD");
            return false;
        }

        System.out.println("Location validation passed - speed: " + requiredSpeedKmh + " km/h");
        return true;
    }
}
