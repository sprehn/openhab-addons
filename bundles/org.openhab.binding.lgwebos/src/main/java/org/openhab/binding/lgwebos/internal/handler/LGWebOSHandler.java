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
package org.openhab.binding.lgwebos.internal.handler;

import static org.openhab.binding.lgwebos.internal.LGWebOSBindingConstants.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.lgwebos.action.LGWebOSActions;
import org.openhab.binding.lgwebos.internal.ChannelHandler;
import org.openhab.binding.lgwebos.internal.LauncherApplication;
import org.openhab.binding.lgwebos.internal.MediaControlPlayer;
import org.openhab.binding.lgwebos.internal.MediaControlStop;
import org.openhab.binding.lgwebos.internal.PowerControlPower;
import org.openhab.binding.lgwebos.internal.TVControlChannel;
import org.openhab.binding.lgwebos.internal.TVControlChannelName;
import org.openhab.binding.lgwebos.internal.ToastControlToast;
import org.openhab.binding.lgwebos.internal.VolumeControlMute;
import org.openhab.binding.lgwebos.internal.VolumeControlVolume;
import org.openhab.binding.lgwebos.internal.handler.WebOSTVSocket.WebOSTVSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LGWebOSHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sebastian Prehn - initial contribution
 */
public class LGWebOSHandler extends BaseThingHandler implements WebOSTVStore, WebOSTVSocketListener {

    private final Logger logger = LoggerFactory.getLogger(LGWebOSHandler.class);

    // ChannelID to CommandHandler Map
    private final Map<String, ChannelHandler> channelHandlers;
    private String deviceId;

    private LauncherApplication appLauncher = new LauncherApplication();
    private final WebOSTVSocket socket;

    public LGWebOSHandler(@NonNull Thing thing, WebSocketClient webSocketClient) {
        super(thing);

        socket = new WebOSTVSocket(webSocketClient, this, thing.getProperties().get(PROPERTY_DEVICE_IP));
        socket.setListener(this);
        socket.connect();

        // WebOSTVService service = new WebOSTVService(serviceDescription, serviceConfig)
        // mouseSocket = new WebOSTVMouseSocket(webSocketClient);

        Map<String, ChannelHandler> handlers = new HashMap<>();
        handlers.put(CHANNEL_VOLUME, new VolumeControlVolume());
        handlers.put(CHANNEL_POWER, new PowerControlPower());
        handlers.put(CHANNEL_MUTE, new VolumeControlMute());
        handlers.put(CHANNEL_CHANNEL, new TVControlChannel());
        handlers.put(CHANNEL_CHANNEL_NAME, new TVControlChannelName());
        handlers.put(CHANNEL_APP_LAUNCHER, appLauncher);
        handlers.put(CHANNEL_MEDIA_STOP, new MediaControlStop());
        handlers.put(CHANNEL_TOAST, new ToastControlToast());
        handlers.put(CHANNEL_MEDIA_PLAYER, new MediaControlPlayer());
        channelHandlers = Collections.unmodifiableMap(handlers);
    }

    public WebOSTVSocket getSocket() {
        return socket;
    }

    public LauncherApplication getLauncherApplication() {
        return appLauncher;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handleCommand({},{}) is called", channelUID, command);
        ChannelHandler handler = channelHandlers.get(channelUID.getId());
        if (handler == null) {
            logger.warn(
                    "Unable to handle command {}. No handler found for channel {}. This must not happen. Please report as a bug.",
                    command, channelUID);
            return;
        }

        handler.onReceiveCommand(channelUID.getId(), this, command);
    }

    @Override
    public String getKey() {
        return getConfig().get(PROPERTY_DEVICE_KEY).toString();
    }

    @Override
    public void setKey(String key) {
        getConfig().put(PROPERTY_DEVICE_KEY, key);
    }

    @Override
    public void setUUID(String uuid) {
        getConfig().put(PROPERTY_DEVICE_UUID, uuid);
    }

    // Connectable Device Listener

    @Override
    public void onStateChanged(WebOSTVSocket.State old, WebOSTVSocket.State state) {
        switch (state) {
            case DISCONNECTED:

                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "TV is off");

                for (Map.Entry<String, ChannelHandler> e : channelHandlers.entrySet()) {
                    e.getValue().onDeviceRemoved(e.getKey(), this);
                    e.getValue().removeAnySubscription(this);
                }

                break;
            case CONNECTING:
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "Connecting ...");
                break;
            case DISCONNECTING:
                updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Disconnecting ...");
                break;
            case REGISTERED:
                updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Connected");
                break;
            case REGISTERING:
                updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Registering");
                break;
        }

        // refreshAllChannelSubscriptions(device);
        // channelHandlers.forEach((k, v) -> v.onDeviceReady(device, k, this));
    }

    @Override
    public void onError(String error) {
        logger.debug("Connection failed - error: {}", error);
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Connection Failed: " + error);
    }

    // callback methods for commandHandlers
    public void postUpdate(String channelId, State state) {
        updateState(channelId, state);
    }

    public boolean isChannelInUse(String channelId) {
        return isLinked(channelId);
    }

    // channel linking modifications

    @Override
    public void channelLinked(ChannelUID channelUID) {
        refreshChannelSubscription(channelUID);
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        refreshChannelSubscription(channelUID);
    }

    // private helpers

    /**
     * Refresh channel subscription for one specific channel.
     *
     * @param channelUID must not be <code>null</code>
     */
    private void refreshChannelSubscription(ChannelUID channelUID) {
        String channelId = channelUID.getId();

        if (socket.isConnected()) {
            channelHandlers.get(channelId).refreshSubscription(channelId, this);
        }

    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(LGWebOSActions.class);
    }
}
