/*
 * WebOSWebAppSession
 * Connect SDK
 *
 * Copyright (c) 2014 LG Electronics.
 * Created by Jeffrey Glenn on 07 Mar 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.connectsdk.service.sessions;

import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.binding.lgwebos.internal.handler.WebOSTVSocket;
import org.openhab.binding.lgwebos.internal.handler.WebOSTVSocket.WebOSTVSocketListener;

import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.Log;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.SubtitleInfo;
import com.connectsdk.core.Util;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.PlaylistControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.command.URLServiceSubscription;
import com.connectsdk.service.sessions.LaunchSession.LaunchSessionType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class WebOSWebAppSession extends WebAppSession {
    private static final String NAMESPACE_KEY = "connectsdk.";
    private static final String ENABLED_SUBTITLE_ID = "1";

    protected WebOSTVService service;

    ResponseListener<ServiceCommand<Object, ResponseListener<Object>>> mConnectionListener;

    public WebOSTVSocket socket;
    public URLServiceSubscription<JsonObject, ResponseListener<JsonObject>> appToAppSubscription;

    private ServiceSubscription<PlayStateListener> mPlayStateSubscription;
    private ServiceSubscription<MessageListener> mMessageSubscription;
    private ConcurrentHashMap<String, ServiceCommand<?, ?>> mActiveCommands;

    private ServiceSubscription<WebAppPinStatusListener> mWebAppPinnedSubscription;

    String mFullAppId;

    private int UID;
    private boolean connected;

    public WebOSWebAppSession(LaunchSession launchSession, DeviceService service) {
        super(launchSession, service);

        UID = 0;
        mActiveCommands = new ConcurrentHashMap<String, ServiceCommand<?, ?>>(0, 0.75f, 10);
        connected = false;

        this.service = (WebOSTVService) service;
    }

    private int getNextId() {
        return ++UID;
    }

    public Boolean isConnected() {
        return connected && socket != null && socket.isConnected();
    }

    public void setConnected(Boolean connected) {
        this.connected = connected;
    }

    public void handleMediaEvent(JsonObject payload) {
        String type = payload.get("type").getAsString();
        if (type.length() == 0) {
            String errorMsg = payload.get("error").getAsString();
            if (errorMsg.length() == 0) {
                return;
            } else {
                Log.w(Util.T, "Play State Error: " + errorMsg);
                if (mPlayStateSubscription != null) {
                    for (PlayStateListener listener : mPlayStateSubscription.getListeners()) {
                        Util.postError(listener, new ServiceCommandError("Error handling media event: " + errorMsg));
                    }
                }
            }
        }

        if ("playState".equals(type)) {
            if (mPlayStateSubscription == null) {
                return;
            }

            String playStateString = payload.get(type).getAsString();
            if (playStateString.length() == 0) {
                return;
            }

            final MediaControl.PlayStateStatus playState = parsePlayState(playStateString);

            for (PlayStateListener listener : mPlayStateSubscription.getListeners()) {
                Util.postSuccess(listener, playState);
            }
        }
    }

    public String getFullAppId() {
        if (mFullAppId == null) {
            if (launchSession.getSessionType() != LaunchSessionType.WebApp) {
                mFullAppId = launchSession.getAppId();
            } else {
                Enumeration<String> enumeration = service.getWebAppIdMappings().keys();

                while (enumeration.hasMoreElements()) {
                    String mappedFullAppId = enumeration.nextElement();
                    String mappedAppId = service.getWebAppIdMappings().get(mappedFullAppId);

                    if (mappedAppId.equalsIgnoreCase(launchSession.getAppId())) {
                        mFullAppId = mappedAppId;
                        break;
                    }
                }
            }
        }

        if (mFullAppId == null) {
            return launchSession.getAppId();
        } else {
            return mFullAppId;
        }
    }

    public void setFullAppId(String fullAppId) {
        mFullAppId = fullAppId;
    }

    private WebOSTVSocketListener mSocketListener = new WebOSTVSocketListener() {

        @Override
        public Boolean onReceiveMessage(JsonObject payload) {
            String type = payload.get("type").getAsString();

            if ("p2p".equals(type)) {
                String fromAppId = payload.get("from").getAsString();

                if (!fromAppId.equalsIgnoreCase(getFullAppId())) {
                    return false;
                }

                JsonElement message = payload.get("payload");

                if (message instanceof JsonObject) {
                    JsonObject messageJSON = message.getAsJsonObject();
                    if (messageJSON.has("contentType")) {
                        String contentType = messageJSON.get("contentType").getAsString();
                        Integer contentTypeIndex = contentType.indexOf("connectsdk.");

                        if (contentTypeIndex >= 0) {
                            String payloadKey = contentType.split("connectsdk.")[1];

                            if (payloadKey == null || payloadKey.length() == 0) {
                                return false;
                            }

                            JsonObject messagePayload = messageJSON.get(payloadKey).getAsJsonObject();

                            if (payloadKey.equalsIgnoreCase("media-error")) {
                                handleMediaEvent(messageJSON);
                                return false;
                            }

                            if (messagePayload == null) {
                                return false;
                            }

                            if (payloadKey.equalsIgnoreCase("mediaEvent")) {
                                handleMediaEvent(messagePayload);
                            } else if (payloadKey.equalsIgnoreCase("mediaCommandResponse")) {
                                handleMediaCommandResponse(messagePayload);
                            }
                        }
                    } else {
                        handleMessage(messageJSON);
                    }

                } else if (message instanceof JsonPrimitive) {
                    handleMessage(message.getAsJsonPrimitive().getAsString());
                }

                return false;
            }

            return true;
        }

        @Override
        public void onFailWithError(ServiceCommandError error) {
            connected = false;
            appToAppSubscription = null;

            if (mConnectionListener != null) {
                if (error == null) {
                    error = new ServiceCommandError("Unknown error connecting to web socket");
                }

                mConnectionListener.onError(error);
            }

            mConnectionListener = null;
        }

        @Override
        public void onConnect() {
            if (mConnectionListener != null) {
                mConnectionListener.onSuccess(null);
            }

            mConnectionListener = null;
        }

        @Override
        public void onCloseWithError(ServiceCommandError error) {
            connected = false;
            appToAppSubscription = null;

            if (mConnectionListener != null) {
                if (error != null) {
                    mConnectionListener.onError(error);
                } else {
                    if (getWebAppSessionListener() != null) {
                        getWebAppSessionListener().onWebAppSessionDisconnect(WebOSWebAppSession.this);
                    }
                }
            }

            mConnectionListener = null;
        }

    };

    @SuppressWarnings("unchecked")
    public void handleMediaCommandResponse(final JsonObject payload) {

        if (!payload.has("requestId") || payload.get("requestId").getAsString().isEmpty()) {
            return;
        }
        String requestID = payload.get("requestId").getAsString();

        final ServiceCommand<JsonObject, ResponseListener<JsonObject>> command = (ServiceCommand<JsonObject, ResponseListener<JsonObject>>) mActiveCommands
                .get(requestID);

        if (command == null) {
            return;
        }

        if (payload.has("error") && !payload.get("error").getAsString().isEmpty()) {
            Util.postError(command.getResponseListener(), new ServiceCommandError(payload.get("error").getAsString()));
        } else {
            Util.postSuccess(command.getResponseListener(), payload);
        }

        mActiveCommands.remove(requestID);
    }

    public void handleMessage(final Object message) {
        Util.run(new Runnable() {

            @Override
            public void run() {
                if (getWebAppSessionListener() != null) {
                    getWebAppSessionListener().onReceiveMessage(WebOSWebAppSession.this, message);
                }
            }
        });

    }

    public PlayStateStatus parsePlayState(String playStateString) {
        if (playStateString.equals("playing")) {
            return PlayStateStatus.Playing;
        } else if (playStateString.equals("paused")) {
            return PlayStateStatus.Paused;
        } else if (playStateString.equals("idle")) {
            return PlayStateStatus.Idle;
        } else if (playStateString.equals("buffering")) {
            return PlayStateStatus.Buffering;
        } else if (playStateString.equals("finished")) {
            return PlayStateStatus.Finished;
        }

        return PlayStateStatus.Unknown;
    }

    @Override
    public void connect(ResponseListener<Object> connectionListener) {
        connect(false, connectionListener);
    }

    @Override
    public void join(ResponseListener<Object> connectionListener) {
        connect(true, connectionListener);
    }

    private void connect(final Boolean joinOnly, final ResponseListener<Object> connectionListener) {
        if (socket != null && socket.getState() == WebOSTVSocket.State.CONNECTING) {
            if (connectionListener != null) {
                connectionListener.onError(new ServiceCommandError(
                        "You have a connection request pending,  please wait until it has finished"));
            }

            return;
        }

        if (isConnected()) {
            if (connectionListener != null) {
                connectionListener.onSuccess(null);
            }

            return;
        }

        mConnectionListener = new ResponseListener<ServiceCommand<Object, ResponseListener<Object>>>() {

            @Override
            public void onError(ServiceCommandError error) {
                if (socket != null) {
                    disconnectFromWebApp();
                }

                if (connectionListener != null) {
                    if (error == null) {
                        error = new ServiceCommandError("Unknown error connecting to web app");
                    }

                    connectionListener.onError(error);
                }
            }

            @Override
            public void onSuccess(ServiceCommand<Object, ResponseListener<Object>> object) {
                ResponseListener<Object> finalConnectionListener = new ResponseListener<Object>() {

                    @Override
                    public void onError(ServiceCommandError error) {
                        disconnectFromWebApp();

                        if (connectionListener != null) {
                            connectionListener.onError(error);
                        }
                    }

                    @Override
                    public void onSuccess(Object object) {
                        connected = true;

                        if (connectionListener != null) {
                            connectionListener.onSuccess(object);
                        }
                    }
                };

                service.connectToWebApp(WebOSWebAppSession.this, joinOnly, finalConnectionListener);
            }
        };

        if (socket != null) {
            if (socket.isConnected()) {
                mConnectionListener.onSuccess(null);
            } else {
                socket.connect();
            }
        } else {

            // TODO: cant we reference socekt, instead of creating it here.
            // socket = new WebOSTVSocket(service, this.service, service.getServiceDescription().getIpAddress());
            socket.setListener(mSocketListener);
            socket.connect();
        }
    }

    @Override
    public void disconnectFromWebApp() {
        connected = false;
        mConnectionListener = null;

        if (appToAppSubscription != null) {
            appToAppSubscription.removeListeners();
            appToAppSubscription = null;
        }

        if (socket != null) {
            socket.setListener(null);
            socket.clearRequests();
            socket.disconnect();
            socket = null;
        }
    }

    @Override
    public void sendMessage(final String message, final ResponseListener<Object> listener) {
        if (message == null || message.length() == 0) {
            Util.postError(listener, new ServiceCommandError("Cannot send an Empty Message"));
            return;
        }

        sendP2PMessage(new JsonPrimitive(message), listener);
    }

    @Override
    public void sendMessage(final JsonObject message, final ResponseListener<Object> listener) {
        if (message == null || message.size() == 0) {
            Util.postError(listener, new ServiceCommandError("Cannot send an Empty Message"));
            return;
        }

        sendP2PMessage(message, listener);
    }

    private void sendP2PMessage(final JsonElement message, final ResponseListener<Object> listener) {
        JsonObject _payload = new JsonObject();

        _payload.addProperty("type", "p2p");
        _payload.addProperty("to", getFullAppId());
        _payload.add("payload", message);

        if (isConnected()) {
            socket.sendMessage(_payload);

            Util.postSuccess(listener, null);
        } else {
            ResponseListener<Object> connectListener = new ResponseListener<Object>() {

                @Override
                public void onError(ServiceCommandError error) {
                    Util.postError(listener, error);
                }

                @Override
                public void onSuccess(Object object) {
                    sendP2PMessage(message, listener);
                }
            };

            connect(connectListener);
        }
    }

    @Override
    public void close(ResponseListener<Object> listener) {
        mActiveCommands.clear();

        if (mPlayStateSubscription != null) {
            mPlayStateSubscription.unsubscribe();
            mPlayStateSubscription = null;
        }

        if (mMessageSubscription != null) {
            mMessageSubscription.unsubscribe();
            mMessageSubscription = null;
        }

        if (mWebAppPinnedSubscription != null) {
            mWebAppPinnedSubscription.unsubscribe();
            mWebAppPinnedSubscription = null;
        }

        service.getWebAppLauncher().closeWebApp(launchSession, listener);
    }

    @Override
    public void pinWebApp(String webAppId, ResponseListener<Object> listener) {
        service.getWebAppLauncher().pinWebApp(webAppId, listener);
    }

    @Override
    public void unPinWebApp(String webAppId, ResponseListener<Object> listener) {
        service.getWebAppLauncher().unPinWebApp(webAppId, listener);
    }

    @Override
    public void isWebAppPinned(String webAppId, WebAppPinStatusListener listener) {
        service.getWebAppLauncher().isWebAppPinned(webAppId, listener);
    }

    @Override
    public ServiceSubscription<WebAppPinStatusListener> subscribeIsWebAppPinned(String webAppId,
            WebAppPinStatusListener listener) {
        mWebAppPinnedSubscription = service.getWebAppLauncher().subscribeIsWebAppPinned(webAppId, listener);
        return mWebAppPinnedSubscription;
    }

    @Override
    public void seek(final long position, ResponseListener<Object> listener) {
        if (position < 0) {
            Util.postError(listener, new ServiceCommandError("Must pass a valid positive value"));
            return;
        }

        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);

        JsonObject message = new JsonObject();
        message.addProperty("contentType", NAMESPACE_KEY + "mediaCommand");
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "seek");
        payload.addProperty("position", position / 1000);
        payload.addProperty("requestId", requestId);
        message.add("mediaCommand", payload);

        ServiceCommand<Object, ResponseListener<Object>> command = new ServiceCommand<Object, ResponseListener<Object>>(
                null, null, null, true, x -> x, listener);

        mActiveCommands.put(requestId, command);

        sendMessage(message, listener);
    }

    @Override
    public void getPosition(final PositionListener listener) {
        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);

        JsonObject message = new JsonObject();
        message.addProperty("contentType", NAMESPACE_KEY + "mediaCommand");
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "getPosition");
        payload.addProperty("requestId", requestId);
        message.add("mediaCommand", payload);

        ServiceCommand<JsonObject, ResponseListener<JsonObject>> command = new ServiceCommand<>(null, null, null, true,
                x -> x, new ResponseListener<JsonObject>() {

                    @Override
                    public void onSuccess(JsonObject jsonObj) {
                        long position = jsonObj.get("position").getAsLong();
                        Util.postSuccess(listener, position * 1000);
                    }

                    @Override
                    public void onError(ServiceCommandError error) {
                        Util.postError(listener, error);
                    }
                });

        mActiveCommands.put(requestId, command);

        sendMessage(message, new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        });
    }

    @Override
    public void getDuration(final DurationListener listener) {
        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);
        JsonObject message = new JsonObject();
        message.addProperty("contentType", NAMESPACE_KEY + "mediaCommand");
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "getDuration");
        payload.addProperty("requestId", requestId);
        message.add("mediaCommand", payload);

        ServiceCommand<JsonObject, ResponseListener<JsonObject>> command = new ServiceCommand<>(null, null, null, true,
                x -> x, new ResponseListener<JsonObject>() {

                    @Override
                    public void onSuccess(JsonObject jsonObj) {
                        long position = jsonObj.get("duration").getAsLong();
                        Util.postSuccess(listener, position * 1000);
                    }

                    @Override
                    public void onError(ServiceCommandError error) {
                        Util.postError(listener, error);
                    }
                });

        mActiveCommands.put(requestId, command);

        sendMessage(message, new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        });
    }

    @Override
    public void getPlayState(final PlayStateListener listener) {
        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);

        JsonObject message = new JsonObject();
        message.addProperty("contentType", NAMESPACE_KEY + "mediaCommand");
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "getPlayState");
        payload.addProperty("requestId", requestId);
        payload.add("mediaCommand", payload);

        ServiceCommand<JsonObject, ResponseListener<JsonObject>> command = new ServiceCommand<>(null, null, null, true,
                x -> x, new ResponseListener<JsonObject>() {

                    @Override
                    public void onSuccess(JsonObject response) {
                        String playStateString = response.get("playState").getAsString();
                        PlayStateStatus playState = parsePlayState(playStateString);
                        Util.postSuccess(listener, playState);

                    }

                    @Override
                    public void onError(ServiceCommandError error) {
                        Util.postError(listener, error);
                    }
                });

        mActiveCommands.put(requestId, command);

        sendMessage(message, new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object response) {
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        });
    }

    @Override
    public ServiceSubscription<PlayStateListener> subscribePlayState(final PlayStateListener listener) {
        if (mPlayStateSubscription == null) {
            mPlayStateSubscription = new URLServiceSubscription<PlayStateStatus, MediaControl.PlayStateListener>(null,
                    null, null, true, x -> null, null);
        }

        if (!connected) {
            connect(new ResponseListener<Object>() {

                @Override
                public void onError(ServiceCommandError error) {
                    Util.postError(listener, error);
                }

                @Override
                public void onSuccess(Object response) {
                }
            });
        }

        return mPlayStateSubscription;
    }

    /*****************
     * Media Control *
     *****************/
    @Override
    public MediaControl getMediaControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getMediaControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    /****************
     * Media Player *
     ****************/
    @Override
    public MediaPlayer getMediaPlayer() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    private void displayImage(final String url, final String mimeType, final String title, final String description,
            final String iconSrc, final MediaPlayer.LaunchListener listener) {
        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);

        JsonObject message = new JsonObject();
        message.addProperty("contentType", NAMESPACE_KEY + "mediaCommand");
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "displayImage");
        payload.addProperty("mediaURL", url);
        payload.addProperty("iconURL", iconSrc);
        payload.addProperty("title", title);
        payload.addProperty("description", description);
        payload.addProperty("mimeType", mimeType);
        payload.addProperty("requestId", requestId);
        message.add("mediaCommand", payload);

        ResponseListener<Object> response = new ResponseListener<Object>() {

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }

            @Override
            public void onSuccess(Object object) {
                Util.postSuccess(listener, new MediaLaunchObject(launchSession, getMediaControl()));
            }
        };

        ServiceCommand<Object, ResponseListener<Object>> command = new ServiceCommand<>(this.service, null, null, true,
                x -> x, response);

        mActiveCommands.put(requestId, command);

        sendP2PMessage(message, new ResponseListener<Object>() {

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }

            @Override
            public void onSuccess(Object object) {
            }
        });
    }

    @Override
    public void displayImage(MediaInfo mediaInfo, MediaPlayer.LaunchListener listener) {
        String mediaUrl = null;
        String mimeType = null;
        String title = null;
        String desc = null;
        String iconSrc = null;

        if (mediaInfo != null) {
            mediaUrl = mediaInfo.getUrl();
            mimeType = mediaInfo.getMimeType();
            title = mediaInfo.getTitle();
            desc = mediaInfo.getDescription();

            if (mediaInfo.getImages() != null && mediaInfo.getImages().size() > 0) {
                ImageInfo imageInfo = mediaInfo.getImages().get(0);
                iconSrc = imageInfo.getUrl();
            }
        }

        displayImage(mediaUrl, mimeType, title, desc, iconSrc, listener);
    }

    @Override
    public void playMedia(final MediaInfo mediaInfo, final boolean shouldLoop,
            final MediaPlayer.LaunchListener listener) {
        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);
        JsonObject message = null;
        ImageInfo iconImage = null;
        List<ImageInfo> images = mediaInfo.getImages();

        if (images != null && !images.isEmpty()) {
            iconImage = images.get(0);
        }

        final String iconSrc = iconImage == null ? null : iconImage.getUrl();
        final SubtitleInfo subtitleInfo = mediaInfo.getSubtitleInfo();

        message = createPlayMediaJsonRequest(mediaInfo, shouldLoop, requestId, iconSrc, subtitleInfo);

        ResponseListener<Object> response = new ResponseListener<Object>() {

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }

            @Override
            public void onSuccess(Object object) {
                Util.postSuccess(listener,
                        new MediaLaunchObject(launchSession, getMediaControl(), getPlaylistControl()));
            }
        };

        ServiceCommand<Object, ResponseListener<Object>> command = new ServiceCommand<Object, ResponseListener<Object>>(
                null, null, null, true, x -> x, response);

        mActiveCommands.put(requestId, command);

        sendMessage(message, new ResponseListener<Object>() {

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }

            @Override
            public void onSuccess(Object object) {
            }
        });
    }

    @NonNull
    private JsonObject createPlayMediaJsonRequest(final MediaInfo mediaInfo, final boolean shouldLoop,
            final String requestId, final String iconSrc, final SubtitleInfo subtitleInfo) {

        JsonObject message = new JsonObject();
        message.addProperty("contentType", NAMESPACE_KEY + "mediaCommand");
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "playMedia");
        payload.addProperty("mediaURL", mediaInfo.getUrl());
        payload.addProperty("iconURL", iconSrc);
        payload.addProperty("title", mediaInfo.getTitle());
        payload.addProperty("description", mediaInfo.getDescription());
        payload.addProperty("mimeType", mediaInfo.getMimeType());
        payload.addProperty("shouldLoop", shouldLoop);
        payload.addProperty("requestId", requestId);
        if (subtitleInfo != null) {
            JsonObject subs = new JsonObject();
            subs.addProperty("default", ENABLED_SUBTITLE_ID);
            subs.addProperty("enabled", ENABLED_SUBTITLE_ID);
            JsonObject track = new JsonObject();
            track.addProperty("id", ENABLED_SUBTITLE_ID);
            track.addProperty("language", subtitleInfo.getLanguage());
            track.addProperty("source", subtitleInfo.getUrl());
            track.addProperty("label", subtitleInfo.getLabel());
            JsonArray tracks = new JsonArray();
            tracks.add(track);
            subs.add("tracks", tracks);
            payload.add("subtitles", subs);

        }

        message.add("mediaCommand", payload);
        return message;
    }

    /****************
     * Playlist Control *
     ****************/
    @Override
    public PlaylistControl getPlaylistControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getPlaylistControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void jumpToTrack(final long index, final ResponseListener<Object> listener) {
        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);

        JsonObject message = new JsonObject();
        message.addProperty("contentType", NAMESPACE_KEY + "mediaCommand");
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "jumpToTrack");
        payload.addProperty("requestId", requestId);
        payload.addProperty("index", (int) index);
        message.add("mediaCommand", payload);

        ServiceCommand<Object, ResponseListener<Object>> command = new ServiceCommand<Object, ResponseListener<Object>>(
                null, null, null, true, x -> x, listener);
        mActiveCommands.put(requestId, command);
        sendMessage(message, listener);
    }

    @Override
    public void previous(final ResponseListener<Object> listener) {
        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);

        JsonObject message = new JsonObject();
        message.addProperty("contentType", NAMESPACE_KEY + "mediaCommand");
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "playPrevious");
        payload.addProperty("requestId", requestId);
        message.add("mediaCommand", payload);

        ServiceCommand<Object, ResponseListener<Object>> command = new ServiceCommand<Object, ResponseListener<Object>>(
                null, null, null, true, x -> x, listener);
        mActiveCommands.put(requestId, command);
        sendMessage(message, listener);
    }

    @Override
    public void next(final ResponseListener<Object> listener) {
        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);

        JsonObject message = new JsonObject();
        message.addProperty("contentType", NAMESPACE_KEY + "mediaCommand");
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "playNext");
        payload.addProperty("requestId", requestId);
        message.add("mediaCommand", payload);

        ServiceCommand<Object, ResponseListener<Object>> command = new ServiceCommand<Object, ResponseListener<Object>>(
                null, null, null, true, x -> x, listener);
        mActiveCommands.put(requestId, command);
        sendMessage(message, listener);
    }
}
