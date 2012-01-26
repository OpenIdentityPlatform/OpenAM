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
 * $Id: NetworkValidator.java,v 1.2 2008/06/25 05:51:22 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.net.InetAddress;
import java.util.StringTokenizer;
import java.util.Map;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

public class NetworkValidator extends ValidatorBase {

    public NetworkValidator() throws InstallException {
        super();
    }

    /*
     * Checks if port is valid
     * 
     * @param port @param props @param state
     * 
     * @return ValidationResult
     */
    public ValidationResult isPortValid(String port, Map props,
            IStateAccess state) {

        int portno = -1;
        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        if ((port != null) && (port.length() > 0) && (port.length() <= 5)) {
            try {
                portno = Integer.parseInt(port);
                if ((portno > 0) && (portno < 65535)) {
                    returnMessage = LocalizedMessage.get(LOC_VA_MSG_VAL_PORT,
                            new Object[] { port });
                    validRes = ValidationResultStatus.STATUS_SUCCESS;
                }
            } catch (NumberFormatException nfe) {
                Debug
                        .log("NetworkValidator.isPortValid threw exception :",
                                nfe);
            }
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_IN_VAL_PORT,
                    new Object[] { port });
        }

        Debug.log("NetworkValidator : Is port : " + port + " valid ? "
                + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);

    }

    /**
     * Checks whether the given host is a valid host or not It allows IP
     * addresses &
     * 
     * @param host
     * @param props
     * @param state
     * 
     * @return ValidationResult
     * 
     */
    public ValidationResult isHostValid(String host, Map props,
            IStateAccess state) {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;
        boolean cont = false;

        if ((host != null) && (host.length() > 0)) {
            try {
                // For jdk bug, we check if hostname is a int or double
                int intHost = Integer.parseInt(host);
                double dobHost = Double.parseDouble(host);
            } catch (NumberFormatException exc) {
                cont = true;
            } catch (Exception ex) {
                cont = true;
            }

            try {
                if (cont) {
                    String tempHost = 
                        InetAddress.getByName(host).getHostName();
                    if (tempHost != null) {
                        returnMessage = LocalizedMessage.get(
                                LOC_VA_MSG_VAL_HOST, new Object[] { host });
                        validRes = ValidationResultStatus.STATUS_SUCCESS;
                    }
                }
            } catch (Exception ex) {
                Debug.log("NetworkVaidator.isHostValid(..) threw exception :",
                        ex);
            }
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_IN_VAL_HOST,
                    new Object[] { host });
        }

        Debug.log("NetworkValidator : Is Host : " + host + " valid ? "
                + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);
    }

    /**
     * Checks whether the host name is fully qualified
     * 
     * @param host
     * @param props
     * @param state
     * 
     * @return ValidationResult
     * 
     */
    public ValidationResult isFullyQualifiedHost(String host, Map props,
            IStateAccess state) {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        if ((host != null) && (host.length() > 0)) {
            if ((!host.startsWith(".")) && (!host.endsWith("."))) {
                StringTokenizer tokens = new StringTokenizer(host, ".");
                if (tokens.countTokens() >= 3) {
                    validRes = ValidationResultStatus.STATUS_SUCCESS;
                }
            }
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(
                    LOC_VA_WRN_IN_VAL_FULLY_QUAL_HOST, new Object[] { host });
        }

        Debug.log("NetworkValidator : Is Host name : " + host + " fully "
                + "qualified ? " + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);
    }

    /**
     * Checks if the host name is local
     * 
     * @param host
     * @param props
     * @param state
     * 
     * @return ValidationResult
     * 
     */
    public ValidationResult isLocalHost(String host, Map props,
            IStateAccess state) {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        try {
            if ((host != null) && (host.length() > 0)) {
                StringTokenizer st = new StringTokenizer(host, ".");
                String hostname = st.nextToken();
                if (hostname != null) {
                    String localHost = 
                        InetAddress.getLocalHost().getHostName();
                    if (hostname.equals(localHost)) {
                        validRes = ValidationResultStatus.STATUS_SUCCESS;
                    }
                }
            }
        } catch (Exception ex) {
            Debug.log("NetworkValidator.isLocalHost(...) threw exception : "
                    + ex);
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_IN_VAL_LOCAL_HOST,
                    new Object[] { host });
        }

        Debug.log("NetworkValidator : Is Host : " + host + " local ?  "
                + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);
    }

    /**
     * Method validateProtocol
     * 
     * 
     * @param proto
     * @param props
     * @param state
     * 
     * @return ValidationResult
     * 
     */
    public ValidationResult isValidProtocol(String proto, Map props,
            IStateAccess state) {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        if ((proto != null) && (proto.length() > 0)) {
            if ((proto.compareToIgnoreCase(STR_VA_HTTP_PROTO) == 0)
                    || (proto.compareToIgnoreCase(STR_VA_HTTPS_PROTO) == 0)) {
                returnMessage = LocalizedMessage.get(LOC_VA_MSG_VAL_PROTO,
                        new Object[] { proto });
                validRes = ValidationResultStatus.STATUS_SUCCESS;
            }
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_IN_VAL_PROTO,
                    new Object[] { proto });
        }

        Debug.log("NetworkValidator : Is protocol : " + proto + " valid ? "
                + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);
    }

    /**
     * Method validateURI
     * 
     * 
     * @param uri
     * @param props
     * @param state
     * 
     * @return ValidationResult
     * 
     */
    public ValidationResult isValidURI(String uri, Map props, 
            IStateAccess state) {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        if ((uri != null) && (uri.trim().length() >= 0)) {
            if (uri.startsWith(STR_VA_DEF_URI_PATH)) {
                returnMessage = LocalizedMessage.get(LOC_VA_MSG_VALID_URI,
                        new Object[] { uri });
                validRes = ValidationResultStatus.STATUS_SUCCESS;
            }
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_IN_VALID_URI,
                    new Object[] { uri });
        }

        Debug.log("NetworkValidator : Is URI : " + uri + " valid ? "
                + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);
    }

    public void initializeValidatorMap() throws InstallException {

        Class[] paramObjs = { String.class, Map.class, IStateAccess.class };

        try {
            getValidatorMap().put("VALID_PORT",
                    this.getClass().getMethod("isPortValid", paramObjs));

            getValidatorMap().put("VALID_HOST",
                    this.getClass().getMethod("isHostValid", paramObjs));

            getValidatorMap().put("VALID_LOCAL_HOST",
                    this.getClass().getMethod("isLocalHost", paramObjs));

            getValidatorMap().put(
                    "VALID_FULLY_QUALIFIED_HOST",
                    this.getClass()
                            .getMethod("isFullyQualifiedHost", paramObjs));

            getValidatorMap().put("VALID_PROTO",
                    this.getClass().getMethod("isValidProtocol", paramObjs));

            getValidatorMap().put("VALID_URI",
                    this.getClass().getMethod("isValidURI", paramObjs));

        } catch (NoSuchMethodException nsme) {
            Debug.log("NetworkValidator: "
                    + "NoSuchMethodException thrown while loading method :",
                    nsme);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), nsme);
        } catch (SecurityException se) {
            Debug.log("NetworkValidator: "
                    + "SecurityException thrown while loading method :", se);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), se);
        } catch (Exception ex) {
            Debug.log("NetworkValidator: "
                    + "Exception thrown while loading method :", ex);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), ex);
        }

    }

    /*
     * Localized messages
     */
    public static String LOC_VA_MSG_VAL_PORT = "VA_MSG_VAL_PORT";

    public static String LOC_VA_WRN_IN_VAL_PORT = "VA_WRN_IN_VAL_PORT";

    public static String LOC_VA_MSG_VAL_PROTO = "VA_MSG_VAL_PROTO";

    public static String LOC_VA_WRN_IN_VAL_PROTO = "VA_WRN_IN_VAL_PROTO";

    public static String LOC_VA_MSG_VAL_HOST = "VA_MSG_VAL_HOST";

    public static String LOC_VA_WRN_IN_VAL_HOST = "VA_WRN_IN_VAL_HOST";

    public static String LOC_VA_WRN_IN_VAL_FULLY_QUAL_HOST = 
        "VA_WRN_IN_VAL_FULLY_QUAL_HOST";

    public static String LOC_VA_WRN_IN_VAL_LOCAL_HOST = 
        "VA_WRN_IN_VAL_LOCAL_HOST";

    public static String LOC_VA_MSG_VALID_URI = "VA_MSG_VALID_URI";

    public static String LOC_VA_WRN_IN_VALID_URI = "VA_WRN_IN_VALID_URI";

    /*
     * String constants
     */
    public static String STR_VA_HTTP_PROTO = "http";

    public static String STR_VA_HTTPS_PROTO = "https";

    public static String STR_VA_DEF_URI_PATH = "/";

}
