/*
 * ServiceCommand
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

package com.connectsdk.service.command;

import java.util.function.Function;

import com.connectsdk.service.capability.listeners.ResponseListener;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Internal implementation of ServiceCommand for URL-based commands
 * T is a response listener for a functional type X.
 */
public class ServiceCommand<X, T extends ResponseListener<X>> {
    // public static final String TYPE_REQ = "request";
    public static final String TYPE_SUB = "subscribe";
    public static final String TYPE_GET = "GET";
    public static final String TYPE_POST = "POST";
    // public static final String TYPE_DEL = "DELETE";
    // public static final String TYPE_PUT = "PUT";

    protected ServiceCommandProcessor processor;
    protected String type; // WebOSTV: {request, subscribe}, NetcastTV: {GET, POST}
    protected JsonObject payload;
    protected String target;
    protected Function<JsonObject, X> converter;

    int requestId;

    T responseListener;

    public ServiceCommand(ServiceCommandProcessor processor, String targetURL, JsonObject payload, boolean isWebOS,
            Function<JsonObject, X> converter, T listener) {
        this.processor = processor;
        this.target = targetURL;
        this.payload = payload;
        this.converter = converter;
        this.responseListener = listener;
        this.type = "request";
        requestId = -1;

    }

    public void send() {
        processor.sendCommand(this);
    }

    public ServiceCommandProcessor getCommandProcessor() {
        return processor;
    }

    public JsonElement getPayload() {
        return payload;
    }

    public String getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public void processResponse(JsonObject response) {
        this.getResponseListener().onSuccess(this.converter.apply(response));
    }

    public void processError(String error) {
        this.getResponseListener().onError(new ServiceCommandError(error));
    }

    public T getResponseListener() {
        return responseListener;
    }

    public interface ServiceCommandProcessor {
        public void unsubscribe(URLServiceSubscription<?, ?> subscription);

        public void unsubscribe(ServiceSubscription<?> subscription);

        public void sendCommand(ServiceCommand<?, ?> command);
    }
}