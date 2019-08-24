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

/*
 * This file is based on:
 *
 * WebOSTVService
 * Connect SDK
 *
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 19 Jan 2014
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
package org.openhab.binding.lgwebos.internal.handler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.openhab.binding.lgwebos.internal.handler.WebOSTVMouseSocket.WebOSTVMouseSocketListener;
import org.openhab.binding.lgwebos.internal.handler.command.ServiceCommand;
import org.openhab.binding.lgwebos.internal.handler.command.ServiceSubscription;
import org.openhab.binding.lgwebos.internal.handler.command.URLServiceSubscription;
import org.openhab.binding.lgwebos.internal.handler.core.AppInfo;
import org.openhab.binding.lgwebos.internal.handler.core.ChannelInfo;
import org.openhab.binding.lgwebos.internal.handler.core.LaunchSession;
import org.openhab.binding.lgwebos.internal.handler.core.LaunchSession.LaunchSessionType;
import org.openhab.binding.lgwebos.internal.handler.core.Response;
import org.openhab.binding.lgwebos.internal.handler.core.ResponseListener;
import org.openhab.binding.lgwebos.internal.handler.core.TextInputStatusInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * WebSocket to handle the communication with WebOS device.
 *
 * @author Hyun Kook Khang - Initial contribution
 * @author Sebastian Prehn - Web Socket implementation and adoption for openHAB
 */
@WebSocket()
@NonNullByDefault
public class WebOSTVSocket {

    private static final Gson GSON = new GsonBuilder().create();

    private static final int PORT = 3001;

    public enum State {
        DISCONNECTED,
        CONNECTING,
        REGISTERING,
        REGISTERED,
        DISCONNECTING
    }

    private State state = State.DISCONNECTED;

    private final Config configStore;
    private final WebSocketClient client;
    private @Nullable Session session;
    private final URI destUri;
    private @Nullable WebOSTVSocketListener listener;
    private final WebOSTVKeyboardInput keyboardInput;
    /**
     * Requests to which we are awaiting response.
     */
    private HashMap<Integer, ServiceCommand<?, ?>> requests = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(WebOSTVSocket.class);
    private int nextRequestId = 0;

    private Queue<ServiceCommand<?, ?>> offlineBuffer = new LinkedList<>();

    public WebOSTVSocket(WebSocketClient client, Config configStore, String ipAddress) {
        this.configStore = configStore;
        this.client = client;
        this.keyboardInput = new WebOSTVKeyboardInput(this);

        try {
            this.destUri = new URI("wss://" + ipAddress + ":" + PORT);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("IP Address or Host provided is invalid: " + ipAddress);
        }
    }

    public State getState() {
        return state;
    }

    private void setState(State state) {
        State oldState = this.state;
        this.state = state;
        Optional.ofNullable(this.listener).ifPresent(l -> l.onStateChanged(oldState, this.state));
    }

    public void setListener(@Nullable WebOSTVSocketListener listener) {
        this.listener = listener;
    }

    public void clearRequests() {
        requests.clear();
    }

    public void connect() {
        synchronized (this) {
            if (state != State.DISCONNECTED) {
                logger.debug("Already connecting; not trying to connect again: {}", state);
                return;
            }
            setState(State.CONNECTING);
        }

        try {
            this.client.connect(this, this.destUri);
            logger.debug("Connecting to: {}", this.destUri);
        } catch (Exception e) {
            logger.warn("Unable to connect.", e);
            setState(State.DISCONNECTED);
        }
    }

    public void disconnect() {
        setState(State.DISCONNECTING);
        try {
            Optional.ofNullable(this.session).ifPresent(s -> s.close());
        } catch (Exception e) {
            logger.warn("Error while closing session.", e);
        }
        setState(State.DISCONNECTED);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        logger.debug("WebSocket Closed - Code: {}, Reason: {}", statusCode, reason);
        this.requests.clear();
        this.session = null;
        setState(State.DISCONNECTED);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.debug("WebSocket Connected to: {}", session.getRemoteAddress().getAddress());
        this.session = session;
        sendHello();
    }

