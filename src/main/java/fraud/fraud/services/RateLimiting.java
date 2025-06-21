package fraud.fraud.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import fraud.fraud.DTO.TransactionRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class RateLimiting {

    public final ValueOperations<String, Integer> val;
    public static final int MAX_REQUESTS = 5;
    private final Duration WINDOW = Duration.ofMinutes(1);

    public RateLimiting(@Qualifier("integerRedisTemplate") RedisTemplate<String, Integer> redisTemplate) {
        this.val= redisTemplate.opsForValue();
    }

    public boolean isAllowed(String key){
        Integer currentCount = val.get(key);

        if(currentCount == null){
            val.set(key, 1,WINDOW);
            return true;
        }else if(currentCount >= MAX_REQUESTS){
            return false;
        }else{
            val.increment(key);
            return true;
        }
    }

}

//plan
//take in user ip
//check if number exceeds max requests

