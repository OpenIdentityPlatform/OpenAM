/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2014 ForgeRock AS.
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
 */

package org.forgerock.openam.upgrade.helpers;

import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;
import java.util.Set;
import static org.forgerock.openam.utils.CollectionUtils.*;
import org.forgerock.openam.upgrade.UpgradeException;

/**
 * Used to upgrade the iPlanetAMAuthService.
 */
public class AuthServiceHelper extends AbstractUpgradeHelper {
    // new modules
    private final static String SECURID = "com.sun.identity.authentication.modules.securid.SecurID";
    private final static String ADAPTIVE = "org.forgerock.openam.authentication.modules.adaptive.Adaptive";
    private final static String OAUTH2 = "org.forgerock.openam.authentication.modules.oauth2.OAuth";
    private final static String OATH = "org.forgerock.openam.authentication.modules.oath.OATH";
    private final static String PERSISTENT_COOKIE = "org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookie";
    private final static String OPEN_ID_CONNECT = "org.forgerock.openam.authentication.modules.oidc.OpenIdConnect";
    private final static String SCRIPTED = "org.forgerock.openam.authentication.modules.scripted.Scripted";
    private final static String SCRIPTED_DEVICE_PRINT = "org.forgerock.openam.authentication.modules.deviceprint.DeviceIdMatch";
    private final static String DEVICE_PRINT_PERSIST = "org.forgerock.openam.authentication.modules.deviceprint.DeviceIdSave";

    // remove modules
    private final static String SAFEWORD = "com.sun.identity.authentication.modules.safeword.SafeWord";
    private final static String UNIX = "com.sun.identity.authentication.modules.unix.Unix";
    private final static String ATTR = "iplanet-am-auth-authenticators";
    // other attributes
    private final static String XUI = "openam-xui-interface-enabled";
    private static final String GOTO_DOMAINS = "iplanet-am-auth-valid-goto-domains";

    public AuthServiceHelper() {
        attributes.add(ATTR);
        attributes.add(GOTO_DOMAINS);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl existingAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException {
        if (GOTO_DOMAINS.equals(newAttr.getName())) {
            return existingAttr.getI18NKey() != null && !existingAttr.getI18NKey().isEmpty() ? newAttr : null;
        }
        if (!(newAttr.getName().equals(ATTR))) {
            return newAttr;
        }

        Set<String> defaultValues = existingAttr.getDefaultValues();

        if (defaultValues.contains(SECURID) && defaultValues.contains(ADAPTIVE) && defaultValues.contains(OAUTH2)
                && defaultValues.contains(OATH) && defaultValues.contains(PERSISTENT_COOKIE)
                && defaultValues.contains(OPEN_ID_CONNECT) && defaultValues.contains(SCRIPTED)
                && defaultValues.contains(SCRIPTED_DEVICE_PRINT) && defaultValues.contains(DEVICE_PRINT_PERSIST)
                && !defaultValues.contains(SAFEWORD) && !defaultValues.contains(UNIX)) {
            // nothing to do
            return null;
        }

        defaultValues.add(SECURID);
        defaultValues.add(ADAPTIVE);
        defaultValues.add(OAUTH2);
        defaultValues.add(OATH);
        defaultValues.add(PERSISTENT_COOKIE);
        defaultValues.add(OPEN_ID_CONNECT);
        defaultValues.add(SCRIPTED);
        defaultValues.add(SCRIPTED_DEVICE_PRINT);
        defaultValues.add(DEVICE_PRINT_PERSIST);
        defaultValues.remove(SAFEWORD);
        defaultValues.remove(UNIX);
        newAttr = updateDefaultValues(newAttr, defaultValues);

        return newAttr;
    }

    @Override
    public AttributeSchemaImpl addNewAttribute(Set<AttributeSchemaImpl> existingAttrs, AttributeSchemaImpl newAttr)
            throws UpgradeException {

        if (!newAttr.getName().equals(XUI)) {
            return newAttr;
        }

        // XUI should not be default for upgraded systems
        newAttr = updateDefaultValues(newAttr, asSet("false"));
        return newAttr;
    }

}
