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
 * $Id: DiscoServiceManager.java,v 1.7 2008/08/06 17:28:08 exu Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.liberty.ws.disco.common;

import java.io.StringReader;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.xml.transform.stream.StreamSource;
import javax.xml.bind.*;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.liberty.ws.disco.plugins.Default64ResourceIDMapper;
import com.sun.identity.liberty.ws.disco.plugins.DiscoEntryHandler;
import com.sun.identity.liberty.ws.disco.plugins.NameIdentifierMapper;
import com.sun.identity.liberty.ws.disco.plugins.jaxb.*;
import com.sun.identity.liberty.ws.interfaces.ResourceIDMapper;
import com.sun.identity.liberty.ws.interfaces.Authorizer;
import com.sun.identity.liberty.ws.soapbinding.Utils;
import com.sun.identity.plugin.configuration.ConfigurationActionEvent;
import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.plugin.configuration.ConfigurationManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import org.xml.sax.InputSource;


/**
 * This is a singleton class. It reads the current values from Discovery
 * Service configuration and updates the values by listening to Discovery
 * Service configuration events.
 */
public class DiscoServiceManager implements ConfigurationListener {
    // constants used by this class only.
    // Don't need to publish them in DiscoConstants.
    private static final String DISCO_SERVICE =
                                "sunIdentityServerDiscoveryService";
    private static final String ATTR_PROVIDER_ID =
                                "sunIdentityServerDiscoProviderID";
    private static final String ATTR_SUPPORTED_AUTHN_MECHS =
                                "sunIdentityServerDiscoSupportedAuthnMechs";
    private static final String ATTR_SUPPORTED_DIRECTIVE =
                                "sunIdentityServerDiscoSupportedDirectives";
    private static final String ATTR_LOOKUP_NEED_POLICY =
                                "sunIdentityServerDiscoLookupNeedPolicyEval";
    private static final String LOOKUP_NEED_POLICY_DEFAULT = "false";
    private static final String ATTR_UPDATE_NEED_POLICY =
                                "sunIdentityServerDiscoUpdateNeedPolicyEval";
    private static final String UPDATE_NEED_POLICY_DEFAULT = "false";
    private static final String ATTR_AUTHORIZER =
                                "sunIdentityServerDiscoAuthorizer";
    private static final String ATTR_ENTRY_HANDLER =
                                "sunIdentityServerDiscoEntryHandler";
    private static final String ATTR_GLOBAL_ENTRY_HANDLER =
                                "sunIdentityServerGlobalDiscoEntryHandler";
    private static final String ATTR_ID_MAPPER =
                        "sunIdentityServerDiscoProviderResourceIDMapper";
    private static final String KEY_PROVIDER_ID = "providerid";
    private static final String KEY_IDMAPPER = "idmapper";
    private static final String ATTR_BOOTSTRAPPING_DISCO_ENTRY =
                                "sunIdentityServerBootstrappingDiscoEntry";
    private static final String ATTR_BOOTSTRAPPING_SESSION_CONTEXT =
                                "sunIdentityServerBootstrappingSessionContext";
    private static final String NEED_SESSION_CONTEXT_DEFAULT = "false";
    private static final String ATTR_BOOTSTRAPPING_ENCRYPT_NI =
                "sunIdentityServerBootstrappingEncryptNIinSessionContext";
    private static final String ENCRYPT_NI_DEFAULT = "false";
    private static final String ATTR_BOOTSTRAPPING_IMPLIED_RESOURCE =
                                "sunIdentityServerBootstrappingImpliedResource";
    private static final String USE_IMPLIED_RESOURCE_DEFAULT = "false";
    private static final String ATTR_OPTION_SECURITY_RESPONSE =
                                "sunIdentityServerDiscoOptionSecurityResponse";
    private static final String USE_RESPONSE_AUTHENTICATION_DEFAULT = "false";
    private static final String ATTR_NAMEID_MAPPER = 
        "sunIdentityServerDiscoNameIdentifierMapper";


