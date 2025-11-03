package io.github.kosyakmakc.socialBridge.AuthSocial.Commands.MinecraftCommands;

import io.github.kosyakmakc.socialBridge.AuthSocial.DatabaseTables.AuthSession;
import io.github.kosyakmakc.socialBridge.AuthSocial.Utils.AuthMessageKey;
import io.github.kosyakmakc.socialBridge.AuthSocial.Utils.AuthPermissions;
import io.github.kosyakmakc.socialBridge.Commands.MinecraftCommands.MinecraftCommandBase;
import io.github.kosyakmakc.socialBridge.MinecraftPlatform.MinecraftUser;
import io.github.kosyakmakc.socialBridge.Utils.MessageKey;

import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

public class LoginCommand extends MinecraftCommandBase {
    private final Random random;

    public LoginCommand() {
        super("login", AuthPermissions.CAN_LOGIN);
        random = new Random(System.currentTimeMillis());
    }

    @Override
    public void execute(MinecraftUser sender, List<Object> args) {
        var bridge = getBridge();
        var logger = bridge.getLogger();

        var code = random.nextInt() % 1_000_000;
        var message = getBridge().getLocalizationService().getMessage(sender.getLocale(), AuthMessageKey.LOGIN_FROM_MINECRAFT);
        var placeholders = new HashMap<String, String>();
        placeholders.put("placeholder-code", Integer.toString(code));

        var sessionDbRecord = new AuthSession(sender.getId(), code, Duration.ofMinutes(10));
        try {
            bridge.queryDatabase(databaseContext -> {
                databaseContext.getDaoTable(AuthSession.class).create(sessionDbRecord);
                return null;
            });

            logger.info(sender.getName() + " start login session");
            sender.sendMessage(message, placeholders);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "failed save auth session to database", e);
            sender.sendMessage(getBridge().getLocalizationService().getMessage(sender.getLocale(), MessageKey.INTERNAL_SERVER_ERROR), new HashMap<>());
        }
    }
}
