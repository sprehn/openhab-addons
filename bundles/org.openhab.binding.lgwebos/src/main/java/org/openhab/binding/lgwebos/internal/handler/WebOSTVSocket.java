package org.openhab.binding.lgwebos.internal.handler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.URLServiceSubscription;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * WebSocket to handle the communication with WebOS device.
 */
@WebSocket()
@NonNullByDefault
public class WebOSTVSocket {

    private static final Gson GSON = new GsonBuilder().create();

    private static final int PORT = 3001;

    public enum State {
        INITIAL,
        CONNECTING,
        REGISTERING,
        REGISTERED,
        DISCONNECTING
    }

    private State state = State.INITIAL;

    private final WebOSTVStore configStore;
    private final WebSocketClient client;
    private @Nullable Session session;
    private final URI destUri;
    private @Nullable WebOSTVSocketListener listener;

    /**
     * Requests to which we are awaiting response.
     */
    private HashMap<Integer, ServiceCommand<?, ?>> requests = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(WebOSTVSocket.class);
    private int nextRequestId = 0;

    private Queue<ServiceCommand<?, ?>> offlineBuffer = new LinkedList<>();

    public WebOSTVSocket(WebSocketClient client, WebOSTVStore configStore, String ipAddress) {
        this.configStore = configStore;
        this.client = client;

        try {
            this.destUri = new URI("wss://" + ipAddress + ":" + PORT);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("IP Address or Host provided is invalid: " + ipAddress);
        }
    }

    public State getState() {
        return state;
    }

    public void setListener(@Nullable WebOSTVSocketListener listener) {
        this.listener = listener;
    }

    public void clearRequests() {
        requests.clear();
    }

    public void connect() {
        synchronized (this) {
            if (state != State.INITIAL) {
                logger.debug("Already connecting; not trying to connect again: " + state);
                return;
            }

            state = State.CONNECTING;
        }

        try {
            this.client.start();
            this.client.connect(this, this.destUri);
            logger.debug("Connecting to: {}", this.destUri);
        } catch (Exception e) {
            connectionError(e);
        }
    }

    public void disconnect() {
        this.state = State.DISCONNECTING;
        try {
            if (this.session != null) {
                this.session.close();
            }
            client.stop();
            this.state = State.INITIAL;
        } catch (Exception e) {
            connectionError(e);
        }
    }

    private void connectionError(Exception e) {
        this.state = State.INITIAL;
        logger.debug("Error connecting to device.", e);
        // handler.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        this.state = State.INITIAL;
        logger.debug("WebSocket Closed - Code: {}, Reason: {}", statusCode, reason);
        this.requests.clear();
        this.session = null;
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
        this.state = State.REGISTERING;

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
        if (key != null) {
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
            case INITIAL:
            case DISCONNECTING:
                logger.debug("Queuing command and (re-)starting socket for {}", command.getTarget());
                offlineBuffer.add(command);
                connect();
                break;
        }
    }

    public void unsubscribe(URLServiceSubscription<?, ?> subscription) {
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
        try {
            if (this.session != null) {
                logger.debug("Message [out]: {}", msg);
                this.session.getRemote().sendString(msg);
            } else {
                logger.warn("No Connection to TV, skipping [out]: {}", msg);
            }
        } catch (IOException e) {
            connectionError(e);
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
                configStore.setUUID(response.getPayload().getAsJsonObject().get("deviceUUID").getAsString());
                sendRegister();
                break;
            case "registered":
                if (response.getPayload() == null) {
                    logger.warn("No payload in registered message: {}", message);
                    break;
                }
                configStore.setKey(response.getPayload().getAsJsonObject().get("client-key").getAsString());
                this.state = State.REGISTERED;

                while (this.offlineBuffer.size() > 0) {
                    sendCommand(this.offlineBuffer.remove());
                }
                break;
        }

    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        // voHandler.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        logger.debug("Connection Error: {}", cause.getMessage());
    }

    public boolean isConnected() {
        return state == State.REGISTERED;
    }

    public interface WebOSTVSocketListener {

        public void onConnect();

        public void onCloseWithError(ServiceCommandError error);

        public void onFailWithError(ServiceCommandError error);

        public Boolean onReceiveMessage(JsonObject message);

    }
}