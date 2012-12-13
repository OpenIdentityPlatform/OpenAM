/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SsoServerSAML2SPEntryImpl.java,v 1.3 2009/10/21 00:03:14 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.monitoring;

import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.agent.SnmpMib;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * This class extends the "SsoServerSAML2SPEntry" class.
 */
public class SsoServerSAML2SPEntryImpl extends SsoServerSAML2SPEntry {
    private static Debug debug = null;
    private static String myMibName;

    /**
     * Constructor
     */
    public SsoServerSAML2SPEntryImpl (SnmpMib myMib) {
        super(myMib);
        myMibName = myMib.getMibName();
        init();
    }

    private void init() {
        if (debug == null) {
            debug = Debug.getInstance("amMonitoring");
        }
    }

    public ObjectName
        createSsoServerSAML2SPEntryObjectName (MBeanServer server)
    {
        String classModule = "SsoServerSAML2SPEntryImpl." +
            "createSsoServerSAML2SPEntryObjectName: ";
        String prfx = "ssoServerSAML2SPEntry.";
        String realm = Agent.getEscRealmNameFromIndex(SsoServerRealmIndex);

        if (debug.messageEnabled()) {
            debug.message(classModule +
                "\n    SsoServerRealmIndex = " + SsoServerRealmIndex +
                "\n    SAML2SPIndex = " + SAML2SPIndex +
                "\n    SAML2SPName = " + SAML2SPName);
        }

        String objname = myMibName +
            "/ssoServerSAML2SPTable:" +
            prfx + "ssoServerRealmName=" + realm + "," +
            prfx + "sAML2SPName=" + SAML2SPName;

        try {
            if (server == null) {
                return null;
            } else {
                // is the object name sufficiently unique?
                return
                    new ObjectName(objname);
            }
        } catch (Exception ex) {
            debug.error(classModule + objname, ex);
            return null;
        }
    }

    public void incSAML2SPInvalArtifsRcvd() {
        long li = SAML2SPInvalidArtifactsRcvd.longValue();
        li++;
        SAML2SPInvalidArtifactsRcvd = Long.valueOf(li);
    }

    public void incSAML2SPValidAssertsRcvd() {
        long li = SAML2SPValidAssertionsRcvd.longValue();
        li++;
        SAML2SPValidAssertionsRcvd = Long.valueOf(li);
    }

    public void incSAML2SPRqtsSent() {
        long li = SAML2SPRqtsSent.longValue();
        li++;
        SAML2SPRqtsSent = Long.valueOf(li);
    }
}
