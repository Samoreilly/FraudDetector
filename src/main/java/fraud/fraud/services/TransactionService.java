package fraud.fraud.services;

import fraud.fraud.DTO.TransactionRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TransactionService {

    @Cacheable(value = "transactionCache")
    public Map<String, String> transactionPipeline(Map<String, String> userData){

        System.out.println("Cached");
        return userData;
    }
}
