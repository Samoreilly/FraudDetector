package fraud.fraud.services;
import com.fasterxml.jackson.databind.ObjectMapper;
import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.ErrorMessages;
import fraud.fraud.entitys.Threat;
import net.vpnblocker.api.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;

//https://github.com/faiqsohail/Java-VPNDetection

@Service
public class TransactionSecurityCheck {

    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;
    public TransactionSecurityCheck(KafkaTemplate<String, TransactionRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    VPNDetection vpnDetection = new VPNDetection();

    public boolean checkVpn(String ip) throws IOException {

        if(ip != null) {
            Boolean isVPN = new VPNDetection().getResponse(ip).hostip;
            System.out.println("IS VPN " + isVPN);
            return isVPN;
        }else{
            return false;
        }
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
}
