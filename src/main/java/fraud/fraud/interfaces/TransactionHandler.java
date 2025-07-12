package fraud.fraud.interfaces;

import fraud.fraud.DTO.TransactionRequest;
import org.springframework.messaging.handler.annotation.Payload;

import java.io.IOException;
import java.util.List;

public interface TransactionHandler {

    void transactionPipeline(@Payload TransactionRequest userData) throws Exception;
    List<TransactionRequest> getTransactions(TransactionRequest userData) throws Exception;
    void saveTransaction(TransactionRequest userData, boolean isFraud);
    void addModel(TransactionRequest userData, boolean isFraud) throws IOException;

}
