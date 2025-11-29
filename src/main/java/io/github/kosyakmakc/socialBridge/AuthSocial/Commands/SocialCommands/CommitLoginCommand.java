package io.github.kosyakmakc.socialBridge.AuthSocial.Commands.SocialCommands;

import io.github.kosyakmakc.socialBridge.AuthSocial.AuthModule;
import io.github.kosyakmakc.socialBridge.AuthSocial.AuthorizeDuplicationException;
import io.github.kosyakmakc.socialBridge.AuthSocial.DatabaseTables.AuthSession;
import io.github.kosyakmakc.socialBridge.AuthSocial.Utils.AuthMessageKey;
import io.github.kosyakmakc.socialBridge.Commands.Arguments.CommandArgument;
import io.github.kosyakmakc.socialBridge.Commands.SocialCommands.SocialCommandBase;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import io.github.kosyakmakc.socialBridge.Utils.MessageKey;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class CommitLoginCommand extends SocialCommandBase {
    private final AuthModule module;

    public CommitLoginCommand(AuthModule module) {
        super(
            "login",
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

        try {
            AtomicReference<CommitLoginState> commitState = new AtomicReference<>(CommitLoginState.NotCommited);
            bridge.queryDatabase(database -> {
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
                    commitState.set(CommitLoginState.NotCommited);
                    return null;
                }

                var session = availableSessions.getFirst();

                try {
                    var isSuccess = module.Authorize(sender, session.getMinecraftId());
                    if (isSuccess) {
                        commitState.set(CommitLoginState.Commited);

                        session.spend();
                        database.getDaoTable(AuthSession.class).update(session);
                    }
                    else {
                        commitState.set(CommitLoginState.NotSupportedPlatform);
                    }
                } catch (AuthorizeDuplicationException e) {
                    commitState.set(CommitLoginState.DuplicationError);
                }

                return null;
            });

            placeholders.put("social-platform-name", sender.getPlatform().getPlatformName());
            var resultState = commitState.get();
            switch (resultState) {
                case Commited -> {
                    logger.info(sender.getName() + " success commited login to " + sender.getPlatform().getPlatformName() + " platform");
                    //mcPlayer.sendMessage(getBridge().getLocalizationService().getMessage(sender.getLocale(), AuthMessageKey.COMMITED_LOGIN), placeholders);
                    sender.sendMessage(getBridge().getLocalizationService().getMessage(sender.getLocale(), AuthMessageKey.SOCIAL_COMMITED_LOGIN), placeholders);
                }
                case NotCommited -> {
                    logger.info(sender.getName() + " failed to commit login");
                    sender.sendMessage(getBridge().getLocalizationService().getMessage(sender.getLocale(), AuthMessageKey.COMMIT_LOGIN_FAILED), placeholders);
                }
                case DuplicationError -> {
                    logger.info(sender.getName() + " duplicating his logins to " + sender.getPlatform().getPlatformName() + ", ignoring it...");
                    sender.sendMessage(getBridge().getLocalizationService().getMessage(sender.getLocale(), AuthMessageKey.YOU_ARE_ALREADY_AUTHORIZED), placeholders);
                }
                case NotSupportedPlatform -> {
                    logger.info(sender.getName() + " trying to commit on not supported platform " + sender.getPlatform().getPlatformName() + ", ignoring it...");
                    sender.sendMessage(getBridge().getLocalizationService().getMessage(sender.getLocale(), AuthMessageKey.UNSUPPORTED_PLATFORM), placeholders);
                }
                default -> throw new IllegalArgumentException("Unexpected value: " + resultState);
            }
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, "failed to commit login", e);
            sender.sendMessage(getBridge().getLocalizationService().getMessage(sender.getLocale(), MessageKey.INTERNAL_SERVER_ERROR), placeholders);
        }
    }

    enum CommitLoginState {
        Commited,
        NotCommited,
        DuplicationError,
        NotSupportedPlatform,
    }
}
