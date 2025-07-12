package fraud.fraud.services;

import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.Notifications.NotificationService;
import fraud.fraud.interfaces.Handler;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TimestampAndLocationHandler implements Handler {

    private final NotificationService  notificationService;
    private final Validator validator;
    private Handler next;

    public TimestampAndLocationHandler(Validator validator, NotificationService notificationService) {
        this.validator = validator;
        this.notificationService = notificationService;
    }
    @Override
    public boolean handle(TransactionRequest userData, List<TransactionRequest> validateTimes){

        boolean location = validator.checkTransactionByLocation(userData), timestamps = validator.checkTimestamps(userData, validateTimes);

        if(location && !timestamps){
            notificationService.sendNotification(userData, "Too many transactions. retry again in 5 seconds");
            System.out.println("sent too early");
            return false;
        }

        return next == null || next.handle(userData, validateTimes);
    }
    @Override
    public void setNext(Handler next){
        this.next = next;
    }
}
