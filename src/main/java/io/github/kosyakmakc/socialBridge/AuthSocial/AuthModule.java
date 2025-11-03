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
import io.github.kosyakmakc.socialBridge.IBridgeModule;
import io.github.kosyakmakc.socialBridge.ISocialBridge;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.Utils.Version;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class AuthModule implements IBridgeModule {
    private static final String NAME = "AuthSocial";

    public static final List<ISocialCommand> socialCommands = List.of(
            new CommitLoginCommand(),
            new LogoutLoginCommand()
    );

    public static final List<IMinecraftCommand> minecraftCommands = List.of(
            new LoginCommand(),
            new StatusCommand()
    );

    public static final List<ITranslationSource> translationSources = List.of(
            new English()
    );

    public static final Version SocialBridge_CompabilityVersion = new Version(0, 2, 0);

    private final HashMap<ISocialPlatform, ISocialPlatformHandler> socialHandlersMap;

    public AuthModule() {
        socialHandlersMap = new HashMap<>();
    }

    public Collection<ISocialPlatformHandler> getSocialHandlers() {
        return socialHandlersMap.values();
    }

    public ISocialPlatformHandler getSocialHandler(ISocialPlatform platform) {
        return socialHandlersMap.getOrDefault(platform, null);
    }

    @Override
    public boolean init(ISocialBridge bridge) {
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
                    throw new RuntimeException("Не удалось проинициализировать обязательную таблицу - " + AuthSession.class.getSimpleName());
                }

                TableUtils.createTableIfNotExists(ctx.getConnectionSource(), Association_telegram.class);
                var daoTg = ctx.registerTable(Association_telegram.class);

                if (daoTg == null) {
                    throw new RuntimeException("Не удалось проинициализировать обязательную таблицу - " + Association_telegram.class.getSimpleName());
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