package io.github.kosyakmakc.socialBridge.AuthModule.paper;

import io.github.kosyakmakc.socialBridge.AuthSocial.AuthModule;
import io.github.kosyakmakc.socialBridge.SocialBridge;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuthModulePlugin extends JavaPlugin {
    public AuthModulePlugin() {
        SocialBridge.INSTANCE.registerModule(new AuthModule());
    }
 }
