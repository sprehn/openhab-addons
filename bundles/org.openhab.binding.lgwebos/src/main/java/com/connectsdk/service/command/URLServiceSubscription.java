/*
 * URLServiceSubscription
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.connectsdk.service.capability.listeners.ResponseListener;
import com.google.gson.JsonObject;

/**
 * Internal implementation of ServiceSubscription for URL-based commands.
 */
public class URLServiceSubscription<X, T extends ResponseListener<X>> extends ServiceCommand<X, T>
        implements ServiceSubscription<T> {
    private List<T> listeners = new ArrayList<T>();

    /*
     * public URLServiceSubscription(ServiceCommandProcessor processor, String uri, JsonObject payload,
     * Function<JsonObject, X> converter, T listener) {
     * super(processor, uri, payload, converter, listener);
     * }
     */

    public URLServiceSubscription(String uri, JsonObject payload, boolean isWebOS, Function<JsonObject, X> converter,
            T listener) {
        super(uri, payload, isWebOS, converter, listener);

        type = TYPE_SUB;
    }

    @Override
    public T addListener(T listener) {
        listeners.add(listener);

        return listener;
    }

    @Override
    public void removeListener(T listener) {
        listeners.remove(listener);
    }

    public void removeListeners() {
        listeners.clear();
    }

    @Override
    public List<T> getListeners() {
        return listeners;
    }
}
