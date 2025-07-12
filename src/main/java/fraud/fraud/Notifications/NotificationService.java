package fraud.fraud.Notifications;

import fraud.fraud.DTO.TransactionRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;

    public NotificationService(KafkaTemplate<String, TransactionRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    public void sendNotification(TransactionRequest userData, String message) {
        userData.setResult(message);
        kafkaTemplate.send("out-transactions", userData.getId(), userData);
    }
}
