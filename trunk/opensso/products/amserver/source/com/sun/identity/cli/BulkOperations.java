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
 * $Id: BulkOperations.java,v 1.7 2008/10/30 18:23:18 veiming Exp $
 *
 */

package com.sun.identity.cli;

import com.iplanet.sso.SSOToken;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Multiple requests command.
 */
public class BulkOperations extends AuthenticatedCommand {
    private static final String BATCH_FILE = "batchfile";
    private static final String STATUS_FILE = "batchstatus";
    
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        SSOToken ssoToken = getAdminSSOToken();

        boolean continueFlag = isOptionSet(IArgument.CONTINUE);
        String statusFileName = getStringOptionValue(STATUS_FILE);
        String batchfile = getStringOptionValue(BATCH_FILE);
        List entries = AttributeValues.parseValues(batchfile);

        if ((entries != null) && !entries.isEmpty()) {
            removeEmptyEntries(entries);
        }

        if ((entries == null) || entries.isEmpty()) {
            String[] arg = {batchfile};
            throw new CLIException(
                MessageFormat.format("bulk-op-empty-datafile", (Object[])arg),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        CLIRequest req = rc.getCLIRequest();
        CommandManager mgr = getCommandManager();
        mgr.setContinueFlag(continueFlag);
        mgr.setStatusFileName(statusFileName);
        
        for (Iterator i = entries.iterator(); i.hasNext(); ) {
            String argv = (String)i.next();
            mgr.addToRequestQueue(new CLIRequest(
                req, makeStringArray(argv), ssoToken));
        }
    }

    private String[] makeStringArray(String argv)
        throws CLIException
    {
        argv = argv.trim();
        argv = argv.replace('\t', ' '); 
        List options = new ArrayList();
        char[] array = argv.toCharArray();
        int len = array.length;
        boolean escape = false;
        int startIdx = 0;
        boolean inDblQuote = false;
        boolean inQuote = false;

        for (int i = 0; i < len; i++) {
            switch (array[i]) {
            case ' ':
                if (!inQuote && !inDblQuote) {
                    String tmp = argv.substring(startIdx, i);
                    tmp = tmp.trim();
                    if (tmp.length() > 0) {
                        options.add(tmp);
                    }
                    startIdx = i+1;
                }
                break;
            case '\'':
                if (inQuote) {
                    if (((i+1) < len) && (array[i+1] != ' ')) {
                        throw new CLIException(
                            getResourceString("unmatch-quote"),
                            ExitCodes.INCORRECT_OPTION);
                    }
                    inQuote = false;
                    options.add(argv.substring(startIdx, i));
                    startIdx = i+1;
                } else if (!inDblQuote) {
                    inDblQuote = true;
                    startIdx = i+1;
                }
                break;
            case '"':
                if (inDblQuote) {
                    if (((i+1) < len) && (array[i+1] != ' ')) {
                        throw new CLIException(
                            getResourceString("unmatch-doublequote"),
                            ExitCodes.INCORRECT_OPTION);
                    }
                    inDblQuote = false;
                    options.add(argv.substring(startIdx, i));
                    startIdx = i+1;
                } else if (!inQuote) {
                    inDblQuote = true;
                    startIdx = i+1;
                }
                break;
            }
        }
        if (inQuote) {
            throw new CLIException(
                getResourceString("unmatch-quote"), ExitCodes.INCORRECT_OPTION);
        }
        if (inDblQuote) {
            throw new CLIException(
                getResourceString("unmatch-doublequote"),
                ExitCodes.INCORRECT_OPTION);
        }
        if (startIdx < len) {
            String tmp = argv.substring(startIdx);
            tmp = tmp.trim();
            if (tmp.length() > 0) {
                options.add(tmp);
            }
        }
        String[] optionArray = new String[options.size()];
        int sz = options.size();
        for (int i = 0; i < sz; i++) {
            optionArray[i] = (String)options.get(i);
        }
        return optionArray;
    }

    private void removeEmptyEntries(List entries) {
        for (Iterator i = entries.iterator(); i.hasNext(); ) {
            String argv = ((String)i.next()).trim();
            if ((argv.length() == 0) || argv.startsWith("#")) {
                i.remove();
            }
        }
    }
}
