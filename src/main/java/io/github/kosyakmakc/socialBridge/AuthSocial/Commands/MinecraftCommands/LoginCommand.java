package io.github.kosyakmakc.socialBridge.AuthSocial.Commands.MinecraftCommands;

import io.github.kosyakmakc.socialBridge.AuthSocial.AuthModule;
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
    private final AuthModule module;

    public LoginCommand(AuthModule module) {
        super("login", AuthMessageKey.LOGIN_FROM_MINECRAFT_DESCRIPTION, AuthPermissions.CAN_LOGIN);
        random = new Random(System.currentTimeMillis());
        this.module = module;
    }

    @Override
    public void execute(MinecraftUser sender, List<Object> args) {
        var bridge = getBridge();
        var logger = module.getLogger();

        var code = random.nextInt(100_000, 1_000_000);
        var placeholders = new HashMap<String, String>();
        placeholders.put("placeholder-code", Integer.toString(code));

        var sessionDbRecord = new AuthSession(sender.getId(), code, Duration.ofMinutes(10));
        bridge.queryDatabase(databaseContext -> {
            try {
                databaseContext.getDaoTable(AuthSession.class).create(sessionDbRecord);
                return true;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "failed save auth session to database", e);
                return false;
            }
        })
        .thenCompose(isSuccess -> {
            if(isSuccess) {
                logger.info(sender.getName() + " start login session");
                return getBridge()
                    .getLocalizationService().getMessage(module, sender.getLocale(), AuthMessageKey.LOGIN_FROM_MINECRAFT)
                    .thenAccept(msgTemplate -> {
                        sender.sendMessage(msgTemplate, placeholders);
                    });
            }
            else {
                return getBridge()
                    .getLocalizationService().getMessage(module, sender.getLocale(), MessageKey.INTERNAL_SERVER_ERROR)
                    .thenAccept(msgTemplate -> {
                        sender.sendMessage(msgTemplate, new HashMap<>());
                    });
            }
        });

    }
}
