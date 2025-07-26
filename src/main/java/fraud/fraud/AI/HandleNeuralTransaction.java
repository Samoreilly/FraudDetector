package fraud.fraud.AI;

import fraud.fraud.DTO.TransactionRequest;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

@Service
public class HandleNeuralTransaction {

    private final NeuralNetworkManager neuralNetworkManager;
    private final MultiLayerNetwork network;
    private DataNormalization normalizer;

    public HandleNeuralTransaction(NeuralNetworkManager neuralNetworkManager) {
        this.neuralNetworkManager = neuralNetworkManager;
        this.network = neuralNetworkManager.getNetwork();
        this.normalizer = neuralNetworkManager.getNormalizer();
    }

    public void handleTransaction(TransactionRequest userData) throws Exception {

        if(userData == null)return;

        long currentEpoch = userData.getTime().toEpochSecond(ZoneOffset.UTC);
        long epochSeconds = 1719650000 + (currentEpoch % 60000);

        double[] data = new double[] {
                Double.parseDouble(userData.getData()),epochSeconds, userData.getLatitude(), userData.getLongitude()
        };

        INDArray input = Nd4j.create(data);
        input = input.reshape(1, data.length);

        normalizer.transform(input);

        INDArray output = network.output(input);

        int predict = Nd4j.argMax(output, 1).getInt(0);
        System.out.println("predict = " + predict);
    }
}
