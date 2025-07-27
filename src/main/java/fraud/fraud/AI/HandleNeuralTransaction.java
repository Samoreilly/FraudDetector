package fraud.fraud.AI;

import com.fasterxml.jackson.databind.ObjectMapper;
import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.services.ValidateTransactions;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.Arrays;

@Service
public class HandleNeuralTransaction {

    private final NeuralNetworkManager neuralNetworkManager;
    private final MultiLayerNetwork network;
    private final DataNormalization normalizer;
    private final ValidateTransactions validateTransactions;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public HandleNeuralTransaction(NeuralNetworkManager neuralNetworkManager, ValidateTransactions  validateTransactions, RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper, ObjectMapper objectMapper1) {
        this.neuralNetworkManager = neuralNetworkManager;
        this.network = neuralNetworkManager.getNetwork();
        this.normalizer = neuralNetworkManager.getNormalizer();
        this.validateTransactions = validateTransactions;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void handleTransaction(TransactionRequest userData) throws Exception {
        if (userData == null) return;
        if (Double.parseDouble(userData.getData()) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }
        Object transaction = redisTemplate.opsForList().getFirst(userData.getId());
        if(transaction == null){
            return;
        }
        TransactionRequest trans = objectMapper.convertValue(transaction, TransactionRequest.class);

        Double prevLatitude = trans.getLatitude();
        Double prevLongitude = trans.getLongitude();
        Double latitude = userData.getLatitude();
        Double longitude = userData.getLongitude();
        double logAmount = Math.log1p(Double.parseDouble(userData.getData()));
        int hourOfDay = userData.getTime().getHour();


        double distance = validateTransactions.calculateDistance(latitude, longitude, prevLatitude, prevLongitude);
        int dayOfWeek = userData.getTime().getDayOfWeek().getValue();
        double[] data = new double[] {
                logAmount,
                hourOfDay,
                dayOfWeek,
                latitude,
                longitude,
                distance
        };

        INDArray input = Nd4j.create(data).reshape(1, data.length);
        normalizer.transform(input);


        INDArray output = network.output(input, false);
        double fraudProb = output.getDouble(1);
        double threshold = 0.7;
        int predict = (fraudProb > threshold) ? 1 : 0;

        System.out.println("Raw input: " + Arrays.toString(data));
        System.out.println("Normalized input: " + input);
        System.out.println("Output: [Legitimate=" + output.getDouble(0) + ", Fraud=" + output.getDouble(1) + "]");
        System.out.println("Prediction: " + predict);
    }

}
