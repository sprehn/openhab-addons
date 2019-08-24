/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/*
 * This file is based on:
 *
 * ChannelInfo
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
package org.openhab.binding.lgwebos.internal.handler.core;

/**
 * {@link ChannelInfo} is a value object to describe a channel on WebOSTV.
 * The id value is mandatory when starting an channel. The channelName is a human readable friendly name, which is not
 * further interpreted by the TV.
 *
 * @author Hyun Kook Khang - Connect SDK initial contribution
 * @author Sebastian Prehn - Adoption for openHAB
 */
public class ChannelInfo {

    private String channelName;
    private String channelId;
    private String channelNumber;
    private int minorNumber;
    private int majorNumber;

    public ChannelInfo() {
    }

    public String getName() {
        return channelName;
    }

    public void setName(String channelName) {
        this.channelName = channelName;
    }

    public String getId() {
        return channelId;
    }

    public void setId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelNumber() {
        return channelNumber;
    }

    public void setChannelNumber(String channelNumber) {
        this.channelNumber = channelNumber;
    }

    /*
     * public JsonObject toJSONObject() {
     * JsonObject obj = new JsonObject();
     *
     * obj.addProperty("name", channelName);
     * obj.addProperty("id", channelId);
     * obj.addProperty("number", channelNumber);
     * obj.addProperty("majorNumber", majorNumber);
     * obj.addProperty("minorNumber", minorNumber);
     * obj.add("rawData", rawData);
     *
     * return obj;
     * }
     */

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        if (channelId != null) {
            result = prime * result + channelId.hashCode();
        } else {
            result = prime * result + ((channelName == null) ? 0 : channelName.hashCode());
            result = prime * result + ((channelNumber == null) ? 0 : channelNumber.hashCode());
            result = prime * result + majorNumber;
            result = prime * result + minorNumber;
        }
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
        ChannelInfo other = (ChannelInfo) obj;

        if (channelId != null) {
            if (channelId.equals(other.channelId)) {
                return true;
            }
        }

        if (channelName == null) {
            if (other.channelName != null) {
                return false;
            }
        } else if (!channelName.equals(other.channelName)) {
            return false;
        }
        if (channelNumber == null) {
            if (other.channelNumber != null) {
                return false;
            }
        } else if (!channelNumber.equals(other.channelNumber)) {
            return false;
        }
        if (majorNumber != other.majorNumber) {
            return false;
        }
        if (minorNumber != other.minorNumber) {
            return false;
        }
        return true;
    }
}
