/*
 * WebOSTVService
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

package com.connectsdk.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.lgwebos.internal.handler.WebOSTVMouseSocket;
import org.openhab.binding.lgwebos.internal.handler.WebOSTVSocket;

import com.connectsdk.core.AppInfo;
import com.connectsdk.core.ChannelInfo;
import com.connectsdk.core.ExternalInputInfo;
import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.Log;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.PointF;
import com.connectsdk.core.ProgramInfo;
import com.connectsdk.core.ProgramList;
import com.connectsdk.core.Util;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManager.PairingLevel;
import com.connectsdk.service.capability.CapabilityMethods;
import com.connectsdk.service.capability.ExternalInputControl;
import com.connectsdk.service.capability.KeyControl;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.MouseControl;
import com.connectsdk.service.capability.PlaylistControl;
import com.connectsdk.service.capability.PowerControl;
import com.connectsdk.service.capability.TVControl;
import com.connectsdk.service.capability.TextInputControl;
import com.connectsdk.service.capability.ToastControl;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.WebAppLauncher;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.command.URLServiceSubscription;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.config.WebOSTVServiceConfig;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.LaunchSession.LaunchSessionType;
import com.connectsdk.service.sessions.WebAppSession;
import com.connectsdk.service.sessions.WebAppSession.WebAppPinStatusListener;
import com.connectsdk.service.sessions.WebOSWebAppSession;
import com.connectsdk.service.webos.WebOSTVKeyboardInput;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class WebOSTVService extends DeviceService
        implements Launcher, MediaControl, MediaPlayer, VolumeControl, TVControl, ToastControl, ExternalInputControl,
        MouseControl, TextInputControl, PowerControl, KeyControl, WebAppLauncher, PlaylistControl {

    public static final String ID = "webOS TV";
    private static final String MEDIA_PLAYER_ID = "MediaPlayer";

    public interface WebOSTVServicePermission {
        public enum Open implements WebOSTVServicePermission {
            LAUNCH,
            LAUNCH_WEB,
            APP_TO_APP,
            CONTROL_AUDIO,
            CONTROL_INPUT_MEDIA_PLAYBACK
        }

        public enum Protected implements WebOSTVServicePermission {
            CONTROL_POWER,
            READ_INSTALLED_APPS,
            CONTROL_DISPLAY,
            CONTROL_INPUT_JOYSTICK,
            CONTROL_INPUT_MEDIA_RECORDING,
            CONTROL_INPUT_TV,
            READ_INPUT_DEVICE_LIST,
            READ_NETWORK_STATE,
            READ_TV_CHANNEL_LIST,
            WRITE_NOTIFICATION_TOAST
        }

        public enum PersonalActivity implements WebOSTVServicePermission {
            CONTROL_INPUT_TEXT,
            CONTROL_MOUSE_AND_KEYBOARD,
            READ_CURRENT_CHANNEL,
            READ_RUNNING_APPS
        }

    }

    public final static String[] kWebOSTVServiceOpenPermissions = { "LAUNCH", "LAUNCH_WEBAPP", "APP_TO_APP",
            "CONTROL_AUDIO", "CONTROL_INPUT_MEDIA_PLAYBACK" };

    public final static String[] kWebOSTVServiceProtectedPermissions = { "CONTROL_POWER", "READ_INSTALLED_APPS",
            "CONTROL_DISPLAY", "CONTROL_INPUT_JOYSTICK", "CONTROL_INPUT_MEDIA_RECORDING", "CONTROL_INPUT_TV",
            "READ_INPUT_DEVICE_LIST", "READ_NETWORK_STATE", "READ_TV_CHANNEL_LIST", "WRITE_NOTIFICATION_TOAST" };

    public final static String[] kWebOSTVServicePersonalActivityPermissions = { "CONTROL_INPUT_TEXT",
            "CONTROL_MOUSE_AND_KEYBOARD", "READ_CURRENT_CHANNEL", "READ_RUNNING_APPS" };

    public interface SecureAccessTestListener extends ResponseListener<Boolean> {
    }

    public interface ACRAuthTokenListener extends ResponseListener<String> {
    }

    public interface LaunchPointsListener extends ResponseListener<JsonArray> {
    }

    static String FOREGROUND_APP = "ssap://com.webos.applicationManager/getForegroundAppInfo";
    static String APP_STATUS = "ssap://com.webos.service.appstatus/getAppStatus";
    static String APP_STATE = "ssap://system.launcher/getAppState";
    static String VOLUME = "ssap://audio/getVolume";
    static String MUTE = "ssap://audio/getMute";
    static String VOLUME_STATUS = "ssap://audio/getStatus";
    static String CHANNEL_LIST = "ssap://tv/getChannelList";
    static String CHANNEL = "ssap://tv/getCurrentChannel";
    static String PROGRAM = "ssap://tv/getChannelProgramInfo";
    static String CURRENT_PROGRAM = "ssap://tv/getChannelCurrentProgramInfo";
    static String THREED_STATUS = "ssap://com.webos.service.tv.display/get3DStatus";

    static final String CLOSE_APP_URI = "ssap://system.launcher/close";
    static final String CLOSE_MEDIA_URI = "ssap://media.viewer/close";
    static final String CLOSE_WEBAPP_URI = "ssap://webapp/closeWebApp";

    WebOSTVMouseSocket mouseSocket;

    WebOSTVKeyboardInput keyboardInput;

    ConcurrentHashMap<String, String> mAppToAppIdMappings;

    ConcurrentHashMap<String, WebOSWebAppSession> mWebAppSessions;

    WebOSTVSocket socket;

    List<String> permissions;

    public WebOSTVService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
        super(serviceDescription, serviceConfig);
        setServiceDescription(serviceDescription);

        mAppToAppIdMappings = new ConcurrentHashMap<String, String>();
        mWebAppSessions = new ConcurrentHashMap<String, WebOSWebAppSession>();
    }

    @Override
    public CapabilityPriorityLevel getPriorityLevel(Class<? extends CapabilityMethods> clazz) {
        if (clazz.equals(MediaPlayer.class)) {
            return getMediaPlayerCapabilityLevel();
        } else if (clazz.equals(MediaControl.class)) {
            return getMediaControlCapabilityLevel();
        } else if (clazz.equals(Launcher.class)) {
            return getLauncherCapabilityLevel();
        } else if (clazz.equals(TVControl.class)) {
            return getTVControlCapabilityLevel();
        } else if (clazz.equals(VolumeControl.class)) {
            return getVolumeControlCapabilityLevel();
        } else if (clazz.equals(ExternalInputControl.class)) {
            return getExternalInputControlPriorityLevel();
        } else if (clazz.equals(MouseControl.class)) {
            return getMouseControlCapabilityLevel();
        } else if (clazz.equals(TextInputControl.class)) {
            return getTextInputControlCapabilityLevel();
        } else if (clazz.equals(PowerControl.class)) {
            return getPowerControlCapabilityLevel();
        } else if (clazz.equals(KeyControl.class)) {
            return getKeyControlCapabilityLevel();
        } else if (clazz.equals(ToastControl.class)) {
            return getToastControlCapabilityLevel();
        } else if (clazz.equals(WebAppLauncher.class)) {
            return getWebAppLauncherCapabilityLevel();
        } else if (clazz.equals(PlaylistControl.class)) {
            return getPlaylistControlCapabilityLevel();
        }
        return CapabilityPriorityLevel.NOT_SUPPORTED;
    }

    @Override
    public void setServiceDescription(ServiceDescription serviceDescription) {
        super.setServiceDescription(serviceDescription);

        if (this.serviceDescription.getVersion() == null && this.serviceDescription.getResponseHeaders() != null) {
            String serverInfo = serviceDescription.getResponseHeaders().get("Server").get(0);
            String systemOS = serverInfo.split(" ")[0];
            String[] versionComponents = systemOS.split("/");
            String systemVersion = versionComponents[versionComponents.length - 1];

            this.serviceDescription.setVersion(systemVersion);

            this.updateCapabilities();
        }
    }

    private DeviceService getDLNAService() {
        Map<String, ConnectableDevice> allDevices = DiscoveryManager.getInstance().getAllDevices();
        ConnectableDevice device = null;
        DeviceService service = null;

        if (allDevices != null && allDevices.size() > 0) {
            device = allDevices.get(this.serviceDescription.getIpAddress());
        }

        if (device != null) {
            service = device.getServiceByName("DLNA");
        }

        return service;
    }

    public static DiscoveryFilter discoveryFilter() {
        return new DiscoveryFilter(ID, "urn:lge-com:service:webos-second-screen:1");
    }

    @Override
    public boolean isConnected() {
        return this.socket.isConnected();
    }

    @Override
    public void connect() {
        this.socket.connect();
    }

    @Override
    public void disconnect() {
        this.socket.disconnect();

        if (mAppToAppIdMappings != null) {
            mAppToAppIdMappings.clear();
        }

        if (mWebAppSessions != null) {
            Enumeration<WebOSWebAppSession> iterator = mWebAppSessions.elements();

            while (iterator.hasMoreElements()) {
                WebOSWebAppSession session = iterator.nextElement();
                session.disconnectFromWebApp();
            }

            mWebAppSessions.clear();
        }
    }

    // @cond INTERNAL

    public ConcurrentHashMap<String, String> getWebAppIdMappings() {
        return mAppToAppIdMappings;
    }

    // @endcond

    @Override
    public Launcher getLauncher() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getLauncherCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void launchApp(String appId, ResponseListener<LaunchSession> listener) {
        AppInfo appInfo = new AppInfo();
        appInfo.setId(appId);

        launchAppWithInfo(appInfo, listener);
    }

    @Override
    public void launchAppWithInfo(AppInfo appInfo, ResponseListener<LaunchSession> listener) {
        launchAppWithInfo(appInfo, null, listener);
    }

    @Override
    public void launchAppWithInfo(final AppInfo appInfo, JsonObject params,
            final ResponseListener<LaunchSession> listener) {
        String uri = "ssap://system.launcher/launch";
        JsonObject payload = new JsonObject();

        final String appId = appInfo.getId();

        String contentId = null;

        if (params != null) {

            contentId = params.get("contentId").getAsString();

        }

        payload.addProperty("id", appId);

        if (contentId != null) {
            payload.addProperty("contentId", contentId);
        }

        if (params != null) {
            payload.add("params", params);
        }

        ResponseListener<JsonObject> responseListener = new ResponseListener<JsonObject>() {

            @Override
            public void onSuccess(JsonObject obj) {
                LaunchSession launchSession = new LaunchSession();
                launchSession.setService(WebOSTVService.this);
                launchSession.setAppId(appId); // note that response uses id to mean appId
                launchSession.setSessionId(obj.get("sessionId").getAsString());
                launchSession.setSessionType(LaunchSessionType.App);
                Util.postSuccess(listener, launchSession);
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<JsonObject, ResponseListener<JsonObject>> request = new ServiceCommand<>(this, uri, payload,
                true, x -> x, responseListener);
        request.send();
    }

    @Override
    public void launchBrowser(String url, final ResponseListener<LaunchSession> listener) {
        String uri = "ssap://system.launcher/open";
        JsonObject payload = new JsonObject();

        ResponseListener<JsonObject> responseListener = new ResponseListener<JsonObject>() {

            @Override
            public void onSuccess(JsonObject obj) {
                LaunchSession launchSession = new LaunchSession();
                launchSession.setService(WebOSTVService.this);
                launchSession.setAppId(obj.get("id").getAsString()); // note that response uses id to mean appId
                launchSession.setSessionId(obj.get("sessionId").getAsString());
                launchSession.setSessionType(LaunchSessionType.App);
                launchSession.setRawData(obj);

                Util.postSuccess(listener, launchSession);
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        payload.addProperty("target", url);

        ServiceCommand<JsonObject, ResponseListener<JsonObject>> request = new ServiceCommand<>(this, uri, payload,
                true, x -> x, responseListener);
        request.send();
    }

    @Override
    public void launchYouTube(String contentId, ResponseListener<LaunchSession> listener) {
        launchYouTube(contentId, (float) 0.0, listener);
    }

    @Override
    public void launchYouTube(final String contentId, float startTime, final ResponseListener<LaunchSession> listener) {
        JsonObject params = new JsonObject();

        if (contentId != null && contentId.length() > 0) {
            if (startTime < 0.0) {
                Util.postError(listener, new ServiceCommandError("Start time may not be negative"));
                return;
            }

            params.addProperty("contentId",
                    String.format("%s&pairingCode=%s&t=%.1f", contentId, UUID.randomUUID().toString(), startTime));

        }

        AppInfo appInfo = new AppInfo() {
            {
                setId("youtube.leanback.v4");
                setName("YouTube");
            }
        };

        launchAppWithInfo(appInfo, params, listener);
    }

    @Override
    public void launchHulu(String contentId, ResponseListener<LaunchSession> listener) {
        JsonObject params = new JsonObject();

        params.addProperty("contentId", contentId);

        AppInfo appInfo = new AppInfo() {
            {
                setId("hulu");
                setName("Hulu");
            }
        };

        launchAppWithInfo(appInfo, params, listener);
    }

    @Override
    public void launchNetflix(String contentId, ResponseListener<LaunchSession> listener) {
        JsonObject params = new JsonObject();
        String netflixContentId = "m=http%3A%2F%2Fapi.netflix.com%2Fcatalog%2Ftitles%2Fmovies%2F" + contentId
                + "&source_type=4";

        params.addProperty("contentId", netflixContentId);

        AppInfo appInfo = new AppInfo() {
            {
                setId("netflix");
                setName("Netflix");
            }
        };

        launchAppWithInfo(appInfo, params, listener);
    }

    @Override
    public void launchAppStore(String appId, ResponseListener<LaunchSession> listener) {
        AppInfo appInfo = new AppInfo("com.webos.app.discovery");
        appInfo.setName("LG Store");

        JsonObject params = new JsonObject();

        if (appId != null && appId.length() > 0) {
            String query = String.format("category/GAME_APPS/%s", appId);
            params.addProperty("query", query);
        }

        launchAppWithInfo(appInfo, params, listener);
    }

    @Override
    public void closeApp(LaunchSession launchSession, ResponseListener<Object> listener) {
        String uri = "ssap://system.launcher/close";
        String appId = launchSession.getAppId();
        String sessionId = launchSession.getSessionId();

        JsonObject payload = new JsonObject();
        payload.addProperty("id", appId);
        payload.addProperty("sessionId", sessionId);

        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(launchSession.getService(), uri,
                payload, true, x -> x, listener);
        request.send();
    }

    @Override
    public void getAppList(final AppListListener listener) {
        String uri = "ssap://com.webos.applicationManager/listApps";

        ResponseListener<JsonObject> responseListener = new ResponseListener<JsonObject>() {

            @Override
            public void onSuccess(JsonObject jsonObj) {
                JsonArray apps = jsonObj.get("apps").getAsJsonArray();
                List<AppInfo> appList = new ArrayList<AppInfo>();
                apps.forEach(app -> {
                    JsonObject obj = app.getAsJsonObject();
                    AppInfo appInfo = new AppInfo() {
                        {
                            setId(obj.get("id").getAsString());
                            setName(obj.get("title").getAsString());
                            setRawData(obj);
                        }
                    };

                    appList.add(appInfo);
                });

                Util.postSuccess(listener, appList);

            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<JsonObject, ResponseListener<JsonObject>> request = new ServiceCommand<>(this, uri, null, true,
                x -> x, responseListener);
        request.send();
    }

    private static final Function<JsonObject, AppInfo> APP_INFO_CONVERTER = (jsonObj) -> new AppInfo() {
        {
            setId(jsonObj.get("appId").getAsString());
            setName(jsonObj.get("appName").getAsString());
            setRawData(jsonObj);
        }
    };

    @Override
    public void getRunningApp(AppInfoListener listener) {
        ServiceCommand<AppInfo, AppInfoListener> request = new ServiceCommand<>(this, FOREGROUND_APP, null, true,
                APP_INFO_CONVERTER, listener);
        request.send();
    }

    @Override
    public ServiceSubscription<AppInfoListener> subscribeRunningApp(AppInfoListener listener) {
        URLServiceSubscription<AppInfo, AppInfoListener> request = new URLServiceSubscription<>(this, FOREGROUND_APP,
                null, true, APP_INFO_CONVERTER, listener);
        request.send();
        return request;

    }

    private static final Function<JsonObject, AppState> APP_STATE_CONVERTER = (jsonObj) -> new AppState(
            jsonObj.get("running").getAsBoolean(), jsonObj.get("visible").getAsBoolean());

    private JsonObject createAppStateParameter(LaunchSession launchSession) {
        JsonObject params = new JsonObject();
        params.addProperty("id", launchSession.getAppId());
        params.addProperty("sessionId", launchSession.getSessionId());
        return params;
    }

    @Override
    public void getAppState(LaunchSession launchSession, AppStateListener listener) {
        ServiceCommand<AppState, AppStateListener> request = new ServiceCommand<>(this, APP_STATE,
                createAppStateParameter(launchSession), true, APP_STATE_CONVERTER, listener);
        request.send();
    }

    @Override
    public ServiceSubscription<AppStateListener> subscribeAppState(LaunchSession launchSession,
            AppStateListener listener) {
        URLServiceSubscription<AppState, AppStateListener> request = new URLServiceSubscription<>(this, APP_STATE,
                createAppStateParameter(launchSession), true, APP_STATE_CONVERTER, listener);
        request.send();
        return request;
    }

    /******************
     * TOAST CONTROL
     *****************/
    @Override
    public ToastControl getToastControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getToastControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void showToast(String message, ResponseListener<Object> listener) {
        showToast(message, null, null, listener);
    }

    @Override
    public void showToast(String message, String iconData, String iconExtension, ResponseListener<Object> listener) {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", message);

        if (iconData != null) {
            payload.addProperty("iconData", iconData);
            payload.addProperty("iconExtension", iconExtension);
        }

        sendToast(payload, listener);
    }

    @Override
    public void showClickableToastForApp(String message, AppInfo appInfo, JsonObject params,
            ResponseListener<Object> listener) {
        showClickableToastForApp(message, appInfo, params, null, null, listener);
    }

    @Override
    public void showClickableToastForApp(String message, AppInfo appInfo, JsonObject params, String iconData,
            String iconExtension, ResponseListener<Object> listener) {
        JsonObject payload = new JsonObject();

        payload.addProperty("message", message);

        if (iconData != null) {
            payload.addProperty("iconData", iconData);
            payload.addProperty("iconExtension", iconExtension);
        }

        if (appInfo != null) {
            JsonObject onClick = new JsonObject();
            onClick.addProperty("appId", appInfo.getId());
            if (params != null) {
                onClick.add("params", params);
            }
            payload.add("onClick", onClick);
        }

        sendToast(payload, listener);
    }

    @Override
    public void showClickableToastForURL(String message, String url, ResponseListener<Object> listener) {
        showClickableToastForURL(message, url, null, null, listener);
    }

    @Override
    public void showClickableToastForURL(String message, String url, String iconData, String iconExtension,
            ResponseListener<Object> listener) {
        JsonObject payload = new JsonObject();

        payload.addProperty("message", message);

        if (iconData != null) {
            payload.addProperty("iconData", iconData);
            payload.addProperty("iconExtension", iconExtension);
        }

        if (url != null) {
            JsonObject onClick = new JsonObject();
            onClick.addProperty("target", url);
            payload.add("onClick", onClick);
        }

        sendToast(payload, listener);
    }

    private void sendToast(JsonObject payload, ResponseListener<Object> listener) {
        String uri = "ssap://system.notifications/createToast";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, payload, true,
                x -> x, listener);
        request.send();
    }

    /******************
     * VOLUME CONTROL
     *****************/
    @Override
    public VolumeControl getVolumeControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getVolumeControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    public void volumeUp() {
        volumeUp(null);
    }

    @Override
    public void volumeUp(ResponseListener<Object> listener) {
        String uri = "ssap://audio/volumeUp";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, null, true, x -> x,
                listener);
        request.send();
    }

    public void volumeDown() {
        volumeDown(null);
    }

    @Override
    public void volumeDown(ResponseListener<Object> listener) {
        String uri = "ssap://audio/volumeDown";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, null, true, x -> x,
                listener);

        request.send();
    }

    public void setVolume(int volume) {
        setVolume(volume, null);
    }

    @Override
    public void setVolume(float volume, ResponseListener<Object> listener) {
        String uri = "ssap://audio/setVolume";
        JsonObject payload = new JsonObject();
        int intVolume = Math.round(volume * 100.0f);
        payload.addProperty("volume", intVolume);
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, payload, true,
                x -> x, listener);
        request.send();
    }

    private static final Function<JsonObject, Float> VOLUME_CONVERTER = (
            jsonObj) -> (float) (jsonObj.get("volume").getAsInt() / 100.0);

    @Override
    public void getVolume(VolumeListener listener) {
        ServiceCommand<Float, VolumeListener> request = new ServiceCommand<>(this, VOLUME, null, true, VOLUME_CONVERTER,
                listener);
        request.send();
    }

    @Override

    public ServiceSubscription<VolumeListener> subscribeVolume(VolumeListener listener) {
        URLServiceSubscription<Float, VolumeListener> request = new URLServiceSubscription<>(this, VOLUME, null, true,
                VOLUME_CONVERTER, listener);
        request.send();
        return request;
    }

    @Override
    public void setMute(boolean isMute, ResponseListener<Object> listener) {
        String uri = "ssap://audio/setMute";
        JsonObject payload = new JsonObject();
        payload.addProperty("mute", isMute);

        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, payload, true,
                x -> x, listener);
        request.send();
    }

    private Function<JsonObject, Boolean> MUTE_STATUS_CONVERTER = (jsonObj) -> jsonObj.get("mute").getAsBoolean();

    @Override
    public void getMute(MuteListener listener) {
        ServiceCommand<Boolean, MuteListener> request = new ServiceCommand<>(this, MUTE, null, true,
                MUTE_STATUS_CONVERTER, listener);
        request.send();
    }

    @Override

    public ServiceSubscription<MuteListener> subscribeMute(MuteListener listener) {
        URLServiceSubscription<Boolean, MuteListener> request = new URLServiceSubscription<>(this, MUTE, null, true,
                MUTE_STATUS_CONVERTER, listener);
        request.send();
        return request;
    }

    private Function<JsonObject, VolumeStatus> VOLUME_STATUS_CONVERTER = (jsonObj) -> new VolumeStatus(
            jsonObj.get("mute").getAsBoolean(), jsonObj.get("volume").getAsInt() / 100);

    public void getVolumeStatus(VolumeStatusListener listener) {
        ServiceCommand<VolumeStatus, VolumeStatusListener> request = new ServiceCommand<>(this, VOLUME_STATUS, null,
                true, VOLUME_STATUS_CONVERTER, listener);
        request.send();
    }

    public ServiceSubscription<VolumeStatusListener> subscribeVolumeStatus(VolumeStatusListener listener) {
        URLServiceSubscription<VolumeStatus, VolumeStatusListener> request = new URLServiceSubscription<>(this,
                VOLUME_STATUS, null, true, VOLUME_STATUS_CONVERTER, listener);
        request.send();
        return request;
    }

    /******************
     * MEDIA PLAYER
     *****************/
    @Override
    public MediaPlayer getMediaPlayer() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH; // TODO: remove
    }

    @Override
    public void getMediaInfo(MediaInfoListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported(new Object() {
        }.getClass().getEnclosingMethod().getName()));
    }

    @Override
    public ServiceSubscription<MediaInfoListener> subscribeMediaInfo(MediaInfoListener listener) {
        listener.onError(ServiceCommandError.notSupported(new Object() {
        }.getClass().getEnclosingMethod().getName()));
        return null;
    }

    private void displayMedia(JsonObject params, final MediaPlayer.LaunchListener listener) {
        String uri = "ssap://media.viewer/open";

        ResponseListener<JsonObject> responseListener = new ResponseListener<JsonObject>() {
            @Override
            public void onSuccess(JsonObject obj) {

                LaunchSession launchSession = LaunchSession.launchSessionForAppId(obj.get("id").getAsString());
                launchSession.setService(WebOSTVService.this);
                launchSession.setSessionId(obj.get("sessionId").getAsString());
                launchSession.setSessionType(LaunchSessionType.Media);

                Util.postSuccess(listener, new MediaLaunchObject(launchSession, WebOSTVService.this));
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        };

        ServiceCommand<JsonObject, ResponseListener<JsonObject>> request = new ServiceCommand<>(this, uri, params, true,
                x -> x, responseListener);
        request.send();
    }

    private void displayImage(final String url, final String mimeType, final String title, final String description,
            final String iconSrc, final MediaPlayer.LaunchListener listener) {
        if ("4.0.0".equalsIgnoreCase(this.serviceDescription.getVersion())) {
            JsonObject params = new JsonObject();
            params.addProperty("target", url);
            params.addProperty("title", title);
            params.addProperty("description", description);
            params.addProperty("mimeType", mimeType);
            params.addProperty("iconSrc", iconSrc);
            this.displayMedia(params, listener);
        } else {

            final MediaInfo mediaInfo = new MediaInfo.Builder(url, mimeType).setTitle(title).setDescription(description)
                    .setIcon(iconSrc).build();

            final WebAppSession.LaunchListener webAppLaunchListener = new WebAppSession.LaunchListener() {

                @Override
                public void onError(ServiceCommandError error) {
                    listener.onError(error);
                }

                @Override
                public void onSuccess(WebAppSession webAppSession) {
                    webAppSession.displayImage(mediaInfo, listener);
                }
            };

            this.getWebAppLauncher().joinWebApp(MEDIA_PLAYER_ID, new WebAppSession.LaunchListener() {

                @Override
                public void onError(ServiceCommandError error) {
                    getWebAppLauncher().launchWebApp(MEDIA_PLAYER_ID, webAppLaunchListener);
                }

                @Override
                public void onSuccess(WebAppSession webAppSession) {
                    webAppSession.displayImage(mediaInfo, listener);
                }
            });
        }
    }

    @Override
    public void displayImage(MediaInfo mediaInfo, MediaPlayer.LaunchListener listener) {
        String mediaUrl = null;
        String mimeType = null;
        String title = null;
        String desc = null;
        String iconSrc = null;

        if (mediaInfo != null) {
            mediaUrl = mediaInfo.getUrl();
            mimeType = mediaInfo.getMimeType();
            title = mediaInfo.getTitle();
            desc = mediaInfo.getDescription();

            if (mediaInfo.getImages() != null && mediaInfo.getImages().size() > 0) {
                ImageInfo imageInfo = mediaInfo.getImages().get(0);
                iconSrc = imageInfo.getUrl();
            }
        }

        displayImage(mediaUrl, mimeType, title, desc, iconSrc, listener);
    }

    @Override
    public void playMedia(MediaInfo mediaInfo, boolean shouldLoop, MediaPlayer.LaunchListener listener) {
        if ("4.0.0".equalsIgnoreCase(this.serviceDescription.getVersion())) {
            playMediaByNativeApp(mediaInfo, shouldLoop, listener);
        } else {
            playMediaByWebApp(mediaInfo, shouldLoop, listener);
        }
    }

    private void playMediaByWebApp(final MediaInfo mediaInfo, final boolean shouldLoop, final LaunchListener listener) {
        final WebAppSession.LaunchListener webAppLaunchListener = new WebAppSession.LaunchListener() {

            @Override
            public void onError(ServiceCommandError error) {
                listener.onError(error);
            }

            @Override
            public void onSuccess(WebAppSession webAppSession) {
                webAppSession.playMedia(mediaInfo, shouldLoop, listener);
            }
        };

        getWebAppLauncher().joinWebApp(MEDIA_PLAYER_ID, new WebAppSession.LaunchListener() {

            @Override
            public void onError(ServiceCommandError error) {
                getWebAppLauncher().launchWebApp(MEDIA_PLAYER_ID, webAppLaunchListener);
            }

            @Override
            public void onSuccess(WebAppSession webAppSession) {
                webAppSession.playMedia(mediaInfo, shouldLoop, listener);
            }
        });
    }

    private void playMediaByNativeApp(MediaInfo mediaInfo, boolean shouldLoop, LaunchListener listener) {
        DeviceService dlnaService = this.getDLNAService();

        if (dlnaService != null) {
            MediaPlayer mediaPlayer = dlnaService.getAPI(MediaPlayer.class);

            if (mediaPlayer != null) {
                mediaPlayer.playMedia(mediaInfo, shouldLoop, listener);
                return;
            }
        }

        String iconSrc = null;
        List<ImageInfo> images = mediaInfo.getImages();

        if (images != null && !images.isEmpty()) {
            ImageInfo iconImage = images.get(0);
            if (iconImage != null) {
                iconSrc = iconImage.getUrl();
            }
        }
        final MediaInfo mediaInfo1 = mediaInfo;
        final boolean shouldLoop1 = shouldLoop;
        final String iconSrc1 = iconSrc;
        JsonObject params = new JsonObject();
        params.addProperty("target", mediaInfo1.getUrl());
        params.addProperty("title", mediaInfo1.getTitle());
        params.addProperty("description", mediaInfo1.getDescription());
        params.addProperty("mimeType", mediaInfo1.getMimeType());
        params.addProperty("iconSrc", iconSrc1);
        params.addProperty("loop", shouldLoop1);

        displayMedia(params, listener);

    }

    @Override
    public void closeMedia(LaunchSession launchSession, ResponseListener<Object> listener) {
        JsonObject payload = new JsonObject();

        if (launchSession.getAppId() != null && launchSession.getAppId().length() > 0) {
            payload.addProperty("id", launchSession.getAppId());
        }

        if (launchSession.getSessionId() != null && launchSession.getSessionId().length() > 0) {
            payload.addProperty("sessionId", launchSession.getSessionId());
        }

        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(launchSession.getService(),
                CLOSE_MEDIA_URI, payload, true, x -> x, listener);
        request.send();
    }

    /******************
     * MEDIA CONTROL
     *****************/
    @Override
    public MediaControl getMediaControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getMediaControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH; // TODO: Remove this
    }

    @Override
    public void play(ResponseListener<Object> listener) {
        String uri = "ssap://media.controls/play";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, null, true, x -> x,
                listener);
        request.send();
    }

    @Override
    public void pause(ResponseListener<Object> listener) {
        String uri = "ssap://media.controls/pause";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, null, true, x -> x,
                listener);
        request.send();
    }

    @Override
    public void stop(ResponseListener<Object> listener) {
        String uri = "ssap://media.controls/stop";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, null, true, x -> x,
                listener);
        request.send();
    }

    @Override
    public void rewind(ResponseListener<Object> listener) {
        String uri = "ssap://media.controls/rewind";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, null, true, x -> x,
                listener);
        request.send();
    }

    @Override
    public void fastForward(ResponseListener<Object> listener) {
        String uri = "ssap://media.controls/fastForward";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, null, true, x -> x,
                listener);
        request.send();
    }

    @Override
    public void previous(ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported(new Object() {
        }.getClass().getEnclosingMethod().getName()));
    }

    @Override
    public void next(ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported(new Object() {
        }.getClass().getEnclosingMethod().getName()));
    }

    @Override
    public void seek(long position, ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported(new Object() {
        }.getClass().getEnclosingMethod().getName()));
    }

    @Override
    public void getDuration(DurationListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported(new Object() {
        }.getClass().getEnclosingMethod().getName()));
    }

    @Override
    public void getPosition(PositionListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported(new Object() {
        }.getClass().getEnclosingMethod().getName()));
    }

    /******************
     * TV CONTROL
     *****************/
    @Override
    public TVControl getTVControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getTVControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH; // TODO: Remove
    }

    @Override
    public void channelUp(ResponseListener<Object> listener) {
        String uri = "ssap://tv/channelUp";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, null, true, x -> x,
                listener);
        request.send();
    }

    @Override
    public void channelDown(ResponseListener<Object> listener) {
        String uri = "ssap://tv/channelDown";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, null, true, x -> x,
                listener);
        request.send();
    }

    @Override
    public void setChannel(ChannelInfo channelInfo, ResponseListener<Object> listener) {
        JsonObject payload = new JsonObject();
        if (channelInfo.getId() != null) {
            payload.addProperty("channelId", channelInfo.getId());
        }
        if (channelInfo.getNumber() != null) {
            payload.addProperty("channelNumber", channelInfo.getNumber());
        }
        setChannel(payload, listener);
    }

    public void setChannelById(String channelId, ResponseListener<Object> listener) {
        JsonObject payload = new JsonObject();
        payload.addProperty("channelId", channelId);
        setChannel(payload, listener);
    }

    private void setChannel(JsonObject payload, ResponseListener<Object> listener) {
        String uri = "ssap://tv/openChannel";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, payload, true,
                x -> x, listener);
        request.send();
    }

    @Override
    public void getCurrentChannel(ChannelListener listener) {
        ServiceCommand<ChannelInfo, ChannelListener> request = new ServiceCommand<>(this, CHANNEL, null, true,
                jsonObj -> parseRawChannelData(jsonObj), listener);
        request.send();
    }

    @Override

    public ServiceSubscription<ChannelListener> subscribeCurrentChannel(ChannelListener listener) {
        URLServiceSubscription<ChannelInfo, ChannelListener> request = new URLServiceSubscription<>(this, CHANNEL, null,
                true, jsonObj -> parseRawChannelData(jsonObj), listener);
        request.send();
        return request;
    }

    private final Function<JsonObject, List<ChannelInfo>> CHANNEL_LIST_CONVERTER = jsonObj -> {
        List<ChannelInfo> list = new ArrayList<>();
        JsonArray array = jsonObj.get("channelList").getAsJsonArray();
        array.forEach(element -> list.add(parseRawChannelData(element.getAsJsonObject())));
        return list;
    };

    @Override
    public void getChannelList(ChannelListListener listener) {
        ServiceCommand<List<ChannelInfo>, ChannelListListener> request = new ServiceCommand<>(this, CHANNEL_LIST, null,
                true, CHANNEL_LIST_CONVERTER, listener);
        request.send();
    }

    // TODO: if this works it means channel list can be cached until this triggers
    public ServiceSubscription<ChannelListListener> subscribeChannelList(final ChannelListListener listener) {
        URLServiceSubscription<List<ChannelInfo>, ChannelListListener> request = new URLServiceSubscription<>(this,
                CHANNEL_LIST, null, true, CHANNEL_LIST_CONVERTER, listener);
        request.send();
        return request;
    }

    public void getChannelCurrentProgramInfo(ProgramInfoListener listener) {
        ServiceCommand<ProgramInfo, ProgramInfoListener> request = new ServiceCommand<>(this, CURRENT_PROGRAM, null,
                true, jsonObj -> parseRawProgramInfo(jsonObj), listener);
        request.send();
    }

    public ServiceSubscription<ProgramInfoListener> subscribeChannelCurrentProgramInfo(ProgramInfoListener listener) {
        URLServiceSubscription<ProgramInfo, ProgramInfoListener> request = new URLServiceSubscription<>(this,
                CURRENT_PROGRAM, null, true, jsonObj -> parseRawProgramInfo(jsonObj), listener);
        request.send();
        return request;
    }

    private final Function<JsonObject, ProgramList> PROGRAM_LIST_CONVERTER = jsonObj -> {
        JsonObject jsonChannel = jsonObj.get("channel").getAsJsonObject();
        ChannelInfo channel = parseRawChannelData(jsonChannel);
        JsonArray programList = jsonObj.get("programList").getAsJsonArray();
        return new ProgramList(channel, programList);
    };

    @Override
    public void getProgramInfo(ProgramInfoListener listener) {
        // TODO need to parse current program when program id is correct
        Util.postError(listener, ServiceCommandError.notSupported(new Object() {
        }.getClass().getEnclosingMethod().getName()));
    }

    @Override
    public void getProgramList(ProgramListListener listener) {
        ServiceCommand<ProgramList, ProgramListListener> request = new ServiceCommand<>(this, PROGRAM, null, true,
                PROGRAM_LIST_CONVERTER, listener);
        request.send();
    }

    @Override

    public ServiceSubscription<ProgramListListener> subscribeProgramList(ProgramListListener listener) {
        URLServiceSubscription<ProgramList, ProgramListListener> request = new URLServiceSubscription<>(this, PROGRAM,
                null, true, PROGRAM_LIST_CONVERTER, listener);
        request.send();
        return request;
    }

    @Override
    public void set3DEnabled(final boolean enabled, final ResponseListener<Object> listener) {
        String uri;
        if (enabled) {
            uri = "ssap://com.webos.service.tv.display/set3DOn";
        } else {
            uri = "ssap://com.webos.service.tv.display/set3DOff";
        }

        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, null, true, x -> x,
                listener);

        request.send();
    }

    private static final Function<JsonObject, Boolean> THREED_STATUS_CONVERTER = jsonObj -> jsonObj.get("status3D")
            .getAsJsonObject().get("status").getAsBoolean();

    @Override
    public void get3DEnabled(final State3DModeListener listener) {
        ServiceCommand<Boolean, State3DModeListener> request = new ServiceCommand<>(this, THREED_STATUS, null, true,
                THREED_STATUS_CONVERTER, listener);
        request.send();
    }

    @Override

    public ServiceSubscription<State3DModeListener> subscribe3DEnabled(final State3DModeListener listener) {
        URLServiceSubscription<Boolean, State3DModeListener> request = new URLServiceSubscription<>(this, THREED_STATUS,
                null, true, THREED_STATUS_CONVERTER, listener);
        request.send();
        return request;
    }

    /**************
     * EXTERNAL INPUT
     **************/
    @Override
    public ExternalInputControl getExternalInput() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getExternalInputControlPriorityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void launchInputPicker(final ResponseListener<LaunchSession> listener) {
        final AppInfo appInfo = new AppInfo() {
            {
                setId("com.webos.app.inputpicker");
                setName("InputPicker");
            }
        };

        launchAppWithInfo(appInfo, null, new ResponseListener<LaunchSession>() {
            @Override
            public void onSuccess(LaunchSession object) {
                listener.onSuccess(object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                appInfo.setId("com.webos.app.inputmgr");
                launchAppWithInfo(appInfo, null, listener);
            }
        });
    }

    @Override
    public void closeInputPicker(LaunchSession launchSession, ResponseListener<Object> listener) {
        closeApp(launchSession, listener);
    }

    @Override
    public void getExternalInputList(final ExternalInputListListener listener) {
        String uri = "ssap://tv/getExternalInputList";
        ServiceCommand<List<ExternalInputInfo>, ExternalInputListListener> request = new ServiceCommand<>(this, uri,
                null, true, jsonObj -> externalnputInfoFromJsonArray((JsonArray) jsonObj.get("devices")), listener);
        request.send();
    }

    @Override
    public void setExternalInput(ExternalInputInfo externalInputInfo, final ResponseListener<Object> listener) {
        String uri = "ssap://tv/switchInput";

        JsonObject payload = new JsonObject();

        if (externalInputInfo != null && externalInputInfo.getId() != null) {
            payload.addProperty("inputId", externalInputInfo.getId());
        } else {
            Log.w(Util.T, "ExternalInputInfo has no id");
        }

        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, payload, true,
                x -> x, listener);
        request.send();
    }

    /**************
     * MOUSE CONTROL
     **************/
    @Override
    public MouseControl getMouseControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getMouseControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void connectMouse() {
        connectMouse();
    }

    @Override
    public void disconnectMouse() {
        if (mouseSocket == null) {
            return;
        }

        mouseSocket.disconnect();
        mouseSocket = null;
    }

    private void connectMouse(Runnable onConnected) {
        if (mouseSocket != null) {
            return;
        }

        String uri = "ssap://com.webos.service.networkinput/getPointerInputSocket";

        ResponseListener<JsonObject> listener = new ResponseListener<JsonObject>() {

            @Override
            public void onSuccess(JsonObject jsonObj) {
                String socketPath = jsonObj.get("socketPath").getAsString().replace("wss:", "ws:").replace(":3001/",
                        ":3000/");
                try {
                    mouseSocket.connect(new URI(socketPath), onConnected);
                } catch (URISyntaxException e) {
                    Log.w(Util.T, "Connect mouse error: " + e.getMessage());
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                Log.w(Util.T, "Connect mouse error: " + error.getMessage());
            }
        };

        ServiceCommand<JsonObject, ResponseListener<JsonObject>> request = new ServiceCommand<>(this, uri, null, true,
                x -> x, listener);
        request.send();
    }

    @Override
    public void click() {
        if (mouseSocket != null) {
            mouseSocket.click();
        } else {
            connectMouse(() -> mouseSocket.click());
        }
    }

    @Override
    public void move(final double dx, final double dy) {
        if (mouseSocket != null) {
            mouseSocket.move(dx, dy);
        } else {
            connectMouse(() -> mouseSocket.move(dx, dy));
        }
    }

    @Override
    public void move(PointF diff) {
        move(diff.x, diff.y);
    }

    @Override
    public void scroll(final double dx, final double dy) {
        if (mouseSocket != null) {
            mouseSocket.scroll(dx, dy);
        } else {
            connectMouse(() -> mouseSocket.scroll(dx, dy));
        }
    }

    @Override
    public void scroll(PointF diff) {
        scroll(diff.x, diff.y);
    }

    /**************
     * KEYBOARD CONTROL
     **************/
    @Override
    public TextInputControl getTextInputControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getTextInputControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public ServiceSubscription<TextInputStatusListener> subscribeTextInputStatus(TextInputStatusListener listener) {
        keyboardInput = new WebOSTVKeyboardInput(this);
        return keyboardInput.connect(listener);
    }

    @Override
    public void sendText(String input) {
        if (keyboardInput != null) {
            keyboardInput.addToQueue(input);
        }
    }

    @Override
    public void sendKeyCode(KeyCode keycode, ResponseListener<Object> listener) {
        switch (keycode) {
            case NUM_0:
            case NUM_1:
            case NUM_2:
            case NUM_3:
            case NUM_4:
            case NUM_5:
            case NUM_6:
            case NUM_7:
            case NUM_8:
            case NUM_9:
                sendSpecialKey(String.valueOf(keycode.getCode()), listener);
                break;
            case DASH:
                sendSpecialKey("DASH", listener);
                break;
            case ENTER:
                sendSpecialKey("ENTER", listener);
            default:
                Util.postError(listener, new ServiceCommandError("The keycode is not available"));
        }
    }

    @Override
    public void sendEnter() {
        if (keyboardInput != null) {
            keyboardInput.sendEnter();
        }
    }

    @Override
    public void sendDelete() {
        if (keyboardInput != null) {
            keyboardInput.sendDel();
        }
    }

    /**************
     * POWER CONTROL
     **************/
    @Override
    public PowerControl getPowerControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getPowerControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void powerOff(ResponseListener<Object> listener) {
        String uri = "ssap://system/turnOff";
        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, null, true, x -> x,
                listener);
        request.send();
    }

    @Override
    public void powerOn(ResponseListener<Object> listener) {
        // TODO: implement wake on lan here
        Util.postError(listener, ServiceCommandError.notSupported(new Object() {
        }.getClass().getEnclosingMethod().getName()));
    }

    /**************
     * KEY CONTROL
     **************/
    @Override
    public KeyControl getKeyControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getKeyControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH; // TODO: remove
    }

    private void sendSpecialKey(final String key, final ResponseListener<Object> listener) {
        if (mouseSocket != null) {
            mouseSocket.button(key);
            listener.onSuccess(null);
        } else {
            connectMouse(() -> {
                mouseSocket.button(key);
                listener.onSuccess(null);
            });
        }
    }

    @Override
    public void up(ResponseListener<Object> listener) {
        sendSpecialKey("UP", listener);
    }

    @Override
    public void down(ResponseListener<Object> listener) {
        sendSpecialKey("DOWN", listener);
    }

    @Override
    public void left(ResponseListener<Object> listener) {
        sendSpecialKey("LEFT", listener);
    }

    @Override
    public void right(ResponseListener<Object> listener) {
        sendSpecialKey("RIGHT", listener);
    }

    @Override
    public void ok(final ResponseListener<Object> listener) {
        if (mouseSocket != null) {
            mouseSocket.click();
            Util.postSuccess(listener, null);
        } else {
            connectMouse(() -> {
                mouseSocket.click();
                Util.postSuccess(listener, null);
            });
        }
    }

    @Override
    public void back(ResponseListener<Object> listener) {
        sendSpecialKey("BACK", listener);
    }

    @Override
    public void home(ResponseListener<Object> listener) {
        sendSpecialKey("HOME", listener);
    }

    /**************
     * Web App Launcher
     **************/

    @Override
    public WebAppLauncher getWebAppLauncher() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getWebAppLauncherCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void launchWebApp(final String webAppId, final WebAppSession.LaunchListener listener) {
        this.launchWebApp(webAppId, null, true, listener);
    }

    @Override
    public void launchWebApp(String webAppId, boolean relaunchIfRunning, WebAppSession.LaunchListener listener) {
        launchWebApp(webAppId, null, relaunchIfRunning, listener);
    }

    @Override
    public void launchWebApp(final String webAppId, final JsonObject params,
            final WebAppSession.LaunchListener listener) {
        if (webAppId == null || webAppId.length() == 0) {
            Util.postError(listener, new ServiceCommandError("You need to provide a valid webAppId."));
            return;
        }

        String uri = "ssap://webapp/launchWebApp";
        JsonObject payload = new JsonObject();
        payload.addProperty("webAppId", webAppId);

        if (params != null) {
            payload.add("urlParams", params);
        }

        ServiceCommand<WebAppSession, WebAppSession.LaunchListener> request = new ServiceCommand<>(this, uri, payload,
                true, x -> createWebAppSession(x, webAppId), listener);
        request.send();
    }

    private WebAppSession createWebAppSession(final JsonObject obj, final String webAppId) {
        final WebOSWebAppSession _webAppSession = mWebAppSessions.get(webAppId);
        LaunchSession launchSession = null;
        WebOSWebAppSession webAppSession = _webAppSession;

        if (webAppSession != null) {
            launchSession = webAppSession.launchSession;
        } else {
            launchSession = LaunchSession.launchSessionForAppId(webAppId);
            webAppSession = new WebOSWebAppSession(launchSession, WebOSTVService.this);
            mWebAppSessions.put(webAppId, webAppSession);
        }

        launchSession.setService(WebOSTVService.this);
        launchSession.setSessionId(obj.get("sessionId").getAsString());
        launchSession.setSessionType(LaunchSessionType.WebApp);
        launchSession.setRawData(obj);
        return webAppSession;
    };

    @Override
    public void launchWebApp(final String webAppId, final JsonObject params, boolean relaunchIfRunning,
            final WebAppSession.LaunchListener listener) {
        if (webAppId == null) {
            Util.postError(listener, new ServiceCommandError("Must pass a web App id"));
            return;
        }

        if (relaunchIfRunning) {
            launchWebApp(webAppId, params, listener);
        } else {
            getLauncher().getRunningApp(new AppInfoListener() {

                @Override
                public void onError(ServiceCommandError error) {
                    listener.onError(error);
                }

                @Override
                public void onSuccess(AppInfo appInfo) {
                    // TODO: this will only work on pinned apps, currently
                    if (appInfo.getId().indexOf(webAppId) != -1) {
                        LaunchSession launchSession = LaunchSession.launchSessionForAppId(webAppId);
                        launchSession.setSessionType(LaunchSessionType.WebApp);
                        launchSession.setService(WebOSTVService.this);
                        launchSession.setRawData(appInfo.getRawData());

                        WebOSWebAppSession webAppSession = webAppSessionForLaunchSession(launchSession);

                        listener.onSuccess(webAppSession);
                    } else {
                        launchWebApp(webAppId, params, listener);
                    }
                }
            });
        }
    }

    @Override
    public void closeWebApp(LaunchSession launchSession, final ResponseListener<Object> listener) {
        if (launchSession == null || launchSession.getAppId() == null || launchSession.getAppId().length() == 0) {
            Util.postError(listener, new ServiceCommandError("Must provide a valid launch session"));
            return;
        }

        final WebOSWebAppSession webAppSession = mWebAppSessions.get(launchSession.getAppId());
        if (webAppSession != null) {
            webAppSession.disconnectFromWebApp();
        }

        String uri = "ssap://webapp/closeWebApp";
        JsonObject payload = new JsonObject();

        if (launchSession.getAppId() != null) {
            payload.addProperty("webAppId", launchSession.getAppId());
        }
        if (launchSession.getSessionId() != null) {
            payload.addProperty("sessionId", launchSession.getSessionId());
        }

        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, uri, payload, true,
                x -> x, listener);
        request.send();
    }

    public void connectToWebApp(final WebOSWebAppSession webAppSession, final boolean joinOnly,
            final ResponseListener<Object> connectionListener) {
        if (mWebAppSessions == null) {
            mWebAppSessions = new ConcurrentHashMap<String, WebOSWebAppSession>();
        }

        if (mAppToAppIdMappings == null) {
            mAppToAppIdMappings = new ConcurrentHashMap<String, String>();
        }

        if (webAppSession == null || webAppSession.launchSession == null) {
            connectionListener.onError(new ServiceCommandError("You must provide a valid LaunchSession object"));
            return;
        }

        String _appId = webAppSession.launchSession.getAppId();
        String _idKey = null;

        if (webAppSession.launchSession.getSessionType() == LaunchSession.LaunchSessionType.WebApp) {
            _idKey = "webAppId";
        } else {
            _idKey = "appId";
        }

        if (_appId == null || _appId.length() == 0) {
            connectionListener.onError(new ServiceCommandError("You must provide a valid web app session"));

            return;
        }

        final String appId = _appId;
        final String idKey = _idKey;

        String uri = "ssap://webapp/connectToApp";
        JsonObject payload = new JsonObject();
        payload.addProperty(idKey, appId);

        ResponseListener<JsonObject> responseListener = new ResponseListener<JsonObject>() {

            @Override
            public void onSuccess(final JsonObject jsonObj) {

                if (jsonObj.has("state")) {
                    String state = jsonObj.get("state").getAsString();
                    if (!"CONNECTED".equalsIgnoreCase(state)) {
                        if (joinOnly && state.equalsIgnoreCase("WAITING_FOR_APP")) {
                            Util.postError(connectionListener,
                                    new ServiceCommandError("Web app is not currently running"));
                        }
                        return;
                    }
                }

                if (jsonObj.has("appId")) {
                    String fullAppId = jsonObj.get("appId").getAsString();

                    if (fullAppId.length() != 0) {
                        if (webAppSession.launchSession.getSessionType() == LaunchSessionType.WebApp) {
                            mAppToAppIdMappings.put(fullAppId, appId);
                        }
                        webAppSession.setFullAppId(fullAppId);
                    }
                }

                if (connectionListener != null) {
                    Util.run(new Runnable() {

                        @Override
                        public void run() {
                            connectionListener.onSuccess(jsonObj);
                        }
                    });
                }
            }

            @NonNullByDefault
            @Override
            public void onError(ServiceCommandError error) {
                webAppSession.disconnectFromWebApp();
                boolean appChannelDidClose = false;
                appChannelDidClose = error.getMessage().contains("app channel closed");
                if (appChannelDidClose) {
                    if (webAppSession.getWebAppSessionListener() != null) {
                        Util.run(() -> webAppSession.getWebAppSessionListener()
                                .onWebAppSessionDisconnect(webAppSession));
                    }
                } else {
                    connectionListener.onError(error);
                }
            }
        };

        webAppSession.appToAppSubscription = new URLServiceSubscription<>(this, uri, payload, true, x -> x,
                responseListener);
        webAppSession.appToAppSubscription.subscribe();
    }

    @Override
    public void pinWebApp(String webAppId, final ResponseListener<Object> listener) {
        if (webAppId == null || webAppId.length() == 0) {
            if (listener != null) {
                listener.onError(new ServiceCommandError("You must provide a valid web app id"));
            }
            return;
        }

        String uri = "ssap://webapp/pinWebApp";
        JsonObject payload = new JsonObject();

        payload.addProperty("webAppId", webAppId);

        ServiceCommand<Object, ResponseListener<Object>> request = new URLServiceSubscription<>(this, uri, payload,
                true, x -> x, listener);
        request.send();
    }

    @Override
    public void unPinWebApp(String webAppId, final ResponseListener<Object> listener) {
        if (webAppId == null || webAppId.length() == 0) {
            if (listener != null) {
                listener.onError(new ServiceCommandError("You must provide a valid web app id"));
            }
            return;
        }

        String uri = "ssap://webapp/removePinnedWebApp";
        JsonObject payload = new JsonObject();

        payload.addProperty("webAppId", webAppId);

        ServiceCommand<Object, ResponseListener<Object>> request = new URLServiceSubscription<>(this, uri, payload,
                true, x -> x, listener);
        request.send();
    }

    @Override
    public void isWebAppPinned(String webAppId, WebAppPinStatusListener listener) {
        if (webAppId == null || webAppId.length() == 0) {
            if (listener != null) {
                listener.onError(new ServiceCommandError("You must provide a valid web app id"));
            }
        }

        String uri = "ssap://webapp/isWebAppPinned";
        JsonObject payload = new JsonObject();
        payload.addProperty("webAppId", webAppId);

        ServiceCommand<Boolean, WebAppPinStatusListener> request = new ServiceCommand<>(this, uri, payload, true,
                obj -> obj.get("pinned").getAsBoolean(), listener);

        request.send();
    }

    @Override
    public ServiceSubscription<WebAppPinStatusListener> subscribeIsWebAppPinned(String webAppId,
            WebAppPinStatusListener listener) {
        if (webAppId == null || webAppId.length() == 0) {
            if (listener != null) {
                listener.onError(new ServiceCommandError("You must provide a valid web app id"));
            }
            return null;
        }

        String uri = "ssap://webapp/isWebAppPinned";
        JsonObject payload = new JsonObject();
        payload.addProperty("webAppId", webAppId);

        URLServiceSubscription<Boolean, WebAppPinStatusListener> request;
        request = new URLServiceSubscription<>(this, uri, payload, true, obj -> obj.get("pinned").getAsBoolean(),
                listener);

        request.send();
        return request;
    }

    /* Join a native/installed webOS app */
    public void joinApp(String appId, WebAppSession.LaunchListener listener) {
        LaunchSession launchSession = LaunchSession.launchSessionForAppId(appId);
        launchSession.setSessionType(LaunchSessionType.App);
        launchSession.setService(this);
        joinWebApp(launchSession, listener);
    }

    /* Connect to a native/installed webOS app */
    public void connectToApp(String appId, final WebAppSession.LaunchListener listener) {
        LaunchSession launchSession = LaunchSession.launchSessionForAppId(appId);
        launchSession.setSessionType(LaunchSessionType.App);
        launchSession.setService(this);

        final WebOSWebAppSession webAppSession = webAppSessionForLaunchSession(launchSession);

        connectToWebApp(webAppSession, false, new ResponseListener<Object>() {
            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }

            @Override
            public void onSuccess(Object object) {
                Util.postSuccess(listener, webAppSession);
            }
        });
    }

    @Override
    public void joinWebApp(final LaunchSession webAppLaunchSession, final WebAppSession.LaunchListener listener) {
        final WebOSWebAppSession webAppSession = this.webAppSessionForLaunchSession(webAppLaunchSession);

        webAppSession.join(new ResponseListener<Object>() {

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }

            @Override
            public void onSuccess(Object object) {
                Util.postSuccess(listener, webAppSession);
            }
        });
    }

    @Override
    public void joinWebApp(String webAppId, WebAppSession.LaunchListener listener) {
        LaunchSession launchSession = LaunchSession.launchSessionForAppId(webAppId);
        launchSession.setSessionType(LaunchSessionType.WebApp);
        launchSession.setService(this);

        joinWebApp(launchSession, listener);
    }

    private WebOSWebAppSession webAppSessionForLaunchSession(LaunchSession launchSession) {
        if (mWebAppSessions == null) {
            mWebAppSessions = new ConcurrentHashMap<String, WebOSWebAppSession>();
        }

        if (launchSession.getService() == null) {
            launchSession.setService(this);
        }

        WebOSWebAppSession webAppSession = mWebAppSessions.get(launchSession.getAppId());

        if (webAppSession == null) {
            webAppSession = new WebOSWebAppSession(launchSession, this);
            mWebAppSessions.put(launchSession.getAppId(), webAppSession);
        }

        return webAppSession;
    }

    @SuppressWarnings("unused")
    private void sendMessage(JsonElement message, LaunchSession launchSession, ResponseListener<Object> listener) {
        if (launchSession == null || launchSession.getAppId() == null) {
            Util.postError(listener, new ServiceCommandError("Must provide a valid LaunchSession object"));
            return;
        }

        if (message == null) {
            Util.postError(listener, new ServiceCommandError("Cannot send a null message"));
            return;
        }

        if (socket == null) {
            connect();
        }

        String appId = launchSession.getAppId();
        String fullAppId = appId;

        if (launchSession.getSessionType() == LaunchSessionType.WebApp) {
            fullAppId = mAppToAppIdMappings.get(appId);
        }

        if (fullAppId == null || fullAppId.length() == 0) {
            Util.postError(listener,
                    new ServiceCommandError("You must provide a valid LaunchSession to send messages to"));

            return;
        }

        JsonObject payload = new JsonObject();

        payload.addProperty("type", "p2p");
        payload.addProperty("to", fullAppId);
        payload.add("payload", message);

        ServiceCommand<Object, ResponseListener<Object>> request = new ServiceCommand<>(this, null, payload, true,
                x -> x, listener);
        sendCommand(request);
    }

    public void sendMessage(String message, LaunchSession launchSession, ResponseListener<Object> listener) {
        if (message != null && message.length() > 0) {
            sendMessage(new JsonPrimitive(message), launchSession, listener);
        } else {
            Util.postError(listener, new ServiceCommandError("Cannot send a null message"));
        }
    }

    public void sendMessage(JsonObject message, LaunchSession launchSession, ResponseListener<Object> listener) {
        if (message != null && message.size() > 0) {
            sendMessage((JsonElement) message, launchSession, listener);
        } else {
            Util.postError(listener, new ServiceCommandError("Cannot send a null message"));
        }
    }

    /*
     * *************
     * SYSTEM CONTROL
     */
    public void getServiceInfo(final ServiceInfoListener listener) {
        String uri = "ssap://api/getServiceList";
        ServiceCommand<JsonArray, ServiceInfoListener> request = new ServiceCommand<>(this, uri, null, true,
                jsonObj -> jsonObj.get("services").getAsJsonArray(), listener);
        request.send();
    }

    public void getSystemInfo(final SystemInfoListener listener) {
        String uri = "ssap://system/getSystemInfo";
        ServiceCommand<JsonObject, SystemInfoListener> request = new ServiceCommand<>(this, uri, null, true,
                jsonObj -> jsonObj.get("features").getAsJsonObject(), listener);
        request.send();
    }

    public void secureAccessTest(final SecureAccessTestListener listener) {
        String uri = "ssap://com.webos.service.secondscreen.gateway/test/secure";
        ServiceCommand<Boolean, SecureAccessTestListener> request = new ServiceCommand<>(this, uri, null, true,
                jsonObj -> jsonObj.get("returnValue").getAsBoolean(), listener);
        request.send();
    }

    public void getACRAuthToken(final ACRAuthTokenListener listener) {
        String uri = "ssap://tv/getACRAuthToken";
        ServiceCommand<String, ACRAuthTokenListener> request = new ServiceCommand<>(this, uri, null, true,
                jsonObj -> jsonObj.get("token").getAsString(), listener);
        request.send();
    }

    public void getLaunchPoints(final LaunchPointsListener listener) {
        String uri = "ssap://com.webos.applicationManager/listLaunchPoints";
        ServiceCommand<JsonArray, LaunchPointsListener> request = new ServiceCommand<>(this, uri, null, true,
                jsonObj -> jsonObj.get("launchPoints").getAsJsonArray(), listener);
        request.send();
    }

    /******************
     * PLAYLIST CONTROL
     *****************/
    @Override
    public PlaylistControl getPlaylistControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getPlaylistControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void jumpToTrack(long index, ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported(new Object() {
        }.getClass().getEnclosingMethod().getName()));
    }

    @Override
    public void setPlayMode(PlayMode playMode, ResponseListener<Object> listener) {
        Util.postError(listener, ServiceCommandError.notSupported(new Object() {
        }.getClass().getEnclosingMethod().getName()));
    }

    @Override
    public void sendCommand(ServiceCommand<?, ?> command) {
        if (socket != null) {
            socket.sendCommand(command);
        }
    }

    @Override
    public void unsubscribe(URLServiceSubscription<?, ?> subscription) {
        if (socket != null) {
            socket.unsubscribe(subscription);
        }
    }

    @Override
    protected void updateCapabilities() {
        List<String> capabilities = new ArrayList<String>();

        capabilities.addAll(VolumeControl.Capabilities);
        capabilities.addAll(MediaPlayer.Capabilities);

        if (DiscoveryManager.getInstance().getPairingLevel() == PairingLevel.ON) {
            capabilities.addAll(TextInputControl.Capabilities);
            capabilities.addAll(MouseControl.Capabilities);
            capabilities.addAll(KeyControl.Capabilities);
            capabilities.addAll(MediaPlayer.Capabilities);
            capabilities.addAll(Launcher.Capabilities);
            capabilities.addAll(TVControl.Capabilities);
            capabilities.addAll(ExternalInputControl.Capabilities);
            capabilities.addAll(ToastControl.Capabilities);
            capabilities.add(PowerControl.Off);
        } else {
            capabilities.add(Application);
            capabilities.add(Application_Params);
            capabilities.add(Application_Close);
            capabilities.add(Browser);
            capabilities.add(Browser_Params);
            capabilities.add(Hulu);
            capabilities.add(Netflix);
            capabilities.add(Netflix_Params);
            capabilities.add(YouTube);
            capabilities.add(YouTube_Params);
            capabilities.add(AppStore);
            capabilities.add(AppStore_Params);
            capabilities.add(AppState);
            capabilities.add(AppState_Subscribe);
        }

        if (serviceDescription != null) {
            if (serviceDescription.getVersion() != null && (serviceDescription.getVersion().contains("4.0.0")
                    || serviceDescription.getVersion().contains("4.0.1"))) {
                capabilities.add(Launch);
                capabilities.add(Launch_Params);

                capabilities.add(Play);
                capabilities.add(Pause);
                capabilities.add(Stop);
                capabilities.add(Seek);
                capabilities.add(Position);
                capabilities.add(Duration);
                capabilities.add(PlayState);

                capabilities.add(WebAppLauncher.Close);

                if (getDLNAService() != null) {
                    capabilities.add(MediaPlayer.Subtitle_SRT);
                }
            } else {
                capabilities.addAll(WebAppLauncher.Capabilities);
                capabilities.addAll(MediaControl.Capabilities);

                capabilities.add(MediaPlayer.Subtitle_WebVTT);

                capabilities.add(PlaylistControl.JumpToTrack);
                capabilities.add(PlaylistControl.Next);
                capabilities.add(PlaylistControl.Previous);

                capabilities.add(MediaPlayer.Loop);
            }

        }

        setCapabilities(capabilities);
    }

    public List<String> getPermissions() {
        if (permissions != null) {
            return permissions;
        }

        List<String> defaultPermissions = new ArrayList<String>();
        Collections.addAll(defaultPermissions, kWebOSTVServiceOpenPermissions);

        if (DiscoveryManager.getInstance().getPairingLevel() == PairingLevel.ON) {
            Collections.addAll(defaultPermissions, kWebOSTVServiceProtectedPermissions);
            Collections.addAll(defaultPermissions, kWebOSTVServicePersonalActivityPermissions);
        }

        permissions = defaultPermissions;

        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;

        WebOSTVServiceConfig config = (WebOSTVServiceConfig) serviceConfig;

        if (config.getClientKey() != null) {
            config.setClientKey(null);

            if (isConnected()) {
                Log.w(Util.T, "Permissions changed -- you will need to re-pair to the TV.");
                disconnect();
            }
        }
    }

    private ProgramInfo parseRawProgramInfo(JsonObject programRawData) {
        String programId;
        String programName;

        ProgramInfo programInfo = new ProgramInfo();
        programInfo.setRawData(programRawData);

        programId = programRawData.get("programId").getAsString();
        programName = programRawData.get("programName").getAsString();
        ChannelInfo channelInfo = parseRawChannelData(programRawData);

        programInfo.setId(programId);
        programInfo.setName(programName);
        programInfo.setChannelInfo(channelInfo);

        return programInfo;
    }

    private ChannelInfo parseRawChannelData(JsonObject channelRawData) {
        String channelName = null;
        String channelId = null;
        String channelNumber = null;
        int minorNumber;
        int majorNumber;

        ChannelInfo channelInfo = new ChannelInfo();
        channelInfo.setRawData(channelRawData);

        if (!channelRawData.has("channelName")) {
            channelName = channelRawData.get("channelName").getAsString();
        }

        if (!channelRawData.has("channelId")) {
            channelId = channelRawData.get("channelId").getAsString();
        }

        channelNumber = channelRawData.get("channelNumber").getAsString();

        if (!channelRawData.has("majorNumber")) {
            majorNumber = channelRawData.get("majorNumber").getAsInt();
        } else {
            majorNumber = parseMajorNumber(channelNumber);
        }

        if (!channelRawData.has("minorNumber")) {
            minorNumber = channelRawData.get("minorNumber").getAsInt();
        } else {
            minorNumber = parseMinorNumber(channelNumber);
        }

        channelInfo.setName(channelName);
        channelInfo.setId(channelId);
        channelInfo.setNumber(channelNumber);
        channelInfo.setMajorNumber(majorNumber);
        channelInfo.setMinorNumber(minorNumber);

        return channelInfo;
    }

    private int parseMinorNumber(String channelNumber) {
        if (channelNumber != null) {
            String tokens[] = channelNumber.split("-");
            return Integer.parseInt(tokens[tokens.length - 1]);
        } else {
            return 0;
        }
    }

    private int parseMajorNumber(String channelNumber) {
        if (channelNumber != null) {
            String tokens[] = channelNumber.split("-");
            return Integer.parseInt(tokens[0]);
        } else {
            return 0;
        }
    }

    private List<ExternalInputInfo> externalnputInfoFromJsonArray(JsonArray inputList) {
        List<ExternalInputInfo> externalInputInfoList = new ArrayList<ExternalInputInfo>();

        inputList.forEach(element -> {

            JsonObject input = element.getAsJsonObject();

            String id = input.get("id").getAsString();
            String name = input.get("label").getAsString();
            boolean connected = input.get("connected").getAsBoolean();
            String iconURL = input.get("icon").getAsString();

            ExternalInputInfo inputInfo = new ExternalInputInfo();
            inputInfo.setRawData(input);
            inputInfo.setId(id);
            inputInfo.setName(name);
            inputInfo.setConnected(connected);
            inputInfo.setIconURL(iconURL);

            externalInputInfoList.add(inputInfo);

        });

        return externalInputInfoList;
    }

    // @Override
    // public LaunchSession decodeLaunchSession(String type, JsonObject obj) throws JSONException {
    // if ("webostv".equals(type)) {
    // LaunchSession launchSession = LaunchSession.launchSessionFromJsonObject(obj);
    // launchSession.setService(this);
    // return launchSession;
    // }
    // return null;
    // }

    @Override
    public void getPlayState(PlayStateListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported(new Object() {
        }.getClass().getEnclosingMethod().getName()));
    }

    @Override
    public ServiceSubscription<PlayStateListener> subscribePlayState(PlayStateListener listener) {
        Util.postError(listener, ServiceCommandError.notSupported(new Object() {
        }.getClass().getEnclosingMethod().getName()));

        return null;
    }

    @Override
    public boolean isConnectable() {
        return true;
    }

    public static interface ServiceInfoListener extends ResponseListener<JsonArray> {
    }

    public static interface SystemInfoListener extends ResponseListener<JsonObject> {
    }
}
