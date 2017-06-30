# Connect SDK Binding

The binding integrates LG WebOS based smart TVs.  This binding uses a [forked version](https://github.com/sprehn/Connect-SDK-Java-Core) of LG's [Connect SDK](https://github.com/ConnectSDK/Connect-SDK-Android-Core) library.


## Supported Things

### LG webOS smart TVs

LG webOS based smart TVs are supported.

#### TV Settings

The TV must be connected to the same network as OpenHAB with a permanent IP address. If the IP changes the binding will discover it as a different device. 

* Connected to the same network as OpenHab
* Under network settings allow "LG CONNECT APPS" to connect.

Note: Under general settings allow mobile applications to connect, if this option is available. In combination with the wake on LAN binding this will allow you to start the TV via OpenHAB.

## Binding Configuration

The binding has only one configuration parameter, which is only required if the binding cannot atomatically detect OpenHAB's local IP address: 
| Name | Description |
| --- | --- |
| LocalIP |  This is the local IP of your OpenHAB host on the network. (Optional) |

The binding will attempt to auto detect your IP, if LocalIP is not set. This works when your hostname resolves to this IP and not to the loopback interface or, if the system has exactly one non-loop back network interface. Otherwise this has to be explicitly set. If you are unable to discover devices please check the log file for error messages.e.g.: 
```
Autodetection of local IP (via getNetworkInterfaces) failed, as multiple interfaces where detected.
```

## Discovery
TVs are auto discovered through SSDP in the local network. The binding broadcast a search message via UDP on the network. 

## Thing Configuration

WebOS TV has no configuration parameters. Please note that at least one channel must be bound to an item before the binding will make an attempt to connect and pair with the TV once that one is turned on.

## Channels

| Channel Type ID | Item Type    | Description  | Read/Write |
| --------------- | ------------ | ------------ | ---------- |
| power | Switch | Current power setting. TV can only be powered off, not on. (ON/OFF) | RW |
| mute | Switch | Current mute setting.  (ON/OFF) |  RW |
| volume | Number | Current volume setting. Note this only works with reasonable values when using internal speakers. Connected to an external amp the volume should be controlled using volumeUp and volumeDown switches to set relative volume. |  RW |
| volumeUp | Switch | Increase volume. (ON/OFF) |  W | 
| volumeDown | Switch | Decrease volume. (ON/OFF) | W | 
| channel | String | Current channel | RW | 
| channelUp | Switch | One channel up (ON/OFF) |  W |
| channelDown | Switch | One channel down (ON/OFF) |  W |
| channelName | String | Current channel name |  R |
| toast | String | Displays a short message on the TV screen. See also rules section. |  W |
| mediaForward | Switch | Media control forward (ON/OFF) |  W | 
| mediaPause | Switch | Media control pause (ON/OFF) | W |
| mediaPlay | Switch | Media control play (ON/OFF) |  W |
| mediaRewind | Switch | Media control rewind (ON/OFF) | W | 
| mediaStop | Switch | Media control stop (ON/OFF) |  W |
| mediaState | String | Media's current state |  R |
| appLauncher | String | Application ID of currently running application. This also allows to start applications on the TV by sending a specific Application ID to this channel. |  RW |


## Actions
### Show Toast
```
showToast(String ip, String text)
```
Sends a toast message to a webOS device using OpenHAB's logo as an icon.
The first parameter is the IP address of your TV. 
The second parameter is the message you want to display.
### Show Toast with Custom Icon
```
showToast(String ip, String icon, String text)
```
Sends a toast message to a webOS device with custom icon. 
The first parameter is the IP address of your TV. 
The second parameter for the icon has to be provided as a URL. To use openhab's icon set you could send this URL for example: http://localhost:8080/icon/energy?state=UNDEF&format=png
The third parameter is the message you want to display.

### Launch a URL
```
launchBrowser(String ip, String url)
```
Opens the given URL in the TV's browser app.

The first parameter is the IP address of your TV. 
The second parameter is the URL you want to open.

### Launch an Application

```
launchApplication(String deviceId, String appId)
```
Opens the application with given appId. To find out what appId constant matches which app, bind the appLauncher channel to a String item and turn the TV to the desired application.

The first parameter is the IP address of your TV. 
The second parameter is the application id that you want to open.


## Full Example
This example assumes the IP of your smart TV is 192.168.2.119.

demo.items:
```
Switch LG_TV0_Power "TV Power" <television> { channel="connectsdk:WebOSTV:192_168_2_119:power" }
Switch LG_TV0_Mute  "TV Mute" { channel="connectsdk:WebOSTV:192_168_2_119:mute"}
Number LG_TV0_Volume "Volume [%S]" { channel="connectsdk:WebOSTV:192_168_2_119:volume" }
Switch LG_TV0_VolumeDown "Volume -" { autoupdate="false", channel="connectsdk:WebOSTV:192_168_2_119:volumeDown" }
Switch LG_TV0_VolumeUp "Volume +" { autoupdate="false", channel="connectsdk:WebOSTV:192_168_2_119:volumeUp" }
Number LG_TV0_ChannelNo "Channel #" { channel="connectsdk:WebOSTV:192_168_2_119:channel" }
Switch LG_TV0_ChannelDown "Channel -"  { autoupdate="false", channel="connectsdk:WebOSTV:192_168_2_119:channelDown"  }
Switch LG_TV0_ChannelUp "Channel +"  { autoupdate="false", channel="connectsdk:WebOSTV:192_168_2_119:channelUp"  }
String LG_TV0_Channel "Channel [%S]"  { channel="connectsdk:WebOSTV:192_168_2_119:channelName"}

String LG_TV0_MediaState "MediaState [%s]" {channel="connectsdk:WebOSTV:192_168_2_119:mediaState"}
String LG_TV0_Toast { channel="connectsdk:WebOSTV:192_168_2_119:toast"}
Switch LG_TV0_Play ">"  { autoupdate="false", channel="connectsdk:WebOSTV:192_168_2_119:mediaPlay" }
Switch LG_TV0_Stop "Stop" { autoupdate="false", channel="connectsdk:WebOSTV:192_168_2_119:mediaStop" }
Switch LG_TV0_Pause "||" { autoupdate="false", channel="connectsdk:WebOSTV:192_168_2_119:mediaPause" }
Switch LG_TV0_Forward ">>" { autoupdate="false", channel="connectsdk:WebOSTV:192_168_2_119:mediaForward" }
Switch LG_TV0_Rewind "<<" { autoupdate="false", channel="connectsdk:WebOSTV:192_168_2_119:mediaRewind" }
String LG_TV0_Application "Application [%s]"  {channel="connectsdk:WebOSTV:192_168_2_119:appLauncher"} 
Player LG_TV0_Player 

// this assumes you also have the wake on lan binding configured & You need to update your broadcast and mac address
Switch LG_TV0_WOL   { wol="192.168.2.255#3c:cd:93:c2:20:e0" }
```

demo.sitemap:

```
sitemap demo label="Main Menu"
{
    Frame label="TV" {
        Switch item=LG_TV0_Power
        Switch item=LG_TV0_Mute
        Text item=LG_TV0_Volume
        Switch item=LG_TV0_VolumeDown
        Switch item=LG_TV0_VolumeUp
        Text item=LG_TV0_ChannelNo
        Text item=LG_TV0_Channel
        Switch item=LG_TV0_ChannelDown
        Switch item=LG_TV0_ChannelUp
        Text item=LG_TV0_MediaState
        Default item=LG_TV0_Player 
        Text item=LG_TV0_Application
        Selection item=LG_TV0_Application mappings=[
            "com.webos.app.livetv"="TV",
            "com.webos.app.tvguide"="TV Guide",
            "netflix" = "Netflix",
            "youtube.leanback.v4" = "Youtube",
            "spotify-beehive" = "Spotify",
            "com.webos.app.hdmi1" = "HDMI 1",
            "com.webos.app.hdmi2" = "HDMI 2",
            "com.webos.app.hdmi3" = "HDMI 3",
            "com.webos.app.hdmi4" = "HDMI 4",
            "com.webos.app.externalinput.av1" = "AV1",
            "com.webos.app.externalinput.av2" = "AV2",
            "com.webos.app.externalinput.component" = "Component",
            "com.webos.app.externalinput.scart" = "Scart"]
    }
}
```


demo.rules:

```
// this assumes you also have the wake on lan binding configured.
rule "Power on TV via Wake on LAN"
when 
Item LG_TV0_Power received command ON
then
    sendCommand( LG_TV0_WOL, ON) 
end


rule "Player 0"
when Item LG_TV0_Player received command
then
logInfo("Player2", receivedCommand.toString)
switch receivedCommand {
    case NEXT: LG_TV0_Forward.send(ON)
    case PLAY: LG_TV0_Play.send(ON)
    case PAUSE: LG_TV0_Pause.send(ON)
    case PREVIOUS: LG_TV0_Rewind.send(ON)
}
end

rule "Player 1"
when Item LG_TV0_Play changed to ON
then
    LG_TV0_Player.send(PLAY)
end

rule "Player 2"
when Item LG_TV0_Pause changed to ON
then
    LG_TV0_Player.send(PAUSE)
end
```


Example of a toast message. 
```
LG_TV0_Toast.sendCommand("Hello World")
```


