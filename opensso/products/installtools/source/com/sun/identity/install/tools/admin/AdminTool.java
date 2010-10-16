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
 * $Id: AdminTool.java,v 1.2 2008/06/25 05:51:16 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.install.tools.admin;

import java.io.File;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Iterator;
import com.sun.identity.install.tools.launch.IAdminTool;
import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ConfigUtil;
import java.util.List;

public class AdminTool implements IAdminTool {

    public AdminTool() throws Exception {
        // See if need to register JCE/JSSE providers
        if (Boolean.getBoolean(PROP_REGISTER_JCE_PROVIDER)) {
            registerProvider(STR_JCEPROVIDER_CLASSNAME);
        }
        if (Boolean.getBoolean(PROP_REGISTER_JSSE_PROVIDER)) {
            registerProvider(STR_JSSEPROVIDER_CLASSNAME);
        }
    }

    private ToolsOptionsInfo getOptionsInfo(String option) {
        ToolsOptionsInfo result = null;
        BaseToolsResolver resolver = ToolsConfiguration.getToolsResolver();
        ArrayList options = resolver.getAllSupportedOptions();
        Iterator it = options.iterator();
        while (it.hasNext()) {
            ToolsOptionsInfo info = (ToolsOptionsInfo) it.next();
            if (info.getOption().equals(option)) {
                result = info;
                break;
            }
        }
        return result;
    }

    private boolean checkImplicitLicenseAcceptance(List args) {
        boolean result = false;
        if (args.size() > 0) {
            Iterator it = args.iterator();
            while (it.hasNext()) {
                String nextArg = (String) it.next();
                if (nextArg.equals(STR_ACCEPT_LICENSE)) {
                    result = true;
                    it.remove();
                    Debug.log("AdminTool.checkImplicitLicenseAcceptance(): "
                            + "License accepted by passing the argument: "
                            + STR_ACCEPT_LICENSE);
                    break;
                }
            }
        }
        if (result) {
            Debug.log("AdminTool.checkImplicitLicenseAcceptance(): "
                    + "License check will not be enforced.");
        } else {
            Debug.log("AdminTool.checkImplicitLicenseAcceptance(): "
                    + "License check will be enforced if applicable.");
        }
        return result;
    }

    public void run(List args) {
        int returnCode = INT_CODE_SUCCESS;
        Debug.log("AdminTool is now starting up.");
        if (acquireLock()) {
            boolean licenseRequired = !checkImplicitLicenseAcceptance(args);
            if (licenseRequired && args.size() > 0) {
                ToolsOptionsInfo info = getOptionsInfo((String) args.get(0));
                if (info != null) {
                    licenseRequired = info.isLicenseCheckRequired();
                    Debug.log("AdminTool.run(): License check for "
                            + "option: " + info.getOption() + ", required: "
                            + licenseRequired);
                }
            }
            Debug.log("AdminTool.run(): License check required: "
                    + licenseRequired);
            boolean licenseAccepted = false;
            try {
                if (licenseRequired) {
                    LicenseChecker lc = new LicenseChecker();
                    licenseAccepted = lc.checkLicenseAcceptance();
                } else {
                    Debug.log("AdminTool.run(): Skipping license check");
                    licenseAccepted = true;
                }
            } catch (Exception ex) {
                Debug.log("License check failed with exception:", ex);
            }
            if (licenseAccepted) {
                if (args.size() == 0) {
                    returnCode += INT_CODE_ERROR_NO_OPTS;
                    Debug.log("No options specified.");
                    showUsage();
                } else {
                    returnCode += dispatch(args);
                }
            } else {
                returnCode += INT_CODE_ERROR_LICENSE;
            }
        } else {
            returnCode += INT_CODE_ERROR_CONCURRENT_EXEC;
            LocalizedMessage concurrentRunError = LocalizedMessage
                    .get(MSG_CONCURRENT_RUN_ERROR);
            Console.println();
            Console.println(concurrentRunError);
            Console.println();
        }

        Debug.log("Exiting with code: " + returnCode);
        System.exit(returnCode);
    }

    private static void registerProvider(String className) throws Exception {
        boolean isRegistered = false;
        Provider[] providers = Security.getProviders();
        if (providers != null && providers.length > 0) {
            for (int i = 0; i < providers.length; i++) {
                if (providers[i].getClass().getName().equals(className)) {
                    isRegistered = true;
                    break;
                }
            }
        }
        if (!isRegistered) {
            Debug.log("Registering provider: " + className);
            Class provider = Class.forName(className);
            Security.addProvider((Provider) provider.newInstance());
        } else {
            Debug.log("Provider: " + className + " is already registered");
        }
    }

