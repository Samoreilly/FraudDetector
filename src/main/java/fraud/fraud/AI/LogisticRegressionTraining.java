package fraud.fraud.AI;

import com.opencsv.CSVReader;
import org.springframework.stereotype.Service;
import smile.classification.LogisticRegression;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class LogisticRegressionTraining {

    private static final String CSV_PATH = "/home/sam-o-reilly/IdeaProjects/FraudDetector/csv/trans.csv";
    private LogisticRegression model;

    private double[] featureMeans;
    private double[] featureStds;

    public static class FraudData {
        public final double[][] features;
        public final int[] labels;

        public FraudData(double[][] features, int[] labels) {
            this.features = features;
            this.labels = labels;
        }
    }
    //read in csv model data
    public List<String[]> readCsvFile(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        List<String[]> records = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(path);
             CSVReader csvReader = new CSVReader(reader)) {

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                records.add(line);
            }
        }
        return records;
    }

    public FraudData processTransactionData(List<String[]> rawData) {
        if (rawData.isEmpty()) {
            throw new IllegalArgumentException("No data provided");
        }

        List<double[]> validFeatures = new ArrayList<>();
        List<Integer> validLabels = new ArrayList<>();

        for (int i = 1; i < rawData.size(); i++) {
            String[] row = rawData.get(i);

            try {
                if (row.length < 6) {
                    System.err.println("Skipping incomplete row " + i);
                    continue;
                }

                double amount = Double.parseDouble(row[1].trim());
                double time = Double.parseDouble(row[2].trim());
                double latitude = Double.parseDouble(row[3].trim());
                double longitude = Double.parseDouble(row[4].trim());
                int label = Integer.parseInt(row[5].trim());

                validFeatures.add(new double[]{amount, time, latitude, longitude});
                validLabels.add(label);

            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.err.println("Error parsing row " + i + ": " + e.getMessage());
                System.err.println("Row content: " + Arrays.toString(row));
            }
        }

        double[][] features = validFeatures.toArray(new double[validFeatures.size()][]);
        int[] labels = validLabels.stream().mapToInt(Integer::intValue).toArray();

        features = normalizeFeatures(features);

        System.out.println("processed " + features.length + " valid samples");
        System.out.println("Feature meanss " + Arrays.toString(featureMeans));
        System.out.println("Feature stds: " + Arrays.toString(featureStds));

        return new FraudData(features, labels);
    }

    private double[][] normalizeFeatures(double[][] features) {
        if (features.length == 0) return features;

        int numFeatures = features[0].length;
        featureMeans = new double[numFeatures];
        featureStds = new double[numFeatures];

        for (int j = 0; j < numFeatures; j++) {
            double sum = 0;
            for (int i = 0; i < features.length; i++) {
                sum += features[i][j];
            }
            featureMeans[j] = sum / features.length;
        }

        for (int j = 0; j < numFeatures; j++) {
            double sumSquaredDiffs = 0;
            for (int i = 0; i < features.length; i++) {
                double diff = features[i][j] - featureMeans[j];
                sumSquaredDiffs += diff * diff;
            }
            featureStds[j] = Math.sqrt(sumSquaredDiffs / features.length);

            // Avoid division by zero
            if (featureStds[j] == 0) {
                featureStds[j] = 1.0;
            }
        }

        double[][] normalizedFeatures = new double[features.length][numFeatures];
        for (int i = 0; i < features.length; i++) {
            for (int j = 0; j < numFeatures; j++) {
                normalizedFeatures[i][j] = (features[i][j] - featureMeans[j]) / featureStds[j];
            }
        }

        return normalizedFeatures;
    }

    private double[] normalizeInput(double[] input) {
        if (featureMeans == null || featureStds == null) {
            throw new IllegalStateException("Feature normalization parameters not available. Train model first.");
        }

        double[] normalized = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            normalized[i] = (input[i] - featureMeans[i]) / featureStds[i];
        }
        return normalized;
    }

    public void trainModel() throws Exception {
        List<String[]> rawData = readCsvFile(CSV_PATH);
        FraudData data = processTransactionData(rawData);
        System.out.println("Training data summary:");
        System.out.println("Total samples: " + data.features.length);

        int fraudCount = 0;
        int legitCount = 0;
        for (int label : data.labels) {
            if (label == 1) fraudCount++;
            else legitCount++;
        }

        System.out.println("Fraud samples: " + fraudCount);
        System.out.println("Legitimate samples: " + legitCount);
        System.out.println("Class balance: " + (double)fraudCount / data.labels.length);

        this.model = LogisticRegression.fit(data.features, data.labels);

        System.out.println("Model trained successfully!");
        testModelOnTrainingData(data);
    }

    private void testModelOnTrainingData(FraudData data) {
        System.out.println("\nTesting model on first 10 training samples:");
        for (int i = 0; i < Math.min(10, data.features.length); i++) {
            double[] probabilities = new double[2];
            int prediction = model.predict(data.features[i]);
            model.predict(data.features[i], probabilities);

            System.out.printf("Sample %d: Actual=%d, Predicted=%d, Prob=%.3f\n",
                    i, data.labels[i], prediction, probabilities[1]);
        }
    }

    public boolean predictFraud(double amount, double time, double latitude, double longitude) {
        if (model == null) {
            throw new IllegalStateException("Model not trained yet. Call trainModel() first.");
        }

        double[] features = {amount, time, latitude, longitude};
        double[] normalizedFeatures = normalizeInput(features);
        int prediction = model.predict(normalizedFeatures);

        return prediction == 1;
    }

    public double getFraudProbability(double amount, double time, double latitude, double longitude) {
        if (model == null) {
            throw new IllegalStateException("Model not trained yet. Call trainModel() first.");
        }

        double[] features = {amount, time, latitude, longitude};
        double[] normalizedFeatures = normalizeInput(features);
        double[] probabilities = new double[2];
        model.predict(normalizedFeatures, probabilities);

        return probabilities[1];
    }
}