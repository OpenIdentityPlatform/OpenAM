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
 * $Id: GetAssignableServices.java,v 1.4 2008/06/25 05:42:15 qcheng Exp $
 *
 */

package com.sun.identity.cli.idrepo;


import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

/**
 * This command gets the assignable services of an identity.
 */
public class GetAssignableServices extends IdentityCommand {
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
        IdType idType = convert2IdType(type);
        String[] params = {realm, type, idName};

        try {
            AMIdentityRepository amir = new AMIdentityRepository(
                adminSSOToken, realm);
            Set set = amir.getAllowedIdOperations(idType);
            if (!set.contains(IdOperation.SERVICE)) {
                throw new CLIException(MessageFormat.format(getResourceString(
                    "realm-does-not-support-service"), (Object[])params),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_GET_ASSIGNABLE_SERVICES", params);
            AMIdentity amid = new AMIdentity(
                adminSSOToken, idName, idType, realm, null); 
            Set services = amid.getAssignableServices();

            if (idType.equals(IdType.USER)) {
                services.remove(Constants.SVC_NAME_USER);
                services.remove(Constants.SVC_NAME_AUTH_CONFIG);
                services.remove(Constants.SVC_NAME_SAML);
            }

                

            if ((services != null) && !services.isEmpty()) {
                String msg = getResourceString("assignable-service-result");
                String[] arg = {""};
                for (Iterator i = services.iterator(); i.hasNext(); ) {
                    arg[0] = (String)i.next();
                    outputWriter.printlnMessage(
                        MessageFormat.format(msg, (Object[])arg));
                }
            } else {
                outputWriter.printlnMessage(getResourceString(
                    "no-service-assignable"));
            }
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, 
                "SUCCEED_GET_ASSIGNABLE_SERVICES", params);
        } catch (IdRepoException e) {
            String[] args = {realm, type, idName, e.getMessage()};
            debugError("GetAssignableServices.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_ASSIGNABLE_SERVICES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, type, idName, e.getMessage()};
            debugError("GetAssignableServices.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_ASSIGNABLE_SERVICES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void discardServicesWithoutAttributeSchema(
        SSOToken adminSSOToken,
        Set serviceNames,
        IdType type
    ) throws SMSException, SSOException {
        for (Iterator iter = serviceNames.iterator(); iter.hasNext(); ) {
            String serviceName = (String)iter.next();
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                serviceName, adminSSOToken);
            String url = mgr.getPropertiesViewBeanURL();

            if (url == null) {
                ServiceSchema serviceSchema = mgr.getSchema(type.getName());
                Set attributes = serviceSchema.getAttributeSchemas();

                if ((attributes == null) || attributes.isEmpty()) {
                    iter.remove();
                } else if (!hasI18nKeys(attributes)) {
                    iter.remove();
                }
            }
        }
    }

    private boolean hasI18nKeys(Set attributeSchemes) {
        boolean has = false;
        for (Iterator i = attributeSchemes.iterator(); (i.hasNext() && !has);){
            AttributeSchema as = (AttributeSchema)i.next();
            String i18nKey = as.getI18NKey();
            has = (i18nKey != null) && (i18nKey.length() > 0);
        }
        return has;
    }

}
