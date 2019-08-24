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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.lgwebos.internal.handler.WebOSHandler;
import org.openhab.binding.lgwebos.internal.handler.command.ServiceSubscription;
import org.openhab.binding.lgwebos.internal.handler.core.ResponseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract implementation of ChannelHander which serves as a base class for all concrete instances.
 *
 * @author Sebastian Prehn - initial contribution
 */
@NonNullByDefault
abstract class BaseChannelHandler<T, R> implements ChannelHandler {
    private final Logger logger = LoggerFactory.getLogger(BaseChannelHandler.class);

    private final ResponseListener<R> defaultResponseListener = new ResponseListener<R>() {

        @Override
        public void onError(@Nullable String error) {
            logger.warn("{}: received error response: {}", getClass().getName(), error);
        }

        @Override
        public void onSuccess(@Nullable R object) {
            logger.debug("{}: {}.", getClass().getName(), object);
        }
    };

    // IP to Subscriptions map
    private Map<ThingUID, ServiceSubscription<T>> subscriptions = new ConcurrentHashMap<>();

    @Override
    public void onDeviceReady(String channelId, WebOSHandler handler) {
        // NOP
    }

    @Override
    public void onDeviceRemoved(String channelId, WebOSHandler handler) {
        // NOP
    }

    @Override
    public final synchronized void refreshSubscription(String channelId, WebOSHandler handler) {
        removeAnySubscription(handler);
        if (handler.isChannelInUse(channelId)) { // only listen if least one item is configured for this channel
            Optional<ServiceSubscription<T>> listener = getSubscription(channelId, handler);

            if (listener.isPresent()) {
                logger.debug("Subscribed {} on IP: {}", this.getClass().getName(), handler.getThing().getUID());
                subscriptions.put(handler.getThing().getUID(), listener.get());
            }
        }
    }

    /**
     * Creates a subscription instance for this device if subscription is supported.
     *
     * @param device device to which state changes to subscribe to
     * @param channelID channel ID
     * @param handler
     * @return an {@code Optional} containing the ServiceSubscription, or an empty {@code Optional} if subscription is
     *         not supported.
     */
    protected Optional<ServiceSubscription<T>> getSubscription(String channelId, WebOSHandler handler) {
        return Optional.empty();
    }

    @Override
    public final synchronized void removeAnySubscription(WebOSHandler handler) {
        ServiceSubscription<T> l = subscriptions.remove(handler.getThing().getUID());
        if (l != null) {
            handler.getSocket().unsubscribe(l);
            logger.debug("Unsubscribed {} on IP: {}", this.getClass().getName(), handler.getThing().getUID());
        }
    }

    protected ResponseListener<R> getDefaultResponseListener() {
        return defaultResponseListener;
    }

}
