package fraud.fraud.DLQ;

import fraud.fraud.DTO.DatabaseDTO;
import fraud.fraud.DTO.DatabaseRepo;
import fraud.fraud.DTO.TransactionRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;


@Service
public class ViewDeadLetterQueue{
    //THIS CLASS WILL BE USED TO PREVIEW DEAD LETTER QUEUE THROUGH AN ENDPOINT IN THE BROWSER
    private final DatabaseRepo databaseRepo;

    public ViewDeadLetterQueue(DatabaseRepo databaseRepo){
        this.databaseRepo = databaseRepo;
    }
    public void previewDLQ(){
        databaseRepo.findAll().forEach(System.out::println);
    }

    @Transactional
    public void sendToQueue(DatabaseDTO transactionRequest){

        databaseRepo.save(transactionRequest);
        System.out.println("SAVED" + transactionRequest);

        previewDLQ();
    }
}
