package fraud.fraud.AI;

import fraud.fraud.AI.randomaimethodologiesandotherstufflol.FilterTransactions;
import fraud.fraud.services.TransactionService;
import fraud.fraud.services.ValidateTransactions;
import jakarta.annotation.PostConstruct;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
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

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class NeuralNetworkManager {

    private static final String CSV_PATH = "/home/sam-o-reilly/IdeaProjects/FraudDetector/csv/trans.csv";
    private final LogisticRegressionTraining logisticRegressionTraining;
    private final ValidateTransactions validateTransactions;
    private final FilterTransactions filterTransactions;

    DataNormalization normalizer = new NormalizerStandardize();//normalise data to get rid of extremely high high's and low low's

    public NeuralNetworkManager(LogisticRegressionTraining logisticRegressionTraining, ValidateTransactions validateTransactions, FilterTransactions filterTransactions) {
        this.logisticRegressionTraining = logisticRegressionTraining;
        this.validateTransactions = validateTransactions;
        this.filterTransactions = filterTransactions;

    }

    public MultiLayerNetwork getNetwork() {
        return network;
    }

    public DataNormalization getNormalizer() {
        return normalizer;
    }
    @PostConstruct
    public void init() throws Exception {
        initModel();
    }

    MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(123)
            .updater(new Adam(0.001))
            .l2(0.001)
            .list()
            .layer(new DenseLayer.Builder()
                    .nIn(6)
                    .nOut(50)
                    .activation(Activation.RELU)
                    .dropOut(0.4)
                    .build())
            .layer(new DenseLayer.Builder()
                    .nIn(50)
                    .nOut(50)
                    .activation(Activation.RELU)
                    .dropOut(0.4)
                    .build())
            .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                    .nIn(50)
                    .nOut(2)
                    .activation(Activation.SOFTMAX)
                    .weightInit(WeightInit.XAVIER)
                    .build())
            .build();

    MultiLayerNetwork network = new MultiLayerNetwork(conf);

    public DataSet createDataset() throws Exception {
        List<String[]> list = logisticRegressionTraining.readCsvFile(CSV_PATH);
        for(int i = 0; i < list.size(); i++) {
            System.out.println("STR" + Arrays.toString(list.get(i)));//READING CSV FILE IS GOOD
        }
        LogisticRegressionTraining.FraudData fraudData = filterTransactions.filterData(list);//new method to extract features and labels

        double[][] rawFeatures = fraudData.features;
        int[] labels = fraudData.labels;
        List<double[]> balancedFeatures = new ArrayList<>();
        List<Integer> balancedLabels = new ArrayList<>();

        for (int i = 0; i < rawFeatures.length; i++) {
            System.out.println("RAW" + rawFeatures[i][0]);
            double latitude = rawFeatures[i][2];
            double longitude = rawFeatures[i][3];

            double amount = Math.min(rawFeatures[i][0], 1000000.0);


            if(amount < 0) System.out.println("amount is negativeamount is negativeamount is negativeamount is negativeamount is negativeamount is negativeamount is negativeamount is negativeamount is negativeamount is negativeamount is negativeamount is negativeamount is negativeamount is negative");

            long epochSeconds = (long) rawFeatures[i][1];

            Instant instant = Instant.ofEpochSecond(epochSeconds);
            int hourOfDay = instant.atZone(ZoneOffset.UTC).getHour();
            int dayOfWeek = instant.atZone(ZoneOffset.UTC).getDayOfWeek().getValue();

            double distance = validateTransactions.calculateDistance(latitude, longitude, 26.7447058687939, 14.835507388130088);

            double[] transformed = new double[] {
                    Math.log1p(amount),
                    hourOfDay,
                    dayOfWeek,
                    latitude,
                    longitude,
                    distance
            };

            balancedFeatures.add(transformed);
            balancedLabels.add(labels[i]);

            if (labels[i] == 1) {
                for (int j = 0; j < (int) Math.round(1.83 - 1); j++) {
                    balancedFeatures.add(transformed);
                    balancedLabels.add(labels[i]);
                }
            }
        }

        double[][] transformedFeatures = balancedFeatures.toArray(new double[0][]);
        int[] balancedLabelArray = balancedLabels.stream().mapToInt(i -> i).toArray();
        INDArray features = Nd4j.create(transformedFeatures);
        INDArray labelArray = FeatureUtil.toOutcomeMatrix(balancedLabelArray, 2);

        System.out.printf("Balanced counts → Legit: %d, Fraud: %d%n",
                Arrays.stream(balancedLabelArray).filter(i -> i == 0).count(),
                Arrays.stream(balancedLabelArray).filter(i -> i == 1).count());

        DataSet dataset = new DataSet(features, labelArray);
        normalizer.fit(dataset);
        normalizer.transform(dataset);
        return dataset;
    }

    public void initModel() throws Exception {
        DataSet dataSet = createDataset();
        network.init();
        SplitTestAndTrain trainTest = dataSet.splitTestAndTrain(0.8);

        int numEpochs = 100;
        Evaluation eval = new Evaluation(2);
        for (int i = 0; i < numEpochs; i++) {
            network.fit(trainTest.getTrain());
            INDArray testOutput = network.output(trainTest.getTest().getFeatures());
            eval.eval(trainTest.getTest().getLabels(), testOutput);
            System.out.println("Epoch " + i + ": Fraud Precision=" + eval.precision(1) +
                    ", Fraud Recall=" + eval.recall(1));
            if (eval.precision(1) > 0.85) break;
        }

        System.out.println("Confusion Matrix:\n" + eval.confusionToString());
        System.out.println("Fraud Precision: " + eval.precision(1));
        System.out.println("Fraud Recall: " + eval.recall(1));
        System.out.println("Fraud F1-Score: " + eval.f1(1));
    }



}
