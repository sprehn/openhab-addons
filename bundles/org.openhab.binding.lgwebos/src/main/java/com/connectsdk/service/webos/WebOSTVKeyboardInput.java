/*
 * WebOSTVKeyboardInput
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

package com.connectsdk.service.webos;

import java.util.ArrayList;
import java.util.List;

import com.connectsdk.core.TextInputStatusInfo;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.capability.TextInputControl.TextInputStatusListener;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.URLServiceSubscription;
import com.google.gson.JsonObject;

public class WebOSTVKeyboardInput {

    WebOSTVService service;
    boolean waiting;
    List<String> toSend;

    static String KEYBOARD_INPUT = "ssap://com.webos.service.ime/registerRemoteKeyboard";
    static String ENTER = "ENTER";
    static String DELETE = "DELETE";

    public WebOSTVKeyboardInput(WebOSTVService service) {
        this.service = service;
        waiting = false;

        toSend = new ArrayList<String>();
    }

    public void addToQueue(String input) {
        toSend.add(input);
        if (!waiting) {
            sendData();
        }
    }

    public void sendEnter() {
        toSend.add(ENTER);
        if (!waiting) {
            sendData();
        }
    }

    public void sendDel() {
        if (toSend.size() == 0) {
            toSend.add(DELETE);
            if (!waiting) {
                sendData();
            }
        } else {
            toSend.remove(toSend.size() - 1);
        }
    }

    private void sendData() {
        waiting = true;

        String uri;
        String typeTest = toSend.get(0);

        JsonObject payload = new JsonObject();

        if (typeTest.equals(ENTER)) {
            toSend.remove(0);
            uri = "ssap://com.webos.service.ime/sendEnterKey";
        } else if (typeTest.equals(DELETE)) {
            uri = "ssap://com.webos.service.ime/deleteCharacters";

            int count = 0;
            while (toSend.size() > 0 && toSend.get(0).equals(DELETE)) {
                toSend.remove(0);
                count++;
            }

            payload.addProperty("count", count);

        } else {
            uri = "ssap://com.webos.service.ime/insertText";
            StringBuilder sb = new StringBuilder();

            while (toSend.size() > 0 && !(toSend.get(0).equals(DELETE) || toSend.get(0).equals(ENTER))) {
                String text = toSend.get(0);
                sb.append(text);
                toSend.remove(0);
            }

            payload.addProperty("text", sb.toString());
            payload.addProperty("replace", 0);

        }

        ResponseListener<JsonObject> responseListener = new ResponseListener<JsonObject>() {

            @Override
            public void onSuccess(JsonObject response) {
                waiting = false;
                if (toSend.size() > 0) {
                    sendData();
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                waiting = false;
                if (toSend.size() > 0) {
                    sendData();
                }
            }
        };

        ServiceCommand<JsonObject, ResponseListener<JsonObject>> request = new ServiceCommand<>(service, uri, payload,
                true, x -> x, responseListener);
        request.send();
    }

    public URLServiceSubscription<TextInputStatusInfo, TextInputStatusListener> connect(
            final TextInputStatusListener listener) {
        JsonObject response = null;

        URLServiceSubscription<TextInputStatusInfo, TextInputStatusListener> subscription = new URLServiceSubscription<>(
                service, KEYBOARD_INPUT, null, true, rawData -> parseRawKeyboardData(rawData), listener);
        subscription.send();

        return subscription;
    }

    private TextInputStatusInfo parseRawKeyboardData(JsonObject rawData) {
        boolean focused = false;
        String contentType = null;
        boolean predictionEnabled = false;
        boolean correctionEnabled = false;
        boolean autoCapitalization = false;
        boolean hiddenText = false;
        boolean focusChanged = false;

        TextInputStatusInfo keyboard = new TextInputStatusInfo();
        keyboard.setRawData(rawData);

        if (rawData.has("currentWidget")) {
            JsonObject currentWidget = (JsonObject) rawData.get("currentWidget");
            focused = currentWidget.get("focus").getAsBoolean();

            if (currentWidget.has("contentType")) {
                contentType = currentWidget.get("contentType").getAsString();
            }
            if (currentWidget.has("predictionEnabled")) {
                predictionEnabled = currentWidget.get("predictionEnabled").getAsBoolean();
            }
            if (currentWidget.has("correctionEnabled")) {
                correctionEnabled = currentWidget.get("correctionEnabled").getAsBoolean();
            }
            if (currentWidget.has("autoCapitalization")) {
                autoCapitalization = currentWidget.get("autoCapitalization").getAsBoolean();
            }
            if (currentWidget.has("hiddenText")) {
                hiddenText = currentWidget.get("hiddenText").getAsBoolean();
            }
        }
        if (rawData.has("focusChanged")) {
            focusChanged = rawData.get("focusChanged").getAsBoolean();
        }

        keyboard.setFocused(focused);
        keyboard.setContentType(contentType);
        keyboard.setPredictionEnabled(predictionEnabled);
        keyboard.setCorrectionEnabled(correctionEnabled);
        keyboard.setAutoCapitalization(autoCapitalization);
        keyboard.setHiddenText(hiddenText);
        keyboard.setFocusChanged(focusChanged);

        return keyboard;
    }

    // public void disconnect() {
    // subscription.unsubscribe();
    // }
}
