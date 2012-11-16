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
 * $Id: BulkFederation.java,v 1.5 2009/10/29 00:03:50 exu Exp $
 *
 */

package com.sun.identity.federation.cli;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfo;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfoKey;
import com.sun.identity.federation.accountmgmt.FSAccountMgmtException;
import com.sun.identity.federation.accountmgmt.FSAccountUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.NameIDInfo;
import com.sun.identity.saml2.common.NameIDInfoKey;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.shared.encode.Base64;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Do bulk federation. This is the first step of the 2 step process.
 * It talks a file that contains local user Id to remote user Id mapping.
 * And generate a file that contains remote user Id to name Id mapping.
 */
public class BulkFederation extends AuthenticatedCommand {
    static final String ARGUMENT_METADATA = "metaalias";
    static final String ARGUMENT_REMOTE_ID = "remoteentityid";
    static final String ARGUMENT_USER_ID_MAPPING = "useridmapping";
    static final String ARGUMENT_NAME_ID_MAPPING = "nameidmapping";
    
    static final String HEADER_LOCAL = "#local:";
    static final String HEADER_REMOTE = "#remote:";
    static final String HEADER_ROLE = "#role:";
    static final String HEADER_SPEC = "#specification:";
    
    private SecureRandom randomGenerator = new SecureRandom();
    private String metaAlias;
    private String localEntityId;
    private String remoteEntityId;
    private boolean isIDP;
    private String userIdMappingFileName;
    private String outFile;
    private String spec;
    
    static Set idffUserAttributesFed = new HashSet(4);
    static Set saml2UserAttributesFed = new HashSet(4);

    static {
        idffUserAttributesFed.add(FSAccountUtils.USER_FED_INFO_KEY_ATTR);
        idffUserAttributesFed.add(FSAccountUtils.USER_FED_INFO_ATTR);
        saml2UserAttributesFed.add(SAML2Constants.NAMEID_INFO_KEY);
        saml2UserAttributesFed.add(SAML2Constants.NAMEID_INFO);
    }
    
    /**
     * Does bulk federation.
     *
     * @param rc Request Context.
     * @throws CLIException if unable to process this request.
     */
    @Override
    public void handleRequest(RequestContext rc)
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();

        metaAlias = getStringOptionValue(ARGUMENT_METADATA);
        remoteEntityId = getStringOptionValue(ARGUMENT_REMOTE_ID);
        userIdMappingFileName = getStringOptionValue(
            ARGUMENT_USER_ID_MAPPING);
        outFile = getStringOptionValue(ARGUMENT_NAME_ID_MAPPING);
        spec = FederationManager.getIDFFSubCommandSpecification(rc);
        BufferedWriter out = null;

        String[] params = {metaAlias, remoteEntityId, userIdMappingFileName,
             outFile, spec};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_DO_BULK_FEDERATION", params);
        
        try {
            if (spec.equals(FederationManager.DEFAULT_SPECIFICATION)) {
                getEntityRoleAndIdSAML2();
                out = validateFiles();
                handleSAML2Request(out);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_DO_BULK_FEDERATION", params);
            } else if (spec.equals(FedCLIConstants.IDFF_SPECIFICATION)) {
                getEntityRoleAndIdIDFF();
                out = validateFiles();
                handleIDFFRequest(out);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_DO_BULK_FEDERATION", params);
            } else {
                throw new CLIException(
                    getResourceString("unsupported-specification"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        } catch (CLIException e) {
            String[] args = {metaAlias, remoteEntityId, 
                userIdMappingFileName, outFile, spec, e.getMessage()};
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_DO_BULK_FEDERATION", args);
            throw e;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    //ignored
                }
            }
        }  
    }
    
