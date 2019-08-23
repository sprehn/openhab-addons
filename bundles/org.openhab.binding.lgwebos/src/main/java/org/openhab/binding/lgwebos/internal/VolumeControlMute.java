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

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.lgwebos.internal.handler.LGWebOSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;

/**
 * Handles TV Control Mute Command.
 *
 * @author Sebastian Prehn - initial contribution
 */
@NonNullByDefault
public class VolumeControlMute extends BaseChannelHandler<ResponseListener<Boolean>, Object> {
    private final Logger logger = LoggerFactory.getLogger(VolumeControlMute.class);

    @Override
    public void onReceiveCommand(String channelId, LGWebOSHandler handler, Command command) {
        if (OnOffType.ON == command || OnOffType.OFF == command) {
            handler.getSocket().setMute(OnOffType.ON == command, getDefaultResponseListener());
        } else {
            logger.warn("Only accept OnOffType. Type was {}.", command.getClass());
        }
    }

    @Override
    protected Optional<ServiceSubscription<ResponseListener<Boolean>>> getSubscription(String channelId,
            LGWebOSHandler handler) {

        return Optional.of(handler.getSocket().subscribeMute(new ResponseListener<Boolean>() {

            @Override
            public void onError(@Nullable ServiceCommandError error) {
                logger.debug("Error in listening to mute changes: {}.", error == null ? "" : error.getMessage());
            }

            @Override
            public void onSuccess(@Nullable Boolean value) {
                if (value == null) {
                    return;
                }
                handler.postUpdate(channelId, OnOffType.from(value));
            }
        }));

    }

}
