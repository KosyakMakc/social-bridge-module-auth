package io.github.kosyakmakc.socialBridge.AuthSocial.Commands.SocialCommands;

import io.github.kosyakmakc.socialBridge.AuthSocial.AuthModule;
import io.github.kosyakmakc.socialBridge.AuthSocial.Utils.AuthMessageKey;
import io.github.kosyakmakc.socialBridge.Commands.SocialCommands.SocialCommandBase;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;

import java.util.HashMap;
import java.util.List;

public class LogoutLoginCommand extends SocialCommandBase {
    private final AuthModule module;

    public LogoutLoginCommand(AuthModule module) {
        super("logout", AuthMessageKey.LOGOUT_DESCRIPTION);
        this.module = module;
    }

    @Override
    public void execute(SocialUser sender, List<Object> args) {
        var logger = module.getLogger();

        var platformName = sender.getPlatform().getPlatformName();
        var socialName = sender.getName();

        var placeholders = new HashMap<String, String>();
        placeholders.put("social-platform-name", sender.getPlatform().getPlatformName());
        placeholders.put("social-name", socialName);

        module
            .tryGetMinecraftUser(sender)
            .thenCompose(player -> {
                if (player == null) {
                    logger.info("social(" + sender.getName() + ") failed to logout - not authenticated.");
                    return getBridge()
                        .getLocalizationService().getMessage(module, sender.getLocale(), AuthMessageKey.LOGOUT_FAILED)
                        .thenCompose(msgTemplate -> sender.sendMessage(msgTemplate, placeholders));
                }
                var minecraftName = player.getName();
                return module
                    .logoutUser(sender)
                    .thenCompose(minecraftId -> {
                        if (minecraftId != null) {
                            placeholders.put("minecraft-name", minecraftName);
                            
                            logger.info("minecraft(" + minecraftName + ") is logout from " + platformName + " platform.");
                            return getBridge()
                                .getLocalizationService().getMessage(module, sender.getLocale(), AuthMessageKey.LOGOUT_SUCCESS)
                                .thenCompose(msgTemplate -> sender.sendMessage(msgTemplate, placeholders));
                        }
                        else {
                            logger.info("social(" + sender.getName() + ") failed to logout - not authenticated.");
                            return getBridge()
                                .getLocalizationService().getMessage(module, sender.getLocale(), AuthMessageKey.LOGOUT_FAILED)
                                .thenCompose(msgTemplate -> sender.sendMessage(msgTemplate, placeholders));
                        }
                    });
            });
    }
}
