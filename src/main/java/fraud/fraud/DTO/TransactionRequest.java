package fraud.fraud.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionRequest implements Serializable {

    public String id;
    public String data;
    public LocalDateTime time;

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
    public TransactionRequest() {

    }
    public TransactionRequest(String id, String data, LocalDateTime time) {
        this.id = id;
        this.data = data;
        this.time = time;
    }
}
