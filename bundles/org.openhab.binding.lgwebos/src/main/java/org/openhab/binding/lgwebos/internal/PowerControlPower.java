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
package org.openhab.binding.lgwebos.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.lgwebos.internal.handler.WebOSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Power Control Command.
 * Note: Connect SDK only supports powering OFF for most devices.
 *
 * @author Sebastian Prehn - initial contribution
 */
@NonNullByDefault
public class PowerControlPower extends BaseChannelHandler<Object> {
    private final Logger logger = LoggerFactory.getLogger(PowerControlPower.class);

    @Override
    public void onReceiveCommand(String channelId, WebOSHandler handler, Command command) {
        if (!handler.getSocket().isConnected()) {
            if (OnOffType.ON == command) {
                logger.debug("Received ON command - Turning TV on is not supported by LG WebOS TVs.");
                // TODO: implement wake on lan here
            } else {
                /*
                 * Unable to send anything to a not connected device.
                 * onDeviceReady nor onDeviceRemoved will be called and item state would be permanently inconsistent.
                 * Therefore setting state to OFF
                 */
                handler.postUpdate(channelId, OnOffType.OFF);
            }
        } else if (OnOffType.OFF == command) {
            handler.getSocket().powerOff(getDefaultResponseListener());
        } else {
            logger.warn("Only accept OnOffType. Type was {}.", command.getClass());
        }

    }

    @Override
    public void onDeviceReady(String channelId, WebOSHandler handler) {
        handler.postUpdate(channelId, OnOffType.ON);
    }

    @Override
    public void onDeviceRemoved(String channelId, WebOSHandler handler) {
        handler.postUpdate(channelId, OnOffType.OFF);
    }
}
