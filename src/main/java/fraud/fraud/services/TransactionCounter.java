package fraud.fraud.services;

import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.Notifications.NotificationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.time.Duration;
import java.util.List;

@Service
public class TransactionCounter {

    private final ValueOperations<String, Integer> counter;
    private final NotificationService notificationService;

    private static final long MAX_TRANSACTIONS = 50L;
    private final Duration WINDOW = Duration.ofMinutes(60);

    public TransactionCounter(@Qualifier("integerRedisTemplate") RedisTemplate<String, Integer> redisTemplate, NotificationService notificationService) {
        this.counter = redisTemplate.opsForValue();//returns users count
        this.notificationService = notificationService;
    }

    public boolean counter(TransactionRequest transactionRequest) {
        String userId = transactionRequest.getId();

        String key = "counter:userid:" + userId;
        boolean initialized = Boolean.TRUE.equals(counter.setIfAbsent(key, 1, WINDOW));//if key not present, returns true

        if(initialized){
            return true;
        }
        Long count = counter.increment(key);
        if (count != null && count >= MAX_TRANSACTIONS) {
            notificationService.sendNotification(transactionRequest, "Too many transactions in the last hour");
            return false;
        }

        return true;

    }
}
