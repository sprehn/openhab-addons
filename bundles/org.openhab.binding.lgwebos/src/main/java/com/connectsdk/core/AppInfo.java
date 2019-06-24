/*
 * AppInfo
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

package com.connectsdk.core;

import com.google.gson.JsonObject;

/**
 * Normalized reference object for information about a DeviceService's app. This
 * object will, in most cases, be used to launch apps.
 *
 * In some cases, all that is needed to launch an app is the app id.
 */
public class AppInfo implements JSONSerializable {
    // @cond INTERNAL
    String id;
    String name;
    JsonObject raw;

    // @endcond

    /**
     * Default constructor method.
     */
    public AppInfo() {
    }

    /**
     * Default constructor method.
     *
     * @param id
     *               App id to launch
     */
    public AppInfo(String id) {
        this.id = id;
    }

    /**
     * Gets the ID of the app on the first screen device. Format is different
     * depending on the platform. (ex. youtube.leanback.v4, 0000001134, netflix,
     * etc).
     *
     * @return ID of the app on the first screen device
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of the app on the first screen device. Format is different
     * depending on the platform. (ex. youtube.leanback.v4, 0000001134, netflix,
     * etc).
     *
     * @param id ID of the app on the first screen device
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the user-friendly name of the app (ex. YouTube, Browser, Netflix,
     * etc).
     *
     * @return user-friendly name of the app
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user-friendly name of the app (ex. YouTube, Browser, Netflix,
     * etc).
     *
     * @param name user-friendly name of the app
     */
    public void setName(String name) {
        this.name = name.trim();
    }

    /** @return the raw data from the first screen device about the app. */
    public JsonObject getRawData() {
        return raw;
    }

    /** @param data the raw data from the first screen device about the app. */
    public void setRawData(JsonObject data) {
        raw = data;
    }

    // @cond INTERNAL
    @Override
    public JsonObject toJSONObject() {
        JsonObject obj = new JsonObject();

        obj.addProperty("name", name);
        obj.addProperty("id", id);

        return obj;
    }

    // @endcond

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AppInfo other = (AppInfo) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

}
