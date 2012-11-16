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
 * $Id: UsageFormatter.java,v 1.5 2008/06/25 05:42:09 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli;


import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * This format the usage text of CLI.
 */
public class UsageFormatter {
    private static UsageFormatter instance = new UsageFormatter();
    private static List globalOptions = new ArrayList();
    private static List globalArgs = new ArrayList();
    private static int MAX_USAGE_LEN = 75;

    static {
        globalOptions.add("LOCALE");
        globalOptions.add("DEBUG");
        globalOptions.add("VERBOSE");

        globalArgs.add("VERSION");
        globalArgs.add("INFORMATION");
        globalArgs.add("HELP");
    }

    /**
     * Returns an instance of this class.
     *
     * @return an instance of this class.
     */
    public static UsageFormatter getInstance() {
        return instance;
    }

    private UsageFormatter() {
    }

    /**
     * Prints the usage of CLI.
     *
     * @param mgr Command Manager object.
     * @throws CLIException if usage text cannot be presented.
     */
    public void format(CommandManager mgr)
        throws CLIException
    {
        StringBuffer buff = new StringBuffer();
        buff.append("\n\n");
        formatUsage(mgr, buff);
        formatGlobalOptions(mgr, buff);
        formatSubcmds(mgr, buff);
        mgr.getOutputWriter().printlnMessage(buff.toString());
    }

    /**
     * Prints the usage of sub command.
     *
     * @param mgr Command Manager object.
     * @param cmd Sub command object.
     * @throws CLIException if usage text cannot be presented.
     */
    public void format(CommandManager mgr, SubCommand cmd)
        throws CLIException
    {
        StringBuffer buff = new StringBuffer();
        buff.append("\n\n");

        ResourceBundle rb = mgr.getResourceBundle();
        String commandName = mgr.getCommandName();
        Object[] params = {commandName, cmd.getName(), 
            rb.getString("USAGE_OPTIONS"),
            rb.getString("USAGE_GLOBAL_OPTIONS"),
            cmd.getDescription()};
        buff.append(MessageFormat.format(CLIConstants.USAGE_SUBCMD_LONG_FORMAT,
            params));
        buff.append("\n");
        formatUsage(mgr, buff, cmd);
        formatGlobalOptions(mgr, buff);
        formatOptions(mgr, buff, cmd);
        mgr.getOutputWriter().printlnMessage(buff.toString());
    }

    private void formatSubcmds(CommandManager mgr, StringBuffer buff)
        throws CLIException
    {
        ResourceBundle rb = mgr.getResourceBundle();
        buff.append(rb.getString("USAGE_SUBCOMMAND_TITLE"));
        buff.append("\n");

        List defObjects = mgr.getDefinitionObjects();
        Map mapCmds = new HashMap();
        Set orderCmds = new TreeSet();

        for (Iterator i = defObjects.iterator(); i.hasNext(); ) {
            IDefinition def = (IDefinition)i.next();
            for (Iterator j = def.getSubCommands().iterator(); j.hasNext(); ) {
                SubCommand cmd = (SubCommand)j.next();
                
                if ((cmd != null) && (!mgr.webEnabled() || cmd.webEnabled())) {
                    String name = cmd.getName();
                    orderCmds.add(name);
                    mapCmds.put(name, cmd.getDescription());
                }
            }
        }
        
        StringBuilder buffCmd = new StringBuilder();
        boolean started = false;
        String webEnabledURL = mgr.getWebEnabledURL();
        for (Iterator i = orderCmds.iterator(); i.hasNext(); ) {
            String cmdName = (String)i.next();
            buffCmd.append(formAbstractCmdUsage(webEnabledURL, cmdName,
                (String)mapCmds.get(cmdName)));
        }

        buff.append(buffCmd.toString());
    }

