/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 ForgeRock AS. All Rights Reserved
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

package com.sun.identity.agents.filter;

import java.util.Map;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.IPDPCache;
import com.sun.identity.agents.common.IPDPCacheEntry;
import com.sun.identity.shared.encode.URLEncDec;
import java.util.Iterator;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * This task handler provides the necessary functionality to process POST data
 * preserved during initial request
 * </p>
 */
public class PostSSOPDPTaskHandler extends AmFilterTaskHandler
implements IPostSSOPDPTaskHandler, IPDPTaskConstants {


    /**
     * The constructor that takes a <code>Manager</code> instance in order
     * to gain access to the infrastructure services such as configuration
     * and log access.
     *
     * @param manager the <code>Manager</code> for the <code>filter</code>
     * subsystem.
     * @throws AgentException in case this task handler fails to initialize
     */
    public PostSSOPDPTaskHandler(Manager manager) {
        super(manager);
    }
    
    @Override
    public void initialize(ISSOContext context, AmFilterMode mode) 
    throws AgentException {
        super.initialize(context, mode);
        initPDPEnabledFlag();
        initApplicationDefaultURLs();
    }

    /**
     * Checks to see if the incoming request is of method POST
     * and suggests any action needed to handle requests that do not comply.
     *
     * @param ctx the <code>AmFilterRequestContext</code> that carries information
     * about the incoming request and response objects.
     *
     * @return <code>null</code> if no action is necessary, or
     * <code>AmFilterResult</code> object indicating the necessary action in
     * order to handle notifications.
     * @throws AgentException in case this request processing results in an
     * unexpected error condition
     */
    public AmFilterResult process(AmFilterRequestContext ctx)
        throws AgentException
    {
        AmFilterResult result = null;
        HttpServletResponse response = ctx.getHttpServletResponse();

        //check URL if this is request to dummypost
        String applicationContextURL = ctx.getApplicationContextURL();
        String destinationURL = ctx.getDestinationURL();

        if (applicationContextURL.length() == destinationURL.length()) {
            //no dummypost included
            return null;
        }
        try {
            String dummypostURL = destinationURL.substring(
                    applicationContextURL.length());

            if (dummypostURL.startsWith("/" + DUMMY_POST)) {
                if (isLogMessageEnabled()) {
                    logMessage("PostSSOPDPTaskHandler: dummypost url found: " +
                            dummypostURL);
                }

                //get sunpostpreserve part
                String sunpostpreserve = dummypostURL.substring(
                        DUMMY_POST.length() + 2);

                if (sunpostpreserve.startsWith(SUN_POST_PRESERVE)) {
                    //get sunpostpreserve without URL parameters
                    int questionMarkPos = sunpostpreserve.indexOf("?");
                    if (questionMarkPos != -1) {
                        sunpostpreserve = sunpostpreserve.substring(
                                0, questionMarkPos);
                    }
                    if (isLogMessageEnabled()) {
                        logMessage("PostSSOPDPTaskHandler: sunpostpreserve url " +
                                "found: " + sunpostpreserve);
                    }
                }

                //check if posted data is stored in the cache
                IPDPCache pdpCache = AmFilterManager.getPDPCache();
                IPDPCacheEntry pdpEntry = pdpCache.getEntry(sunpostpreserve);
                if (pdpEntry != null) {
                    //create result if PD exists
                    if (isLogMessageEnabled()) {
                        logMessage("PostSSOPDPTaskHandler: PDP entry retrived " +
                                "from cache : " + pdpEntry);
                    }

                    result = ctx.getServeDataResult(createForm(pdpEntry));
                    pdpCache.removeEntry(sunpostpreserve);
                } else {
                    //get redirection URI for requested application context
                    String applicationName = getApplicationName(
                                                ctx.getHttpServletRequest());
                    if (isLogMessageEnabled()) {
                        logMessage("PostSSOPDPTaskHandler: application name: " +
                                   applicationName);
                    }
                    String uri = (String)_applicationDefaultURLs.get(
                                                            applicationName);
                    if (uri != null && uri.trim().length() != 0) {
                        if (isLogMessageEnabled()) {
                            logMessage("PostSSOPDPTaskHandler: no PDP entry, " +
                                        "redirecting to: " + uri);
                        }
                        result = ctx.getCustomRedirectResult(uri);
                    } else {
                        //forbid access
                        if (isLogMessageEnabled()) {
                            logMessage("PostSSOPDPTaskHandler: no default " +
                                       "redirection URL for application: " +
                                       applicationName + " - forbidding access");
                        }
                        result = ctx.getBlockAccessResult();
                    }
                }
            }
        } catch (Exception ex) {
            logError("PostSSOPDPTaskHandler: Problem processing task", ex);
            result = null;
        }

        return result;
    }

    /**
     * Returns a boolean value indicating if this task handler is enabled or not.
     * @return true if this task handler is enabled, false otherwise
     */
    public boolean isActive() {
        return  isPDPCheckEnabled();
    }

    /**
     * Returns a String that can be used to identify this task handler
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_POST_SSO_POST_DATA_PRESERVATION_TASK_HANDLER_NAME;
    }

    private boolean isPDPCheckEnabled() {
        return _isPDPCheckEnabled;
    }
    
    private void initPDPEnabledFlag() {
        _isPDPCheckEnabled = getConfigurationBoolean(
                CONFIG_POSTDATA_PRESERVE_ENABLE,
                DEFAULT_POSTDATA_PRESERVE_ENABLE);
        if (isLogMessageEnabled()) {
            logMessage("PostSSOPDPTaskHandler: enabled: " + _isPDPCheckEnabled);
        }
    }

    private void initApplicationDefaultURLs() {
        _applicationDefaultURLs = getConfigurationMap(
                                    CONFIG_POSTDATA_PRESERVE_NOENTRY_URL);
        if (isLogMessageEnabled()) {
            logMessage("PostSSOPDPTaskHandler: cache noentry URLs: " +
                    _applicationDefaultURLs);
        }
    }

    private StringBuilder createInputTag(String name, String[] values) {
        StringBuilder result = new StringBuilder();
        if (values != null && values.length > 0) {
            for (int i = 0; i < values.length; i++) {
                result.append(String.format(HTML_FORM_INPUT_FIELD,
                                    name, values[i]));
            }
        }
        return result;
    }

    private String createForm(IPDPCacheEntry entry) {

        StringBuilder result = new StringBuilder();
        result.append(String.format(HTML_PAGE_FORM_HEAD,
                             entry.getOriginalURL()));
        Map parameters = entry.getParameterMap();
        for (Iterator i = parameters.keySet().iterator(); i.hasNext(); ) {
            String keyName = (String)i.next();
            String[] keyValues = (String[])parameters.get(keyName);
            result.append(createInputTag(keyName, keyValues));
        }
        result.append(HTML_PAGE_FORM_TAIL);

        return result.toString();
    }
    private boolean _isPDPCheckEnabled;

    private static Map _applicationDefaultURLs;
    private static final String HTML_PAGE_FORM_HEAD="<html><body>" +
        "<form name=\"pdpForm\" action=\"%s\" method=\"POST\">";
    private static final String HTML_FORM_INPUT_FIELD="<input type=\"hidden\"" +
         " name=\"%s\" value=\"%s\"/>";
    private static final String HTML_PAGE_FORM_TAIL="</form>" +
        "<script type=\"text/javascript\" language=\"javascript\">" +
            "document.pdpForm.submit();" +
        "</script></body></html>";
}
