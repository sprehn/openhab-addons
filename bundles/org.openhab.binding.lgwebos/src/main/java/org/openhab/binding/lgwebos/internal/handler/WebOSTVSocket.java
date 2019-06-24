package org.openhab.binding.lgwebos.internal.handler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.paho.client.mqttv3.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.URLServiceSubscription;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * WebSocket to handle the communication with WebOS device.
 */
@WebSocket()
public class WebOSTVSocket {
    private Session session;
    private static final Gson GSON = new GsonBuilder().create();

    private static final int PORT = 3001;

    private final static String[] PERMISSIONS = { "LAUNCH", "LAUNCH_WEBAPP", "APP_TO_APP", "CONTROL_AUDIO",
            "CONTROL_INPUT_MEDIA_PLAYBACK", "CONTROL_POWER", "READ_INSTALLED_APPS", "CONTROL_DISPLAY",
            "CONTROL_INPUT_JOYSTICK", "CONTROL_INPUT_MEDIA_RECORDING", "CONTROL_INPUT_TV", "READ_INPUT_DEVICE_LIST",
            "READ_NETWORK_STATE", "READ_TV_CHANNEL_LIST", "WRITE_NOTIFICATION_TOAST", "CONTROL_INPUT_TEXT",
            "CONTROL_MOUSE_AND_KEYBOARD", "READ_CURRENT_CHANNEL", "READ_RUNNING_APPS" };

    private static final JsonObject MANIFEST;
    static {
        MANIFEST = new JsonObject();
        MANIFEST.addProperty("manifestVersion", 1);
        MANIFEST.add("permissions", GSON.toJsonTree(PERMISSIONS));
    }

    private final LGWebOSHandler handler;
    private final WebSocketClient client;
    private final URI destUri;
    // private HashMap<Integer, ServiceCommand<JsonElement>> requests = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(WebOSTVSocket.class);
    private int nextRequestId = 0;

    public WebOSTVSocket(WebSocketClient webSocketClient, LGWebOSHandler handler, String ipAddress) {
        this.handler = handler;
        this.client = webSocketClient;

        URI tempUri = null;
        try {
            tempUri = new URI("wss://" + ipAddress + ":" + PORT);
        } catch (URISyntaxException e) {
            connectionError(e);
        }
        this.destUri = tempUri;

    }

    public void connect() {
        try {
            this.client.start();
            this.client.connect(this, this.destUri);
            System.out.printf("Connecting to : %s%n", this.destUri);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (this.session != null && this.session.isOpen()) {
                this.session.close();
            }
            client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return this.session != null && this.session.isOpen();
    }

    private void connectionError(Exception e) {
        logger.debug("Error connecting to device.", e);
        // handler.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        logger.debug("WebSocket Closed. Code: {}; Reason: {}", statusCode, reason);
        this.session = null;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.debug("WebSocket Connected: {}", session.getRemoteAddress().getAddress());
        this.session = session;
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
        JsonObject packet = new JsonObject();
        packet.addProperty("id", this.nextRequestId++);
        packet.addProperty("type", "register");

        JsonObject payload = new JsonObject();
        String key = handler.getKey();
        if (key != null) {
            payload.addProperty("client-key", key);
        }
        payload.addProperty("pairingType", "PROMPT");
        payload.add("manifest", MANIFEST);
        packet.add("payload", payload);

        sendMessage(packet);
    }

    private void sendMessage(JsonElement packet) {
        String json = GSON.toJson(packet);
        logger.debug("Message [out]: {}", json);
        try {
            this.session.getRemote().sendString(json);
        } catch (IOException e) {
            connectionError(e);
        }
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        logger.debug("Message [in]: {}", message);
        Response response = GSON.fromJson(message, Response.class);

        ServiceCommand<JsonElement> request = null;
        if (response.getId() != null) {
            request = requests.get(response.getId());
        }

        JsonParser parser = new JsonParser();
        JsonObject payload = (JsonObject) parser.parse(message);

        switch (response.getType()) {
            case "registered":
                String key = payload.get("client-key").getAsString();
                handler.setKey(key);

                // Track SSL certificate
                // Not the prettiest way to get it, but we don't have direct access to the SSLEngine
                // ((WebOSTVServiceConfig) mService.getServiceConfig())
                // .setServerCertificate(customTrustManager.getLastCheckedCertificate());

                if (response.getId() != null) {
                    requests.remove(response.getId());
                }

                break;
            case "response":
                if (request != null) {
                    request.getResponseListener().onSuccess(response.getPayload());
                    // if (!(request instanceof URLServiceSubscription)) {
                    // if (!(response.getPayload() instanceof JSONObject // TODO:
                    // && ((JSONObject) response.getPayload()).has("pairingType"))) {
                    // requests.remove(response.getId());
                    // }
                    // }
                } else {
                    logger.warn("No matching request id: {}", response);
                }
                break;
            case "error":
                String error = response.getError();

                if (Strings.isEmpty(error)) {
                    break;
                }

                logger.debug("Error: {}", error);

                if (response.getPayload() != null) {
                    logger.debug("Error Payload: {}" + response.getPayload().toString());
                }

                if (response.getId() != null) {

                    if (request != null) {
                        request.getResponseListener().onError(new ServiceCommandError(error));

                        if (!(request instanceof URLServiceSubscription)) {
                            requests.remove(response.getId());
                        }

                    }
                }
                break;

            case "hello":
                String uuid = payload.get("deviceUUID").getAsString();
                // TODO: store this uuid
                // state registering
                sendRegister();
                break;

        }

    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        // voHandler.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        logger.debug("Connection failed: {}", cause.getMessage());
    }
}