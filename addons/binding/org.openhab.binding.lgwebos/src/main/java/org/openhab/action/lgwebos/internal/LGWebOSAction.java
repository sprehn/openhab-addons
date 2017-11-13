/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.action.lgwebos.internal;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.eclipse.smarthome.model.script.engine.action.ActionDoc;
import org.eclipse.smarthome.model.script.engine.action.ActionService;
import org.eclipse.smarthome.model.script.engine.action.ParamDoc;
import org.openhab.binding.lgwebos.internal.discovery.LGWebOSDiscovery;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connectsdk.core.AppInfo;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.capability.CapabilityMethods;
import com.connectsdk.service.capability.KeyControl;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.TextInputControl;
import com.connectsdk.service.capability.ToastControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.sessions.LaunchSession;

/**
 * This class provides a rules action to start applications and send toast messages.
 *
 * @author Sebastian Prehn - initial contribution
 */
@Component(service = ActionService.class, immediate = true)
public class LGWebOSAction implements ActionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LGWebOSAction.class);

    private static LGWebOSDiscovery discovery;
    private static ResponseListener<LaunchSession> responseListenerLaunchSession = createDefaultResponseListener();
    private static ResponseListener<Object> responseListenerObject = createDefaultResponseListener();

    @Override
    public String getActionClassName() {
        return getActionClass().getCanonicalName();
    }

    @Override
    public Class<?> getActionClass() {
        return LGWebOSAction.class;
    }

    @Reference
    protected void bindDiscovery(LGWebOSDiscovery discovery) {
        LGWebOSAction.discovery = discovery;
    }

    protected void unbindDiscovery(LGWebOSDiscovery discovery) {
        LGWebOSAction.discovery = null;
    }

    @ActionDoc(text = "sends a toast message to a web os device with openhab icon")
    public static void showToast(@ParamDoc(name = "deviceId") String deviceId, @ParamDoc(name = "text") String text)
            throws IOException {
        showToast(deviceId, LGWebOSAction.class.getResource("/openhab-logo-square.png").toString(), text);
    }

    @ActionDoc(text = "sends a toast message to a web os device with custom icon")
    public static void showToast(@ParamDoc(name = "deviceId") String deviceId, @ParamDoc(name = "icon") String icon,
            @ParamDoc(name = "text") String text) throws IOException {
        Optional<ToastControl> control = getControl(ToastControl.class, deviceId);
        if (control.isPresent()) {
            BufferedImage bi = ImageIO.read(new URL(icon));
            try (ByteArrayOutputStream os = new ByteArrayOutputStream();
                    OutputStream b64 = Base64.getEncoder().wrap(os);) {
                ImageIO.write(bi, "png", b64);
                control.get().showToast(text, os.toString(StandardCharsets.UTF_8.name()), "png",
                        responseListenerObject);
            }
        }
    }

    @ActionDoc(text = "opens the given URL in the TV's browser app")
    public static void launchBrowser(@ParamDoc(name = "deviceId") String deviceId, @ParamDoc(name = "url") String url) {
        getControl(Launcher.class, deviceId)
                .ifPresent(control -> control.launchBrowser(url, responseListenerLaunchSession));
    }

    @ActionDoc(text = "opens the application with given appId")
    public static void launchApplication(@ParamDoc(name = "deviceId") String deviceId,
            @ParamDoc(name = "appId") String appId) {
        getControl(Launcher.class, deviceId)
                .ifPresent(control -> control.launchApp(appId, responseListenerLaunchSession));
        ;

    }

    @ActionDoc(text = "opens the application with given appId and passes additional parameters")
    public static void launchApplicationWithParam(@ParamDoc(name = "deviceId") String deviceId,
            @ParamDoc(name = "appId") String appId, Object param) {
        Optional<Launcher> control = getControl(Launcher.class, deviceId);
        if (control.isPresent()) {
            control.get().getAppList(new Launcher.AppListListener() {
                @Override
                public void onError(ServiceCommandError error) {
                    LOGGER.warn("error requesting application list: {}.", error.getMessage());
                }

                @Override
                public void onSuccess(List<AppInfo> appInfos) {
                    Optional<AppInfo> appInfo = appInfos.stream().filter(a -> a.getId().equals(appId)).findFirst();
                    if (appInfo.isPresent()) {
                        control.get().launchAppWithInfo(appInfo.get(), param, responseListenerLaunchSession);
                    } else {
                        LOGGER.warn("TV does not support any app with id: {}.", appId);
                    }
                }
            });
        }
    }

    @ActionDoc(text = "sends a text input to a web os device")
    public static void sendText(@ParamDoc(name = "deviceId") String deviceId, @ParamDoc(name = "text") String text) {
        getControl(TextInputControl.class, deviceId).ifPresent(control -> control.sendText(text));
    }

    @ActionDoc(text = "sends the Enter key to a web os device")
    public static void sendEnter(@ParamDoc(name = "deviceId") String deviceId) {
        getControl(TextInputControl.class, deviceId).ifPresent(control -> control.sendEnter());
    }

    @ActionDoc(text = "sends the Delete key to a web os device")
    public static void sendDelete(@ParamDoc(name = "deviceId") String deviceId) {
        getControl(TextInputControl.class, deviceId).ifPresent(control -> control.sendDelete());
    }

    @ActionDoc(text = "sends the OK key to a web os device")
    public static void sendOK(@ParamDoc(name = "deviceId") String deviceId) {
        getControl(KeyControl.class, deviceId).ifPresent(control -> control.ok(responseListenerObject));
    }

    @ActionDoc(text = "sends the Back key to a web os device")
    public static void sendBack(@ParamDoc(name = "deviceId") String deviceId) {
        getControl(KeyControl.class, deviceId).ifPresent(control -> control.back(responseListenerObject));
    }

    @ActionDoc(text = "sends the Down key to a web os device")
    public static void sendDown(@ParamDoc(name = "deviceId") String deviceId) {
        getControl(KeyControl.class, deviceId).ifPresent(control -> control.down(responseListenerObject));
    }

    @ActionDoc(text = "sends the Home key to a web os device")
    public static void sendHome(@ParamDoc(name = "deviceId") String deviceId) {
        getControl(KeyControl.class, deviceId).ifPresent(control -> control.home(responseListenerObject));
    }

    @ActionDoc(text = "sends the Left key to a web os device")
    public static void sendLeft(@ParamDoc(name = "deviceId") String deviceId) {
        getControl(KeyControl.class, deviceId).ifPresent(control -> control.left(responseListenerObject));
    }

    @ActionDoc(text = "sends the Right key to a web os device")
    public static void sendRight(@ParamDoc(name = "deviceId") String deviceId) {
        getControl(KeyControl.class, deviceId).ifPresent(control -> control.right(responseListenerObject));
    }

    @ActionDoc(text = "sends the Up key to a web os device")
    public static void sendUp(@ParamDoc(name = "deviceId") String deviceId) {
        getControl(KeyControl.class, deviceId).ifPresent(control -> control.up(responseListenerObject));
    }

    private static <C extends CapabilityMethods> Optional<C> getControl(Class<C> clazz, String deviceId) {
        final ConnectableDevice device = discovery.getDiscoveryManager().getCompatibleDevices().get(deviceId);
        if (device == null) {
            LOGGER.warn("No device found with id: {}", deviceId);
            return Optional.empty();
        }
        C control = device.getCapability(clazz);
        if (control == null) {
            LOGGER.warn("Device {} does not have the ability: {}", deviceId, clazz.getName());
            return Optional.empty();
        }
        return Optional.of(control);
    }

    private static <O> ResponseListener<O> createDefaultResponseListener() {
        return new ResponseListener<O>() {

            @Override
            public void onError(ServiceCommandError error) {
                LOGGER.warn("Response: {}", error.getMessage());
            }

            @Override
            public void onSuccess(O object) {
                LOGGER.debug("Response: {}", object == null ? "OK" : object.toString());
            }
        };
    }
}
