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

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class AuthModule implements IBridgeModule {
    private static final String NAME = "AuthSocial";

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
                TableUtils.createTableIfNotExists(ctx.getConnectionSource(), Association_telegram.class);
                var daoSession = ctx.registerTable(AuthSession.class);

                TableUtils.createTableIfNotExists(ctx.getConnectionSource(), Association_telegram.class);
                var daoTg = ctx.registerTable(Association_telegram.class);

                if (daoSession == null) {
                    throw new RuntimeException("Не удалось проинициализировать обязательную таблицу - " + AuthSession.class.getSimpleName());
                }

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
    public Runtime.Version getCompabilityVersion() {
        return Runtime.Version.parse("0.1.0");
    }

    @Override
    public List<ISocialCommand> getSocialCommands() {
        return List.of(
                new CommitLoginCommand(),
                new LogoutLoginCommand()
        );
    }

    @Override
    public List<IMinecraftCommand> getMinecraftCommands() {
        return List.of(
                new LoginCommand(),
                new StatusCommand()
        );
    }

    @Override
    public List<ITranslationSource> getTranslations() {
        return List.of(new English());
    }

    @Override
    public String getName() {
        return NAME;
    }

}