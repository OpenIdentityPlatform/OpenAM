/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: UpdateMetadataKeyInfo.java,v 1.5 2009/10/29 00:03:50 exu Exp $
 *
 * Portions Copyrighted 2014-2015 ForgeRock AS.
 */

package com.sun.identity.federation.cli;

import static org.forgerock.openam.utils.CollectionUtils.*;

import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.meta.IDFFMetaSecurityUtils;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaSecurityUtils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import com.sun.identity.wsfederation.meta.WSFederationMetaSecurityUtils;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import org.apache.xml.security.encryption.XMLCipher;

import java.text.MessageFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Export Meta Data.
 */
public class UpdateMetadataKeyInfo extends AuthenticatedCommand {
    // constant to remove cert alias from entity
    private static final String NULL_ALIAS = "null";
    
    private String realm;
    private String entityID;
    private boolean sign;
    private List<String> spSigningAliases;
    private List<String> idpSigningAliases;
    private String attrqSigningAlias;
    private String attraSigningAlias;
    private String authnaSigningAlias;
    private String pepSigningAlias;
    private String pdpSigningAlias;
    private List<String> spEncryptionAliases;
    private List<String> idpEncryptionAliases;
    private String attrqEncryptionAlias;
    private String attraEncryptionAlias;
    private String authnaEncryptionAlias;
    private String pepEncryptionAlias;
    private String pdpEncryptionAlias;
    private boolean isWebBase;

    /**
     * Updates key information in metadata.
     * Both signing and encryption are supported for Service Provider,
     * Identuty Provider, Attribute Query Provider, Attribute Authority, 
     * Authentication Authority, XACML PEP and XACML PDP. 
     * Two information will be updated in this call:
     * 1. The signing/ecnryption alias in the extended metadata.
     * 2. The KeyDescriptor for signing/encryption in the extended metadata.
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
        spSigningAliases = rc.getOption(FedCLIConstants.ARGUMENT_SP_S_CERT_ALIAS);
        idpSigningAliases = rc.getOption(FedCLIConstants.ARGUMENT_IDP_S_CERT_ALIAS);
        spEncryptionAliases = rc.getOption(FedCLIConstants.ARGUMENT_SP_E_CERT_ALIAS);
        idpEncryptionAliases = rc.getOption(FedCLIConstants.ARGUMENT_IDP_E_CERT_ALIAS);
        /* TODO : handle other alias
        attrqSigningAlias = 
            getStringOptionValue(FedCLIConstants.ARGUMENT_ATTRQ_S_CERT_ALIAS);
        attraSigningAlias = 
            getStringOptionValue(FedCLIConstants.ARGUMENT_ATTRA_S_CERT_ALIAS);
        authnaSigningAlias = 
            getStringOptionValue(FedCLIConstants.ARGUMENT_AUTHNA_S_CERT_ALIAS);
        pepSigningAlias = 
            getStringOptionValue(FedCLIConstants.ARGUMENT_PEP_S_CERT_ALIAS);
        pdpSigningAlias = 
            getStringOptionValue(FedCLIConstants.ARGUMENT_PDP_S_CERT_ALIAS);
        attrqEncryptionAlias = 
            getStringOptionValue(FedCLIConstants.ARGUMENT_ATTRQ_E_CERT_ALIAS);
        attraEncryptionAlias = 
            getStringOptionValue(FedCLIConstants.ARGUMENT_ATTRA_E_CERT_ALIAS);
        authnaEncryptionAlias = 
            getStringOptionValue(FedCLIConstants.ARGUMENT_AUTHNA_E_CERT_ALIAS);
        pepEncryptionAlias = 
            getStringOptionValue(FedCLIConstants.ARGUMENT_PEP_E_CERT_ALIAS);
        pdpEncryptionAlias = 
            getStringOptionValue(FedCLIConstants.ARGUMENT_PDP_E_CERT_ALIAS);
        */

        validateOptions();

        String webURL = getCommandManager().getWebEnabledURL();
        isWebBase = (webURL != null) && (webURL.trim().length() > 0);

        String spec = FederationManager.getIDFFSubCommandSpecification(rc);

        String[] params = {realm, entityID, Objects.toString(spSigningAliases), Objects.toString(idpSigningAliases),
                Objects.toString(spEncryptionAliases), Objects.toString(idpEncryptionAliases), spec};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_UPDATE_ENTITY_KEYINFO", params);

