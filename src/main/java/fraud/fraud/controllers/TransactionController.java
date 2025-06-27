package fraud.fraud.controllers;

import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.ErrorMessages;
import fraud.fraud.Monitoring.CustomMetricsService;
import fraud.fraud.RateLimitExceededException;
import fraud.fraud.services.RateLimiting;
import fraud.fraud.services.SetupSse;
import fraud.fraud.services.TransactionSecurityCheck;
import fraud.fraud.services.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class TransactionController {

    private final TransactionService transactionService;
    private final RateLimiting rateLimiting;
    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;
    private final SetupSse setupSse;
    private final CustomMetricsService customMetricsService;
    private final TransactionSecurityCheck transactionSecurityCheck;

    public TransactionController(TransactionService transactionService, RateLimiting rateLimiting, KafkaTemplate<String, TransactionRequest> kafkaTemplate, SetupSse setupSse, CustomMetricsService customMetricsService, TransactionSecurityCheck transactionSecurityCheck) {
        this.transactionService = transactionService;
        this.rateLimiting = rateLimiting;
        this.kafkaTemplate = kafkaTemplate;
        this.setupSse = setupSse;
        this.customMetricsService = customMetricsService;
        this.transactionSecurityCheck = transactionSecurityCheck;
    }

    @PostMapping("/tr")
    public ResponseEntity<String> transaction(@RequestBody TransactionRequest transactionInfo, HttpServletRequest request, HttpSession session) throws IOException {
        customMetricsService.incrementTotalApiRequests();
        String ip = getClientIp(request);
        String key = "rate_limit:ip" + ip;
        String id = session.getId();
        System.out.println("LATITUDE"+transactionInfo.getLatitude());
        System.out.println("LONGITUDE"+transactionInfo.getLongitude());

        if (session.isNew()) {
            session.setAttribute("created", System.currentTimeMillis());
        }

        System.out.println("Transaction endpoint - Session ID: " + id);

        if (rateLimiting.isAllowed(key) && !transactionSecurityCheck.checkVpn(ip)) {
            transactionInfo.setClientIp(ip);
            transactionInfo.setId(id);
            transactionInfo.setResult("Not a vpn");
            kafkaTemplate.send("out-transactions", transactionInfo.getId(), transactionInfo);
            kafkaTemplate.send("transactions",transactionInfo);
            return ResponseEntity.accepted().body("Transaction submitted");
        } else {
            transactionInfo.setResult("Is a vpn");
            kafkaTemplate.send("out-transactions", transactionInfo.getId(), transactionInfo);
            customMetricsService.incrementRateLimitedRequests();
            throw new RateLimitExceededException(ErrorMessages.RATE_LIMIT_EXCEEDED);
        }
    }

    @GetMapping("/streams/results")
    public SseEmitter streamResults(HttpSession session) {

        if (session.isNew()) {
            session.setAttribute("created", System.currentTimeMillis());
        }

        String sessionId = session.getId();
        System.out.println("SSE endpoint - Session ID: " + sessionId);
        return setupSse.streamResults(session);
    }

    @GetMapping()
    public Map<String, String> getSessionId(HttpSession session) {
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
