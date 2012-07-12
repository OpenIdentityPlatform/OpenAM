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
 * $Id: AuthConfigTest.java,v 1.3 2008/06/25 05:44:16 qcheng Exp $
 *
 */

package com.sun.identity.cli.authentication;

import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.cli.CLIConstants;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIRequest;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.DevNullOutputWriter;
import com.sun.identity.cli.IArgument;
import com.sun.identity.test.common.TestBase;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.annotations.AfterGroups;


/**
 * This is to test the authentication configuration sub commands.
 */
public class AuthConfigTest extends TestBase{
    private CommandManager cmdManager;
    private static DevNullOutputWriter outputWriter = new DevNullOutputWriter();
    private static final String TEST_AUTH_INSTANCE = "clitestauthinstance";
    private static final String TEST_AUTH_CONFIG = "clitestauthconfig";
    private static final String TEST_AUTH_TYPE = "DataStore";

    /**
     * Creates a new instance of <code>AuthConfigTest</code>
     */
    public AuthConfigTest() {
        super("CLI");
    }
    
    /**
     * Create the CLIManager.
     */
    @BeforeTest(groups = {"cli-authconfig"})
    public void suiteSetup()
        throws CLIException {
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "amadm");
        env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
            "com.sun.identity.cli.AccessManager");
        env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
        cmdManager = new CommandManager(env);
    }
    
    
    @Parameters ({"realm"})
    @Test(groups = {"cli-authconfig", "ops", "list-auth-instances"})
    public void listAuthInstances(String realm)
        throws CLIException {
        String[] param = {realm};
        entering("listAuthInstances", param);
        String[] args = {
            "list-auth-instances",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("listAuthInstances");
    }

    @Parameters ({"realm"})
    @Test(groups = {"cli-authconfig", "ops", "create-auth-instance"})
    public void createAuthInstance(String realm)
        throws CLIException, AMConfigurationException {
        String[] param = {realm};
        entering("createAuthInstance", param);
        String[] args = {
            "create-auth-instance",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + AuthOptions.AUTH_INSTANCE_NAME,
            TEST_AUTH_INSTANCE,
            CLIConstants.PREFIX_ARGUMENT_LONG + AuthOptions.AUTH_INSTANCE_TYPE,
            TEST_AUTH_TYPE
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("createAuthInstance");
    }
    
    @Parameters ({"realm"})
    @Test(groups = {"cli-authconfig", "ops", "list-auth-cfgs"})
    public void listAuthConfigurations(String realm)
        throws CLIException, AMConfigurationException {
        String[] param = {realm};
        entering("listAuthConfigurations", param);
        String[] args = {
            "list-auth-cfgs",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("listAuthConfigurations");
    }
    
    @Parameters ({"realm"})
    @Test(groups = {"cli-authconfig", "ops", "create-auth-cfg"},
            dependsOnMethods = {"createAuthInstance"})
    public void createAuthConfiguration(String realm)
        throws CLIException, AMConfigurationException {
        String[] param = {realm};
        entering("createAuthConfiguration", param);
        String[] args = {
            "create-auth-cfg",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + AuthOptions.AUTH_CONFIG_NAME,
            TEST_AUTH_CONFIG
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("createAuthConfiguration");
    }

/*
 * The following test will fail under flat file configuration datastore
 * because there is not notification in place. Auth configuration and instance
 * cannot be returned by the AMAuthConfigurationManager after they are created.
 * Works OK with Sun DS as configuration datastore.
 
    @Parameters ({"realm"})
    @Test(groups = {"cli-authconfig", "ops", "update-auth-instance"},
        dependsOnMethods = {"createAuthInstance"})
    public void updateAuthInstance(String realm)
        throws CLIException, AMConfigurationException {
        String[] param = {realm};
        entering("updateAuthInstance", param);
        String[] args = {
            "update-auth-instance",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + AuthOptions.AUTH_INSTANCE_NAME,
            TEST_AUTH_INSTANCE,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_VALUES,
            "sunAMAuthDataStoreAuthLevel=1"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("updateAuthInstance");
    }
    
    @Parameters ({"realm"})
    @Test(groups = {"cli-authconfig", "ops", "get-auth-instance"},
        dependsOnMethods = {"createAuthInstance"})
    public void getAuthInstance(String realm)
        throws CLIException, AMConfigurationException {
        String[] param = {realm};
        entering("getAuthInstance", param);
        String[] args = {
            "get-auth-instance",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + AuthOptions.AUTH_INSTANCE_NAME,
            TEST_AUTH_INSTANCE
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getAuthInstance");
    }

    @Parameters ({"realm"})
    @Test(groups = {"cli-authconfig", "ops", "get-auth-cfg-entr"},
        dependsOnMethods = {"createAuthConfiguration"})
    public void getAuthConfigurationEntries(String realm)
        throws CLIException, AMConfigurationException {
       String[] param = {realm};
        entering("getAuthConfigurationEntries", param);
        String[] args = {
            "get-auth-cfg-entr",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + AuthOptions.AUTH_CONFIG_NAME,
            TEST_AUTH_CONFIG
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getAuthConfigurationEntries");
    }

    @Parameters ({"realm"})
    @Test(groups = {"cli-authconfig", "ops",
        "update-auth-cfg-entr"},
        dependsOnMethods = {"createAuthConfiguration"})
    public void updateAuthConfigurationEntries(String realm)
        throws CLIException, AMConfigurationException {
       String[] param = {realm};
        entering("updateAuthConfigurationEntries", param);
        String[] args = {
            "update-auth-cfg-entr",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + AuthOptions.AUTH_CONFIG_NAME,
            TEST_AUTH_CONFIG,
            CLIConstants.PREFIX_ARGUMENT_LONG + AuthOptions.AUTH_CONFIG_ENTRIES,
            TEST_AUTH_INSTANCE + "|REQUIRED|"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("updateAuthConfigurationEntries");
    }
    
    @Parameters ({"realm"})
    @Test(groups = {"cli-authconfig", "delete-auth-instances"}, alwaysRun=true, 
        dependsOnGroups = {"ops"}, 
        dependsOnMethods = {"deleteAuthConfiguration"})
    public void deleteAuthInstance(String realm)
        throws CLIException, AMConfigurationException {
        String[] param = {realm};
        entering("deleteAuthInstance", param);
        String[] args = {
            "delete-auth-instances",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                AuthOptions.AUTH_INSTANCE_NAMES,
            TEST_AUTH_INSTANCE
        };
        
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("deleteAuthInstance");
    }
    
    @Parameters ({"realm"})
    @Test(groups = {"cli-authconfig", "delete-auth-cfgs"}, alwaysRun=true,
        dependsOnGroups = {"ops"})
    public void deleteAuthConfiguration(String realm)
        throws CLIException, AMConfigurationException {
        String[] param = {realm};
        entering("deleteAuthConfiguration", param);
        String[] args = {
            "delete-auth-cfgs",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                AuthOptions.AUTH_CONFIG_NAMES,
            TEST_AUTH_CONFIG
        };
        
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("deleteAuthConfiguration");
    }
    */
}
