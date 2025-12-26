package io.github.kosyakmakc.socialBridge.AuthSocial.SocialPlatformHandlers;

import io.github.kosyakmakc.socialBridge.AuthSocial.AuthModule;
import io.github.kosyakmakc.socialBridge.AuthSocial.DatabaseTables.Association_telegram;
import io.github.kosyakmakc.socialBridge.AuthSocial.Utils.LoginState;
import io.github.kosyakmakc.socialBridge.ISocialBridge;
import io.github.kosyakmakc.socialBridge.MinecraftPlatform.MinecraftUser;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.Identifier;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import io.github.kosyakmakc.socialBridgeTelegram.TelegramPlatform;
import io.github.kosyakmakc.socialBridgeTelegram.TelegramUser;
import io.github.kosyakmakc.socialBridgeTelegram.DatabaseTables.TelegramUserTable;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TelegramHandler implements ISocialPlatformHandler {
    private final ISocialBridge bridge;
    private final Logger logger;

    public TelegramHandler (ISocialBridge bridge) {
        this.bridge = bridge;
        logger = Logger.getLogger(bridge.getLogger().getName() + '.' + AuthModule.NAME + '.' + this.getClass().getSimpleName());
    }

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
    public CompletableFuture<LoginState> Authorize(SocialUser sender, UUID minecraftId) {
        if (!(sender instanceof TelegramUser tgUser)) {
            throw new RuntimeException("incorrect usage, SocialUser(" + sender.getClass().getName() + ") MUST BE assigned to this ISocialPlatform(" + this.getClass().getName() + ")");
        }

        return bridge.queryDatabase(databaseContext -> {
            try {
                var existedRows = databaseContext.getDaoTable(Association_telegram.class)
                .queryBuilder()
                    .where()
                    .eq(Association_telegram.MINECRAFT_ID_FIELD_NAME, minecraftId)
                    .and()
                    .eq(Association_telegram.IS_DELETED_FIELD_NAME, false)
                    .countOf();
                    
                if (existedRows > 0) {
                    return LoginState.DuplicationError;
                } else {
                    var association = new Association_telegram(minecraftId, (long) tgUser.getId().value());
                    databaseContext.getDaoTable(Association_telegram.class).create(association);
                }
                
                return LoginState.Commited;
            }
            catch (SQLException err) {
                err.printStackTrace();
                return LoginState.NotCommited;
            }
        });
    }

    @Override
    public CompletableFuture<MinecraftUser> tryGetMinecraftUser(SocialUser socialUser) {
        if (!(socialUser instanceof TelegramUser tgUser)) {
            throw new RuntimeException("incorrect usage, SocialUser(" + socialUser.getClass().getName() + ") MUST BE assigned to this ISocialPlatform(" + this.getClass().getName() + ")");
        }
        return bridge.queryDatabase(databaseContext -> {
            try {
                var association = databaseContext.getDaoTable(Association_telegram.class)
                        .queryBuilder()
                        .where()
                        .eq(Association_telegram.TELEGRAM_ID_FIELD_NAME, tgUser.getId().value())
                        .and()
                        .eq(Association_telegram.IS_DELETED_FIELD_NAME, false)
                        .queryForFirst();

                if (association != null) {
                    return association.getMinecraftId();
                }

                return null;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "failed get minecraft user", e);
                return null;
            }
        })
        .thenCompose(uuid -> {
            if (uuid == null) {
                return CompletableFuture.completedStage(null);
            }
            else {
                return bridge.getMinecraftPlatform().tryGetUser(uuid);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isAuthorized(MinecraftUser minecraftUser) {
        return bridge.queryDatabase(databaseContext -> {
            try {
                var association = databaseContext.getDaoTable(Association_telegram.class)
                        .queryBuilder()
                        .where()
                        .eq(Association_telegram.MINECRAFT_ID_FIELD_NAME, minecraftUser.getId())
                        .and()
                        .eq(Association_telegram.IS_DELETED_FIELD_NAME, false)
                        .queryForFirst();

                if (association != null) {
                    return true;
                }

                return false;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "failed to check IsAuthorized minecraft-user", e);
                return false;
            }
        });

    }

    @Override
    public CompletableFuture<UUID> logoutUser(SocialUser socialUser) {
        if (!(socialUser instanceof TelegramUser tgUser)) {
            throw new RuntimeException("incorrect usage, SocialUser(" + socialUser.getClass().getName() + ") MUST BE assigned to this ISocialPlatform(" + this.getClass().getName() + ")");
        }
        return bridge.queryDatabase(databaseContext -> {
            try {
                var association = databaseContext.getDaoTable(Association_telegram.class)
                        .queryBuilder()
                        .where()
                        .eq(Association_telegram.TELEGRAM_ID_FIELD_NAME, tgUser.getId().value())
                        .and()
                        .eq(Association_telegram.IS_DELETED_FIELD_NAME, false)
                        .queryForFirst();

                if (association != null) {
                    association.Delete();
                    databaseContext.getDaoTable(Association_telegram.class).update(association);
                    return association.getMinecraftId();
                } else {
                    return null;
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "failed get minecraft user", e);
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<SocialUser> tryGetSocialUser(Identifier id) {
        return bridge.queryDatabase(databaseContext -> {try {
                var userRecord = databaseContext.getDaoTable(TelegramUserTable.class)
                        .queryBuilder()
                        .where()
                        .eq(TelegramUserTable.ID_FIELD_NAME, (long) id.value())
                        .queryForFirst();

                return userRecord;
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "failed get social user", e);
                return null;
            }
        })
        .thenApply(userRecord -> {
            if (userRecord == null) {
                return null;
            }
            else {
                return new TelegramUser(getPlatform(), userRecord);
            }
        });
    }
}
