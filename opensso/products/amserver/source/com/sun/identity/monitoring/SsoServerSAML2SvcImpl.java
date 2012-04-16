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
 * $Id: SsoServerSAML2SvcImpl.java,v 1.3 2009/10/21 00:03:14 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.monitoring;

import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.agent.SnmpMib;
import javax.management.MBeanServer;

/**
 * This class extends the "SsoServerSAML2Svc" class.
 */
public class SsoServerSAML2SvcImpl extends SsoServerSAML2Svc {
    private static Debug debug = null;

    public static final String IDP_ARTIF_CACHE = "idpArtifCache";
    public static final String IDP_ARTIF_ISSUED = "idpArtifIssued";
    public static final String IDP_ASSERT_CACHE = "idpAssertCache";
    public static final String IDP_ASSERT_ISSUED = "idpAssertIssued";
    public static final String IDP_RQTS_RCVD = "idpRqtRcvd";
    public static final String IDP_INVAL_RQTS_RCVD = "idpInvalRqtRcvd";
    public static final String SP_VAL_ASSERTS_RCVD = "spValidAssertRcvd";
    public static final String SP_RQTS_SENT = "spRqtSent";
    public static final String SP_INVAL_ARTIFS_RCVD = "spInvalArtifRcvd";

    /**
     * Constructor
     */
    public SsoServerSAML2SvcImpl (SnmpMib myMib) {
        super(myMib);
        init(myMib, null);
    }

    public SsoServerSAML2SvcImpl (SnmpMib myMib, MBeanServer server) {
        super(myMib, server);
        init(myMib, server);
    }


    private void init (SnmpMib myMib, MBeanServer server) {
        if (debug == null) {
            debug = Debug.getInstance("amMonitoring");
        }
    }

    public void incHostedIDPCount() {
        setStatus();
        long li = SAML2HostedIDPCount.longValue();
        li++;
        SAML2HostedIDPCount = Long.valueOf(li);
    }

    public void incRemoteIDPCount() {
        setStatus();
        long li = SAML2RemoteIDPCount.longValue();
        li++;
        SAML2RemoteIDPCount = Long.valueOf(li);
    }

    public void incFedSessionCount() {
        setStatus();
        long li = SAML2FedSessionCount.longValue();
        li++;
        SAML2FedSessionCount = Long.valueOf(li);
    }

    public void decFedSessionCount() {
        long li = SAML2FedSessionCount.longValue();
        li--;
        SAML2FedSessionCount = Long.valueOf(li);
    }

    public void setFedSessionCount(long count) {
        setStatus();
        SAML2FedSessionCount = Long.valueOf(count);
    }

    public void incIdpSessionCount() {
        setStatus();
        long li = SAML2IDPSessionCount.longValue();
        li++;
        SAML2IDPSessionCount = Long.valueOf(li);
    }

    public void decIdpSessionCount() {
        long li = SAML2IDPSessionCount.longValue();
        li--;
        SAML2IDPSessionCount = Long.valueOf(li);
    }

    public void setIdpSessionCount(long count) {
        setStatus();
        SAML2IDPSessionCount = Long.valueOf(count);
    }