    private static Debug debug = Debug.getInstance("libIDWSF");
    private static ConfigurationInstance ci = null;
    private static JAXBContext jc = null;
    private static String selfProviderID = null;
    private static Set authnMechs = null;
    private static Set supportedDirectives = null;
    private static boolean policyEvalLookup = false;
    private static boolean policyEvalUpdate = false;
    private static Authorizer authorizer = null;
    private static DiscoEntryHandler entryHandler = null;
    private static DiscoEntryHandler globalEntryHandler = null;
    private static Map idMappers = null;
    private static String bootDiscoEntryStr = null;
    private static boolean requireSessionContextStmt = false;
    private static boolean encryptNI = false;
    private static boolean useImpliedRes = false;
    private static boolean useRespAuth = false;
    private static NameIdentifierMapper nameIdMapper = null;

    private DiscoServiceManager() {
    }

    static {
        try {
            ci = ConfigurationManager.getConfigurationInstance("DISCO");
            ci.addListener(new DiscoServiceManager());
            jc = JAXBContext.newInstance(Utils.getJAXBPackages());
	    setValues();
        } catch (ConfigurationException ce) {
            debug.error("DiscoServiceManager.static:", ce);
        } catch (JAXBException jex) {
            debug.error("DiscoServiceManager.static: Unable to " +
                "get JAXBContext:", jex);
        }
    }

    /**
     * This method will be invoked when a component's 
     * configuration data has been changed. The parameters componentName,
     * realm and configName denotes the component name,
     * organization and configuration instance name that are changed 
     * respectively.
     *
     * @param e Configuration action event, like ADDED, DELETED, MODIFIED etc.
     */
    public void configChanged(ConfigurationActionEvent e) {
        debug.message("DiscoServiceManager.configChanged.");
        setValues();
    }

    /**
     * Returns the provider ID for Discovery Service. Null would be returned
     * if it's not configured in the admin console. During installation, a
     * default value will be configured.
     * @return provider ID of discovery service.
     */
    public static synchronized String getDiscoProviderID() {
        return selfProviderID;
    }

    /**
     * Returns the Set of <code>SecurityMechID</code>s that the discovery
     * service supports. A set of default values will be configured during
     * installation time.
     * @return Set of <code>SecurityMechID</code>s that the discovery service
     *  supports.
     */
    public static Set getSupportedAuthenticationMechanisms() {
        return authnMechs;
    }

    /**
     * Returns the Set of <code>Directive</code>s that the discovery service
     * supports.
     * @return Set of <code>Directive</code>s the discovery service supports.
     */
    public static Set getSupportedDirectives() {
        return supportedDirectives;
    }

    /**
     * Returns a boolean value which indicates whether policy evaluation is
     * needed for discovery lookup.
     * @return true if policy evaluation is needed for discovery lookup; false
     *  otherwise.
     */
    public static boolean needPolicyEvalLookup() {
        return policyEvalLookup;
    }

    /**
     * Returns a boolean value which indicates whether policy evaluation is
     * needed for discovery update.
     * @return true if policy evaluation is needed for discovery update; false
     *  otherwise.
     */
    public static boolean needPolicyEvalUpdate() {
        return policyEvalUpdate; }

    /**
     * Returns the <code>Authorizer</code> specified in the discovery service.
     * If no <code>Authorizer</code> is configured, an instance of
     * <code>DefaultDiscoAuthorizer</code> will be returned.
     * @return Authorizer configured in discovery service.
     */
    public static Authorizer getAuthorizer() {
        return authorizer;
    }

    /**
     * Returns the <code>NameIdentifierMapper</code> class specified in the 
     * discovery service.
     * @return instance of <code>NameIdentifierMapper</code> class. 
     *     <code>null</code> if no handler is configured, or unable to
     *     instantiate the mapper class.
     */
    public static synchronized NameIdentifierMapper getNameIdentifierMapper() {
        return nameIdMapper;
    }

    /**
     * Returns the <code>DiscoEntryHandler</code> specified in the discovery
     * service.
     * @return DiscoEntryHandler of the service. <code>null</code> if no
     *  handler is configured.
     */
    public static synchronized DiscoEntryHandler getDiscoEntryHandler() {
        return entryHandler;
    }

