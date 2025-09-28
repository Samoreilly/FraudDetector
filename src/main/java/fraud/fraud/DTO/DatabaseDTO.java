package fraud.fraud.DTO;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class DatabaseDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String autoId;// unique for each DLQ entry

    private String id;
    private String data;
    private LocalDateTime time;
    private String clientIp;
    private String result;
    private Double latitude;
    private Double longitude;
    private int isFraud;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public int getIsFraud() {
        return isFraud;
    }
    @Override
    public String toString() {
        return "DatabaseDTO{" +
                "id='" + id + '\'' +
                ", data='" + data + '\'' +
                ", time=" + time +
                ", clientIp='" + clientIp + '\'' +
                ", result='" + result + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", isFraud=" + isFraud +
                '}';
    }
    public void setIsFraud(int isFraud) {
        this.isFraud = isFraud;
    }
    public DatabaseDTO(){

    }


    public DatabaseDTO(String id, String data, LocalDateTime time, String clientIp, String result, Double latitude, Double longitude, int isFraud) {
        this.id = id;
        this.data = data;
        this.time = time;
        this.clientIp = clientIp;
        this.result = result;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isFraud = isFraud;
    }
}