    public void incIDPCounter (String realm, String idpName, String counter) {
        String classMethod = "SsoServerSAML2SvcImpl.incIDPCounter:";

        setStatus();
        /*
         *  given the realm's and IDP's name, get the corresponding
         *  entry in the SAML2IDP table.
         *  remember that the IDP's name must be "colon-corrected".
         *
         *  realm name might have to be orgDN, then converted to
         *  "/"-separated form like in SsoServerAuthSvcImpl.java.
         */
        String entName = idpName;
        if (entName.indexOf(":") >= 0) {
            entName = entName.replaceAll(":", "&#58;");
        }

        entName = realm + "|" + entName;

        SsoServerSAML2IDPEntryImpl ssei = Agent.getSAML2IDPEntry(entName);

        if (ssei == null) {
            debug.error(classMethod + "no SAML2IDP entry for realm/idp: " +
                entName);
            return;
        }

        if (counter.equals(SsoServerSAML2SvcImpl.IDP_ARTIF_CACHE)) {
            ssei.incSAML2IDPArtifsInCache();
        } else if (counter.equals(SsoServerSAML2SvcImpl.IDP_ARTIF_ISSUED)) {
            ssei.incSAML2IDPArtifsIssued();
        } else if (counter.equals(SsoServerSAML2SvcImpl.IDP_ASSERT_CACHE)) {
            ssei.incSAML2IDPAssertsInCache();
        } else if (counter.equals(SsoServerSAML2SvcImpl.IDP_ASSERT_ISSUED)) {
            ssei.incSAML2IDPAssertsIssued();
        } else if (counter.equals(SsoServerSAML2SvcImpl.IDP_RQTS_RCVD)) {
            ssei.incSAML2IDPRqtsRcvd();
        } else if (counter.equals(SsoServerSAML2SvcImpl.IDP_INVAL_RQTS_RCVD)) {
            ssei.incSAML2IDPInvalRqtsRcvd();
        } else {
            debug.error(classMethod + "unknown SAML2IDP counter: " + counter);
        }
    }

    public void decIDPCounter (String realm, String idpName, String counter) {
        String classMethod = "SsoServerSAML2SvcImpl.decIDPCounter:";

        /*
         *  given the realm's and IDP's name, get the corresponding
         *  entry in the SAML2IDP table.
         *  remember that the IDP's name must be "colon-corrected".
         *
         *  realm name might have to be orgDN, then converted to
         *  "/"-separated form like in SsoServerAuthSvcImpl.java.
         */
        String entName = idpName;
        if (entName.indexOf(":") >= 0) {
            entName = entName.replaceAll(":", "&#58;");
        }

        entName = realm + "|" + entName;

        SsoServerSAML2IDPEntryImpl ssei = Agent.getSAML2IDPEntry(entName);

        if (ssei == null) {
            debug.error(classMethod + "no SAML2IDP entry for realm/idp: " +
                entName);
            return;
        }

        // only artifacts and assertions in cache counts decremented
        if (counter.equals(SsoServerSAML2SvcImpl.IDP_ARTIF_CACHE)) {
            ssei.decSAML2IDPArtifsInCache();
        } else if (counter.equals(SsoServerSAML2SvcImpl.IDP_ASSERT_CACHE)) {
            ssei.decSAML2IDPAssertsInCache();
        } else {
            debug.error(classMethod + "unknown SAML2IDP counter: " + counter);
        }
    }

    public void incSPCounter (String realm, String spName, String counter) {
        String classMethod = "SsoServerSAML2SvcImpl.incSPCounter:";

        /*
         *  given the realm's and SP's name, get the corresponding
         *  entry in the SAML2SP table.
         *  remember that the SP's name must be "colon-corrected".
         *
         *  realm name might have to be orgDN, then converted to
         *  "/"-separated form like in SsoServerAuthSvcImpl.java.
         */
        String entName = spName;
        if (entName.indexOf(":") >= 0) {
            entName = entName.replaceAll(":", "&#58;");
        }
        setStatus();

        entName = realm + "|" + entName;

        SsoServerSAML2SPEntryImpl ssei = Agent.getSAML2SPEntry(entName);

        if (ssei == null) {
            debug.error(classMethod + "no SAML2SP entry for realm/sp: " +
                entName);
            return;
        }

        if (counter.equals(SsoServerSAML2SvcImpl.SP_VAL_ASSERTS_RCVD)) {
            ssei.incSAML2SPValidAssertsRcvd();
        } else if (counter.equals(SsoServerSAML2SvcImpl.SP_RQTS_SENT)) {
            ssei.incSAML2SPRqtsSent();
        } else if (counter.equals(SsoServerSAML2SvcImpl.SP_INVAL_ARTIFS_RCVD)){
            ssei.incSAML2SPInvalArtifsRcvd();
        }
    }

    private void setStatus() {
        if (SAML2Status.equals("dormant")) {
            SAML2Status = "operational";
        }
    }
}
