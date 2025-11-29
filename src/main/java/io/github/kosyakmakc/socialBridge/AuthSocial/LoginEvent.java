package io.github.kosyakmakc.socialBridge.AuthSocial;

import java.util.UUID;

import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;

public record LoginEvent(SocialUser user, UUID minecraftId) {}
