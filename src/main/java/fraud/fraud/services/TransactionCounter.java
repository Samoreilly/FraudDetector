package fraud.fraud.services;

import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.Notifications.NotificationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

public class TransactionCounter {

    private final ValueOperations<String, Integer> counter;
    private final NotificationService notificationService;
    private final Duration WINDOW = Duration.ofMinutes(60);

    public TransactionCounter(@Qualifier("integerRedisTemplate") RedisTemplate<String, Integer> redisTemplate, NotificationService notificationService) {
        this.counter = redisTemplate.opsForValue();//returns users couont
        this.notificationService = notificationService;
    }

    public boolean counter(TransactionRequest transactionRequest) {
        String userId = transactionRequest.getId();

        Long count = counter.increment(userId);
        if (count == 1) {
            counter.getOperations().expire(userId,  WINDOW);
        }
        if(count >= 50){
            notificationService.sendNotification(transactionRequest,"Too many transactions in the last hour");
            return false;
        }
        return true;

    }
}
