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
 * $Id: ListAgentGroups.java,v 1.4 2008/06/25 05:42:10 qcheng Exp $
 *
 */

package com.sun.identity.cli.agentconfig;


import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIConstants;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.common.DisplayUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * This command searches for agent groups.
 */
public class ListAgentGroups extends AuthenticatedCommand {
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();

        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();
        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String patternType = getStringOptionValue(IArgument.AGENT_TYPE);
        String filter = getStringOptionValue(IArgument.FILTER);

        if (patternType == null) {
            patternType = "";
        }

        if ((filter == null) || (filter.length() == 0)) {
            filter = "*";
        }

        String[] params = {realm, patternType, filter};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO, "ATTEMPT_LIST_AGENT_GROUPS",
            params);

        try {
            AMIdentityRepository amir = new AMIdentityRepository(
                adminSSOToken, realm);
            IdSearchResults isr = amir.searchIdentities(IdType.AGENTGROUP,
                filter, new IdSearchControl());
            Set results = isr.getSearchResults();

            if ((results != null) && !results.isEmpty()) {
                for (Iterator i = results.iterator(); i.hasNext(); ) {
                    AMIdentity amid = (AMIdentity)i.next();
                    if (!matchType(amid, patternType)) {
                        i.remove();
                    }
                }
            }

            if ((results != null) && !results.isEmpty()) {
                for (Iterator i = results.iterator(); i.hasNext(); ) {
                    AMIdentity amid = (AMIdentity)i.next();
                    Object[] args = {amid.getName(), amid.getUniversalId()};
                    outputWriter.printlnMessage(MessageFormat.format(
                        getResourceString("format-search-agent-group-results"),
                            args));
                }
            } else {
                outputWriter.printlnMessage(getResourceString(
                    "search-agent-group-no-entries"));
            }

            writeLog(LogWriter.LOG_ACCESS, Level.INFO, 
                "SUCCEED_LIST_AGENT_GROUPS", params);
        } catch (IdRepoException e) {
            String[] args = {realm, patternType, filter, e.getMessage()};
            debugError("ListAgentGroups.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LIST_AGENT_GROUPS", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, patternType, filter, e.getMessage()};
            debugError("ListAgentGroups.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LIST_AGENT_GROUPS", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private boolean matchType(AMIdentity amid, String pattern)
        throws IdRepoException, SSOException {
        boolean matched = (pattern.length() == 0);

        if (!matched) {
            Map attrValues = amid.getAttributes();
            Set set = (Set)attrValues.get(CLIConstants.ATTR_NAME_AGENT_TYPE);
            if ((set != null) && !set.isEmpty()) {
                String t = (String)set.iterator().next();
                matched = DisplayUtils.wildcardMatch(t, pattern);
            }
        }

        return matched;
    }
}
