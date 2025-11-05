package fraud.fraud.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fraud.fraud.DTO.TransactionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class DecryptData {

    @Value("${encryption-key}")
    private String KEY;


    ObjectMapper mapper = new ObjectMapper();
    {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    //this method decrypts the encrypted data sent from frontend
    //uses sha-256 to make our decryption key stored in application.properties for security reasons
    public String decryptData(String encryptedBase64) throws Exception {
        // decodes it into bytes
        byte[] allBytes = Base64.getDecoder().decode(encryptedBase64);

        // iv is extracted from encrypted data and the encrypted data is extracted from allBytes
        byte[] iv = new byte[12];
        byte[] encrypted = new byte[allBytes.length - 12];
        System.arraycopy(allBytes, 0, iv, 0, 12);//this copies the data from the first 12 bytes to the iv array
        System.arraycopy(allBytes, 12, encrypted, 0, encrypted.length);//this copies the data from the second 12 bytes to the encrypted array

        // these make the decryption key
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(KEY.getBytes("UTF-8"));

        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        //cipher uses decrypt mode and with gcm algorithm
        //initialization vector is used to add randomnesss/ makes same texts appear in different formats when encrypted
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, "UTF-8");
    }

    // this is a simple object mapper that casts the string to my TransactionRequest object which is used
    // all across my codebase
    public <T> T decryptToObject(String encryptedBase64, Class<T> clazz) throws Exception {
        String decryptedJson = decryptData(encryptedBase64);
        return mapper.readValue(decryptedJson, clazz);
    }
}
