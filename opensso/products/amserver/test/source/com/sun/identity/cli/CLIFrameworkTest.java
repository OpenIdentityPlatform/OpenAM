/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CLIFrameworkTest.java,v 1.2 2008/06/25 05:44:15 qcheng Exp $
 *
 */

package com.sun.identity.cli;

import com.sun.identity.test.common.TestBase;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * This is test to test the CLI Framework on parsing arguments;
 * checking for mandatory argument; etc.
 */
public class CLIFrameworkTest extends TestBase{
    private CommandManager cmdManager;
    private static DevNullOutputWriter outputWriter = new DevNullOutputWriter();

    /**
     * Creates a new instance of <code>CLIFrameworkTest</code>
     */
    public CLIFrameworkTest() {
        super("CLI");
    }
    
    /**
     * Create the CLIManager.
     *
     */
    @BeforeSuite(groups = {"cli-framework"})
    public void suiteSetup()
        throws CLIException
    {
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "testclifw");
        env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
            "com.sun.identity.cli.MockCLIManager");
        env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
        cmdManager = new CommandManager(env);
    }
    
    @Test(groups = {"cli-framework"}, expectedExceptions = {CLIException.class})
    public void invalidSubCommand() 
        throws CLIException {
        entering("invalidSubCommand", null);
        String[] args = {"ba-ba-black-sheep"};
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("invalidSubCommand");
    }

    @Test(groups = {"cli-framework"}, expectedExceptions = {CLIException.class})
    public void withoutMandatoryOption() 
        throws CLIException {
        entering("withoutMandatoryOption", null);
        String[] args = {"test-command",
            CLIConstants.PREFIX_ARGUMENT_LONG + "optional",
            "test"};
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("withoutMandatoryOption");
    }
    
    @Test(groups = {"cli-framework"}, expectedExceptions = {CLIException.class})
    public void withInvalidOption() 
        throws CLIException {
        entering("withInvalidOption", null);
        String[] args = {"test-command",
            CLIConstants.PREFIX_ARGUMENT_LONG + "notdefined",
            "test"};
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("withInvalidOption");
    }
    
    @Test(groups = {"cli-framework"}, expectedExceptions = {CLIException.class})
    public void withoutOptions() 
        throws CLIException {
        entering("withoutOptions", null);
        String[] args = {"test-command"};
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("withoutOptions");
    }
    
    @Test(groups = {"cli-framework"})
    public void withMandatoryOption() 
        throws CLIException {
        entering("withMandatoryOption", null);
        String[] args = {"test-command",
            CLIConstants.PREFIX_ARGUMENT_LONG + "mandatory",
            "test"};
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("withMandatoryOption");
    }

    @Test(groups = {"cli-framework"})
    public void matchOptionValues() 
        throws CLIException {
        entering("matchOptionValues", null);
        String[] args = {"test-command",
            CLIConstants.PREFIX_ARGUMENT_LONG + "mandatory",
            "mandatory",
            CLIConstants.PREFIX_ARGUMENT_LONG + "optional",
            "optional",
            CLIConstants.PREFIX_ARGUMENT_LONG + "testmatch"};
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("matchOptionValues");
    }
}
