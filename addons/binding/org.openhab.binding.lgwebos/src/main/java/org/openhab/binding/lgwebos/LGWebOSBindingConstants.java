/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lgwebos;

import java.util.Set;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

import com.google.common.collect.ImmutableSet;

/**
 * This class defines common constants, which are used across the whole binding.
 *
 * @author Sebastian Prehn - Initial contribution
 */
public class LGWebOSBindingConstants {

    public static final String BINDING_ID = "lgwebos";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_WEBOSTV = new ThingTypeUID(BINDING_ID, "WebOSTV");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = ImmutableSet.of(THING_TYPE_WEBOSTV);

    // List of all Channel ids
    public static final String CHANNEL_VOLUME = "volume";
    public static final String CHANNEL_POWER = "power";
    public static final String CHANNEL_MUTE = "mute";
    public static final String CHANNEL_CHANNEL = "channel";
    public static final String CHANNEL_TOAST = "toast";
    public static final String CHANNEL_VOLUME_UP = "volumeUp";
    public static final String CHANNEL_VOLUME_DOWN = "volumeDown";
    public static final String CHANNEL_CHANNEL_UP = "channelUp";
    public static final String CHANNEL_CHANNEL_DOWN = "channelDown";
    public static final String CHANNEL_CHANNEL_NAME = "channelName";
    public static final String CHANNEL_PROGRAM = "program";
    public static final String CHANNEL_EXT_INPUT = "externalInput";
    public static final String CHANNEL_MEDIA_FORWARD = "mediaForward";
    public static final String CHANNEL_MEDIA_PAUSE = "mediaPause";
    public static final String CHANNEL_MEDIA_PLAY = "mediaPlay";
    public static final String CHANNEL_MEDIA_REWIND = "mediaRewind";
    public static final String CHANNEL_MEDIA_STOP = "mediaStop";
    public static final String CHANNEL_MEDIA_STATE = "mediaState";
    public static final String CHANNEL_APP_LAUCHER = "appLauncher";
    public static final String PROPERTY_IP_ADDRESS = "IP";
}
