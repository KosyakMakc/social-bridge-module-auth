package io.github.kosyakmakc.socialBridge.AuthSocial.SocialPlatformHandlers;

import io.github.kosyakmakc.socialBridge.AuthSocial.AuthorizeDuplicationException;
import io.github.kosyakmakc.socialBridge.MinecraftPlatform.MinecraftUser;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;

import java.sql.SQLException;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public interface ISocialPlatformHandler {
    boolean isConnected();
    ISocialPlatform getPlatform();

    void Authorize(SocialUser sender, UUID minecraftId) throws SQLException, AuthorizeDuplicationException;
    MinecraftUser tryGetMinecraftUser(SocialUser socialUser);
    boolean isAuthorized(MinecraftUser minecraftUser);
    @Nullable UUID logoutUser(SocialUser socialUser);
}
