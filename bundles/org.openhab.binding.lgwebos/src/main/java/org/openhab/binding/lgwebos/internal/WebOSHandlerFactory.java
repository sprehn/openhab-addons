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

import static org.openhab.binding.lgwebos.internal.WebOSBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.io.net.http.WebSocketFactory;
import org.openhab.binding.lgwebos.internal.handler.WebOSHandler;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link WebOSHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Sebastian Prehn - initial contribution
 */
@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.lgwebos")
public class WebOSHandlerFactory extends BaseThingHandlerFactory {
    private final Logger logger = LoggerFactory.getLogger(WebOSHandlerFactory.class);
    @NonNullByDefault({})
    private WebSocketClient webSocketClient;

    @Reference
    protected void setWebSocketClient(WebSocketFactory webSocketFactory) {
        this.webSocketClient = webSocketFactory.getCommonWebSocketClient();
    }

    protected void unsetWebSocketClient(WebSocketFactory webSocketFactory) {
        this.webSocketClient = null;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (thingTypeUID.equals(THING_TYPE_WEBOSTV)) {
            return new WebOSHandler(thing, webSocketClient);
        }
        return null;
    }

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        try {
            this.webSocketClient.start();
        } catch (Exception e) {
            logger.warn("Unable to to start websocket client.", e);
        }
    }

    @Override
    protected void deactivate(ComponentContext componentContext) {
        super.deactivate(componentContext);
        try {
            this.webSocketClient.stop();
        } catch (Exception e) {
            logger.warn("Unable to to start websocket client.", e);
        }
    }

}
