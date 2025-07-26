package fraud.fraud.AI;

import fraud.fraud.services.TransactionService;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.util.FeatureUtil;
import org.nd4j.shade.guava.primitives.Ints;
import org.nd4j.linalg.dataset.DataSet;
import org.springframework.stereotype.Service;
import smile.data.Dataset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class NeuralNetworkManager {

    private static final String CSV_PATH = "/home/sam-o-reilly/IdeaProjects/FraudDetector/csv/trans.csv";
    private final LogisticRegressionTraining logisticRegressionTraining;
    DataNormalization normalizer = new NormalizerStandardize();//normalise data to get rid of extremely high high's and low low's
    public NeuralNetworkManager(LogisticRegressionTraining logisticRegressionTraining) {
        this.logisticRegressionTraining = logisticRegressionTraining;
    }
    public MultiLayerNetwork getNetwork() {
        return network;
    }

    public DataNormalization getNormalizer() {
        return normalizer;
    }

    MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(123)  // for reproducibility
            .updater(new Adam(0.001))  // optimizer and learning rate
            .list()
            .layer(new DenseLayer.Builder()
                    .nIn(4)//features
                    .nOut(50)//neurons
                    .activation(Activation.RELU)
                    .build())
            .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                    .nIn(50)
                    .nOut(2)//classes - fraud or no fraud
                    .activation(Activation.SOFTMAX)
                    .build())
            .build();

    MultiLayerNetwork network = new MultiLayerNetwork(conf);

    public DataSet createDataset() throws Exception {

        List<String[]> list = logisticRegressionTraining.readCsvFile(CSV_PATH);//get data

        LogisticRegressionTraining.FraudData fraudData = logisticRegressionTraining.processTransactionData(list);//extract features and labels
        INDArray arr = Nd4j.create(fraudData.features);

        int[] label = fraudData.labels;


        INDArray labels = FeatureUtil.toOutcomeMatrix(label, 2);

        DataSet dataset = new DataSet(arr, labels);//create dataset
        normalizer.fit(dataset);
        normalizer.transform(dataset);
        return dataset;

    }
    public void initModel() throws Exception {
        DataSet dataSet = createDataset();//initilize and normalise data

        List<String[]> list = logisticRegressionTraining.readCsvFile(CSV_PATH);
        LogisticRegressionTraining.FraudData fraudData = logisticRegressionTraining.processTransactionData(list);
        network.init();
        network.fit(dataSet);

        Evaluation eval= new Evaluation(2);

        INDArray iLabels = FeatureUtil.toOutcomeMatrix(fraudData.labels, 2);


        INDArray output = network.output(dataSet.getFeatures());
        eval.eval(iLabels, output);
        System.out.println(eval.stats());
    }



}