    private void sendHello() {
        JsonObject packet = new JsonObject();
        packet.addProperty("id", this.nextRequestId++);
        packet.addProperty("type", "hello");

        JsonObject payload = new JsonObject();
        payload.addProperty("appId", "org.openhab");
        payload.addProperty("appName", "openHAB");
        payload.addProperty("appRegion", Locale.getDefault().getDisplayCountry());
        packet.add("payload", payload);

        sendMessage(packet);
    }

    private void sendRegister() {
        setState(State.REGISTERING);

        JsonObject packet = new JsonObject();
        packet.addProperty("id", this.nextRequestId++);
        packet.addProperty("type", "register");

        JsonObject manifest = new JsonObject();
        manifest.addProperty("manifestVersion", 1);

        String[] permissions = { "LAUNCH", "LAUNCH_WEBAPP", "APP_TO_APP", "CONTROL_AUDIO",
                "CONTROL_INPUT_MEDIA_PLAYBACK", "CONTROL_POWER", "READ_INSTALLED_APPS", "CONTROL_DISPLAY",
                "CONTROL_INPUT_JOYSTICK", "CONTROL_INPUT_MEDIA_RECORDING", "CONTROL_INPUT_TV", "READ_INPUT_DEVICE_LIST",
                "READ_NETWORK_STATE", "READ_TV_CHANNEL_LIST", "WRITE_NOTIFICATION_TOAST", "CONTROL_INPUT_TEXT",
                "CONTROL_MOUSE_AND_KEYBOARD", "READ_CURRENT_CHANNEL", "READ_RUNNING_APPS" };

        manifest.add("permissions", GSON.toJsonTree(permissions));

        JsonObject payload = new JsonObject();
        String key = configStore.getKey();
        if (key != null && !key.isEmpty()) {
            payload.addProperty("client-key", key);
        }
        payload.addProperty("pairingType", "PROMPT"); // PIN, COMBINED
        payload.add("manifest", manifest);
        packet.add("payload", payload);

        sendMessage(packet);
    }

    private int nextRequestId() {
        int requestId;
        do {
            requestId = nextRequestId++;
        } while (requests.containsKey(requestId));
        return requestId;
    }

    public void sendCommand(ServiceCommand<?, ?> command) {
        switch (state) {
            case REGISTERED:
                JsonObject payload = command.getPayload().getAsJsonObject();
                if (payload.has("type") && "p2p".equals(payload.get("type").getAsString())) {
                    // p2p is a special case in which uses a different format
                    this.sendMessage(payload);
                } else {
                    int requestId = nextRequestId();
                    requests.put(requestId, command);
                    JsonObject packet = new JsonObject();
                    packet.addProperty("type", command.getType());
                    packet.addProperty("id", requestId);
                    packet.addProperty("uri", command.getTarget());
                    packet.add("payload", payload);
                    this.sendMessage(packet);
                }
                break;
            case CONNECTING:
            case REGISTERING:
                logger.debug("Queuing command for {}", command.getTarget());
                offlineBuffer.add(command);
                break;
            case DISCONNECTED:
            case DISCONNECTING:
                logger.debug("Queuing command and (re-)starting socket for {}", command.getTarget());
                offlineBuffer.add(command);
                connect();
                break;
        }
    }

    public void unsubscribe(ServiceSubscription<?> subscription) {
        Optional<Entry<Integer, ServiceCommand<?, ?>>> entry = this.requests.entrySet().stream()
                .filter(e -> e.getValue().equals(subscription)).findFirst();
        if (entry.isPresent()) {
            int requestId = entry.get().getKey();
            this.requests.remove(requestId);
            JsonObject packet = new JsonObject();
            packet.addProperty("type", "unsubscribe");
            packet.addProperty("id", requestId);
            sendMessage(packet);
        }
    }

