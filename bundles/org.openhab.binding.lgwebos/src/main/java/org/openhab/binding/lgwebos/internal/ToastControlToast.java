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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.lgwebos.internal.handler.WebOSHandler;

/**
 * Handles Toast Control Command. This allows to send messages to the TV screen.
 *
 * @author Sebastian Prehn - initial contribution
 */
@NonNullByDefault
public class ToastControlToast extends BaseChannelHandler<Object> {

    @Override
    public void onReceiveCommand(String channelId, WebOSHandler handler, Command command) {
        handler.getSocket().showToast(command.toString(), getDefaultResponseListener());
    }
}
