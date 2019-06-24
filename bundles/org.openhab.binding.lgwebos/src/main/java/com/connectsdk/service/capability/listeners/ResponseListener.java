/*
 * ResponseListener
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

package com.connectsdk.service.capability.listeners;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.connectsdk.service.command.ServiceCommandError;

/**
 * Generic asynchronous operation response success handler block. If there is any response data to be processed, it will
 * be provided via the responseObject parameter.
 */
@NonNullByDefault
public interface ResponseListener<T> extends ErrorListener {

    /**
     * Returns the success of the call of type T.
     * Contains the output data as a generic object reference.
     * This value may be any of a number of types as defined by T in subclasses of ResponseListener.
     * It is also possible that responseObject will be <code>null</code> for operations that don't require data to be
     * returned (move mouse, send key code, etc).
     *
     * @param responseObject Response object, can be any number of object types, depending on the
     *                           protocol/capability/etc, even <code>null</code>
     */
    void onSuccess(T responseObject);

    /**
     * Method to return the error that was generated. Will pass an error object with a helpful status code and error
     * message.
     *
     * @param error ServiceCommandError object describing the nature of the problem. Error descriptions are not
     *                  localized and mostly intended for developer use. It is not recommended to display most error
     *                  descriptions in UI elements.
     */
    void onError(ServiceCommandError error);
}
