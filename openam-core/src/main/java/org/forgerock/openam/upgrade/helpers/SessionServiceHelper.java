/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock, Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.upgrade.helpers;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;
import java.util.HashSet;
import java.util.Set;
import org.forgerock.openam.upgrade.UpgradeException;

/**
 * This class is used by the upgrade mechanism (pre-upgrade) to set the value of
 * the iplanet-am-session-constraint-resulting-behavior attribute in the session
 * service to maintain the correct custom value. The behaviour is as follows:
 *
 * <ul> <li>Destroy All Sessions property is set to true and service settings is
 * DESTROY_OLD_SESSION; post upgrade setting should be DestroyAllAction.</li>
 * <li>Destroy All Sessions property is set to false and service settings is
 * DESTROY_OLD_SESSION; This is the default, no action need be taken.</li>
 * <li>Deny Access; If this is the current value then post upgrade value should
 * be DenyAccessAction.</li> </ul>
 *
 * @author steve
 */
public class SessionServiceHelper extends AbstractUpgradeHelper {

    private final static String DENY_ACCESS = "DENY_ACCESS";
    private final static String DESTROY_OLD_SESSION = "DESTROY_OLD_SESSION";
    private final static String NEW_DENY_ACCESS =
            "org.forgerock.openam.session.service.DenyAccessAction";
    private final static String DESTROY_ALL_SESSIONS_CLASS =
            "org.forgerock.openam.session.service.DestroyAllAction";
    private final static String DESTROY_OLDEST_SESSION_CLASS =
            "org.forgerock.openam.session.service.DestroyOldestAction";
    private static final String DESTROY_ALL_SESSIONS =
            "openam.session.destroy_all_sessions";
    private static final String DESTROY_OLDEST_SESSION =
            "openam.session.destroy_oldest_session";
    private static final String SESSION_CONSTRAINT_HANDLER_ATTR =
            "iplanet-am-session-constraint-handler";
    private static final String SESSION_CONSTRAINT_HANDLER_OLD_ATTR =
            "iplanet-am-session-constraint-resulting-behavior";
    private static final String SFO_USER_ATTR = "iplanet-am-session-store-username";
    private static final String SFO_PWD_ATTR = "iplanet-am-session-store-password";
    private static final String SFO_CPL_MAX_WAIT_TIME_ATTR = "iplanet-am-session-store-cpl-max-wait-time";
    private static final String SFO_JDBC_URL_ATTR = "iplanet-am-session-jdbc-url";

    public SessionServiceHelper() {
        attributes.add(SFO_USER_ATTR);
        attributes.add(SFO_PWD_ATTR);
        attributes.add(SFO_CPL_MAX_WAIT_TIME_ATTR);
        attributes.add(SFO_JDBC_URL_ATTR);
    }

    @Override
    public AttributeSchemaImpl addNewAttribute(Set<AttributeSchemaImpl> existingAttrs, AttributeSchemaImpl newAttr)
            throws UpgradeException {
        if (newAttr.getName().equals(SESSION_CONSTRAINT_HANDLER_ATTR)) {
            for (AttributeSchemaImpl attr : existingAttrs) {
                if (attr.getName().equals(SESSION_CONSTRAINT_HANDLER_OLD_ATTR)) {
                    Set<String> defaultValues = attr.getDefaultValues();
                    Set<String> newDefaultValues = new HashSet<String>();
                    if (destroyAllSessionsSet() && defaultValues.contains(DESTROY_OLD_SESSION)) {
                        newDefaultValues.add(DESTROY_ALL_SESSIONS_CLASS);
                        newAttr = updateDefaultValues(newAttr, newDefaultValues);
                    } else if (defaultValues.contains(DENY_ACCESS)) {
                        newDefaultValues.add(NEW_DENY_ACCESS);
                        newAttr = updateDefaultValues(newAttr, newDefaultValues);
                    } else if (destroyOldestSessionSet()) {
                        newDefaultValues.add(DESTROY_OLDEST_SESSION_CLASS);
                        newAttr = updateDefaultValues(newAttr, newDefaultValues);
                    }
                    break;
                }
            }
        }

        return newAttr;
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl existingAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException {
        String i18nKey = existingAttr.getI18NKey();
        if (i18nKey == null || i18nKey.isEmpty()) {
            //Since at the moment all the upgradable attributes should have empty i18nKey, it's safe to return null.
            //This way these attributes won't show up on the upgrade report.
            return null;
        }
        return newAttr;
    }

    private boolean destroyAllSessionsSet() {
        return SystemProperties.getAsBoolean(DESTROY_ALL_SESSIONS);
    }

    private boolean destroyOldestSessionSet() {
        return SystemProperties.getAsBoolean(DESTROY_OLDEST_SESSION);
    }
}
