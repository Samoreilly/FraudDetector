package fraud.fraud.controllers;


import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.ErrorMessages;
import fraud.fraud.RateLimitExceededException;
import fraud.fraud.services.RateLimiting;
import fraud.fraud.services.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;
    private final RateLimiting rateLimiting;
    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;

    public TransactionController(TransactionService transactionService, RateLimiting rateLimiting, KafkaTemplate<String, TransactionRequest> kafkaTemplate) {
        this.transactionService = transactionService;
        this.rateLimiting = rateLimiting;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/tr")
    public ResponseEntity<String> transaction(@RequestBody TransactionRequest transactionInfo, HttpServletRequest request){
        String ip = getClientIp(request);
        String key = "rate_limit:ip" + ip;
        String id = transactionInfo.getId();

        if (rateLimiting.isAllowed(key)) {
            transactionInfo.setClientIp(key);
            kafkaTemplate.send("transactions", transactionInfo);
            return ResponseEntity.accepted().body("Transaction submitted");
        } else {
            throw new RateLimitExceededException(ErrorMessages.RATE_LIMIT_EXCEEDED);
        }

    }

    public String getClientIp(HttpServletRequest request){
        String xfHeader = request.getHeader("x-forwarded-for");
        if(xfHeader != null && !xfHeader.isEmpty()){
            System.out.println(xfHeader);
            return xfHeader.split(",")[0].trim();
        }
        System.out.println(xfHeader);
        return request.getRemoteAddr();
    }
}
