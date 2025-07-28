package fraud.fraud.services;

import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.Notifications.NotificationService;
import fraud.fraud.entitys.Threat;
import fraud.fraud.interfaces.Handler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

//https://github.com/faiqsohail/Java-VPNDetection

@Service
public class TransactionSecurityCheck {

    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;
    private final NotificationService notificationService;
    private final TransactionPipeline pipeline;

    public TransactionSecurityCheck(KafkaTemplate<String, TransactionRequest> kafkaTemplate, NotificationService notificationService, List<Handler> handlers) throws Exception {
        this.kafkaTemplate = kafkaTemplate;
        this.notificationService = notificationService;
        this.pipeline = new TransactionPipeline(handlers);

    }


    public boolean checkFraud(double fraudProb, boolean isFraud, TransactionRequest userData){

        if(isFraud && fraudProb >= .90){
            userData.setFlagged(Threat.IMMEDIATE);
            notificationService.sendNotification(userData, "Your transaction is extremely suspicious and was flagged");
            return true;
        }
        if(isFraud || fraudProb >= .75){
            userData.setFlagged(Threat.HIGH);
            notificationService.sendNotification(userData, "Your transaction was deemed suspicious");
            return false;
        }else if(fraudProb >= .45){
            userData.setFlagged(Threat.MEDIUM);
            notificationService.sendNotification(userData, "Your transaction was deemed mildly suspicious");
            return false;
        }else{
            userData.setFlagged(Threat.LOW);
            notificationService.sendNotification(userData, "Your transaction cleared fraud check");
        }
        return false;
    }
}
