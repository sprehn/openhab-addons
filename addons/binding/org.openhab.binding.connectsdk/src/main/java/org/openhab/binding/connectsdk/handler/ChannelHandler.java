/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.connectsdk.handler;

import org.eclipse.smarthome.core.types.Command;

import com.connectsdk.device.ConnectableDevice;

/**
 * Channel Handler mediates between connect sdk device state changes and openhab channel events.
 *
 * @author Sebastian Prehn
 * @since 1.8.0
 */
public interface ChannelHandler {

    void onReceiveCommand(ConnectableDevice device, Command command);

    void refreshSubscription(ConnectableDevice device, String channelId, ConnectSDKHandler handler);

    void removeAnySubscription(ConnectableDevice device);

    void onDeviceRemoved(final ConnectableDevice device, final String channelId, final ConnectSDKHandler handler);

    void onDeviceReady(final ConnectableDevice device, final String channelId, final ConnectSDKHandler handler);

}