/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.connectsdk;

import java.util.Set;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

import com.google.common.collect.ImmutableSet;

/**
 * The {@link ConnectSDKBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Sebastian Prehn - Initial contribution
 */
public class ConnectSDKBindingConstants {

    public static final String BINDING_ID = "connectsdk";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_WebOSTV = new ThingTypeUID(BINDING_ID, "WebOSTV");

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = ImmutableSet.of(THING_TYPE_WebOSTV);

    // List of all Channel ids
    public final static String CHANNEL_VOLUME = "volume";
    public final static String CHANNEL_POWER = "power";
    public final static String CHANNEL_MUTE = "mute";
    public final static String CHANNEL_CHANNEL = "channel";
    public final static String CHANNEL_TOAST = "toast";
    public final static String CHANNEL_VOLUME_UP = "volumeUp";
    public final static String CHANNEL_VOLUME_DOWN = "volumeDown";
    public final static String CHANNEL_CHANNEL_UP = "channelUp";
    public final static String CHANNEL_CHANNEL_DOWN = "channelDown";
    public final static String CHANNEL_CHANNEL_NAME = "channelName";
    public final static String CHANNEL_PROGRAM = "program";
    public final static String CHANNEL_EXT_INPUT = "externalInput";
    public final static String CHANNEL_MEDIA_FORWARD = "mediaForward";
    public final static String CHANNEL_MEDIA_PAUSE = "mediaPause";
    public final static String CHANNEL_MEDIA_PLAY = "mediaPlay";
    public final static String CHANNEL_MEDIA_REWIND = "mediaRewind";
    public final static String CHANNEL_MEDIA_STOP = "mediaStop";
    public final static String CHANNEL_MEDIA_STATE = "mediaState";
    public final static String CHANNEL_APP_LAUCHER = "appLauncher";
    public final static String PROPERTY_IP_ADDRESS = "IP";
}
