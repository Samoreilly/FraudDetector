package fraud.fraud.AI.randomaimethodologiesandotherstufflol;

import fraud.fraud.AI.LogisticRegressionTraining;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class FilterTransactions {

    public LogisticRegressionTraining.FraudData filterData(List<String[]> rawData) {
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
                System.out.println("yanana"+ amount);
                double time = Double.parseDouble(row[2].trim());
                double latitude = Double.parseDouble(row[3].trim());
                double longitude = Double.parseDouble(row[4].trim());
                int label = Integer.parseInt(row[5].trim());
                System.out.println("check-logrt"+ amount + " " + time + " " + latitude + " " + longitude);
                validFeatures.add(new double[]{amount, time, latitude, longitude});
                validLabels.add(label);

            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.err.println("Error parsing row " + i + ": " + e.getMessage());
                System.err.println("Row content: " + Arrays.toString(row));
            }
        }

        double[][] features = validFeatures.toArray(new double[validFeatures.size()][]);
        int[] labels = validLabels.stream().mapToInt(Integer::intValue).toArray();

        return new LogisticRegressionTraining.FraudData(features, labels);
    }

}
