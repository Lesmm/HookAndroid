package common.modules.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.content.Context;

public class INetworkUtil {

    // copy from android.net.NetworkUtils.java
    // also, you can use java Reflect to implement
    /**
     * Convert a IPv4 address from an integer to an InetAddress.
     * @param hostAddress an int corresponding to the IPv4 address in network byte order
     */
    public static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Convert a IPv4 address from an InetAddress to an integer
     * @param inetAddr is an InetAddress corresponding to the IPv4 address
     * @return the IP address as an integer in network byte order
     */
    public static int inetAddressToInt(Inet4Address inetAddr)
            throws IllegalArgumentException {
        byte [] addr = inetAddr.getAddress();
        return ((addr[3] & 0xff) << 24) | ((addr[2] & 0xff) << 16) |
                ((addr[1] & 0xff) << 8) | (addr[0] & 0xff);
    }


    public static String intToHostAddress(int hostAddress) {
        InetAddress address = INetworkUtil.intToInetAddress(hostAddress);
        if (address != null) {
            return address.getHostAddress();
        }
        return null;
    }
    public static String intToHostAddress2(int ip) {
        return String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
    }

    public static int hostAddressToInt(String hostAddress) {
        try {
            Inet4Address address = (Inet4Address)Inet4Address.getByName(hostAddress);
            if (address != null) {
                return inetAddressToInt(address);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 755214528 -> 192.168.3.45; 17017024 -> 192.168.3.1
    public static void grepIpToInt(Context context) {
        String filename = context.getFilesDir().getAbsolutePath() + "/IpToInt.json";
        IFileUtil.appendTextToFile("{", filename);
        for (int i = 1; i <= 255; i++) {
            for (int j = 1; j <= 255; j++) {
//                if (i == 1 && j == 1) {
//                    continue;
//                }
                String ipString = "192.168." + i +"." + j;
                int intAddress = INetworkUtil.hostAddressToInt(ipString);
                String log = "\"" + ipString + "\"" + ":" + intAddress ;

                if (!(i == 255 && j == 255)) {
                    log = log + ",";
                }

                log = log + "\r\n";
                IFileUtil.appendTextToFile(log, filename);
            }
        }
        IFileUtil.appendTextToFile("}", filename);
    }


}
