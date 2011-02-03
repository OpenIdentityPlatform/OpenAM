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
 * $Id: FedMonAgent.java,v 1.1 2009/06/19 02:48:04 bigfatrat Exp $
 *
 */

package com.sun.identity.plugin.monitoring;


/**
 *  This interface defines methods which will be invoked by the
 *  Federation Framework to update monitoring-related counters
 */

public interface FedMonAgent {

    /**
     * Initializes the provider.
     */
    public void init();

    /**
     *  Checks the operational status of the monitoring agent.
     * @return whether the monitoring agent is "running" (true) or not (false).
     */
    public boolean isRunning();

    /**
     * Get a handle to the monitoring MBean for the SAML1 service.
     * @return handle for the SAML1 service MBean.
     */
    public Object getSaml1SvcMBean();

    /**
     * Get a handle to the monitoring MBean for the SAML2 service.
     * @return handle for the SAML2 service MBean.
     */
    public Object getSaml2SvcMBean();

    /**
     * Get a handle to the monitoring MBean for the ID-FF service.
     * @return handle for the ID-FF service MBean.
     */
    public Object getIdffSvcMBean();

    /**
     * Get a handle to the monitoring MBean for the Federation Circles
     * Of Trust (COTs)
     * @return handle for the Federation Circles Of Trust MBean.
     */
    public Object getFedCOTsMBean();

    /**
     * Get a handle to the monitoring MBean for the Federation Entities.
     * @return handle for the Federation Entities MBean.
     */
    public Object getFedEntsMBean();
}
