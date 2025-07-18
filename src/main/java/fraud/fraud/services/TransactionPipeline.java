package fraud.fraud.services;

import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.interfaces.Handler;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionPipeline {

    private final Handler firstHandler;

    public TransactionPipeline(List<Handler> handlers) throws Exception {

        if (handlers == null || handlers.isEmpty()) {
            throw new IllegalArgumentException("Handlers list cannot be null or empty");
        }
        for(int i = 0; i < handlers.size() - 1; i++){
            handlers.get(i).setNext(handlers.get(i+1));
        }
        this.firstHandler = handlers.get(0);

    }
    public boolean process(TransactionRequest request, List<TransactionRequest> previousTransactions) throws Exception {
        return firstHandler.handle(request, previousTransactions);
    }
}
