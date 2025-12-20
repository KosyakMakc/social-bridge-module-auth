package io.github.kosyakmakc.socialBridge.AuthSocial.SocialPlatformHandlers;

import io.github.kosyakmakc.socialBridge.AuthSocial.Utils.LoginState;
import io.github.kosyakmakc.socialBridge.MinecraftPlatform.MinecraftUser;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ISocialPlatformHandler {
    boolean isConnected();
    ISocialPlatform getPlatform();

    CompletableFuture<LoginState> Authorize(SocialUser sender, UUID minecraftId);
    CompletableFuture<MinecraftUser> tryGetMinecraftUser(SocialUser socialUser);
    CompletableFuture<Boolean> isAuthorized(MinecraftUser minecraftUser);
    CompletableFuture<UUID> logoutUser(SocialUser socialUser);
}
