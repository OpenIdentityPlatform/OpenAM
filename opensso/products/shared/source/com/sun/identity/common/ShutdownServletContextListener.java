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
 * $Id: ShutdownServletContextListener.java,v 1.3 2008/08/08 00:40:59 ww203982 Exp $
 *
 */

package com.sun.identity.common;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * The <code>ShutdownServletContextListener</code> class is used to trigger
 * ShutdownManager to shutdown everything when it is undeployed, or the web
 * container is terminated.
 */

public class ShutdownServletContextListener implements ServletContextListener
{
    public void contextInitialized(ServletContextEvent sce) {
    }

    public void contextDestroyed(ServletContextEvent sce) {
        ShutdownManager shutdownMan = ShutdownManager.getInstance();
        if (shutdownMan.acquireValidLock()) {
            try {
                shutdownMan.shutdown();
            } finally {
                shutdownMan.releaseLockAndNotify();
            }
        }
    }
}
