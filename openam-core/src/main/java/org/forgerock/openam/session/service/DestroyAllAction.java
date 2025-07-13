/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * Portions Copyrighted 2017-2025 3A Systems, LLC.
 */
package org.forgerock.openam.session.service;

import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.QuotaExhaustionActionImpl;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * This action will invalidate all currently existing sessions, but it will
 * let the current session to get created, so this way the user will always have
 * only one session.
 *
 * @author Steve Ferris
 */
public class DestroyAllAction extends QuotaExhaustionActionImpl {

    @Override
    public boolean action(InternalSession is, Map<String,Long> sessions) {
        for (String sid : sessions.keySet()) 
        	if (!StringUtils.equals(is.getSessionID().toString(), sid))
	        {
        		destroy(sid,sessions);
	        }
        return false;
    }
}
