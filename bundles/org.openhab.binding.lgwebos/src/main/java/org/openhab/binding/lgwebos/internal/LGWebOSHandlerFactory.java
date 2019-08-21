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

import static org.openhab.binding.lgwebos.internal.LGWebOSBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.lgwebos.handler.LGWebOSHandler;
import org.openhab.binding.lgwebos.internal.discovery.LGWebOSUpnpDiscoveryParticipant;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link LGWebOSHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Sebastian Prehn - initial contribution
 */
@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.lgwebos")
public class LGWebOSHandlerFactory extends BaseThingHandlerFactory {
    private @Nullable LGWebOSUpnpDiscoveryParticipant discovery;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Reference
    protected void bindDiscovery(LGWebOSUpnpDiscoveryParticipant discovery) {
        this.discovery = discovery;
    }

    protected void unbindDiscovery(LGWebOSUpnpDiscoveryParticipant discovery) {
        this.discovery = null;
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        LGWebOSUpnpDiscoveryParticipant lgWebOSDiscovery = discovery;
        if (lgWebOSDiscovery == null) {
            throw new IllegalStateException(
                    "LGWebOSUpnpDiscoveryParticipant must be bound before ThingHandlers can be created");
        }
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (thingTypeUID.equals(THING_TYPE_WEBOSTV)) {
            return new LGWebOSHandler(thing, lgWebOSDiscovery);
        }
        return null;
    }
}
