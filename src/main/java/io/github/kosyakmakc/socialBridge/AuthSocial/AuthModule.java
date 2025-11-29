package io.github.kosyakmakc.socialBridge.AuthSocial;

import com.j256.ormlite.table.TableUtils;
import io.github.kosyakmakc.socialBridge.AuthSocial.Commands.MinecraftCommands.LoginCommand;
import io.github.kosyakmakc.socialBridge.AuthSocial.Commands.MinecraftCommands.StatusCommand;
import io.github.kosyakmakc.socialBridge.AuthSocial.Commands.SocialCommands.CommitLoginCommand;
import io.github.kosyakmakc.socialBridge.AuthSocial.Commands.SocialCommands.LogoutLoginCommand;
import io.github.kosyakmakc.socialBridge.AuthSocial.DatabaseTables.Association_telegram;
import io.github.kosyakmakc.socialBridge.AuthSocial.DatabaseTables.AuthSession;
import io.github.kosyakmakc.socialBridge.AuthSocial.SocialPlatformHandlers.ISocialPlatformHandler;
import io.github.kosyakmakc.socialBridge.AuthSocial.SocialPlatformHandlers.TelegramHandler;
import io.github.kosyakmakc.socialBridge.AuthSocial.Translations.English;
import io.github.kosyakmakc.socialBridge.Commands.MinecraftCommands.IMinecraftCommand;
import io.github.kosyakmakc.socialBridge.Commands.SocialCommands.ISocialCommand;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.DefaultTranslations.ITranslationSource;
import io.github.kosyakmakc.socialBridge.MinecraftPlatform.MinecraftUser;
import io.github.kosyakmakc.socialBridge.IBridgeModule;
import io.github.kosyakmakc.socialBridge.ISocialBridge;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import io.github.kosyakmakc.socialBridge.Utils.Version;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.jetbrains.annotations.Nullable;

public class AuthModule implements IBridgeModule {
    public static final Version SocialBridge_CompabilityVersion = new Version(0, 2, 1);
    private static final String NAME = "AuthSocial";
    private Logger logger;

    public final AuthEvents events = new AuthEvents();

    public final List<ISocialCommand> socialCommands = List.of(
            new CommitLoginCommand(this),
            new LogoutLoginCommand(this)
    );

    public final List<IMinecraftCommand> minecraftCommands = List.of(
            new LoginCommand(this),
            new StatusCommand(this)
    );

    public final List<ITranslationSource> translationSources = List.of(
            new English()
    );

    private final HashMap<ISocialPlatform, ISocialPlatformHandler> socialHandlersMap;

    public AuthModule() {
        socialHandlersMap = new HashMap<>();
    }

    public boolean Authorize(SocialUser socialUser, UUID minecraftId) throws AuthorizeDuplicationException, SQLException{
        var handler = getSocialHandler(socialUser.getPlatform());
        if (handler == null) {
            return false;
        }

        handler.Authorize(socialUser, minecraftId);
        events.login.invoke(new LoginEvent(socialUser, minecraftId));
        return true;
    }

    public @Nullable MinecraftUser tryGetMinecraftUser(SocialUser socialUser) {
        
        var handler = getSocialHandler(socialUser.getPlatform());
        if (handler == null) {
            return null;
        }

        return handler.tryGetMinecraftUser(socialUser);
    }

    public @Nullable UUID logoutUser(SocialUser socialUser) {
        
        var handler = getSocialHandler(socialUser.getPlatform());
        if (handler == null) {
            return null;
        }

        var minecraftId = handler.logoutUser(socialUser);
        if (minecraftId != null) {
            events.login.invoke(new LoginEvent(socialUser, minecraftId));
        }
        return minecraftId;
    }

    public Logger getLogger() {
        return logger;
    }

    public Collection<ISocialPlatformHandler> getSocialHandlers() {
        return socialHandlersMap.values();
    }

    public ISocialPlatformHandler getSocialHandler(ISocialPlatform platform) {
        return socialHandlersMap.getOrDefault(platform, null);
    }

    @Override
    public boolean init(ISocialBridge bridge) {
        logger = Logger.getLogger(bridge.getLogger().getName() + '.' + NAME);

        for (var handler : List.of(
                new TelegramHandler(bridge)
        )) {
            if (handler.isConnected()) {
                socialHandlersMap.put(handler.getPlatform(), handler);
            }
        }

        try {
            bridge.queryDatabase(ctx -> {
                TableUtils.createTableIfNotExists(ctx.getConnectionSource(), AuthSession.class);
                var daoSession = ctx.registerTable(AuthSession.class);

                if (daoSession == null) {
                    throw new RuntimeException("Failed to create required database table - " + AuthSession.class.getSimpleName());
                }

                TableUtils.createTableIfNotExists(ctx.getConnectionSource(), Association_telegram.class);
                var daoTg = ctx.registerTable(Association_telegram.class);

                if (daoTg == null) {
                    throw new RuntimeException("Failed to create required database table - " + Association_telegram.class.getSimpleName());
                }

                return null;
            });
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Version getCompabilityVersion() {
        return SocialBridge_CompabilityVersion;
    }

    @Override
    public List<ISocialCommand> getSocialCommands() {
        return socialCommands;
    }

    @Override
    public List<IMinecraftCommand> getMinecraftCommands() {
        return minecraftCommands;
    }

    @Override
    public List<ITranslationSource> getTranslations() {
        return translationSources;
    }

    @Override
    public String getName() {
        return NAME;
    }
}