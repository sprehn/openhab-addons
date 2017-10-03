/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lgwebos.internal;

import static org.openhab.binding.lgwebos.LGWebOSBindingConstants.*;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.lgwebos.handler.LGWebOSHandler;
import org.openhab.binding.lgwebos.internal.discovery.LGWebOSDiscovery;

/**
 * The {@link LGWebOSHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Sebastian Prehn - Initial contribution
 */
public class LGWebOSHandlerFactory extends BaseThingHandlerFactory {
    private LGWebOSDiscovery discovery;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    protected void bindDiscovery(LGWebOSDiscovery discovery) {
        this.discovery = discovery;
    }

    protected void unbindDiscovery(LGWebOSDiscovery discovery) {
        discovery = null;
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (thingTypeUID.equals(THING_TYPE_WEBOSTV)) {
            return new LGWebOSHandler(thing, discovery.getDiscoveryManager());
        }
        return null;
    }
}