    private String formAbstractCmdUsage(
        String webEnabledURL,
        String cmdName,
        String description
    ) {
        StringBuilder buff = new StringBuilder();

        if (webEnabledURL != null) {
            buff.append("  <a href=\"")
                .append(webEnabledURL)
                .append("?cmd=")
                .append(cmdName)
                .append("\">")
                .append(cmdName)
                .append("</a> ");
        } else {
            buff.append("    ").append(cmdName);
        }
        buff.append("\n");
        
        StringTokenizer st = new StringTokenizer(description, " ");
        int currentLen = 8;
        buff.append("        ");
        while (st.hasMoreTokens()) {
            String t = st.nextToken();
            if ((currentLen + t.length()) > MAX_USAGE_LEN) {
                buff.append("\n        ")
                    .append(t)
                    .append(" ");
                currentLen = 8;
            } else {
                buff.append(t)
                    .append(" ");
            }
            currentLen += t.length() + 1;
        }
        buff.append("\n\n");

        return buff.toString();
    }

    private void formatUsage(
        CommandManager mgr,
        StringBuffer buff,
        SubCommand cmd
    ) throws CLIException {
        ResourceBundle rb = mgr.getResourceBundle();
        buff.append(rb.getString("USAGE"));
        buff.append("\n");

        buff.append(mgr.getCommandName())
            .append(" ")
            .append(cmd.getName());

        formatOptionNames(cmd.getMandatoryOptions(), cmd, buff,
            CLIConstants.USAGE_OPTION_NAME_FORMAT);
        formatOptionNames(cmd.getOptionalOptions(), cmd, buff,
            CLIConstants.USAGE_OPTIONAL_OPTION_NAME_FORMAT);

        buff.append("\n")
            .append("\n");
    }

    private void formatOptionNames(
        List list,
        SubCommand cmd,
        StringBuffer buff,
        String template
    ) {
        for (Iterator i = list.iterator(); i.hasNext();) {
            String opt = (String)i.next();
            if (!cmd.isOptionAlias(opt)) {
                Object[] params = {opt, cmd.getShortOptionName(opt)};
                buff.append("\n")
                    .append(MessageFormat.format(template, params));
                List aliases = cmd.getOptionAliases(opt);

                if ((aliases != null) && !aliases.isEmpty()) {
                    for (Iterator j = aliases.iterator(); j.hasNext();) {
                        String alias = (String)j.next();
                        String[] p = {alias, cmd.getShortOptionName(alias)};
                        buff.append(",")
                            .append(MessageFormat.format(
                                template, (Object[])p));
                    }
                }
            }
        }
    }

    private void formatUsage(CommandManager mgr, StringBuffer buff)
        throws CLIException
    {
        ResourceBundle rb = mgr.getResourceBundle();
        String commandName = mgr.getCommandName();
        buff.append(rb.getString("USAGE"));
        buff.append("\n");

        for (Iterator i = globalArgs.iterator(); i.hasNext(); ) {
            try {
                String option = (String)i.next();
                Field fldLong = CLIConstants.class.getField(
                    CLIConstants.PREFIX_ARGUMENT + option);
                Field fldShort = CLIConstants.class.getField(
                    CLIConstants.PREFIX_SHORT_ARGUMENT + option);
                Object[] params = {commandName, fldLong.get(null),
                    fldShort.get(null),
                    rb.getString(CLIConstants.PREFIX_ARGUMENT + option)};
                buff.append(MessageFormat.format(
                    CLIConstants.USAGE_FORMAT, params));
                buff.append("\n");
            } catch (Exception e) {
                throw new CLIException(e.getMessage(),
                    ExitCodes.USAGE_FORMAT_ERROR);
            }
        }

        {
            Object[] params = {commandName,
                rb.getString("USAGE_SUBCOMMAND"),
                rb.getString("USAGE_GLOBAL_OPTIONS"),
                rb.getString("USAGE_OPTIONS"),
                rb.getString("ARGUMENT_SUBCOMMAND")};
            buff.append(MessageFormat.format(
                CLIConstants.USAGE_SUBCMD_FORMAT, params));
            buff.append("\n");
        }

        {
            Object[] params = {commandName,
                rb.getString("USAGE_SUBCOMMAND"),
                CLIConstants.ARGUMENT_HELP,
                CLIConstants.SHORT_ARGUMENT_HELP,
                rb.getString("ARGUMENT_SUBCOMMAND_HELP")};
            buff.append(MessageFormat.format(
                CLIConstants.USAGE_SUBCMD_HELP_FORMAT, params));
            buff.append("\n");
        }
    }

