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
 * $Id: FedletMonSAML2SvcProvider.java,v 1.2 2009/12/23 23:32:49 exu Exp $
 *
 */

package com.sun.identity.plugin.monitoring.impl;

import com.sun.identity.plugin.monitoring.FedMonSAML2Svc;

/**
 *  This class implements the SAML2 Monitoring
 */

public class FedletMonSAML2SvcProvider implements FedMonSAML2Svc {

    public FedletMonSAML2SvcProvider() {
    }

    public void init() {
    }

    public void incFedSessionCount() {
    }

    public void decFedSessionCount() {
    }

    public void setFedSessionCount(long count) {
    }

    public void incIdpSessionCount() {
    }

    public void decIdpSessionCount() {
    }

    public void setIdpSessionCount(long count) {
    }

    /**
     *  increment number of
     *  @param realm name of the IDP's realm
     *  @param idpName the name of the IDP
     *  @param counter the IDP counter to increment
     */
    public void incIDPCounter (String realm, String idpName, String counter) {
    }

    /**
     *  decrement number of Assertions or Artifacts in the
     *  SAML2 Caches.
     *  @param realm name of the IDP's realm
     *  @param idpName the name of the IDP
     *  @param counter the IDP counter to decrement
     */
    public void decIDPCounter (String realm, String idpName, String counter) {
    }
}
