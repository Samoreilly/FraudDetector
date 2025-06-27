package fraud.fraud.services;
import net.vpnblocker.api.*;
import org.springframework.stereotype.Service;

import java.io.IOException;

//https://github.com/faiqsohail/Java-VPNDetection

@Service
public class TransactionSecurityCheck {

    VPNDetection vpnDetection = new VPNDetection();

    public boolean checkVpn(String ip) throws IOException {

        if(ip != null) {
            Boolean isVPN = new VPNDetection().getResponse(ip).hostip;
            System.out.println("IS VPN " + isVPN);
            return isVPN;
        }else{
            return false;
        }
    }
}
