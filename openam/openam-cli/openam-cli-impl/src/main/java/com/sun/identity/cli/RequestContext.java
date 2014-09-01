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
 * $Id: RequestContext.java,v 1.11 2009/04/02 01:16:07 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.cli;

import com.sun.identity.shared.locale.Locale;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Request context class contains information about the request.
 * This object is passed to sub command handler which services the request.
 */
public class RequestContext {
    private Map mapOptions = new HashMap();
    private CommandManager commandMgr;
    private CLIRequest request;
    private ResourceBundle rb;
    private SubCommand subCommand;

    /**
     * Creates a request context object.
     *
     * @param request Request object.
     * @param commandMgr Command Manager object.
     * @param subcmd Sub Command object.
     * @throws CLIException if this object cannot be constructed.
     */
    public RequestContext(
        CLIRequest request,
        CommandManager commandMgr,
        SubCommand subcmd
    ) throws CLIException {
        this.commandMgr = commandMgr;
        this.request = request;
        rb = commandMgr.getResourceBundle();
        subCommand = subcmd;

        if (commandMgr.isVerbose()) {
            commandMgr.getOutputWriter().printlnMessage(
                rb.getString("verbose-constructing-request-context"));
        }
        String[] argv = request.getOptions();
        CLIRequest parentRequest = request.getParent();
        String[] parentArgv = (parentRequest != null) ?
            parentRequest.getOptions() : null;
        parseArgs(commandMgr.getCommandName(), subcmd, argv, parentArgv);

        if (commandMgr.isVerbose()) {
            commandMgr.getOutputWriter().printlnMessage(
                getResourceString("verbose-validate-mandatory-options"));
        }
        
        if (!subcmd.validateOptions(mapOptions, request.getSSOToken())) {
            throw createIncorrectOptionException(commandMgr.getCommandName(),
                argv);
        }
    }

    /**
     * Returns the <code>CommandManager</code> object.
     *
     * @return the <code>CommandManager</code> object.
     */
    public CommandManager getCommandManager() {
        return commandMgr;
    }

    /**
     * Returns CLI Request object.
     *
     * @return CLI Request object.
     */
    public CLIRequest getCLIRequest() {
        return request;
    }

    /**
     * Returns sub command object.
     *
     * @return sub command object.
     */
    public SubCommand getSubCommand() {
        return subCommand;
    }

    /**
     * Returns resource string.
     *
     * @param key Key of resource string.
     * @return resource string.
     */
    public String getResourceString(String key) {
        return rb.getString(key);
    }

    /**
     * Returns values of an argument/option.
     *
     * @return values of an argument/option.
     */
    public List getOption(String name) {
        return (List)mapOptions.get(name);
    }

    /**
     * Returns a map of argument/option to its values (List).
     *
     * @return a map of argument/option to its values.
     */
    public Map getOptions() {
        return mapOptions;
    }

    private void parseArgs(
        String commandName,
        SubCommand subcmd,
        String[] argv,
        String[] parentArgv
    ) throws CLIException {
        if (parentArgv != null) {
            parseParentArgs(commandName, subcmd, parentArgv);
            mapOptions.remove(IArgument.DATA_FILE);
        }
        parseArgs(commandName, subcmd, argv);
    }

    private void parseParentArgs(
        String commandName,
        SubCommand subcmd,
        String[] argv
    ) throws CLIException {
        //skip the first index because it is the sub command name.
        List values = null;
        for (int i = 1; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.startsWith(CLIConstants.PREFIX_ARGUMENT_LONG)) {
                String option = arg.substring(2);
                int skip = skipGlobalOption(option);
                if (skip > -1) {
                    i += skip;
                    values = null;
                } else if (subcmd.isSupportedOption(option)) {
                    values = new ArrayList();
                    mapOptions.put(option, values);
                } else {
                    values = null;
                }
            } else if (arg.startsWith(CLIConstants.PREFIX_ARGUMENT_SHORT)) {
                String option = arg.substring(1);
                int skip = skipGlobalShortOption(option);
                if (skip > -1) {
                    i += skip;
                    values = null;
                } else {
                    String longOption = subcmd.getLongOptionName(option);
                    if (longOption != null) {
                        values = new ArrayList();
                        mapOptions.put(longOption, values);
                    } else {
                        values = null;
                    }
                }
            } else if (values != null) {
                if (commandMgr.webEnabled()) {
                    values.add(Locale.URLDecodeField(arg, 
                        commandMgr.getDebugger()));
                } else {
                    values.add(arg);
                }
            }
        }
    }

    private void parseArgs(
        String commandName,
        SubCommand subcmd,
        String[] argv
    ) throws CLIException {
        //skip the first index because it is the sub command name.
        List values = null;
        for (int i = 1; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.startsWith(CLIConstants.PREFIX_ARGUMENT_LONG)) {
                String option = arg.substring(2);
                int skip = skipGlobalOption(option);
                if (skip > -1) {
                    i += skip;
                    values = null;
                } else if (subcmd.isSupportedOption(option)) {
                    values = new ArrayList();
                    mapOptions.put(option, values);
                } else {
                    throw createIncorrectOptionException(commandName, argv);
                }
            } else if (arg.startsWith(CLIConstants.PREFIX_ARGUMENT_SHORT)) {
                String option = arg.substring(1);
                int skip = skipGlobalShortOption(option);
                if (skip > -1) {
                    i += skip;
                    values = null;
                } else {
                    String longOption = subcmd.getLongOptionName(option);
                    if (longOption == null) {
                        throw createIncorrectOptionException(commandName, argv);
                    } else {
                        values = new ArrayList();
                        mapOptions.put(longOption, values);
                    }
                }
            } else if (values == null) {
                throw createIncorrectOptionException(commandName, argv);
            } else if (arg.trim().length() > 0) {
                if (commandMgr.webEnabled()) {
                    values.add(Locale.URLDecodeField(arg, 
                        commandMgr.getDebugger()));
                } else {
                    values.add(arg);
                }
            }
        }
    }

    private int skipGlobalOption(String option) {
        int skip = -1;
        if (option.equals(CLIConstants.ARGUMENT_DEBUG) ||
            option.equals(CLIConstants.ARGUMENT_VERBOSE) ||
            option.equals(CLIConstants.ARGUMENT_NOLOG)
        ) {
            skip = 0;
        } else if (option.equals(CLIConstants.ARGUMENT_LOCALE)) {
            skip = 1;
        }
        return skip;
    }

    private int skipGlobalShortOption(String option) {
        int skip = -1;
        if (option.equals(CLIConstants.SHORT_ARGUMENT_DEBUG) ||
            option.equals(CLIConstants.SHORT_ARGUMENT_VERBOSE) ||
            option.equals(CLIConstants.SHORT_ARGUMENT_NOLOG)
        ) {
            skip = 0;
        } else if (option.equals(CLIConstants.SHORT_ARGUMENT_LOCALE)) {
            skip = 1;
        }
        return skip;
    }

    private CLIException createIncorrectOptionException(
        String commandName,
        String[] argv
    ) {
        Object[] param = {commandName + " " + CLIRequest.addAllArgs(argv)};
        return new CLIException(MessageFormat.format(
            rb.getString("error-message-incorrect-options"),
            param), ExitCodes.INCORRECT_OPTION, subCommand.getName());
    }
}
