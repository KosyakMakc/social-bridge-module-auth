package io.github.kosyakmakc.socialBridge.AuthSocial.SocialPlatformHandlers;

import io.github.kosyakmakc.socialBridge.AuthSocial.AuthorizeDuplicationException;
import io.github.kosyakmakc.socialBridge.AuthSocial.DatabaseTables.Association_telegram;
import io.github.kosyakmakc.socialBridge.ISocialBridge;
import io.github.kosyakmakc.socialBridge.MinecraftPlatform.MinecraftUser;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import io.github.kosyakmakc.socialBridgeTelegram.TelegramPlatform;
import io.github.kosyakmakc.socialBridgeTelegram.TelegramUser;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.jetbrains.annotations.Nullable;

public record TelegramHandler(ISocialBridge bridge) implements ISocialPlatformHandler {

    @Override
    public boolean isConnected() {
        try {
            Class.forName("io.github.kosyakmakc.socialBridgeTelegram.TelegramPlatform");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public ISocialPlatform getPlatform() {
        return bridge.getSocialPlatform(TelegramPlatform.class);
    }

    @Override
    public void Authorize(SocialUser sender, UUID minecraftId) throws SQLException, AuthorizeDuplicationException {
        if (!(sender instanceof TelegramUser tgUser)) {
            throw new RuntimeException("incorrect usage, SocialUser(" + sender.getClass().getName() + ") MUST BE assigned to this ISocialPlatform(" + this.getClass().getName() + ")");
        }

        var isDuplicate = new AtomicBoolean(false);
        bridge.queryDatabase(databaseContext -> {
            var existedRows = databaseContext.getDaoTable(Association_telegram.class)
                    .queryBuilder()
                    .where()
                    .eq(Association_telegram.MINECRAFT_ID_FIELD_NAME, minecraftId)
                    .and()
                    .eq(Association_telegram.IS_DELETED_FIELD_NAME, false)
                    .countOf();

            if (existedRows > 0) {
                isDuplicate.set(true);
            } else {
                var association = new Association_telegram(minecraftId, (long) tgUser.getId());
                databaseContext.getDaoTable(Association_telegram.class).create(association);
            }

            return null;
        });

        if (isDuplicate.get()) {
            throw new AuthorizeDuplicationException();
        }
    }

    @Override
    public MinecraftUser tryGetMinecraftUser(SocialUser socialUser) {
        if (!(socialUser instanceof TelegramUser tgUser)) {
            throw new RuntimeException("incorrect usage, SocialUser(" + socialUser.getClass().getName() + ") MUST BE assigned to this ISocialPlatform(" + this.getClass().getName() + ")");
        }
        var logger = bridge.getLogger();
        var result = new AtomicReference<MinecraftUser>(null);
        try {
            bridge.queryDatabase(databaseContext -> {
                var association = databaseContext.getDaoTable(Association_telegram.class)
                        .queryBuilder()
                        .where()
                        .eq(Association_telegram.TELEGRAM_ID_FIELD_NAME, tgUser.getId())
                        .and()
                        .eq(Association_telegram.IS_DELETED_FIELD_NAME, false)
                        .queryForFirst();

                if (association != null) {
                    result.set(bridge.getMinecraftPlatform().getUser(association.getMinecraftId()));
                }

                return null;
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "failed get minecraft user", e);
        }

        return result.get();
    }

    @Override
    public boolean isAuthorized(MinecraftUser minecraftUser) {
        var logger = bridge.getLogger();
        var result = new AtomicReference<MinecraftUser>(null);
        try {
            bridge.queryDatabase(databaseContext -> {
                var association = databaseContext.getDaoTable(Association_telegram.class)
                        .queryBuilder()
                        .where()
                        .eq(Association_telegram.MINECRAFT_ID_FIELD_NAME, minecraftUser.getId())
                        .and()
                        .eq(Association_telegram.IS_DELETED_FIELD_NAME, false)
                        .queryForFirst();

                if (association != null) {
                    result.set(bridge.getMinecraftPlatform().getUser(association.getMinecraftId()));
                }

                return null;
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "failed to check IsAuthorized minecraft-user", e);
        }

        return result.get() != null;
    }

    @Override
    public @Nullable UUID logoutUser(SocialUser socialUser) {
        if (!(socialUser instanceof TelegramUser tgUser)) {
            throw new RuntimeException("incorrect usage, SocialUser(" + socialUser.getClass().getName() + ") MUST BE assigned to this ISocialPlatform(" + this.getClass().getName() + ")");
        }
        var logger = bridge.getLogger();
        var result = new AtomicReference<UUID>(null);
        try {
            bridge.queryDatabase(databaseContext -> {
                var association = databaseContext.getDaoTable(Association_telegram.class)
                        .queryBuilder()
                        .where()
                        .eq(Association_telegram.TELEGRAM_ID_FIELD_NAME, tgUser.getId())
                        .and()
                        .eq(Association_telegram.IS_DELETED_FIELD_NAME, false)
                        .queryForFirst();

                if (association != null) {
                    association.Delete();
                    databaseContext.getDaoTable(Association_telegram.class).update(association);
                    result.set(association.getMinecraftId());
                } else {
                    result.set(null);
                }

                return null;
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "failed get minecraft user", e);
        }

        return result.get();
    }
}