    public void sendMessage(JsonObject json) {
        String msg = GSON.toJson(json);
        Session s = this.session;
        try {
            if (s != null) {
                logger.debug("Message [out]: {}", msg);
                s.getRemote().sendString(msg);
            } else {
                logger.warn("No Connection to TV, skipping [out]: {}", msg);
            }
        } catch (IOException e) {
            logger.error("Unable to send message.", e);
        }
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        logger.debug("Message [in]: {}", message);
        Response response = GSON.fromJson(message, Response.class);
        ServiceCommand<?, ?> request = null;
        if (response.getId() != null) {
            request = requests.get(response.getId());
            if (request == null) {
                logger.warn("Received a response with id {}, for which no request was found. This should not happen.",
                        response.getId());
            } else {
                // for subscriptions we want to keep the original
                // message, so that we have a reference to the response listener
                if (!(request instanceof URLServiceSubscription<?, ?>)) {
                    requests.remove(response.getId());
                }
            }
        }

        switch (response.getType()) {
            case "response":
                if (request == null) {
                    logger.warn("No matching request found for response message: {}", message);
                    break;
                }
                if (response.getPayload() == null) {
                    logger.warn("No payload in response message: {}", message);
                    break;
                }
                request.processResponse(response.getPayload().getAsJsonObject());
                break;
            case "error":
                logger.debug("Error: {}", message);

                if (request == null) {
                    logger.warn("No matching request found for error message: {}", message);
                    break;
                }
                if (response.getPayload() == null) {
                    logger.warn("No payload in error message: {}", message);
                    break;
                }
                request.processError(response.getError());
                break;

            case "hello":
                if (response.getPayload() == null) {
                    logger.warn("No payload in error message: {}", message);
                    break;
                }
                // configStore.storeDeviceUUID(response.getPayload().getAsJsonObject().get("deviceUUID").getAsString());
                sendRegister();
                break;
            case "registered":
                if (response.getPayload() == null) {
                    logger.warn("No payload in registered message: {}", message);
                    break;
                }
                configStore.storeKey(response.getPayload().getAsJsonObject().get("client-key").getAsString());
                setState(State.REGISTERED);

                while (this.offlineBuffer.size() > 0) {
                    sendCommand(this.offlineBuffer.remove());
                }
                break;
        }

    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        Optional.ofNullable(this.listener).ifPresent(l -> l.onError(cause.getMessage()));
        logger.debug("Connection Error.", cause);
    }

    public boolean isConnected() {
        return state == State.REGISTERED;
    }

    public interface WebOSTVSocketListener {

        public void onStateChanged(State oldState, State newState);

        public void onError(String errorMessage);

    }

    private Function<JsonObject, Boolean> MUTE_STATUS_CONVERTER = (jsonObj) -> jsonObj.get("mute").getAsBoolean();

    public ServiceSubscription<ResponseListener<Boolean>> subscribeMute(ResponseListener<Boolean> listener) {
        URLServiceSubscription<Boolean, ResponseListener<Boolean>> request = new URLServiceSubscription<>(MUTE, null,
                true, MUTE_STATUS_CONVERTER, listener);
        sendCommand(request);
        return request;
    }

    private static final Function<JsonObject, Float> VOLUME_CONVERTER = (
            jsonObj) -> (float) (jsonObj.get("volume").getAsInt() / 100.0);

    private static String FOREGROUND_APP = "ssap://com.webos.applicationManager/getForegroundAppInfo";
    // private static String APP_STATUS = "ssap://com.webos.service.appstatus/getAppStatus";
    // private static String APP_STATE = "ssap://system.launcher/getAppState";
    private static String VOLUME = "ssap://audio/getVolume";
    private static String MUTE = "ssap://audio/getMute";
    // private static String VOLUME_STATUS = "ssap://audio/getStatus";
    private static String CHANNEL_LIST = "ssap://tv/getChannelList";
    private static String CHANNEL = "ssap://tv/getCurrentChannel";
    // private static String PROGRAM = "ssap://tv/getChannelProgramInfo";
    // private static String CURRENT_PROGRAM = "ssap://tv/getChannelCurrentProgramInfo";
    // private static String THREED_STATUS = "ssap://com.webos.service.tv.display/get3DStatus";

