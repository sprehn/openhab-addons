/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lgwebos.handler;

import org.eclipse.smarthome.core.library.types.NextPreviousType;
import org.eclipse.smarthome.core.library.types.PlayPauseType;
import org.eclipse.smarthome.core.library.types.RewindFastforwardType;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaControl.PlayStateListener;
import com.connectsdk.service.capability.MediaControl.PlayStateStatus;
import com.connectsdk.service.capability.PlaylistControl;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;

/**
 * Handles commands of a Player Item.
 * Subscribes to the current play state.
 *
 * @author Sebastian Prehn
 * @since 2.2.0
 */
public class MediaControlPlayer extends BaseChannelHandler<PlayStateListener> {
    private Logger logger = LoggerFactory.getLogger(MediaControlPlayer.class);

    private MediaControl getMediaControl(final ConnectableDevice device) {
        return device.getCapability(MediaControl.class);
    }

    private PlaylistControl getPlayListControl(final ConnectableDevice device) {
        return device.getCapability(PlaylistControl.class);
    }

    @Override
    public void onReceiveCommand(final ConnectableDevice d, Command command) {
        if (command instanceof NextPreviousType) {
            if (NextPreviousType.NEXT.equals(command) && d.hasCapabilities(PlaylistControl.Next)) {
                getPlayListControl(d).next(createDefaultResponseListener());
            }
            if (NextPreviousType.PREVIOUS.equals(command) && d.hasCapabilities(MediaControl.Rewind)) {
                getPlayListControl(d).previous(createDefaultResponseListener());
            }
        } else if (command instanceof PlayPauseType) {
            if (PlayPauseType.PLAY.equals(command) && d.hasCapabilities(MediaControl.Play)) {
                getMediaControl(d).play(createDefaultResponseListener());
            }
            if (PlayPauseType.PAUSE.equals(command) && d.hasCapabilities(MediaControl.Pause)) {
                getMediaControl(d).pause(createDefaultResponseListener());
            }
        } else if (command instanceof RewindFastforwardType) {
            if (RewindFastforwardType.FASTFORWARD.equals(command) && d.hasCapabilities(MediaControl.FastForward)) {
                getMediaControl(d).fastForward(createDefaultResponseListener());
            }
            if (RewindFastforwardType.REWIND.equals(command) && d.hasCapabilities(MediaControl.Rewind)) {
                getMediaControl(d).rewind(createDefaultResponseListener());
            }
        } else {
            logger.warn("Only accept NextPreviousType, PlayPauseType, RewindFastforwardType. Type was {}.",
                    command.getClass());
        }
    }

    @Override
    protected ServiceSubscription<PlayStateListener> getSubscription(final ConnectableDevice device,
            final String channelId, final LGWebOSHandler handler) {
        if (device.hasCapability(MediaControl.PlayState_Subscribe)) {
            return getMediaControl(device).subscribePlayState(new PlayStateListener() {

                @Override
                public void onError(ServiceCommandError error) {
                    logger.warn("{} {} {}", error.getCode(), error.getPayload(), error.getMessage());
                }

                @Override
                public void onSuccess(PlayStateStatus status) {
                    switch (status) {
                        case Playing:
                            handler.postUpdate(channelId, PlayPauseType.PLAY);
                            break;
                        case Paused:
                            handler.postUpdate(channelId, PlayPauseType.PAUSE);
                            break;
                        case Unknown:
                        case Buffering:
                        case Idle:
                        case Finished:
                        default:
                            handler.postUpdate(channelId, UnDefType.UNDEF);
                    }
                }
            });
        } else {
            return null;
        }
    }
}
