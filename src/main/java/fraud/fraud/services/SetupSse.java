package fraud.fraud.services;

import jakarta.servlet.http.HttpSession;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SetupSse {

    private final Map<String, SseEmitter> userEmitter = new ConcurrentHashMap<>();

    public SseEmitter streamResults(HttpSession session) {
        SseEmitter emitter = new SseEmitter();
        userEmitter.put(session.getId(), emitter);
        System.out.println(session.getId());

        try {
            emitter.send(SseEmitter.event().name("Connected"));
        } catch (IOException e) {
            System.out.println("Error sending event" + e);
        }
        emitter.onCompletion(() -> userEmitter.remove(session.getId()));
        emitter.onTimeout(() -> userEmitter.remove(session.getId()));

        return emitter;
    }

    @KafkaListener(topics = "out-transactions", groupId = "out-transactions")
    public void sendResult(ConsumerRecord<String, String> record) {
        String id = record.key();
        SseEmitter emitter = userEmitter.get(id);
        if (record.value() != null) {
            System.out.println("ID NOT NULL");
        }
        if (emitter != null && isEmitterActive(emitter)) {//check if session id exists
            try {
                Map<String, String> map = new HashMap<>();
                System.out.println("RECORD VALUE --- " + record.value());
                map.put("data", record.value());
                emitter.send(map);//return to frontend
                System.out.println("successfully sent record.value");

            } catch (Exception e) {
                e.printStackTrace();
                handleDisconnectedClient(emitter);
                System.out.println("NOT ACTIVE");
            }
        }
    }

    private boolean isEmitterActive(SseEmitter emitter) {

        return userEmitter.containsValue(emitter);
    }

    private void handleDisconnectedClient(SseEmitter emitter) {
        userEmitter.values().remove(emitter);
        emitter.complete();
    }
}




//    @KafkaListener(topics = "url-results", groupId = "produce-url-python")
//    public void sendResult(ConsumerRecord<String, String> record){
//        System.out.println(record.value() + "wijadiwl");
//        System.out.println(record.key() + "key");
//        String id = record.key();//get session id from message key from python stream
//        System.out.println("RECORD KEYYY---A"+ record.key());
//        SseEmitter emitter = userEmitter.get(id);//get connection from user emitter
//
//        if(record.value() != null){
//            System.out.println("NOT NULLLL");
//        }
//        if(emitter != null && isEmitterActive(emitter)){//check if session id exists
//            try{
//                Map<String, String> map = new HashMap<>();
//                System.out.println("RECORD VALUE --- " + record.value());
//                map.put("data",record.value());
//                emitter.send(map);//return to frontend
//                System.out.println("successfully sent record.value");
//
//            }catch(Exception e){
//                e.printStackTrace();
//                handleDisconnectedClient(emitter);
//                System.out.println("NOT ACTIVE");
//            }
//        }
//    }
//    private boolean isEmitterActive(SseEmitter emitter) {
//
//        return userEmitter.containsValue(emitter);
//    }
//
//    private void handleDisconnectedClient(SseEmitter emitter) {
//        userEmitter.values().remove(emitter);
//        emitter.complete();
//    }
//
//}

