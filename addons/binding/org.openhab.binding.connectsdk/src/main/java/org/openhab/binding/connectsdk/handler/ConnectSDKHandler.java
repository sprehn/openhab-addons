/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.connectsdk.handler;

import static org.openhab.binding.connectsdk.ConnectSDKBindingConstants.*;

import java.util.List;
import java.util.Map;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.ConnectableDeviceListener;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManagerListener;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.DeviceService.PairingType;
import com.connectsdk.service.command.ServiceCommandError;
import com.google.common.collect.ImmutableMap;

/**
 * The {@link ConnectSDKHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sebastian Prehn - Initial contribution
 */
public class ConnectSDKHandler extends BaseThingHandler implements ConnectableDeviceListener, DiscoveryManagerListener {

    private Logger logger = LoggerFactory.getLogger(ConnectSDKHandler.class);
    private DiscoveryManager discoveryManager;

    // ChannelID to CommandHandler Map
    private final Map<String, ChannelHandler> channelHandlers = ImmutableMap.<String, ChannelHandler> builder()
            .put(CHANNEL_VOLUME, new VolumeControlVolume()).put(CHANNEL_VOLUME_UP, new VolumeControlUp())
            .put(CHANNEL_VOLUME_DOWN, new VolumeControlDown()).put(CHANNEL_POWER, new PowerControlPower())
            .put(CHANNEL_MUTE, new VolumeControlMute()).put(CHANNEL_CHANNEL, new TVControlChannel())
            .put(CHANNEL_CHANNEL_UP, new TVControlUp()).put(CHANNEL_CHANNEL_DOWN, new TVControlDown())
            .put(CHANNEL_CHANNEL_NAME, new TVControlChannelName()).put(CHANNEL_APP_LAUCHER, new LauncherApplication())
            .put(CHANNEL_MEDIA_FORWARD, new MediaControlForward()).put(CHANNEL_MEDIA_PAUSE, new MediaControlPause())
            .put(CHANNEL_MEDIA_PLAY, new MediaControlPlay()).put(CHANNEL_MEDIA_REWIND, new MediaControlRewind())
            .put(CHANNEL_MEDIA_STOP, new MediaControlStop()).put(CHANNEL_MEDIA_STATE, new MediaControlPlayState())
            .put(CHANNEL_TOAST, new ToastControlToast()).build();

    public ConnectSDKHandler(Thing thing, DiscoveryManager discoveryManager) {
        super(thing);
        this.discoveryManager = discoveryManager;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("internalReceiveCommand({},{}) is called", channelUID, command);
        ChannelHandler handler = channelHandlers.get(channelUID.getId());
        final ConnectableDevice device = getDevice();
        if (device == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    String.format("%s not found under connect sdk devices", getThing().getUID()));
            return;
        }
        handler.onReceiveCommand(device, command);
    }

    private ConnectableDevice getDevice() {
        String ip = this.getThing().getProperties().get(PROPERTY_IP_ADDRESS);
        return this.discoveryManager.getCompatibleDevices().get(ip);
    }

    @Override
    public void initialize() {
        this.discoveryManager.addListener(this);

        ConnectableDevice device = getDevice();
        if (device == null) {// If TV is off getDevice() will return null
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "TV is off");
        } else {
            device.addListener(this);
            if (isAnyChannelLinked()) {
                device.connect(); // if successful onDeviceReady will set online state
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING,
                        "Will connect when at least one channel is linked.");
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        ConnectableDevice device = getDevice();
        if (device != null) {
            device.removeListener(this);
        }
        this.discoveryManager.removeListener(this);
        this.discoveryManager = null;
    }
    // Connectable Device Listener

    @Override
    public void onDeviceReady(ConnectableDevice device) { // this gets called on connection success
        updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Device Ready");
        refreshAllChannelSubscriptions(device);
        for (Map.Entry<String, ChannelHandler> e : channelHandlers.entrySet()) {
            e.getValue().onDeviceReady(device, e.getKey(), this);
        }
    }

    @Override
    public void onDeviceDisconnected(ConnectableDevice device) {
        logger.debug("Device disconnected: {}", device);
        for (Map.Entry<String, ChannelHandler> e : channelHandlers.entrySet()) {
            e.getValue().onDeviceRemoved(device, e.getKey(), this);
            e.getValue().removeAnySubscription(device);
        }
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "TV is off");
    }

    @Override
    public void onPairingRequired(ConnectableDevice device, DeviceService service, PairingType pairingType) {
        updateStatus(this.thing.getStatus(), ThingStatusDetail.CONFIGURATION_PENDING, "Pairing Required");
    }

    @Override
    public void onCapabilityUpdated(ConnectableDevice device, List<String> added, List<String> removed) {
        logger.debug("Capabilities updated: {} - added: {} - removed: {}", device, added, removed);
        refreshAllChannelSubscriptions(device);
    }

    @Override
    public void onConnectionFailed(ConnectableDevice device, ServiceCommandError error) {
        logger.debug("Connection failed: {} - error: {}", device, error.getMessage());
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Connection Failed");
    }

    // callback methods for commandHandlers
    public void postUpdate(String channelUID, State state) {
        this.updateState(channelUID, state);
    }

    public boolean isChannelInUse(String channelId) {
        return this.isLinked(channelId);
    }

    // channel linking modifications

    @Override
    public void channelLinked(ChannelUID channelUID) {
        final ConnectableDevice device = getDevice();
        if (device != null) {
            if (!device.isConnected()) {
                device.connect();
            } else {
                refreshChannelSubscription(channelUID);
            }
        }
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        refreshChannelSubscription(channelUID);
        final ConnectableDevice device = getDevice();
        if (!isAnyChannelLinked() && device != null && device.isConnected()) {
            device.disconnect();
        }
    }

    // private helpers

    private void refreshChannelSubscription(ChannelUID channelUID) {
        String channelId = channelUID.getId();
        ChannelHandler handler = channelHandlers.get(channelId);
        if (handler != null) {
            handler.refreshSubscription(getDevice(), channelId, this);
        }
    }

    private void refreshAllChannelSubscriptions(ConnectableDevice device) {
        for (Map.Entry<String, ChannelHandler> e : channelHandlers.entrySet()) {
            e.getValue().refreshSubscription(device, e.getKey(), this);
        }
    }

    private boolean isAnyChannelLinked() {
        for (String channelId : channelHandlers.keySet()) {
            if (this.isLinked(channelId)) {
                return true;
            }
        }
        return false;
    }

    // just to make sure, this device is registered, if it was powered off during initialization
    @Override
    public void onDeviceAdded(DiscoveryManager manager, ConnectableDevice device) {
        String ip = this.getThing().getProperties().get(PROPERTY_IP_ADDRESS);
        if (device.getIpAddress().equals(ip)) {
            device.addListener(this);
            if (isAnyChannelLinked()) {
                device.connect(); // if successful onDeviceReady will set online state
            }
        }
    }

    @Override
    public void onDeviceUpdated(DiscoveryManager manager, ConnectableDevice device) {
        String ip = this.getThing().getProperties().get(PROPERTY_IP_ADDRESS);
        if (device.getIpAddress().equals(ip)) {
            device.addListener(this);
        }
    }

    @Override
    public void onDeviceRemoved(DiscoveryManager manager, ConnectableDevice device) {
        // NOP
    }

    @Override
    public void onDiscoveryFailed(DiscoveryManager manager, ServiceCommandError error) {
        // NOP
    }

}
