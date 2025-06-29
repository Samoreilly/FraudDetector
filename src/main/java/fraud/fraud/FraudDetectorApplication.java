package fraud.fraud;

import fraud.fraud.AI.LogisticRegressionTraining;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableCaching
@EnableKafka
public class FraudDetectorApplication {
	@Autowired
	private LogisticRegressionTraining logisticRegression;

	public static void main(String[] args) {
		SpringApplication.run(FraudDetectorApplication.class, args);

	}


}
