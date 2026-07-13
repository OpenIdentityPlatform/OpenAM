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
 * Copyright 2026 3A Systems LLC.
 */

package org.forgerock.openam.oauth2;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A mutable view of an OAuth2 access/refresh token that is exposed to the OAuth2 Access Token
 * Modification script (script context {@code OAUTH2_ACCESS_TOKEN_MODIFICATION}).
 * <p>
 * The script can read contextual information about the token being issued and add, override or
 * remove claims that will be merged into the resulting stateless JWT.
 */
public class ScriptableAccessToken {

    /** Read-only context values about the token being issued (sub, scope, realm, ...). */
    private final Map<String, Object> info;
    /** Claims added or overridden by the script. */
    private final Map<String, Object> fields = new HashMap<>();
    /** Claim names the script requested to be removed. */
    private final Set<String> removedFields = new HashSet<>();

    /**
     * Constructs a new {@code ScriptableAccessToken}.
     *
     * @param info the read-only context values exposed to the script via {@link #getField(String)}.
     */
    public ScriptableAccessToken(Map<String, Object> info) {
        this.info = info == null ? new HashMap<String, Object>() : new HashMap<>(info);
    }

    /**
     * Adds or overrides a claim on the token being issued.
     *
     * @param name the claim name.
     * @param value the claim value.
     */
    public void setField(String name, Object value) {
        if (name == null) {
            return;
        }
        fields.put(name, value);
        removedFields.remove(name);
    }

    /**
     * Returns the value of a claim previously set by the script, or, if not set, the value of the
     * matching read-only context value.
     *
     * @param name the claim/context name.
     * @return the value, or {@code null} if not present.
     */
    public Object getField(String name) {
        if (fields.containsKey(name)) {
            return fields.get(name);
        }
        return info.get(name);
    }

    /**
     * Requests removal of a claim from the token being issued. Note that only custom claims added
     * by the script (or non-protected claims) can be removed; protected/standard JWT claims are
     * preserved.
     *
     * @param name the claim name to remove.
     */
    public void removeField(String name) {
        if (name == null) {
            return;
        }
        fields.remove(name);
        removedFields.add(name);
    }

    /**
     * @return the read-only context values exposed to the script.
     */
    public Map<String, Object> getInfo() {
        return Collections.unmodifiableMap(info);
    }

    /**
     * @return the claims added or overridden by the script.
     */
    public Map<String, Object> getFields() {
        return fields;
    }

    /**
     * @return the names of the claims the script requested to be removed.
     */
    public Set<String> getRemovedFields() {
        return removedFields;
    }
}