    public ServiceSubscription<ResponseListener<Float>> subscribeVolume(ResponseListener<Float> listener) {
        URLServiceSubscription<Float, ResponseListener<Float>> request = new URLServiceSubscription<>(VOLUME, null,
                true, VOLUME_CONVERTER, listener);
        sendCommand(request);
        return request;
    }

    public void setMute(boolean isMute, ResponseListener<Object> listener) {
        String uri = "ssap://audio/setMute";
        JsonObject payload = new JsonObject();
        payload.addProperty("mute", isMute);

        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(uri, payload, x -> x, listener);
        sendCommand(request);
    }

    public void setVolume(float volume, ResponseListener<Object> listener) {
        String uri = "ssap://audio/setVolume";
        JsonObject payload = new JsonObject();
        int intVolume = Math.round(volume * 100.0f);
        payload.addProperty("volume", intVolume);
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(uri, payload, x -> x, listener);
        sendCommand(request);
    }

    public void volumeUp(ResponseListener<Object> listener) {
        String uri = "ssap://audio/volumeUp";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(uri, null, x -> x, listener);
        sendCommand(request);
    }

    public void volumeDown(ResponseListener<Object> listener) {
        String uri = "ssap://audio/volumeDown";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(uri, null, x -> x, listener);

        sendCommand(request);
    }

    public ServiceSubscription<ResponseListener<ChannelInfo>> subscribeCurrentChannel(
            ResponseListener<ChannelInfo> listener) {
        URLServiceSubscription<ChannelInfo, ResponseListener<ChannelInfo>> request = new URLServiceSubscription<>(
                CHANNEL, null, true, jsonObj -> parseRawChannelData(jsonObj), listener);
        sendCommand(request);
        return request;
    }

    private ChannelInfo parseRawChannelData(JsonObject channelRawData) {
        String channelName = null;
        String channelId = null;
        String channelNumber = null;

        ChannelInfo channelInfo = new ChannelInfo();

        if (!channelRawData.has("channelName")) {
            channelName = channelRawData.get("channelName").getAsString();
        }

        if (!channelRawData.has("channelId")) {
            channelId = channelRawData.get("channelId").getAsString();
        }

        channelNumber = channelRawData.get("channelNumber").getAsString();

        channelInfo.setName(channelName);
        channelInfo.setId(channelId);
        channelInfo.setChannelNumber(channelNumber);

        return channelInfo;
    }

    public void setChannel(ChannelInfo channelInfo, ResponseListener<Object> listener) {
        JsonObject payload = new JsonObject();
        if (channelInfo.getId() != null) {
            payload.addProperty("channelId", channelInfo.getId());
        }
        if (channelInfo.getChannelNumber() != null) {
            payload.addProperty("channelNumber", channelInfo.getChannelNumber());
        }
        setChannel(payload, listener);
    }

    private void setChannel(JsonObject payload, ResponseListener<Object> listener) {
        String uri = "ssap://tv/openChannel";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(uri, payload, x -> x, listener);
        sendCommand(request);
    }

    private final Function<JsonObject, List<ChannelInfo>> CHANNEL_LIST_CONVERTER = jsonObj -> {
        List<ChannelInfo> list = new ArrayList<>();
        jsonObj.get("channelList").getAsJsonArray()
                .forEach(element -> list.add(parseRawChannelData(element.getAsJsonObject())));
        return list;
    };

    public void channelUp(ResponseListener<Object> listener) {
        String uri = "ssap://tv/channelUp";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(uri, null, x -> x, listener);
        sendCommand(request);
    }

    public void channelDown(ResponseListener<Object> listener) {
        String uri = "ssap://tv/channelDown";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(uri, null, x -> x, listener);
        sendCommand(request);
    }

    public void getChannelList(ResponseListener<List<ChannelInfo>> listener) {
        ServiceCommand<List<ChannelInfo>, ResponseListener<List<ChannelInfo>>> request = new ServiceCommand<>(
                CHANNEL_LIST, null, CHANNEL_LIST_CONVERTER, listener);
        sendCommand(request);
    }

    // TOAST

