package fraud.fraud.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fraud.fraud.entitys.Threat;

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
    public int isFraud;
    public Threat flagged;

    public int getIsFraud() {
        return isFraud;
    }

    public void setIsFraud(int isFraud) {
        this.isFraud = isFraud;
    }

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

    public Threat isFlagged() {
        return flagged;
    }

    public void setFlagged(Threat flagged) {
        this.flagged = flagged;
    }

    public TransactionRequest() {

    }
    public TransactionRequest(String id, String data, LocalDateTime time, String clientIp, String result,  Double latitude, Double longitude, int isFraud, Threat flagged) {
        this.id = id;//0
        this.data = data;//1
        this.time = time;//2
        this.clientIp = clientIp;//3
        this.result = result;//4
        this.latitude = latitude;//5
        this.longitude = longitude;//6
        this.isFraud = isFraud;//7
        this.flagged = flagged;
    }
}
