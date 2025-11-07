package fraud.fraud.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fraud.fraud.DTO.TransactionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
    * encrypts and decrypts data using AES/GCM encryption
    * this is important because the data is sensitive
 */
@Service
public class RedisEncryptionService {


    //stored in application.properties for security reasons
    @Value("${encryption-key}")
    private String KEY;

    private final ObjectMapper mapper;

    public RedisEncryptionService() {
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }


    public String encryptForRedis(TransactionRequest transaction) throws Exception {
        // convert transaction to JSON string
        String json = mapper.writeValueAsString(transaction);
        
        // encrypt the JSON string
        return encryptData(json);
    }

    //
    public TransactionRequest decryptFromRedis(String encryptedBase64) throws Exception {
   
        String decryptedJson = decryptData(encryptedBase64);
        
        // convert
        return mapper.readValue(decryptedJson, TransactionRequest.class);
    }

    /**
     * Encrypts data using AES/GCM with a random IV.
     */
    private String encryptData(String data) throws Exception {
        // generate iv for randomness in encryption
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[12];
        random.nextBytes(iv);

        // create key from master key using SHA-256
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(KEY.getBytes("UTF-8"));
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        // encrypt
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));

        // encode to base64
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * decrypts data using AES/GCM .
     */
    private String decryptData(String encryptedBase64) throws Exception {
        // decode encoded data
        byte[] allBytes = Base64.getDecoder().decode(encryptedBase64);

        // extract IV and encrypted data
        byte[] iv = new byte[12];
        byte[] encrypted = new byte[allBytes.length - 12];
        System.arraycopy(allBytes, 0, iv, 0, 12);
        System.arraycopy(allBytes, 12, encrypted, 0, encrypted.length);

        // Create key with sha-256
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(KEY.getBytes("UTF-8"));
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        // decrypt
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
        byte[] decrypted = cipher.doFinal(encrypted);

        return new String(decrypted, "UTF-8");
    }
}

