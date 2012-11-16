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
 * $Id: CommandManager.java,v 1.37 2010/01/28 00:47:10 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */

package com.sun.identity.cli;


import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.log.Logger;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.setup.Bootstrap;
import com.sun.identity.setup.ConfiguratorException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.tools.bundles.VersionCheck;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This is the "engine" that drives the CLI. This is a singleton class.
 */
public class CommandManager {
    private final static String IMPORT_SVC_CMD = "import-svc-cfg";
    private final static String RESOURCE_BUNDLE_NAME = "cliBase";
    public static ResourceBundle resourceBundle;
    private static Debug debugger;        
    private ResourceBundle rbMessages;
    private Map environment;
    private String commandName;
    private String logName;
    private FileOutputStream statusOS;
    private IOutput outputWriter;
    private List definitionObjects;
    private List requestQueue = new Vector();
    private boolean bContinue;
    public static InitializeSystem initSys;
    private Set ssoTokens = new HashSet();
    private static boolean importSvcCmd;

    static {
        resourceBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);
    }
    
    /**
     * Entry point to the engine.
     */
    public static void main(String[] argv) {
        boolean bBootstrapped = true;
        importSvcCmd =(argv.length > 0) && argv[0].equals(IMPORT_SVC_CMD);
        if (importSvcCmd) {
            try {
                initSys = new InitializeSystem();
            } catch (FileNotFoundException ex) {
                System.err.println("Cannot bootstrap the system" +
                    ex.getMessage());
                System.exit(1);
            } catch (IOException ex) {
                System.err.println("Cannot bootstrap the system" +
                    ex.getMessage());
            } catch (LDAPServiceException ex) {
                System.err.println("Cannot bootstrap the system" +
                    ex.getMessage());
            }
        } else {
            try {
                Bootstrap.load();
                // Initialize AdminTokenAction
                AdminTokenAction.getInstance().authenticationInitialized();
		System.setProperty("java.util.logging.config.class",
		    "com.sun.identity.log.s1is.LogConfigReader");
		System.setProperty("java.util.logging.manager",
		    "com.sun.identity.log.LogManager");
            } catch (ConfiguratorException ex) {
                bBootstrapped = false;
                if ((argv.length > 0) &&
                    !argv[0].equals(CLIConstants.PREFIX_ARGUMENT_LONG +
                    CLIConstants.ARGUMENT_VERSION) && 
                    !argv[0].equals(CLIConstants.PREFIX_ARGUMENT_SHORT +
                    CLIConstants.SHORT_ARGUMENT_VERSION)
                ) {
                    System.err.println(ex.getL10NMessage(Locale.getDefault()));
                    System.exit(1);
                }
            } catch (Exception e) {
                System.err.println("Cannot bootstrap the system" +
                    e.getMessage());
                System.exit(1);
            }

            if (bBootstrapped) {
                if (VersionCheck.isVersionValid() == 1) {
                    System.exit(1);
                }
            }
        }
        if (bBootstrapped) {
            debugger = Debug.getInstance("amCLI");
            getIsInstallTime();
            Crypt.checkCaller();
        }
        new CommandManager(argv);
    }

    /**
     * Constructs a command line manager.
     *
     * @param env Map of option name to values.
     */
    public CommandManager(Map env)
        throws CLIException {
        init(env);
    }

    /**
     * Constructs a command line manager.
     *
     * @param argv Options from the command shell.
     */
    public CommandManager(String[] argv) {
        int exitCode = 0;
        try {
            init(argv);
            requestQueue.add(new CLIRequest(null, argv));
            serviceRequestQueue();
        } catch (CLIException e) {
            // cannot print debugger for import service configuration 
            // sub command before CLI is not bootstrapped.
            if (!importSvcCmd) {
                Debugger.error(this, "CommandManager.<init>", e);
            }
            String remainReq = null;
            if (!requestQueue.isEmpty()) {
                String[] arg = {Integer.toString(requestQueue.size())};
                remainReq = MessageFormat.format(rbMessages.getString(
                    "remaining-unprocessed-requests"), (Object[])arg);
            }
            String msg = e.getL10NMessage(getLocale());

            if (outputWriter != null) {
                outputWriter.printlnError(msg);
                if (remainReq != null) {
                    outputWriter.printlnError(remainReq);
                }
            } else {
                System.out.println(msg);
                if (remainReq != null) {
                    System.out.println(remainReq);
                }
            }

            printUsageOnException(e);
            exitCode = e.getExitCode();
        } finally {
            destroySSOTokens();
            ShutdownManager shutdownMan = ShutdownManager.getInstance();
            if (shutdownMan.acquireValidLock()) {
                try {
                    shutdownMan.shutdown();
                } finally {
                    shutdownMan.releaseLockAndNotify();
                }
            }
        }
        System.exit(exitCode);
    }

    private void printUsageOnException(CLIException e) {
        int exitCode = e.getExitCode();

        try {
            if (exitCode == ExitCodes.INCORRECT_OPTION) {
                String scmd = e.getSubcommandName();
                if (scmd != null) {
                    SubCommand cmd = getSubCommand(scmd);

                    if (cmd != null) {
                        UsageFormatter.getInstance().format(this, cmd);
                    }
                }
            }
        } catch (CLIException ex) {
            debugger.error("CommandManager.printUsageOnException", ex);
        }
    }


    private void init(Map env)
        throws CLIException {
        environment = new HashMap();
        Locale locale = (Locale)env.get(CLIConstants.ARGUMENT_LOCALE);
        if (locale == null) {
            locale = Locale.getDefault();
        }
        environment.put(CLIConstants.ARGUMENT_LOCALE, locale);

        try {
            rbMessages = ResourceBundle.getBundle(
                RESOURCE_BUNDLE_NAME, locale);
        } catch (MissingResourceException e) {
            outputWriter.printlnError(e.getMessage());
            System.exit(ExitCodes.MISSING_RESOURCE_BUNDLE);
        }

        String defintionFiles = (String)env.get(
            CLIConstants.SYS_PROPERTY_DEFINITION_FILES);
        setupDefinitions(defintionFiles);

        commandName = (String)env.get(CLIConstants.SYS_PROPERTY_COMMAND_NAME);
        if ((commandName == null) || (commandName.length() == 0)) {
            throw new CLIException(rbMessages.getString(
                "exception-message-missing-command-name"),
                ExitCodes.MISSING_COMMAND_NAME);
        }

        outputWriter = (IOutput)env.get(
            CLIConstants.SYS_PROPERTY_OUTPUT_WRITER);
        if (outputWriter == null) {
            throw new CLIException("output writer is not defined.",
                ExitCodes.OUTPUT_WRITER_CLASS_CANNOT_INSTANTIATE);
        }

        if (env.get(CLIConstants.ARGUMENT_DEBUG) != null) {
            environment.put(CLIConstants.ARGUMENT_DEBUG, Boolean.TRUE);
        }

        if (env.get(CLIConstants.ARGUMENT_VERBOSE) != null) {
            environment.put(CLIConstants.ARGUMENT_VERBOSE, Boolean.TRUE);
        }

        String webEnabledURL = (String)env.get(CLIConstants.WEB_ENABLED_URL);
        if (webEnabledURL != null) {
            environment.put(CLIConstants.WEB_ENABLED_URL, webEnabledURL);
        }
        debugger = Debug.getInstance("amCLI");
    }

    private void init(String[] argv)
        throws CLIException
    {
        environment = new HashMap();
        getLocale(argv);

        String defintionFiles = System.getProperty(
            CLIConstants.SYS_PROPERTY_DEFINITION_FILES);
        setupDefinitions(defintionFiles);

        commandName = System.getProperty(
            CLIConstants.SYS_PROPERTY_COMMAND_NAME);
        if ((commandName == null) || (commandName.length() == 0)) {
            throw new CLIException(rbMessages.getString(
                "exception-message-missing-command-name"),
                ExitCodes.MISSING_COMMAND_NAME);
        }

        String outputWriterClassName = System.getProperty(
            CLIConstants.SYS_PROPERTY_OUTPUT_WRITER);
        getOutputWriter(outputWriterClassName);

        if (getFlag(argv, CLIConstants.ARGUMENT_DEBUG,
            CLIConstants.SHORT_ARGUMENT_DEBUG)
        ) {
            environment.put(CLIConstants.ARGUMENT_DEBUG, Boolean.TRUE);
        }

        if (getFlag(argv, CLIConstants.ARGUMENT_VERBOSE,
            CLIConstants.SHORT_ARGUMENT_VERBOSE)
        ) {
            environment.put(CLIConstants.ARGUMENT_VERBOSE, Boolean.TRUE);
        }

        if (getFlag(argv, CLIConstants.ARGUMENT_NOLOG,
            CLIConstants.SHORT_ARGUMENT_NOLOG)
        ) {
            environment.put(CLIConstants.ARGUMENT_NOLOG, Boolean.TRUE);
        }
        debugger = Debug.getInstance("amCLI");
    }

    private void setupDefinitions(String defintionFiles)
        throws CLIException
    {
        if ((defintionFiles == null) || (defintionFiles.length() == 0)) {
            throw new CLIException(rbMessages.getString(
                "exception-message-missing-definition-class"),
                ExitCodes.MISSING_DEFINITION_FILES);
        }
        initDefinitions(defintionFiles);
    }

    private void initDefinitions(String definitionClassNames)
        throws CLIException
    {
        if (isVerbose()) {
            outputWriter.printlnMessage(rbMessages.getString(
                "verbose-reading-definition-files"));
        }
        definitionObjects = new ArrayList();
        StringTokenizer st = new StringTokenizer(definitionClassNames, ",");
        while (st.hasMoreTokens()) {
            String className = st.nextToken();
            getDefinitionObject(className);
        }
    }

    private void getDefinitionObject(String className)
        throws CLIException
    {
        try {
            Class clazz = Class.forName(className);
            IDefinition defClass = (IDefinition)clazz.newInstance();
            defClass.init((Locale)environment.get(
                CLIConstants.ARGUMENT_LOCALE));
            definitionObjects.add(defClass);
            logName = defClass.getLogName();
        } catch (ClassNotFoundException e) {
            Object[] param = {className};
            String message = MessageFormat.format(
                rbMessages.getString(
                    "exception-message-definition-class-not-found"), param);
            throw new CLIException(message, ExitCodes.MISSING_DEFINITION_CLASS);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Object[] param = {className};
            String message = MessageFormat.format(
                rbMessages.getString(
                    "exception-message-illegal-access-definition-class"),
                    param);
            throw new CLIException(message,
                ExitCodes.ILLEGEL_ACCESS_DEFINITION_CLASS);
        } catch (InstantiationException e) {
            e.printStackTrace();
            Object[] param = {className};
            String message = MessageFormat.format(
                rbMessages.getString(
                    "exception-message-instantiation-definition-class"), param);
            throw new CLIException(message,
                ExitCodes.INSTANTIATION_DEFINITION_CLASS);
        } catch (ClassCastException e) {
            Object[] param = {className};
            String message = MessageFormat.format(
                rbMessages.getString(
                    "exception-message-class-cast-definition-class"), param);
            throw new CLIException(message,
                ExitCodes.CLASS_CAST_DEFINITION_CLASS);
        }
    }

    /**
     * Returns resource bundle.
     *
     * @return resource bundle.
     */
    public ResourceBundle getResourceBundle() {
        return rbMessages;
    }

    /**
     * Returns commandline interface name.
     *
     * @return commandline interface name.
     */
    public String getCommandName() {
        return commandName;
    }
    
    /**
     * Sets status file name.
     *
     * @param statusFileName Status file name.
     * @throws CLIException if status file cannot be created.
     */
    public void setStatusFileName(String statusFileName) 
        throws CLIException {
        if ((statusFileName != null) && (statusFileName.trim().length() > 0)
        ) {
            try {
                statusOS = new FileOutputStream(statusFileName);
            } catch (FileNotFoundException ex) {
                throw new CLIException(ex, 
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        }
    }

    /**
     * Returns a list of definition objects. Since this class is just
     * a engine, it requires definition objects to dictate the behavior
     * of the CLI.
     *
     * @return a list of definition objects.
     */
    public List getDefinitionObjects() {
        return definitionObjects;
    }

    /**
     * Returns <code>true</code> is log is turned off.
     *
     * @return <code>true</code> is log is turned off.
     */
    public boolean isLogOff() {
        return (environment.get(CLIConstants.ARGUMENT_NOLOG) != null);
    }

    /**
     * Returns log name.
     *
     * @return log name.
     */
    public String getLogName() {
        return logName;
    }


    /**
     * Returns locale object.
     *
     * @return locale object.
     */
    public Locale getLocale() {
        return (Locale)environment.get(CLIConstants.ARGUMENT_LOCALE);
    }

    /**
     * Returns true of the CLI has verbose set.
     *
     * @return true of the CLI has verbose set.
     */
    public boolean isVerbose() {
        return (environment.get(CLIConstants.ARGUMENT_VERBOSE) != null);
    }

    /**
     * Returns true of the CLI has debug turned on.
     *
     * @return true of the CLI has debug turned on.
     */
    public boolean isDebugOn() {
        return (environment.get(CLIConstants.ARGUMENT_DEBUG) != null);
    }

    /**
     * Returns debugger.
     *
     * @return debugger.
     */
    public static Debug getDebugger() {
        return debugger;
    }

    /**
     * Returns output writer.
     *
     * @return output writer.
     */
    public IOutput getOutputWriter() {
        return outputWriter;
    }

    /**
     * Returns the sub command of a given name.
     *
     * @param name Name of Sub Command.
     * @return the sub command.
     */
    public SubCommand getSubCommand(String name) {
        SubCommand subcmd = null;
        for (Iterator i = definitionObjects.iterator();
            i.hasNext() && (subcmd == null);
        ) {
            IDefinition def = (IDefinition)i.next();
            subcmd = def.getSubCommand(name);
        }
        return subcmd;
    }

    /**
     * Returns product name.
     *
     * @return product name.
     */
    public String getProductName() {
        String productName = "";
        if ((definitionObjects != null) && !definitionObjects.isEmpty()) {
            IDefinition def = (IDefinition)definitionObjects.get(
                definitionObjects.size() -1);
            productName = def.getProductName();
        }
        return productName;
    }

    private void getOutputWriter(String className)   
        throws CLIException
    {
        try {
            if ((className == null) || (className.length() == 0)) {
                outputWriter = (IOutput)OutputWriter.class.newInstance();
            } else {
                outputWriter = (IOutput)Class.forName(className).newInstance();
            }
        } catch (Exception e) {
            String[] param = {className};
            String msg = "Cannot construct output writer {0}.";
            // cannot localize yet - have not gotten resource bundle
            throw new CLIException(MessageFormat.format(msg, (Object[])param),
                ExitCodes.OUTPUT_WRITER_CLASS_CANNOT_INSTANTIATE);
        }
    }

    /**
     * Services the request queue.
     *
     * @throws CLIException if request cannot be processed.
     */
    public void serviceRequestQueue() 
        throws CLIException {
        if (isVerbose()) {
            outputWriter.printlnMessage(
                rbMessages.getString("verbose-processing-request"));
        }
        
        try {
            while (!requestQueue.isEmpty()) {
                CLIRequest req = (CLIRequest)requestQueue.remove(0);
                try {
                    req.process(this);
                    if (statusOS != null) {
                        String status = formatStatus(req.getOptions(), 0);
                        statusOS.write(status.getBytes());
                    }
                } catch (CLIException e) {
                    if (isVerbose()) {
                        e.printStackTrace(System.out);
                    }
                    if (statusOS != null) {
                        String status = formatStatus(req.getOptions(), 
                            e.getExitCode());
                        statusOS.write(status.getBytes());
                    }
                    if (bContinue) {
                        outputWriter.printlnError(e.getMessage());
                    } else {
                        throw e;
                    }
                }
            }
        } catch (IOException e) {
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (statusOS != null) {
                try {
                    statusOS.close();
                } catch (IOException ex) {
                    // ignored
                }
                statusOS = null;
            }
        }
    }
    
    private static String formatStatus(String[] options, int exitCode) {
        String strCode = Integer.toString(exitCode);
        if (exitCode < 10) {
            strCode = "  " + strCode;
        } else if (exitCode < 100) {
            strCode = " " + strCode;
        }
        
        StringBuilder buff = new StringBuilder();
        buff.append(strCode).append(" ");
        
        for (int i = 0; i < options.length; i++) {
            buff.append(options[i]).append(" ");
        }
        
        return buff.toString() + "\n";
    }

    private static boolean getFlag(
        String[] argv,
        String longName,
        String shortName
    ) throws CLIException
    {
        boolean flag = false;
        for (int i = 0; (i < argv.length) && !flag; i++) {
            String s = argv[i];
            if (s.equals(CLIConstants.PREFIX_ARGUMENT_LONG + longName) ||
                s.equals(CLIConstants.PREFIX_ARGUMENT_SHORT + shortName)
            ) {
                flag = true;
                int nextIdx = i+1;
                if (nextIdx < argv.length) {
                    String str = argv[nextIdx];
                    if (!str.startsWith(CLIConstants.PREFIX_ARGUMENT_SHORT)){
                        throw new CLIException(
                            "Incorrect " + longName + " option.",
                            ExitCodes.INCORRECT_OPTION);
                    }
                }
            }
        }
        return flag;
    }

    private void getLocale(String[] argv)
        throws CLIException
    {
        Locale locale = null;
        for (int i = 0; (i < argv.length) && (locale == null); i++) {
            String s = argv[i];
            if (s.equals(
                    CLIConstants.PREFIX_ARGUMENT_LONG +
                    CLIConstants.ARGUMENT_LOCALE) ||
                s.equals(
                    CLIConstants.PREFIX_ARGUMENT_SHORT +
                    CLIConstants.SHORT_ARGUMENT_LOCALE)
            ) {
                int nextIdx = i+1;
                if (nextIdx >= argv.length) {
                    throw new CLIException("Incorrect locale option.",
                        ExitCodes.INCORRECT_OPTION);
                } else {
                    String strLocale = argv[nextIdx];
                    if (strLocale.startsWith(
                        CLIConstants.PREFIX_ARGUMENT_SHORT)
                    ) {
                        throw new CLIException("Incorrect locale option.",
                            ExitCodes.INCORRECT_OPTION);
                    } else {
                        locale = getLocale(strLocale);
                    }
                }
            }
        }

        if (locale == null) {
            locale = Locale.getDefault();
        }

        environment.put(CLIConstants.ARGUMENT_LOCALE, locale);

        try {
            rbMessages = ResourceBundle.getBundle(
                RESOURCE_BUNDLE_NAME, locale);
        } catch (MissingResourceException e) {
            outputWriter.printlnError(e.getMessage());
            System.exit(ExitCodes.MISSING_RESOURCE_BUNDLE);
        }
    }

    private static Locale getLocale(String strLocale) {
        StringTokenizer st = new StringTokenizer(strLocale, "_");
        String lang = (st.hasMoreTokens()) ? st.nextToken() : "";
        String country = (st.hasMoreTokens()) ? st.nextToken() : "";
        String variant = (st.hasMoreTokens()) ? st.nextToken() : "";
        return new Locale(lang, country, variant);
    }

    private static void getIsInstallTime() {
        String strInstallTime = System.getProperty("installTime");

        if ((strInstallTime != null) && (strInstallTime.trim().length() > 0)) {
            if (strInstallTime.trim().toLowerCase().equals("true")) {
                /*
                 * Set the property to inform AdminTokenAction that
                 * "amadmin" CLI is executing the program
                 */
                SystemProperties.initializeProperties(
                    AdminTokenAction.AMADMIN_MODE, "true");
            }
        }
    }

    /**
     * Sets/Resets the continue flag. Queue of requests shall be processed 
     * in the event that one or more requests are errornous if this flag is
     * set. On the other hand, queue of requests shall be terminated at the
     * first encountered errorous request if this flag is reset.
     *
     * @param bContinue Continue status flag.
     */
    public void setContinueFlag(boolean bContinue) {
        this.bContinue = bContinue;
    }

    /**
     * Adds request to request queue.
     *
     * @param request CLI Request object to be added to the queue.
     */
    public void addToRequestQueue(CLIRequest request) {
        requestQueue.add(request);
    }

    /**
     * Returns Web enabled URL.
     *
     * @return Web enabled URL.
     */
    public String getWebEnabledURL() {
        return (String)environment.get(CLIConstants.WEB_ENABLED_URL);
    }


    /**
     * Returns <code>true</code> if command manager is created from JSP.
     *
     * @return <code>true</code> if command manager is created from JSP.
     */
    public boolean webEnabled() {
        String url = getWebEnabledURL();
        return (url != null) && (url.length() > 0);
    }

    /**
     * Registers Single Single On Token which will be destroyed after
     * CLI is done.
     *
     * @param ssoToken Single Sign On Token.
     */
    public void registerSSOToken(SSOToken ssoToken) {
        ssoTokens.add(ssoToken);
	Logger.token.set(ssoToken);
    }

    private void destroySSOTokens() {
        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            for (Iterator i = ssoTokens.iterator(); i.hasNext(); ) {
                SSOToken token = (SSOToken)i.next();
                mgr.destroyToken(token);
            }
            if (!importSvcCmd) {
                Logger.token.set(null);
            }
        } catch (SSOException e) {
            Debugger.error(this, "CommandManager.destroySSOTokens", e);
        }
    }
}
