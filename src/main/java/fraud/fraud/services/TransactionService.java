package fraud.fraud.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import fraud.fraud.DTO.TransactionRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class TransactionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SetupSse setupSse;
    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;

    public TransactionService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper, SetupSse setupSse, KafkaTemplate<String, TransactionRequest>  kafkaTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.setupSse = setupSse;
        this.kafkaTemplate = kafkaTemplate;
    }

    public boolean checkTimestamps(TransactionRequest userData, List<TransactionRequest> validateTimes){

        if(validateTimes == null || validateTimes.isEmpty()){
            return true;
        }
        Duration duration = Duration.between(validateTimes.getFirst().getTime(),userData.getTime());
        return duration.getSeconds() >= 5;
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
    @KafkaListener(topics = "transactions", groupId = "in-transactions", containerFactory = "factory")
    public void transactionPipeline(@Payload TransactionRequest userData){
        System.out.println(userData);
        //getTransactions(userData);//retrieve users transactions as a list
        List<TransactionRequest> transactions = getTransactions(userData);

        if(!checkTimestamps(userData, transactions)){
            System.out.println("sent too early");
            return;
        }
        saveTransaction(userData);//save transaction
        System.out.println("Cached");

    }
    public void saveTransaction(TransactionRequest userData){
        redisTemplate.opsForList().leftPush(userData.getId(), userData);
        userData.setResult("Successful");
        try {
            kafkaTemplate.send("out-transactions", userData);
            System.out.println("Transaction sent successfully");
        } catch (Exception e) {
            System.err.println("Failed to send transaction: " + e.getMessage());
        }
    }
}
