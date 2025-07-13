/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt See the License for the specific language
 * governing permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at opensso/legal/CDDLv1.0.txt. If applicable,
 * add the following below the CDDL Header, with the fields enclosed by brackets
 * [] replaced by your own identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 * Portions Copyrighted 2017-2025 3A Systems, LLC.
 */
package org.forgerock.openam.session.service;

import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.QuotaExhaustionActionImpl;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class DestroyNextExpiringAction extends QuotaExhaustionActionImpl {

    @Override
    public boolean action(InternalSession is, Map<String, Long> sessions) {
        String nextExpiringSessionID = null;
        long smallExpTime = Long.MAX_VALUE;
        for (Map.Entry<String, Long> entry : sessions.entrySet()) 
    		if (!StringUtils.equals(is.getSessionID().toString(), entry.getKey())){
	            if (entry.getValue() < smallExpTime || nextExpiringSessionID==null) {
            		nextExpiringSessionID = entry.getKey();
            		smallExpTime = entry.getValue();
	            }
	        }
        if (nextExpiringSessionID != null) { 
        	destroy(nextExpiringSessionID,sessions);
        }
        return false;
    }
}