    private void formatGlobalOptions(CommandManager mgr, StringBuffer buff)
        throws CLIException {
        ResourceBundle rb = mgr.getResourceBundle();
        buff.append(rb.getString("USAGE_GLOBAL_OPTIONS_TITLE"));
        buff.append("\n");

        for (Iterator i = globalOptions.iterator(); i.hasNext(); ) {
            try {
                String option = (String)i.next();
                Field fldLong = CLIConstants.class.getField(
                    CLIConstants.PREFIX_ARGUMENT + option);
                Field fldShort = CLIConstants.class.getField(
                    CLIConstants.PREFIX_SHORT_ARGUMENT + option);
                Object[] params = {fldLong.get(null), fldShort.get(null),
                    rb.getString(CLIConstants.PREFIX_ARGUMENT + option)};
                buff.append(MessageFormat.format(
                    CLIConstants.USAGE_OPTIONAL_OPTION_FORMAT, params));
                buff.append("\n");
            } catch (Exception e) {
                throw new CLIException(e.getMessage(),
                    ExitCodes.USAGE_FORMAT_ERROR);
            }
        }

        buff.append("\n");
    }

    private void formatOptions(
        CommandManager mgr,
        StringBuffer buff,
        SubCommand cmd
    ) throws CLIException {
        ResourceBundle rb = mgr.getResourceBundle();
        buff.append(rb.getString("USAGE_OPTIONS_TITLE"));
        buff.append("\n");
        formatOption(cmd.getMandatoryOptions(), cmd, buff);
        formatOption(cmd.getOptionalOptions(), cmd, buff);
        buff.append("\n");
    }

    private void formatOption(List list, SubCommand cmd, StringBuffer buff) {
        for (Iterator i = list.iterator(); i.hasNext();) {
            String opt = (String)i.next();
            if (!cmd.isOptionAlias(opt)) {
                List aliases = cmd.getOptionAliases(opt);

                if ((aliases == null) || aliases.isEmpty()) {
                    formatOption(buff, opt, cmd,
                        CLIConstants.USAGE_OPTION_FORMAT);
                } else {
                    formatOption(buff, opt, aliases, cmd,
                        CLIConstants.USAGE_OPTION_FORMAT,
                        CLIConstants.USAGE_OPTION_WITH_ALIAS_FORMAT);
                }
            }
        }
    }

    private void formatOption(
        StringBuffer buff,
        String opt,
        SubCommand cmd, 
        String format
    ) {
        Object[] params = {opt, cmd.getShortOptionName(opt),
            cmd.getOptionDescription(opt)};
        buff.append(MessageFormat.format(format, params));
        buff.append("\n");
    }

    private void formatOption(
        StringBuffer buff,
        String opt,
        List aliases,
        SubCommand cmd,
        String format,
        String aliasFormat
    ) {
        Object[] params = {opt, cmd.getShortOptionName(opt),
            cmd.getOptionDescription(opt)};
        buff.append(MessageFormat.format(aliasFormat, params));
        int sz = aliases.size();

        for (int i = 0; i < sz; i++) {
            String alias = (String)aliases.get(i);
            Object[] p = {alias, cmd.getShortOptionName(alias),
                cmd.getOptionDescription(alias)};
            if (i == (sz-1)) {
                buff.append(MessageFormat.format(format, p));
            } else {
                buff.append(MessageFormat.format(aliasFormat, p));
            }
        }

        buff.append("\n");
    }
}
