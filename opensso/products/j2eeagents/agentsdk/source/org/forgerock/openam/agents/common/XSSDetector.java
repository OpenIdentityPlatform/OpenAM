/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.agents.common;

import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.filter.AmFilterResult;
import com.sun.identity.agents.filter.AmFilterResultStatus;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Bernhard Thalmayr
 */
public class XSSDetector {

    private static final String ENCODING = "UTF-8";
    private static final String PROPERTIES_DELIMITER = ",";
    private static final String XSS_REDIRECT_URI_PROP = "xss.redirect.uri";
    private static final String XSS_ELEMENTS_PROP = "xss.code.elements";
    private static final String XSS_DEFAULT_REDIRECT_URI = "/agentapp/XSSCodeDetected.html";

    /**
     * Checks to see if a given String includes XSS code.
     *
     * @param input to be checked
     * @param manager the <code>Manager</code>
     * @param application for which a possible HTTP redirect should be created
     *
     * @return <code>null</code> if no action is necessary, or
     * <code>AmFilterResult</code> object indicating the necessary action in
     * order to handle XSS code.
     */

    public static AmFilterResult handle(String input, Manager manager, String application) {
        if ((null == input) || (null == application)) {
            return new AmFilterResult(AmFilterResultStatus.STATUS_SERVER_ERROR);
        }

        List<String> XSSElements = getXSSCodeElements(manager);
        if (XSSElements.isEmpty()) {
            return null;
        } else {
            String XSSredirectURI = getXSSRedirectURI(manager, application);
            String inputString = input.toLowerCase();
            for (String XSSElement : XSSElements) {
                try {
                    if ((inputString.indexOf(XSSElement) != -1) || (inputString.indexOf((URLEncoder.encode(XSSElement, ENCODING)))) != -1) {
                        logMessage(manager, "XSSDetector.handle: Found XSS code <" + XSSElement + ">");
                        return new AmFilterResult(AmFilterResultStatus.STATUS_REDIRECT, XSSredirectURI);
                    }
                } catch (UnsupportedEncodingException ex) {
                    logError(manager, "XSSDetector.handle: URLEncoding failed");
                    logMessage(manager, "XSSDetector.handle: URLEncoding failed", ex);
                    return new AmFilterResult(AmFilterResultStatus.STATUS_SERVER_ERROR);
                }
            }
        }
        return null;
    }

    /**
     * Checks to see if the incoming request includes XSS code.
     *
     * @param request the <code>HttpServletRequest</code> that carries
     * information about the incoming request and response objects.
     * @param manager the <code>Manager</code>
     * @param application for which a possible HTTP redirect should be created
     *
     * @return <code>null</code> if no action is necessary, or
     * <code>AmFilterResult</code> object indicating the necessary action in
     * order to handle XSS code.
     */

