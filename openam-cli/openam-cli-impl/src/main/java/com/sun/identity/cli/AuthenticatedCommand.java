/*
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
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */
package com.sun.identity.cli;


import static org.forgerock.openam.utils.CollectionUtils.asSet;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.security.DecodeAction;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * This is the base class for all commands that require a user to be 
 * authenticated in order to execute a command.
 */
public abstract class AuthenticatedCommand extends CLICommandBase {
    private static final String FILE_REFERENCE_SUFFIX = "-file";
    // One-off case of an existing properties that makes use of the -file suffix in an existing property
    // => don't apply the file reference rule in this case.
    private static final Set<String> FILE_REFERENCE_SUFFIX_EXEMPT =
            asSet("iplanet-am-logging-num-hist-file", "iplanet-am-auth-windowsdesktopsso-keytab-file");
    private String adminID;
    private String adminPassword;
    protected SSOToken ssoToken;
    
    /**
     * Authenticates the administrator. Dervived classes needs to
     * call this method from the dervived method,
     * <code>handleRequest(RequestContext rc)</code>.
     * Override this method to get user name and passowrd.
     *
     * @param rc Request Context.
     * @throws CLIException if authentication fails.
     */
    @Override
    public void handleRequest(RequestContext rc)
        throws CLIException
    {
        super.handleRequest(rc);
        ssoToken = rc.getCLIRequest().getSSOToken();
        
        if (ssoToken == null) {
            adminID = getStringOptionValue(
                AccessManagerConstants.ARGUMENT_ADMIN_ID);
            adminPassword = getPassword();
        }
    }

    private String getPassword()
        throws CLIException
    {
        String fileName = getStringOptionValue(
            AccessManagerConstants.ARGUMENT_PASSWORD_FILE);
        String password = CLIUtil.getFileContent(getCommandManager(),
            fileName, true);
        String decodedPwd = (String) AccessController.doPrivileged(
                new DecodeAction(password));
        if (decodedPwd != null) {
            password = decodedPwd;
        }
        validatePwdFilePermissions(fileName);
        return password;
    }

    private void validatePwdFilePermissions(String fileName)
        throws CLIException {
        if (System.getProperty("path.separator").equals(":")) {
            try {
                String[] parameter = {"/bin/ls", "-l", fileName};
                Process p = Runtime.getRuntime().exec(parameter);
                BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));
                String s = stdInput.readLine();
                if (s != null) {
                    int idx = s.indexOf(" ");
                    if (idx != -1) {
                        String permission = s.substring(0, idx);
                        if (!permission.startsWith("-r--------")) {
                            String msg = getCommandManager().getResourceBundle()
                                .getString(
                                    "error-message-password-file-not-readonly");
                            Object[] param = {fileName};
                            throw new CLIException(MessageFormat.format(
                                msg, param), 
                                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                            
                        }
                    }
                }
            } catch (IOException e) {
                //ignore, this should not happen because we are able to 
                // read the file in getPassword method.
            }
        }
    }

    protected String getAdminPassword() {
        return adminPassword;
    }

    protected String getAdminID() {
        return adminID;
    }

    protected SSOToken getAdminSSOToken() {
        return ssoToken;
    }

    protected void ldapLogin()
        throws CLIException
    {
        if (ssoToken == null) {
            Authenticator auth = Authenticator.getInstance();
            String bindUser = getAdminID();
            ssoToken = auth.ldapLogin(getCommandManager(), bindUser,
                getAdminPassword());
        } else {
            try {
                SSOTokenManager mgr = SSOTokenManager.getInstance();
                mgr.validateToken(ssoToken);
            } catch (SSOException e) {
                throw new CLIException(e, ExitCodes.SESSION_EXPIRED);
            }
        }
    }

    @Override
    protected void writeLog(
        int type,
        Level level,
        String msgid,
        String[] msgdata
    ) throws CLIException {
        CommandManager mgr = getCommandManager();
        LogWriter.log(mgr, type, level, msgid, msgdata,getAdminSSOToken());
    }

    /**
     * Post-process any attributes specified for the module instance (either via data file or on the command line) to
     * resolve any file references. Any attribute can be specified using a -file suffix on the attribute name. This
     * will cause the value to be treated as a file name, and the associated file to be read in (in the platform
     * default encoding) and used as the attribute value. The attribute will be renamed to remove the -file suffix
     * during this process.
     *
     * @param attrs the raw attributes read from the command line and/or data file.
     * @return the processed attributes with all file references resolved.
     * @throws CLIException if a referenced file cannot be read or if an attribute is specified both normally and using
     * a -file reference.
     */
    protected Map<String, Set<String>> processFileAttributes(Map<String, Set<String>> attrs) throws CLIException {
        Map<String, Set<String>> result = attrs;
        if (attrs != null) {
            result = new LinkedHashMap<>(attrs.size());

            for (Map.Entry<String, Set<String>> attr : attrs.entrySet()) {
                String key = attr.getKey();
                Set<String> values = attr.getValue();

                if (key != null && key.endsWith(FILE_REFERENCE_SUFFIX) && !FILE_REFERENCE_SUFFIX_EXEMPT.contains(key)) {
                    key = key.substring(0, key.length() - FILE_REFERENCE_SUFFIX.length());

                    if (attrs.containsKey(key)) {
                        throw new CLIException("Cannot specify both normal and " + FILE_REFERENCE_SUFFIX
                                + " attribute: " + key, ExitCodes.DUPLICATED_OPTION);
                    }

                    if (values != null) {
                        Set<String> newValues = new LinkedHashSet<String>(values.size());
                        for (String value : values) {
                            newValues.add(CLIUtil.getFileContent(getCommandManager(), value));
                        }
                        values = newValues;
                    }
                }

                result.put(key, values);
            }

        }
        return result;
    }
}
