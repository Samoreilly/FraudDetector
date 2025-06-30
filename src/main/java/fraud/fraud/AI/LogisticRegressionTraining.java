package fraud.fraud.AI;

import com.opencsv.CSVReader;
import org.springframework.stereotype.Service;
import smile.classification.LogisticRegression;
import smile.data.DataFrame;
import smile.io.Read;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class LogisticRegressionTraining {

    private static final String CSV_PATH = "/home/sam-o-reilly/IdeaProjects/FraudDetector/csv/trans.csv";
    private LogisticRegression model;

    public static class FraudData {
        public final double[][] features;
        public final int[] labels;

        public FraudData(double[][] features, int[] labels) {
            this.features = features;
            this.labels = labels;
        }
    }

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

        int dataSize = rawData.size() - 1;
        double[][] features = new double[dataSize][5];
        int[] labels = new int[dataSize];

        for (int i = 1; i < rawData.size(); i++) {
            String[] row = rawData.get(i);
            int index = i - 1;

            try {
                //split into features which is the data that matters and a label which is the result
                features[index][0] = Double.parseDouble(row[1]); // amount
                features[index][1] = Double.parseDouble(row[2]);// time
                features[index][2] = ipToDouble(row[3]);// client ip
                features[index][3] = Double.parseDouble(row[4]); // latitude
                features[index][4] = Double.parseDouble(row[5]); // longitude

                labels[index] = Integer.parseInt(row[6]); // isFraud - result

            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.err.println("Error parsing row " + i + ": " + e.getMessage());
                System.err.println("Row content: " + java.util.Arrays.toString(row));
                continue;
            }
        }

        return new FraudData(features, labels);
    }
    public static double ipToDouble(String ip) {
        String[] parts = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = result << 8;
            result |= Integer.parseInt(parts[i]);
        }
        return (double) result;
    }


    public void trainModel() throws Exception {
        List<String[]> rawData = readCsvFile(CSV_PATH);
        FraudData data = processTransactionData(rawData);

        this.model = LogisticRegression.fit(data.features, data.labels);//train with logistic regression

        System.out.println("Model trained successfully with " + data.features.length + " samples");
    }



    //predict model based off transaction data -- i will add more later
    public boolean predictFraud(double amount, double time, String clientIp, double latitude, double longitude) {
        if (model == null) {
            throw new IllegalStateException("Model not trained yet. Call trainModel() first.");
        }
        double convertedIp = ipToDouble(clientIp);
        double[] features = {amount, time, convertedIp, latitude, longitude};
        int prediction = model.predict(features);

        return prediction == 1; // assuming 1 = fraud, 0 = legitimate
    }

    public double getFraudProbability(double amount, double time, String clientIp, double latitude, double longitude) {
        if (model == null) {
            throw new IllegalStateException("Model not trained yet. Call trainModel() first.");
        }
        double convertedIp = ipToDouble(clientIp);

        double[] features = {amount,time, convertedIp, latitude, longitude};
        double[] probabilities = new double[2];
        model.predict(features, probabilities);

        return probabilities[1];
    }

}