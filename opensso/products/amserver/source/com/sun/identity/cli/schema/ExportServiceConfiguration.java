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
 * $Id: ExportServiceConfiguration.java,v 1.5 2009/09/12 00:32:47 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.cli.schema;

import com.iplanet.services.util.AMEncryption;
import com.iplanet.services.util.ConfigurableKey;
import com.iplanet.services.util.JCEEncryption;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.log.Level;
import com.sun.identity.shared.encode.Hash;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class ExportServiceConfiguration extends AuthenticatedCommand {
    
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
        String encryptSecret = getStringOptionValue(IArgument.ENCRYPT_SECRET);
        FileOutputStream fout = null;
        String[] param = {"tty"};
        String[] paramException = {"tty", ""};
        
        try {
            
            if ((outputFile != null) && (outputFile.length() > 0)) {
                fout = new FileOutputStream(outputFile);
                param[0] = outputFile;
                paramException[0] = outputFile;
            }       
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_EXPORT_SM_CONFIG_DATA", param);

            ServiceManager sm = new ServiceManager(adminSSOToken);
            AMEncryption encryptObj = new JCEEncryption();
            ((ConfigurableKey)encryptObj).setPassword(encryptSecret);
 
            String resultXML = sm.toXML(encryptObj);
            resultXML += "<!-- " + Hash.hash(encryptSecret) + " -->";
            if (fout != null) {
                fout.write(resultXML.getBytes("UTF-8"));
            } else {
                System.out.write(resultXML.getBytes("UTF-8"));
            }

            getOutputWriter().printlnMessage(getResourceString(
                "export-service-configuration-succeeded"));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_IMPORT_SM_CONFIG_DATA", param);
        } catch (UnsupportedEncodingException e) {
            paramException[1] = e.getMessage();
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_EXPORT_SM_CONFIG_DATA", paramException);
           throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            paramException[1] = e.getMessage();
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_EXPORT_SM_CONFIG_DATA", paramException);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            paramException[1] = e.getMessage();
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_EXPORT_SM_CONFIG_DATA", paramException);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            paramException[1] = e.getMessage();
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_EXPORT_SM_CONFIG_DATA", paramException);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (Exception e) {
            paramException[1] = e.getMessage();
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_EXPORT_SM_CONFIG_DATA", paramException);
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
}
