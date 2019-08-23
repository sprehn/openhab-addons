/*
 * DefaultConnectableDeviceStore
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

package com.connectsdk.device;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.connectsdk.core.Context;
import com.connectsdk.core.Log;
import com.connectsdk.core.Util;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Default implementation of ConnectableDeviceStore. It stores data in a file in application
 * data directory.
 */
public class DefaultConnectableDeviceStore implements ConnectableDeviceStore {
    // @cond INTERNAL

    public static final String KEY_VERSION = "version";
    public static final String KEY_CREATED = "created";
    public static final String KEY_UPDATED = "updated";
    public static final String KEY_DEVICES = "devices";

    static final int CURRENT_VERSION = 0;

    static final String DIRPATH = "/android/data/connect_sdk/";
    static final String FILENAME = "StoredDevices";

    static final String IP_ADDRESS = "ipAddress";
    static final String FRIENDLY_NAME = "friendlyName";
    static final String MODEL_NAME = "modelName";
    static final String MODEL_NUMBER = "modelNumber";
    static final String SERVICES = "services";
    static final String DESCRIPTION = "description";
    static final String CONFIG = "config";

    static final String FILTER = "filter";
    static final String UUID = "uuid";
    static final String PORT = "port";

    static final String SERVICE_UUID = "serviceUUID";
    static final String CLIENT_KEY = "clientKey";
    static final String SERVER_CERTIFICATE = "serverCertificate";
    static final String PAIRING_KEY = "pairingKey";

    static final String DEFAULT_SERVICE_WEBOSTV = "WebOSTVService";

    // @endcond

    /** Date (in seconds from 1970) that the ConnectableDeviceStore was created. */
    public long created;
    /** Date (in seconds from 1970) that the ConnectableDeviceStore was last updated. */
    public long updated;
    /** Current version of the ConnectableDeviceStore, may be necessary for migrations */
    public int version;

    // @cond INTERNAL
    private String fileFullPath;

    private Map<String, JsonObject> storedDevices = new ConcurrentHashMap<String, JsonObject>();
    private Map<String, ConnectableDevice> activeDevices = new ConcurrentHashMap<String, ConnectableDevice>();

    public DefaultConnectableDeviceStore(Context context) {
        fileFullPath = context.getDataDir() + "/" + FILENAME;

        load();
    }
    // @endcond

    @Override
    public void addDevice(ConnectableDevice device) {
        if (device == null || device.getServices().size() == 0) {
            return;
        }

        if (!activeDevices.containsKey(device.getId())) {
            activeDevices.put(device.getId(), device);
        }

        JsonObject storedDevice = getStoredDevice(device.getId());

        if (storedDevice != null) {
            updateDevice(device);
        } else {
            storedDevices.put(device.getId(), device.toJSONObject());

            store();
        }
    }

    @Override
    public void removeDevice(ConnectableDevice device) {
        if (device == null) {
            return;
        }

        activeDevices.remove(device.getId());
        storedDevices.remove(device.getId());

        store();
    }

    @Override
    public void updateDevice(ConnectableDevice device) {
        if (device == null || device.getServices().size() == 0) {
            return;
        }

        JsonObject storedDevice = getStoredDevice(device.getId());

        if (storedDevice == null) {
            return;
        }

        storedDevice.addProperty(ConnectableDevice.KEY_LAST_IP, device.getLastKnownIPAddress());
        storedDevice.addProperty(ConnectableDevice.KEY_LAST_SEEN, device.getLastSeenOnWifi());
        storedDevice.addProperty(ConnectableDevice.KEY_LAST_CONNECTED, device.getLastConnected());
        storedDevice.addProperty(ConnectableDevice.KEY_LAST_DETECTED, device.getLastDetection());

        JsonObject services;

        if (storedDevice.has(ConnectableDevice.KEY_SERVICES)) {
            services = storedDevice.get(ConnectableDevice.KEY_SERVICES).getAsJsonObject();
        } else {
            services = new JsonObject();
        }

        for (DeviceService service : device.getServices()) {
            JsonObject serviceInfo = service.toJSONObject();

            if (serviceInfo != null) {
                services.add(service.getServiceDescription().getUUID(), serviceInfo);
            }
        }

        storedDevice.add(ConnectableDevice.KEY_SERVICES, services);

        storedDevices.put(device.getId(), storedDevice);
        activeDevices.put(device.getId(), device);

        store();

    }

