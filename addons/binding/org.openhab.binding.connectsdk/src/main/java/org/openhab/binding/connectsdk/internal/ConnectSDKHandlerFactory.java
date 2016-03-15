/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.connectsdk.internal;

import static org.openhab.binding.connectsdk.ConnectSDKBindingConstants.*;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.connectsdk.handler.ConnectSDKHandler;
import org.openhab.binding.connectsdk.internal.discovery.ConnectSDKDiscovery;

/**
 * The {@link ConnectSDKHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Sebastian Prehn - Initial contribution
 */
public class ConnectSDKHandlerFactory extends BaseThingHandlerFactory {
    private ConnectSDKDiscovery discovery;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    protected void bindDiscovery(ConnectSDKDiscovery discovery) {
        this.discovery = discovery;
    }

    protected void unbindDiscovery(ConnectSDKDiscovery discovery) {
        discovery = null;
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (thingTypeUID.equals(THING_TYPE_WebOSTV)) {
            return new ConnectSDKHandler(thing, discovery.getDiscoveryManager());
        }
        return null;
    }

}
