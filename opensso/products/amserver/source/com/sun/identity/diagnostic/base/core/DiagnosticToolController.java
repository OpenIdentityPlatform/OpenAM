/*
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
 * $Id: DiagnosticToolController.java,v 1.1 2008/11/22 02:19:53 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core;

/**
 * This is entry point for the tool. It sets up 
 * logging and other resources and delegates the control 
 * to <code>ToolManager</code> that starts the 
 * application.
 *
 */

public class DiagnosticToolController {
    
    private static ToolManager toolManager;
    
    public DiagnosticToolController() {
    }
    
    /**
     * This is the entry point method for the tool.
     * It sets up the ToolManager and starts the application.
     */
    public static void setupServices() {
        toolManager = ToolManager.getInstance();
        toolManager.initApplication();
        toolManager.runApplication();   
    }
}
