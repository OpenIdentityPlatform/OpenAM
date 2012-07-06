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
 * $Id: FedMonIDFFSvcProvider.java,v 1.2 2009/08/03 18:18:38 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.plugin.monitoring.impl;

import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.SsoServerIDFFSvcImpl;
import com.sun.identity.plugin.monitoring.FedMonIDFFSvc;

/**
 *  This class implements the IDFF Service Monitoring
 */

public class FedMonIDFFSvcProvider implements FedMonIDFFSvc {

    private static SsoServerIDFFSvcImpl sIDFFSvc;

    public FedMonIDFFSvcProvider() {
    }

    public void init() {
        sIDFFSvc = Agent.getIdffSvcMBean();
    }

    public void incIdLocalSessToken() {
        if (sIDFFSvc != null) {
            sIDFFSvc.incIdLocalSessToken();
        }
    }

    public void decIdLocalSessToken() {
        if (sIDFFSvc != null) {
            sIDFFSvc.decIdLocalSessToken();
        }
    }

    public void setIdLocalSessToken(long count) {
        if (sIDFFSvc != null) {
            sIDFFSvc.setIdLocalSessToken(count);
        }
    }

    public void incIdAuthnRqt() {
        if (sIDFFSvc != null) {
            sIDFFSvc.incIdAuthnRqt();
        }
    }

    public void incUserIDSessionList() {
        if (sIDFFSvc != null) {
            sIDFFSvc.incUserIDSessionList();
        }
    }

    public void decUserIDSessionList() {
        if (sIDFFSvc != null) {
            sIDFFSvc.decUserIDSessionList();
        }
    }

    public void setUserIDSessionList(long count) {
        if (sIDFFSvc != null) {
            sIDFFSvc.setUserIDSessionList(count);
        }
    }

    public void incArtifacts() {
        if (sIDFFSvc != null) {
            sIDFFSvc.incArtifacts();
        }
    }

    public void decArtifacts() {
        if (sIDFFSvc != null) {
            sIDFFSvc.decArtifacts();
        }
    }

    public void setArtifacts(long count) {
        if (sIDFFSvc != null) {
            sIDFFSvc.setArtifacts(count);
        }
    }

    public void incAssertions() {
        if (sIDFFSvc != null) {
            sIDFFSvc.incAssertions();
        }
    }

    public void decAssertions() {
        if (sIDFFSvc != null) {
            sIDFFSvc.decAssertions();
        }
    }

    public void setAssertions(long count) {
        if (sIDFFSvc != null) {
            sIDFFSvc.setAssertions(count);
        }
    }

    public void setRelayState(long state) {
        if (sIDFFSvc != null) {
            sIDFFSvc.setRelayState(state);
        }
    }

    public void incIdDestn() {
        if (sIDFFSvc != null) {
            sIDFFSvc.incIdDestn();
        }
    }

    public void decIdDestn() {
        if (sIDFFSvc != null) {
            sIDFFSvc.decIdDestn();
        }
    }

    public void setIdDestn(long count) {
        if (sIDFFSvc != null) {
            sIDFFSvc.setIdDestn(count);
        }
    }
}
