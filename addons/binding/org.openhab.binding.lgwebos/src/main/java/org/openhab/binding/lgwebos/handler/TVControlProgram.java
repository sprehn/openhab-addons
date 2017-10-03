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

import com.connectsdk.core.ProgramInfo;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.capability.TVControl;
import com.connectsdk.service.capability.TVControl.ProgramInfoListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;

/**
 * Handles TV Control Program State. This is read only.
 * Subscribes to to current program name.
 *
 * @author Sebastian Prehn
 * @since 1.8.0
 */
public class TVControlProgram extends BaseChannelHandler<ProgramInfoListener> {
    private Logger logger = LoggerFactory.getLogger(TVControlProgram.class);

    private TVControl getControl(final ConnectableDevice device) {
        return device.getCapability(TVControl.class);
    }

    @Override
    public void onReceiveCommand(final ConnectableDevice d, Command command) {
        // nothing to do, this is read only.
    }

    @Override
    protected ServiceSubscription<ProgramInfoListener> getSubscription(final ConnectableDevice device,
            final String channelId, final ConnectSDKHandler handler) {
        if (device.hasCapability(TVControl.Program_Subscribe)) {
            return getControl(device).subscribeProgramInfo(new ProgramInfoListener() {

                @Override
                public void onError(ServiceCommandError error) {
                    logger.warn("{} {} {}", error.getCode(), error.getPayload(), error.getMessage());
                }

                @Override
                public void onSuccess(ProgramInfo programInfo) {
                    handler.postUpdate(channelId, new StringType(programInfo.getName()));
                }
            });
        } else {
            return null;
        }
    }

}
