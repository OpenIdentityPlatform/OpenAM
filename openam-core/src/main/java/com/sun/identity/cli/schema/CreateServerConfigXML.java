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
 * $Id: CreateServerConfigXML.java,v 1.7 2009/11/20 23:52:53 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli.schema;

import com.iplanet.dpro.session.service.AMSessionRepository;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AccessManagerConstants;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIUtil;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.common.DNUtils;
import com.sun.identity.log.Level;
import com.sun.identity.security.EncodeAction;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.util.Iterator;
import java.util.List;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.RDN;

public class CreateServerConfigXML extends AuthenticatedCommand implements Constants {

    static final String DS_HOST = "dshost";
    static final String DS_PORT = "dsport";
    static final String DS_ADMIN = "dsadmin";
    static final String DS_BASEDN = "basedn";
    
    private static final String DS_PWD_FILE = "dspassword-file";
    
    private String dsHost;
    private String dsPort;
    private String dsAdmin;
    private String dsPassword;
    private String basedn;
    
    /**
     * Handles request.
     *
     * @param rc Request Context.
     * @throws CLIException if request cannot be processed.
     */
    public void handleRequest(RequestContext rc)
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        SSOToken adminSSOToken = getAdminSSOToken();
        String outputFile = getStringOptionValue(IArgument.OUTPUT_FILE);
        FileOutputStream fout = null;
        String[] param = {"tty"};
        String[] paramException = {"tty", ""};
        
        dsHost = getStringOptionValue(DS_HOST);
        dsPort = getStringOptionValue(DS_PORT);
        dsAdmin = getStringOptionValue(DS_ADMIN);
        String dsPasswordFile = getStringOptionValue(DS_PWD_FILE);
        basedn = getStringOptionValue(DS_BASEDN);
        
        if ((dsHost == null) || (dsHost.length() == 0)) {
            dsHost = "ds.opensso.java.net";
        }
        if ((dsPort == null) || (dsPort.length() == 0)) {
            dsPort = "389";
        }
        if ((dsAdmin == null) || (dsAdmin.length() == 0)) {
            dsAdmin = "cn=Directory Manager";
        }
        if ((dsPasswordFile == null) || (dsPasswordFile.length() == 0)) {
            dsPassword = "11111111";
        } else {
            dsPassword = CLIUtil.getFileContent(getCommandManager(),
                dsPasswordFile);
        }
        if ((basedn == null) || (basedn.length() == 0)) {
            basedn = DEFAULT_ROOT_SUFFIX;
        }
        dsPassword = (String)AccessController.doPrivileged(
            new EncodeAction(dsPassword));
        
        try {
            if ((outputFile != null) && (outputFile.length() > 0)) {
                fout = new FileOutputStream(outputFile);
                param[0] = outputFile;
                paramException[0] = outputFile;
            }       
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_CREATE_SERVERCONFIG_XML", param);
            
            String template = getResource("serverconfig.xml");
            String modified = modifyXML(template);
            
            if (fout != null) {
                fout.write(modified.getBytes());
            } else {
                getOutputWriter().printlnMessage(modified);
            }
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_CREATE_SERVERCONFIG_XML", param);
        } catch (IOException e) {
            paramException[1] = e.getMessage();
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_CREATE_SERVERCONFIG_XML", paramException);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException ioe) {
                    //ignored
                }
            }
        }
    }
    
    private static String getResource(String file)
        throws IOException
    {
        InputStreamReader fin = null;
        StringBuffer sbuf = new StringBuffer();

        try {
            fin = new InputStreamReader(
                ClassLoader.getSystemResourceAsStream(file));
            char[] cbuf = new char[1024];
            int len;
            while ((len = fin.read(cbuf)) > 0) {
                sbuf.append(cbuf, 0, len);
            }
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (Exception ex) {
                    //No handling requried
                }
            }
        }
        return sbuf.toString();
    }
    
    private String modifyXML(String xml)
        throws CLIException {
        String amadminPwds = CLIUtil.getFileContent(getCommandManager(),
            getStringOptionValue(AccessManagerConstants.ARGUMENT_PASSWORD_FILE),
            true);
        amadminPwds = (String)AccessController.doPrivileged(
            new EncodeAction(amadminPwds));
        String canRootSuffix = canonicalize(basedn);
        xml = xml.replaceAll("@DIRECTORY_SERVER@", dsHost);
        xml = xml.replaceAll("@DIRECTORY_PORT@", dsPort);
        xml = xml.replaceAll("@NORMALIZED_ORGBASE@", 
            DNUtils.normalizeDN(basedn));
        xml = xml.replaceAll("@DS_DIRMGRDN@", dsAdmin);
        xml = xml.replaceAll("@ENCADMINPASSWD@", dsPassword);
        xml = xml.replaceAll("@ENCADADMINPASSWD@", amadminPwds);
        xml = xml.replaceAll("@SM_CONFIG_BASEDN@", canRootSuffix);
        xml = xml.replaceAll("@ROOT_SUFFIX@", canRootSuffix);
        xml = xml.replaceAll("@ORG_BASE@", canRootSuffix);

        return xml;
    }
    
    private String canonicalize(String nSuffix) {
        StringBuffer buff = new StringBuffer(1024);
        DN dn = new DN(nSuffix);
        List rdns = dn.getRDNs();
        for (Iterator iter = rdns.iterator(); iter.hasNext();) {
            RDN rdn = (RDN) iter.next();
            buff.append(LDAPDN.escapeRDN(rdn.toString()));
            if (iter.hasNext()) {
                buff.append(",");
            }
        }
        return buff.toString();
    }

}