        try {
            if (spec.equals(FederationManager.DEFAULT_SPECIFICATION)) {
                handleSAML2Request(rc);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_UPDATE_ENTITY_KEYINFO", params);
            } else if (spec.equals(FedCLIConstants.IDFF_SPECIFICATION)) {
                handleIDFFRequest(rc);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_UPDATE_ENTITY_KEYINFO", params);
            } else if (spec.equals(FedCLIConstants.WSFED_SPECIFICATION)) {
                handleWSFedRequest(rc);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_UPDATE_ENTITY_KEYINFO", params);
            } else {
                // TODO : need to support PEP/PDP/AuthnA/AttrQ/AttrA later
                throw new CLIException(
                    getResourceString("unsupported-specification"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        } catch (CLIException e) {
            String[] args = {realm, entityID, Objects.toString(spSigningAliases), Objects.toString(idpSigningAliases),
                    Objects.toString(spEncryptionAliases), Objects.toString(idpEncryptionAliases), spec, e.getMessage()};
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_UPDATE_ENTITY_KEYINFO", args);
            throw e;
        }
    }

    private void validateOptions() throws CLIException {
        if (isEmpty(idpSigningAliases) && isEmpty(spSigningAliases) && isEmpty(idpEncryptionAliases)
                && isEmpty(spEncryptionAliases)) {
            throw new CLIException(getResourceString("update-meta-keyinfo-exception-alias-null"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void handleSAML2Request(RequestContext rc) throws CLIException {
        try {
            SAML2MetaManager metaManager = new SAML2MetaManager(ssoToken);
            com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement descriptor =
                metaManager.getEntityDescriptor(realm, entityID);
            if (descriptor == null) {
                Object[] objs2 = {entityID, realm};
                throw new CLIException(MessageFormat.format(getResourceString(
                    "update-meta-keyinfo-exception-entity-not-exist"),
                    objs2), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            if (!isEmpty(spSigningAliases)) {
                if (NULL_ALIAS.equals(getFirstItem(spSigningAliases))) {
                    SAML2MetaSecurityUtils.updateProviderKeyInfo(
                        realm, entityID, null, true, false, null, 0);
                } else {
                    SAML2MetaSecurityUtils.updateProviderKeyInfo(
                        realm, entityID, new LinkedHashSet<>(spSigningAliases), true, false, null, 0);
                }
            } 
            if (!isEmpty(idpSigningAliases)) {
                if (NULL_ALIAS.equals(getFirstItem(idpSigningAliases))) {
                    SAML2MetaSecurityUtils.updateProviderKeyInfo(
                        realm, entityID, null, true, true, null, 0);
                } else {
                    SAML2MetaSecurityUtils.updateProviderKeyInfo(
                        realm, entityID, new LinkedHashSet<>(idpSigningAliases), true, true, null, 0);
                }
            }
            if (!isEmpty(spEncryptionAliases)) {
                if (NULL_ALIAS.equals(getFirstItem(spEncryptionAliases))) {
                    SAML2MetaSecurityUtils.updateProviderKeyInfo(realm,
                        entityID, null, false, false, 
                        XMLCipher.AES_128, 128);
                } else {
                    SAML2MetaSecurityUtils.updateProviderKeyInfo(realm,
                        entityID, new LinkedHashSet<>(spEncryptionAliases), false, false,
                        XMLCipher.AES_128, 128);
                }
            }
            if (!isEmpty(idpEncryptionAliases)) {
                if (NULL_ALIAS.equals(getFirstItem(idpEncryptionAliases))) {
                    SAML2MetaSecurityUtils.updateProviderKeyInfo(realm,
                        entityID, null, false, true,
                        XMLCipher.AES_128, 128);
                } else {
                    SAML2MetaSecurityUtils.updateProviderKeyInfo(realm,
                        entityID, new LinkedHashSet<>(idpEncryptionAliases), false, true,
                        XMLCipher.AES_128, 128);
                }
            }

            Object[] objs = { entityID };
            getOutputWriter().printlnMessage(MessageFormat.format(
                getResourceString("update-keyinfo-succeeded"), objs));

        } catch (SAML2Exception e) {
            SAML2MetaUtils.debug.error("UpdateMetaKey.handleSAML2Request", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
    
    private void handleIDFFRequest(RequestContext rc) throws CLIException {
        try {
            IDFFMetaManager metaManager = new IDFFMetaManager(ssoToken);
            com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement descriptor =
                metaManager.getEntityDescriptor(realm, entityID);
            if (descriptor == null) {
                Object[] objs2 = {entityID, realm};
                throw new CLIException(MessageFormat.format(getResourceString(
                    "update-meta-keyinfo-exception-entity-not-exist"),
                    objs2), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            if (!isEmpty(spSigningAliases)) {
                if (NULL_ALIAS.equals(getFirstItem(spSigningAliases))) {
                    IDFFMetaSecurityUtils.updateProviderKeyInfo(
                        realm, entityID, null, true, false, null, 0);
                } else {
                    IDFFMetaSecurityUtils.updateProviderKeyInfo(
                        realm, entityID, getFirstItem(spSigningAliases), true, false, null, 0);
                }
            } 
            if (!isEmpty(idpSigningAliases)) {
                if (NULL_ALIAS.equals(getFirstItem(idpSigningAliases))) {
                    IDFFMetaSecurityUtils.updateProviderKeyInfo(
                        realm, entityID, null, true, true, null, 0);
                } else {
                    IDFFMetaSecurityUtils.updateProviderKeyInfo(
                        realm, entityID, getFirstItem(idpSigningAliases), true, true, null, 0);
                }
            }
            if (!isEmpty(spEncryptionAliases)) {
                if (NULL_ALIAS.equals(getFirstItem(spEncryptionAliases))) {
                    IDFFMetaSecurityUtils.updateProviderKeyInfo(realm,
                        entityID, null, false, false, 
                        XMLCipher.AES_128, 128);
                } else {
                    IDFFMetaSecurityUtils.updateProviderKeyInfo(realm,
                        entityID, getFirstItem(spEncryptionAliases), false, false,
                        XMLCipher.AES_128, 128);
                }
            }
            if (!isEmpty(idpEncryptionAliases)) {
                if (NULL_ALIAS.equals(getFirstItem(idpEncryptionAliases))) {
                    IDFFMetaSecurityUtils.updateProviderKeyInfo(realm,
                        entityID, null, false, true,
                        XMLCipher.AES_128, 128);
                } else {
                    IDFFMetaSecurityUtils.updateProviderKeyInfo(realm,
                        entityID, getFirstItem(idpEncryptionAliases), false, true,
                        XMLCipher.AES_128, 128);
                }
            }

            Object[] objs = { entityID };
            getOutputWriter().printlnMessage(MessageFormat.format(
                getResourceString("update-keyinfo-succeeded"), objs));

        } catch (IDFFMetaException e) {
            IDFFMetaUtils.debug.error("UpdateMetaKey.handleIDFFRequest", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void handleWSFedRequest(RequestContext rc) throws CLIException {
        try {
            WSFederationMetaManager metaManager = new WSFederationMetaManager(
                ssoToken);
            FederationElement descriptor =
                metaManager.getEntityDescriptor(realm, entityID);
            if (descriptor == null) {
                Object[] objs2 = {entityID, realm};
                throw new CLIException(MessageFormat.format(getResourceString(
                    "update-meta-keyinfo-exception-entity-not-exist"),
                    objs2), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            if (!isEmpty(spSigningAliases)) {
                if (NULL_ALIAS.equals(getFirstItem(spSigningAliases))) {
                    WSFederationMetaSecurityUtils.updateProviderKeyInfo(
                        realm, entityID, null, false);
                } else {
                    WSFederationMetaSecurityUtils.updateProviderKeyInfo(
                        realm, entityID, getFirstItem(spSigningAliases), false);
                }
            } 
            if (!isEmpty(idpSigningAliases)) {
                if (NULL_ALIAS.equals(getFirstItem(idpSigningAliases))) {
                    WSFederationMetaSecurityUtils.updateProviderKeyInfo(
                        realm, entityID, null, true);
                } else {
                    WSFederationMetaSecurityUtils.updateProviderKeyInfo(
                        realm, entityID, getFirstItem(idpSigningAliases), true);
                }
            }
            if (!isEmpty(spEncryptionAliases)) {
                Object[] objs2 = {entityID, realm};
                throw new CLIException(MessageFormat.format(getResourceString(
                    "update-meta-keyinfo-exception-invalid-option"),
                    objs2), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            if (!isEmpty(idpEncryptionAliases)) {
                Object[] objs2 = {entityID, realm};
                throw new CLIException(MessageFormat.format(getResourceString(
                    "update-meta-keyinfo-exception-invalid-option"),
                    objs2), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }

            Object[] objs = { entityID };
            getOutputWriter().printlnMessage(MessageFormat.format(
                getResourceString("update-keyinfo-succeeded"), objs));

        } catch (WSFederationMetaException e) {
            WSFederationMetaUtils.debug.error(
                "UpdateMetaKey.handleIDFFRequest", e);
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }       
    }
}