    public void showToast(String message, ResponseListener<Object> listener) {
        showToast(message, null, null, listener);
    }

    public void showToast(String message, @Nullable String iconData, @Nullable String iconExtension,
            ResponseListener<Object> listener) {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", message);

        if (iconData != null && iconExtension != null) {
            payload.addProperty("iconData", iconData);
            payload.addProperty("iconExtension", iconExtension);
        }

        sendToast(payload, listener);
    }

    private void sendToast(JsonObject payload, ResponseListener<Object> listener) {
        String uri = "ssap://system.notifications/createToast";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(uri, payload, x -> x, listener);
        sendCommand(request);
    }

    // POWER
    public void powerOff(ResponseListener<Object> listener) {
        String uri = "ssap://system/turnOff";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(uri, null, x -> x, listener);
        sendCommand(request);
    }

    // MEDIA CONTROL
    public void play(ResponseListener<Object> listener) {
        String uri = "ssap://media.controls/play";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(uri, null, x -> x, listener);
        sendCommand(request);
    }

    public void pause(ResponseListener<Object> listener) {
        String uri = "ssap://media.controls/pause";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(uri, null, x -> x, listener);
        sendCommand(request);
    }

    public void stop(ResponseListener<Object> listener) {
        String uri = "ssap://media.controls/stop";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(uri, null, x -> x, listener);
        sendCommand(request);
    }

    public void rewind(ResponseListener<Object> listener) {
        String uri = "ssap://media.controls/rewind";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(uri, null, x -> x, listener);
        sendCommand(request);
    }

    public void fastForward(ResponseListener<Object> listener) {
        String uri = "ssap://media.controls/fastForward";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(uri, null, x -> x, listener);
        sendCommand(request);
    }

    // APPS

    public void getAppList(final ResponseListener<List<AppInfo>> listener) {
        String uri = "ssap://com.webos.applicationManager/listApps";

        ServiceCommand<List<AppInfo>, ResponseListener<List<AppInfo>>> request = new ServiceCommand<>(uri, null,
                jsonObj -> GSON.fromJson(jsonObj, new TypeToken<ArrayList<AppInfo>>() {
                }.getType()), listener);

        sendCommand(request);
    }

    public void launchAppWithInfo(AppInfo appInfo, ResponseListener<LaunchSession> listener) {
        launchAppWithInfo(appInfo, null, listener);
    }

    public void launchAppWithInfo(final AppInfo appInfo, @Nullable JsonObject params,
            final ResponseListener<LaunchSession> listener) {
        String uri = "ssap://system.launcher/launch";
        JsonObject payload = new JsonObject();

        final String appId = appInfo.getId();

        String contentId = null;

        if (params != null) {
            contentId = params.get("contentId").getAsString();
        }

        payload.addProperty("id", appId);

        if (contentId != null) {
            payload.addProperty("contentId", contentId);
        }

        if (params != null) {
            payload.add("params", params);
        }

        ServiceCommand<LaunchSession, ResponseListener<LaunchSession>> request = new ServiceCommand<>(uri, payload,
                obj -> {
                    LaunchSession launchSession = new LaunchSession();
                    launchSession.setService(this);
                    launchSession.setAppId(appId); // note that response uses id to mean appId
                    launchSession.setSessionId(obj.get("sessionId").getAsString());
                    launchSession.setSessionType(LaunchSessionType.App);
                    return launchSession;
                }, listener);
        sendCommand(request);
    }

    public void launchBrowser(String url, final ResponseListener<LaunchSession> listener) {
        String uri = "ssap://system.launcher/open";
        JsonObject payload = new JsonObject();
        payload.addProperty("target", url);

        ServiceCommand<LaunchSession, ResponseListener<LaunchSession>> request = new ServiceCommand<>(uri, payload,
                obj -> new LaunchSession() { // TODO: GSON can do this?
                    {
                        setService(WebOSTVSocket.this);
                        setAppId(obj.get("id").getAsString()); // note that response uses id to mean appId
                        setSessionId(obj.get("sessionId").getAsString());
                        setSessionType(LaunchSessionType.App);
                        // setRawData(obj);
                    }
                }, listener);
        sendCommand(request);
    }

