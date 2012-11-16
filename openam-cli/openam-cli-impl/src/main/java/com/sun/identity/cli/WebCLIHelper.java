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
 * $Id: WebCLIHelper.java,v 1.16 2009/11/04 23:36:45 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */
package com.sun.identity.cli;

import com.iplanet.sso.SSOToken;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javax.servlet.http.HttpServletRequest;
import org.owasp.esapi.ESAPI;

/**
 * Helper class for web based CLI JSP.
 */
public class WebCLIHelper {
    private BufferOutputWriter outputWriter;
    private String jspName;
    private CommandManager cmdMgr;

    public WebCLIHelper(
        HttpServletRequest request,
        String definitionClass,
        String commandName,
        String jspName
    ) throws CLIException {
        outputWriter = new BufferOutputWriter();
        this.jspName = jspName;

        Map env = new HashMap();
        env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
        env.put(CLIConstants.ARGUMENT_LOCALE, request.getLocale());
        env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
            definitionClass);
        env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, commandName);
        env.put(CLIConstants.WEB_ENABLED_URL, jspName);
        cmdMgr = new CommandManager(env);
    }

    public String getHTML(HttpServletRequest request, SSOToken ssoToken)
        throws CLIException {
        String html;
        String cmdName = request.getParameter("cmd");

        if (cmdName != null) {
            StringBuilder buff = new StringBuilder();
            buff.append(getNavBackLinkHTML());

            String submit = request.getParameter("submit");
            if (submit == null) {
                buff.append(autogenUI(cmdName));
            } else {
                if (!request.getMethod().equals("POST")) {
                    String message = cmdMgr.getResourceBundle().getString(
                        "error-message-http-post-required");
                    throw new CLIException(
                        message, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                }
                buff.append(processRequest(cmdName, request, ssoToken));
            }
            html = buff.toString();
        } else {
            html = getUsageHTML();
        }

        outputWriter.clearBuffer();
        return html;
    }

    private String processRequest(
        String cmdName,
        HttpServletRequest request,
        SSOToken ssoToken
    ) throws CLIException {
        SubCommand cmd = cmdMgr.getSubCommand(cmdName);
        
        if (!cmd.webEnabled()) {
            return "";
        }
        
        List list = new ArrayList();
        Map map = request.getParameterMap();

        for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            if (!key.equals("submit") && !key.equals("cmd") &&
                !key.endsWith("lblb")
            ) {
                String[] values = (String[])map.get(key);
                if (cmd.isUnaryOption(key)) {
                    list.add("--" + key);
                } else {
                    List temp = new ArrayList();
                    for (int j = 0; j < values.length; j++) {
                        String str = values[j];
                        str = str.trim();
                        if (str.length() > 0) {
                            temp.add(str);
                        }
                    }
                    if (!temp.isEmpty()) {
                        list.add("--" + key);
                        list.addAll(temp);
                    }
                }
            }
        }

        int sz = list.size();
        String[] args = new String[sz+1];
        args[0] = cmdName;
        for (int i = 0; i < sz; i++) {
            args[i+1] = (String)list.get(i);
        }

        CLIRequest req = new CLIRequest(null, args, ssoToken);
        cmdMgr.addToRequestQueue(req);
        cmdMgr.serviceRequestQueue();
        return escapeTags(outputWriter.getBuffer());
    }

    private String getNavBackLinkHTML() {
        return "<a href=\"" + jspName + "\">" +
            cmdMgr.getResourceBundle().getString(
            "web-interface-goto-main-page") +
            "</a><br /><br />";
    }

    private String getUsageHTML()
        throws CLIException {
        String[] arg = {"--help"};
        CLIRequest req = new CLIRequest(null, arg);
        cmdMgr.addToRequestQueue(req);
        cmdMgr.serviceRequestQueue();
        String strHelp = outputWriter.getBuffer();
        int idx = strHelp.indexOf("  <a ");
        if (idx != -1) {
            strHelp = strHelp.substring(idx);
        }
        return strHelp;
    }

    public String autogenUI(String cmdName)
        throws CLIException
    {
        ResourceBundle rb = cmdMgr.getResourceBundle();
        StringBuffer buff = new StringBuffer();
        String[] paramCmdName = {cmdName};

        buff.append(MessageFormat.format(rb.getString("web-interface-cmd-name"),
            (Object[])paramCmdName))
            .append("<br />");
        SubCommand cmd = cmdMgr.getSubCommand(cmdName);
        if ((cmd == null) || !cmd.webEnabled()) {
            throw new CLIException(rb.getString(
                "web-interface-cmd-name-not-found"),
                ExitCodes.INVALID_SUBCOMMAND);
        }
        buff.append(cmd.getDescription())
            .append("<br /><br />");
        buff.append("<form action=\"")
            .append(jspName)
            .append("?cmd=")
            .append(cmdName)
            .append("&submit=\" method=\"post\" ")
            .append("onSubmit=\"selectListBoxes(this)\">");
        buff.append("<table border=0>");

        for (Iterator i = cmd.getMandatoryOptions().iterator(); i.hasNext(); ) {
            genUI(cmd, (String)i.next(), true, buff);
        }
        for (Iterator i = cmd.getOptionalOptions().iterator(); i.hasNext(); ) {
            genUI(cmd, (String)i.next(), false, buff);
        }

        buff.append("<tr><td colspan=2 align=\"center\">")
            .append("<input type=\"submit\" value=\"")
            .append(rb.getString("web-interface-button-submit"))
            .append("\"/>&nbsp;")
            .append("<input type=\"reset\" value=\"")
            .append(rb.getString("web-interface-button-reset"))
            .append("\"/></td></tr>");
        buff.append("</table></form>");
        return buff.toString();
    }

    public void genUI(
        SubCommand cmd,
        String opt,
        boolean mandatory,
        StringBuffer buff
    ) {
        ResourceBundle rb = cmdMgr.getResourceBundle();
        
        if (!cmd.isOptionAlias(opt) && !isAuthField(opt) &&
            !isIgnored(cmd, opt)
        ) {
            String desc = cmd.getOptionDescription(opt, true);
            if (desc.endsWith(".")) {
                desc = desc.substring(0, desc.length() -1);
            }
            buff.append("<tr><td valign=\"top\">")
                .append(escapeTags(desc));
            if (mandatory) {
                buff.append("<font color=\"red\">*</font>");
            }
            buff.append(":</td>");

            if (cmd.textareaUI(opt)) {
                buff.append("<td><textarea cols=75 rows=30 name=\"")
                    .append(opt)
                    .append("\"></textarea>");
            } else if (cmd.checkboxUI(opt)) {
                buff.append("<td><input type=\"checkbox\" name=\"")
                    .append(opt)
                    .append("\" value=\"true\" />");
            } else if (cmd.isBinaryOption(opt)) {
                if (opt.indexOf("password") == -1) {
                    buff.append("<td><input type=\"text\" name=\"")
                        .append(opt)
                        .append("\" />");
                } else {
                    buff.append("<td><input type=\"password\" name=\"")
                        .append(opt)
                        .append("\" />");
                }
            } else {
                buff.append("<td><table border=0><tr><td>")
                    .append("<select name=\"")
                    .append(opt)
                    .append("\" size=\"10\" style=\"width:200\" ")
                    .append("width=\"200\" multiple=\"true\">")
                    .append("<td><input type=\"button\" value=\"")
                    .append(rb.getString("web-interface-button-remove"))
                    .append("\"")
                    .append(" onClick=\"removeSelFromList('")
                    .append(opt)
                    .append("'); return false;\" />")
                    .append("</td></tr><tr><td colspan=2>")
                    .append("<input type=\"text\" name=\"")
                    .append(opt)
                    .append("lblb")
                    .append("\" size=\"30\"/>&nbsp;")
                    .append("<input type=\"button\" value=\"")
                    .append(rb.getString("web-interface-button-add"))
                    .append("\"")
                    .append(" onClick=\"addOption('")
                    .append(opt)
                    .append("'); return false;\" />")
                    .append("</td></tr></table>");
                buff.append("\n<script language=\"javascript\">")
                    .append("listboxes[listboxes.length] ='")
                    .append(opt)
                    .append("';")
                    .append("</script>");
            }
            buff.append("<br /><br />")
                .append("</td></tr>");
        }
    }

    private boolean isAuthField(String opt) {
        return opt.equals("adminid") || opt.equals("password-file");
    }

    private boolean isIgnored(SubCommand cmd, String opt) {
        return opt.equals("continue") || opt.equals("outfile") ||
            (opt.equals("datafile") &&
                cmd.isSupportedOption("attributevalues")) ||
            (opt.equals("datafile") &&
                cmd.isSupportedOption("entries")) ||
            (opt.equals("datafile") &&
                cmd.isSupportedOption("choicevalues"));
    }

    public static String escapeTags(String html) {
        return ESAPI.encoder().encodeForHTML(html);
    }

}
