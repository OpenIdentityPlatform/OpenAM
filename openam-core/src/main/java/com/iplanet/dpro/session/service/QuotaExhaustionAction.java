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
 * $Id: QuotaExhaustionAction.java,v 1.2 2008/06/25 05:41:30 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2011-2013 ForgeRock, Inc.
 */
package com.iplanet.dpro.session.service;

import java.util.Map;

/**
 * Interface to define the resulting behavior when the session quota is exhausted.
 *
 * @supported.all.api
 */
public interface QuotaExhaustionAction {

    /**
     * Performs an action, when the session quota is exhausted. The action implementation should destroy at least one
     * session (either by destroying an old session, or rejecting the new one) in order to adhere the session quota.
     *
     * @param is the to-be-actived InternalSession
     * @param existingSessions all existing sessions belonging to the same uuid (Map:sid-&gt;expiration_time)
     * @return <code>true</code> if the session activation request should be rejected, <code>false</code> otherwise
     */
    public boolean action(InternalSession is, Map<String, Long> existingSessions);
}
