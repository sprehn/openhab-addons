/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.net.NetworkAddressService;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.lgwebos.internal.handler.LGWebOSHandler;
import org.openhab.binding.lgwebos.internal.handler.core.CommandConfirmation;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Power Control Command.
 * Note: Connect SDK only supports powering OFF for most devices.
 *
 * @author Sebastian Prehn - Initial contribution
 */
@NonNullByDefault
public class PowerControlPower extends BaseChannelHandler<CommandConfirmation> {
    private final Logger logger = LoggerFactory.getLogger(PowerControlPower.class);
    private final ConfigProvider configProvider;

    public PowerControlPower(ConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public void onReceiveCommand(String channelId, LGWebOSHandler handler, Command command) {
        if (OnOffType.ON == command) {

            String macAddress = configProvider.getMacAddress();
            if (macAddress.isEmpty()) {
                logger.debug(
                        "Received ON - Turning TV on via API is not supported by LG WebOS TVs. You may succeed using wake on lan (WOL). Please set the macAddress config value in Thing configuration to enable this.");
            } else {
                byte[] bytes = new byte[6];
                String[] hex = macAddress.split("(\\:|\\-)");
                if (hex.length != 6) {
                    logger.warn("Invalid MAC address: {}", macAddress);
                    return;
                }

                try {
                    for (int i = 0; i < 6; i++) {
                        bytes[i] = (byte) Integer.parseInt(hex[i], 16);
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Invalid hex digit in MAC address: {}", macAddress);
                    return;
                }

                logger.debug("WOL packet will be prepared for MAC: {}", macAddress);
                sendWOLPacket(bytes);
            }
        }

        if (!handler.getSocket().isConnected()) {
            /*
             * Unable to send anything to a not connected device.
             * onDeviceReady nor onDeviceRemoved will be called and item state would be permanently inconsistent.
             * Therefore setting state to OFF
             */
            handler.postUpdate(channelId, OnOffType.OFF);
        } else if (OnOffType.OFF == command) {
            handler.getSocket().powerOff(getDefaultResponseListener());
        } else {
            logger.warn("Only accept OnOffType. Type was {}.", command.getClass());
        }

    }

    @Override
    public void onDeviceReady(String channelId, LGWebOSHandler handler) {
        handler.postUpdate(channelId, OnOffType.ON);
    }

    @Override
    public void onDeviceRemoved(String channelId, LGWebOSHandler handler) {
        handler.postUpdate(channelId, OnOffType.OFF);
    }

    public interface ConfigProvider {
        String getMacAddress();
    }

    private static final int PORT = 9;

    private void sendWOLPacket(byte[] mac) {

        BundleContext ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceTracker<NetworkAddressService, NetworkAddressService> serviceTracker = new ServiceTracker<>(ctx,
                NetworkAddressService.class.getName(), null);
        serviceTracker.open();
        NetworkAddressService networkAddressService = serviceTracker.getService();
        if (networkAddressService == null) {
            logger.warn("Unable to obtain network Address Service.");
            return;
        }
        String broadcastAddress = networkAddressService.getConfiguredBroadcastAddress();
        serviceTracker.close();

        if (broadcastAddress == null) {
            logger.warn(
                    "Unable to identify broadcast address. Please configure the network interface in system setting.");
            return;
        }

        InetAddress broadcast;
        try {
            broadcast = InetAddress.getByName(broadcastAddress);
        } catch (UnknownHostException e) {
            logger.warn("Unable to parse WOL broadcast address: {} ", broadcastAddress);
            return;
        }

        ;
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.send(createWolPacket(broadcast, mac));
            logger.debug("WOL packet sent to: {}", broadcast);
        } catch (IOException e) {
            logger.warn("Failed to send WOL packet: {} ", e.getMessage());
            return;
        }
    }

    /**
     * Prepares the magic WOL packet, which consists of six 0xff bytes plus 16 times mac address.
     *
     * @param address the broadcast address
     * @param mac the mac address
     * @return
     */
    private DatagramPacket createWolPacket(InetAddress address, byte[] mac) {
        byte[] bytes = new byte[6 + 16 * mac.length];

        // first 6 bytes are 0xff
        for (int i = 0; i < 6; i++) {
            bytes[i] = (byte) 0xff;
        }
        // repeat mac address 16 times
        for (int i = 6; i < bytes.length; i += mac.length) {
            System.arraycopy(mac, 0, bytes, i, mac.length);
        }

        return new DatagramPacket(bytes, bytes.length, address, PORT);
    }

}
