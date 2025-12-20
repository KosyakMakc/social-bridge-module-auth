package io.github.kosyakmakc.socialBridge.AuthSocial;

import io.github.kosyakmakc.socialBridge.Utils.AsyncEvent;

public class AuthEvents {
    public final AsyncEvent<LoginEvent> login = new AsyncEvent<LoginEvent>();
    public final AsyncEvent<LogoutEvent> logout = new AsyncEvent<LogoutEvent>();
}
