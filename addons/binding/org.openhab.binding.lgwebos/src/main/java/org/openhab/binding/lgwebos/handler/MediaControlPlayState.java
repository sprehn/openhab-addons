/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lgwebos.handler;

import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaControl.PlayStateListener;
import com.connectsdk.service.capability.MediaControl.PlayStateStatus;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;

/**
 * Handles Media Control State PlayState. This is read only.
 * Subscribes to the current play state.
 *
 * @author Sebastian Prehn
 * @since 1.8.0
 */
public class MediaControlPlayState extends BaseChannelHandler<PlayStateListener> {
    private Logger logger = LoggerFactory.getLogger(MediaControlPlayState.class);

    private MediaControl getControl(final ConnectableDevice device) {
        return device.getCapability(MediaControl.class);
    }

    @Override
    public void onReceiveCommand(final ConnectableDevice d, Command command) {
        // nothing to do, this is read only.
    }

    @Override
    protected ServiceSubscription<PlayStateListener> getSubscription(final ConnectableDevice device,
            final String channelId, final ConnectSDKHandler handler) {
        if (device.hasCapability(MediaControl.PlayState_Subscribe)) {
            return getControl(device).subscribePlayState(new PlayStateListener() {

                @Override
                public void onError(ServiceCommandError error) {
                    logger.warn("{} {} {}", error.getCode(), error.getPayload(), error.getMessage());
                }

                @Override
                public void onSuccess(PlayStateStatus status) {
                    handler.postUpdate(channelId, new StringType(status.name()));
                }
            });
        } else {
            return null;
        }
    }
}
