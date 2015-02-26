/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IdConstants.java,v 1.9 2008/08/19 19:09:09 veiming Exp $
 *
 */

package com.sun.identity.idm;

/**
 *
 * @supported.all.api
 */
public interface IdConstants {

    // The properties below are Id Repo service config attributes to be
    // read by the SDK when it needs to determine which configs to call.
    public static final String REPO_SERVICE = "sunIdentityRepositoryService";

    public static final String SUPPORTED_TYPES = "SupportedIdentities";

    public static final String ATTRIBUTE_COMBINER =
        "sunIdRepoAttributeCombiner";

    public static final String ID_REPO = "sunIdRepoClass";

    public static final String NAMING_ATTR = "sunIdRepoNamingAttribute";

    public static final String ATTR_MAP = "sunIdRepoAttributeMapping";

    public static final String SUPPORTED_OP = "sunIdRepoSupportedOperations";

    public static final String ORGANIZATION_ALIAS_ATTR = 
        "sunOrganizationAliases";

    public static final String ORGANIZATION_STATUS_ATTR = 
        "sunOrganizationStatus";

    public static final String AMSDK_PLUGIN = "com.iplanet.am.sdk.AMSDKRepo";

    public static final String SPECIAL_PLUGIN = 
        "com.sun.identity.idm.plugins.internal.SpecialRepo";

    public static final String AGENTREPO_PLUGIN = 
        "com.sun.identity.idm.plugins.internal.AgentsRepo";

    public static final String AMSDK_PLUGIN_NAME = "amSDK";

    public static final String ATTR_MEMBER_OF = "canBeMemberOf";

    public static final String ATTR_HAVE_MEMBERS = "canHaveMembers";

    public static final String ATTR_ADD_MEMBERS = "canAddMembers";

    public static final String SERVICE_NAME = "servicename";
    
    public static final String SLASH_SEPARATOR = "/";

    public static final String SERVICE_ATTRS = 
        "sun-idrepo-ldapv3-config-service-attributes";

    /**
     *  amadmin user from SunIdentityRepositoryService
     */
    public static final String AMADMIN_USER = "amadmin";


    /**
     *  anonymous user from SunIdentityRepositoryService
     */
    public static final String ANONYMOUS_USER = "anonymous";

    // The properties below are OpenSSO Agent service config
    // attributes to be
    // read by the SDK when it needs to determine which configs to call.
    public static final String AGENT_SERVICE = "AgentService";

    public static final String AGENT_TYPE = "AgentType";

    public static final String IDREPO_CACHESTAT = "idRepoCacheStat";

}
