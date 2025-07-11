package fraud.fraud;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

public class ErrorMessages {


    public static final String RATE_LIMIT_EXCEEDED = "Too many requests";
    public static final String TOO_EARLY = "Request sent too early";
    public static final String ACCESS_DENIED = "Access denied";
    public static final String INVALID_ITEM = "Invalid";
    public static final String TRANSACTION_SUCCESSFUL = "Transaction completed successfully";

    public static final int IMMEDIATE_THRESHOLD_MET = 8000;
    public static final int HIGH_THRESHOLD_MET = 5000;

    private ErrorMessages() {

    }
}
