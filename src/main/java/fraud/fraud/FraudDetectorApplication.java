package fraud.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;


@EnableCaching
@EnableKafka
@SpringBootApplication
public class FraudDetectorApplication {
	public static void main(String[] args) {
		SpringApplication.run(FraudDetectorApplication.class, args);

	}
}
