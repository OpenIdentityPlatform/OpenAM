/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RealmDiscoveryDescriptionViewBeanBase.java,v 1.2 2008/06/25 05:49:42 qcheng Exp $
 *
 */

package com.sun.identity.console.realm;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.service.SMDiscoveryDescriptionViewBeanBase;

public abstract class RealmDiscoveryDescriptionViewBeanBase
    extends SMDiscoveryDescriptionViewBeanBase
{
    public RealmDiscoveryDescriptionViewBeanBase(
	String pageName,
	String defaultDisplayURL
    ) {
	super(pageName, defaultDisplayURL);
    }

    /**
     * Handles cancel request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        RealmResourceOfferingViewBeanBase vb =
	    (RealmResourceOfferingViewBeanBase)getReturnToViewBean();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
}
