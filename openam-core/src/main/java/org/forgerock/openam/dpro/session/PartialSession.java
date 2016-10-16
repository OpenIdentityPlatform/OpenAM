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
import static org.forgerock.openam.session.SessionConstants.*;

import java.util.Calendar;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.tokens.CoreTokenField;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.debug.Debug;

/**
 * The JSON representation of partially described session objects.
 */
public class PartialSession {

    private static final Debug DEBUG = Debug.getInstance(SessionConstants.SESSION_DEBUG);
    private String username;
    private String universalId;
    private String realm;
    private String sessionHandle;
    private String maxIdleExpirationTime;
    private String maxSessionExpirationTime;

    public PartialSession(PartialToken partialToken) {
        universalId = partialToken.getValue(CoreTokenField.USER_ID);
        try {
            username = new AMIdentity(null, universalId).getName();
        } catch (IdRepoException ire) {
            DEBUG.error("Unable to retrieve username from universal ID: {}", universalId, ire);
        }
        realm = partialToken.getValue(SessionTokenField.REALM.getField());
        sessionHandle = partialToken.getValue(SessionTokenField.SESSION_HANDLE.getField());
        maxIdleExpirationTime = DateUtils.toUTCDateFormat(
                partialToken.<Calendar>getValue(SessionTokenField.MAX_IDLE_EXPIRATION_TIME.getField()).getTime());
        maxSessionExpirationTime = DateUtils.toUTCDateFormat(
                partialToken.<Calendar>getValue(SessionTokenField.MAX_SESSION_EXPIRATION_TIME.getField()).getTime());
    }

    public JsonValue asJson() {
        return json(object(
                field(JSON_SESSION_USERNAME, username),
                field(JSON_SESSION_UNIVERSAL_ID, universalId),
                field(JSON_SESSION_REALM, realm),
                field(JSON_SESSION_HANDLE, sessionHandle),
                field(JSON_SESSION_MAX_IDLE_EXPIRATION_TIME, maxIdleExpirationTime),
                field(JSON_SESSION_MAX_SESSION_EXPIRATION_TIME, maxSessionExpirationTime)
        ));
    }
}
