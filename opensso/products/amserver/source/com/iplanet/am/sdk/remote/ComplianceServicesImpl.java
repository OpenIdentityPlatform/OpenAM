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
 * $Id: ComplianceServicesImpl.java,v 1.5 2008/06/25 05:41:26 qcheng Exp $
 *
 */

package com.iplanet.am.sdk.remote;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMSDKBundle;
import com.iplanet.am.sdk.common.IComplianceServices;
import com.iplanet.dpro.session.Session;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import java.rmi.RemoteException;

public class ComplianceServicesImpl implements IComplianceServices {

    private SOAPClient client;

    private static Debug debug = RemoteServicesImpl.getDebug();

    public ComplianceServicesImpl(SOAPClient soapClient) {
        client = soapClient;
    }

    public boolean isAncestorOrgDeleted(SSOToken token, String dn,
            int profileType) throws AMException {
        try {

            Object[] objs = { token.getTokenID().toString(), dn,
                    new Integer(profileType) };
            Boolean res = ((Boolean) client.send(client.encodeMessage(
                    "isAncestorOrgDeleted", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null));
            return res.booleanValue();

        } catch (AMRemoteException amrex) {
            debug.error("ComplianceServicesImpl.isAncestorOrgDeleted()- "
                    + "encountered exception=", amrex);
            throw RemoteServicesImpl.convertException(amrex);        
        } catch (RemoteException rex) {
            debug.error("ComplianceServicesImpl.isAncestorOrgDeleted()- "
                    + "encountered exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            debug.error("ComplianceServicesImpl.isAncestorOrgDeleted()- "
                    + "encountered exception=", ex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }
    }

    public void verifyAndDeleteObject(SSOToken token, String profileDN)
            throws AMException {
        try {
            Object[] objs = { token.getTokenID().toString(), profileDN };
            client.send(client.encodeMessage("verifyAndDeleteObject", objs),
                    Session.getLBCookie(token.getTokenID().toString()), null);
        } catch (AMRemoteException amrex) {
            debug.error("ComplianceServicesImpl.verifyAndDeleteObject()- "
                    + "encountered exception=", amrex);
            throw RemoteServicesImpl.convertException(amrex);
        } catch (RemoteException rex) {
            debug.error("ComplianceServicesImpl.verifyAndDeleteObject()- "
                    + "encountered exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            debug.error("ComplianceServicesImpl.verifyAndDeleteObject()- "
                    + "encountered exception=", ex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }
    }

    public String getDeletedObjectFilter(int objectType) throws AMException,
            SSOException {
        try {

            Object[] objs = { new Integer(objectType) };
            return ((String) client.send(client.encodeMessage(
                    "getDeletedObjectFilter", objs), null, null));

        } catch (AMRemoteException amrex) {
            debug.error("ComplianceServicesImpl.getDeletedObjectFilter()- "
                    + "encountered exception=", amrex);
            throw RemoteServicesImpl.convertException(amrex);
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (RemoteException rex) {
            debug.error("ComplianceServicesImpl.getDeletedObjectFilter()- "
                    + "encountered exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            debug.error("ComplianceServicesImpl.getDeletedObjectFilter()- "
                    + "encountered exception=", ex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }
    }
}
