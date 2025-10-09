package fraud.fraud.DLQ;

import fraud.fraud.DTO.DatabaseDTO;
import fraud.fraud.DTO.DatabaseRepo;
import fraud.fraud.DTO.TransactionRequest;
import org.hibernate.dialect.Database;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DeadLetterQueueController {

    private final ViewDeadLetterQueue viewDeadLetterQueue;
    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;
    private final DatabaseRepo databaseRepo;

    public DeadLetterQueueController(ViewDeadLetterQueue viewDeadLetterQueue, KafkaTemplate<String, TransactionRequest> kafkaTemplate, DatabaseRepo databaseRepo){
        this.viewDeadLetterQueue = viewDeadLetterQueue;
        this.kafkaTemplate = kafkaTemplate;
        this.databaseRepo = databaseRepo;
    }

    @GetMapping("/preview")
    public ResponseEntity<List<DatabaseDTO>> dlqPreviewApi(){
        return new ResponseEntity<List<DatabaseDTO>>(viewDeadLetterQueue.getDLQEvents(), HttpStatus.ACCEPTED);
    }
    @GetMapping("/retry")
    public void queueDLQEvents(){


        List<DatabaseDTO> events = viewDeadLetterQueue.getDLQEvents();
        if(events.isEmpty()){
            System.out.println("NO DLQ EVENTS FOUND");
            return;
        }

        try {


            for (DatabaseDTO dlq : events) {
                TransactionRequest txRequest = new TransactionRequest();
                txRequest.setId(dlq.getId());
                txRequest.setData(dlq.getData());
                txRequest.setTime(dlq.getTime());
                txRequest.setClientIp(dlq.getClientIp());
                txRequest.setResult(dlq.getResult());
                txRequest.setLatitude(dlq.getLatitude());
                txRequest.setLongitude(dlq.getLongitude());
                txRequest.setIsFraud(dlq.getIsFraud());
                txRequest.setFlagged(null);

                kafkaTemplate.send("transactions", txRequest);
            }
            System.out.println("Sent to Kafka Listener!");

            databaseRepo.deleteAll();//w engineering
            queueDLQEvents();
        }catch (Exception e){
            System.out.println("Error retrying dlq events");
        }

    }
}
