/*
 * ServiceCommandError
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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This class implements base service error which is based on HTTP response codes
 */
@NonNullByDefault
public class ServiceCommandError extends Error {

    private static final long serialVersionUID = 4232138682873631468L;

    public ServiceCommandError(String desc) {
        super(desc);
    }

    /**
     * Create an error which indicates that feature is not supported by a service
     *
     * @param feature the feature which is not supported
     *
     * @return ServiceCommandError
     */
    public static ServiceCommandError notSupported(String feature) {
        return new ServiceCommandError("Feature is not supported: " + feature);
    }

}
