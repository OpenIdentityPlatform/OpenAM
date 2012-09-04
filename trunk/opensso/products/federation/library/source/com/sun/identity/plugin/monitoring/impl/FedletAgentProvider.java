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
 * $Id: FedletAgentProvider.java,v 1.2 2009/12/23 23:32:49 exu Exp $
 *
 */

package com.sun.identity.plugin.monitoring.impl;

import com.sun.identity.plugin.monitoring.FedMonAgent;

/**
 *  This class is the AM implementation of the Monitoring interface
 */

public class FedletAgentProvider implements FedMonAgent
{

    public FedletAgentProvider() {
    }

    public void init() {
    }

    /**
     *  Returns whether agent is "running" or not
     */
    public boolean isRunning() {
        return false;
    }

    /*
     *  Returns the pointer to the SAML1 service mbean
     */
    public Object getSaml1SvcMBean() {
        return null;
    }

    /*
     *  Returns the pointer to the SAML2 service mbean
     */
    public Object getSaml2SvcMBean() {
        return null;
    }

    /*
     *  Returns the pointer to the IDFF service mbean
     */
    public Object getIdffSvcMBean() {
        return null;
    }

    /*
     *  Returns the pointer to the Fed COTs mbean
     */
    public Object getFedCOTsMBean() {
        return null;
    }

    /*
     *  Returns the pointer to the Federation Entities mbean
     */
    public Object getFedEntsMBean() {
        return null;
    }
}

