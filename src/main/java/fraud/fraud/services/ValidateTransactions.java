package fraud.fraud.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.Notifications.NotificationService;
import fraud.fraud.services.RedisEncryptionService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ValidateTransactions {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationService notificationService;
    private final RedisEncryptionService redisEncryptionService;

    public ValidateTransactions(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper, KafkaTemplate<String,  TransactionRequest> kafkaTemplate, NotificationService notificationService, RedisEncryptionService redisEncryptionService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.notificationService = notificationService;
        this.redisEncryptionService = redisEncryptionService;
    }


    public Double averageTransaction(TransactionRequest transactionRequest) throws Exception {

        List<Object> transactions = redisTemplate.opsForList().range(transactionRequest.getId(), 0 , -1);
        int val = 0;
        int total = 0;

        if(transactions.isEmpty() || transactions.getFirst() == null)return null;

        for(Object transaction : transactions) {
            try {
                TransactionRequest tr;
                // Check if data is encrypted or of type TransactionRequest
                if (transaction instanceof String) {
                    tr = redisEncryptionService.decryptFromRedis((String) transaction);
                } else {
                    //convert to transaction request object
                    tr = objectMapper.convertValue(transaction, TransactionRequest.class);
                }
                val += Integer.parseInt(tr.getData());
                total++;
            }catch(NumberFormatException e) {
                continue;
            }
        }
        if(total == 0)return null;
        double average = (double) val / total;

        transactionRequest.setResult("Users average transaction = "+ String.format("%.2f", average));
        notificationService.sendNotification(transactionRequest, transactionRequest.getResult());
        return average;
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
