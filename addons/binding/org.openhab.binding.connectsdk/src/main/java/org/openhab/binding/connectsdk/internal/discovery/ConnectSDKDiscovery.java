package org.openhab.binding.connectsdk.internal.discovery;

import static org.openhab.binding.connectsdk.ConnectSDKBindingConstants.*;

import java.util.Map;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.connectsdk.ConnectSDKBindingConstants;
import org.openhab.binding.connectsdk.internal.ContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connectsdk.core.Util;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManagerListener;
import com.connectsdk.service.command.ServiceCommandError;

//TODO: openhab> logout or bundle:stop <>
// causes
//java.net.SocketException: Socket closed
//    at java.net.PlainDatagramSocketImpl.receive0(Native Method)
//    at java.net.AbstractPlainDatagramSocketImpl.receive(AbstractPlainDatagramSocketImpl.java:143)
//    at java.net.DatagramSocket.receive(DatagramSocket.java:812)
//    at com.connectsdk.discovery.provider.ssdp.SSDPClient.multicastReceive(SSDPClient.java:109)
//    at com.connectsdk.discovery.provider.SSDPDiscoveryProvider$2.run(SSDPDiscoveryProvider.java:268)
//    at java.lang.Thread.run(Thread.java:745)

public class ConnectSDKDiscovery extends AbstractDiscoveryService implements DiscoveryManagerListener {
    private static final Logger logger = LoggerFactory.getLogger(ConnectSDKDiscovery.class);

    private DiscoveryManager discoveryManager;

    // optional local ip, can be configured through config admin
    private String localIP;

    public ConnectSDKDiscovery() {
        super(ConnectSDKBindingConstants.SUPPORTED_THING_TYPES_UIDS, 60, true);
        ContextImpl ctx = new ContextImpl(null);// TODO: .. move this to activate method
        DiscoveryManager.init(ctx); // TODO: .. move this to activate method, give handler reference to this instance,
                                    // so that they can query discovermanager
    }

    @Override
    protected void activate(Map<String, Object> configProperties) {
        logger.info(configProperties.toString());
        Util.start();
        discoveryManager = DiscoveryManager.getInstance();
        discoveryManager.setPairingLevel(DiscoveryManager.PairingLevel.ON);
        discoveryManager.addListener(this);

        super.activate(configProperties); // starts background discovery
    }

    @Override
    protected void deactivate() {
        super.deactivate(); // stops background discovery

        discoveryManager.removeListener(this);
        discoveryManager = null;
        DiscoveryManager.destroy();
        Util.stop();
    }

    public DiscoveryManager getDiscoveryManager() {
        return discoveryManager;
    }

    @Override
    protected void startScan() {
        // no adhoc scanning. Discovery Service runs in background
    }

    @Override
    protected void startBackgroundDiscovery() {
        discoveryManager.start();
    }

    @Override
    protected void stopBackgroundDiscovery() {
        discoveryManager.stop();
    }

    // DiscoveryManagerListener

    @Override
    public void onDeviceAdded(DiscoveryManager manager, ConnectableDevice device) {
        thingDiscovered(createDiscoveryResult(device));
    }

    @Override
    public void onDeviceUpdated(DiscoveryManager manager, ConnectableDevice device) {
        logger.info("Device updated: {}", device);
        thingRemoved(createThingUID(device));
        thingDiscovered(createDiscoveryResult(device));
    }

    @Override
    public void onDeviceRemoved(DiscoveryManager manager, ConnectableDevice device) {
        logger.info("Device removed: {}", device);
        thingRemoved(createThingUID(device));
    }

    @Override
    public void onDiscoveryFailed(DiscoveryManager manager, ServiceCommandError error) {
        logger.warn("Discovery Failed {}", error.getMessage());

    }

    // Helpers for DiscoveryManagerListener Impl
    private DiscoveryResult createDiscoveryResult(ConnectableDevice device) {
        ThingUID thingUID = createThingUID(device);
        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID)
                .withProperty(PROPERTY_IP_ADDRESS, device.getIpAddress()).withLabel(device.getFriendlyName()).build();
        return result;
    }

    private ThingUID createThingUID(ConnectableDevice device) {
        return new ThingUID(THING_TYPE_WebOSTV, device.getIpAddress().replace('.', '_'));
    }

}
