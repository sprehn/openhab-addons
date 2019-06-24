/*
 * ServiceConfig
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

package com.connectsdk.service.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.connectsdk.core.Util;
import com.google.gson.JsonObject;

public class ServiceConfig {
    public static final String KEY_CLASS = "class";
    public static final String KEY_LAST_DETECT = "lastDetection";
    public static final String KEY_UUID = "UUID";
    private String serviceUUID;
    private long lastDetected = Long.MAX_VALUE;

    boolean connected;
    boolean wasConnected;

    public ServiceConfigListener listener;

    public ServiceConfig(String serviceUUID) {
        this.serviceUUID = serviceUUID;
    }

    public ServiceConfig(ServiceDescription desc) {
        this.serviceUUID = desc.getUUID();
        this.connected = false;
        this.wasConnected = false;
        this.lastDetected = Util.getTime();
    }

    public ServiceConfig(ServiceConfig config) {
        this.serviceUUID = config.serviceUUID;
        this.connected = config.connected;
        this.wasConnected = config.wasConnected;
        this.lastDetected = config.lastDetected;

        this.listener = config.listener;
    }

    public ServiceConfig(JsonObject json) {
        serviceUUID = json.get(KEY_UUID).getAsString();
        lastDetected = json.get(KEY_LAST_DETECT).getAsLong();
    }

    @SuppressWarnings("unchecked")
    public static ServiceConfig getConfig(JsonObject json) {
        Class<ServiceConfig> newServiceClass;
        try {
            newServiceClass = (Class<ServiceConfig>) Class
                    .forName(ServiceConfig.class.getPackage().getName() + "." + json.get(KEY_CLASS).getAsString());
            Constructor<ServiceConfig> constructor = newServiceClass.getConstructor(JsonObject.class);

            return constructor.newInstance(json);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getServiceUUID() {
        return serviceUUID;
    }

    public void setServiceUUID(String serviceUUID) {
        this.serviceUUID = serviceUUID;
        notifyUpdate();
    }

    @Override
    public String toString() {
        return serviceUUID;
    }

    public long getLastDetected() {
        return lastDetected;
    }

    public void setLastDetected(long value) {
        lastDetected = value;
        notifyUpdate();
    }

    public void detect() {
        setLastDetected(Util.getTime());
    }

    public ServiceConfigListener getListener() {
        return listener;
    }

    public void setListener(ServiceConfigListener listener) {
        this.listener = listener;
    }

    public JsonObject toJSONObject() {
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty(KEY_CLASS, this.getClass().getSimpleName());
        jsonObj.addProperty(KEY_LAST_DETECT, lastDetected);
        jsonObj.addProperty(KEY_UUID, serviceUUID);
        return jsonObj;
    }

    protected void notifyUpdate() {
        if (listener != null) {
            listener.onServiceConfigUpdate(this);
        }
    }

    public static interface ServiceConfigListener {
        public void onServiceConfigUpdate(ServiceConfig serviceConfig);
    }
}
