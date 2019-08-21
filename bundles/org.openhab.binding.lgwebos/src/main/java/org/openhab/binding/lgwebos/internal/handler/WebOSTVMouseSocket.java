package org.openhab.binding.lgwebos.internal.handler;

import java.io.IOException;
import java.net.URI;

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

@WebSocket()
@NonNullByDefault
public class WebOSTVMouseSocket {
    private final Logger logger = LoggerFactory.getLogger(WebOSTVMouseSocket.class);

    public enum State {
        INITIAL,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }

    public enum ButtonType {
        HOME,
        BACK,
        UP,
        DOWN,
        LEFT,
        RIGHT,
    }

    private State state = State.INITIAL;
    private final WebSocketClient client;
    private @Nullable Session session;
    private @Nullable Runnable onConnected;

    public WebOSTVMouseSocket(WebSocketClient client) {
        this.client = client;
        /*
         * try {
         * this.destUri = new URI(path.replace("wss:", "ws:").replace(":3001/", ":3000/")); // downgrade to plaintext
         * } catch (URISyntaxException e) {
         * throw new IllegalArgumentException("Path provided is invalid: " + path);
         * }
         */
    }

    public void connect(URI destUri, @Nullable Runnable onConnected) {
        synchronized (this) {
            if (state != State.INITIAL) {
                logger.debug("Already connecting; not trying to connect again: " + state);
                return;
            }

            state = State.CONNECTING;
        }

        this.onConnected = onConnected;
        try {
            this.client.start();
            this.client.connect(this, destUri);
            logger.debug("Connecting to: {}", destUri);
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
        this.session = null;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.debug("WebSocket Connected to: {}", session.getRemoteAddress().getAddress());
        this.session = session;
        this.state = State.CONNECTED;
        if (this.onConnected != null) {
            this.onConnected.run();
        }
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        logger.debug("Message [in]: {}", message);
    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        // voHandler.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        logger.debug("Connection failed: {}", cause.getMessage());
    }

    private void sendMessage(String msg) {
        try {
            if (this.session != null) {
                logger.debug("Message [out]: {}", msg);
                this.session.getRemote().sendString(msg);
            } else {
                logger.warn("No Connection to TV, skipping [out]: ", msg);
            }

        } catch (IOException e) {
            connectionError(e);
        }
    }

    public void click() {
        sendMessage("type:click\n" + "\n");
    }

    public void button(ButtonType type) {
        String keyName;
        switch (type) {
            case HOME:
                keyName = "HOME";
                break;
            case BACK:
                keyName = "BACK";
                break;
            case UP:
                keyName = "UP";
                break;
            case DOWN:
                keyName = "DOWN";
                break;
            case LEFT:
                keyName = "LEFT";
                break;
            case RIGHT:
                keyName = "RIGHT";
                break;

            default:
                keyName = "NONE";
                break;
        }

        button(keyName);
    }

    public void button(String keyName) {
        sendMessage("type:button\n" + "name:" + keyName + "\n" + "\n");

    }

    public void move(double dx, double dy) {
        sendMessage("type:move\n" + "dx:" + dx + "\n" + "dy:" + dy + "\n" + "down:0\n" + "\n");

    }

    public void move(double dx, double dy, boolean drag) {
        sendMessage("type:move\n" + "dx:" + dx + "\n" + "dy:" + dy + "\n" + "down:" + (drag ? 1 : 0) + "\n" + "\n");

    }

    public void scroll(double dx, double dy) {
        sendMessage("type:scroll\n" + "dx:" + dx + "\n" + "dy:" + dy + "\n" + "\n");
    }

}
