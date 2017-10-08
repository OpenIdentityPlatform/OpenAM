/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.i18n.apidescriptor;

/**
 * Constants for Api Descriptor I18N used across OpenAM.
 *
 * @since 14.0.0
 */
public final class ApiDescriptorConstants {

    /** Constant used as key to API Descriptor translations **/
    public static final String TRANSLATION_KEY_PREFIX = "i18n:api-descriptor/";

    /** Constant used as key to API Descriptor translations **/
    public static final String TITLE = "title";

    /** Constant used as key to API Descriptor translations **/
    public static final String DESCRIPTION = "description";

    /** Constant used as key to API Descriptor translations **/
    public static final String PATH_PARAM = "pathparam.";

    /** Constant used as key to API Descriptor translations **/
    public static final String CREATE = "create.";

    /** Constant used as key to API Descriptor translations **/
    public static final String READ = "read.";

    /** Constant used as key to API Descriptor translations **/
    public static final String UPDATE = "update.";

    /** Constant used as key to API Descriptor translations **/
    public static final String DELETE = "delete.";

    /** Constant used as key to API Descriptor translations **/
    public static final String PATCH = "patch.";

    /** Constant used as key to API Descriptor translations **/
    public static final String ACTION = "action.";

    /** Constant used as key to API Descriptor translations **/
    public static final String QUERY = "query.";

    /** Constant used as key to API Descriptor translations **/
    public static final String ERROR = "error.";

    /** Constant used as key to API Descriptor translations **/
    public static final String PARAMETER = "parameter.";

    /** Constant used as key to API Descriptor translations **/
    public static final String ID = "id.";

    /** Constant used as key to API Descriptor translations **/
    public static final String FILTER = "filter.";

    /** Constant used as key to API Descriptor translations **/
    public static final String CREATE_DESCRIPTION = CREATE + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String READ_DESCRIPTION = READ + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String UPDATE_DESCRIPTION = UPDATE + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String DELETE_DESCRIPTION = DELETE + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String ACTION_DESCRIPTION = ACTION + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String QUERY_DESCRIPTION = QUERY + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String PATCH_DESCRIPTION = PATCH + DESCRIPTION;
    
    /** Constant used as key to API Descriptor translations **/
    public static final String ERROR_400_DESCRIPTION = ERROR + "400." + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String ERROR_403_DESCRIPTION = "error.403." + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String ERROR_404_DESCRIPTION = "error.404." + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String ERROR_405_DESCRIPTION = "error.405." + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String ID_QUERY = ID + QUERY;

    /** Constant used as key to API Descriptor translations **/
    public static final String ID_QUERY_DESCRIPTION = ID_QUERY + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String PARAMETER_DESCRIPTION = PARAMETER + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String ERROR_401_DESCRIPTION = "error.401." + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String ERROR_409_DESCRIPTION = "error.409." + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String ERROR_500_DESCRIPTION = ERROR + "500." + DESCRIPTION;

    /** Constant used as key to API Descriptor translations **/
    public static final String EXAMPLE_PROVIDER = TRANSLATION_KEY_PREFIX + "ExampleProvider#";

    /** Constant used as key to {@code RecordResource} resource location **/
    public static final String RECORD_RESOURCE = TRANSLATION_KEY_PREFIX + "RecordResource#";

    /** Constant used as key to Locate ServerInfo resource translations **/
    public static final String SERVER_INFO_RESOURCE = TRANSLATION_KEY_PREFIX + "ServerInfoResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String UMA_LABEL_RESOURCE = TRANSLATION_KEY_PREFIX + "UmaLabelResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String UMA_POLICY_RESOURCE = TRANSLATION_KEY_PREFIX + "UmaPolicyResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String SERVER_VERSION_RESOURCE = TRANSLATION_KEY_PREFIX + "ServerVersionResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String DASHBOARD_RESOURCE = TRANSLATION_KEY_PREFIX + "DashboardResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String CORE_TOKEN_RESOURCE = TRANSLATION_KEY_PREFIX + "CoreTokenResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String USER_DEVICES_RESOURCE = TRANSLATION_KEY_PREFIX + "UserDevicesResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String TRUSTED_DEVICES_RESOURCE = TRANSLATION_KEY_PREFIX + "TrustedDevicesResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String PUSH_DEVICES_RESOURCE = TRANSLATION_KEY_PREFIX + "PushDevicesResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String OATH_DEVICES_RESOURCE = TRANSLATION_KEY_PREFIX + "OathDevicesResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String SCRIPT_RESOURCE = TRANSLATION_KEY_PREFIX + "ScriptResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String RESOURCE_SET_RESOURCE = TRANSLATION_KEY_PREFIX + "ResourceSetResource#";

