package io.github.kosyakmakc.socialBridge.AuthSocial.Commands.SocialCommands;

import io.github.kosyakmakc.socialBridge.AuthSocial.AuthModule;
import io.github.kosyakmakc.socialBridge.AuthSocial.DatabaseTables.AuthSession;
import io.github.kosyakmakc.socialBridge.AuthSocial.Utils.AuthMessageKey;
import io.github.kosyakmakc.socialBridge.AuthSocial.Utils.LoginState;
import io.github.kosyakmakc.socialBridge.Commands.Arguments.CommandArgument;
import io.github.kosyakmakc.socialBridge.Commands.SocialCommands.SocialCommandBase;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class CommitLoginCommand extends SocialCommandBase {
    private final AuthModule module;

    public CommitLoginCommand(AuthModule module) {
        super(
            "login",
            AuthMessageKey.COMMITED_LOGIN_DESCRIPTION,
            List.of(
                CommandArgument.ofInteger("auth-code")));
        this.module = module;
    }

    @Override
    public void execute(SocialUser sender, List<Object> args) {
        var bridge = getBridge();
        var logger = module.getLogger();

        var authCode = (int) args.getFirst();
        var placeholders = new HashMap<String, String>();

        bridge.queryDatabase(database -> {
            try {
                var availableSessions = database.getDaoTable(AuthSession.class).queryBuilder()
                        .orderBy(AuthSession.EXPIRED_AT_FIELD_NAME, true)
                        .where()
                        .eq(AuthSession.AUTH_CODE_FIELD_NAME, authCode)
                        .and()
                        .eq(AuthSession.IS_SPENT_FIELD_NAME, false)
                        .and()
                        .gt(AuthSession.EXPIRED_AT_FIELD_NAME, Date.from(Instant.now()))
                        .query();

                if (availableSessions.isEmpty()) {
                    return null;
                }

                var session = availableSessions.getFirst();

                session.spend();
                database.getDaoTable(AuthSession.class).update(session);

                return session.getMinecraftId();
            }
            catch (SQLException e) {
                logger.log(Level.SEVERE, "failed to commit login", e);
                return null;
            }
        })
        .thenCompose(minecraftId -> {
            if (minecraftId != null) {
                return module.Authorize(sender, minecraftId);
            }
            else {
                return CompletableFuture.completedFuture(LoginState.NotCommited);
            }
        })
        .thenAccept(loginState -> {
            placeholders.put("social-platform-name", sender.getPlatform().getPlatformName());
            switch (loginState) {
                case Commited -> {
                    logger.info(sender.getName() + " success commited login to " + sender.getPlatform().getPlatformName() + " platform");
                    //mcPlayer.sendMessage(getBridge().getLocalizationService().getMessage(sender.getLocale(), AuthMessageKey.COMMITED_LOGIN), placeholders);
                    getBridge()
                        .getLocalizationService().getMessage(module, sender.getLocale(), AuthMessageKey.SOCIAL_COMMITED_LOGIN)
                        .thenAccept(msgTemplate -> sender.sendMessage(msgTemplate, placeholders));
                }
                case NotCommited -> {
                    logger.info(sender.getName() + " failed to commit login");
                    getBridge()
                        .getLocalizationService().getMessage(module, sender.getLocale(), AuthMessageKey.COMMIT_LOGIN_FAILED)
                        .thenAccept(msgTemplate -> sender.sendMessage(msgTemplate, placeholders));
                }
                case DuplicationError -> {
                    logger.info(sender.getName() + " duplicating his logins to " + sender.getPlatform().getPlatformName() + ", ignoring it...");
                    getBridge()
                        .getLocalizationService().getMessage(module, sender.getLocale(), AuthMessageKey.YOU_ARE_ALREADY_AUTHORIZED)
                        .thenAccept(msgTemplate -> sender.sendMessage(msgTemplate, placeholders));
                }
                case NotSupportedPlatform -> {
                    logger.info(sender.getName() + " trying to commit on not supported platform " + sender.getPlatform().getPlatformName() + ", ignoring it...");
                    getBridge()
                        .getLocalizationService().getMessage(module, sender.getLocale(), AuthMessageKey.UNSUPPORTED_PLATFORM)
                        .thenAccept(msgTemplate -> sender.sendMessage(msgTemplate, placeholders));
                }
                default -> throw new IllegalArgumentException("Unexpected value: " + loginState);
            }
        });
    }
}
