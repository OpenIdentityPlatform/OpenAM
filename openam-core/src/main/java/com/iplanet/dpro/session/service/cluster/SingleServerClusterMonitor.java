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
 * $Id: SessionService.java,v 1.37 2010/02/03 03:52:54 bina Exp $
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */

package com.iplanet.dpro.session.service.cluster;

import com.iplanet.dpro.session.SessionID;

/**
 * No-op implementation of ClusterMonitor to be used when session fail-over is not enabled.
 *
 * @since 13.0.0
 */
public class SingleServerClusterMonitor implements ClusterMonitor {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSiteUp(String siteId) {
        throw new UnsupportedOperationException(
                "isSiteUp only applicable when session failover enabled.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reinitialize() throws Exception {
        throw new UnsupportedOperationException(
                "reinitialize only applicable when session failover enabled.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkServerUp(String serverID) {
        throw new UnsupportedOperationException(
                "checkServerUp only applicable when session failover enabled.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCurrentHostServer(SessionID sid) {
        return sid.getSessionServerID();
    }

}
