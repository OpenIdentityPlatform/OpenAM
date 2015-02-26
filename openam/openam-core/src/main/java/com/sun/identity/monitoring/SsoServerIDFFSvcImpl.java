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
 * $Id: SsoServerIDFFSvcImpl.java,v 1.3 2009/10/21 00:02:10 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.monitoring;

import com.sun.management.snmp.agent.SnmpMib;
import javax.management.MBeanServer;

/**
 * This class extends the "SsoServerIDFFSvc" class.
 */
public class SsoServerIDFFSvcImpl extends SsoServerIDFFSvc {

    /**
     * Constructors
     */
    public SsoServerIDFFSvcImpl(SnmpMib myMib) {
        super(myMib);
        init(myMib, null);
    }

    public SsoServerIDFFSvcImpl(SnmpMib myMib, MBeanServer server) {
        super(myMib, server);
        init(myMib, server);
    }

    private void init(SnmpMib myMib, MBeanServer server) {
    }

    public void incIdLocalSessToken() {
        setStatus();
        long li = IDFFIdLocalSessToken.longValue();
        li++;
        IDFFIdLocalSessToken = Long.valueOf(li);
    }

    public void decIdLocalSessToken() {
        long li = IDFFIdLocalSessToken.longValue();
        li--;
        IDFFIdLocalSessToken = Long.valueOf(li);
    }

    public void setIdLocalSessToken(long count) {
        setStatus();
        IDFFIdLocalSessToken = Long.valueOf(count);
    }

    public void incIdAuthnRqt() {
        setStatus();
        long li = IDFFIdAuthnRqt.longValue();
        li++;
        IDFFIdAuthnRqt = Long.valueOf(li);
    }

    public void incUserIDSessionList() {
        setStatus();
        long li = IDFFUserIDSessionList.longValue();
        li++;
        IDFFUserIDSessionList = Long.valueOf(li);
    }

    public void decUserIDSessionList() {
        long li = IDFFUserIDSessionList.longValue();
        li--;
        IDFFUserIDSessionList = Long.valueOf(li);
    }

    public void setUserIDSessionList(long count) {
        setStatus();
        IDFFUserIDSessionList = Long.valueOf(count);
    }

    public void incArtifacts() {
        setStatus();
        long li = IDFFArtifacts.longValue();
        li++;
        IDFFArtifacts = Long.valueOf(li);
    }

    public void decArtifacts() {
        long li = IDFFArtifacts.longValue();
        li--;
        IDFFArtifacts = Long.valueOf(li);
    }

    public void setArtifacts(long count) {
        setStatus();
        IDFFArtifacts = Long.valueOf(count);
    }

    public void incAssertions() {
        setStatus();
        long li = IDFFAssertions.longValue();
        li++;
        IDFFAssertions = Long.valueOf(li);
    }

    public void decAssertions() {
        long li = IDFFAssertions.longValue();
        li--;
        IDFFAssertions = Long.valueOf(li);
    }

    public void setAssertions(long count) {
        setStatus();
        IDFFAssertions = Long.valueOf(count);
    }

    public void setRelayState(long state) {
        // might need to change this attribute's type
        setStatus();
        IDFFRelayState = Long.valueOf(state);
    }

    public void incIdDestn() {
        // is this a counter?
        setStatus();
        long li = IDFFIdDestn.longValue();
        li++;
        IDFFIdDestn = Long.valueOf(li);
    }

    public void decIdDestn() {
        long li = IDFFIdDestn.longValue();
        li--;
        IDFFIdDestn = Long.valueOf(li);
    }

    public void setIdDestn(long count) {
        setStatus();
        IDFFIdDestn = Long.valueOf(count);
    }

    private void setStatus() {
        if (IDFFStatus.equals("dormant")) {
            IDFFStatus = "operational";
        }
    }
}
