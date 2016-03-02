/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.connectsdk.handler;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.VolumeControl.MuteListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;

/**
 * @author Sebastian Prehn
 * @since 1.8.0
 */
public class VolumeControlMute extends AbstractChannelHandler<MuteListener> {
    private static final Logger logger = LoggerFactory.getLogger(VolumeControlMute.class);

    private VolumeControl getControl(final ConnectableDevice device) {
        return device.getCapability(VolumeControl.class);
    }

    @Override
    public void onReceiveCommand(final ConnectableDevice d, Command command) {
        if (d.hasCapabilities(VolumeControl.Mute_Set)) {
            OnOffType onOffType;
            if (command instanceof OnOffType) {
                onOffType = (OnOffType) command;
            } else if (command instanceof StringType) {
                onOffType = OnOffType.valueOf(command.toString());
            } else {
                logger.warn("only accept OnOffType");
                return;
            }

            getControl(d).setMute(OnOffType.ON.equals(onOffType), createDefaultResponseListener());

        }

    }

    @Override
    protected ServiceSubscription<MuteListener> getSubscription(final ConnectableDevice device, final String channelId,
            final ConnectSDKHandler handler) {
        if (device.hasCapability(VolumeControl.Mute_Subscribe)) {
            return getControl(device).subscribeMute(new MuteListener() {

                @Override
                public void onError(ServiceCommandError error) {
                    logger.debug("error: {} {} {}", error.getCode(), error.getPayload(), error.getMessage());
                }

                @Override
                public void onSuccess(Boolean value) {
                    handler.postUpdate(channelId, value ? OnOffType.ON : OnOffType.OFF);

                }
            });
        } else {
            return null;
        }

    }

}