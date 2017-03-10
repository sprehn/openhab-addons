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
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.sessions.LaunchSession;

/**
 * @author Sebastian Prehn
 * @since 2.1.0
 */
public class LaunchHulu extends AbstractChannelHandler<Launcher.AppInfoListener> {

    private Launcher getControl(final ConnectableDevice device) {
        return device.getCapability(Launcher.class);
    }

    @Override
    public void onReceiveCommand(final ConnectableDevice d, Command command) {
        if (d.hasCapabilities(Launcher.Hulu)) {
            final String value = command.toString();
            final Launcher control = getControl(d);
            control.launchHulu(value, LaunchHulu.this.<LaunchSession>createDefaultResponseListener());
        }
    }

}