    private void handleSAML2Request(BufferedWriter out)
        throws CLIException {
        Map userIdMapping = getUserIdMapping(userIdMappingFileName);
        for (Iterator i = userIdMapping.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry)i.next();
            String localUserId = (String)e.getKey();
            String remoteUserId = (String)e.getValue();
            saml2FederateUser(localUserId, remoteUserId, out);
        }
        IOutput outputWriter = getOutputWriter();
        outputWriter.printlnMessage(getResourceString(
            "bulk-federation-succeeded"));
    }
    
    private void handleIDFFRequest(BufferedWriter out)
        throws CLIException {
        Map userIdMapping = getUserIdMapping(userIdMappingFileName);
        for (Iterator i = userIdMapping.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry)i.next();
            String localUserId = (String)e.getKey();
            String remoteUserId = (String)e.getValue();
            idffFederateUser(localUserId, remoteUserId, out);
        }
        IOutput outputWriter = getOutputWriter();
        outputWriter.printlnMessage(getResourceString(
            "bulk-federation-succeeded"));
    }

    private void idffFederateUser(
        String localUserId, 
        String remoteUserId,
        BufferedWriter out
    ) throws CLIException {
        SSOToken adminSSOToken = getAdminSSOToken();
        try {
            AMIdentity amid = IdUtils.getIdentity(adminSSOToken, localUserId);
            String nameId = createNameIdentifier();
            FSAccountFedInfoKey key = (isIDP) ?
                new FSAccountFedInfoKey(remoteEntityId, nameId) :
                new FSAccountFedInfoKey(localEntityId, nameId);
            FSAccountFedInfo info = null;
            
            if (isIDP) {
                info = new FSAccountFedInfo(remoteEntityId,
                    new NameIdentifier(nameId, remoteEntityId, 
                    IFSConstants.NI_FEDERATED_FORMAT_URI),
                    IFSConstants.LOCAL_NAME_IDENTIFIER, true);
            } else {
                info = new FSAccountFedInfo(remoteEntityId,
                    new NameIdentifier(nameId, localEntityId, 
                    IFSConstants.NI_FEDERATED_FORMAT_URI),
                    IFSConstants.REMOTE_NAME_IDENTIFIER, true);
            }

            Map attributes = amid.getAttributes(idffUserAttributesFed);

            Set setInfoKey = (Set)attributes.get(
                FSAccountUtils.USER_FED_INFO_KEY_ATTR);
            if ((setInfoKey == null) || setInfoKey.isEmpty()) {
                setInfoKey = new HashSet(2);
                attributes.put(FSAccountUtils.USER_FED_INFO_KEY_ATTR,
                    setInfoKey);
            }
            setInfoKey.add(FSAccountUtils.objectToKeyString(key));

            Set setInfo = (Set)attributes.get(
                FSAccountUtils.USER_FED_INFO_ATTR);
            if ((setInfo == null) || setInfo.isEmpty()) {
                setInfo = new HashSet(2);
                attributes.put(FSAccountUtils.USER_FED_INFO_ATTR, setInfo);
            }
            setInfo.add(FSAccountUtils.objectToInfoString(info));

            amid.setAttributes(attributes);
            amid.store();
            out.write(remoteUserId + "|" + nameId);
            out.newLine();
        } catch (FSAccountMgmtException e) {
            debugError("BulkFederation.idffFederateUser", e);
            Object[] param = {localUserId};
            throw new CLIException(MessageFormat.format(
                getResourceString("bulk-federation-cannot-federate"),
                param), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SAMLException e) {
            debugError("BulkFederation.idffFederateUser", e);
            Object[] param = {localUserId};
            throw new CLIException(MessageFormat.format(
                getResourceString("bulk-federation-cannot-federate"),
                param), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            debugError("BulkFederation.idffFederateUser", e);
            Object[] param = {localUserId};
            throw new CLIException(MessageFormat.format(
                getResourceString("bulk-federation-cannot-federate"),
                param), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IdRepoException e) {
            debugError("BulkFederation.idffFederateUser", e);
            IOutput outputWriter = getOutputWriter();
            outputWriter.printlnError(e.getMessage());
        } catch (SSOException e) {
            debugError("BulkFederation.idffFederateUser", e);
            IOutput outputWriter = getOutputWriter();
            outputWriter.printlnError(e.getMessage());
        }
    }

    private void saml2FederateUser(
        String localUserId, 
        String remoteUserId,
        BufferedWriter out
    ) throws CLIException {
        SSOToken adminSSOToken = getAdminSSOToken();
        try {
            AMIdentity amid = IdUtils.getIdentity(adminSSOToken, localUserId);
            String nameIdValue = createNameIdentifier();
            NameID nameId = AssertionFactory.getInstance().createNameID();
            nameId.setFormat(
                "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"); 
            if (isIDP) {
                nameId.setNameQualifier(localEntityId);
                nameId.setSPNameQualifier(remoteEntityId);
            } else {
                nameId.setNameQualifier(remoteEntityId);
                nameId.setSPNameQualifier(localEntityId);
            }
            nameId.setValue(nameIdValue);
            String role = (isIDP) ? SAML2Constants.IDP_ROLE : 
                SAML2Constants.SP_ROLE;
            
            NameIDInfoKey key = new NameIDInfoKey(nameIdValue,
                localEntityId, remoteEntityId);
            NameIDInfo info = new NameIDInfo(localEntityId, remoteEntityId,
                nameId, role, true);

            Map attributes = amid.getAttributes(saml2UserAttributesFed);

            Set setInfoKey = (Set)attributes.get(
                SAML2Constants.NAMEID_INFO_KEY);
            if ((setInfoKey == null) || setInfoKey.isEmpty()) {
                setInfoKey = new HashSet(2);
                attributes.put(SAML2Constants.NAMEID_INFO_KEY, setInfoKey);
            }
            setInfoKey.add(key.toValueString());

            Set setInfo = (Set)attributes.get(
                SAML2Constants.NAMEID_INFO);
            if ((setInfo == null) || setInfo.isEmpty()) {
                setInfo = new HashSet(2);
                attributes.put(SAML2Constants.NAMEID_INFO, setInfo);
            }
            setInfo.add(info.toValueString());

            amid.setAttributes(attributes);
            amid.store();
            out.write(remoteUserId + "|" + nameIdValue);
            out.newLine();
        } catch (SAML2Exception e) {
            debugError("BulkFederation.saml2FederateUser", e);
            Object[] param = {localUserId};
            throw new CLIException(MessageFormat.format(
                getResourceString("bulk-federation-cannot-federate"),
                param), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            debugError("BulkFederation.saml2FederateUser", e);
            Object[] param = {localUserId};
            throw new CLIException(MessageFormat.format(
                getResourceString("bulk-federation-cannot-federate"),
                param), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IdRepoException e) {
            debugError("BulkFederation.saml2FederateUser", e);
            IOutput outputWriter = getOutputWriter();
            outputWriter.printlnError(e.getMessage());
        } catch (SSOException e) {
            debugError("BulkFederation.saml2FederateUser", e);
            IOutput outputWriter = getOutputWriter();
            outputWriter.printlnError(e.getMessage());
        }
    }

    private void getEntityRoleAndIdIDFF() 
        throws CLIException {
        try {
            IDFFMetaManager idffMgr = new IDFFMetaManager(ssoToken);
            String role = idffMgr.getProviderRoleByMetaAlias(metaAlias);
            if (role == null) {
                Object[] param = {metaAlias};
                throw new CLIException(MessageFormat.format(
                    getResourceString("bulk-federation-unknown-metaalias"),
                    param), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            isIDP = role.equals(IFSConstants.IDP);
            localEntityId = idffMgr.getEntityIDByMetaAlias(metaAlias);
        } catch (IDFFMetaException e) {
            debugError("BulkFederation.getEntityRoleAndIdIDFF", e);
            Object[] param = {metaAlias};
            throw new CLIException(MessageFormat.format(
                getResourceString("bulk-federation-unknown-metaalias"),
                param), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
    
    private void getEntityRoleAndIdSAML2() 
        throws CLIException {
        try {
            SAML2MetaManager saml2Mgr = new SAML2MetaManager(ssoToken);
            String role = saml2Mgr.getRoleByMetaAlias(metaAlias);
            if (role.equals(SAML2Constants.UNKNOWN_ROLE)) {
                Object[] param = {metaAlias};
                throw new CLIException(MessageFormat.format(
                    getResourceString("bulk-federation-unknown-metaalias"),
                    param), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            isIDP = role.equals(SAML2Constants.IDP_ROLE);
            localEntityId = saml2Mgr.getEntityByMetaAlias(metaAlias);
        } catch (SAML2MetaException e) {
            debugError("BulkFederation.getEntityRoleAndIdSAML2", e);
            Object[] param = {metaAlias};
            throw new CLIException(MessageFormat.format(
                getResourceString("bulk-federation-unknown-metaalias"),
                param), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }    
    
    /*
     * Checks if the input file exists
     * Checks if the output file is writable
     * Returns a handle to the output file writer.
     */
    private BufferedWriter validateFiles() throws CLIException {
        File input = new File(userIdMappingFileName);
        if (!input.exists()) {
            Object[] param = {userIdMappingFileName};
            throw new CLIException(MessageFormat.format(
                getResourceString("bulk-federation-infile-do-not-exists"),
                param), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
        File output = new File(outFile);
        if (output.exists()) {
            Object[] param = {outFile};
            throw new CLIException(MessageFormat.format(
                getResourceString("bulk-federation-outfile-exists"), param),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        try {
            output.createNewFile();
            if (!output.canWrite()) {
                Object[] param = {outFile};
                throw new CLIException(MessageFormat.format(
                    getResourceString("bulk-federation-outfile-cannot-write"),
                    param), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            BufferedWriter out = new BufferedWriter(new FileWriter(output));
            out.write(HEADER_LOCAL + localEntityId);
            out.newLine();
            out.write(HEADER_REMOTE + remoteEntityId);
            out.newLine();
            String role = (isIDP) ? "IDP" : "SP";
            out.write(HEADER_ROLE + role);
            out.newLine();
            
            if (spec.equals(FedCLIConstants.IDFF_SPECIFICATION)) {
                out.write(HEADER_SPEC + FedCLIConstants.IDFF_SPECIFICATION);
            } else {
                out.write(HEADER_SPEC + FedCLIConstants.SAML2_SPECIFICATION);
            }
            out.newLine();
            return out;
        } catch (IOException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    /*
     * Expecting localUserId|remoteUserId format
     */
    private Map getUserIdMapping(String fileName)
        throws CLIException {
        Map map = new HashMap();
        BufferedReader io = null;
        try {
            io = new BufferedReader(new FileReader(fileName));
            String line = io.readLine();
            while (line != null) {
                line = line.trim();
                int len = line.length();
                if (len > 0) {
                    int idx = line.indexOf('|');
                    if ((idx == -1) || (idx == 0) || (idx == (len -1))) {
                        Object[] param = {line, fileName};
                        throw new CLIException(MessageFormat.format(
                            getResourceString("bulk-federation-wrong-format"),
                            param), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                    }
                    map.put(line.substring(0, idx), line.substring(idx+1));
                }
                line = io.readLine();
            }
        } catch (IOException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (io != null) {
                try {
                    io.close();
                } catch (IOException ex) {
                    //ignored
                }      
            }
        }
        return map;
    }
    
    private String createNameIdentifier()
        throws CLIException {
        byte[] handleBytes = new byte[21];
        randomGenerator.nextBytes(handleBytes);
        if (handleBytes == null) {
            throw new CLIException(
                getResourceString("bulk-federation-cannot-generate-name-id"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        return Base64.encode(handleBytes);
    }
}
