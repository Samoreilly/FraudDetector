package fraud.fraud.controllers;

import fraud.fraud.AI.NeuralNetworkManager;
import fraud.fraud.DTO.EncryptedRequest;
import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.ErrorMessages;
import fraud.fraud.Monitoring.CustomMetricsService;
import fraud.fraud.Notifications.NotificationService;
import fraud.fraud.RateLimitExceededException;
import fraud.fraud.services.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    //next up connect user to database, set session id from user id in database, pass to frontend and use in redis caching and kafka send()
    private final RateLimiting rateLimiting;
    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;
    private final SetupSse setupSse;
    private final CustomMetricsService customMetricsService;
    private final TransactionSecurityCheck transactionSecurityCheck;
    private final VpnValidation  vpnValidation;
    private final NotificationService  notificationService;
    private final NeuralNetworkManager  neuralNetworkManager;
    private final DecryptData decryptData;

    public TransactionController(RateLimiting rateLimiting, DecryptData decryptData, KafkaTemplate<String, TransactionRequest> kafkaTemplate, SetupSse setupSse, CustomMetricsService customMetricsService, TransactionSecurityCheck transactionSecurityCheck,  VpnValidation vpnValidation, NotificationService notificationService, NeuralNetworkManager neuralNetworkManager) {
        this.rateLimiting = rateLimiting;
        this.kafkaTemplate = kafkaTemplate;
        this.setupSse = setupSse;
        this.customMetricsService = customMetricsService;
        this.transactionSecurityCheck = transactionSecurityCheck;
        this.vpnValidation = vpnValidation;
        this.notificationService = notificationService;
        this.neuralNetworkManager = neuralNetworkManager;
        this.decryptData= decryptData;
    }


    @PostMapping("/tr")
    public ResponseEntity<String> transaction(
            @RequestBody TransactionRequest encryptedRequest,
            HttpServletRequest request,
            @AuthenticationPrincipal OAuth2User principal) throws Exception {
        if(principal == null){
            return ResponseEntity.status(401).body("Unauthorized");
        }
        System.out.println(encryptedRequest.toString());

        String encrypted = encryptedRequest.getData();

        // decrypt directly to TransactionRequest object
        TransactionRequest transactionInfo = decryptData.decryptToObject(encrypted, TransactionRequest.class);
        System.out.println("Transaction endpoint - decrypted data: " + transactionInfo);
        // set client ip if needed
        transactionInfo.setClientIp(request.getRemoteAddr());

        //neuralNetworkManager.initModel();// for testing

        customMetricsService.incrementTotalApiRequests();

        String ip = getClientIp(request);
        String key = "rate_limit:ip" + ip;
        String sessionId = principal.getAttribute("sub");

        logger.debug("Transaction endpoint - sessionId={}, lat={}, lon={}", sessionId, transactionInfo.getLatitude(), transactionInfo.getLongitude());


        System.out.println("Transaction endpoint - Session ID: " + sessionId);

        if (rateLimiting.isAllowed(key) && !vpnValidation.checkVpn(ip)) {
            System.out.println("IP KEY"+ key);
            transactionInfo.setClientIp(ip);
            transactionInfo.setId(sessionId);

            notificationService.sendNotification(transactionInfo, "Not a vpn");
            kafkaTemplate.send("transactions",transactionInfo);

            return ResponseEntity.accepted().body("Transaction submitted");
        }else{

            notificationService.sendNotification(transactionInfo, "Vpn detected");
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
        return ResponseEntity.ok(Map.of("sessionId", sessionId));

    }


    public String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("x-forwarded-for");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            System.out.println("X-Forwarded-For: " + xfHeader);
            logger.debug("X-Forwarded-For resolved IP: {}",xfHeader.split(",")[0].trim());
            return xfHeader.split(",")[0].trim();
        }
        System.out.println("Remote Address: " + request.getRemoteAddr());
        logger.debug("Remote Address: {}", request.getRemoteAddr());
        return request.getRemoteAddr();
    }
}
