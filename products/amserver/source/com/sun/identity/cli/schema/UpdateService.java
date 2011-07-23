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
 * $Id: UpdateService.java,v 1.5 2008/06/25 05:42:19 qcheng Exp $
 *
 */

package com.sun.identity.cli.schema;


import com.sun.identity.shared.xml.XMLUtils;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIConstants;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.Debugger;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSSchema;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManager;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Update service schema.
 */
public class UpdateService extends AuthenticatedCommand {
    /**
     * Updates service schema. Delete the service schema if it exists and
     * load the schema.
     *
     * @param rc Request Context.
     * @throws CLIException if request cannot be processed.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        SSOToken adminSSOToken = getAdminSSOToken();

        boolean continueFlag = isOptionSet(IArgument.CONTINUE);
        IOutput outputWriter = getOutputWriter();        
        List xmlFiles = (List)rc.getOption(IArgument.XML_FILE);
        ServiceManager ssm = null;
        
        try {
            ssm = new ServiceManager(adminSSOToken);
        } catch (SMSException e) {
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
        CommandManager mgr = getCommandManager();
        String url = mgr.getWebEnabledURL();
        if ((url != null) && (url.length() > 0)) {
            String strXML = (String)xmlFiles.iterator().next();
            
            try {
                List serviceNames = getServiceNames(
                    SMSSchema.getXMLDocument(strXML, true));
                deleteServices(rc, ssm, serviceNames, adminSSOToken,
                    continueFlag, outputWriter);
                loadSchemaXML(ssm, strXML);
                outputWriter.printlnMessage(
                    getResourceString("service-updated"));
            } catch (SMSException e) {
               throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        } else {
            for (Iterator i = xmlFiles.iterator(); i.hasNext(); ) {
                String file = (String)i.next();
                FileInputStream fis = null;

                try {
                    fis = new FileInputStream(file);
                    List serviceNames = getServiceNames(
                        SMSSchema.getXMLDocument(fis));
                    deleteServices(rc, ssm, serviceNames, adminSSOToken,
                        continueFlag, outputWriter);
                    loadSchema(ssm, file);
                    outputWriter.printlnMessage(
                        getResourceString("service-updated"));
                } catch (CLIException e) {
                    if (continueFlag) {
                        outputWriter.printlnError(
                            getResourceString("service-updated-failed") +
                            e.getMessage());
                        if (isVerbose()) {
                            outputWriter.printlnError(Debugger.getStackTrace(e));
                        }
                    } else {
                        throw e;
                    }
                } catch (SMSException e) {
                    if (continueFlag) {
                        outputWriter.printlnError(
                            getResourceString("service-updated-failed") +
                            e.getMessage());
                        if (isVerbose()) {
                            outputWriter.printlnError(Debugger.getStackTrace(e));
                        }
                    } else {
                        throw new CLIException(
                            e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                    }
                } catch (FileNotFoundException e) {
                    if (continueFlag) {
                        outputWriter.printlnError(
                            getResourceString("service-updated-failed") +
                            e.getMessage());
                        if (isVerbose()) {
                            outputWriter.printlnError(Debugger.getStackTrace(e));
                        }
                    } else {
                        throw new CLIException(
                            e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                    }
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException ie) {
                            //igore if file input stream cannot be closed.
                        }
                    }
                }
            }
        }
    }

    private void loadSchema(ServiceManager ssm, String fileName)
        throws CLIException {
        String[] param = {fileName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_LOAD_SCHEMA", param);
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(fileName);
            ssm.registerServices(fis);
        } catch (IOException e) {
            String[] args = {fileName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, "FAILED_LOAD_SCHEMA",
                args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {fileName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, "FAILED_LOAD_SCHEMA",
                args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {fileName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, "FAILED_LOAD_SCHEMA",
                args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ie) {
                    //igore if file input stream cannot be closed.
                }
            }
        }
    }

    private void loadSchemaXML(ServiceManager ssm, String xml)
        throws CLIException {
        String[] param = {CLIConstants.WEB_INPUT};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_LOAD_SCHEMA", param);
        ByteArrayInputStream bis = null;
        try {
            bis = new ByteArrayInputStream(xml.getBytes());
            ssm.registerServices(bis);
        } catch (SSOException e) {
            String[] args = {CLIConstants.WEB_INPUT, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, "FAILED_LOAD_SCHEMA",
                args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {CLIConstants.WEB_INPUT, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, "FAILED_LOAD_SCHEMA",
                args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException ie) {
                    //igore if file input stream cannot be closed.
                }
            }
        }
    }
    
    private List getServiceNames(Document doc) {
        List serviceNames = new ArrayList();
        NodeList nodes = doc.getElementsByTagName("Service");
        
        if (nodes != null) {
            int len = nodes.getLength();
            
            for (int i = 0; i < len; i++) {
                Node serviceNode = nodes.item(i);
                String name = XMLUtils.getNodeAttributeValue(
                    serviceNode, "name");
                if ((name != null) && (name.length() > 0)) {
                    serviceNames.add(name);
                }
            }
        }

        return serviceNames;
    }

    private void deleteServices(
        RequestContext rc,
        ServiceManager ssm,
        List serviceNames, 
        SSOToken adminSSOToken,
        boolean continueFlag,
        IOutput outputWriter
    ) throws CLIException {
        for (Iterator i = serviceNames.iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            try {
                String[] param = {name};
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "ATTEMPT_DELETE_SERVICE", param);
                deleteService(rc, ssm, name, adminSSOToken);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_DELETE_SERVICE", param);
            } catch (CLIException e) {
                if (continueFlag) {
                    if (isVerbose()) {
                        outputWriter.printlnError(Debugger.getStackTrace(e));
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    private void deleteService(
        RequestContext rc,
        ServiceManager ssm,
        String serviceName, 
        SSOToken adminSSOToken
    ) throws CLIException {
        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                serviceName, adminSSOToken);

            if (scm.getGlobalConfig(null) != null) {
                scm.removeGlobalConfiguration(null);
            }

            ssm.deleteService(serviceName);
        } catch (SSOException e) {
            String[] args = {serviceName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_DELETE_SERVICE", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {serviceName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_DELETE_SERVICE", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
