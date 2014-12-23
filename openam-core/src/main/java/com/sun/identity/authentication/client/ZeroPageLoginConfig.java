/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package com.sun.identity.authentication.client;

import org.forgerock.json.fluent.JsonValue;

import java.util.Collections;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.*;

/**
 * Interface to different methods for configuring Zero Page Login (ZPL). For local authentication, this uses the
 * LoginState, otherwise (DAS) it uses system properties.
 */
public final class ZeroPageLoginConfig {
    private final boolean enabled;
    private final Set<String> whitelist;
    private final boolean allowWithoutReferer;

    public ZeroPageLoginConfig(final boolean enabled, final Set<String> whitelist, final boolean allowWithoutReferer) {
        this.enabled = enabled;
        this.whitelist = whitelist == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(whitelist);
        this.allowWithoutReferer = allowWithoutReferer;
    }

    /**
     * Indicates whether ZPL is enabled at all.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the whitelist of allowed Referer URLs.
     */
    public Set<String> getRefererWhitelist() {
        return whitelist;
    }

    /**
     * Indicates whether ZPL requests should be allowed if the request does not include a Referer header.
     */
    public boolean isAllowedWithoutReferer() {
        return allowWithoutReferer;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ZeroPageLoginConfig that = (ZeroPageLoginConfig) o;

        return allowWithoutReferer == that.allowWithoutReferer && enabled == that.enabled &&
                whitelist.equals(that.whitelist);
    }

    @Override
    public int hashCode() {
        int result = (enabled ? 1 : 0);
        result = 31 * result + whitelist.hashCode();
        result = 31 * result + (allowWithoutReferer ? 1 : 0);
        return result;
    }

    public JsonValue toJson() {
        return json(object(field("enabled", enabled),
                           field("whitelist", whitelist),
                           field("allowWithoutReferer", allowWithoutReferer)));
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
