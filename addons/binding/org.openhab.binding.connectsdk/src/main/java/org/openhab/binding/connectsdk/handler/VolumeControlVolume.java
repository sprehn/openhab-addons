/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.connectsdk.handler;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.VolumeControl.VolumeListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;

/**
 * @author Sebastian Prehn
 * @since 1.8.0
 */
public class VolumeControlVolume extends AbstractChannelHandler<VolumeListener> {
    private Logger logger = LoggerFactory.getLogger(VolumeControlVolume.class);

    private VolumeControl getControl(final ConnectableDevice device) {
        return device.getCapability(VolumeControl.class);
    }

    @Override
    public void onReceiveCommand(final ConnectableDevice d, Command command) {
        if (d.hasCapabilities(VolumeControl.Volume_Set)) {
            PercentType percent;
            if (command instanceof PercentType) {
                percent = (PercentType) command;
            } else if (command instanceof DecimalType) {
                percent = new PercentType(((DecimalType) command).toBigDecimal());
            } else if (command instanceof StringType) {
                percent = new PercentType(((StringType) command).toString());
            } else {
                logger.warn("only accept precentType");
                return;
            }
            getControl(d).setVolume(percent.floatValue() / 100.0f, createDefaultResponseListener());
        }
    }

    @Override
    protected ServiceSubscription<VolumeListener> getSubscription(final ConnectableDevice device,
            final String channelUID, final ConnectSDKHandler handler) {
        if (device.hasCapability(VolumeControl.Volume_Subscribe)) {
            return getControl(device).subscribeVolume(new VolumeListener() {

                @Override
                public void onError(ServiceCommandError error) {
                    logger.warn("{} {} {}", error.getCode(), error.getPayload(), error.getMessage());
                }

                @Override
                public void onSuccess(Float value) {
                    handler.postUpdate(channelUID, new PercentType(Math.round(value * 100)));
                }
            });
        } else {
            return null;
        }
    }
}
