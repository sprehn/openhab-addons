/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.connectsdk.handler;

import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connectsdk.core.AppInfo;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.Launcher.AppLaunchListener;
import com.connectsdk.service.capability.Launcher.AppListListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.sessions.LaunchSession;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * @author Sebastian Prehn
 * @since 1.8.0
 */
public class AppLauncher extends AbstractChannelHandler<AppListListener> {
    private static final Logger logger = LoggerFactory.getLogger(AppLauncher.class);

    private Launcher getControl(final ConnectableDevice device) {
        return device.getCapability(Launcher.class);
    }

    @Override
    public void onReceiveCommand(final ConnectableDevice d, Command command) {
        if (d.hasCapabilities(Launcher.Application_List, Launcher.Application)) {
            final String value = command.toString();
            final Launcher control = getControl(d);
            control.getAppList(new AppListListener() {
                @Override
                public void onError(ServiceCommandError error) {
                    logger.error("error requesting external input list: {}.", error.getMessage());
                }

                @Override
                public void onSuccess(List<AppInfo> infos) {
                    if (logger.isDebugEnabled()) {
                        for (AppInfo c : infos) {
                            logger.debug("Input {} - {}", c.getId(), c.getName());
                        }
                    }
                    try {
                        AppInfo appInfo = Iterables.find(infos, new Predicate<AppInfo>() {
                            @Override
                            public boolean apply(AppInfo c) {
                                return c.getId().equals(value);
                            };
                        });
                        control.launchAppWithInfo(appInfo, new AppLaunchListener() {

                            @Override
                            public void onError(ServiceCommandError error) {
                                logger.error("Error {}: {}.", this.getClass().getName(), error.getMessage());

                            }

                            @Override
                            public void onSuccess(LaunchSession object) {
                                logger.debug("Success {}: {}.", this.getClass().getName(), object);
                            }
                        });
                    } catch (NoSuchElementException ex) {
                        logger.warn("Device does not have input: {}.", value);
                    }

                }
            });

        }

    }

}