    public void closeLaunchSession(LaunchSession launchSession, ResponseListener<Object> listener) {
        WebOSTVSocket service = launchSession.getService();

        switch (launchSession.getSessionType()) {
            case App:
            case ExternalInputPicker:
                service.closeApp(launchSession, listener);
                break;
            /*
             * case Media:
             * if (service instanceof MediaPlayer) {
             * ((MediaPlayer) service).closeMedia(launchSession, listener);
             * }
             * break;
             *
             *
             * case WebApp:
             * if (service instanceof WebAppLauncher) {
             * ((WebAppLauncher) service).closeWebApp(launchSession, listener);
             * }
             * break;
             * case Unknown:
             */
            default:
                listener.onError("This DeviceService does not know ho to close this LaunchSession");
                break;
        }
    }

    public void closeApp(LaunchSession launchSession, ResponseListener<Object> listener) {
        String uri = "ssap://system.launcher/close";
        String appId = launchSession.getAppId();
        String sessionId = launchSession.getSessionId();

        JsonObject payload = new JsonObject();
        payload.addProperty("id", appId);
        payload.addProperty("sessionId", sessionId);

        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(uri, payload, x -> x, listener);
        launchSession.getService().sendCommand(request);
    }

    public ServiceSubscription<ResponseListener<AppInfo>> subscribeRunningApp(ResponseListener<AppInfo> listener) {
        URLServiceSubscription<AppInfo, ResponseListener<AppInfo>> request = new URLServiceSubscription<>(
                FOREGROUND_APP, null, true, jsonObj -> GSON.fromJson(jsonObj, AppInfo.class), listener);
        sendCommand(request);
        return request;

    }

    // KEYBOARD

    public ServiceSubscription<ResponseListener<TextInputStatusInfo>> subscribeTextInputStatus(
            ResponseListener<TextInputStatusInfo> listener) {
        return keyboardInput.connect(listener);
    }

    public void sendText(String input) {
        keyboardInput.addToQueue(input);
    }

    public void sendEnter() {
        keyboardInput.sendEnter();
    }

    public void sendDelete() {
        keyboardInput.sendDel();
    }

    // MOUSE

    public void executeMouse(Consumer<WebOSTVMouseSocket> onConnected) {
        WebOSTVMouseSocket mouseSocket = new WebOSTVMouseSocket(this.client);
        mouseSocket.setListener(new WebOSTVMouseSocketListener() {

            @Override
            public void onStateChanged(WebOSTVMouseSocket.State oldState, WebOSTVMouseSocket.State newState) {
                switch (newState) {
                    case CONNECTED:
                        onConnected.accept(mouseSocket);
                        mouseSocket.disconnect();
                        break;
                    default:
                        break;
                }

            }

            @Override
            public void onError(@NonNull String errorMessage) {
                logger.debug("Error in communication with Mouse Socket: {}", errorMessage);
            }
        });

        String uri = "ssap://com.webos.service.networkinput/getPointerInputSocket";

        ResponseListener<JsonObject> listener = new ResponseListener<JsonObject>() {

            @Override
            public void onSuccess(@Nullable JsonObject jsonObj) {
                if (jsonObj != null) {
                    String socketPath = jsonObj.get("socketPath").getAsString().replace("wss:", "ws:").replace(":3001/",
                            ":3000/");
                    try {
                        mouseSocket.connect(new URI(socketPath));
                    } catch (URISyntaxException e) {
                        logger.warn("Connect mouse error: {}", e.getMessage());
                    }
                }
            }

            @Override
            public void onError(String error) {
                logger.warn("Connect mouse error: {}", error);
            }
        };

        ServiceCommand<JsonObject, ResponseListener<JsonObject>> request = new ServiceCommand<>(uri, null, x -> x,
                listener);
        sendCommand(request);

    }

    public interface Config {
        void storeKey(@Nullable String key);

        @Nullable
        String getKey();
    }

}