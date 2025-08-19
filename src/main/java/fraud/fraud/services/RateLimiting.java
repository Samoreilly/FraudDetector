package fraud.fraud.services;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimiting {

    public final ValueOperations<String, Integer> val;
    public static final int MAX_REQUESTS = 10;
    private final Duration WINDOW = Duration.ofMinutes(1);

    public RateLimiting(@Qualifier("integerRedisTemplate") RedisTemplate<String, Integer> redisTemplate){
        this.val = redisTemplate.opsForValue();
    }

    public boolean isAllowed(String key){
        Integer currentCount = val.get(key);
        boolean initialized = Boolean.TRUE.equals(val.setIfAbsent(key, 1,WINDOW));//if key not present, returns true

        if(initialized){
            return true;
        }

        Long count = val.increment(key);
        if (count != null && count >= MAX_REQUESTS) {
            return false;
        }
        return count < MAX_REQUESTS;
    }

}

//plan
//take in user ip
//check if number exceeds max requests

