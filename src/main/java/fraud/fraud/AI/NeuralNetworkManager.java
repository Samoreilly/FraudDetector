package fraud.fraud.AI;

import fraud.fraud.services.TransactionService;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.util.FeatureUtil;
import org.nd4j.shade.guava.primitives.Ints;
import org.nd4j.linalg.dataset.DataSet;
import smile.data.Dataset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NeuralNetworkManager {

    private static final String CSV_PATH = "/home/sam-o-reilly/IdeaProjects/FraudDetector/csv/trans.csv";
    private final LogisticRegressionTraining logisticRegressionTraining;

    public NeuralNetworkManager(LogisticRegressionTraining logisticRegressionTraining) {
        this.logisticRegressionTraining = logisticRegressionTraining;
    }

    public void createDataset() throws Exception {

        List<String[]> list = logisticRegressionTraining.readCsvFile(CSV_PATH);//get data

        LogisticRegressionTraining.FraudData fraudData = logisticRegressionTraining.processTransactionData(list);//extract features and labels
        INDArray arr = Nd4j.create(fraudData.features);

        int[] label = fraudData.labels;


        INDArray labels= FeatureUtil.toOutcomeMatrix(label, 2);

        DataSet dataset = new DataSet(arr, labels);//create dataset
        DataNormalization normalizer = new NormalizerStandardize();//normalise data to get rid of extremely high high's and low low's
        normalizer.fit(dataset);
        normalizer.transform(dataset);

    }
    public void initModel(){
        MultiLayerNetwork network = new MultiLayerNetwork(conf);
    }



}