    @Override
    public void removeAll() {
        activeDevices.clear();
        storedDevices.clear();

        store();
    }

    @Override
    public ConnectableDevice getDevice(String uuid) {
        if (uuid == null || uuid.length() == 0) {
            return null;
        }

        ConnectableDevice foundDevice = getActiveDevice(uuid);

        if (foundDevice == null) {
            JsonObject foundDeviceInfo = getStoredDevice(uuid);

            if (foundDeviceInfo != null) {
                foundDevice = new ConnectableDevice(foundDeviceInfo);
            }
        }

        return foundDevice;
    }

    private ConnectableDevice getActiveDevice(String uuid) {
        ConnectableDevice foundDevice = activeDevices.get(uuid);

        if (foundDevice == null) {
            for (ConnectableDevice device : activeDevices.values()) {
                for (DeviceService service : device.getServices()) {
                    if (uuid.equals(service.getServiceDescription().getUUID())) {
                        return device;
                    }
                }
            }
        }
        return foundDevice;
    }

    private JsonObject getStoredDevice(String uuid) {
        JsonObject foundDevice = storedDevices.get(uuid);

        if (foundDevice == null) {
            for (JsonObject device : storedDevices.values()) {
                if (device.has(ConnectableDevice.KEY_SERVICES)) {
                    JsonObject services = device.get(ConnectableDevice.KEY_SERVICES).getAsJsonObject();
                    if (services.has(uuid)) {
                        return device;
                    }
                }

            }
        }
        return foundDevice;
    }

    @Override
    public ServiceConfig getServiceConfig(ServiceDescription serviceDescription) {
        if (serviceDescription == null) {
            return null;
        }
        String uuid = serviceDescription.getUUID();
        if (uuid == null || uuid.length() == 0) {
            return null;
        }

        JsonObject device = getStoredDevice(uuid);
        if (device != null) {
            if (device.has(ConnectableDevice.KEY_SERVICES)) {
                JsonObject services = device.get(ConnectableDevice.KEY_SERVICES).getAsJsonObject();
                if (services.has(uuid)) {
                    JsonObject service = services.get(uuid).getAsJsonObject();
                    if (service.has(DeviceService.KEY_CONFIG)) {
                        JsonObject serviceConfigInfo = service.get(DeviceService.KEY_CONFIG).getAsJsonObject();
                        return ServiceConfig.getConfig(serviceConfigInfo);
                    }
                }
            }
        }

        return null;
    }

    // @cond INTERNAL
    private void load() {
        String line;

        File file = new File(fileFullPath);
        version = CURRENT_VERSION;
        created = Util.getTime();
        updated = Util.getTime();
        if (file.exists()) {
            Gson gson = new Gson();
            JsonObject data;
            try {
                data = gson.fromJson(new FileReader(file), JsonObject.class);

                if (data.has(KEY_DEVICES)) {
                    JsonArray deviceArray = data.get(KEY_DEVICES).getAsJsonArray();
                    deviceArray.forEach(element -> {
                        JsonObject device = element.getAsJsonObject();
                        storedDevices.put(device.get(ConnectableDevice.KEY_ID).getAsString(), device);
                    });
                }

                version = Util.optInt(data, KEY_VERSION, CURRENT_VERSION);
                created = Util.optLong(data, KEY_CREATED, 0);
                updated = Util.optLong(data, KEY_UPDATED, 0);
            } catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void store() {
        updated = Util.getTime();

        File output = new File(fileFullPath);

        if (!output.exists() && !output.getParentFile().mkdirs()) {
            Log.e(Util.T, "Failed to create folders structure to device store " + output.getParentFile().toString());
            return;
        }

        JsonObject deviceStore = new JsonObject();

        deviceStore.addProperty(KEY_VERSION, version);
        deviceStore.addProperty(KEY_CREATED, created);
        deviceStore.addProperty(KEY_UPDATED, updated);

        JsonArray deviceArray = new JsonArray();
        storedDevices.values().forEach(device -> deviceArray.add(device));

        deviceStore.add(KEY_DEVICES, deviceArray);

        try (BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8))) {
            out.write(new Gson().toJson(deviceStore));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // @endcond
}