    /**
     * Returns the glbal <code>DiscoEntryHandler</code> for 
     * business-to-enterprise (B2E) scenarios. This handler is invoked 
     * when the resource id is implied. 
     */
    public static synchronized DiscoEntryHandler getGlobalEntryHandler() {
        return globalEntryHandler;
    }

    /**
     * Returns the <code>ResourceIDMapper</code> associated with the providerID.
     * @param providerID a provider's ID
     * @return ResourceIDMapper associated with providerID. Null will be
     * returned if <code>providerID</code> is null, or couldn't find the
     * matching <code>ResourceIDMapper</code> in the configuration. Caller
     * could call <code>DiscoServiceManager.getDefaultResourceIDMapper()</code>
     * to obtain the default <code>ResourceIDMapper</code>.
     */
    public static synchronized ResourceIDMapper getResourceIDMapper(
                                                        String providerID)
    {
        if ((idMappers == null) || (providerID == null)) {
            return null;
        }
        return ((ResourceIDMapper) idMappers.get(providerID));
    }

    /**
     * Returns the default <code>ResourceIDMapper</code> of the discovery
     * service.
     * @return ResourceIDMapper of the discovery service.
     */
    public static ResourceIDMapper getDefaultResourceIDMapper() {
        return new Default64ResourceIDMapper();
    }

    /**
     * Returns the <code>DiscoEntryElement</code> of the discovery service
     * configured for bootstrapping. Null will be returned if it's not
     * configured. A default value will be configured during installation.
     * @return Bootstrapping <code>DiscoEntryElement</code>
     */
    public static synchronized DiscoEntryElement getBootstrappingDiscoEntry() {
        DiscoEntryElement bootDiscoEntry = null;
        if ((bootDiscoEntryStr != null) && (bootDiscoEntryStr.length() != 0)) {
            try {
                Unmarshaller u = jc.createUnmarshaller();
                bootDiscoEntry = (DiscoEntryElement) u.unmarshal(
                        XMLUtils.createSAXSource(new InputSource(new StringReader(bootDiscoEntryStr))));
            } catch (Exception e) {
                debug.error("DiscoServiceManager.setValues: "
                        + "Exception when creating Disco Resource Offering:",e);
                bootDiscoEntry = null;
            }
        } else {
            if (debug.messageEnabled()) {
                debug.message("DiscoServiceManager.setValues: "
                        + "no Discovery Resource Offering specified.");
            }
            bootDiscoEntry = null;
        }

        return bootDiscoEntry;
    }

