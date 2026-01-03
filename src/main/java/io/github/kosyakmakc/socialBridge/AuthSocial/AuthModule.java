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
import io.github.kosyakmakc.socialBridge.AuthSocial.Translations.Russian;
import io.github.kosyakmakc.socialBridge.AuthSocial.Utils.LoginState;
import io.github.kosyakmakc.socialBridge.Commands.MinecraftCommands.IMinecraftCommand;
import io.github.kosyakmakc.socialBridge.Commands.SocialCommands.ISocialCommand;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.DefaultTranslations.ITranslationSource;
import io.github.kosyakmakc.socialBridge.MinecraftPlatform.IModuleLoader;
import io.github.kosyakmakc.socialBridge.MinecraftPlatform.MinecraftUser;
import io.github.kosyakmakc.socialBridge.ISocialBridge;
import io.github.kosyakmakc.socialBridge.ISocialModule;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import io.github.kosyakmakc.socialBridge.Utils.Version;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class AuthModule implements ISocialModule {
    public static final UUID ID = UUID.fromString("11752e9b-8968-42ca-8513-6ce3e52a27b4");
    public static final Version SocialBridge_CompabilityVersion = new Version(0, 5, 0);
    public static final String NAME = "auth";
    private Logger logger;
    private ISocialBridge bridge;

    private final IModuleLoader loader;
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
            new English(),
            new Russian()
    );

    private final HashMap<ISocialPlatform, ISocialPlatformHandler> socialHandlersMap;

    public AuthModule(IModuleLoader loader) {
        this.loader = loader;
        socialHandlersMap = new HashMap<>();
    }

    @Override
    public UUID getId() {
        return ID;
    }

    @Override
    public IModuleLoader getLoader() {
        return loader;
    }

    public CompletableFuture<LoginState> Authorize(SocialUser socialUser, UUID minecraftId) {
        var handler = getSocialHandler(socialUser.getPlatform());
        if (handler == null) {
            return CompletableFuture.completedFuture(LoginState.NotSupportedPlatform);
        }

        return handler
            .Authorize(socialUser, minecraftId)
            .thenCompose(loginState -> {
                if (loginState == LoginState.Commited) {
                    return events.login.invoke(new LoginEvent(socialUser, minecraftId))
                    .thenApply(Void -> loginState);
                }
                
                return CompletableFuture.completedFuture(loginState);
            });
    }

    public CompletableFuture<MinecraftUser> tryGetMinecraftUser(SocialUser socialUser) {
        
        var handler = getSocialHandler(socialUser.getPlatform());
        if (handler == null) {
            return null;
        }

        return handler.tryGetMinecraftUser(socialUser);
    }

    public CompletableFuture<UUID> logoutUser(SocialUser socialUser) {
        
        var handler = getSocialHandler(socialUser.getPlatform());
        if (handler == null) {
            return null;
        }

        return handler
            .logoutUser(socialUser)
            .thenCompose(minecraftId -> {
                if (minecraftId != null) {
                    return events.login
                            .invoke(new LoginEvent(socialUser, minecraftId))
                            .thenApply(Void -> minecraftId);
                }

                return CompletableFuture.completedFuture(minecraftId);
            });
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
    public CompletableFuture<Boolean> enable(ISocialBridge bridge) {
        logger = Logger.getLogger(bridge.getLogger().getName() + '.' + NAME);
        this.bridge = bridge;

        for (var handler : List.of(
                new TelegramHandler(bridge)
        )) {
            if (handler.isConnected()) {
                socialHandlersMap.put(handler.getPlatform(), handler);
            }
        }

        return bridge.queryDatabase(ctx -> {
            try {
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

                return true;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        })
        .exceptionally(err -> {
            err.printStackTrace();
            return false;
        });
    }

    @Override
    public CompletableFuture<Boolean> disable() {
        this.bridge = null;
        socialHandlersMap.clear();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public ISocialBridge getBridge() {
        return bridge;
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