package fraud.fraud.controllers;


import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.services.TransactionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class Transactions {

    private final TransactionService transactionService;

    public Transactions(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/tr")
    public Map<String, String> transaction(@RequestBody TransactionRequest transactionInfo){

        Map<String, String> userData = new HashMap<>();

        userData.put(transactionInfo.getId(), transactionInfo.getData());
        return transactionService.transactionPipeline(userData);
    }
}
