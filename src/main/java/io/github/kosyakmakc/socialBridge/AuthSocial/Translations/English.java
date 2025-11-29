package io.github.kosyakmakc.socialBridge.AuthSocial.Translations;

import io.github.kosyakmakc.socialBridge.AuthSocial.Utils.AuthMessageKey;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.DefaultTranslations.ITranslationSource;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.LocalizationService;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.Tables.Localization;

import java.util.List;

public class English implements ITranslationSource {
    @Override
    public String getLanguage() {
        return LocalizationService.defaultLocale;
    }

    @Override
    public List<Localization> getRecords() {
        return List.of(
                // game formatted
                new Localization(getLanguage(), AuthMessageKey.LOGIN_FROM_MINECRAFT.key(), "Your authorization code - <placeholder-code>."),
                new Localization(getLanguage(), AuthMessageKey.COMMITED_LOGIN.key(), "You are <dark_green>successfully</dark_green> connected <social-platform-name> platform."),

                new Localization(getLanguage(), AuthMessageKey.STATUS_COMMAND_HEADER.key(), "You have active social sessions in:"),
                new Localization(getLanguage(), AuthMessageKey.STATUS_COMMAND_RECORD.key(), "- <social-platform-name>"),
                new Localization(getLanguage(), AuthMessageKey.STATUS_COMMAND_EMPTY.key(), "No any active session."),

                // social formatted
                new Localization(getLanguage(), AuthMessageKey.UNSUPPORTED_PLATFORM.key(), "Sorry, but this platform is unsupported."),
                new Localization(getLanguage(), AuthMessageKey.SOCIAL_COMMITED_LOGIN.key(), "You are successfully connected."),
                new Localization(getLanguage(), AuthMessageKey.YOU_ARE_ALREADY_AUTHORIZED.key(), "You are already authorized on this platform."),
                new Localization(getLanguage(), AuthMessageKey.COMMIT_LOGIN_FAILED.key(), "Unable confirm authorization with this code."),

                // shared text
                new Localization(getLanguage(), AuthMessageKey.LOGOUT_SUCCESS.key(), "You(<social-name>) are successfully logout from profile(<minecraft-name>) on this platform."), // also available <social-platform-name>
                new Localization(getLanguage(), AuthMessageKey.LOGOUT_FAILED.key(), "You(<social-name>) unable to logout - not authenticated.") // also available <social-platform-name>
        );
    }
}
