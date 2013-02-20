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
 * $Id: ImportMetaData.java,v 1.15 2009/10/29 00:03:50 exu Exp $
 *
 */

 /*
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.federation.cli;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.COTException;
import com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.w3c.dom.Document;

/**
 * Import Meta Data.
 */
public class ImportMetaData extends AuthenticatedCommand {
    private String metadata;
    private String extendedData;
    private String cot;
    private String realm;
    private String spec;
    private boolean webAccess;

    /**
     * Imports Meta Data.
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
        metadata = getStringOptionValue(FedCLIConstants.ARGUMENT_METADATA);
        extendedData = getStringOptionValue(
            FedCLIConstants.ARGUMENT_EXTENDED_DATA);
        cot = getStringOptionValue(FedCLIConstants.ARGUMENT_COT);

        spec = FederationManager.getIDFFSubCommandSpecification(rc);
        String[] params = {realm, metadata, extendedData, cot, spec};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_IMPORT_ENTITY", params);

        if ((metadata == null) && (extendedData == null)) {
            String[] args = {realm, metadata, extendedData, cot,
                spec, getResourceString("import-entity-exception-no-datafile")};
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IMPORT_ENTITY", args);
            throw new CLIException(
                getResourceString("import-entity-exception-no-datafile"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        validateCOT();

        CommandManager mgr = getCommandManager();
        String url = mgr.getWebEnabledURL();
        webAccess = (url != null) && (url.length() > 0);

        try {
            if (spec.equals(FederationManager.DEFAULT_SPECIFICATION)) {
                handleSAML2Request(rc);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_IMPORT_ENTITY", params);
            } else if (spec.equals(FedCLIConstants.IDFF_SPECIFICATION)) {
                handleIDFFRequest(rc);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_IMPORT_ENTITY", params);
            } else if (spec.equals(FedCLIConstants.WSFED_SPECIFICATION)) {
                handleWSFedRequest(rc);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_IMPORT_ENTITY", params);
            } else {
                throw new CLIException(
                    getResourceString("unsupported-specification"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        } catch (CLIException e) {
            String[] args = {realm, metadata, extendedData, cot,
                spec, e.getMessage()};
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IMPORT_ENTITY", args);
            throw e;
        }
    }


    private void validateCOT()
        throws CLIException {
        if ((cot != null) && (cot.length() > 0))  {
            try {
                CircleOfTrustManager cotManager = new CircleOfTrustManager(
                    ssoToken);
                if (!cotManager.getAllCirclesOfTrust(realm).contains(cot)) {
                    String[] args = {realm, metadata, extendedData, cot,
                        spec,
                        getResourceString(
                        "import-entity-exception-cot-no-exist")
                    };
                    writeLog(LogWriter.LOG_ERROR, Level.INFO,
                        "FAILED_IMPORT_ENTITY", args);
                    throw new CLIException(
                        getResourceString(
                        "import-entity-exception-cot-no-exist"),
                        ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                }
            } catch (COTException e) {
                throw new CLIException(e.getMessage(),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        }
    }

    private void handleSAML2Request(RequestContext rc)
        throws CLIException {
        try {
            SAML2MetaManager metaManager = new SAML2MetaManager(ssoToken);
            EntityConfigElement configElt = null;

            if (extendedData != null) {
                configElt = geEntityConfigElement();
                /*
                 * see note at the end of this class for how we decide
                 * the realm value
                 */
                if (configElt != null && configElt.isHosted()) {
                    List config = configElt.
                       getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
                    if (!config.isEmpty()) {
                        BaseConfigType bConfig = (BaseConfigType)
                            config.iterator().next();
                        realm = SAML2MetaUtils.getRealmByMetaAlias(
                            bConfig.getMetaAlias());
                    }
                }
            }

            List<String> entityIds = null;
            // Load the metadata if it has been provided
            if (metadata != null) {
                entityIds = importSAML2Metadata(metaManager);
            }
            // Load the extended metadata if it has been provided
            if (configElt != null) {
                metaManager.createEntityConfig(realm, configElt);
            }

            if (entityIds != null) {
                String out = (webAccess) ? "web" : metadata;
                Object[] objs = { out };
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString("import-entity-succeeded"), objs));
            }
            if (configElt != null) {
                String out = (webAccess) ? "web" : extendedData;
                Object[] objs = { out };
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString("import-entity-succeeded"), objs));
            }

            if ((cot != null) && (cot.length() > 0) &&
                (entityIds != null) && (!entityIds.isEmpty())) {
                CircleOfTrustManager cotManager = new CircleOfTrustManager(
                    ssoToken);
                for (String entityID : entityIds) {
                    if (!cotManager.isInCircleOfTrust(realm, cot, spec, entityID)) {
                        cotManager.addCircleOfTrustMember(
                            realm, cot, spec, entityID);
                    }
                }
            }
        } catch (COTException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SAML2MetaException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void handleIDFFRequest(RequestContext rc)
        throws CLIException {
        try {
            IDFFMetaManager metaManager = new IDFFMetaManager(ssoToken);
            String entityID = null;
            com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement
                configElt = null;

            if (extendedData != null) {
                configElt = getIDFFEntityConfigElement();

                /*
                 * see note at the end of this class for how we decide
                 * the realm value
                 */
                if ((configElt != null) && configElt.isHosted()) {
                    IDPDescriptorConfigElement idpConfig =
                        IDFFMetaUtils.getIDPDescriptorConfig(configElt);
                    if (idpConfig != null) {
                        realm = SAML2MetaUtils.getRealmByMetaAlias(
                            idpConfig.getMetaAlias());
                    } else {
                        SPDescriptorConfigElement spConfig =
                            IDFFMetaUtils.getSPDescriptorConfig(configElt);
                        if (spConfig != null) {
                            realm = SAML2MetaUtils.getRealmByMetaAlias(
                                spConfig.getMetaAlias());
                        }
                    }
                }
            }

            if (metadata != null) {
                entityID = importIDFFMetaData(realm, metaManager);
            }
            if (configElt != null) {
                String out = (webAccess) ? "web" : extendedData;
                Object[] objs = { out };
                metaManager.createEntityConfig(realm, configElt);
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString("import-entity-succeeded"), objs));
            }

            if ((cot != null) && (cot.length() > 0) &&
                (entityID != null) && (entityID.length() > 0)) {
                CircleOfTrustManager cotManager = new CircleOfTrustManager(
                    ssoToken);
                if (!cotManager.isInCircleOfTrust(realm, cot, spec, entityID)) {
                    cotManager.addCircleOfTrustMember(realm, cot, spec,
                        entityID);
                }
            }
        } catch (IDFFMetaException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (COTException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void handleWSFedRequest(RequestContext rc)
        throws CLIException {
        try {
            String federationID = null;
            com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement
                configElt = null;

            if (extendedData != null) {
                configElt = getWSFedEntityConfigElement();
                /*
                 * see note at the end of this class for how we decide
                 * the realm value
                 */
                if (configElt != null && configElt.isHosted()) {
                    List config = configElt.
                       getIDPSSOConfigOrSPSSOConfig();
                    if (!config.isEmpty()) {
                        com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType
                            bConfig =
                            (com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType)
                            config.iterator().next();
                        realm = WSFederationMetaUtils.getRealmByMetaAlias(
                            bConfig.getMetaAlias());
                    }
                }
            }

            WSFederationMetaManager metaManager = new WSFederationMetaManager(
                ssoToken);
            if (metadata != null) {
                federationID = importWSFedMetaData();
            }

            if (configElt != null) {
                metaManager.createEntityConfig(realm, configElt);

                String out = (webAccess) ? "web" : extendedData;
                Object[] objs = { out };
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString("import-entity-succeeded"), objs));
            }

            if ((cot != null) && (cot.length() > 0) &&
                (federationID != null)) {
                CircleOfTrustManager cotManager = new CircleOfTrustManager(
                    ssoToken);
                if (!cotManager.isInCircleOfTrust(realm, cot, spec,
                    federationID)
                ) {
                    cotManager.addCircleOfTrustMember(realm, cot, spec,
                        federationID);
                }
            }
        } catch (COTException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (WSFederationMetaException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private List<String> importSAML2Metadata(
        SAML2MetaManager metaManager)
        throws SAML2MetaException, CLIException {

        List<String> result = null;

        InputStream is = null;
        String out = (webAccess) ? "web" : metadata;
        Object[] objs = { out };

        try {
            Document doc;
            Debug debug = CommandManager.getDebugger();

            if (webAccess) {
                doc = XMLUtils.toDOMDocument(metadata, debug);
            } else {
                is = new FileInputStream(metadata);
                doc = XMLUtils.toDOMDocument(is, debug);
            }

            if (doc == null) {
                throw new CLIException(MessageFormat.format(
                    getResourceString(
                    "import-entity-exception-invalid-descriptor-file"),
                    objs), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }

            result = SAML2MetaUtils.importSAML2Document(metaManager, realm, doc);

            if (result.isEmpty()) {
                throw new CLIException(MessageFormat.format(
                    getResourceString(
                    "import-entity-exception-invalid-descriptor-file"),
                    objs), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }

            return result;
        } catch (FileNotFoundException e) {
            throw new CLIException(MessageFormat.format(
                getResourceString("file-not-found"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debugWarning("ImportMetaData.importMetaData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-descriptor-file"),
                objs), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debugWarning("ImportMetaData.importMetaData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-descriptor-file"),
                objs), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (is !=null ) {
                try {
                    is.close();
                } catch (IOException e) {
                    //do not if the file cannot be closed.
                }
            }
        }
    }

    private String importIDFFMetaData(String realm, IDFFMetaManager metaManager)
        throws IDFFMetaException, CLIException
    {
        InputStream is = null;
        String out = (webAccess) ? "web" : metadata;
        Object[] objs = { out };
        String entityID = null;

        try {
            Object obj;
            if (webAccess) {
                obj = IDFFMetaUtils.convertStringToJAXB(metadata);
            } else {
                is = new FileInputStream(metadata);
                Document doc = XMLUtils.toDOMDocument(is,
                    CommandManager.getDebugger());
                obj = IDFFMetaUtils.convertNodeToJAXB(doc);
            }

            if (obj instanceof
                com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement) {
                com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement
                    descriptor =
                 (com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement)
                    obj;
                entityID = descriptor.getProviderID();
                //TODO: signature
                //SAML2MetaSecurityUtils.verifySignature(doc);
                //
                metaManager.createEntityDescriptor(realm, descriptor);
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString("import-entity-succeeded"), objs));
            }
            return entityID;
        } catch (FileNotFoundException e) {
            throw new CLIException(MessageFormat.format(
                getResourceString("file-not-found"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debugWarning("ImportMetaData.importMetaData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-descriptor-file"),
                objs), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debugWarning("ImportMetaData.importMetaData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-descriptor-file"),
                objs), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (is !=null ) {
                try {
                    is.close();
                } catch (IOException e) {
                    //do not if the file cannot be closed.
                }
            }
        }
    }

    private String importWSFedMetaData()
        throws WSFederationMetaException, CLIException
    {
        InputStream is = null;
        String out = (webAccess) ? "web" : metadata;
        Object[] objs = { out };
        String federationID = null;

        try {
            Object obj;
            Document doc;
            if (webAccess) {
                obj = WSFederationMetaUtils.convertStringToJAXB(metadata);
                doc = XMLUtils.toDOMDocument(metadata,
                    CommandManager.getDebugger());
            } else {
                is = new FileInputStream(metadata);
                doc = XMLUtils.toDOMDocument(is, CommandManager.getDebugger());
                obj = WSFederationMetaUtils.convertNodeToJAXB(doc);
            }

            if (obj instanceof com.sun.identity.wsfederation.jaxb.wsfederation.FederationMetadataElement) {
                // Just get the first element for now...
                // TODO - loop through Federation elements?
                obj = ((com.sun.identity.wsfederation.jaxb.wsfederation.FederationMetadataElement)obj).getAny().get(0);
            }

            if (obj instanceof com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement) {
                com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement
                federation =
                (com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement)obj;
                federationID = federation.getFederationID();
                if ( federationID == null )
                {
                    federationID = WSFederationConstants.DEFAULT_FEDERATION_ID;
                }
                // WSFederationMetaSecurityUtils.verifySignature(doc);
                WSFederationMetaManager metaManager = new
                    WSFederationMetaManager(ssoToken);
                metaManager.createFederation(realm, federation);
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString("import-entity-succeeded"), objs));
            }
            return federationID;
        } catch (FileNotFoundException e) {
            throw new CLIException(MessageFormat.format(
                getResourceString("file-not-found"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debugWarning("ImportMetaData.importMetaData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-descriptor-file"),
                objs), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debugWarning("ImportMetaData.importMetaData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-descriptor-file"),
                objs), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (is !=null ) {
                try {
                    is.close();
                } catch (IOException e) {
                    //do not if the file cannot be closed.
                }
            }
        }
    }

    private EntityConfigElement geEntityConfigElement()
        throws SAML2MetaException, CLIException {
        String out = (webAccess) ? "web" : extendedData;
        Object[] objs = { out };
        InputStream is = null;

        try {
            Object obj = null;
            if (webAccess) {
                obj = SAML2MetaUtils.convertStringToJAXB(extendedData);
            } else {
                is = new FileInputStream(extendedData);
                obj = SAML2MetaUtils.convertInputStreamToJAXB(is);
            }

            return (obj instanceof EntityConfigElement) ?
                (EntityConfigElement)obj : null;
        } catch (FileNotFoundException e) {
            throw new CLIException(MessageFormat.format(
                getResourceString("file-not-found"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debugWarning("ImportMetaData.importExtendedData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-config-file"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debugWarning("ImportMetaData.importExtendedData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-config-file"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (is !=null ) {
                try {
                    is.close();
                } catch (IOException e) {
                    //do not if the file cannot be closed.
                }
            }
        }
    }

    private com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement
        getIDFFEntityConfigElement() throws IDFFMetaException, CLIException {
        String out = (webAccess) ? "web" : extendedData;
        Object[] objs = { out };

        try {
            Object obj;

            if (webAccess) {
                obj = IDFFMetaUtils.convertStringToJAXB(extendedData);
            } else {
                obj = IDFFMetaUtils.convertStringToJAXB(
                    getFileContent(extendedData));
            }

            return (obj instanceof
            com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement) ?
             (com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement)
                obj : null;
        } catch (IOException e) {
            throw new CLIException(MessageFormat.format(
                getResourceString("file-not-found"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debugWarning("ImportMetaData.importExtendedData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-config-file"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debugWarning("ImportMetaData.importExtendedData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-config-file"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement
        getWSFedEntityConfigElement()
        throws WSFederationMetaException, CLIException {
        String out = (webAccess) ? "web" : extendedData;
        Object[] objs = { out };
        InputStream is = null;

        try {
            Object obj = null;
            if (webAccess) {
                obj = WSFederationMetaUtils.convertStringToJAXB(extendedData);
            } else {
                is = new FileInputStream(extendedData);
                obj = WSFederationMetaUtils.convertInputStreamToJAXB(is);
            }

            return (obj instanceof
                com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement) ?
                (com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement)obj :
                null;
        } catch (FileNotFoundException e) {
            throw new CLIException(MessageFormat.format(
                getResourceString("file-not-found"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            debugWarning("ImportMetaData.importExtendedData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-config-file"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalArgumentException e) {
            debugWarning("ImportMetaData.importExtendedData", e);
            throw new CLIException(MessageFormat.format(
                getResourceString(
                    "import-entity-exception-invalid-config-file"), objs),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (is !=null ) {
                try {
                    is.close();
                } catch (IOException e) {
                    //do not if the file cannot be closed.
                }
            }
        }
    }

    private static String getFileContent(String fileName)
        throws IOException {
        BufferedReader br = null;
        StringBuffer buff = new StringBuffer();
        try {
            br = new BufferedReader(new FileReader(fileName));
            String line = br.readLine();
            while (line != null) {
                buff.append(line).append("\n");
                line = br.readLine();
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
        return buff.toString();
    }
}

/* Deciding realm value
if (extended metadata xml exists) {
    if (hosted) {
        get the realm value from meta alias either from IDP or SP
        config element.
    } else {
        use the value provide by --realm/-e option
    }
} else {
    use the value provide by --realm/-e option
}
 */
