package org.leoms.admin.security;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientAddressAccess {
    private ClientAddressAccess() {}

    public static String from(HttpServletRequest request) {
        return ClientAddress.from(request);
    }
}
