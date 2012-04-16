/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: WebAppsValidator.java,v 1.2 2008/11/28 12:36:23 saueree Exp $
 */

package com.sun.identity.agents.tools.tomcat.v6;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.ValidationResult;
import com.sun.identity.install.tools.configurator.ValidationResultStatus;
import com.sun.identity.install.tools.configurator.ValidatorBase;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;


public class WebAppsValidator extends ValidatorBase implements IConstants,
    IConfigKeys {
    /*
     * Localized constants
     */
    private static String LOC_VA_MSG_TOMCAT_VAL_WEB_APP_LOCATION =
    	"VA_MSG_TOMCAT_VAL_WEB_APP_LOCATION";
    private static String LOC_VA_WRN_TOMCAT_IN_VAL_WEB_APP_LOCATION =
    	"VA_WRN_TOMCAT_IN_VAL_WEB_APP_LOCATION";

    /** Hashmap of Validator names and integers */
    Map validMap = new HashMap();
    ArrayList _mapContextPaths = new ArrayList();
    ArrayList _webAppPaths = null;

    public WebAppsValidator() throws InstallException {
        super();
    }

    /**
     * Method areWebAppsValid
     *
     *
     * @param dir
     * @param props
     * @param IStateAccess
     *
     * @return ValidationResult
     *
     */
    public ValidationResult areWebAppsValid(
        String configDir,
        Map props,
        IStateAccess state) {
        ValidationResultStatus validRes =
        	ValidationResultStatus.STATUS_SUCCESS;

        LocalizedMessage returnMessage = null;
        String webAppWebXMLFile;

        getValidAppContextPaths(state);

        if ((_webAppPaths != null) && !_webAppPaths.isEmpty()) {
            for (int index = 0; index < _webAppPaths.size(); index++) {
                webAppWebXMLFile = (String) _webAppPaths.get(index);

                if ((!isValidFile(webAppWebXMLFile))) {
                    validRes = ValidationResultStatus.STATUS_FAILED;

                    returnMessage = LocalizedMessage.get(
                            LOC_VA_WRN_TOMCAT_IN_VAL_WEB_APP_LOCATION,
                            STR_TOMCAT_GROUP,
                            new Object[] { webAppWebXMLFile });

                    _webAppPaths.clear();

                    break;
                }

                Debug.log(
                    "WebAppsValidator.areWebAppsValid() : " +
                    "Is Tomcat application context dir "
                    + webAppWebXMLFile + " valid ? "
                    + validRes.isSuccessful());
            }
        } else {
            Debug.log(
                "WebAppsValidator.areWebAppsValid(): no web apps found to " +
                "validate");
        }

        if (validRes.getIntValue() ==
        	ValidationResultStatus.INT_STATUS_SUCCESS) {

        	state.put(
                STR_WEB_APP_CONTEXT_PATH_LIST,
                _mapContextPaths);

            returnMessage = LocalizedMessage.get(
                    LOC_VA_MSG_TOMCAT_VAL_WEB_APP_LOCATION,
                    STR_TOMCAT_GROUP,
                    new Object[] { _webAppPaths.toString() });
        }

        return new ValidationResult(
            validRes,
            null,
            returnMessage);
    }

    public void initializeValidatorMap() throws InstallException {
        Class[] paramObjs = { String.class, Map.class, IStateAccess.class };

        try {
            getValidatorMap()
                .put(
                "VALID_WEBAPPS",
                this.getClass().getMethod(
                    "areWebAppsValid",
                    paramObjs));
        } catch (NoSuchMethodException nsme) {
            Debug.log(
                "HomeDirValidator: NoSuchMethodException "
                + "thrown while loading method :",
                nsme);
            throw new InstallException(
                LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND),
                nsme);
        } catch (SecurityException se) {
            Debug.log(
                "HomeDirValidator: SecurityException thrown "
                + "while loading method :",
                se);
            throw new InstallException(
                LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND),
                se);
        } catch (Exception ex) {
            Debug.log(
                "HomeDirValidator: Exception thrown while "
                + "loading method :",
                ex);
            throw new InstallException(
                LocalizedMessage.get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND),
                ex);
        }
    }

    private boolean isValidFile(String fileName) {
        boolean isValid = false;

        StringBuffer appXmlBuffer = new StringBuffer(fileName);
        appXmlBuffer.append(STR_FORWARD_SLASH);
        appXmlBuffer.append(DIR_NAME_WEBINF);
        appXmlBuffer.append(STR_FORWARD_SLASH);
        appXmlBuffer.append(FILE_NAME_WEB_XML);

        File webxml = new File(appXmlBuffer.toString());

        if (webxml.exists() && webxml.canRead() && webxml.canWrite()) {
            isValid = true;
            _mapContextPaths.add(webxml.getAbsolutePath());
        } else {
            Debug.log(
                "WebAppsValidator.isValidFile(): context " + fileName
                + "is not readable or writable");
        }

        return isValid;
    }

    private void getValidAppContextPaths(IStateAccess state) {
        _webAppPaths = (ArrayList) state.get(STR_WEB_APPS_PATH);
    }

    private ArrayList getWebAppPaths() {
        return _webAppPaths;
    }

    private void setWebAppPaths(ArrayList webAppPaths) {
        _webAppPaths = webAppPaths;
    }

    private ArrayList getMapContextPaths() {
        return _mapContextPaths;
    }

    private void setMapContextPaths(ArrayList mapContextPaths) {
        _mapContextPaths = mapContextPaths;
    }
}
