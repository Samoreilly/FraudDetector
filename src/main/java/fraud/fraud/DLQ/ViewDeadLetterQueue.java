package fraud.fraud.DLQ;

import fraud.fraud.DTO.DatabaseDTO;
import fraud.fraud.DTO.DatabaseRepo;
import fraud.fraud.DTO.TransactionRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ViewDeadLetterQueue{
    //THIS CLASS WILL BE USED TO PREVIEW DEAD LETTER QUEUE THROUGH AN ENDPOINT IN THE BROWSER
    private final DatabaseRepo databaseRepo;

    public ViewDeadLetterQueue(DatabaseRepo databaseRepo){
        this.databaseRepo = databaseRepo;
    }

    public List<DatabaseDTO> previewDLQ(){
        return databaseRepo.findAll();
    }

    @Transactional
    public void sendToQueue(DatabaseDTO transactionRequest){

        databaseRepo.save(transactionRequest);
        System.out.println("SAVED" + transactionRequest);

        previewDLQ();
    }
}
