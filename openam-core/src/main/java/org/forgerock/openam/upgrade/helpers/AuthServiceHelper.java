/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;
import java.util.Set;
import org.forgerock.openam.upgrade.UpgradeException;

/**
 * Used to upgrade the iPlanetAMAuthService. 
 * 
 * @author steve
 */
public class AuthServiceHelper extends AbstractUpgradeHelper { 
    // new modules
    private final static String SECURID = "com.sun.identity.authentication.modules.securid.SecurID";
    private final static String ADAPTIVE = "org.forgerock.openam.authentication.modules.adaptive.Adaptive";
    private final static String OAUTH2 = "org.forgerock.openam.authentication.modules.oauth2.OAuth";
    private final static String OATH = "org.forgerock.openam.authentication.modules.oath.OATH";
    private final static String DEVICE_PRINT = "org.forgerock.openam.authentication.modules.deviceprint.DevicePrintModule";
    private final static String PERSISTENT_COOKIE = "org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookie";
    
    // remove modules
    private final static String SAFEWORD = "com.sun.identity.authentication.modules.safeword.SafeWord";
    private final static String UNIX = "com.sun.identity.authentication.modules.unix.Unix";
    private final static String ATTR = "iplanet-am-auth-authenticators";

    public AuthServiceHelper() {
        attributes.add(ATTR);
    }
    
    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl existingAttr, AttributeSchemaImpl newAttr)
    throws UpgradeException {
        if (!(newAttr.getName().equals(ATTR))) {
            return newAttr;
        }
        
        Set<String> defaultValues = existingAttr.getDefaultValues();
        
        if (defaultValues.contains(SECURID) && defaultValues.contains(ADAPTIVE) && defaultValues.contains(OAUTH2)
                && defaultValues.contains(OATH) && defaultValues.contains(DEVICE_PRINT)
                && defaultValues.contains(PERSISTENT_COOKIE)
                && !defaultValues.contains(SAFEWORD) && !defaultValues.contains(UNIX)) {
            // nothing to do
            return null;
        }
        
        defaultValues.add(SECURID);
        defaultValues.add(ADAPTIVE);
        defaultValues.add(OAUTH2);
        defaultValues.add(OATH);
        defaultValues.add(DEVICE_PRINT);
        defaultValues.add(PERSISTENT_COOKIE);
        defaultValues.remove(SAFEWORD);
        defaultValues.remove(UNIX);
        newAttr = updateDefaultValues(newAttr, defaultValues);
        
        return newAttr;
    }    
}
