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
 * $Id: ExportMetaData.java,v 1.10 2009/10/29 00:03:50 exu Exp $
 *
 */

/**
 * Portions Copyrighted 2013 ForgeRock AS
 */
package com.sun.identity.federation.cli;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaConstants;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaSecurityUtils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import com.sun.identity.wsfederation.meta.WSFederationMetaSecurityUtils;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import javax.xml.bind.JAXBException;
import org.w3c.dom.Document;
import java.util.logging.Level;

/**
 * Export Meta Data.
 */
public class ExportMetaData extends AuthenticatedCommand {
    private String realm;
    private String entityID;
    private boolean sign;
    private String metadata;
    private String extendedData;
    private boolean isWebBase;

    /**
     * Exports Meta Data.
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
        entityID = getStringOptionValue(FedCLIConstants.ARGUMENT_ENTITY_ID);
        sign = isOptionSet(FedCLIConstants.ARGUMENT_SIGN);
        metadata = getStringOptionValue(FedCLIConstants.ARGUMENT_METADATA);
        extendedData = getStringOptionValue(
            FedCLIConstants.ARGUMENT_EXTENDED_DATA);
        String webURL = getCommandManager().getWebEnabledURL();
        isWebBase = (webURL != null) && (webURL.trim().length() > 0);

        String spec = FederationManager.getIDFFSubCommandSpecification(rc);
        String[] params = {realm, entityID, metadata, extendedData, spec};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_EXPORT_ENTITY", params);

        if ((metadata == null) && (extendedData == null)) {
            String[] args = {realm, entityID, metadata, extendedData,
                spec, getResourceString("export-entity-exception-no-datafile")};
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_EXPORT_ENTITY", args);
            throw new CLIException(
                getResourceString("export-entity-exception-no-datafile"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        try {
            if (spec.equals(FederationManager.DEFAULT_SPECIFICATION)) {
                handleSAML2Request(rc);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_EXPORT_ENTITY", params);
            } else if (spec.equals(FedCLIConstants.IDFF_SPECIFICATION)) {
                handleIDFFRequest(rc);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_EXPORT_ENTITY", params);
            } else if (spec.equals(FedCLIConstants.WSFED_SPECIFICATION)) {
                handleWSFedRequest(rc);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_EXPORT_ENTITY", params);
            } else {
                throw new CLIException(
                    getResourceString("unsupported-specification"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        } catch (CLIException e) {
            String[] args = {realm, entityID, metadata, extendedData,
                spec, e.getMessage()};
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_EXPORT_ENTITY", args);
            throw e;
        }
    }
    
    private void handleSAML2Request(RequestContext rc)
        throws CLIException {        
        if (metadata != null) {
            if (sign) {
                runExportMetaSign();
            } else {
                runExportMeta();
            }
        }
        
        if (extendedData != null) {
            runExportExtended();
        }
    }
    
    private void handleIDFFRequest(RequestContext rc)
        throws CLIException {   
        if (metadata != null) {
            if (sign) {
                runIDFFExportMetaSign();
            } else {
                runIDFFExportMeta();
            }
        }
        
        if (extendedData != null) {
            runIDFFExportExtended();
        }
    }

    private void handleWSFedRequest(RequestContext rc)
        throws CLIException {        
        if (metadata != null) {
            if (sign) {
                runWSFedExportMetaSign();
            } else {
                runWSFedExportMeta();
            }
        }
        
        if (extendedData != null) {
            runWSFedExportExtended();
        }
    }
    
    private void runExportMetaSign()
        throws CLIException
    {
        PrintWriter pw = null;
        String out = (isWebBase) ? "web" : metadata;
        Object[] objs = {out};

        try {
            SAML2MetaManager metaManager = new SAML2MetaManager(ssoToken);
            EntityDescriptorElement descriptor =
                metaManager.getEntityDescriptor(realm, entityID);
            
            if (descriptor == null) {
                Object[] objs2 = {entityID, realm};
                throw new CLIException(MessageFormat.format(
                    getResourceString(
                        "export-entity-exception-entity-descriptor-not-exist"),
                    objs2), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            
            Document doc = SAML2MetaSecurityUtils.sign(realm, descriptor);
            if (doc == null) {
                runExportMeta();
                return;
            } else {
                String xmlstr = XMLUtils.print(doc);
                xmlstr = workaroundAbstractRoleDescriptor(xmlstr);

                if (isWebBase) {
                    getOutputWriter().printlnMessage(xmlstr);
                } else {
                    pw = new PrintWriter(new FileWriter(metadata));
                    pw.print(xmlstr);
                }
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString(
                        "export-entity-export-descriptor-succeeded"), objs));
            }
        } catch (SAML2MetaException e) {
            debugError("ExportMetaData.runExportMetaSign", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException jaxbe) {
            Object[] objs3 = {entityID, realm};
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "export-entity-exception-invalid_descriptor"), objs3),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            debugError("ExportMetaData.runExportMetaSign", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (pw !=null ) {
                pw.close();
            }
        }
    }

    private void runIDFFExportMetaSign()
        throws CLIException
    {
        PrintWriter pw = null;
        String out = (isWebBase) ? "web" : metadata;
        Object[] objs = {out};

        try {
            IDFFMetaManager metaManager = new IDFFMetaManager(ssoToken);
            com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement
                descriptor = metaManager.getEntityDescriptor(
                    realm,entityID);
            
            if (descriptor == null) {
                Object[] objs2 = {entityID, realm};
                throw new CLIException(MessageFormat.format(
                    getResourceString(
                        "export-entity-exception-entity-descriptor-not-exist"),
                    objs2), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            
            SPDescriptorConfigElement spConfig = 
                metaManager.getSPDescriptorConfig(realm, entityID);
            IDPDescriptorConfigElement idpConfig = 
                metaManager.getIDPDescriptorConfig(realm, entityID);

            Document doc = null;
/*
 * TODO: Signing
 * Document doc = SAML2MetaSecurityUtils.sign(
                descriptor, spConfig, idpConfig);
 */
            if (doc == null) {
                runIDFFExportMeta();
                return;
            } else {
                String xmlstr = XMLUtils.print(doc);

                if (isWebBase) {
                    getOutputWriter().printlnMessage(xmlstr);
                } else {
                    pw = new PrintWriter(new FileWriter(metadata));
                    pw.print(xmlstr);
                }

                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString(
                        "export-entity-export-descriptor-succeeded"), objs));
            }
        } catch (IDFFMetaException e) {
            debugError("ExportMetaData.runIDFFExportMetaSign", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);

        } catch (IOException e) {
            debugError("ExportMetaData.runIDFFExportMetaSign", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (pw !=null ) {
                pw.close();
            }
        }
    }

    private void runWSFedExportMetaSign()
        throws CLIException
    {
        PrintWriter pw = null;
        String out = (isWebBase) ? "web" : metadata;
        Object[] objs = {out};

        try {
            WSFederationMetaManager metaManager = new WSFederationMetaManager(
                ssoToken);
            FederationElement descriptor =
                metaManager.getEntityDescriptor(realm, entityID);
            
            if (descriptor == null) {
                Object[] objs2 = {entityID, realm};
                throw new CLIException(MessageFormat.format(
                    getResourceString(
                        "export-entity-exception-entity-descriptor-not-exist"),
                    objs2), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            
            com.sun.identity.wsfederation.jaxb.entityconfig.SPSSOConfigElement 
                spConfig = metaManager.getSPSSOConfig(realm, 
                entityID);
            com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement 
                idpConfig = metaManager.getIDPSSOConfig(realm, 
                entityID);
            Document doc = WSFederationMetaSecurityUtils.sign(
                descriptor, spConfig, idpConfig);
            if (doc == null) {
                runWSFedExportMeta();
                return;
            } else {
                String xmlstr = XMLUtils.print(doc);

                if (isWebBase) {
                    getOutputWriter().printlnMessage(xmlstr);
                } else {
                    pw = new PrintWriter(new FileWriter(metadata));
                    pw.print(xmlstr);
                }
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString(
                        "export-entity-export-descriptor-succeeded"), objs));
            }
        } catch (WSFederationMetaException e) {
            debugError("ExportMetaData.runExportMetaSign", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException jaxbe) {
            Object[] objs3 = {entityID, realm};
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "export-entity-exception-invalid_descriptor"), objs3),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            debugError("ExportMetaData.runExportMetaSign", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (pw !=null ) {
                pw.close();
            }
        }
    }
    
    private void runExportMeta() 
        throws CLIException
    {
        PrintWriter pw = null;
        String out = (isWebBase) ? "web" : metadata;
        Object[] objs = {out};
        Object[] objs2 = {entityID, realm};
        
        try {
            SAML2MetaManager metaManager = new SAML2MetaManager(ssoToken);
            EntityDescriptorElement descriptor =
                metaManager.getEntityDescriptor(realm, entityID);
            if (descriptor == null) {
                throw new CLIException(MessageFormat.format(getResourceString(
                    "export-entity-exception-entity-descriptor-not-exist"),
                    objs2), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            
            String xmlstr = SAML2MetaUtils.convertJAXBToString(descriptor);
            xmlstr = workaroundAbstractRoleDescriptor(xmlstr);

            xmlstr = SAML2MetaSecurityUtils.formatBase64BinaryElement(xmlstr);

            if (isWebBase) {
                getOutputWriter().printlnMessage(xmlstr);
            } else {
                pw = new PrintWriter(new FileWriter(metadata));
                pw.print(xmlstr);
            }

            getOutputWriter().printlnMessage(MessageFormat.format(
                getResourceString(
                "export-entity-export-descriptor-succeeded"), objs));
        } catch (SAML2MetaException e) {
            debugError("ExportMetaData.runExportMeta", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            debugError("ExportMetaData.runExportMeta", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debugWarning("ExportMetaData.runExportMeta", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                "export-entity-exception-invalid_descriptor"), objs2),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debugWarning("ExportMetaData.runExportMeta", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                "export-entity-exception-invalid_descriptor"), objs2),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (pw !=null ) {
                pw.close();
            }
        }
    }
    
    private void runIDFFExportMeta() 
        throws CLIException
    {
        PrintWriter pw = null;
        String out = (isWebBase) ? "web" : metadata;
        Object[] objs = {out};
        Object[] objs2 = {entityID, realm};
        
        try {
            IDFFMetaManager metaManager = new IDFFMetaManager(ssoToken);
            com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement
                descriptor = metaManager.getEntityDescriptor(realm, entityID);
            if (descriptor == null) {
                throw new CLIException(MessageFormat.format(getResourceString(
                    "export-entity-exception-entity-descriptor-not-exist"),
                    objs2), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            
            String xmlstr = IDFFMetaUtils.convertJAXBToString(descriptor);
            xmlstr = SAML2MetaSecurityUtils.formatBase64BinaryElement(xmlstr);

            if (isWebBase) {
                getOutputWriter().printlnMessage(xmlstr);
            } else {
                pw = new PrintWriter(new FileWriter(metadata));
                pw.print(xmlstr);
            }
            getOutputWriter().printlnMessage(MessageFormat.format(
                getResourceString(
                "export-entity-export-descriptor-succeeded"), objs));
        } catch (IDFFMetaException e) {
            debugError("ExportMetaData.runIDFFExportMeta", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            debugError("ExportMetaData.runIDFFExportMeta", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debugWarning("ExportMetaData.runIDFFExportMeta", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                "export-entity-exception-invalid_descriptor"), objs2),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debugWarning("ExportMetaData.runExportMeta", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                "export-entity-exception-invalid_descriptor"), objs2),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (pw !=null ) {
                pw.close();
            }
        }
    }

    private void runWSFedExportMeta() 
        throws CLIException
    {
        PrintWriter pw = null;
        String out = (isWebBase) ? "web" : metadata;
        Object[] objs = {out};
        Object[] objs2 = {entityID, realm};
        
        try {
            WSFederationMetaManager metaManager = new WSFederationMetaManager(
                ssoToken);
            FederationElement federation =
                metaManager.getEntityDescriptor(realm, entityID);
            if (federation == null) {
                throw new CLIException(MessageFormat.format(getResourceString(
                    "export-entity-exception-entity-descriptor-not-exist"),
                    objs2), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            
            String xmlstr = 
                WSFederationMetaUtils.convertJAXBToString(federation);
            xmlstr = 
                WSFederationMetaSecurityUtils.formatBase64BinaryElement(xmlstr);

            if (isWebBase) {
                getOutputWriter().printlnMessage(xmlstr);
            } else {
                pw = new PrintWriter(new FileWriter(metadata));
                pw.print(xmlstr);
            }

            getOutputWriter().printlnMessage(MessageFormat.format(
                getResourceString(
                "export-entity-export-descriptor-succeeded"), objs));
        } catch (WSFederationMetaException e) {
            debugError("ExportMetaData.runExportMeta", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            debugError("ExportMetaData.runExportMeta", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debugWarning("ExportMetaData.runExportMeta", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                "export-entity-exception-invalid_descriptor"), objs2),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debugWarning("ExportMetaData.runExportMeta", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                "export-entity-exception-invalid_descriptor"), objs2),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (pw !=null ) {
                pw.close();
            }
        }
    }
    
    private void runExportExtended()
        throws CLIException
    {
        OutputStream os = null;
        String out = (isWebBase) ? "web" : extendedData;
        Object[] objs = {out};
        Object[] objs2 = {entityID, realm};
        
        try {
            SAML2MetaManager metaManager = new SAML2MetaManager(ssoToken);
            EntityConfigElement config =
                metaManager.getEntityConfig(realm, entityID);
            if (config == null) {
                throw new CLIException(MessageFormat.format(getResourceString(
                    "export-entity-exception-entity-config-not-exist"),
                    objs2), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            if (isWebBase) {
                os =  new ByteArrayOutputStream();
            } else {
                os = new FileOutputStream(extendedData);
            }

            SAML2MetaUtils.convertJAXBToOutputStream(config, os);

            if (isWebBase) {
                getOutputWriter().printlnMessage(os.toString());
            }

            getOutputWriter().printlnMessage(MessageFormat.format(
                getResourceString(
                "export-entity-export-config-succeeded"), objs));
        } catch (SAML2MetaException e) {
            debugError("ExportMetaData.runExportExtended", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (FileNotFoundException e) {
            debugWarning("ExportMetaData.runExportExtended", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debugWarning("ExportMetaData.runExportExtended", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debugWarning("ExportMetaData.runExportExtended", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                "export-entity-exception-invalid-config"), objs2),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (os !=null ) {
                try {
                    os.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private void runIDFFExportExtended()
        throws CLIException {
        OutputStream os = null;
        String out = (isWebBase) ? "web" : extendedData;
        Object[] objs = {out};
        Object[] objs2 = {entityID, realm};
        
        try {
            IDFFMetaManager metaManager = new IDFFMetaManager(ssoToken);
            com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement
                config = metaManager.getEntityConfig(realm, entityID);
            if (config == null) {
                throw new CLIException(MessageFormat.format(getResourceString(
                    "export-entity-exception-entity-config-not-exist"),
                    objs2), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            
            String xmlString = IDFFMetaUtils.convertJAXBToString(config);

            if (isWebBase) {
                getOutputWriter().printlnMessage(xmlString);
            } else {
                os = new FileOutputStream(extendedData);
                os.write(xmlString.getBytes());
            }

            getOutputWriter().printlnMessage(MessageFormat.format(
                getResourceString(
                "export-entity-export-config-succeeded"), objs));
        } catch (IDFFMetaException e) {
            debugWarning("ExportMetaData.runIDFFExportExtended", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            debugWarning("ExportMetaData.runIDFFExportExtended", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debugWarning("ExportMetaData.runIDFFExportExtended", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debugWarning("ExportMetaData.runIDFFExportExtended", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                "export-entity-exception-invalid-config"), objs2),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (os !=null ) {
                try {
                    os.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
    
    private void runWSFedExportExtended()
        throws CLIException
    {
        OutputStream os = null;
        String out = (isWebBase) ? "web" : extendedData;
        Object[] objs = {out};
        Object[] objs2 = {entityID, realm};
        
        try {
            WSFederationMetaManager metaManager = new WSFederationMetaManager(
                ssoToken);
            com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement 
                config = metaManager.getEntityConfig(realm, entityID);
            if (config == null) {
                throw new CLIException(MessageFormat.format(getResourceString(
                    "export-entity-exception-entity-config-not-exist"),
                    objs2), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            if (isWebBase) {
                os =  new ByteArrayOutputStream();
            } else {
                os = new FileOutputStream(extendedData);
            }

            WSFederationMetaUtils.convertJAXBToOutputStream(config, os);

            if (isWebBase) {
                getOutputWriter().printlnMessage(os.toString());
            }

            getOutputWriter().printlnMessage(MessageFormat.format(
                getResourceString(
                "export-entity-export-config-succeeded"), objs));
        } catch (WSFederationMetaException e) {
            debugError("ExportMetaData.runExportExtended", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (FileNotFoundException e) {
            debugWarning("ExportMetaData.runExportExtended", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debugWarning("ExportMetaData.runExportExtended", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debugWarning("ExportMetaData.runExportExtended", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                "export-entity-exception-invalid-config"), objs2),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (os !=null ) {
                try {
                    os.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private static String workaroundAbstractRoleDescriptor(String xmlstr) {
        int index =
            xmlstr.indexOf(":" +SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR);
        if (index == -1) {
            return xmlstr;
        }

        int index2 = xmlstr.lastIndexOf("<", index);
        if (index2 == -1) {
            return xmlstr;
        }

        String prefix = xmlstr.substring(index2 + 1, index);
        String type =  prefix + ":" +
            SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR_TYPE;

        xmlstr = xmlstr.replaceAll("<" + prefix + ":" +
            SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR,
            "<" + SAML2MetaConstants.ROLE_DESCRIPTOR + " " +
            SAML2Constants.XSI_DECLARE_STR + " xsi:type=\"" + type + "\"");
        xmlstr = xmlstr.replaceAll("</" + prefix + ":" +
            SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR,
            "</" + SAML2MetaConstants.ROLE_DESCRIPTOR);
        return xmlstr;
    }


}
