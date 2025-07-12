package fraud.fraud.interfaces;

import fraud.fraud.DTO.TransactionRequest;

import java.util.List;

public interface Handler {
    boolean handle(TransactionRequest userData, List<TransactionRequest> transactions);
    void setNext(Handler next);
}
