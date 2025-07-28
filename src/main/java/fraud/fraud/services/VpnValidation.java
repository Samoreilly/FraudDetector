package fraud.fraud.services;

import net.vpnblocker.api.VPNDetection;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
public class VpnValidation {

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
