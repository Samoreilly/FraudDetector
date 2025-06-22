package fraud.fraud.controllers;


import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.ErrorMessages;
import fraud.fraud.RateLimitExceededException;
import fraud.fraud.services.RateLimiting;
import fraud.fraud.services.SetupSse;
import fraud.fraud.services.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;
    private final RateLimiting rateLimiting;
    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;
    private final SetupSse setupSse;

    public TransactionController(TransactionService transactionService, RateLimiting rateLimiting, KafkaTemplate<String, TransactionRequest> kafkaTemplate, SetupSse setupSse) {
        this.transactionService = transactionService;
        this.rateLimiting = rateLimiting;
        this.kafkaTemplate = kafkaTemplate;
        this.setupSse = setupSse;
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
    @GetMapping("/streams/results")
    public SseEmitter streamResults(HttpSession session){//session is auto injected when get request is made
        return setupSse.streamResults(session);
    }
    @GetMapping()
    public String sessionId(HttpSession session){
        session.setAttribute("id","1:");
        return session.getId();

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
