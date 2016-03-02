/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.connectsdk.handler;

import org.eclipse.smarthome.core.types.Command;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.capability.VolumeControl;

/**
 * @author Sebastian Prehn
 * @since 1.8.0
 */
public class VolumeControlDown extends AbstractChannelHandler<Void> {

    private VolumeControl getControl(final ConnectableDevice device) {
        return device.getCapability(VolumeControl.class);
    }

    @Override
    public void onReceiveCommand(final ConnectableDevice d, Command command) {
        if (d.hasCapabilities(VolumeControl.Volume_Up_Down)) {
            getControl(d).volumeDown(createDefaultResponseListener());
        }

    }

}