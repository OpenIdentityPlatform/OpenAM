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

import java.util.Calendar;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.TimeUtils;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.DateUtils;
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

    public PartialSession(PartialToken partialToken) {
        for (CoreTokenField field : partialToken.getFields()) {
            if (field.equals(CoreTokenField.USER_ID)) {
                universalId = partialToken.getValue(CoreTokenField.USER_ID);
                try {
                    username = new AMIdentity(null, universalId).getName();
                } catch (IdRepoException ire) {
                    DEBUG.error("Unable to retrieve username from universal ID: {}", universalId, ire);
                }
            } else if (field.equals(SessionTokenField.REALM.getField())) {
                realm = partialToken.getValue(SessionTokenField.REALM.getField());
            } else if (field.equals(SessionTokenField.SESSION_HANDLE.getField())) {
                sessionHandle = partialToken.getValue(SessionTokenField.SESSION_HANDLE.getField());
            } else if (field.equals(SessionTokenField.LATEST_ACCESS_TIME.getField())) {
                latestAccessTime = DateUtils.toUTCDateFormat(TimeUtils.fromUnixTime(Long.valueOf(
                        partialToken.<String>getValue(SessionTokenField.LATEST_ACCESS_TIME.getField()))).getTime());
            } else if (field.equals(SessionTokenField.MAX_IDLE_EXPIRATION_TIME.getField())) {
                maxIdleExpirationTime = DateUtils.toUTCDateFormat(partialToken.<Calendar>getValue(
                        SessionTokenField.MAX_IDLE_EXPIRATION_TIME.getField()).getTime());
            } else if (field.equals(SessionTokenField.MAX_SESSION_EXPIRATION_TIME.getField())) {
                maxSessionExpirationTime = DateUtils.toUTCDateFormat(partialToken.<Calendar>getValue(
                        SessionTokenField.MAX_SESSION_EXPIRATION_TIME.getField()).getTime());
            }
        }
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
}
