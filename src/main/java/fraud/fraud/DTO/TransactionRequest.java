package fraud.fraud.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionRequest implements Serializable {

    public String id;
    public String data;
    public LocalDateTime time;
    public String clientIp;
    public String result;
    public Double latitude;
    public Double longitude;

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
    public void setTime(LocalDateTime time) {
        this.time = time;
    }
    public LocalDateTime getTime() {
        return time;
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

    public void setLongitude(Double lng) {
        this.longitude = lng;
    }
    public TransactionRequest() {

    }
    public TransactionRequest(String id, String data, LocalDateTime time, String clientIp, String result,  Double latitude, Double longitude) {
        this.id = id;
        this.data = data;
        this.time = time;
        this.clientIp = clientIp;
        this.result = result;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
