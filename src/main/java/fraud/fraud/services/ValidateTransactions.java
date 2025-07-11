package fraud.fraud.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import fraud.fraud.DTO.TransactionRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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
}
