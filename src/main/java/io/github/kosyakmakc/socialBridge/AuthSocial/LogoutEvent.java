package io.github.kosyakmakc.socialBridge.AuthSocial;

import io.github.kosyakmakc.socialBridge.MinecraftPlatform.MinecraftUser;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;

public record LogoutEvent(SocialUser socialUser, MinecraftUser minecraftUser) { }
