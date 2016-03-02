package android.net.wifi;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.openhab.binding.connectsdk.internal.ConnectSDKHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WifiInfo
// implements android.os.Parcelable
{
    private static final Logger logger = LoggerFactory.getLogger(ConnectSDKHandlerFactory.class);
    private final ConnectSDKHandlerFactory binding;

    WifiInfo(ConnectSDKHandlerFactory binding) {
        this.binding = binding;
    }

    public java.lang.String getSSID() {
        throw new RuntimeException("Stub!");
    }

    public java.lang.String getBSSID() {
        throw new RuntimeException("Stub!");
    }

    public int getRssi() {
        throw new RuntimeException("Stub!");
    }

    public int getLinkSpeed() {
        throw new RuntimeException("Stub!");
    }

    public java.lang.String getMacAddress() {
        throw new RuntimeException("Stub!");
    }

    public int getNetworkId() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Attempts to autodetect the ip on the local network. It will ignore loopback and IPv6 addresses.
     *
     * @return local ip or <code>null</code> if detection was not possible.
     */
    private Inet4Address findLocalInetAddresses() {
        // try to find IP via Java method (one some systems this returns the loopback interface though
        try {
            final InetAddress inetAddress = InetAddress.getLocalHost();
            if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                logger.debug("Autodetected (via getLocalHost) local IP: {}", inetAddress);
                return (Inet4Address) inetAddress;
            }
        } catch (UnknownHostException ex) {
            logger.warn("Unable to resolve your hostname", ex);
        }

        // try to find the single non-loop back interface available
        final List<Inet4Address> interfaces = new ArrayList<Inet4Address>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    for (InterfaceAddress adr : networkInterface.getInterfaceAddresses()) {
                        InetAddress inadr = adr.getAddress();
                        if (inadr instanceof Inet4Address) {
                            interfaces.add((Inet4Address) inadr);
                        }
                    }
                }
            }

            if (interfaces.size() == 1) { // found exactly one interface, good
                logger.debug("Autodetected (via getNetworkInterfaces) local IP: {}", interfaces.get(0));
                return interfaces.get(0);
            }
        } catch (SocketException e) {
            logger.warn("Failed to detect network interfaces and addresses", e);
        }

        logger.error(
                "Your hostname resolves to a loopback address and the plugin cannot autodetect your network interface out of the following available options: {}.",
                interfaces);
        return null;
    }

    // public android.net.wifi.SupplicantState getSupplicantState() { throw new RuntimeException("Stub!"); }
    public int getIpAddress() {
        byte[] b = findLocalInetAddresses().getAddress();
        // inverse operation to Util.convertIpAddress(ip);
        return (b[3] & 0x000000ff) << 24 | (b[2] & 0x000000ff) << 16 | (b[1] & 0x000000ff) << 8 | b[0] & 0x000000ff;
    }

    public boolean getHiddenSSID() {
        throw new RuntimeException("Stub!");
    }

    // public static android.net.NetworkInfo.DetailedState getDetailedStateOf(android.net.wifi.SupplicantState
    // suppState) { throw new RuntimeException("Stub!"); }
    @Override
    public java.lang.String toString() {
        throw new RuntimeException("Stub!");
    }

    public int describeContents() {
        throw new RuntimeException("Stub!");
    }

    // public void writeToParcel(android.os.Parcel dest, int flags) { throw new RuntimeException("Stub!"); }
    public static final java.lang.String LINK_SPEED_UNITS = "Mbps";
}
