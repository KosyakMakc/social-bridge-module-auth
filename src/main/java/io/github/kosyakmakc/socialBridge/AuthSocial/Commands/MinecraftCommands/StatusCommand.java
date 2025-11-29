package io.github.kosyakmakc.socialBridge.AuthSocial.Commands.MinecraftCommands;

import io.github.kosyakmakc.socialBridge.AuthSocial.AuthModule;
import io.github.kosyakmakc.socialBridge.AuthSocial.Utils.AuthMessageKey;
import io.github.kosyakmakc.socialBridge.AuthSocial.Utils.AuthPermissions;
import io.github.kosyakmakc.socialBridge.Commands.MinecraftCommands.MinecraftCommandBase;
import io.github.kosyakmakc.socialBridge.MinecraftPlatform.MinecraftUser;

import java.util.HashMap;
import java.util.List;

public class StatusCommand extends MinecraftCommandBase {
    private final AuthModule module;

    public StatusCommand(AuthModule module) {
        super("status", AuthPermissions.CAN_STATUS);
        this.module = module;
    }

    @Override
    public void execute(MinecraftUser minecraftUser, List<Object> list) {
        minecraftUser.sendMessage(getBridge().getLocalizationService().getMessage(minecraftUser.getLocale(), AuthMessageKey.STATUS_COMMAND_HEADER), new HashMap<>());
        var isAny = false;
        for (var handler : module.getSocialHandlers()) {
            if (handler.isAuthorized(minecraftUser)) {
                isAny = true;
                var placeholders = new HashMap<String, String>();
                placeholders.put("social-platform-name", handler.getPlatform().getPlatformName());
                minecraftUser.sendMessage(getBridge().getLocalizationService().getMessage(minecraftUser.getLocale(), AuthMessageKey.STATUS_COMMAND_RECORD), placeholders);
            }
        }

        if (!isAny) {
            minecraftUser.sendMessage(getBridge().getLocalizationService().getMessage(minecraftUser.getLocale(), AuthMessageKey.STATUS_COMMAND_EMPTY), new HashMap<>());
        }
    }
}
