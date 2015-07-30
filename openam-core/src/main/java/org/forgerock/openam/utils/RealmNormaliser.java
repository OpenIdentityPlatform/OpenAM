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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.utils;

import javax.inject.Singleton;

import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.utils.StringUtils;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;

/**
 * Normalises the realm so that the returned realm is never {@code null} or an empty String.
 *
 * @since 12.0.0
 */
@Singleton
public class RealmNormaliser {
    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final CoreWrapper coreWrapper = new CoreWrapper();

    /**
     * Normalises the realm.
     * <br/>
     * If the specified realm is {@code null} or an empty String, '/' is returned. Otherwise the specified realm is
     * checked for its validity and returned in "/" separated format . 
     *
     * @param realm The realm to normalise.
     * @return The normalised realm.
     */
    public String normalise(String realm) throws NotFoundException {
        if (StringUtils.isNotEmpty(realm)) {
            try {
                SSOToken adminToken = coreWrapper.getAdminToken();
                String orgDN = coreWrapper.getOrganization(adminToken, realm);
                return coreWrapper.convertOrgNameToRealmName(orgDN);
            } catch (SSOException ssoe) {
                logger.error("RealmNormaliser::Unable to verify realm : " + realm, ssoe);
            } catch(IdRepoException idre) {
                logger.error("RealmNormaliser::Unable to verify realm : " + realm, idre);
            }
            throw new NotFoundException("Invalid realm, " + realm);
        }
        return "/";
    }
}