    /**
     * Updates discovery service cache.
     */
    private static synchronized void setValues() {
        try {
            Map attrsMap = ci.getConfiguration(null, null);
            // self provider id
            selfProviderID = CollectionHelper.getMapAttr(
                attrsMap, ATTR_PROVIDER_ID);

            // supported security mech id
            authnMechs = (Set) attrsMap.get(ATTR_SUPPORTED_AUTHN_MECHS);

            // supported directives
            supportedDirectives = (Set) attrsMap.get(ATTR_SUPPORTED_DIRECTIVE);

            policyEvalLookup = Boolean.valueOf(CollectionHelper.getMapAttr(
                attrsMap, ATTR_LOOKUP_NEED_POLICY,
                    LOOKUP_NEED_POLICY_DEFAULT)).booleanValue();

            policyEvalUpdate = Boolean.valueOf(CollectionHelper.getMapAttr(
                attrsMap, ATTR_UPDATE_NEED_POLICY,
                    UPDATE_NEED_POLICY_DEFAULT)).booleanValue();

            authorizer = null;
            // authorizer
            String authorizerName = CollectionHelper.getMapAttr(
                attrsMap, ATTR_AUTHORIZER);
            if ((authorizerName != null) && (authorizerName.length() != 0)) {
                try {
                    authorizer = (Authorizer) Class.
                                forName(authorizerName).newInstance();
                } catch (Exception e) {
                    if (debug.messageEnabled()) {
                        debug.error("DiscoServiceManager.setValues: "
                        + "Exception when instantiating authorizer. Using "
                        + "default Authorizer. Exception", e);
                    }
                }
            }

            // entry handler
            String handlerName = CollectionHelper.getMapAttr(
                attrsMap, ATTR_ENTRY_HANDLER);
            if ((handlerName != null) && (handlerName.length() != 0)) {
                try {
                    entryHandler = (DiscoEntryHandler) Class.
                                forName(handlerName).newInstance();
                } catch (Exception e) {
                    if (debug.messageEnabled()) {
                        debug.error("DiscoServiceManager.setValues: "
                        + "Exception when instantiating entry handler:", e);
                    }
                }
            }

            String globalHandler = CollectionHelper.getMapAttr(
                    attrsMap, ATTR_GLOBAL_ENTRY_HANDLER);
            if ((globalHandler != null) && (globalHandler.length() != 0)) {
                try {
                    globalEntryHandler = (DiscoEntryHandler) Class.
                                forName(globalHandler).newInstance();
                } catch (Exception e) {
                    if (debug.messageEnabled()) {
                        debug.error("DiscoServiceManager.setValues: Exception"+
                         " when instantiating global entry handler:", e);
                    }
                }
            }

            // Name Identifier Mapper 
            String niMapperName = CollectionHelper.getMapAttr(
                attrsMap, ATTR_NAMEID_MAPPER);
            if ((niMapperName != null) && (niMapperName.length() != 0)) {
                try {
                    if (debug.messageEnabled()) {
                        debug.message("DiscoServiceManager.setValues: "
                            + "disco name id mapper=" + niMapperName);
                    }
                    nameIdMapper = (NameIdentifierMapper) Class.
                        forName(niMapperName).newInstance();
                } catch (Exception e) {
                    if (debug.messageEnabled()) {
                        debug.error("DiscoServiceManager.setValues: "
                        + "Exception when instantiating nameid mapper:", e);
                    }
                }
            }

            // the syntax for each set value is:
            // providerid=<providerid>|idmapper=<the class for ResourceIDMapper>
            Set values = (Set)attrsMap.get(ATTR_ID_MAPPER);
            Map newIDMapper = new HashMap();
            if (values != null) {
                for (Iterator iter = values.iterator(); iter.hasNext();) {
                    String value = (String)iter.next();
                    StringTokenizer stz = new StringTokenizer(value, "|");
                    if (stz.countTokens() == 2) {
                        String providerID = null;
                        ResourceIDMapper mapper = null;
                        while(stz.hasMoreTokens()) {
                            String token = stz.nextToken();
                            int pos = -1;
                            // ignore the attribute if it doesn't include "="
                            if ((pos = token.indexOf("=")) == -1) {
                                debug.error("DiscoServiceManager.set"
                                + "Values: illegal format for ResourceIDMapper:"
                                + token);
                                break;
                            }
                            // ignore the attribute if it is like "providerid="
                            int nextpos = pos + 1;
                            if (nextpos >= token.length()) {
                                debug.error("DiscoServiceManager.set"
                                + "Values: illegal format of ResourceIDMapper:"
                                + token);
                                break;
                            }
                            String key = token.substring(0, pos);
                            if (key.equalsIgnoreCase(KEY_PROVIDER_ID)) {
                                providerID = token.substring(nextpos);
                            } else if (key.equalsIgnoreCase(KEY_IDMAPPER)) {
                                try {
                                    mapper = (ResourceIDMapper) Class.
                                        forName(token.substring(nextpos)).
                                        newInstance();
                                } catch (Exception e) {
                                    debug.error("DiscoServiceManager"
                                    + ".setValues: couldn't instantiate "
                                    + "ResourceIDMapper: " + token + ":", e);
                                    break;
                                }
                            } else {
                                debug.error("DiscoServiceManager.set"
                                + "Values: illegal format of ResourceIDMapper:"
                                + token);
                                break;
                            }
                        }
                        if ((providerID == null) || (mapper == null)) {
                            debug.error("DiscoServiceManager.set"
                                + "Values: Invalid syntax for "
                                + "ResourceIDMapper:" + value);
                        } else {
                            newIDMapper.put(providerID, mapper);
                        }
                    } else {
                        if (debug.warningEnabled()) {
                            debug.warning("DiscoServiceManager.set"
                                + "Values: Invalid syntax for ResourceIDMapper:"
                                + value);
                        }
                    }
                }
            }
            idMappers = newIDMapper;

            // disco resource offering for bootstrapping
            bootDiscoEntryStr = CollectionHelper.getMapAttr(
                attrsMap, ATTR_BOOTSTRAPPING_DISCO_ENTRY);

            tagswapBootDiscoEntry();

            requireSessionContextStmt = Boolean.valueOf(
                CollectionHelper.getMapAttr(
                    attrsMap, ATTR_BOOTSTRAPPING_SESSION_CONTEXT,
                    NEED_SESSION_CONTEXT_DEFAULT)).booleanValue();
            if (debug.messageEnabled()) {
                debug.message("DiscoServiceManager.setValues: need Session "
                    + "Context Statement?" + requireSessionContextStmt);
            }

            encryptNI = Boolean.valueOf(CollectionHelper.getMapAttr(
                attrsMap, ATTR_BOOTSTRAPPING_ENCRYPT_NI,
                    ENCRYPT_NI_DEFAULT)).booleanValue();
            if (debug.messageEnabled()) {
                debug.message("DiscoServiceManager.setValues: encrypt NI in "
                    + "Session Context?" + encryptNI);
            }

            useImpliedRes = Boolean.valueOf(CollectionHelper.getMapAttr(
                attrsMap, ATTR_BOOTSTRAPPING_IMPLIED_RESOURCE,
                    USE_IMPLIED_RESOURCE_DEFAULT)).booleanValue();
            if (debug.messageEnabled()) {
                debug.message("DiscoServiceManager.setValues: use implied "
                    + "resource?" + useImpliedRes);
            }

            useRespAuth = Boolean.valueOf(CollectionHelper.getMapAttr(
                attrsMap, ATTR_OPTION_SECURITY_RESPONSE,
                    USE_RESPONSE_AUTHENTICATION_DEFAULT)).booleanValue();
            if (debug.messageEnabled()) {
                debug.message("DiscoServiceManager.setValues: use response "
                    + "authentication?" + useRespAuth);
            }
        } catch (Exception e) {
            debug.error("DiscoServiceManager.setValues: Exception", e);
        }
    }

