/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
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
 *
 */
package org.forgerock.agents.appserver.v81;

import org.forgerock.openam.agents.common.CommonLifeCycleListener;

import com.sun.appserv.server.LifecycleEvent;
import com.sun.appserv.server.LifecycleListener;
import com.sun.appserv.server.ServerLifecycleException;

/**
 *
 * @author Laurence Noton
 */
public class ASLifeCycleListener implements LifecycleListener {

	@Override
	public void handleEvent(LifecycleEvent le)
			throws ServerLifecycleException {
		int eventType = le.getEventType();
        if (eventType == LifecycleEvent.STARTUP_EVENT) {
            CommonLifeCycleListener.startup();
        } else if (eventType == LifecycleEvent.SHUTDOWN_EVENT) {
            CommonLifeCycleListener.shutdown();
        }
		
	}
}
