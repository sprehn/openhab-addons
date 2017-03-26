package org.openhab.action.connectsdk.internal;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import org.eclipse.smarthome.model.script.engine.action.ActionDoc;
import org.eclipse.smarthome.model.script.engine.action.ActionService;
import org.eclipse.smarthome.model.script.engine.action.ParamDoc;
import org.openhab.binding.connectsdk.internal.discovery.ConnectSDKDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.capability.CapabilityMethods;
import com.connectsdk.service.capability.TextInputControl;
import com.connectsdk.service.capability.ToastControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;

public class ConnectSDKAction implements ActionService {
    private static final Logger logger = LoggerFactory.getLogger(ConnectSDKAction.class);

    private static ConnectSDKDiscovery discovery;

    @Override
    public String getActionClassName() {
        return getActionClass().getCanonicalName();
    }

    @Override
    public Class<?> getActionClass() {
        return ConnectSDKAction.class;
    }

    protected void bindDiscovery(ConnectSDKDiscovery discovery) {
        ConnectSDKAction.discovery = discovery;
    }

    protected void unbindDiscovery(ConnectSDKDiscovery discovery) {
        discovery = null;
    }

    @ActionDoc(text = "sends a toast message to a web os device")
    public static void showToast(@ParamDoc(name = "deviceId") String deviceId,
            @ParamDoc(name = "text") final String text) throws IOException {
        showToast(deviceId, ConnectSDKAction.class.getResource("/openhab-logo-square.png").toString(), text);
    }

    @ActionDoc(text = "sends a toast message to a web os device")
    public static void showToast(@ParamDoc(name = "deviceId") String deviceId,
            @ParamDoc(name = "icon") final String icon, @ParamDoc(name = "text") final String text) throws IOException {
        ToastControl control = getControl(ToastControl.class, deviceId);
        if (control != null) {

            BufferedImage bi = ImageIO.read(new URL(icon));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", os);
            control.showToast(text, DatatypeConverter.printBase64Binary(os.toByteArray()), "png",
                    createDefaultResponseListener());

        }
    }

    @ActionDoc(text = "sends a toast message to a web os device")
    public static void sendText(@ParamDoc(name = "deviceId") String deviceId,

            @ParamDoc(name = "text") final String text) {
        TextInputControl control = getControl(TextInputControl.class, deviceId);
        if (control != null) {
            control.sendText(text);
        }
    }

    public static void sendEnter(@ParamDoc(name = "deviceId") String deviceId) {
        TextInputControl control = getControl(TextInputControl.class, deviceId);
        if (control != null) {
            control.sendEnter();
        }
    }

    public static void sendDelete(@ParamDoc(name = "deviceId") String deviceId) {
        TextInputControl control = getControl(TextInputControl.class, deviceId);
        if (control != null) {
            control.sendDelete();
        }
    }

    private static <C extends CapabilityMethods> C getControl(Class<C> clazz, String deviceId) {
        final ConnectableDevice d = discovery.getDiscoveryManager().getCompatibleDevices().get(deviceId);
        if (d == null) {
            logger.error("No device found with id: {}", deviceId);
            return null;
        }
        C control = d.getCapability(clazz);
        if (control == null) {
            logger.error("Device {} does not have the ability: {}", deviceId, clazz.getName());
            return null;
        }
        return control;
    }

    private static <O> ResponseListener<O> createDefaultResponseListener() {
        return new ResponseListener<O>() {

            @Override
            public void onError(ServiceCommandError error) {
                logger.error(error.getMessage());
            }

            @Override
            public void onSuccess(O object) {
                logger.debug(object == null ? "OK" : object.toString());
            }
        };
    }
}
