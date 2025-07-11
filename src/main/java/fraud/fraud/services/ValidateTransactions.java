package fraud.fraud.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import fraud.fraud.DTO.TransactionRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class ValidateTransactions {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public ValidateTransactions(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper, KafkaTemplate<String,  TransactionRequest> kafkaTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Double averageTransaction(TransactionRequest transactionRequest) {

        List<Object> transactions = redisTemplate.opsForList().range(transactionRequest.getId(),0,-1);
        int val = 0;
        int total = 0;
        if(transactions.isEmpty() || transactions.getFirst() == null)return null;
        for(Object transaction : transactions) {
            try {
                TransactionRequest tr = objectMapper.convertValue(transaction, TransactionRequest.class);
                val += Integer.parseInt(tr.getData());
                total++;
            }catch(NumberFormatException e) {
                continue;
            }
        }
        if(total == 0)return null;
        double average = (double) val / total;

        transactionRequest.setResult("Users average transaction = "+ String.format("%.2f", average));
        kafkaTemplate.send("out-transactions", transactionRequest.getId(), transactionRequest);
        return average;
    }
    public boolean checkTimestamps(TransactionRequest userData, List<TransactionRequest> validateTimes){ // restructered - moved from TransactionService.java to get away from 1 god class(transaction service)
        userData.setResult("Validating your transaction for potential fraud");
        kafkaTemplate.send("out-transactions", userData.getId(), userData);
        if(validateTimes == null || validateTimes.isEmpty()){
            return true;
        }
        Duration duration = Duration.between(validateTimes.getFirst().getTime(),userData.getTime());
        return duration.getSeconds() >= 10;
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
        double lat2Rad = Math.toRadians(endLat);
        double deltaLatRad = Math.toRadians(endLat - startLat);
        double deltaLonRad = Math.toRadians(endLong - startLong);

        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +//haversine formula to calculate distance between two points on earth in metres
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }
}
