package io.github.kosyakmakc.socialBridge.AuthSocial.DatabaseTables;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.Tables.IDatabaseTable;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@DatabaseTable(tableName = Association_telegram.TABLE_NAME)
public class Association_telegram implements IDatabaseTable {
    public static final String TABLE_NAME = "association_telegram";

    public static final String ID_FIELD_NAME = "id";
    public static final String MINECRAFT_ID_FIELD_NAME = "minecraft_id";
    public static final String TELEGRAM_ID_FIELD_NAME = "telegram_id";
    public static final String IS_DELETED_FIELD_NAME = "is_deleted";
    public static final String CREATED_AT_FIELD_NAME = "created_at";

    @DatabaseField(columnName = ID_FIELD_NAME, generatedId = true)
    private int id;

    @DatabaseField(columnName = MINECRAFT_ID_FIELD_NAME, index = true)
    private UUID minecraftId;

    @DatabaseField(columnName = TELEGRAM_ID_FIELD_NAME, index = true)
    private long telegramId;

    @DatabaseField(columnName = IS_DELETED_FIELD_NAME, index = true)
    private boolean isDeleted;

    @DatabaseField(columnName = CREATED_AT_FIELD_NAME)
    private Date createdAt;

    @SuppressWarnings("unused")
    public Association_telegram() {

    }

    @SuppressWarnings("unused")
    public Association_telegram(UUID minecraftId, long telegramId) {
        this.minecraftId = minecraftId;
        this.telegramId = telegramId;
        this.isDeleted = false;

        var now = Instant.now();
        this.createdAt = Date.from(now);
    }

    @SuppressWarnings("unused")
    public UUID getMinecraftId() {
        return this.minecraftId;
    }

    @SuppressWarnings("unused")
    public long getTelegramId() {
        return telegramId;
    }

    @SuppressWarnings("unused")
    public boolean isDeleted() {
        return isDeleted;
    }

    @SuppressWarnings("unused")
    public void Delete() {
        this.isDeleted = true;
    }

    @SuppressWarnings("unused")
    public Date getCreatedAt() {
        return createdAt;
    }
}
