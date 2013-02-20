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
 * $Id: FedMonSAML1SvcProvider.java,v 1.1 2009/06/19 02:43:57 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.plugin.monitoring.impl;

import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.SsoServerSAML1SvcImpl;
import com.sun.identity.plugin.monitoring.FedMonSAML1Svc;

/**
 *  This class implements the SAML1 Monitoring
 */

public class FedMonSAML1SvcProvider implements FedMonSAML1Svc {

    private static SsoServerSAML1SvcImpl sSAML1Svc;

    public FedMonSAML1SvcProvider() {
    }

    public void init() {
        sSAML1Svc = Agent.getSaml1SvcMBean();
    }

    /**
     *  increment number of reads, writes, hits, or misses
     *  for the SAML1 Assertions or Artifacts Cache.
     *  @param assertOrArtifact Whether the Assertions or Artifacts Cache
     *  @param rWHM Read, Write, Hit, or Miss entry to increment
     */
    public void incSAML1Cache(String assertOrArtifact, String rWHM) {
        if (sSAML1Svc != null) {
            sSAML1Svc.incSAML1Cache(assertOrArtifact, rWHM);
        }
    }
}
