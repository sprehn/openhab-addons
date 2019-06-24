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

import static org.openhab.binding.lgwebos.internal.LGWebOSBindingConstants.*;

import java.io.File;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.lgwebos.internal.LGWebOSBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connectsdk.core.Context;
import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryManager;

/**
 * This class provides the bridge between openhab thing discovery and connect sdk device discovery.
 *
 * @author Sebastian Prehn - initial contribution
 */
@NonNullByDefault
/*
 * @Component(service = { DiscoveryService.class,
 * LGWebOSDiscovery.class }, immediate = true, configurationPid = "binding.lgwebos.discover")
 */
public class LGWebOSDiscovery extends AbstractDiscoveryService implements Context {
    private static final int DISCOVERY_TIMEOUT_SECONDS = 5;

    private final Logger logger = LoggerFactory.getLogger(LGWebOSDiscovery.class);

    private @Nullable DiscoveryManager discoveryManager;

    public LGWebOSDiscovery() {
        super(LGWebOSBindingConstants.SUPPORTED_THING_TYPES_UIDS, DISCOVERY_TIMEOUT_SECONDS, true);
        DiscoveryManager.init(this);
    }

    @Override
    protected void activate(@Nullable Map<String, @Nullable Object> configProperties) {
        logger.debug("Config Parameters: {}", configProperties);
        Util.init(scheduler);

        DiscoveryManager manager = DiscoveryManager.getInstance();
        manager.setPairingLevel(DiscoveryManager.PairingLevel.ON);
        // manager.addListener(this);
        discoveryManager = manager;

        super.activate(configProperties); // starts background discovery
    }

    @Override
    protected void deactivate() {
        super.deactivate(); // stops background discovery
        /*
         * DiscoveryManager manager = discoveryManager;
         *
         * if (manager != null) {
         * manager.removeListener(this);
         * }
         */
        discoveryManager = null;
        DiscoveryManager.destroy();
        Util.uninit();
    }

    // @Override
    @Override
    protected void startScan() {
        // no adhoc scanning. Discovery Service runs in background, but re-discover all known devices in case they were
        // deleted from the inbox.
        /*
         * DiscoveryManager manager = discoveryManager;
         * if (manager != null) {
         * manager.getCompatibleDevices().values().forEach(device -> thingDiscovered(createDiscoveryResult(device)));
         * }
         */
    }

    @Override
    protected void startBackgroundDiscovery() {
        /*
         * DiscoveryManager manager = discoveryManager;
         * if (manager != null) {
         * manager.start();
         * }
         */
    }

    @Override
    protected void stopBackgroundDiscovery() {
        /*
         * DiscoveryManager manager = discoveryManager;
         * if (manager != null) {
         * manager.stop();
         * }
         */
    }

    // DiscoveryManagerListener

    /*
     * @Override
     * public void onDeviceAdded(@Nullable DiscoveryManager manager, @Nullable ConnectableDevice device) {
     * if (device == null) {
     * throw new IllegalArgumentException("ConnectableDevice must not be null");
     * }
     * thingDiscovered(createDiscoveryResult(device));
     * }
     */

    void deviceDiscovered(String deviceId, String friendlyName, String ip) {

        ThingUID thingUID = new ThingUID(THING_TYPE_WEBOSTV, deviceId);
        this.thingDiscovered(DiscoveryResultBuilder.create(thingUID).withLabel(friendlyName)
                .withProperty(PROPERTY_DEVICE_ID, deviceId).withRepresentationProperty(PROPERTY_DEVICE_ID).build());
    }

    /*
     * @Override
     * public void onDeviceUpdated(@Nullable DiscoveryManager manager, @Nullable ConnectableDevice device) {
     * logger.debug("Device updated: {}", device);
     * }
     *
     * @Override
     * public void onDeviceRemoved(@Nullable DiscoveryManager manager, @Nullable ConnectableDevice device) {
     * if (device == null) {
     * throw new IllegalArgumentException("ConnectableDevice must not be null");
     * }
     * logger.debug("Device removed: {}", device);
     * thingRemoved(createThingUID(device));
     * }
     *
     * @Override
     * public void onDiscoveryFailed(@Nullable DiscoveryManager manager, @Nullable ServiceCommandError error) {
     * logger.warn("Discovery Failed {}", error == null ? "" : error.getMessage());
     * }
     *
     * // Helpers for DiscoveryManagerListener Impl
     * private DiscoveryResult createDiscoveryResult(ConnectableDevice device) {
     * ThingUID thingUID = createThingUID(device);
     * return DiscoveryResultBuilder.create(thingUID).withLabel(device.getFriendlyName())
     * .withProperty(PROPERTY_DEVICE_ID, device.getId()).withRepresentationProperty(PROPERTY_DEVICE_ID)
     * .build();
     * }
     *
     *
     * private ThingUID createThingUID(ConnectableDevice device) {
     * return new ThingUID(THING_TYPE_WEBOSTV, device.getId());
     * }
     */

    // Context Implementation
    @Override
    public String getDataDir() {
        return ConfigConstants.getUserDataFolder() + File.separator + "lgwebos";
    }

}
