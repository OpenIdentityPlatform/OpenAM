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
 * $Id: CLIManager.java,v 1.1 2008/11/22 02:19:55 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.ui.cli;

import java.util.ResourceBundle;
import com.sun.identity.diagnostic.base.core.ui.UIManager;

/**
 * This class is responsible for taking care of UI workflow in
 * CLI mode.
 */
public class CLIManager extends UIManager implements CLIConstants {
    
    private ResourceBundle rb;
    
    /** Creates a new instance of CLIManager */
    public CLIManager() {
    }
    
    /**
     * This method is called once during application start up for CLI mode.
     * It performs any CLI specific UI initialization.
     */
    public void init() {
        rb = ResourceBundle.getBundle(CLI_RESOURCE_BUNDLE);
    }
    
    /**
     * This method is responsible to bring up the tool
     * application in CLI mode.
     */
    public void startApplication() {
        try {
            CLIHandler.processCLI(rb);
        } catch (Exception e) {
            System.out.println(rb.getString("cli-error-occured"));
            System.out.println(rb.getString("cli-exit-message"));
            System.exit(1);
        }
    }
   
    /**
     * This method is called during application shutdown in CLI mode.
     */
    public void stopApplication() {
    }
}
