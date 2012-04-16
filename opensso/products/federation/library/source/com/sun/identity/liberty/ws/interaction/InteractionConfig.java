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
 * $Id: InteractionConfig.java,v 1.5 2008/08/06 17:28:10 exu Exp $
 *
 */

package com.sun.identity.liberty.ws.interaction;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.xml.namespace.QName;

/**
 * Class that provides access to configuration settings of interaction 
 * service and redirect based user interactions. 
 *
 */
class InteractionConfig {

    static final String WSC_SPECIFIED_INTERACTION_CHOICE 
        = "com.sun.identity.liberty.interaction.wscSpecifiedInteractionChoice";

    static final String INTERACT_IF_NEEDED 
        = "interactIfNeeded";

    static final String DO_NOT_INTERACT 
        = "doNotInteract";

    static final String DO_NOT_INTERACT_FOR_DATA 
        = "doNotInteractForData";

    static final String WSC_WILL_INCLUDE_USER_INTERACTION_HEADER_OLD
        = "com.sun.identity.liberty.interaction." +
          "wscWillInlcudeUserInteractionHeader";
        
    static final String WSC_WILL_INCLUDE_USER_INTERACTION_HEADER
        = "com.sun.identity.liberty.interaction." +
          "wscWillIncludeUserInteractionHeader";
        
    static final String WSC_WILL_REDIRECT 
        = "com.sun.identity.liberty.interaction.wscWillRedirect";
        
    static final String WSC_SPECIFIED_MAX_INTERACTION_TIME 
        = "com.sun.identity.liberty.interaction.wscSpecifiedMaxInteractionTime";

    static final int DEFAULT_WSC_MAX_INTERACTION_TIME = 60;

    static final String WSC_WILL_ENFORCE_HTTPS_CHECK 
        = "com.sun.identity.liberty.interaction.wscWillEnforceHttpsCheck";

    static final String WSP_WILL_REDIRECT 
        = "com.sun.identity.liberty.interaction.wspWillRedirect";

    static final String WSP_WILL_REDIRECT_FOR_DATA 
        = "com.sun.identity.liberty.interaction.wspWillRedirectForData";

    static final String WSP_REDIRECT_TIME 
        = "com.sun.identity.liberty.interaction.wspRedirectTime";

    static final int DEFAULT_WSP_REDIRECT_TIME = 30;

    static final String WSP_REDIRECT_HANDLER  
            = "com.sun.identity.liberty.interaction.wspRedirectHandler";

    static final String WSP_REDIRECT_HANDLER_SERVLET  = "WSPRedirectHandler";

    static final String INTERACTION_CONFIG_CLASS
            = "com.sun.identity.liberty.interaction.interactionConfigClass";

    static final String LB_WSP_REDIRECT_HANDLER  
            = "com.sun.identity.liberty.interaction.lbWspRedirectHandler";

    static final String TRUSTED_WSP_REDIRECT_HANDLERS  
            = "com.sun.identity.liberty.interaction.trustedWspRedirectHandlers";

    static final String WSP_WILL_ENFORCE_HTTPS_CHECK 
        = "com.sun.identity.liberty.interaction.wspWillEnforceHttpsCheck";

    static final String 
        WSP_WILL_ENFORCE_RETURN_TO_HOST_EQUALS_REQUEST_HOST 
        = "com.sun.identity.liberty.interaction." + 
          "wspWillEnforceReturnToHostEqualsRequestHost";

    static final String HTML_STYLE_SHEET_LOCATION 
        = "com.sun.identity.liberty.interaction.htmlStyleSheetLocation";

    static final String WML_STYLE_SHEET_LOCATION 
        = "com.sun.identity.liberty.interaction.wmlStyleSheetLocation";

    public static final String HANDLER_HOST_ID = "HandlerHostId";

    static final String YES = "yes";

    private static InteractionConfig interactionConfig = null;
    private static Debug debug = Debug.getInstance("libIDWSF");

    private QName wscSpecifiedInteractionChoice 
            = InteractionManager.QNAME_INTERACT_IF_NEEDED;

