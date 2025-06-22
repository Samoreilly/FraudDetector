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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true") // Enable CORS with credentials
public class TransactionController {

    private final TransactionService transactionService;
    private final RateLimiting rateLimiting;
    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;
    private final SetupSse setupSse;

    public TransactionController(TransactionService transactionService, RateLimiting rateLimiting,
                                 KafkaTemplate<String, TransactionRequest> kafkaTemplate, SetupSse setupSse) {
        this.transactionService = transactionService;
        this.rateLimiting = rateLimiting;
        this.kafkaTemplate = kafkaTemplate;
        this.setupSse = setupSse;
    }

    @PostMapping("/tr")
    public ResponseEntity<String> transaction(@RequestBody TransactionRequest transactionInfo,
                                              HttpServletRequest request, HttpSession session) {
        String ip = getClientIp(request);
        String key = "rate_limit:ip" + ip;
        String id = session.getId();

        // Force session creation if it doesn't exist
        if (session.isNew()) {
            session.setAttribute("created", System.currentTimeMillis());
        }

        System.out.println("Transaction endpoint - Session ID: " + id);

        if (rateLimiting.isAllowed(key)) {
            transactionInfo.setClientIp(ip);
            transactionInfo.setId(id);
            kafkaTemplate.send("transactions",transactionInfo);//THIS SHOULD BE IN SERVICE NOT HERE.. FIX SOON
            return ResponseEntity.accepted().body("Transaction submitted");
        } else {
            throw new RateLimitExceededException(ErrorMessages.RATE_LIMIT_EXCEEDED);
        }
    }

    @GetMapping("/streams/results")
    public SseEmitter streamResults(HttpSession session) {
        // Force session creation if it doesn't exist
        if (session.isNew()) {
            session.setAttribute("created", System.currentTimeMillis());
        }

        String sessionId = session.getId();
        System.out.println("SSE endpoint - Session ID: " + sessionId);
        return setupSse.streamResults(session);
    }

    @GetMapping()
    public Map<String, String> getSessionId(HttpSession session) {
        // Force session creation if it doesn't exist
        if (session.isNew()) {
            session.setAttribute("created", System.currentTimeMillis());
        }

        String sessionId = session.getId();
        System.out.println("Session ID endpoint - Session ID: " + sessionId);
        return Map.of("sessionId", sessionId);
    }


    public String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("x-forwarded-for");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            System.out.println("X-Forwarded-For: " + xfHeader);
            return xfHeader.split(",")[0].trim();
        }
        System.out.println("Remote Address: " + request.getRemoteAddr());
        return request.getRemoteAddr();
    }
}