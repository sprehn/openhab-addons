/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.connectsdk.handler;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.capability.ToastControl;

/**
 * @author Sebastian Prehn
 * @since 1.8.0
 */
public class ToastControlToast extends AbstractChannelHandler<Void> {
    private static final Logger logger = LoggerFactory.getLogger(ToastControlToast.class);

    private ToastControl getControl(final ConnectableDevice device) {
        return device.getCapability(ToastControl.class);
    }

    @Override
    public void onReceiveCommand(final ConnectableDevice d, Command command) {
        if (d.hasCapabilities(ToastControl.Show_Toast)) {
            final String value = command.toString();
            final ToastControl control = getControl(d);
            try {
                BufferedImage bi = ImageIO.read(getClass().getResource("/openhab-logo-square.png"));
                ByteArrayOutputStream os = new ByteArrayOutputStream();

                // only in java 1.8
                // OutputStream b64 = Base64.getEncoder().wrap(os);
                // ImageIO.write(bi, "png", b64);
                // control.showToast(value, os.toString("UTF-8"), "png", createDefaultResponseListener() );

                ImageIO.write(bi, "png", os);
                control.showToast(value, DatatypeConverter.printBase64Binary(os.toByteArray()), "png",
                        createDefaultResponseListener());
            } catch (IOException ex) {
                logger.warn(ex.getMessage(), ex);
            }
        }
    }

}