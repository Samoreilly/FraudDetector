package fraud.fraud.services;

import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.ErrorMessages;
import fraud.fraud.Notifications.NotificationService;
import fraud.fraud.entitys.Threat;
import fraud.fraud.interfaces.Handler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FraudAverageValidation implements Handler {

    private final NotificationService notificationService;
    private final ValidateTransactions validateTransactions;
    private Handler next;

    public FraudAverageValidation(NotificationService notificationService, ValidateTransactions validateTransactions) {
        this.notificationService = notificationService;
        this.validateTransactions = validateTransactions;
    }

    @Override
    public boolean handle(TransactionRequest userData, List<TransactionRequest> transactions) throws Exception {
    //this method checks the difference between the incoming transaction and the average of there total transactions
        double diff = Math.abs(validateTransactions.averageTransaction(userData) - Double.parseDouble(userData.getData()));
        System.out.println("-------------+++++++++++++++++=-- ENTERED AVERAGE HANDLER");
        if(diff > ErrorMessages.IMMEDIATE_THRESHOLD_MET){
            userData.setFlagged(Threat.IMMEDIATE);
            userData.setResult("your transaction was marked a huge threat comparing to your average transaction amounts");
            notificationService.sendNotification(userData, userData.getResult());
            System.out.println("ZIGIAJIADIALJDAWLIDJALIZWJZLIJDWLWAIJDALIDJALDIJALIDBZLIDBWAIWDAVBDIHQA9FOIHJALGIHALIH");
            return false;

        }else if(diff > ErrorMessages.HIGH_THRESHOLD_MET){

            userData.setResult("your transaction was marked a high threat comparing to your average transaction amounts");
            notificationService.sendNotification(userData, userData.getResult());
            userData.setFlagged(Threat.HIGH);
        }
        return next == null || next.handle(userData, transactions);
    }
    @Override
    public void setNext(Handler next){
        this.next = next;
    }

}
