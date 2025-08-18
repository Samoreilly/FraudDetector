package fraud.fraud.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import fraud.fraud.AI.AnomalyTraining;
import fraud.fraud.AI.HandleNeuralTransaction;
import fraud.fraud.AI.LogisticRegressionTraining;
import fraud.fraud.AI.NeuralNetworkManager;
import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.Notifications.NotificationService;
import fraud.fraud.interfaces.TransactionHandler;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService implements TransactionHandler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SetupSse setupSse;
    private final KafkaTemplate<String, TransactionRequest> kafkaTemplate;
    private final TransactionSecurityCheck transactionSecurityCheck;
    private final LogisticRegressionTraining logisticRegressionTraining;
    private final ValidateTransactions validateTransactions;
    private final NotificationService notificationService;
    private final TransactionPipeline pipeline;
    private final AnomalyTraining anomalyTraining;
    private final HandleNeuralTransaction  handleNeuralTransaction;
    private final NeuralNetworkManager neuralNetworkManager;
    private final TransactionCounter transactionCounter;

    LogisticRegressionTraining service = new LogisticRegressionTraining();

    public TransactionService(RedisTemplate<String, Object> redisTemplate, LogisticRegressionTraining logisticRegressionTraining, ObjectMapper objectMapper, SetupSse setupSse, KafkaTemplate<String, TransactionRequest>  kafkaTemplate, TransactionSecurityCheck transactionSecurityCheck, ValidateTransactions validateTransactions, NotificationService  notificationService, TransactionPipeline pipeline, AnomalyTraining anomalyTraining,  HandleNeuralTransaction handleNeuralTransaction, NeuralNetworkManager neuralNetworkManager, TransactionCounter transactionCounter) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.setupSse = setupSse;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionSecurityCheck = transactionSecurityCheck;
        this.logisticRegressionTraining = logisticRegressionTraining;
        this.validateTransactions = validateTransactions;
        this.notificationService = notificationService;
        this.pipeline = pipeline;
        this.anomalyTraining = anomalyTraining;
        this.handleNeuralTransaction = handleNeuralTransaction;
        this.neuralNetworkManager = neuralNetworkManager;
        this.transactionCounter = transactionCounter;
    }


    public List<TransactionRequest> getTransactions(TransactionRequest userData) throws Exception { // passes into transaction of type TransactionRequest
        String key = userData.getId();


        Long size = redisTemplate.opsForList().size(key);
        notificationService.sendNotification(userData, "Checking your previous transactions");
        List<Object> transactions = redisTemplate.opsForList().range(userData.getId(),0,-1);//gets userdata by id by looking from start to end of list
        if(transactions == null){
            return null;
        }
        if(size == null || size == 0){
            redisTemplate.expire(userData.getId(), Duration.ofMinutes(10));//set time to live if it doesnt exist
        }
        List<TransactionRequest> tx = new ArrayList<>();

        for(Object obj : transactions){
            TransactionRequest trans = objectMapper.convertValue(obj, TransactionRequest.class);

            tx.add(trans);

        }
        System.out.println("777 " + transactions);
        for (TransactionRequest transaction : tx) {

            System.out.println(transaction.getData());
        }

        return tx;
    }

    @KafkaListener(topics = "transactions", groupId = "in-transactions", containerFactory = "factory")
    public void transactionPipeline(@Payload TransactionRequest userData) throws Exception {


//        handleNeuralTransaction.handleTransaction(userData);


        anomalyTraining.anomalyPipeline(userData);

        notificationService.sendNotification(userData, "Processing your transaction");
        System.out.println(userData);




        long currentEpoch = userData.getTime().toEpochSecond(ZoneOffset.UTC);
        //normalize time to fit into the models time range. As the models time range is around 2024 and input data is in 2025
        double epochSeconds = 1719650000 + (currentEpoch % 60000);
        service.trainModel();
        boolean isFraud = service.predictFraud(Double.parseDouble(userData.getData()),epochSeconds, userData.getLatitude(), userData.getLongitude()); // amount, lat, lng
        double fraudProb = service.getFraudProbability(Double.parseDouble(userData.getData()), epochSeconds, userData.getLatitude(), userData.getLongitude());


        System.out.printf("Fraud Prediction: %s\n", isFraud ? "FRAUD" : "LEGITIMATE");
        System.out.printf("Fraud Probability: %.2f%% (%.4f)\n", fraudProb * 100, fraudProb);
        if(transactionSecurityCheck.checkFraud(fraudProb, isFraud, userData)) {
            System.out.println("Fraud detected - exiting early from pipeline");
            return;
        }
        // retrieve users transactions as a list
        List<TransactionRequest> transactions = getTransactions(userData);
        boolean result = pipeline.process(userData, transactions);
        System.out.println(result + "-----------------------HANDLER RESULT");
        if(result){
            saveTransaction(userData, isFraud);//save transaction
        }else{
            notificationService.sendNotification(userData, "Transaction pipeline error");
        }
        System.out.println("Cached");

    }

    public void saveTransaction(TransactionRequest userData, boolean isFraud){
        redisTemplate.opsForList().leftPush(userData.getId(), userData);
        notificationService.sendNotification(userData, "Caching your transaction");

        try {
            boolean res = transactionCounter.counter(userData);
            if(res)return;// checks how much transactions are made per user in the last hour

            notificationService.sendNotification(userData, "Successful transaction");
            addModel(userData, isFraud);// add data to csv to build improve dataset
            System.out.println("Transaction sent successfully");
            //redis counter logic goes ho

        } catch (Exception e) {
            System.err.println("Failed to send transaction: " + e.getMessage());
            notificationService.sendNotification(userData, "Error processing transaction");
        }
    }

    public void addModel(TransactionRequest userData, boolean isFraud) throws IOException {
        String CSV_PATH = "/home/sam-o-reilly/IdeaProjects/FraudDetector/csv/trans.csv";

        long currentEpoch = userData.getTime().toEpochSecond(ZoneOffset.UTC);
        long epochSeconds = 1719650000 + (currentEpoch % 60000);

        String val = isFraud ? "1" : "0";

        String[] data = new String[] {
                userData.getId(),
                userData.getData(),
                String.valueOf(epochSeconds),
                String.valueOf(userData.getLatitude()),
                String.valueOf(userData.getLongitude()),
                val
        };
        try (CSVWriter writer = new CSVWriter( new FileWriter(CSV_PATH, true),
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {
            writer.writeNext(data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to append data to CSV file", e);
        }


    }
}
