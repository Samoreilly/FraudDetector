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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class TransactionController {

    //next up connect user to database, set session id from user id in database, pass to frontend and use in redis caching and kafka send()
    private final RateLimiting rateLimiting;
    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;
    private final SetupSse setupSse;
    private final CustomMetricsService customMetricsService;
    private final TransactionSecurityCheck transactionSecurityCheck;

    public TransactionController(RateLimiting rateLimiting, KafkaTemplate<String, TransactionRequest> kafkaTemplate, SetupSse setupSse, CustomMetricsService customMetricsService, TransactionSecurityCheck transactionSecurityCheck) {
        this.rateLimiting = rateLimiting;
        this.kafkaTemplate = kafkaTemplate;
        this.setupSse = setupSse;
        this.customMetricsService = customMetricsService;
        this.transactionSecurityCheck = transactionSecurityCheck;
    }


    @PostMapping("/tr")
    public ResponseEntity<String> transaction(@RequestBody TransactionRequest transactionInfo, HttpServletRequest request, @AuthenticationPrincipal OAuth2User principal) throws IOException {

        if(principal == null){
            return ResponseEntity.status(401).body("Unauthorized");
        }

        customMetricsService.incrementTotalApiRequests();
        String ip = getClientIp(request);
        String key = "rate_limit:ip" + ip;
        String sessionId = principal.getAttribute("sub");

        System.out.println("LATITUDE"+transactionInfo.getLatitude());
        System.out.println("LONGITUDE"+transactionInfo.getLongitude());


        System.out.println("Transaction endpoint - Session ID: " + sessionId);

        if (rateLimiting.isAllowed(key) && !transactionSecurityCheck.checkVpn(ip)) {
            System.out.println("IP KEY"+ key);
            transactionInfo.setClientIp(ip);
            transactionInfo.setId(sessionId);
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
    public SseEmitter streamResults(@AuthenticationPrincipal OAuth2User principal) {


        if(principal == null){
            return null;
        }else {
            String sessionId = principal.getAttribute("sub");
            System.out.println("SSE endpoint - Session ID: " + sessionId);
            return setupSse.streamResults(principal);
        }


    }

    @GetMapping()
    public ResponseEntity<Map<String, String>> getSessionId(@AuthenticationPrincipal OAuth2User principal, HttpServletResponse response) {

        if(principal == null){
            return ResponseEntity.status(401).body(Map.of("Error","Unauthorized"));
        }

        String sessionId = principal.getAttribute("sub");

        ResponseCookie cookie = ResponseCookie.from("sessionId", sessionId)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(3600)
                .build();
        response.addHeader("Set-Cookie",cookie.toString());
        return ResponseEntity.ok(Map.of("sessionId",sessionId));

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
