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

package org.forgerock.openam.agents.filter;

import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.filter.AmFilterMode;
import com.sun.identity.agents.filter.AmFilterRequestContext;
import com.sun.identity.agents.filter.AmFilterResult;
import com.sun.identity.agents.filter.AmFilterResultStatus;
import com.sun.identity.agents.filter.AmFilterTaskHandler;
import com.sun.identity.agents.filter.ISSOContext;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.openam.agents.common.XSSDetector;

/**
 *
 * @author Bernhard Thalmayr
 */

/**
 * <p>
 * This task handler provides the necessary functionality to check incoming
 * requests for XSS Code
 * </p>
 */
public class XSSDetectionTaskHandler extends AmFilterTaskHandler implements IXSSDetectionTaskHandler {
    private String CDSSORedirectURI = null;
    private String notificationURI = null;
    private boolean CDSSOEnabled = false;
    private boolean notificationEnabled = false;
    private static final String SYSTEM_CONFIG_NOTIFICATION_ENABLED = "com.sun.identity.agents.notification.enabled";

    public XSSDetectionTaskHandler(Manager manager) {
        super(manager);
    }

    @Override
    public void initialize(ISSOContext context, AmFilterMode mode) throws AgentException {
        super.initialize(context, mode);
        String notificationEnabledValue = getSystemConfiguration(SYSTEM_CONFIG_NOTIFICATION_ENABLED);
        setNotificationEnabled(Boolean.valueOf(notificationEnabledValue));
        setCDSSOEnabled(getConfigurationBoolean(CONFIG_CDSSO_ENABLED));

        if (isNotificationEnabled()) {
            setNotificationURI(AgentConfiguration.getClientNotificationURL().trim());
        }

        if (isCDSSOEnabled()) {
            setCDSSORedirectURI(getConfigurationString(CONFIG_CDSSO_REDIRECT_URI).trim());
        }
    }

    public boolean isActive() {
        return true;
    }

    public String getHandlerName() {
        return AM_FILTER_XSSDETECTION_TASK_HANDLER_NAME;
    }

    /**
     * Checks to see if the incoming request includes XSS code.
     *
     * @param ctx the <code>AmFilterRequestContext</code> that carries
     * information about the incoming request and response objects.
     *
     * @return <code>null</code> if no action is necessary, or
     * <code>AmFilterResult</code> object indicating the necessary action in
     * order to handle XSS code.
     * @throws AgentException in case the handling of this request results in
     * an unexpected error condition
     * <p>
     * It bypasses notificaton-requests and CDSSO-requests if necessary.
     * </p>
     */

    public AmFilterResult process(AmFilterRequestContext ctx) throws AgentException {
        HttpServletRequest request = ctx.getHttpServletRequest();
        if (isNotificationEnabled()) {
            try {
                String notificationPath = (new URL(getNotificationURI())).getPath();
                if (request.getRequestURI().equals(notificationPath)) {
                    if (isLogMessageEnabled()) {
                        logMessage("XSSDetectionTaskHandler.process: Skipping process of notification request");
                    }
                    return null;
                }
            } catch (MalformedURLException ex) {
                logError("XSSDetectionTaskHandler.process: Could not create URL", ex);
                return new AmFilterResult(AmFilterResultStatus.STATUS_SERVER_ERROR);
            }
        }

        if (isCDSSOEnabled()) {
            if (request.getRequestURI().equals(getCDSSORedirectURI())) {
                if (isLogMessageEnabled()) {
                    logMessage("XSSDetectionTaskHandler.process: Skipping process of CDSSO redirect request");
                }
                return null;
            }
        }

        return XSSDetector.handle(request, getManager(), getApplicationName(request));
    }


    /**
     * @return the CDSSORedirectURI
     */
    public String getCDSSORedirectURI() {
        return CDSSORedirectURI;
    }

    /**
     * @param CDSSORedirectURI the CDSSORedirectURI to set
     */
    public void setCDSSORedirectURI(String CDSSORedirectURI) {
        this.CDSSORedirectURI = CDSSORedirectURI;
    }

    /**
     * @return the notificationURI
     */
    public String getNotificationURI() {
        return notificationURI;
    }

    /**
     * @param notificationURI the notificationURI to set
     */
    public void setNotificationURI(String notificationURI) {
        this.notificationURI = notificationURI;
    }

    /**
     * @return the CDSSOEnabled
     */
    public boolean isCDSSOEnabled() {
        return CDSSOEnabled;
    }

    /**
     * @param CDSSOEnabled the CDSSOEnabled to set
     */
    public void setCDSSOEnabled(boolean CDSSOEnabled) {
        this.CDSSOEnabled = CDSSOEnabled;
    }

    /**
     * @return the notificationEnabled
     */
    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    /**
     * @param notificationEnabled the notificationEnabled to set
     */
    public void setNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

}
