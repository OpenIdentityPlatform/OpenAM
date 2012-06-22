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
 * $Id: ListEntities.java,v 1.8 2009/10/29 00:03:50 exu Exp $
 *
 */

package com.sun.identity.federation.cli;

import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

/**
 * List entities.
 */
public class ListEntities extends AuthenticatedCommand {
    private String realm;

    /**
     * Lists Entities.
     *
     * @param rc Request Context.
     * @throws CLIException if unable to process this request.
     */
    @Override
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();

        realm = getStringOptionValue(FedCLIConstants.ARGUMENT_REALM, "/");
        
        String spec = FederationManager.getIDFFSubCommandSpecification(rc);
        String[] params = {realm, spec};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_LIST_ENTITIES", params);

        if (spec.equals(FederationManager.DEFAULT_SPECIFICATION)) {
            handleSAML2Request(rc);
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_LIST_ENTITIES", params);
        } else if (spec.equals(FedCLIConstants.IDFF_SPECIFICATION)) {
            handleIDFFRequest(rc);
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_LIST_ENTITIES", params);
        } else if (spec.equals(FedCLIConstants.WSFED_SPECIFICATION)) {
            handleWSFedRequest(rc);
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_LIST_ENTITIES", params);
        } else {
            String[] args = {realm, 
                getResourceString("unsupported-specification")};
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LIST_ENTITIES", args);
            throw new CLIException(
                getResourceString("unsupported-specification"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
    
    private void handleSAML2Request(RequestContext rc)
        throws CLIException {  
        IOutput outputWriter = getOutputWriter();
        Object[] objs = {realm};
        try {
            SAML2MetaManager metaManager = new SAML2MetaManager(ssoToken);
            Set entities = metaManager.getAllEntities(realm);
            
            if ((entities == null) || entities.isEmpty()) {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("list-entities-no-entities"), objs));
            } else {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("list-entities-entity-listing"), objs));
                for (Iterator i = entities.iterator(); i.hasNext();) {
                    String name = (String)i.next();
                    outputWriter.printlnMessage("  " + name);
                }
            }
        } catch (SAML2MetaException e) {
            debugWarning("ListEntities.handleRequest", e);
            String[] args = {realm, e.getMessage()}; 
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LIST_ENTITIES", args);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void handleIDFFRequest(RequestContext rc)
        throws CLIException {  
        IOutput outputWriter = getOutputWriter();
        Object[] objs = {realm};
        try {
            IDFFMetaManager metaManager = new IDFFMetaManager(ssoToken);
            Set entities = metaManager.getAllEntities(realm);
            
            if ((entities == null) || entities.isEmpty()) {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("list-entities-no-entities"), objs));
            } else {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("list-entities-entity-listing"), objs));
                for (Iterator i = entities.iterator(); i.hasNext();) {
                    String name = (String)i.next();
                    outputWriter.printlnMessage("  " + name);
                }
            }
        } catch (IDFFMetaException e) {
            debugWarning("ListEntities.handleIDFFRequest", e);
            String[] args = {realm, e.getMessage()}; 
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LIST_ENTITIES", args);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
    
    private void handleWSFedRequest(RequestContext rc)
        throws CLIException {  
        IOutput outputWriter = getOutputWriter();
        Object[] objs = {realm};
        try {
            WSFederationMetaManager metaManager = new WSFederationMetaManager(
                ssoToken);
            Set entities = metaManager.getAllEntities(realm);
            
            if ((entities == null) || entities.isEmpty()) {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("list-entities-no-entities"), objs));
            } else {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("list-entities-entity-listing"), objs));
                for (Iterator i = entities.iterator(); i.hasNext();) {
                    String name = (String)i.next();
                    outputWriter.printlnMessage("  " + name);
                }
            }
        } catch (WSFederationMetaException e) {
            debugWarning("ListEntities.handleRequest", e);
            String[] args = {realm, e.getMessage()}; 
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LIST_ENTITIES", args);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
