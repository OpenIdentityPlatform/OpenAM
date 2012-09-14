/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ToolManager.java,v 1.1 2008/11/22 02:19:53 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core;

import com.sun.identity.diagnostic.base.core.log.ToolOutputManager;
import com.sun.identity.diagnostic.base.core.service.ToolServiceManager;
import com.sun.identity.diagnostic.base.core.ui.UIManager;

/**
 * This is a main controller for the whole application.
 */
public class ToolManager {
    
    public static ToolManager tManager = null;
    private ToolServiceManager sManager = null;
    private ToolContext tContext = null;
    private UIManager uiManager = null;
    private ToolOutputManager toolOutputManager = null;
    
    /** Creates a new instance of ToolManager */
    private ToolManager() {
    }
    
    /**
     * Creates a new instance of this class if one doesn't exist.
     * This is implemented as a singlelton so that there is one
     * manager for the application.
     */
    public static ToolManager getInstance() {
        if (tManager != null) {
            return tManager;
        }
        synchronized (ToolManager.class) {
            if (tManager == null) {
                tManager = new ToolManager();
            }
        }
        return tManager;
    }
    
    /**
     * This method is responsible for initializing and setting up
     * the application for start. It creates the tool service manager
     * and tool context and initalizes the UIManager.
     */
    public void initApplication() {
        sManager = ToolServiceManager.getInstance();
        tContext = new ToolContext(this, sManager);
        tContext.configure();
        toolOutputManager = ToolOutputManager.getInstance();
        toolOutputManager.init(tContext);
        sManager.init(tContext);
        sManager.publishServices();
        sManager.activateServicesOnStartup();
        uiManager = new UIManager(tContext);
        uiManager.init();
    }
    
    /**
     * This actually starts the application by delegating control to
     * the UIManager.
     */
    public void runApplication() {
        uiManager.startApplication();
    }
    
    /**
     * Returns the ToolContext that is managed by this
     * ToolManager.
     *
     * @return <code>ToolContext</code> of the application.
     */
    public ToolContext getToolContext() {
        return tContext;
    }
    
    /**
     * Returns the output writer manager that is managed by this
     * ToolManager.
     *
     * @return <code>ToolOutputManager</code> of the application.
     */
    public ToolOutputManager getToolOutputManager() {
        return toolOutputManager;
    }
}