    private static void tagswapBootDiscoEntry() {
        bootDiscoEntryStr = bootDiscoEntryStr.replaceAll(
            Constants.TAG_SERVER_PROTO, SystemConfigurationUtil.getProperty(
                Constants.AM_SERVER_PROTOCOL));
        bootDiscoEntryStr = bootDiscoEntryStr.replaceAll(
            Constants.TAG_SERVER_HOST, SystemConfigurationUtil.getProperty(
                Constants.AM_SERVER_HOST));
        bootDiscoEntryStr = bootDiscoEntryStr.replaceAll(
            Constants.TAG_SERVER_PORT, SystemConfigurationUtil.getProperty(
                Constants.AM_SERVER_PORT));
        bootDiscoEntryStr = bootDiscoEntryStr.replaceAll(
            Constants.TAG_SERVER_URI, SystemConfigurationUtil.getProperty(
                Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR));
    }

    /**
     * Returns flag used by the IDP to decide if 
     * <code>SessionContextStatement</code> needs to be generated for discovery
     * bootstrapping.
     * @return true if <code>SessionContextStatement</code> needs to be
     *  generated; false otherwise.
     */
    public static boolean needSessionContextStatement() {
        return requireSessionContextStmt;
    }

    /**
     * Returns flag used by the IDP to decide if <code>NameIdentifier</code> in
     * <code>SessionContext</code> needs to be encrypted for discovery
     * bootstrapping.
     * @return true if <code>NameIdentifier</code> in
     *  <code>SessionContext</code> needs to be encrypted; false otherwise.
     */
    public static boolean encryptNIinSessionContext() {
        return encryptNI;
    }

    /**
     * Returns flag used by Discovery Service to decide whether Response
     * is always authenticated or not.
     * @return true if response authentication is used; false otherwise.
     */
    public static boolean useResponseAuthentication() {
        return useRespAuth;
    }

    /**
     * Returns flag used by the IDP/AuthnSvc to decide whether to use
     * implied resource for discovery bootstrapping.
     * @return true if implied resource is used; false otherwise.
     */
    public static boolean useImpliedResource() {
        return useImpliedRes;
    }
}
