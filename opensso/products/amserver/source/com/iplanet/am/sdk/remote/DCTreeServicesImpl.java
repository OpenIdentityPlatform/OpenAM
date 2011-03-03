/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 * $Id: DCTreeServicesImpl.java,v 1.4 2008/06/25 05:41:26 qcheng Exp $
 *
 */

package com.iplanet.am.sdk.remote;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMSDKBundle;
import com.iplanet.am.sdk.common.DCTreeServicesHelper;
import com.iplanet.am.sdk.common.IDCTreeServices;
import com.iplanet.dpro.session.Session;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import java.rmi.RemoteException;

public class DCTreeServicesImpl extends DCTreeServicesHelper implements
        IDCTreeServices {
    private SOAPClient client;

    private static Debug debug = RemoteServicesImpl.getDebug();

    public DCTreeServicesImpl(SOAPClient soapClient) {
        client = soapClient;
    }

    public String getOrganizationDN(SSOToken token, String domainName)
            throws AMException {
        try {
            Object[] objs = { token.getTokenID().toString(), domainName }; 
            return ((String) client.send(client.encodeMessage(
                  "getOrgDNFromDomain", objs), 
                  Session.getLBCookie(token.getTokenID().toString()), null));
        } catch (AMRemoteException amrex) {
            debug.error("DCTreeServicesImpl.getOrganizationDN()- "
                    + "encountered exception=", amrex);
            throw RemoteServicesImpl.convertException(amrex);
        } catch (RemoteException rex) {
            debug.error("DCTreeServicesImpl.getOrganizationDN()- "
                    + "encountered exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            debug.error("DCTreeServicesImpl.getOrganizationDN()- "
                    + "encountered exception=", ex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }
    }
}
