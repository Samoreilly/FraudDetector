package fraud.fraud.services;
import com.fasterxml.jackson.databind.ObjectMapper;
import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.ErrorMessages;
import fraud.fraud.Notifications.NotificationService;
import fraud.fraud.entitys.Threat;
import fraud.fraud.interfaces.Handler;
import net.vpnblocker.api.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

//https://github.com/faiqsohail/Java-VPNDetection

@Service
public class TransactionSecurityCheck {

    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;
    private final NotificationService notificationService;
    private final TransactionPipeline pipeline;

    public TransactionSecurityCheck(KafkaTemplate<String, TransactionRequest> kafkaTemplate, NotificationService notificationService, List<Handler> handlers) {
        this.kafkaTemplate = kafkaTemplate;
        this.notificationService = notificationService;
        this.pipeline = new TransactionPipeline(handlers);

    }

    //this method checks the difference between the incoming transaction and the average of there total transactions
    public boolean checkAverageDifference(Double diff, TransactionRequest userData){
        if(diff > ErrorMessages.IMMEDIATE_THRESHOLD_MET){
            userData.setFlagged(Threat.IMMEDIATE);
            userData.setResult("your transaction was marked a huge threat comparing to your average transaction amounts");
            kafkaTemplate.send("out-transactions", userData.getId(), userData);
            return false;
        }else if(diff > ErrorMessages.HIGH_THRESHOLD_MET){
            userData.setResult("your transaction was marked a high threat comparing to your average transaction amounts");
            kafkaTemplate.send("out-transactions", userData.getId(), userData);
            userData.setFlagged(Threat.HIGH);
            return true;
        }else{
            return true;
        }
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
