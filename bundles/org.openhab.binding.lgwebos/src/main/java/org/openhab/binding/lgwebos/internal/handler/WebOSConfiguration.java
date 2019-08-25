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
package org.openhab.binding.lgwebos.internal.handler;

/**
 * The {@link WebOSConfiguration} class contains the thing configuration
 * parameters for LGWebOS devices
 *
 * @author Sebastian Prehn - Initial contribution
 */
public class WebOSConfiguration {
    String ipAddress; // name has to match LGWebOSBindingConstants.CONFIG_IPADDRESS
    int port = 3001;
    String key; // name has to match LGWebOSBindingConstants.CONFIG_KEY
    // String uuid;
    // String mac; // mac address of TV, if set we can attempt wake on lan
}
