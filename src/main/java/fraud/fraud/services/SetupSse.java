package fraud.fraud.services;

import fraud.fraud.DTO.TransactionRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SetupSse {

    private final Map<String, SseEmitter> userEmitter = new ConcurrentHashMap<>();

    public SseEmitter streamResults(HttpSession session) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // Set longer timeout
        String sessionId = session.getId();

        System.out.println("Setting up SSE for session: " + sessionId);
        userEmitter.put(sessionId, emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name("Connected")
                    .data("SSE connection established for session: " + sessionId));
        } catch (IOException e) {
            System.out.println("Error sending connection event: " + e.getMessage());
            userEmitter.remove(sessionId);
        }

        // Handle cleanup when connection is closed
        emitter.onCompletion(() -> {
            System.out.println("SSE connection completed for session: " + sessionId);
            userEmitter.remove(sessionId);
        });

        emitter.onTimeout(() -> {
            System.out.println("SSE connection timeout for session: " + sessionId);
            userEmitter.remove(sessionId);
        });

        emitter.onError((ex) -> {
            System.out.println("SSE connection error for session: " + sessionId + ", error: " + ex.getMessage());
            userEmitter.remove(sessionId);
        });

        return emitter;
    }

    @KafkaListener(topics = "out-transactions", groupId = "out-transactions",
            containerFactory = "outTransactionsListenerContainerFactory")
    public void sendResult(ConsumerRecord<String, TransactionRequest> record) {
        System.out.println("Received Kafka message - Key: " + record.key() + ", Value: " + record.value());

        String sessionId = record.key();
        if (sessionId == null) {
            System.out.println("Record key is null, cannot find SSE connection");
            return;
        }

        SseEmitter emitter = userEmitter.get(sessionId);
        if (emitter == null) {
            System.out.println("Cannot find SSE emitter for session: " + sessionId);
            System.out.println("Available sessions: " + userEmitter.keySet());
            return;
        }

        TransactionRequest transactionRequest = record.value();
        if (transactionRequest == null) {
            System.out.println("Transaction request is null");
            return;
        }

        try {
            System.out.println("sending result to SSE for session: " + sessionId);
            String result = transactionRequest.getResult();

            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("data", result != null ? result : "Processing completed");
            responseMap.put("sessionId", sessionId);
            responseMap.put("timestamp", String.valueOf(System.currentTimeMillis()));

            emitter.send(SseEmitter.event()
                    .name("transaction-result")
                    .data(responseMap));

            System.out.println("successfully sent result to SSE");

        } catch (IOException e) {
            System.out.println("error sending SSE message: " + e.getMessage());
            handleDisconnectedClient(sessionId, emitter);
        } catch (Exception e) {
            System.out.println("error processing Kafka message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDisconnectedClient(String sessionId, SseEmitter emitter) {
        System.out.println("Handling disconnected client for session: " + sessionId);
        userEmitter.remove(sessionId);
        try {
            emitter.complete();
        } catch (Exception e) {
            System.out.println("Error completing emitter: " + e.getMessage());
        }
    }

    public int getActiveConnectionsCount() {
        return userEmitter.size();
    }

    public java.util.Set<String> getActiveSessionIds() {
        return userEmitter.keySet();
    }
}