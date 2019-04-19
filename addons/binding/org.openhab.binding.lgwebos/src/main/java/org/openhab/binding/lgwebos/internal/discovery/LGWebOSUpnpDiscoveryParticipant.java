package org.openhab.binding.lgwebos.internal.discovery;

import static org.openhab.binding.lgwebos.internal.LGWebOSBindingConstants.*;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.upnp.UpnpDiscoveryParticipant;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.jupnp.UpnpService;
import org.jupnp.model.message.header.ServiceTypeHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.types.ServiceType;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryListener;
import org.openhab.binding.lgwebos.internal.LGWebOSBindingConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;

@NonNullByDefault
@Component(immediate = true, configurationPid = "binding.lgwebos.upnp")
public class LGWebOSUpnpDiscoveryParticipant implements UpnpDiscoveryParticipant, RegistryListener {

    private final Logger logger = LoggerFactory.getLogger(LGWebOSUpnpDiscoveryParticipant.class);

    @NonNullByDefault({})
    private UpnpService upnpService;

    private final ServiceType serviceType = new ServiceType("lge-com", "webos-second-screen", 1);

    @NonNullByDefault({})
    private ScheduledExecutorService scheduler;

    @Reference
    public void setUpnpService(UpnpService upnpService) {
        this.upnpService = upnpService;
    }

    public void unsetUpnpService(UpnpService upnpService) {
        this.upnpService = null;
    }

    @Activate
    private void activate() {
        scheduler = ThreadPoolManager.getScheduledPool("LG WebOS Discovery");
        scheduler.scheduleAtFixedRate(() -> search(), 0, 10, TimeUnit.SECONDS);

        upnpService.getRegistry().addListener(this);
    }

    @Deactivate
    private void deactivate() {
        upnpService.getRegistry().removeListener(this);
        scheduler.shutdown();
        scheduler = null;
    }

    @Override
    public Set<@NonNull ThingTypeUID> getSupportedThingTypeUIDs() {
        return LGWebOSBindingConstants.SUPPORTED_THING_TYPES_UIDS;
    }

    @Override
    public @Nullable DiscoveryResult createResult(RemoteDevice device) {
        ThingUID thingUID = getThingUID(device);

        if (thingUID == null) {
            return null;
        }

        return DiscoveryResultBuilder.create(thingUID).withLabel(device.getDetails().getFriendlyName())
                .withProperty(PROPERTY_DEVICE_ID, device.getIdentity().getUdn().getIdentifierString())
                .withProperty(PROPERTY_DEVICE_HOST, device.getIdentity().getDescriptorURL().getHost())
                .withRepresentationProperty(PROPERTY_DEVICE_ID).build();
    }

    @Override
    public @Nullable ThingUID getThingUID(RemoteDevice device) {
        /*
         * "LG Smart TV" with service type urn:lge-com:service:webos-second-screen:1
         * does not automatically show up by the normal background scans.
         * an actual ssdp search request is required first.
         *
         * the device does announce itself via broadcasts as:
         * device:
         * type= urn:schemas-upnp-org:device:MediaRenderer:1
         * UDADeviceType
         * with the following services:
         * urn:schemas-upnp-org:service:AVTransport:1
         * urn:schemas-upnp-org:service:ConnectionManager:1
         * urn:schemas-upnp-org:service:RenderingControl:1
         * modelName "LG TV"
         * but those devices have different UDNs as well
         */

        if (device.findService(serviceType) != null) {
            logger.debug("found MATCHING device {}", device);
            // || "LG TV".equals(device.getDetails().getModelDetails().getModelName())) {
            return new ThingUID(THING_TYPE_WEBOSTV, device.getIdentity().getUdn().getIdentifierString());
        } else {
            logger.debug("found non matching device {}", device);
        }

        return null;
    }

    private void search() {
        upnpService.getControlPoint().search(new ServiceTypeHeader(serviceType));
    }

    // RegistryListener
    @Override
    public void afterShutdown() {

    }

    @Override
    public void beforeShutdown(@Nullable Registry arg0) {

    }

    @Override
    public void localDeviceAdded(@Nullable Registry arg0, @Nullable LocalDevice device) {
        logger.debug("local device added {}", device);
    }

    @Override
    public void localDeviceRemoved(@Nullable Registry arg0, @Nullable LocalDevice device) {
        logger.debug("local device removed {}", device);

    }

    @Override
    public void remoteDeviceAdded(@Nullable Registry arg0, @Nullable RemoteDevice device) {
        logger.debug("remote device added {}", device);
        RemoteService remoteService = device.findService(serviceType);
        if (remoteService != null) {
            ServiceDescription service = new ServiceDescription();
            service.setUUID(remoteService.getServiceId().toString()); // TODO is this a UUID?
            service.setServiceFilter(remoteService.getServiceType().toString());
            service.setFriendlyName(device.getDetails().getFriendlyName());
            service.setModelName(device.getDetails().getModelDetails().getModelName());
            service.setModelNumber(device.getDetails().getModelDetails().getModelNumber());
            service.setModelDescription(device.getDetails().getModelDetails().getModelDescription());
            service.setManufacturer(device.getDetails().getManufacturerDetails().getManufacturer());
            service.setIpAddress(device.getIdentity().getDescriptorURL().getHost());
            remoteService.getDevice().getVersion().toString(); // TODO compare this with the connect sdk version
                                                               // handling
            // service.setVersion(version); // TODO
            WebOSTVService webOSTVService = new WebOSTVService(service, new ServiceConfig(service));

            // TODO: need to store it so that handlers can retrieve it.
            // TODO: need to persist secrets,which are currently in StoredDecives

            // TODO: or register this instead of SSDPDiscoveryProvider in DiscoveryManager

            // devicesList.put("com.connectsdk.service.WebOSTVService",
            // "com.connectsdk.discovery.provider.SSDPDiscoveryProvider");

        }
    }

    @Override
    public void remoteDeviceDiscoveryFailed(@Nullable Registry arg0, @Nullable RemoteDevice arg1,
            @Nullable Exception arg2) {
    }

    @Override
    public void remoteDeviceDiscoveryStarted(@Nullable Registry arg0, @Nullable RemoteDevice arg1) {
    }

    @Override
    public void remoteDeviceRemoved(@Nullable Registry arg0, @Nullable RemoteDevice device) {
        logger.debug("remote device removed {}", device); // <-- TODO mark offline
    }

    @Override
    public void remoteDeviceUpdated(@Nullable Registry arg0, @Nullable RemoteDevice device) {
        logger.debug("remote device updated {}", device);

        // TODO add timeout, so that device disappears if unseen for a while (even if it did not send byebye maybe due
        // to sudden power loss)
    }
}
