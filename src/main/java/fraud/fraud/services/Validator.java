package fraud.fraud.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.Notifications.NotificationService;
import fraud.fraud.services.RedisEncryptionService;
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
    private final NotificationService notificationService;
    private final RedisEncryptionService redisEncryptionService;

    public Validator(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper, KafkaTemplate<String,  TransactionRequest> kafkaTemplate, ValidateTransactions validateTransactions, NotificationService notificationService, RedisEncryptionService redisEncryptionService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.validateTransactions = validateTransactions;
        this.notificationService = notificationService;
        this.redisEncryptionService = redisEncryptionService;
    }
    public boolean checkTimestamps(TransactionRequest userData, List<TransactionRequest> validateTimes){ // restructered - moved from TransactionService.java to get away from 1 god class(transaction service)
        notificationService.sendNotification(userData, "Validating your transaction for potential fraud");
        if(validateTimes == null || validateTimes.isEmpty()){
            return true;
        }
        Duration duration = Duration.between(validateTimes.getFirst().getTime(),userData.getTime());
        return duration.getSeconds() >= 0;
    }
    public boolean checkTransactionByLocation(TransactionRequest userData) throws Exception {                             //...
        Object transaction = redisTemplate.opsForList().getFirst(userData.getId());
        if(transaction == null){

            return false;
        }
        notificationService.sendNotification(userData, "Validating latitude and longitude");
        TransactionRequest trans;
        if (transaction instanceof String) {
            trans = redisEncryptionService.decryptFromRedis((String) transaction);
        } else {
            trans = objectMapper.convertValue(transaction, TransactionRequest.class);
        }

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
            notificationService.sendNotification(userData, "Suspicious transaction");
            System.out.println("Fraud detected - impossible travel speed: " + requiredSpeedKmh + " km/h");
            System.out.println("FAILLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLEDDDDDDDDDDDDDDDDD");
            return false;
        }

        System.out.println("Location validation passed - speed: " + requiredSpeedKmh + " km/h");
        return true;
    }
}
