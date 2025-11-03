package io.github.kosyakmakc.socialBridge.AuthSocial.Commands.SocialCommands;

import io.github.kosyakmakc.socialBridge.AuthSocial.AuthModule;
import io.github.kosyakmakc.socialBridge.AuthSocial.Utils.AuthMessageKey;
import io.github.kosyakmakc.socialBridge.Commands.SocialCommands.SocialCommandBase;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;

import java.util.HashMap;
import java.util.List;

public class LogoutLoginCommand extends SocialCommandBase {
    public LogoutLoginCommand() {
        super("logout");
    }

    @Override
    public void execute(SocialUser sender, List<Object> args) {
        var bridge = getBridge();
        var logger = bridge.getLogger();

        var platformName = sender.getPlatform().getPlatformName();
        var socialName = sender.getName();

        var placeholders = new HashMap<String, String>();
        placeholders.put("social-platform-name", sender.getPlatform().getPlatformName());
        placeholders.put("social-name", socialName);

        var handler = bridge.getModule(AuthModule.class).getSocialHandler(sender.getPlatform());
        if (handler == null) {
            sender.sendMessage(getBridge().getLocalizationService().getMessage(sender.getLocale(), AuthMessageKey.UNSUPPORTED_PLATFORM), placeholders);
            return;
        }

        var player = handler.tryGetMinecraftUser(sender);
        if (player == null) {
            logger.info("social(" + sender.getName() + ") failed to logout - not authenticated.");
            sender.sendMessage(getBridge().getLocalizationService().getMessage(sender.getLocale(), AuthMessageKey.LOGOUT_FAILED), placeholders);
            return;
        }
        var minecraftName = player.getName();
        var isLogout = handler.logoutUser(sender);

        if (isLogout) {
            placeholders.put("minecraft-name", minecraftName);

            logger.info("minecraft(" + minecraftName + ") is logout from " + platformName + " platform.");
            sender.sendMessage(getBridge().getLocalizationService().getMessage(sender.getLocale(), AuthMessageKey.LOGOUT_SUCCESS), placeholders);
        }
        else {
            logger.info("social(" + sender.getName() + ") failed to logout - not authenticated.");
            sender.sendMessage(getBridge().getLocalizationService().getMessage(sender.getLocale(), AuthMessageKey.LOGOUT_FAILED), placeholders);
        }
    }
}
