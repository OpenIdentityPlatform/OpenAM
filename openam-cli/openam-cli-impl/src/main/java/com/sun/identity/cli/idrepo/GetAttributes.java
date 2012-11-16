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
 * $Id: GetAttributes.java,v 1.9 2009/09/05 01:30:46 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli.idrepo;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.BackwardCompSupport;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * This command gets attribute values of an identity.
 */
public class GetAttributes extends IdentityCommand {
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);

        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();
        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String idName = getStringOptionValue(ARGUMENT_ID_NAME);
        String type = getStringOptionValue(ARGUMENT_ID_TYPE);
        List attributeNames = rc.getOption(IArgument.ATTRIBUTE_NAMES);
        IdType idType = convert2IdType(type);
        String[] params = {realm, type, idName};

        try {
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_IDREPO_GET_ATTRIBUTES", params);
            AMIdentity amid = new AMIdentity(
                adminSSOToken, idName, idType, realm, null); 
            Set attrSchemas = getAttributeSchemas(type, adminSSOToken);             
            Map rawValues = null;
          
            if ((attributeNames != null) && !attributeNames.isEmpty()) {
                //if user specified particular attributes as command option args
                Set attrNames = new HashSet();                                  
                attrNames.addAll(attributeNames);
                rawValues = amid.getAttributes(attrNames);
            } else {
                rawValues = amid.getAttributes();
            }
            
            Object[] args = {idName};

            if ((rawValues != null) && !rawValues.isEmpty()) {
                String msg = getResourceString("idrepo-attribute-result");
                String[] arg = {"", ""};
                for (Iterator i = rawValues.keySet().iterator(); i.hasNext(); ) {
                    String attrName = (String)i.next();
                    Set attrValues = (Set)rawValues.get(attrName);
                    arg[0] = attrName;
                    
                    arg[1] = isPassword(attrSchemas, attrName) ? "********" :
                        tokenize(attrValues);
                    outputWriter.printlnMessage(MessageFormat.format(msg, 
                        (Object[])arg));
                }
            } else {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("idrepo-no-attributes"), args));
            }
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_IDREPO_GET_ATTRIBUTES", params);
        } catch (IdRepoException e) {
            String[] args = {realm, type, idName, e.getMessage()};
            debugError("GetAttributes.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IDREPO_GET_ATTRIBUTES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {realm, type, idName, e.getMessage()};
            debugError("GetAttributes.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IDREPO_GET_ATTRIBUTES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, type, idName, e.getMessage()};
            debugError("GetAttributes.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IDREPO_GET_ATTRIBUTES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
    
    private boolean isPassword(Set attrSchemas, String attrName) {
        boolean isPwd = false;
        for (Iterator i = attrSchemas.iterator(); i.hasNext(); ) {
            AttributeSchema as = (AttributeSchema)i.next();
            if (attrName.equals(as.getName())) {
                 AttributeSchema.Syntax syntax = as.getSyntax();
                 isPwd = (syntax != null) && 
                     (syntax == AttributeSchema.Syntax.PASSWORD);
                 break;
            }
        }
        return isPwd;
    }
 
    private Set getAttributeSchemas(String idType, SSOToken token)
        throws SMSException, SSOException, IdRepoException { 
        Set attributeSchemas = Collections.EMPTY_SET;
        String serviceName = getSvcNameForIdType(idType);
  
        if (serviceName != null) {
            ServiceSchemaManager svcSchemaMgr = new ServiceSchemaManager(
                serviceName, token);
            ServiceSchema svcSchema = svcSchemaMgr.getSchema(idType);
            if (svcSchema != null) {
                attributeSchemas = svcSchema.getAttributeSchemas();
            }
        }
        for (Iterator i = attributeSchemas.iterator(); i.hasNext(); ) {
            AttributeSchema as = (AttributeSchema)i.next();
            String i18nKey = as.getI18NKey();
            if ((i18nKey == null) || (i18nKey.trim().length() == 0)) {
                i.remove();
            }
        }
        return attributeSchemas;
    }
    
    private String getSvcNameForIdType(String idType)
        throws IdRepoException {
        String serviceName = IdUtils.getServiceName(IdUtils.getType(idType));        
        if ((serviceName == null) || (serviceName.trim().length() == 0)) {
            if (ServiceManager.isCoexistenceMode()) {
                BackwardCompSupport support = BackwardCompSupport.getInstance();
                serviceName = support.getServiceName(idType);
            }
        }
        return serviceName;
    }

}
