/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.lgwebos.internal.discovery;

import static org.openhab.binding.lgwebos.internal.WebOSBindingConstants.THING_TYPE_WEBOSTV;

import java.time.Instant;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.upnp.UpnpDiscoveryParticipant;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.jupnp.model.meta.RemoteDevice;
import org.openhab.binding.lgwebos.internal.WebOSBindingConstants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Upnp Discovery participant add the ability to auto discover LG Web OS devices on the network.
 * Some users choose to not use upnp. Therefore this can only play an optional role and help discover the device and its
 * ip.
 *
 * @author Sebastian Prehn - Initial contribution
 */
@NonNullByDefault
@Component(service = UpnpDiscoveryParticipant.class, immediate = true, configurationPid = "discovery.lgwebos.upnp")
public class WebOSUpnpDiscoveryParticipant implements UpnpDiscoveryParticipant {

    private final Logger logger = LoggerFactory.getLogger(WebOSUpnpDiscoveryParticipant.class);
    private static final String DEVICE_ID = "deviceId";

    @Override
    public Set<@NonNull ThingTypeUID> getSupportedThingTypeUIDs() {
        return WebOSBindingConstants.SUPPORTED_THING_TYPES_UIDS;
    }

    @Override
    public @Nullable DiscoveryResult createResult(RemoteDevice device) {
        ThingUID thingUID = getThingUID(device);

        if (thingUID == null) {
            return null;
        }

        return DiscoveryResultBuilder.create(thingUID).withLabel(device.getDetails().getFriendlyName())
                .withProperty(DEVICE_ID, device.getIdentity().getUdn().getIdentifierString())
                .withProperty(WebOSBindingConstants.CONFIG_IPADDRESS,
                        device.getIdentity().getDescriptorURL().getHost())
                .withLabel(device.getDetails().getFriendlyName())
                .withProperty("modelName", device.getDetails().getModelDetails().getModelName())
                .withProperty("modelNumber", device.getDetails().getModelDetails().getModelNumber())
                .withProperty("descriptorUrl", device.getIdentity().getDescriptorURL())
                .withProperty("serialNumber", device.getDetails().getSerialNumber())
                .withProperty("manufacturer", device.getDetails().getManufacturerDetails().getManufacturer())
                .withProperty("lastDetection", Instant.now().toString()).withRepresentationProperty(DEVICE_ID)
                .withThingType(THING_TYPE_WEBOSTV).build();
    }

    @Override
    public @Nullable ThingUID getThingUID(RemoteDevice device) {
        logger.debug("Discovered remote device {}", device);
        if (device.findService(WebOSBindingConstants.UPNP_SERVICE_TYPE) != null) {
            logger.debug("Found LG WebOS TV: {}", device);
            return new ThingUID(THING_TYPE_WEBOSTV, device.getIdentity().getUdn().getIdentifierString());
        }
        return null;
    }

}
