package fraud.fraud.DTO;

// wrapper class to receive encrypted data from frontend
public class EncryptedRequest {
    public String encryptedData;

    public EncryptedRequest() {}

    public EncryptedRequest(String encryptedData) {
        this.encryptedData = encryptedData;
    }

    public String getEncryptedData() {
        return encryptedData;
    }

    public void setEncryptedData(String encryptedData) {
        this.encryptedData = encryptedData;
    }
}


