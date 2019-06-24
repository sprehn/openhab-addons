/*
 * Util
 * Connect SDK
 *
 * Copyright (c) 2014 LG Electronics.
 * Created by Jeffrey Glenn on 27 Feb 2014
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

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.google.gson.JsonObject;

@NonNullByDefault
public final class Util {
    static public final String T = "Connect SDK";

    @Nullable
    static private ExecutorService executor;

    /**
     * Configure Util on component start.
     *
     * @param e must not be <code>null</code>
     */
    public static void init(ExecutorService e) {
        executor = e;
    }

    public static void uninit() {
        executor = null;
    }

    public static void run(Runnable runnable) {
        executor.execute(runnable);
    }

    public static <T> void postSuccess(final ResponseListener<T> listener, final T object) {

        executor.execute(new Runnable() {

            @Override
            public void run() {
                listener.onSuccess(object);
            }
        });
    }

    public static <T> void postError(final ResponseListener<T> listener, final ServiceCommandError error) {

        executor.execute(new Runnable() {

            @Override
            public void run() {
                listener.onError(error);
            }
        });
    }

    public static long getTime() {
        return TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());
    }

    public static @Nullable String optString(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : null;
    }

    public static int optInt(JsonObject json, String key, int defaultValue) {
        return json.has(key) ? json.get(key).getAsInt() : defaultValue;
    }

    public static long optLong(JsonObject json, String key, long defaultValue) {
        return json.has(key) ? json.get(key).getAsInt() : defaultValue;
    }

}