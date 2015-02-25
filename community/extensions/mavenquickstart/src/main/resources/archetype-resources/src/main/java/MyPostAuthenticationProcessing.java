/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 ForgeRock AS. All Rights Reserved
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

package ${packageName};

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.debug.Debug;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MyPostAuthenticationProcessing implements AMPostAuthProcessInterface {

    private static Debug debug = Debug.getInstance("PostAuthenticationProcessing");

    @Override
    public void onLoginSuccess(Map requestMap, HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {
        debug.message("PostAuthenticationProcessing#onLogin started");
        try {
            //get the identity for profileattribute checking
            AMIdentity identity = IdUtils.getIdentity(ssoToken);
            //set a session property based on previous logic
            ssoToken.setProperty("my.own.session.property", "value");
        } catch (IdRepoException ire) {
            debug.warning("IdRepoException occured during getIdentity", ire);
        } catch (SSOException ssoe) {
            debug.warning("SSOException occured during getIdentity", ssoe);
        }
    }

    @Override
    public void onLoginFailure(Map requestMap, HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
    }

    @Override
    public void onLogout(HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {
    }
}