    /** Constant used as key to {@code KbaResource} resource bundle location **/
    public static final String KBA_RESOURCE = TRANSLATION_KEY_PREFIX + "KbaResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String PENDING_REQUEST_RESOURCE = TRANSLATION_KEY_PREFIX + "PendingRequestResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String OAUTH2_USER_APPLICATIONS = TRANSLATION_KEY_PREFIX + "OAuth2UserApplications#";

    /** Constant used as key to API Descriptor translations **/
    public static final String SNS_MESSAGE_RESOURCE = TRANSLATION_KEY_PREFIX + "SnsMessageResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String AUDIT_HISTORY_RESOURCE = TRANSLATION_KEY_PREFIX + "AuditUserHistoryResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String SELF_SERVICE_REQUEST_HANDLER = TRANSLATION_KEY_PREFIX + "SelfServiceRequestHandler_";

    /** Constant used as key to {@code AuditService} resource bundle location **/
    public static final String AUDIT_SERVICE = TRANSLATION_KEY_PREFIX + "AuditService#";

    /** Constant used as key to API Descriptor translations **/
    public static final String SESSION_RESOURCE = TRANSLATION_KEY_PREFIX + "SessionResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String SESSION_PROPERTIES_RESOURCE = TRANSLATION_KEY_PREFIX + "SessionPropertiesResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String SMS_RESOURCE_PROVIDER = TRANSLATION_KEY_PREFIX + "SmsResourceProvider#";

    /** Constant used as key to API Descriptor translations **/
    public static final String SERVERS_RESOURCE = TRANSLATION_KEY_PREFIX + "ServersResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String SITES_RESOURCE = TRANSLATION_KEY_PREFIX + "SitesResourceProvider#";

    /** Constant used as key to API Descriptor translations **/
    public static final String REALM_AUTH_MODULES = TRANSLATION_KEY_PREFIX + "AuthenticationModuleRealmSmsHandler#";

    /** Constant used as key to API Descriptor translations **/
    public static final String APPLICATIONS_RESOURCE = TRANSLATION_KEY_PREFIX + "ApplicationsResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String SERVER_PROPERTIES = TRANSLATION_KEY_PREFIX + "SmsServerPropertiesResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String CONSOLE = "i18n:amConsole#";

    /** Constant used as key to API Descriptor translations **/
    public static final String RESOURCE_TYPES_RESOURCE = TRANSLATION_KEY_PREFIX + "ResourceTypesResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String POLICY_RESOURCE = TRANSLATION_KEY_PREFIX + "PolicyResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String DECISION_COMBINERS_RESOURCE = TRANSLATION_KEY_PREFIX + "DecisionCombinersResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String CONDITION_TYPES_RESOURCE = TRANSLATION_KEY_PREFIX + "ConditionTypesResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String SUBJECT_TYPES_RESOURCE = TRANSLATION_KEY_PREFIX + "SubjectTypesResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String SUBJECT_ATTRIBUTES_RESOURCE_V1 = TRANSLATION_KEY_PREFIX + "SubjectAttributesResourceV1#";

    /** Constant used as key to API Descriptor translations **/
    public static final String APPLICATION_TYPES_RESOURCE = TRANSLATION_KEY_PREFIX + "ApplicationTypesResource#";

    /** Constant used as key to API Descriptor translations **/
    public static final String POLICY_RESOURCE_WITH_COPY_MOVE = TRANSLATION_KEY_PREFIX +
            "PolicyResourceWithCopyMoveSupport#";

    /** Constant used as key to API Descriptor translations **/
    public static final String SMS_AGGREGATING_AGENTS_QUERY_HANDLER = TRANSLATION_KEY_PREFIX +
            "SmsAggregatingAgentsQueryHandler#";

    /** Constant used as key to API Descriptor translations **/
    public static final String SMS_REALM_PROVIDER = TRANSLATION_KEY_PREFIX + "SmsRealmProvider#";

    private ApiDescriptorConstants() {
        // Constants class only
    }
}
