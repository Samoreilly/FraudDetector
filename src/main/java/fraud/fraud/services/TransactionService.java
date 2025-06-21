package fraud.fraud.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import fraud.fraud.DTO.TransactionRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public TransactionService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean checkTimestamps(TransactionRequest userData, List<TransactionRequest> validateTimes){

        if(validateTimes == null || validateTimes.isEmpty()){
            return true;
        }
        Duration duration = Duration.between(validateTimes.getFirst().getTime(),userData.getTime());
        return duration.getSeconds() >= 40;
    }

    public List<TransactionRequest> getTransactions(TransactionRequest userData){ // passes into transaction of type TransactionRequest
        String key = userData.getId();
        Long size = redisTemplate.opsForList().size(key);


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

    public Map<String, String> transactionPipeline(TransactionRequest userData){
        //getTransactions(userData);//retrieve users transactions as a list
        List<TransactionRequest> transactions = getTransactions(userData);

        if(!checkTimestamps(userData, transactions)){
            System.out.println("sent too early");
            return Map.of("Error", "Transaction sent too early");
        }
        saveTransaction(userData);//save transaction
        Map<String, String> result = new HashMap<>();
        result.put(userData.getId(), userData.getData());
        System.out.println("Cached");

        return result;
    }
    public void saveTransaction(TransactionRequest userData){
        redisTemplate.opsForList().leftPush(userData.getId(),userData);
    }
}
