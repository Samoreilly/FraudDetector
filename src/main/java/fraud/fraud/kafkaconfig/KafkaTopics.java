package fraud.fraud.kafkaconfig;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.config.TopicBuilder;

public class KafkaTopics {

    public NewTopic urlTopic(){
        return TopicBuilder.name("transactions")
                .build();
    }
    public NewTopic resultTopic(){
        return TopicBuilder.name("out-transactions")
                .build();
    }

}
