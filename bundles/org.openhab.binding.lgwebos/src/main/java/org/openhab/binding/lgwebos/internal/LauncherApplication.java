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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.lgwebos.internal.handler.LGWebOSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connectsdk.core.AppInfo;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.sessions.LaunchSession;

/**
 * Provides ability to launch an application on the TV.
 *
 * @author Sebastian Prehn - initial contribution
 */
@NonNullByDefault
public class LauncherApplication extends BaseChannelHandler<ResponseListener<AppInfo>, LaunchSession> {
    private final Logger logger = LoggerFactory.getLogger(LauncherApplication.class);
    private final Map<ThingUID, @Nullable List<AppInfo>> applicationListCache = new HashMap<>();

    @Override
    public void onDeviceReady(String channelId, LGWebOSHandler handler) {
        super.onDeviceReady(channelId, handler);

        handler.getSocket().getAppList(new Launcher.AppListListener() {

            @Override
            public void onError(@Nullable ServiceCommandError error) {
                logger.warn("Error requesting application list: {}.", error == null ? "" : error.getMessage());
            }

            @Override
            @NonNullByDefault({})
            public void onSuccess(List<AppInfo> appInfos) {
                if (logger.isDebugEnabled()) {
                    for (AppInfo a : appInfos) {
                        logger.debug("AppInfo {} - {}", a.getId(), a.getName());
                    }
                }
                applicationListCache.put(handler.getThing().getUID(), appInfos);
            }
        });

    }

    @Override
    public void onDeviceRemoved(String channelId, LGWebOSHandler handler) {
        super.onDeviceRemoved(channelId, handler);
        applicationListCache.remove(handler.getThing().getUID());
    }

    @Override
    public void onReceiveCommand(String channelId, LGWebOSHandler handler, Command command) {

        final String value = command.toString();

        List<AppInfo> appInfos = applicationListCache.get(handler.getThing().getUID());
        if (appInfos == null) {
            logger.warn("No application list cached for this device {}, ignoring command.",
                    handler.getThing().getUID());
        } else {
            Optional<AppInfo> appInfo = appInfos.stream().filter(a -> a.getId().equals(value)).findFirst();
            if (appInfo.isPresent()) {
                handler.getSocket().launchApp(appInfo.get().getId(), getDefaultResponseListener());
            } else {
                logger.warn("TV does not support any app with id: {}.", value);
            }
        }

    }

    @Override
    protected Optional<ServiceSubscription<ResponseListener<AppInfo>>> getSubscription(String channelId,
            LGWebOSHandler handler) {

        return Optional.of(handler.getSocket().subscribeRunningApp(new ResponseListener<AppInfo>() {

            @Override
            public void onError(@Nullable ServiceCommandError error) {
                logger.debug("Error in listening to application changes: {}.", error == null ? "" : error.getMessage());
            }

            @Override
            public void onSuccess(@Nullable AppInfo appInfo) {
                if (appInfo == null) {
                    handler.postUpdate(channelId, UnDefType.UNDEF);
                } else {
                    handler.postUpdate(channelId, new StringType(appInfo.getId()));
                }
            }
        }));

    }

    public @Nullable List<AppInfo> getAppInfos(ThingUID key) {
        return applicationListCache.get(key);
    }
}
