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

        Integer count = counter.get(userId);//get users count

        if (count == null) {
            counter.set(userId, 1, WINDOW);
            return true;
        }else if(count >= 50){
            notificationService.sendNotification(transactionRequest, "Too much transactions made in the last hour");
            return false;
        }else{
            counter.increment(userId);
            return true;
        }
    }
}