    public static AmFilterResult handle(HttpServletRequest request, Manager manager, String application) {
        if ((null == request) || (null == application)) {
            return new AmFilterResult(AmFilterResultStatus.STATUS_SERVER_ERROR);
        }

        List<String> XSSElements = getXSSCodeElements(manager);
        if (XSSElements.isEmpty()) {
            return null;
        } else {
            String XSSredirectURI = getXSSRedirectURI(manager, application);

            try {
                //first check resuest URI
                String requestURI = request.getRequestURI().toLowerCase();
                if (null != requestURI) {
                    for (String XSSElement : XSSElements) {
                        if ((requestURI.indexOf(XSSElement) != -1) || (requestURI.indexOf(URLEncoder.encode(XSSElement, ENCODING).toLowerCase()) != -1)) {
                            return new AmFilterResult(AmFilterResultStatus.STATUS_REDIRECT, XSSredirectURI);
                        }
                    }
                }

                // then check request parameters
                Map<String, String[]> parameterMap = request.getParameterMap();
                if ((null == parameterMap) || (parameterMap.isEmpty())) {
                    return null;
                } else {
                    for (Entry<String, String[]> parameter : parameterMap.entrySet()) {
                        for (String XSSElement : XSSElements) {
                            String key = parameter.getKey().toLowerCase();
                            if ((key.indexOf(XSSElement) != -1) || (key.indexOf(URLEncoder.encode(XSSElement, ENCODING).toLowerCase()) != -1)) {
                                logMessage(manager, "XSSDetector.handle: Found XSS code <" + XSSElement + "> in request parameter");
                                return new AmFilterResult(AmFilterResultStatus.STATUS_REDIRECT, XSSredirectURI);
                            }
                            String[] parameterValues = parameter.getValue();
                            if ((null != parameterValues) && (parameterValues.length != 0)) {
                                for (String value : parameterValues) {
                                    value = value.toLowerCase();
                                    if ((value.indexOf(XSSElement) != -1) || (value.indexOf(URLEncoder.encode(XSSElement, ENCODING).toLowerCase()) != -1)) {
                                        logMessage(manager, "XSSDetector.handle: Found XSS code <" + XSSElement + "> in request parameter value");
                                        return new AmFilterResult(AmFilterResultStatus.STATUS_REDIRECT, XSSredirectURI);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (UnsupportedEncodingException ex) {
                logError(manager, "XSSDetector.handle: URLEncoding failed");
                logMessage(manager, "XSSDetector.handle: URLEncoding failed", ex);
                return new AmFilterResult(AmFilterResultStatus.STATUS_SERVER_ERROR);
            }
        }
        return null;
    }

    private static String getXSSRedirectURI(Manager manager, String applicationName) {
        String config = manager.getApplicationConfigurationString(XSS_REDIRECT_URI_PROP, applicationName);
        if ((null == config) || (config.trim().length() == 0)) {
            config = manager.getConfiguration(XSS_REDIRECT_URI_PROP);
            //config = manager.getConfiguration(XSS_REDIRECT_URI_PROP,XSS_DEFAULT_REDIRECT_URI);
        }
        /*
         * TODO remove workaround if update issue is fixed
         * Workaround for OpenAM update issue begin
         * read freeform properties
         */
        if ((null == config) || (config.trim().length() == 0)) {
            // first try to get the applicatioin specific value
            StringBuilder keyBuilder = new StringBuilder(XSS_REDIRECT_URI_PROP);
            keyBuilder.append("[").append(applicationName).append("]");
            config = manager.getSystemConfiguration(keyBuilder.toString());

            if ((null == config) || (config.trim().length() == 0)) {
                // then get the global or default value
                config = manager.getSystemConfiguration(XSS_REDIRECT_URI_PROP,XSS_DEFAULT_REDIRECT_URI);
            }
        }
        // Workaround end
        return config.trim();
    }

    /*
     * get configuration elements to be treated as XSSCode
     * @param manager
     * @return a list of trimmed,lowercased Strings
     */
    private static List<String> getXSSCodeElements(Manager manager) {
        List<String> result = new ArrayList<String>();
        String[] configElements = manager.getConfigurationStrings(XSS_ELEMENTS_PROP);
        /*
         * TODO remove workaround if update issue is fixed
         * Workaround for OpenAM update issue begin
         * read freeform properties
         */
        if ((configElements == null) || (configElements.length == 0)) {
            String freeFormProps = manager.getSystemConfiguration(XSS_ELEMENTS_PROP);
            if (freeFormProps != null) {
                configElements = freeFormProps.split(PROPERTIES_DELIMITER);
            }
        }
        // Workaround end
        for (String configElem : configElements) {
            result.add(configElem.trim().toLowerCase());
        }
        return result;
    }

    private static void logMessage(Manager manager, String message) {
        if (manager.getModule().isLogMessageEnabled()) {
            manager.getModule().logMessage(message);
        }
    }

    private static void logMessage(Manager manager, String message, Throwable cause) {
        if (manager.getModule().isLogMessageEnabled()) {
            manager.getModule().logMessage(message, cause);
        }
    }

    private static void logError(Manager manager, String error) {
        manager.getModule().logError(error);
    }
}