    private boolean wscWillIncludeUserInteractionHeader = true;
    private boolean wscWillRedirect = true;
    private int wscSpecifiedMaxInteractionTime 
            =  DEFAULT_WSC_MAX_INTERACTION_TIME;;
    private boolean wscWillEnforceHttpsCheck = false;
    private String wscSpecifiedConnectTo = "null";

    private boolean wspWillRedirect = true;
    private boolean wspWillRedirectForData = true;
    private int wspRedirectTime 
            = DEFAULT_WSP_REDIRECT_TIME;

    protected String wspRedirectHandler = null;
    protected String lbWspRedirectHandler = null;
    protected Map trustedWspRedirectHandlers = new HashMap();
    protected String localServerId = null;

    private boolean wspWillEnforceHttpsCheck = false;
    private boolean wspWillEnforceReturnToHostEqualsRequestHost = false;
    private String htmlStyleSheetLocation = null;
    private String wmlStyleSheetLocation = null;

    private static String interactionConfigClassName = null;


    synchronized static InteractionConfig getInstance() {

        if (interactionConfig == null) {

            interactionConfigClassName = SystemPropertiesManager.get(
                    INTERACTION_CONFIG_CLASS);
            if (debug.messageEnabled()) {
                debug.message("InteractionConfig.getInstance():"
                        + "interactionConfigClassName:" 
                        + interactionConfigClassName);
            }

            if (interactionConfigClassName == null) {
                if (debug.messageEnabled()) {
                    debug.message("InteractionConfig.getInstance():"
                            + " interactionConfigClassName not specified," 
                            + " defaulting to "
                            + " federation library InteractionConfig");
                }
                interactionConfig = new InteractionConfig();
            } else {
                try {
                    interactionConfig = (InteractionConfig)Class.forName(
                        interactionConfigClassName)
                        .newInstance();
                } catch (InstantiationException ie) {
                    debug.error("InteractionConfig.getInstance()"
                            + " Can not instantiate class:"
                            + interactionConfigClassName);
                } catch (IllegalAccessException iae) {
                    debug.error("InteractionConfig.getInstance()"
                            + " Illegal access to class:"
                            + interactionConfigClassName);
                } catch (ClassNotFoundException cnfe) {
                    debug.error("InteractionConfig.getInstance()"
                            + " class not found :"
                            + interactionConfigClassName);
                } finally {
                    if (interactionConfig == null ) {
                        if (debug.warningEnabled()) {
                            debug.warning("InteractionConfig.getInstance():"
                                    + "did not find configured class, "
                                    + " would use config class:" 
                                    +  "com.sun.identity.liberty.ws."
                                    +  "interaction.InteractionConfig");
                        }
                        interactionConfig = new InteractionConfig();
                    }
                }
            }
            if (debug.messageEnabled()) {
                debug.message("InteractionConfig.getInstance():"
                        + "created instance:" + interactionConfig.toString());
            }
        }


        return interactionConfig;
    }

