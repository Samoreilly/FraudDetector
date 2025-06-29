package fraud.fraud.AI;

import com.opencsv.CSVReader;
import fraud.fraud.DTO.TransactionRequest;
import org.springframework.stereotype.Service;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class LogisticRegression {

    public List<String[]> readLineByLine(Path filePath) throws Exception {
        List<String[]> list = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(filePath)) {
            try (CSVReader csvReader = new CSVReader(reader)) {
                String[] line;
                while ((line = csvReader.readNext()) != null) {
                    list.add(line);
                }
            }
        }
        assignFeaturesAndLabels(list);
        return list;
    }
    public List<String[]> readLineByLineExample() throws Exception {
        Path path = Paths.get("/home/sam-o-reilly/IdeaProjects/FraudDetector/csv/trans.csv");
        return readLineByLine(path);
    }
    public void assignFeaturesAndLabels(List<String[]> list) throws Exception {
        int n = list.size() - 1;
        double[][] features = new double[n][3];
        int[] labels = new int[n];


        for (int i = 1; i < list.size(); i++) { // start from 1 to skip header
            String[] t = list.get(i);

            // Features:
            features[i - 1][0] = Double.parseDouble(t[1]);
            features[i - 1][1] = Double.parseDouble(t[5]);
            features[i - 1][2] = Double.parseDouble(t[6]);

            // Label:
            labels[i - 1] = Integer.parseInt(t[6]);         // isFraud
        }
        for (int i = 0; i < n; i++) {
            System.out.printf("Features: %.2f, %.4f, %.4f | Label: %d\n",
                    features[i][0], features[i][1], features[i][2], labels[i]);
        }
    }
}
