package org.openhab.binding.lgwebos.internal.discovery;

import static org.openhab.binding.lgwebos.internal.LGWebOSBindingConstants.*;

import java.time.Instant;
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

/**
 * This Upnp Discovery participant add the ability to auto discover LG Web OS devices on the network.
 * Some users choose to not use upnp. Therefore this can only play an optional role and help discover the device.
 *
 * TODO: in order to detect devices without upnp we need to ping or find some other mechanism.
 */
@NonNullByDefault
@Component(immediate = true, configurationPid = "binding.lgwebos.upnp")
public class LGWebOSUpnpDiscoveryParticipant implements UpnpDiscoveryParticipant, RegistryListener {

    private final Logger logger = LoggerFactory.getLogger(LGWebOSUpnpDiscoveryParticipant.class);

    @NonNullByDefault({})
    private UpnpService upnpService;

    @NonNullByDefault({})
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
                .withProperty(PROPERTY_DEVICE_IP, device.getIdentity().getDescriptorURL().getHost())
                .withProperty("friendlyName", device.getDetails().getFriendlyName())
                .withProperty("modelName", device.getDetails().getModelDetails().getModelName())
                .withProperty("modelNumber", device.getDetails().getModelDetails().getModelNumber())
                .withProperty("descriptorUrl", device.getIdentity().getDescriptorURL())
                .withProperty("serialNumber", device.getDetails().getSerialNumber())
                .withProperty("manufacturer", device.getDetails().getManufacturerDetails().getManufacturer())
                // .withProperty("lastConnected", Instant.now().toString())
                .withProperty("lastDetection", Instant.now().toString())
                // .withProperty(PROPERTY_DEVICE_HOST, )
                .withRepresentationProperty(PROPERTY_DEVICE_ID).withThingType(THING_TYPE_WEBOSTV).build();
    }

    @Override
    public @Nullable ThingUID getThingUID(RemoteDevice device) {
        /*
         * LG WebOS TV does not automatically show up by the normal background scans.
         * A special ssdp search request for lge-com:service:webos-second-screen:1 is required first.
         */

        if (device.findService(serviceType) != null) {
            logger.debug("Found LG WebOS TV: {}", device);
            // || "LG TV".equals(device.getDetails().getModelDetails().getModelName())) {
            return new ThingUID(THING_TYPE_WEBOSTV, device.getIdentity().getUdn().getIdentifierString());
        }

        return null; // device not supported by this participant
    }

    private void search() {
        upnpService.getControlPoint().search(new ServiceTypeHeader(serviceType));
    }

    // RegistryListener
    @Override
    public void afterShutdown() {
        // intentionally empty
    }

    @Override
    public void beforeShutdown(@Nullable Registry registry) {
        // intentionally empty
    }

    @Override
    public void localDeviceAdded(@Nullable Registry registry, @Nullable LocalDevice device) {
        logger.debug("local device added {}", device);
    }

    @Override
    public void localDeviceRemoved(@Nullable Registry registry, @Nullable LocalDevice device) {
        logger.debug("local device removed {}", device);
    }

    @Override
    public void remoteDeviceAdded(@Nullable Registry registry, @Nullable RemoteDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("Device parameter was null");
        }
        logger.debug("remote device added {}", device);

        RemoteService remoteService = device.findService(serviceType);
        if (remoteService != null) {
            /*
             * ServiceDescription service = new ServiceDescription();
             * service.setUUID(remoteService.getServiceId().toString()); // TODO is this a UUID?
             * service.setServiceFilter(remoteService.getServiceType().toString());
             * service.setFriendlyName(device.getDetails().getFriendlyName());
             * service.setModelName(device.getDetails().getModelDetails().getModelName());
             * service.setModelNumber(device.getDetails().getModelDetails().getModelNumber());
             * service.setModelDescription(device.getDetails().getModelDetails().getModelDescription());
             * service.setManufacturer(device.getDetails().getManufacturerDetails().getManufacturer());
             * service.setIpAddress(device.getIdentity().getDescriptorURL().getHost());
             * remoteService.getDevice().getVersion().toString(); // TODO compare this with the connect sdk version
             * // handling
             * // service.setVersion(version); // TODO
             * WebOSTVService webOSTVService = new WebOSTVService(service, new ServiceConfig(service));
             */

            // lgWebOsDiscovery.deviceDiscovered(remoteService.getServiceId().toString(),
            // device.getDetails().getFriendlyName(), device.getIdentity().getDescriptorURL().getHost());
            // TODO: need to store it so that handlers can retrieve it.
            // TODO: need to persist secrets,which are currently in StoredDecives

            // TODO: or register this instead of SSDPDiscoveryProvider in DiscoveryManager

            // devicesList.put("com.connectsdk.service.WebOSTVService",
            // "com.connectsdk.discovery.provider.SSDPDiscoveryProvider");

        }
    }

    @Override
    public void remoteDeviceDiscoveryFailed(@Nullable Registry registry, @Nullable RemoteDevice device,
            @Nullable Exception exception) {
        logger.debug("remote device removed - {} {}", device, exception);
        // intentionally empty
    }

    @Override
    public void remoteDeviceDiscoveryStarted(@Nullable Registry registry, @Nullable RemoteDevice device) {
        // intentionally empty
    }

    @Override
    public void remoteDeviceRemoved(@Nullable Registry registry, @Nullable RemoteDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("Device parameter was null");
        }
        logger.debug("remote device removed - ID: {} and IP: {}", device.getIdentity().getUdn().getIdentifierString(),
                device.getIdentity().getDescriptorURL().getHost());

    }

    @Override
    public void remoteDeviceUpdated(@Nullable Registry registry, @Nullable RemoteDevice device) {
        logger.debug("remote device updated and still alive {}", device);
        // nothing to do
    }
}
