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
 * $Id: UIManager.java,v 1.1 2008/11/22 02:19:54 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.ui;

import com.sun.identity.diagnostic.base.core.ToolContext;
import com.sun.identity.diagnostic.base.core.common.ToolConstants;
import com.sun.identity.diagnostic.base.core.ui.cli.CLIManager;
import com.sun.identity.diagnostic.base.core.ui.gui.GUIManager;

/**
 * This clas represents the base class for generic UI workflow.
 */

public class UIManager {
    private ToolContext tContext;
    private UIManager uiManager;
    
    public UIManager() {
    }
    
    /**
     * Creates a new instance of UIManager
     *
     * @param tContext ToolContext of the tool
     */
    public UIManager(ToolContext tContext) {
        this.tContext = tContext;
    }
    
    /**
     * Instatiates the actual <code>UIManager</code> based on the
     * application run mode.
     */
    public void init() {
        if (tContext.getMode().equals(ToolConstants.GUI_MODE)) {
            uiManager = new GUIManager();
        } else if (tContext.getMode().equals(ToolConstants.CLI_MODE)) {
            uiManager = new CLIManager();
        } else {
            uiManager = new CLIManager();
        }
        uiManager.init();
    }
    
    /**
     * Delegates control to the actual UIManager to bring up the
     * application.
     */
    public void startApplication() {
        uiManager.startApplication();
    }
    
    public void stopApplication() {
        uiManager.stopApplication();
    }
}
