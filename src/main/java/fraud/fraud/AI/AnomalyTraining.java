package fraud.fraud.AI;

import com.opencsv.CSVReader;
import fraud.fraud.DTO.TransactionRequest;
import fraud.fraud.Notifications.NotificationService;
import org.springframework.stereotype.Service;
import smile.anomaly.IsolationForest;
import java.io.Reader;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnomalyTraining implements Serializable {

    private static final String CSV_PATH = "/home/sam-o-reilly/IdeaProjects/FraudDetector/csv/trans.csv";
    private final NotificationService notificationService;

    public AnomalyTraining(NotificationService notificationService) {
        this.notificationService = notificationService;
    }


    public List<double[]> readCsvFile(String filePath) throws Exception {
        Path path = Paths.get(filePath);

        List<double[]> records = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(path); CSVReader csvReader = new CSVReader(reader)) {

            csvReader.readNext();

            String[]line;
            while ((line = csvReader.readNext()) != null) {
                double amount = Double.parseDouble(line[1]);
                double time = Double.parseDouble(line[2]);
                double lat = Double.parseDouble(line[3]);
                double lon= Double.parseDouble(line[4]);
                records.add(new double[]{amount, time, lat, lon});

            }
        }
        return records;
    }
    public double anomalyPipeline(TransactionRequest transactionRequest) throws Exception {

        double[][] data = readCsvFile(CSV_PATH).toArray(new double[0][]);

        IsolationForest model = IsolationForest.fit(data);
        long currentEpoch = transactionRequest.getTime().toEpochSecond(ZoneOffset.UTC);
        double epochSeconds = 1719650000 + (currentEpoch % 60000);

        double[] input = new double[4];
        input[0] = Double.parseDouble(transactionRequest.getData());
        input[1] = epochSeconds;
        input[2] = transactionRequest.getLatitude();
        input[3] = transactionRequest.getLongitude();

        double fraud = model.score(input);
        System.out.println("++++++++++++++++++++++++++++FRUAD+===========================");
        System.out.println(fraud);
        notificationService.sendNotification(transactionRequest, "iso-tree" + fraud);
        return fraud;


    }

}