    protected InteractionConfig() {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("InteractionConfig():constructed singleton instance:"
                + "with Values="+toString());
        }
    }

    boolean wscIncludesUserInteractionHeader() {
        return wscWillIncludeUserInteractionHeader;
    }

    boolean wscSupportsRedirect() {
        return wscWillRedirect;
    }

    QName getWSCSpecifiedInteractionChoice() {
        return wscSpecifiedInteractionChoice;
    }

    int getWSCSpecifiedMaxInteractionTime() {
        return wscSpecifiedMaxInteractionTime;
    }

    String getWSCSpecifiedConnectTo() {
        return wscSpecifiedConnectTo;
    }

    boolean wscEnforcesHttpsCheck() {
        return wscWillEnforceHttpsCheck;
    }

    int getWSPRedirectTime() {
        return wspRedirectTime;
    }

    String getWSPRedirectHandler() {
        return wspRedirectHandler;
    }

    String getLbWSPRedirectHandler() {
        return lbWspRedirectHandler;
    }

    Map getTrustedWSPRedirectHandlers() {
        return trustedWspRedirectHandlers;
    }

    String getLocalServerId() {
        return localServerId;
    }

    boolean wspSupportsRedirect() {
        return wspWillRedirect;
    }

    boolean wspRedirectsForData() {
        return wspWillRedirectForData;
    }

    boolean wspEnforcesHttpsCheck() {
        return wspWillEnforceHttpsCheck;
    }

    boolean wspEnforcesReturnToHostEqualsRequestHost() {
        return wspWillEnforceReturnToHostEqualsRequestHost;
    }

    String getHTMLStyleSheetLocation() {
        return htmlStyleSheetLocation;
    }

    String getWMLStyleSheetLocation() {
        return wmlStyleSheetLocation;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("InteractionConfig:wscSpecifiedInteractionChoice=" 
                + wscSpecifiedInteractionChoice);
        sb.append(":wscWillIncludeUserInteractionHeader="
                + wscWillIncludeUserInteractionHeader);
        sb.append(":wscWillRedirect=" 
                +wscWillRedirect);
        sb.append(":wscSpecifiedMaxInteractionTime=" 
                + wscSpecifiedMaxInteractionTime); 
        sb.append(":wscWillEnforceHttpsCheck="
                + wscWillEnforceHttpsCheck);
        sb.append(":wscSpecifiedConnectTo="
                + wscSpecifiedConnectTo);
    
        sb.append(":wspWillRedirect="
                + wspWillRedirect);
        sb.append(":wspWillRedirectForData="
                + wspWillRedirectForData);
        sb.append(":wspRedirectTime="
                + wspRedirectTime);
        sb.append(":wspRedirectHandler="
                + wspRedirectHandler);
        sb.append(":lbWspRedirectHandler="
                + lbWspRedirectHandler);
        sb.append(":trustedWspRedirectHandlers="
                + trustedWspRedirectHandlers);
        sb.append(":localServerId="
                + localServerId);
        sb.append(":interactionConfigClassName="
                + interactionConfigClassName);
        sb.append(":interactionConfig.getClass().getName()="
                + getClass().getName());
        sb.append(":wspWillEnforceHttpsCheck="
                + wspWillEnforceHttpsCheck);
        sb.append(":wspWillEnforceReturnToHostEqualsRequestHost="
                + wspWillEnforceReturnToHostEqualsRequestHost);
        sb.append(":htmlStyleSheetLocation="
                + htmlStyleSheetLocation);
        sb.append(":wmlStyleSheetLocation="
                + wmlStyleSheetLocation);
        return sb.toString();
    }

    protected void initialize() {

        String s = null;

        s = SystemPropertiesManager.get(WSC_SPECIFIED_INTERACTION_CHOICE);
        if (s != null) {
            if (s.equals(INTERACT_IF_NEEDED)) {
                wscSpecifiedInteractionChoice 
                        = InteractionManager.QNAME_INTERACT_IF_NEEDED;
            } else if(s.equals(DO_NOT_INTERACT)) {
                wscSpecifiedInteractionChoice 
                        = InteractionManager.QNAME_DO_NOT_INTERACT;
            } else if (s.equals(DO_NOT_INTERACT_FOR_DATA)) {
                wscSpecifiedInteractionChoice 
                        = InteractionManager.QNAME_DO_NOT_INTERACT_FOR_DATA;
            } else {

                //default
                if (debug.warningEnabled()) {
                    debug.warning("InteractionConfig.initialize():"
                            + "invalid wscSpecifiedInteractionChoice=" + s 
                            + ":defaulting to = " + INTERACT_IF_NEEDED);
                }
                wscSpecifiedInteractionChoice 
                        = InteractionManager.QNAME_INTERACT_IF_NEEDED;
            }
        } else {

            //default
            if (debug.warningEnabled()) {
                debug.warning("InteractionConfig.initialize():"
                        + "wscSpecifiedInteractionChoice not specified "
                        + ":defaulting to = " + INTERACT_IF_NEEDED);
            }
            wscSpecifiedInteractionChoice 
                    = InteractionManager.QNAME_INTERACT_IF_NEEDED;
        }

        s = SystemPropertiesManager.get(WSC_WILL_INCLUDE_USER_INTERACTION_HEADER);
        if ( s == null) {
            s = SystemPropertiesManager.get(
                    WSC_WILL_INCLUDE_USER_INTERACTION_HEADER_OLD);
        }

        if (s != null) {
            if (s.equalsIgnoreCase(YES)) {
                wscWillIncludeUserInteractionHeader = true; 
            } else {
                wscWillIncludeUserInteractionHeader = false; 
            }
        } else {

            //default
            if (debug.warningEnabled()) {
                debug.warning("InteractionConfig.initialize():"
                        + "wscWillIncludeUerInteractionHeader not specified"
                        + ":defaulting to = " + "true");
            }
            wscWillIncludeUserInteractionHeader = true; 
        }


        s = SystemPropertiesManager.get(WSC_WILL_REDIRECT);
        if (s != null) {
            if (s.equalsIgnoreCase(YES)) {
                wscWillRedirect = true; 
            } else {
                wscWillRedirect = false; 
            }
        } else {

            //default
            if (debug.warningEnabled()) {
                debug.warning("InteractionConfig.initialize():"
                        + "wscWillRedirect not specified"
                        + ":defaulting to = " + "true");
            }
            wscWillRedirect = true; 
        }

        s = SystemPropertiesManager.get(WSC_SPECIFIED_MAX_INTERACTION_TIME);
        if (s != null) {
            try {
                wscSpecifiedMaxInteractionTime = Integer.parseInt(s);
            } catch (NumberFormatException nfe) {
                debug.error("InteractionConfig.initialize():"
                        + "invalid wscSpecifiedMaxInteractionTime=" + s, nfe);
                debug.error("InteractionConfig.initialize():"
                        + "defaulting wscSpecifiedMaxInteractionTimeto=" 
                        + DEFAULT_WSC_MAX_INTERACTION_TIME);
                wscSpecifiedMaxInteractionTime 
                        = DEFAULT_WSC_MAX_INTERACTION_TIME;
            }
        } else {

            //default
            if (debug.warningEnabled()) {
                debug.warning("InteractionConfig.initialize():"
                        + "wscSpecifiedMaxInteractionTime not specified"
                        + ":defaulting to = " 
                        + DEFAULT_WSC_MAX_INTERACTION_TIME);
            }
            wscSpecifiedMaxInteractionTime 
                    = DEFAULT_WSC_MAX_INTERACTION_TIME;
        }

        s = SystemPropertiesManager.get(WSC_WILL_ENFORCE_HTTPS_CHECK);
        if (s != null) {
            if (s.equalsIgnoreCase(YES)) {
                wscWillEnforceHttpsCheck = true; 
            } else {
                wscWillEnforceHttpsCheck = false; 
            }
        } else {

            //default
            if (debug.warningEnabled()) {
                debug.warning("InteractionConfig.initialize():"
                        + "wscWillEnforceHttpsCheck not specified"
                        + ":defaulting to = " + "true");
            }
            wscWillEnforceHttpsCheck = true; 
        }

        s = SystemPropertiesManager.get(WSP_WILL_REDIRECT);
        if (s != null) {
            if (s.equalsIgnoreCase(YES)) {
                wspWillRedirect = true; 
            } else {
                wspWillRedirect = false; 
            }
        } else {

            //default
            if (debug.warningEnabled()) {
                debug.warning("InteractionConfig.initialize():"
                        + "wspWillRedirect not specified"
                        + ":defaulting to = " + "true");
            }
            wspWillRedirect = true; 
        }

        s = SystemPropertiesManager.get(WSP_WILL_REDIRECT_FOR_DATA);
        if (s != null) {
            if (s.equalsIgnoreCase(YES)) {
                wspWillRedirectForData = true; 
            } else {
                wspWillRedirectForData = false; 
            }
        } else {

            //default
            if (debug.warningEnabled()) {
                debug.warning("InteractionConfig.initialize():"
                        + "wspWillRedirectForData not specified"
                        + ":defaulting to = " + "true");
            }
            wspWillRedirectForData = true; 
        }


        s = SystemPropertiesManager.get(WSP_REDIRECT_TIME);
        if (s != null) {
            try {
                wspRedirectTime = Integer.parseInt(s);
            } catch (NumberFormatException nfe) {
                debug.error("InteractionConfig.initialize():"
                        + "invalid wspRedirectTime=" + s, nfe);
                debug.error("InteractionConfig.initialize():"
                        + "defaulting wspRedirectTime=" 
                        + DEFAULT_WSP_REDIRECT_TIME);
                wspRedirectTime 
                        = DEFAULT_WSP_REDIRECT_TIME;
            }
        } else {

            //default
            if (debug.warningEnabled()) {
                debug.warning("InteractionConfig.initialize():"
                        + "wspRedirectTime not specified"
                        + ":defaulting to = " 
                        + DEFAULT_WSP_REDIRECT_TIME);
            }
            wspRedirectTime 
                    = DEFAULT_WSP_REDIRECT_TIME;
        }

        s = SystemPropertiesManager.get(WSP_WILL_ENFORCE_HTTPS_CHECK);
        if (s != null) {
            if (s.equalsIgnoreCase(YES)) {
                wspWillEnforceHttpsCheck = true; 
            } else {
                wspWillEnforceHttpsCheck = false; 
            }
        } else {

            //default
            if (debug.warningEnabled()) {
                debug.warning("InteractionConfig.initialize():"
                        + "wspWillEnforceHttpsCheck not specified"
                        + ":defaulting to = " + "true");
            }
            wspWillEnforceHttpsCheck = true; 
        }

        s = SystemPropertiesManager.get(
                WSP_WILL_ENFORCE_RETURN_TO_HOST_EQUALS_REQUEST_HOST);
        if (s != null) {
            if (s.equalsIgnoreCase(YES)) {
                wspWillEnforceReturnToHostEqualsRequestHost = true; 
            } else {
                wspWillEnforceReturnToHostEqualsRequestHost = false; 
            }
        } else {

            //default
            if (debug.warningEnabled()) {
                debug.warning("InteractionConfig.initialize():"
                        + "wspWillEnforceReturnToHostEqualsRequestHost not "
                        + " specified:defaulting to = " + "true");
            }
            wspWillEnforceReturnToHostEqualsRequestHost = true; 
        }

        wspRedirectHandler 
                = SystemPropertiesManager.get(WSP_REDIRECT_HANDLER);
        if (wspRedirectHandler == null) {
            debug.error("InteractionConfig.initialize():"
                    + "wspRedirectHandler is null");
        }

        lbWspRedirectHandler 
                = SystemPropertiesManager.get(LB_WSP_REDIRECT_HANDLER);
        if (lbWspRedirectHandler == null) {
            if(debug.messageEnabled()) {
                debug.message("InteractionConfig.initialize():"
                        + "lbWspRedirectHandler is null");
            }
        }

        String trustedRedirectHandlersString  
                = SystemPropertiesManager.get(TRUSTED_WSP_REDIRECT_HANDLERS);
        if (trustedRedirectHandlersString == null) {
            if(debug.messageEnabled()) {
                debug.message("InteractionConfig.initialize():"
                        + "trustedRedirectHandlersString is null");
            }
        } else {
            StringTokenizer st 
                    = new StringTokenizer(trustedRedirectHandlersString, " ");
            int handlerId = 1;
            while (st.hasMoreTokens()) {
                trustedWspRedirectHandlers.put(Integer.toString(handlerId), 
                        st.nextToken());
                handlerId++;
            }
        }


        localServerId = null;
        for (Iterator iter = trustedWspRedirectHandlers.keySet().iterator();
                iter.hasNext(); ) {
            String key = (String)iter.next();
            if (wspRedirectHandler.equals(trustedWspRedirectHandlers.get(key))) {
                localServerId = key;
                break;
            }
        }

        if (localServerId == null) {
            debug.error("WSPRedirectHandlerServlet.handleRequest():"
                    + "local serverId is null for wspRedirectHandler:"
                    + wspRedirectHandler);
        }

        htmlStyleSheetLocation 
                = SystemPropertiesManager.get(HTML_STYLE_SHEET_LOCATION);
        if (htmlStyleSheetLocation == null) {
            debug.error("InteractionConfig.initialize():"
                    + "htmlStyleSheetLocation is null");
        }

        wmlStyleSheetLocation 
                = SystemPropertiesManager.get(WML_STYLE_SHEET_LOCATION);
        if (wmlStyleSheetLocation == null) {
            debug.error("InteractionConfig.initialize():"
                    + "wmlStyleSheetLocation is null");
        }
    }

}