    private int dispatch(List filteredArgs) {
        int returnCode = INT_CODE_SUCCESS;
        String optionArg = (String) filteredArgs.remove(0);
        Debug.log("Trying to dispatch to option handler for: " + optionArg);
        BaseToolsResolver resolver = ToolsConfiguration.getToolsResolver();
        ArrayList options = resolver.getAllSupportedOptions();

        if (optionArg.equals(STR_USAGE_OPTION)) {
            showUsage();
        } else if (optionArg.equals(STR_HELP_OPTION)) {
            Iterator it = options.iterator();
            while (it.hasNext()) {
                Console.println();
                ToolsOptionsInfo info = (ToolsOptionsInfo) it.next();
                Console.println(info.getDescription());
                String handlerClass = info.getHandlerClass();
                try {
                    IToolsOptionHandler handler = (IToolsOptionHandler) Class
                            .forName(handlerClass).newInstance();
                    handler.displayHelp();
                } catch (Exception ex) {
                    returnCode += INT_CODE_ERROR_UNKNOWN;
                    Debug.log("Exception caught during help dispatch:", ex);
                    LocalizedMessage unknownError = LocalizedMessage
                            .get(MSG_UNKNOWN_ERROR);
                    Console.println();
                    Console.println(unknownError, new Object[] { ex
                            .getMessage() });
                    Console.println();
                    break;
                }
            }
        } else {
            String handlerClass = null;
            Iterator it = options.iterator();
            boolean optionValid = false;
            while (it.hasNext()) {
                ToolsOptionsInfo info = (ToolsOptionsInfo) it.next();
                if (info.getOption().equals(optionArg)) {
                    handlerClass = info.getHandlerClass();
                    optionValid = true;
                    break;
                }
            }

            if (!optionValid) {
                returnCode += INT_CODE_ERROR_INVALID_OPTS;
                Debug.log("No handler found for option: " + optionArg);
                LocalizedMessage errorInvalidOption = LocalizedMessage
                        .get(MSG_INVALID_OPTION);
                Console.println();
                Console.println(errorInvalidOption, new Object[] { 
                        optionArg });
                Console.println();
            } else {
                try {
                    IToolsOptionHandler handler = (IToolsOptionHandler) Class
                            .forName(handlerClass).newInstance();
                    if (handler.checkArguments(filteredArgs)) {
                        handler.handleRequest(filteredArgs);
                    } else { // Invalid arguments specified. Display help!
                        handler.displayHelp();
                    }
                } catch (Exception ex) {
                    returnCode += INT_CODE_ERROR_UNKNOWN;
                    Debug.log("Exception caught during option dispatch", ex);
                    LocalizedMessage unknownError = LocalizedMessage
                            .get(MSG_UNKNOWN_ERROR);
                    Console.println();
                    Console.println(unknownError, new Object[] { ex
                            .getMessage() });
                    Console.println();
                }
            }
        }

        return returnCode;
    }

    private void showUsage() {
        LocalizedMessage usageMessage = LocalizedMessage.get(MSG_USAGE);
        LocalizedMessage availableOpts = LocalizedMessage.get(MSG_AVAIL_OPTS);
        Console.println();
        Console.println(usageMessage);
        Console.println();
        Console.println(availableOpts);
        BaseToolsResolver resolver = ToolsConfiguration.getToolsResolver();
        ArrayList options = resolver.getAllSupportedOptions();
        Iterator it = options.iterator();
        while (it.hasNext()) {
            ToolsOptionsInfo info = (ToolsOptionsInfo) it.next();
            Console.println(info.getDescription());
        }
        Console.println(LocalizedMessage.get(MSG_OPTION_DESC_USAGE));
        Console.println(LocalizedMessage.get(MSG_OPTION_DESC_HELP));
        Console.println();
    }

    private boolean acquireLock() {
        boolean result = false;
        Debug.log("Trying to acquire lock.");
        File lockFile = getLockFile();
        if (!lockFile.exists()) {
            // If the lock file does not exist, try to create it now.
            try {
                // lockFile.createNewFile() will return true only if the lock
                // file gets greated. If it returns false, it indicates that
                // potentially another instance of admintool created the file
                // before this instance could. Other problems such as writer
                // permissions etc could also break this logic.
                result = lockFile.createNewFile();
                if (result) {
                    // If the lock file was successfully created, then mark it
                    // for deletion on exit. That will automatically release
                    // the lock when this intance of admintool exits.
                    lockFile.deleteOnExit();
                }
            } catch (Exception ex) {
                Debug.log("Failed to create lock file", ex);
                result = false;
            }
        }

        Debug.log("Lock acquired = " + result);
        return result;
    }

    private File getLockFile() {
        return new File(ConfigUtil.getLogsDirPath() + "/" + STR_LOCK_FILENAME);
    }

    public static final String MSG_USAGE = "usage";

    public static final String MSG_AVAIL_OPTS = "available_options";

    public static final String MSG_INVALID_OPTION = "invalid_option";

    public static final String MSG_UNKNOWN_ERROR = "error_unknown";

    public static final String MSG_CONCURRENT_RUN_ERROR = 
        "concurrent_run_error";

    public static final String MSG_OPTION_DESC_USAGE = 
        "AA_MSG_OPTION_DESC_USAGE";

    public static final String MSG_OPTION_DESC_HELP = 
        "AA_MSG_OPTION_DESC_HELP";

    public static final String STR_LOCK_FILENAME = 
        ToolsConfiguration.getProductShortName() + "admin.lock";

    public static final int INT_CODE_SUCCESS = 0x0;

    public static final int INT_CODE_ERROR_NO_OPTS = 0x1;

    public static final int INT_CODE_ERROR_INVALID_OPTS = 0x2;

    public static final int INT_CODE_ERROR_CONCURRENT_EXEC = 0x4;

    public static final int INT_CODE_ERROR_UNKNOWN = 0x8;

    public static final int INT_CODE_ERROR_LICENSE = 0x10;

    public static final String STR_HELP_OPTION = "--help";

    public static final String STR_USAGE_OPTION = "--usage";

    public static final String STR_ACCEPT_LICENSE = "--acceptLicense";

    private static final String STR_JCEPROVIDER_CLASSNAME = 
        "com.sun.crypto.provider.SunJCE";

    private static final String STR_JSSEPROVIDER_CLASSNAME = 
        "com.sun.net.ssl.internal.ssl.Provider";

    static {
        ToolsConfiguration.initialize();
    }
}
