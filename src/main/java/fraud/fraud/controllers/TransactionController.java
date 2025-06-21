package fraud.fraud.controllers;


import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.services.RateLimiting;
import fraud.fraud.services.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;
    private final RateLimiting rateLimiting;

    public TransactionController(TransactionService transactionService, RateLimiting rateLimiting) {
        this.transactionService = transactionService;
        this.rateLimiting = rateLimiting;
    }

    @PostMapping("/tr")
    public Map<String, String> transaction(@RequestBody TransactionRequest transactionInfo, HttpServletRequest request){
        String ip = getClientIp(request);
        String key = "rate_limit:ip" + ip;


        Map<String, String> userData = new HashMap<>();
        if(rateLimiting.isAllowed(key)) {
            userData.put(transactionInfo.getId(), transactionInfo.getData());
            transactionInfo.setClientIp(key);
            return transactionService.transactionPipeline(transactionInfo);
        }else{
            return Map.of("Status","TOO_MUCH_REQUESTS");
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
