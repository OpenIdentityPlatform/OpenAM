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
 * $Id: CLIUtil.java,v 1.8 2008/10/30 18:25:02 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli;


import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This is an utility class. 
 */
public class CLIUtil {
    private CLIUtil() {
    }

    /**
     * Returns content of a file.
     *
     * @param mgr Command Line Manager.
     * @param fileName Name of file.
     * @return content of a file.
     * @throws CLIException if file content cannot be returned.
     */
    public static String getFileContent(CommandManager mgr, String fileName)
        throws CLIException
    {
        return getFileContent(mgr, fileName, false);
    }

    /**
     * Returns content of a file.
     *
     * @param mgr Command Line Manager.
     * @param fileName Name of file.
     * @param singleLine <code>true</code> to only read one line from the file.
     * @return content of a file.
     * @throws CLIException if file content cannot be returned.
     */
    public static String getFileContent(
        CommandManager mgr,
        String fileName,
        boolean singleLine
    ) throws CLIException {
        File test = new File(fileName);
        if (!test.exists()) {
            Object[] param = {fileName};
            throw new CLIException(MessageFormat.format(
                mgr.getResourceBundle().getString(
                    "error-message-file-does-not-exist"), param),
                ExitCodes.CANNOT_READ_FILE);
        }
        StringBuilder buff = new StringBuilder();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(fileName));
            if (in.ready()) {
                String line = in.readLine();
                while (line != null) {
                    buff.append(line);
                    if (singleLine) {
                        break;
                    } else {
                        buff.append("\n");
                        line = in.readLine();
                    }
                }
            }
        } catch(IOException e){
            throw new CLIException(e.getMessage(), ExitCodes.CANNOT_READ_FILE);
        } finally {
            if (in != null ) {
                try {
                    in.close();
                } catch (Exception e) {
                    Debug debugger = CommandManager.getDebugger();
                    if (debugger.warningEnabled()) {
                        debugger.warning("cannot close file, " + fileName, e);
                    }
                }
            }
        }
        return buff.toString();
    }

    /**
     * Returns a set of attributes (of password syntax) of a given service.
     *
     * @param serviceName Name of service.
     * @return a set of attributes (of password syntax) of a given service.
     * @throws SMSException if error occurs when reading the service schema 
     *         layer
     * @throws SSOException if Single sign-on token is invalid.
     */
    public static Set getPasswordFields(String serviceName) 
        throws SMSException, SSOException
    {
        Set setPasswords = new HashSet();
        SSOToken ssoToken = (SSOToken)AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        ServiceSchemaManager ssm = new ServiceSchemaManager(
            serviceName, ssoToken);
        if (ssm != null) {
            ServiceSchema schema = ssm.getOrganizationSchema();
            if (schema != null) {
                Set attributeSchemas = schema.getAttributeSchemas();
                
                for (Iterator i = attributeSchemas.iterator(); i.hasNext(); ) {
                    AttributeSchema as = (AttributeSchema)i.next();
                    if (as.getSyntax().equals(AttributeSchema.Syntax.PASSWORD)){
                        setPasswords.add(as.getName());
                    }
                }
            }
        }
        return setPasswords;
    }
    
    /**
     * Returns a set of attributes (of password syntax) of a given service.
     *
     * @param serviceName Name of service.
     * @param schemaType Type of Schema.
     * @param subSchema Name of SubSchema
     * @return a set of attributes (of password syntax) of a given service.
     * @throws SMSException if error occurs when reading the service schema 
     *         layer
     * @throws SSOException if Single sign-on token is invalid.
     */
    public static Set getPasswordFields(
        String serviceName,
        SchemaType schemaType,
        String subSchema) 
        throws SMSException, SSOException
    {
        Set setPasswords = new HashSet();
        SSOToken ssoToken = (SSOToken)AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        ServiceSchemaManager ssm = new ServiceSchemaManager(
            serviceName, ssoToken);
        if (ssm != null) {
            ServiceSchema schema = ssm.getSchema(schemaType);
            if (schema != null) {
                ServiceSchema ss = schema.getSubSchema(subSchema);
                Set attributeSchemas = ss.getAttributeSchemas();
                
                for (Iterator i = attributeSchemas.iterator(); i.hasNext(); ) {
                    AttributeSchema as = (AttributeSchema)i.next();
                    if (as.getSyntax().equals(AttributeSchema.Syntax.PASSWORD)){
                        setPasswords.add(as.getName());
                    }
                }
            }
        }
        return setPasswords;
    }
    
    
    
    /**
     * Writes to a file.
     *
     * @param file Name of file.
     * @param content Content to be written.
     * @throws IOException if file cannot be accessed.
     */
    public static void writeToFile(String file, String content) 
        throws IOException {
        FileOutputStream fout = null;
        
        try {
            fout = new FileOutputStream(file);
            fout.write(content.getBytes());
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
