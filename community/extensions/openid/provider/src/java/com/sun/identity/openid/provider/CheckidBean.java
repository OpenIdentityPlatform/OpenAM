/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: CheckidBean.java,v 1.1 2009/08/24 11:51:49 hubertlvg Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2007 Paul C. Bryan
 */

package com.sun.identity.openid.provider;

import java.net.URL;
import java.security.Principal;
import java.util.regex.Matcher;

public class CheckidBean extends BackingBean
{
    public CheckidBean() {
        super();
    }


    /**
     * TODO: Description
     *
     * @return TODO.
     */
    protected Principal getPrincipal() {
        return request.getUserPrincipal();
    }



    /**
     * TODO: Description
     *
     * @param principal TODO.
     * @param identity TODO.
     * @return TODO.
     */
    protected boolean identityMatches(Principal principal, URL identity)
    {
        if (principal == null || identity == null) {
            return false;
        }

        String principalName = principal.getName();

        if (principalName == null || principalName.length() == 0) {
            return false;
        }

        Matcher principalMatcher = Config.getPattern(
                Config.PRINCIPAL_PATTERN).matcher(principalName);

        // principal name must match configured pattern
        if (!principalMatcher.matches()) {
            return false;
        }

        // extract principal user ID from principal name
        String principalUserId = principalMatcher.group(1);

        if (principalUserId.length() == 0) {
            return false;
        }

        Matcher identityMatcher = Config.getPattern(
                Config.IDENTITY_PATTERN).matcher(Codec.encodeURL(identity));

        // supplied OpenID identity must match configured pattern
        if (!identityMatcher.matches()) {
            return false;
        }

        // extract username from OpenID identity
        String identityUserId = identityMatcher.group(1);

        if (identityUserId.length() == 0) {
            return false;
        }

        return principalUserId.equals(identityUserId);
    }


}