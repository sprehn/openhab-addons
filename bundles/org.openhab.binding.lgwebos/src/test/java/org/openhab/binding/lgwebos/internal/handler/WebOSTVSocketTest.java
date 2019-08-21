package org.openhab.binding.lgwebos.internal.handler;

import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Test;

public class WebOSTVSocketTest {

    @Test
    public void test() {
        String ipAddress = "192.168.2.119";

        WebOSTVStore configStore = new WebOSTVStore() {
            String uuid;
            String key;

            @Override
            public void setUUID(String uuid) {
                this.uuid = uuid;
            }

            @Override
            public void setKey(String key) {
                this.key = key;
            }

            @Override
            public String getKey() {
                return key;
            }
        };

        WebSocketClient wsClient = new WebSocketClient();
        WebOSTVSocket socket = new WebOSTVSocket(wsClient, configStore, ipAddress);
        socket.connect();
        System.out.println(socket.getState());

        socket.disconnect();
        System.out.println(socket.getState());

    }

}
