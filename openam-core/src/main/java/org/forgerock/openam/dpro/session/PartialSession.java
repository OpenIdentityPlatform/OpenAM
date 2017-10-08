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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.dpro.session;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.*;
import static org.forgerock.openam.session.SessionConstants.*;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.util.annotations.VisibleForTesting;

import com.sun.identity.shared.debug.Debug;

/**
 * The JSON representation of partially described session objects.
 */
@Title(SESSION_RESOURCE + "schema." + TITLE)
@Description(SESSION_RESOURCE + "schema." + DESCRIPTION)
public class PartialSession {

    private static final Debug DEBUG = Debug.getInstance(SessionConstants.SESSION_DEBUG);
    @Title(SESSION_RESOURCE + "schema.property.username." + TITLE)
    @Description(SESSION_RESOURCE + "schema.property.username." + DESCRIPTION)
    private String username;
    @Title(SESSION_RESOURCE + "schema.property.universalId." + TITLE)
    @Description(SESSION_RESOURCE + "schema.property.universalId." + DESCRIPTION)
    private String universalId;
    @Title(SESSION_RESOURCE + "schema.property.realm." + TITLE)
    @Description(SESSION_RESOURCE + "schema.property.realm." + DESCRIPTION)
    private String realm;
    @Title(SESSION_RESOURCE + "schema.property.sessionHandle." + TITLE)
    @Description(SESSION_RESOURCE + "schema.property.sessionHandle." + DESCRIPTION)
    private String sessionHandle;
    @Title(SESSION_RESOURCE + "schema.property.latestAccessTime." + TITLE)
    @Description(SESSION_RESOURCE + "schema.property.latestAccessTime." + DESCRIPTION)
    private String latestAccessTime;
    @Title(SESSION_RESOURCE + "schema.property.maxIdleExpirationTime." + TITLE)
    @Description(SESSION_RESOURCE + "schema.property.maxIdleExpirationTime." + DESCRIPTION)
    private String maxIdleExpirationTime;
    @Title(SESSION_RESOURCE + "schema.property.maxSessionExpirationTime." + TITLE)
    @Description(SESSION_RESOURCE + "schema.property.maxSessionExpirationTime." + DESCRIPTION)
    private String maxSessionExpirationTime;

    private PartialSession(String username, String universalId, String realm, String sessionHandle,
            String latestAccessTime, String maxIdleExpirationTime, String maxSessionExpirationTime) {
        this.username = username;
        this.universalId = universalId;
        this.realm = realm;
        this.sessionHandle = sessionHandle;
        this.latestAccessTime = latestAccessTime;
        this.maxIdleExpirationTime = maxIdleExpirationTime;
        this.maxSessionExpirationTime = maxSessionExpirationTime;
    }

    /**
     * Returns the username associated with this partial session.
     *
     * @return The user friendly username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the universal ID associated with this partial session.
     *
     * @return The universal ID of the user this partial session belongs to.
     */
    public String getUniversalId() {
        return universalId;
    }

    /**
     * Returns the realm associated with this partial session.
     *
     * @return The realm where this session was created.
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Returns the session handle associated with this partial session.
     *
     * @return The session's handle.
     */
    public String getSessionHandle() {
        return sessionHandle;
    }

    /**
     * The timestamp of when the session was last used.
     *
     * @return The timestamp of when the session was last used.
     */
    public String getLatestAccessTime() {
        return latestAccessTime;
    }

    /**
     * The timestamp of when the max idle timeout will be reached.
     *
     * @return When this session will time out due to inactivity.
     */
    public String getMaxIdleExpirationTime() {
        return maxIdleExpirationTime;
    }

    /**
     * The timestamp of when the max session timeout will be reached.
     *
     * @return When the session will time out.
     */
    public String getMaxSessionExpirationTime() {
        return maxSessionExpirationTime;
    }

    public JsonValue asJson() {
        return json(object(
                fieldIfNotNull(JSON_SESSION_USERNAME, username),
                fieldIfNotNull(JSON_SESSION_UNIVERSAL_ID, universalId),
                fieldIfNotNull(JSON_SESSION_REALM, realm),
                fieldIfNotNull(JSON_SESSION_HANDLE, sessionHandle),
                fieldIfNotNull(JSON_SESSION_LATEST_ACCESS_TIME, latestAccessTime),
                fieldIfNotNull(JSON_SESSION_MAX_IDLE_EXPIRATION_TIME, maxIdleExpirationTime),
                fieldIfNotNull(JSON_SESSION_MAX_SESSION_EXPIRATION_TIME, maxSessionExpirationTime)
        ));
    }

    /**
     * A builder to help with construction of {@link PartialSession} instances.
     *
     * @see PartialSessionFactory
     */
    @VisibleForTesting
    public static class Builder {

        private String username;
        private String universalId;
        private String realm;
        private String sessionHandle;
        private String latestAccessTime;
        private String maxIdleExpirationTime;
        private String maxSessionExpirationTime;

        /**
         * Set the username.
         *
         * @param username The username to set.
         * @return This builder.
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * Set the universal ID.
         *
         * @param universalId The universal ID to set.
         * @return This builder.
         */
        public Builder universalId(String universalId) {
            this.universalId = universalId;
            return this;
        }

        /**
         * Set the realm.
         *
         * @param realm The realm to set.
         * @return This builder.
         */
        public Builder realm(String realm) {
            this.realm = realm;
            return this;
        }

        /**
         * Set the session handle.
         *
         * @param sessionHandle The session handle to set.
         * @return This builder.
         */
        public Builder sessionHandle(String sessionHandle) {
            this.sessionHandle = sessionHandle;
            return this;
        }

        /**
         * Set the latest access time.
         *
         * @param latestAccessTime The latest access time to set.
         * @return This builder.
         */
        public Builder latestAccessTime(String latestAccessTime) {
            this.latestAccessTime = latestAccessTime;
            return this;
        }

        /**
         * Set the max idle expiration time.
         *
         * @param maxIdleExpirationTime The max idle expiration time to set.
         * @return This builder.
         */
        public Builder maxIdleExpirationTime(String maxIdleExpirationTime) {
            this.maxIdleExpirationTime = maxIdleExpirationTime;
            return this;
        }

        /**
         * Set the max session expiration time.
         *
         * @param maxSessionExpirationTime The max session expiration time to set.
         * @return This builder.
         */
        public Builder maxSessionExpirationTime(String maxSessionExpirationTime) {
            this.maxSessionExpirationTime = maxSessionExpirationTime;
            return this;
        }

        /**
         * Builds a {@link PartialSession} object based on the previously provided parameters.
         *
         * @return The constructed {@link PartialSession} instance.
         */
        public PartialSession build() {
            return new PartialSession(username, universalId, realm, sessionHandle, latestAccessTime,
                    maxIdleExpirationTime, maxSessionExpirationTime);
        }
    }
}
