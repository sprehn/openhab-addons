package org.openhab.binding.lgwebos.internal.discovery;

import static org.openhab.binding.lgwebos.LGWebOSBindingConstants.*;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.upnp.UpnpDiscoveryParticipant;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.jupnp.UpnpService;
import org.jupnp.model.message.header.ServiceTypeHeader;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.types.ServiceType;
import org.openhab.binding.lgwebos.LGWebOSBindingConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, configurationPid = "binding.lgwebos.upnp")

public class LGWebOSUpnpDiscoveryParticipant implements UpnpDiscoveryParticipant {

    private UpnpService upnpService;
    // private final Logger logger = LoggerFactory.getLogger(LGWebOSUpnpDiscoveryParticipant.class);
    private final ServiceType serviceType = new ServiceType("lge-com", "webos-second-screen", 1);
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
    }

    @Deactivate
    private void deactivate() {
        scheduler.shutdown();
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
            // || "LG TV".equals(device.getDetails().getModelDetails().getModelName())) {
            return new ThingUID(THING_TYPE_WEBOSTV, device.getIdentity().getUdn().getIdentifierString());
        }

        return null;
    }

    private void search() {
        upnpService.getControlPoint().search(new ServiceTypeHeader(serviceType));
    }
}
