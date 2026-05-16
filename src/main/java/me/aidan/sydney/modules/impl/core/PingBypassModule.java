package me.aidan.sydney.modules.impl.core;

import me.aidan.sydney.Sydney;
import me.aidan.sydney.modules.Module;
import me.aidan.sydney.modules.RegisterModule;
import me.aidan.sydney.proxy.PingBypassProxy;
import me.aidan.sydney.settings.impl.BooleanSetting;
import me.aidan.sydney.settings.impl.ModeSetting;
import me.aidan.sydney.settings.impl.NumberSetting;
import me.aidan.sydney.settings.impl.StringSetting;

import java.io.IOException;

@RegisterModule(name = "PingBypass", description = "Runs a local PingBypass proxy server, or connects through a remote one.", category = Module.Category.CORE, drawn = false)
public class PingBypassModule extends Module {
    public ModeSetting mode = new ModeSetting("Mode", "Proxy mode.", "Local", new String[]{"Local", "Remote"});
    public StringSetting remoteIp = new StringSetting("RemoteIP", "The IP address of the remote PingBypass proxy server.", "0.0.0.0");
    public NumberSetting remotePort = new NumberSetting("RemotePort", "The port of the remote PingBypass proxy server.", 25565, 1, 65535);
    public StringSetting targetIp = new StringSetting("TargetIP", "The target Minecraft server IP to forward to (local mode).", "localhost");
    public NumberSetting targetPort = new NumberSetting("TargetPort", "The target Minecraft server port (local mode).", 25565, 1, 65535);
    public NumberSetting listenPort = new NumberSetting("ListenPort", "Local port to listen on (local mode).", 25566, 1, 65535);
    public BooleanSetting autoStart = new BooleanSetting("AutoStart", "Automatically starts the proxy when the module is enabled.", true);

    private PingBypassProxy proxy;

    @Override
    public void onEnable() {
        if (mode.getValue().equalsIgnoreCase("Remote")) {
            Sydney.CHAT_MANAGER.tagged("Remote mode: connect to " + remoteIp.getValue() + ":" + remotePort.getValue().intValue() + " in your multiplayer menu.", getName());
            return;
        }

        if (proxy != null && proxy.isRunning()) {
            Sydney.CHAT_MANAGER.tagged("Proxy is already running on port " + listenPort.getValue().intValue(), getName());
            return;
        }

        proxy = new PingBypassProxy(
                targetIp.getValue(),
                targetPort.getValue().intValue(),
                listenPort.getValue().intValue()
        );

        new Thread(() -> {
            try {
                proxy.start();
            } catch (IOException e) {
                Sydney.CHAT_MANAGER.tagged("Failed to start proxy: " + e.getMessage(), getName());
            }
        }, "PingBypassProxy").start();

        Sydney.CHAT_MANAGER.tagged("Proxy started on port " + listenPort.getValue().intValue()
                + " -> " + targetIp.getValue() + ":" + targetPort.getValue().intValue(), getName());
    }

    @Override
    public void onDisable() {
        if (proxy != null && proxy.isRunning()) {
            proxy.stop();
            proxy = null;
            Sydney.CHAT_MANAGER.tagged("Proxy stopped.", getName());
        }
    }
}
