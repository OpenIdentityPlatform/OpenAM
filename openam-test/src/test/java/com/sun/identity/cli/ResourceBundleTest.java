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
 * $Id: ResourceBundleTest.java,v 1.5 2008/06/25 05:44:16 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.cli;

import com.iplanet.sso.SSOException;
import com.sun.identity.common.ISResourceBundle;
import com.sun.identity.test.common.TestBase;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This is to test the resource bundle sub command.
 */
public class ResourceBundleTest extends TestBase{
    private CommandManager cmdManager;
    private static DevNullOutputWriter outputWriter = new DevNullOutputWriter();

    /**
     * Creates a new instance of <code>CLIFrameworkTest</code>
     */
    public ResourceBundleTest() {
        super("CLI");
    }
    
    /**
     * Create the CLIManager.
     */
    @BeforeTest(groups = {"cli-resource-bundle"})
    public void suiteSetup()
        throws CLIException
    {
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "testclifw");
        env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
            "com.sun.identity.cli.AccessManager");
        env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
        cmdManager = new CommandManager(env);
    }
    
    @Parameters ({"bundle-name", "bundle-file-name", "locale"})
    @Test(groups = {"cli-resource-bundle", "add-res-bundle"})
    public void addResourceBundle(
        String bundleName, 
        String fileName, 
        String locale
    ) throws CLIException, SSOException {
        entering("addResourceBundle", null);
        String[] args = (locale.length() == 0) ? new String[5] : new String[7];
        
        args[0] = "add-res-bundle";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + 
            IArgument.RESOURCE_BUNDLE_NAME;
        args[2] = bundleName;
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG + 
            AddResourceBundle.ARGUMENT_RESOURCE_BUNDLE_FILE_NAME;
        args[4] = fileName;
        
        if (locale.length() > 0) {
            args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.RESOURCE_BUNDLE_LOCALE;
            args[6] = locale;
        }
        
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        
        ResourceBundle res = (locale.length() == 0) ?
            ISResourceBundle.getResourceBundle(getAdminSSOToken(), bundleName, 
                (String)null) :
            ISResourceBundle.getResourceBundle(getAdminSSOToken(), bundleName, 
                locale);
        assert (res != null);
        exiting("addResourceBundle");
    }

    @Parameters ({"bundle-name", "locale"})
    @Test(groups = {"cli-resource-bundle", "list-res-bundle"},
        dependsOnMethods = {"addResourceBundle"})
    public void getResourceBundle(String bundleName, String locale) 
        throws CLIException {
        entering("getResourceBundle", null);
        String[] args = (locale.length() == 0) ? new String[3] : new String[5];
        
        args[0] = "list-res-bundle";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + 
            IArgument.RESOURCE_BUNDLE_NAME;
        args[2] = bundleName;
        
        if (locale.length() > 0) {
            args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.RESOURCE_BUNDLE_LOCALE;
            args[4] = locale;
        }

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getResourceBundle");
    }

    @Parameters ({"bundle-name", "locale"})
    @Test(groups = {"cli-resource-bundle", "remove-res-bundle"}, 
        dependsOnMethods = {"getResourceBundle"})
    public void removeResourceBundle(String bundleName, String locale) 
        throws CLIException, SSOException {
        entering("removeResourceBundle", null);
        String[] args = (locale.length() == 0) ? new String[3] : new String[5];
        
        args[0] = "remove-res-bundle";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG + 
            IArgument.RESOURCE_BUNDLE_NAME;
        args[2] = bundleName;
        
        if (locale.length() > 0) {
            args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
                IArgument.RESOURCE_BUNDLE_LOCALE;
            args[4] = locale;
        }
        
        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        try {
            ResourceBundle res = (locale.length() == 0) ?
                ISResourceBundle.getResourceBundle(getAdminSSOToken(), 
                    bundleName, (String)null) :
                ISResourceBundle.getResourceBundle(getAdminSSOToken(), 
                    bundleName, locale);
            assert (res.getLocale() == null);
        } catch (MissingResourceException e) {
            //Ignored
        }
        if (!locale.isEmpty()) {
            //clean up the default resourcebundle too, after removing the JP version
            removeResourceBundle(bundleName, "");
        }
        exiting("removeResourceBundle");
    }
}
