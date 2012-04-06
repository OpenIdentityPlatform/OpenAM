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
 * $Id: CLICommandBase.java,v 1.7 2009/11/18 23:54:24 dillidorai Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli;


import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;

/**
 * This is the base class for all CLI command implementation.
 */
public abstract class CLICommandBase implements CLICommand {
    private SubCommand subCommand;
    private RequestContext requestContext;
    private ResourceBundle rb;

    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        subCommand = rc.getSubCommand();
        requestContext = rc;
        rb = rc.getSubCommand().getResourceBundle();
        IOutput outputWriter = getOutputWriter();

        outputWriter.printlnMessage("");
        CommandManager mgr = getCommandManager();

        if (mgr.isVerbose()) {
            ResourceBundle mrc = mgr.getResourceBundle();
            String msg = mrc.getString("verbose-executeCmd");
            String[] arg = {getClass().getName()};
            outputWriter.printlnMessage(
                MessageFormat.format(msg, (Object[])arg));
        }
    }
    
    protected String getStringOptionValue(
        String optionName,
        String defValue
    ) {
        String value = getStringOptionValue(optionName);
        if ((value == null) || (value.trim().length() == 0)) {
            value = defValue;
        }
        return value;
    }

    protected String getStringOptionValue(String optionName) {
        List list = (List)requestContext.getOption(optionName);
        return ((list != null) && !list.isEmpty()) ? (String)list.get(0) : null;
    }

    protected String getResourceString(String key) {
        return rb.getString(key);
    }

    protected boolean isOptionSet(String key) {
        return (requestContext.getOption(key) != null);
    }

    protected IOutput getOutputWriter() {
        CommandManager mgr = requestContext.getCommandManager();
        return mgr.getOutputWriter();
    }

    protected CommandManager getCommandManager() {
        return requestContext.getCommandManager();
    }

    protected boolean isVerbose() {
        return requestContext.getCommandManager().isVerbose();
    }

    protected void writeLog(
        int type,
        Level level,
        String msgid,
        String[] msgdata
    ) throws CLIException {
        CommandManager mgr = requestContext.getCommandManager();
        LogWriter.log(mgr, type, level, msgid, msgdata, null);
    }

    protected void debugError(String msg) {
        CommandManager mgr = requestContext.getCommandManager();
        Debugger.error(mgr, msg);
    }

    protected void debugError(String msg, Throwable e) {
        CommandManager mgr = requestContext.getCommandManager();
        Debugger.error(mgr, msg, e);
    }

    protected void debugMessage(String msg) {
        CommandManager mgr = requestContext.getCommandManager();
        Debugger.message(mgr, msg);
    }

    protected void debugWarning(String msg, Throwable e) {
        CommandManager mgr = requestContext.getCommandManager();
        Debugger.warning(mgr, msg, e);
    }

    protected String tokenize(Collection collection) {
        StringBuilder buff = new StringBuilder();
        if ((collection != null) && !collection.isEmpty()) {
            boolean first = true;
            for (Iterator i = collection.iterator(); i.hasNext(); ) {
                if (!first) {
                    buff.append(", ");
                } else {
                    first = false;
                }
                buff.append((i.next()).toString());
            }
        }
        return buff.toString();
    }
}
