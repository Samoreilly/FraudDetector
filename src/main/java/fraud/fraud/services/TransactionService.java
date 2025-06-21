package fraud.fraud.services;

import fraud.fraud.DTO.TransactionRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    private final RedisTemplate<String, TransactionRequest> redisTemplate;

    public TransactionService(RedisTemplate<String, TransactionRequest> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<TransactionRequest> getTransactions(TransactionRequest userData){
        List<TransactionRequest> transactions = redisTemplate.opsForList().range(userData.getId(),0,-1);
        if(transactions == null){
            return transactions;
        }
        System.out.println("777 " + transactions);
        for (TransactionRequest transaction : transactions) {
            System.out.println(transaction.getData());
        }
        return transactions;

    }

    public Map<String, String> transactionPipeline(TransactionRequest userData){
        saveTransaction(userData);
        getTransactions(userData);
        Map<String, String> result = new HashMap<>();
        result.put(userData.getId(), userData.getData());
        System.out.println("Cached");

        return result;
    }
    public void saveTransaction(TransactionRequest userData){
        redisTemplate.opsForList().leftPush(userData.getId(),userData);
    }
}
