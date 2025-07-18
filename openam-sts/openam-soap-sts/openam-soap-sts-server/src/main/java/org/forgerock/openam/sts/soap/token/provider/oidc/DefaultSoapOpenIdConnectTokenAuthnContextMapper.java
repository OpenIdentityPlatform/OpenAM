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
* information: "Portions Copyrighted [year] [name of copyright owner]".
*
* Copyright 2015 ForgeRock AS.
* Portions Copyrighted 2025 3A-Systems LLC.
*/

package org.forgerock.openam.sts.soap.token.provider.oidc;

import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.wss4j.dom.handler.WSHandlerResult;

import java.util.List;

/**
 * @see SoapOpenIdConnectTokenAuthnContextMapper
 * The default implementation simply returns null, as this claim is optional
 */
public class DefaultSoapOpenIdConnectTokenAuthnContextMapper implements SoapOpenIdConnectTokenAuthnContextMapper {
    @Override
    public String getAuthnContext(List<WSHandlerResult> securityPolicyBindingTraversalYield) {
        return null;
    }

    @Override
    public String getAuthnContextForDelegatedToken(List<WSHandlerResult> securityPolicyBindingTraversalYield, ReceivedToken delegatedToken) {
        return null;
    }
}
