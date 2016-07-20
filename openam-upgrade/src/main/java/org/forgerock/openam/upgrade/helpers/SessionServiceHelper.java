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
 * Copyright 2011-2016 ForgeRock AS.
 */
package org.forgerock.openam.upgrade.helpers;

import static org.forgerock.openam.utils.CollectionUtils.asSet;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.forgerock.openam.cts.api.CoreTokenConstants;
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
    private static final String STATELESS_SIGNING_ALGORITHM = "openam-session-stateless-signing-type";
    private static final List<String> ECDSA_SIGNING_ALGORITHMS = Arrays.asList("ES256", "ES384", "ES512");
    private static final String STATELESS_ENCRYPTION_TYPE = "openam-session-stateless-encryption-type";
    private static final List<String> ENCRYPTION_TYPES = Arrays.asList("NONE", "RSA", "AES_KEYWRAP", "DIRECT");
    private static final String STATELESS_ENCRYPTION_KEY = "openam-session-stateless-encryption-aes-key";
    private static final String STATELESS_COMPRESSION_TYPE = "openam-session-stateless-compression-type";

    private final static String REDUCED_CROSSTALK_ENABLED = CoreTokenConstants.IS_REDUCED_CROSSTALK_ENABLED;

    public SessionServiceHelper() {
        attributes.add(SFO_USER_ATTR);
        attributes.add(SFO_PWD_ATTR);
        attributes.add(SFO_CPL_MAX_WAIT_TIME_ATTR);
        attributes.add(SFO_JDBC_URL_ATTR);
        attributes.add(STATELESS_SIGNING_ALGORITHM);
        attributes.add(STATELESS_ENCRYPTION_TYPE);
        attributes.add(STATELESS_ENCRYPTION_KEY);
        attributes.add(STATELESS_COMPRESSION_TYPE);
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
        } else if (REDUCED_CROSSTALK_ENABLED.equals(newAttr.getName())) {
            updateDefaultValues(newAttr, asSet("false"));
        }

        return newAttr;
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl existingAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException {
        AttributeSchemaImpl result = newAttr;
        String i18nKey = existingAttr.getI18NKey();
        if (i18nKey == null || i18nKey.isEmpty()) {
            //Since at the moment all the upgradable attributes should have empty i18nKey, it's safe to return null.
            //This way these attributes won't show up on the upgrade report.
            return null;
        }

        if (STATELESS_SIGNING_ALGORITHM.equals(existingAttr.getName())) {
            result = checkAndUpdateChoiceValues(existingAttr, newAttr, ECDSA_SIGNING_ALGORITHMS);
        } else if (STATELESS_ENCRYPTION_TYPE.equals(existingAttr.getName())) {
            result = checkAndUpdateChoiceValues(existingAttr, newAttr, ENCRYPTION_TYPES);
        }
        return result;
    }

    /**
     * Checks whether the given attribute already contains all of the given choice values, and if not upgrades it.
     *
     * @param existingAttr the existing attribute.
     * @param newAttr the new attribute.
     * @param newChoices the new choice values.
     * @return the new attribute if it was updated, or {@code null} if no update was necessary.
     * @throws UpgradeException if the choice values cannot be updated.
     */
    private AttributeSchemaImpl checkAndUpdateChoiceValues(AttributeSchemaImpl existingAttr,
            AttributeSchemaImpl newAttr, List<String> newChoices)
            throws UpgradeException {
        List<String> choices = new ArrayList<>(Arrays.asList(existingAttr.getChoiceValues()));
        if (choices.containsAll(newChoices)) {
            return null;
        } else {
            choices.addAll(newChoices);
            updateChoiceValues(newAttr, choices);
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
