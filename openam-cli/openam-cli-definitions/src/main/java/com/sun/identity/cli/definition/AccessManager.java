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
 * $Id: AccessManager.java,v 1.115 2010/01/15 18:10:54 veiming Exp $
 *
 */

/**
 * Portions Copyrighted 2010-2013 ForgeRock Inc
 */
package com.sun.identity.cli.definition;


import com.sun.identity.cli.annotation.DefinitionClassInfo;
import com.sun.identity.cli.annotation.Macro;
import com.sun.identity.cli.annotation.SubCommandInfo;
import com.sun.identity.cli.annotation.ResourceStrings;

public class AccessManager {
    @DefinitionClassInfo(
        productName="OpenAM",
        logName="amadm",
        resourceBundle="AccessManagerCLI")
    private String product;

    @ResourceStrings(
        string={"resourcebundle-not-found=Resource Bundle not found.",
            "realm-does-not-exist=Could not process the request because realm {0} did not exist.",
            "identity-does-not-exist=Could not process the request because identity {0} did not exist.",
        "missing-attributevalues=attributevalues and datafile options are missing.",
        "missing-choicevalues=choicevalues and datafile options are missing.",
        "attribute-schema-not-exist=Attribute schema {0} did not exist.",
        "serverconfig-no-supported=This sub command is not supported because platform service is not upgraded.",
        "application-type-invalid=Application Type {0} is invalid.",
        "actions-required=actions attributes are required. Example: get=true",
        "resources-required=resources attributes are required. This defines the resources that are supported by this application.",
        "subjects-required=subjects attributes are required. This defines the subject plugin classes for administration console.",
        "conditions-required=conditions attributes are required. This defines the condition plugin classes for administration console.",
        "entitlement-combiner-required=entitlementCombiner attribute is required.",
        "entitlement-combiner-class-not-found=entitlementCombiner {0} class not found.",
        "entitlement-combiner-does-not-extend-superclass=entitlementCombiner {0} did not extend EntitlementCombiner base class.",
        "resource-comparator-class-not-found=resourceComparator {0} class not found.",
        "resource-comparator-does-not-extend-interface=resourceComparator {0} did not implement ResourceName interface.",
        "save-index-class-not-found=saveIndexImpl {0} class not found.",
        "save-index-does-not-extend-interface=saveIndexImpl {0} did not implement ISaveIndex interface.",
        "search-index-class-not-found=searchIndexImpl {0} class not found.",
        "search-index-does-not-extend-interface=searchIndexImpl {0} did not implement ISearchIndex interface."
        }
    )
    private String resourcestrings;

    @Macro(
        mandatoryOptions={
            "adminid|u|s|Administrator ID of running the command.",
            "password-file|f|s|File name that contains password of administrator."},
        optionalOptions={},
        optionAliases={})
    private String authentication;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.BulkOperations",
        description="Do multiple requests in one command.",
        webSupport="true",
        mandatoryOptions={
            "batchfile|Z|s|Name of file that contains commands and options."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "batchstatus|b|s|Name of status file.",
            "continue|c|u|Continue processing the rest of the request when preceeding request was erroneous."},
        resourceStrings={
            "bulk-op-empty-datafile=Batch file, {0} was empty.",
            "unmatch-quote=Unmatched '.",
            "unmatch-doublequote=Unmatched \"."})
    private String do_batch;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.SessionCommand",
        description="List Sessions.",
        webSupport="false",
        mandatoryOptions={
            "host|t|s|Host Name."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "filter|x|s|Filter (Pattern).",
            "quiet|q|u|Do not prompt for session invalidation."},
        resourceStrings={
            "session-invalid-host-name=Invalid Host Name {0}. Expected format was <protocol>://<host>:<port>.",
            "sizeLimitExceeded=Search size limit exceeded. Please refine your search.",
            "timeLimitExceeded=Search time limit exceeded. Please refine your search.",
            "session-no-sessions=There were no valid sessions.",
            "session-current-session=[Current Session]",
            "session-index=Index:",
            "session-userId=User Id:",
            "session-time-remain=Time Remain:",
            "session-max-session-time=Max Session Time:",
            "session-idle-time=Idle Time:",
            "session-max-idle-time=Max Idle Time:",
            "session-to-invalidate=To invalidate sessions, enter the index numbers",
            "session-cr-to-exit=[CR without a number to exit]:",
            "session-selection-not-in-list=Your selection was not in the session list.",
            "session-io-exception-reading-input=IO Exception reading input:",
            "session-destroy-session-succeeded=Destroy Session Succeeded."})
    private String list_sessions;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.AddResourceBundle",
        description="Add resource bundle to data store.",
        webSupport="true",
        mandatoryOptions={
            "bundlename|b|s|Resource Bundle Name.",
            "bundlefilename|B|s|Resource bundle physical file name."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "bundlelocale|o|s|Locale of the resource bundle."},
        resourceStrings={
            "resourcebundle-added=Resource Bundle was added."})
    private String add_res_bundle;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.GetResourceBundle",
        description="List resource bundle in data store.",
        webSupport="true",
        mandatoryOptions={
            "bundlename|b|s|Resource Bundle Name."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "bundlelocale|o|s|Locale of the resource bundle."},
        resourceStrings={
            "resourcebundle-returned=Resource Bundle was returned."})
    private String list_res_bundle;


    @SubCommandInfo(
        implClassName="com.sun.identity.cli.DeleteResourceBundle",
        description="Remove resource bundle from data store.",
        webSupport="true",
        mandatoryOptions={
            "bundlename|b|s|Resource Bundle Name."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "bundlelocale|o|s|Locale of the resource bundle."},
        resourceStrings={
            "resourcebundle-deleted=Resource Bundle was deleted."})
    private String remove_res_bundle;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.LoadSchema",
        description="Create a new service in server.",
        webSupport="true",
        mandatoryOptions={
            "xmlfile|X|m|XML file(s) that contains schema."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "continue|c|u|Continue adding service if one or more previous service cannot be added."},
        resourceStrings={
            "subcmd-create-svc-__web__-xmlfile=Service Schema XML",
            "one-or-more-services-not-added=One or more services were not added.",
            "schema-added=Service was added.",
            "schema-failed=Service was not added."})
    private String create_svc;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.DeleteService",
        description="Delete service from the server.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|m|Service Name(s)."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "continue|c|u|Continue deleting service if one or more previous services cannot be deleted.",
            "deletepolicyrule|r|u|Delete policy rule."},
        resourceStrings={
            "one-or-more-services-not-deleted=One or more services were not deleted.",
            "service-deleted=Service was deleted.",
            "service-deletion-failed=Service was not deleted.",
            "delete-service-no-policy-rules=There were no policy rules.",
            "delete-service-no-policy-schema=There were no policy schema.",
            "delete-service-delete-policy-rules=Delete policy rules."})
    private String delete_svc;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.UpdateService",
        description="Update service.",
        webSupport="true",
        mandatoryOptions={
            "xmlfile|X|m|XML file(s) that contains schema."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "continue|c|u|Continue updating service if one or more previous services cannot be updated."},
        resourceStrings={
            "subcmd-update-svc-__web__-xmlfile=Service Schema XML",
            "service-updated=Schema was updated.",
            "service-updated-failed=Schema was not updated."})
    private String update_svc;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.AddAttributeSchema",
        description="Add attribute schema to an existing service.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Service Name.",
            "schematype|t|s|Schema Type.",
            "attributeschemafile|F|m|XML file containing attribute schema definition."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "subcmd-add-attrs-__web__-attributeschemafile=Attribute Schema XML",
            "attribute-schema-added=Attribute schema was added.",
            "add-attribute-schema-failed=Attribute schema was not added."})
    private String add_attrs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.SMSMigration",
        description="Migrate organization to realm.",
        webSupport="true",
        mandatoryOptions={
            "entrydn|e|s|Distinguished name of organization to be migrated."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "sms-migration-succeed=Migration succeeded."})
    private String do_migration70;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.CreateRealm",
        description="Create realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm to be created."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "create-realm-succeed=Realm was created."})
    private String create_realm;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.DeleteRealm",
        description="Delete realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm to be deleted."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "recursive|r|u|Delete descendent realms recursively."},
        resourceStrings={
            "delete-realm-succeed=Realm was deleted."})
    private String delete_realm;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.SearchRealms",
        description="List realms by name.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm where search begins."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "filter|x|s|Filter (Pattern).",
            "recursive|r|u|Search recursively"},
        resourceStrings={
            "search-realm-succeed=Search completed.",
            "search-realm-no-results=There were no realms.",
            "search-realm-results={0}"})
    private String list_realms;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.RealmAssignService",
        description="Add service to a realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "servicename|s|s|Service Name."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains attribute values data."},
        resourceStrings={
            "assign-service-to-realm-succeed=Service, {1} was added to realm, {0}."})
    private String add_svc_realm;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.RealmGetAssignedServices",
        description="Show services in a realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "mandatory|y|u|Include Mandatory services."},
        resourceStrings={
            "realm-get-assigned-services-succeed=Services were returned.",
            "realm-get-assigned-services-no-services=There no services in this realm.",
            "realm-get-assigned-services-results={0}"})
    private String show_realm_svcs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.RealmGetAssignableServices",
        description="List the assignable services to a realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "realm-getassignable-services-succeed=Assignable Services were returned.",
            "realm-getassignable-services-no-services=There were no assignable services for this realm.",
            "realm-getassignable-services-result={0}"})
    private String list_realm_assignable_svcs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.RealmUnassignService",
        description="Remove service from a realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "servicename|s|s|Name of service to be removed."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "unassign-service-from-realm-succeed=Service, {1} was removed from realm, {0}.",
            "unassign-service-from-realm-service-not-assigned=Service, {1} was not added to realm, {0}."})
    private String remove_svc_realm;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.RealmGetAttributeValues",
        description="Get realm property values.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "servicename|s|s|Name of service."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "get-attr-values-of-realm-no-values={0} had no attributes.",
            "get-attr-values-of-realm-result={0}={1}"})
    private String get_realm;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.RealmGetServiceAttributeValues",
        description="Get realm's service attribute values.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "servicename|s|s|Name of service."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "get-service-attr-values-of-realm-no-attr=There were no attribute values.",
            "get-service-attr-values-of-realm-result={0}={1}"})
    private String get_realm_svc_attrs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.RealmRemoveAttribute",
        description="Delete attribute from a realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "servicename|s|s|Name of service.",
            "attributename|a|s|Name of attribute to be removed."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "remove-attribute-from-realm-succeed=Attribute was removed."})
    private String delete_realm_attr;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.RealmModifyService",
        description="Set service attribute values in a realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "servicename|s|s|Name of service."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains attribute values data."},
        resourceStrings={
            "modify-service-of-realm-succeed={1} under {0} was modified.",
            "modify-service-of-realm-not-assigned=Service, {1} was not modified because it was not added to {0}."})
    private String set_svc_attrs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.RealmRemoveServiceAttributes",
        description="Remove service attribute values in a realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "servicename|s|s|Name of service."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values to be removed e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains attribute values to be removed."},
        resourceStrings={
            "realm-remove-service-attributes-succeed=The following attributes were removed.",
            "realm-remove-service-attributes-not-assigned=Service, {1} was not modified because it was not added to {0}."})
    private String remove_svc_attrs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.RealmAddServiceAttributes",
        description="Add service attribute values in a realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "servicename|s|s|Name of service."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values to be added e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains attribute values to be added."},
        resourceStrings={
            "realm-add-service-attributes-succeed=The following attributes were added.",
            "realm-add-service-attributes-not-assigned=Service, {1} was not modified because it was not added to {0}."})
    private String add_svc_attrs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.RealmSetServiceAttributeValues",
        description="Set attribute values of a service that is assigned to a realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "servicename|s|s|Name of service."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "append|p|u|Set this flag to append the values to existing ones.",
            "attributevalues|a|m|Attribute values e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains attribute values data."},
        resourceStrings={
            "realm-set-svc-attr-values-service-not-assigned=Service was not assigned to realm.",
            "set-svc-attribute-values-realm-succeed=Attribute values were set."})
    private String set_realm_svc_attrs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.RealmSetAttributeValues",
        description="Set attribute values of a realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "servicename|s|s|Name of service."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "append|p|u|Set this flag to append the values to existing ones.",
            "attributevalues|a|m|Attribute values e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains attribute values data."},
        resourceStrings={
            "add-attribute-values-realm-succeed=Attribute values were added.",
            "set-attribute-values-realm-succeed=Attribute values were set."})
    private String set_realm_attrs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.RealmCreatePolicy",
        description="Create policies in a realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "xmlfile|X|s|Name of file that contains policy XML definition."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "subcmd-create-policies-__web__-xmlfile=Policy XML",
            "create-policy-in-realm-succeed=Policies were created under realm, {0}."})
    private String create_policies;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.RealmDeletePolicy",
        description="Delete policies from a realm.",
        webSupport="true",
        mandatoryOptions={"realm|e|s|Name of realm."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "policynames|p|m|Names of policy to be deleted.",
            "file|D|s|Name of file that contains the policy names to be deleted."},
        resourceStrings={
            "missing-policy-names=Policy names need to be provided either with --policynames or --file option",
            "delete-policy-in-realm-succeed=Policies were deleted under realm, {0}."})
    private String delete_policies;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.RealmUpdatePolicy",
        description="Update policies in a realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "xmlfile|X|s|Name of file that contains policy XML definition."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "update-policy-in-realm-name-not-found=The policy provided did not exist in the policy store with the same name. If this is a new policy then run create-policies.",
            "update-policy-in-realm-succeed=Policies were updated under realm, {0}."})
    private String update_policies;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.RealmGetPolicy",
        description="List policy definitions in a realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "policynames|p|m|Names of policy. This can be an wildcard. All policy definition in the realm will be returned if this option is not provided.",
            "outfile|o|s|Filename where policy definition will be printed to. Definition will be printed in standard output if this option is not provided.",
            "namesonly|n|u|Returns only names of matching policies. Policies are not returned." },
        resourceStrings={
            "get-policy-names-in-realm-succeed=Policy names were returned under realm, {0}.",
            "get-policy-names-in-realm-no-policies=There were not matching policy names under realm, {0}.",
            "get-policy-in-realm-succeed=Policy definitions were returned under realm, {0}.",
            "get-policy-in-realm-no-policies=There were not matching policies under realm, {0}."})
    private String list_policies;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.RemoveAttributeDefaults",
        description="Remove default attribute values in schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "attributenames|a|m|Attribute name(s)."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "schema-remove-attribute-defaults-succeed=Schema attribute defaults were removed.",
            "schema-sub-schema-does-not-exists=Sub Schema did not exist, {0}.",
            "supported-schema-type=Unsupported Schema Type, {0}."})
    private String remove_attr_defs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.AddAttributeDefaults",
        description="Add default attribute values in schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains attribute values data.",
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "schema-add-attribute-defaults-succeed=Schema attribute defaults were added."})
    private String add_attr_defs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.GetAttributeDefaults",
        description="Get default attribute values in schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema. One of dynamic, global, or organization (meaning realm)."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema.",
            "attributenames|a|m|Attribute name(s)."},
        resourceStrings={
            "schema-get-attribute-defaults-succeed=Schema attribute defaults were returned.",
            "schema-get-attribute-defaults-no-matching-attr=There were no attribute values.",
            "schema-get-attribute-defaults-result={0}={1}"})
    private String get_attr_defs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.SetAttributeDefaults",
        description="Set default attribute values in schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema.",
            "attributevalues|a|m|Attribute values e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains attribute values data."},
        resourceStrings={
            "schema-set-attribute-defaults-succeed=Schema attribute defaults were set."})
    private String set_attr_defs;

    @SubCommandInfo(
        implClassName="org.forgerock.openam.cli.schema.GetAttributeSchemaChoiceValues",
        description="Get choice values of attribute schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "attributename|a|s|Name of attribute."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "attribute-schema-i18nkey=I18n Key",
            "attribute-schema-choice-value=Choice Value"})
    private String get_attr_choicevals;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.SetAttributeSchemaChoiceValues",
        description="Set choice values of attribute schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "attributename|a|s|Name of attribute."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "add|p|u|Set this flag to append the choice values to existing ones.",
            "subschemaname|c|s|Name of sub schema.",
            "datafile|D|s|Name of file that contains attribute values data.",
            "choicevalues|k|m|Choice value e.g. o102=Inactive."},
        resourceStrings={
            "attribute-schema-set-choice-value-succeed=Choice Values were set."})
    private String set_attr_choicevals;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.SetAttributeSchemaBooleanValues",
        description="Set boolean values of attribute schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "attributename|a|s|Name of attribute.",
            "truevalue|e|s|Value for true.",
            "truei18nkey|k|s|Internationalization key for true value.",
            "falsevalue|z|s|Value for false.",
            "falsei18nkey|j|s|Internationalization key for false value."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "attribute-schema-set-boolean-values-succeed=Boolean Values were set."})
    private String set_attr_bool_values;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.RemoveAttributeSchemaChoiceValues",
        description="Remove choice values from attribute schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "attributename|a|s|Name of attribute.",
            "choicevalues|k|m|Choice values e.g. Inactive"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "attribute-schema-remove-choice-value-succeed=Choice Values were removed."})
    private String remove_attr_choicevals;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.ModifyAttributeSchemaType",
        description="Set type member of attribute schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "attributeschema|a|s|Name of attribute schema",
            "type|p|s|Attribute Schema Type"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "attribute-schema-modify-type-succeed=Attribute Schema, {3} was modified."})
    private String set_attr_type;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.ModifyAttributeSchemaUIType",
        description="Set UI type member of attribute schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "attributeschema|a|s|Name of attribute schema",
            "uitype|p|s|Attribute Schema UI Type"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "attribute-schema-modify-ui-type-succeed=Attribute Schema, {3} was modified."})
    private String set_attr_ui_type;

   @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.ModifyAttributeSchemaSyntax",
        description="Set syntax member of attribute schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "attributeschema|a|s|Name of attribute schema",
            "syntax|x|s|Attribute Schema Syntax"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "attribute-schema-modify-syntax-succeed=Attribute Schema, {3} was modified."})
    private String set_attr_syntax;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.ModifyAttributeSchemaI18nKey",
        description="Set i18nKey member of attribute schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "attributeschema|a|s|Name of attribute schema",
            "i18nkey|k|s|Attribute Schema I18n Key"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "attribute-schema-modify-i18n-key-succeed=Attribute Schema, {3} was modified."})
    private String set_attr_i18n_key;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.ModifyAttributeSchemaPropertiesViewBeanURL",
        description="Set properties view bean URL member of attribute schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "attributeschema|a|s|Name of attribute schema",
            "url|r|s|Attribute Schema Properties View Bean URL"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "attribute-schema-modify-properties-view-bean-url-key-succeed=Attribute Schema, {3} was modified."})
    private String set_attr_view_bean_url;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.ModifyAttributeSchemaAny",
        description="Set any member of attribute schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "attributeschema|a|s|Name of attribute schema",
            "any|y|s|Attribute Schema Any value"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "attribute-schema-modify-any-succeed=Attribute Schema, {3} was modified."})
    private String set_attr_any;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.RemoveAttributeSchemaDefaultValues",
        description="Delete attribute schema default values.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "attributeschema|a|s|Name of attribute schema",
            "defaultvalues|e|m|Default value(s) to be deleted"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "attribute-schema-remove-default-values-succeed=Attribute Schema, {3} was modified."})
    private String delete_attr_def_values;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.SetAttributeSchemaValidator",
        description="Set attribute schema validator.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "attributeschema|a|s|Name of attribute schema",
            "validator|r|s|validator class name"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "attribute-schema-set-validator-succeed=Attribute Schema, {3} was modified."})
    private String set_attr_validator;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.SetAttributeSchemaStartRange",
        description="Set attribute schema start range.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "attributeschema|a|s|Name of attribute schema",
            "range|r|s|Start range"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "attribute-schema-set-start-range-succeed=Attribute Schema, {3} was modified."})
    private String set_attr_start_range;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.SetAttributeSchemaEndRange",
        description="Set attribute schema end range.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "attributeschema|a|s|Name of attribute schema",
            "range|r|s|End range"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "attribute-schema-set-end-range-succeed=Attribute Schema, {3} was modified."})
    private String set_attr_end_range;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.RemoveAttributeSchemas",
        description="Delete attribute schemas from a service",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "attributeschema|a|m|Name of attribute schema to be removed."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "remove-attribute-schema-succeed=Attribute schema, {3} was removed."})
    private String delete_attr;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.SetServiceSchemaI18nKey",
        description="Set service schema i18n key.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "i18nkey|k|s|I18n Key."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "service-schema-set-i18n-key-succeed=Service Schema, {0} was modified."})
    private String set_svc_i18n_key;

     @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.SetServiceSchemaPropertiesViewBeanURL",
        description="Set service schema properties view bean URL.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "url|r|s|Service Schema Properties View Bean URL"},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "service-schema-set-properties-view-bean-url-succeed=Service Schema, {0} was modified."})
    private String set_svc_view_bean_url;

     @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.SetServiceRevisionNumber",
        description="Set service schema revision number.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "revisionnumber|r|s|Revision Number"},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "service-schema-set-revision-number-succeed=Service Schema, {0} was modified."})
    private String set_revision_number;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.GetServiceRevisionNumber",
        description="Get service schema revision number.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "service-schema-get-revision-number-succeed=Revision number of service {0} was {1}."})
    private String get_revision_number;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.AddSubConfiguration",
        description="Create a new sub configuration.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "subconfigname|g|s|Name of sub configuration."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains attribute values data.",
            "realm|e|s|Name of realm (Sub Configuration shall be added to global configuration if this option is not provided).",
            "subconfigid|b|s|ID of parent configuration(Sub Configuration shall be added to root configuration if this option is not provided).",
            "priority|p|s|Priority of the sub configuration."},
        resourceStrings={
            "add-sub-configuration-succeed=Sub Configuration {1} was added.",
            "add-sub-configuration-priority-no-integer=Priority needs to be an integer.",
            "add-sub-configuration-to-realm-succeed=Sub Configuration {1} was added to realm {0}",
            "add-sub-configuration-no-global-config=There were no global configurations for service, {0}"})
    private String create_sub_cfg;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.DeleteSubConfiguration",
        description="Remove Sub Configuration.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "subconfigname|g|s|Name of sub configuration."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "realm|e|s|Name of realm (Sub Configuration shall be added to global configuration if this option is not provided)."},
        resourceStrings={
            "delete-sub-configuration-succeed=Sub Configuration {1} was deleted.",
            "delete-sub-configuration-to-realm-succeed=Sub Configuration {1} was deleted from realm {0}"})
    private String delete_sub_cfg;

    @SubCommandInfo(
        implClassName="org.forgerock.openam.cli.schema.GetSubConfiguration",
        description="Get sub configuration.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "subconfigname|g|s|Name of sub configuration."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "realm|e|s|Name of realm (Sub Configuration shall be added to global configuration if this option is not provided)."},
        resourceStrings={
            "get-sub-configuration-succeed=Sub Configuration, {0} was retrieved.",
            "get-sub-configuration-to-realm-succeed=Sub Configuration, {1} was retrieved in realm {0}"})
    private String get_sub_cfg;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.ModifySubConfiguration",
        description="Set sub configuration.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "subconfigname|g|s|Name of sub configuration.",
            "operation|o|s|Operation (either add/set/modify) to be performed on the sub configuration."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains attribute values data.",
            "realm|e|s|Name of realm (Sub Configuration shall be added to global configuration if this option is not provided)."},
        resourceStrings={
            "modify-sub-configuration-succeed=Sub Configuration, {0} was modified.",
            "modify-sub-configuration-to-realm-succeed=Sub Configuration, {1} was modify in realm, {0}",
            "modify-sub-configuration-invalid-operation=Invalid operation, supported operation were 'add', 'delete' and 'set'."})
    private String set_sub_cfg;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.AddSubSchema",
        description="Add sub schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "filename|F|s|Name of file that contains the schema"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of sub schema."},
        resourceStrings={
            "subcmd-add-sub-schema-__web__-filename=Service Schema XML",
            "add-subschema-succeed=Sub Schema, {2} of type, {1} was added to service {0}."})
    private String add_sub_schema;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.RemoveSubSchema",
        description="Remove sub schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "subschemanames|a|m|Name(s) of sub schema to be removed."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "subschemaname|c|s|Name of parent sub schema."},
        resourceStrings={
            "remove-subschema-succeed={3} of Sub Schema, {2} of type, {1} was removed from service {0}."})
    private String remove_sub_schema;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.ModifyInheritance",
        description="Set Inheritance value of Sub Schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "schematype|t|s|Type of schema.",
            "subschemaname|c|s|Name of sub schema.",
            "inheritance|r|s|Value of Inheritance."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "modify-inheritance-succeed=Inheritance of Sub Schema, {2} of type, {1} in service {0} was modified."})
    private String set_inheritance;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.AddPluginInterface",
        description="Add Plug-in interface to service.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "interfacename|i|s|Name of interface.",
            "pluginname|g|s|Name of Plug-in.",
            "i18nkey|k|s|Plug-in I18n Key."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "add-plugin-interface-succeed=Plug-in interface, {1} was add to service, {0}."})
    private String add_plugin_interface;

    @SubCommandInfo(
        implClassName="org.forgerock.openam.cli.schema.AddPluginSchema",
        description="Add Plug-in schema to service.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "interfacename|i|s|Name of interface.",
            "pluginname|g|s|Name of Plug-in.",
            "i18nkey|k|s|Plug-in I18n Key.",
            "i18nname|n|s|Plug-in I18n Name.",
            "classname|c|s|Name of the Plugin Schema class implementation"},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "add-plugin-schema-succeed=Plug-in schema, {1} was added to service, {0}.",
            "add-plugin-schema-failed=Plug-in schema, {1} was not added to service, {0}: {2}"})
    private String add_plugin_schema;

    @SubCommandInfo(
        implClassName="org.forgerock.openam.cli.schema.RemovePluginSchema",
        description="Add Plug-in interface to service.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "interfacename|i|s|Name of interface.",
            "pluginname|g|s|Name of Plug-in."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "remove-plugin-schema-succeed=Plug-in schema, {1} was removed from the service, {0}.",
            "remove-plugin-schema-failed=Plug-in schema, {1} was not removed from the service, {0}: {2}"})
    private String remove_plugin_schema;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.SetPluginSchemaPropertiesViewBeanURL",
        description="Set properties view bean URL of plug-in schema.",
        webSupport="true",
        mandatoryOptions={
            "servicename|s|s|Name of service.",
            "interfacename|i|s|Name of interface.",
            "pluginname|g|s|Name of Plug-in.",
            "url|r|s|Properties view bean URL."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "set-properties-viewbean-url-plugin-schema-succeed=Properties View Bean of Plug-in schema, {1} of service, {0} was set."})
    private String set_plugin_viewbean_url;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.CreateIdentity",
        description="Create identity in a realm",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "idname|i|s|Name of identity.",
            "idtype|t|s|Type of Identity such as User, Role and Group."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. sunIdentityServerDeviceStatus=Active.",
            "datafile|D|s|Name of file that contains attribute values data."},
        resourceStrings={
            "create-identity-succeed=Identity, {2} of type {1} was created in realm, {0}.",
            "multi-identity-failed=Multiple identities of name, {2} of type {1} in realm, {0} found.",
            "identity-not-found=Could not find identity of name, {2} of type {1} in realm, {0}.",
            "invalid-identity-type=Invalid identity type.",
            "does-not-support-creation={0} did not support identity creation of type, {1}."})
    private String create_identity;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.DeleteIdentities",
        description="Delete identities in a realm",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "idtype|t|s|Type of Identity such as User, Role and Group."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "idnames|i|m|Names of identites.",
            "file|D|s|Name of file that contains the identity names to be deleted."},
        resourceStrings={
            "missing-identity-names=Identity names need to be provided either with --idnames or --file option.",
            "delete-identity-succeed=The following {1} was deleted from {0}.",
            "delete-identities-succeed=The following {1}s were deleted from {0}."})
    private String delete_identities;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.SearchIdentities",
        description="List identities in a realm",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "filter|x|s|Filter (Pattern).",
            "idtype|t|s|Type of Identity such as User, Role and Group."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "search-identities-succeed=Search of Identities of type {1} in realm, {0} succeeded.",
            "search-identities-no-entries=There were no entries.",
            "format-search-identities-results={0} ({1})"})
    private String list_identities;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.GetAllowedIdOperations",
        description="Show the allowed operations of an identity a realm",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "idtype|t|s|Type of Identity such as User, Role and Group."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "get-allowed-ops-no-ops=No operations were allowed for {1} under {0}.",
            "allowed-ops-result={0}"})
    private String show_identity_ops;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.GetSupportedDataTypes",
        description="Show the supported data type in the system.",
        webSupport="true",
        mandatoryOptions={},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "get-supported-no-supported-datatype=There were no supported data type."})
    private String show_data_types;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.GetSupportedIdTypes",
        description="Show the supported identity type in a realm",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "supported-type-result={0}",
            "no-supported-idtype=There were no supported identity types."})
    private String show_identity_types;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.GetAssignableServices",
        description="List the assignable service to an identity",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "idname|i|s|Name of identity.",
            "idtype|t|s|Type of Identity such as User, Role and Group."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "assignable-service-result={0}",
            "realm-does-not-support-service=realm, {0} did not support services.",
            "no-service-assignable=There were no assignable services."})
    private String list_identity_assignable_svcs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.GetAssignedServices",
        description="Get the service in an identity",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "idname|i|s|Name of identity.",
            "idtype|t|s|Type of Identity such as User, Role and Group."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "assigned-service-result={0}",
            "no-service-assigned=There were no services."})
    private String get_identity_svcs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.GetServiceAttributes",
        description="Show the service attribute values of an identity",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "idname|i|s|Name of identity.",
            "idtype|t|s|Type of Identity such as User, Role and Group.",
            "servicename|s|s|Name of service."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "idrepo-service-attribute-result={0}={1}",
            "idrepo-no-service-attributes=There were no service attribute values."})
    private String show_identity_svc_attrs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.GetAttributes",
        description="Get identity property values",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "idname|i|s|Name of identity.",
            "idtype|t|s|Type of Identity such as User, Role and Group."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributenames|a|m|Attribute name(s). All attribute values shall be returned if the option is not provided."},
        resourceStrings={
            "idrepo-attribute-result={0}={1}",
            "idrepo-no-attributes={0} had no attributes."})
    private String get_identity;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.GetMemberships",
        description="Show the memberships of an identity. For sample show the memberships of an user.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "idname|i|s|Name of identity.",
            "idtype|t|s|Type of Identity such as User, Role and Group.",
            "membershipidtype|m|s|Membership identity type."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "idrepo-memberships-result={0} ({1})",
            "idrepo-no-memberships=Identity {2} did not have any {3} memberships.",
            "idrepo-cannot-be-member={0} could not have {1} membership."})
    private String show_memberships;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.GetMembers",
        description="Show the members of an identity. For example show the members of a role",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "idname|i|s|Name of identity.",
            "idtype|t|s|Type of Identity such as User, Role and Group.",
            "membershipidtype|m|s|Membership identity type."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "idrepo-members-result={0} ({1})",
            "idrepo-no-members={2} did not have any {3} members.",
            "idrepo-cannot-be-member={0} could not have {1} members."})
    private String show_members;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.AddMember",
        description="Add an identity as member of another identity",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "memberidname|m|s|Name of identity that is member.",
            "memberidtype|y|s|Type of Identity of member such as User, Role and Group.",
            "idname|i|s|Name of identity.",
            "idtype|t|s|Type of Identity"},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "idrepo-get-addmember-succeed={0} was added to {1}."})
    private String add_member;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.RemoveMember",
        description="Remove membership of identity from another identity",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "memberidname|m|s|Name of identity that is member.",
            "memberidtype|y|s|Type of Identity of member such as User, Role and Group.",
            "idname|i|s|Name of identity.",
            "idtype|t|s|Type of Identity"},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "idrepo-get-removemember-succeed={0} was removed from {1}."})
    private String remove_member;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.AssignService",
        description="Add Service to an identity",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "idname|i|s|Name of identity.",
            "idtype|t|s|Type of Identity such as User, Role and Group.",
            "servicename|s|s|Name of service."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains attribute values data."},
        resourceStrings={
            "idrepo-assign-service-succeed={3} was added to identity {2} of type, {1} in realm, {0}."})
    private String add_svc_identity;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.UnassignService",
        description="Remove Service from an identity",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "idname|i|s|Name of identity.",
            "idtype|t|s|Type of Identity such as User, Role and Group.",
            "servicename|s|s|Name of service."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "idrepo-unassign-service-succeed={3} was removed from identity {2} of type, {1} in realm, {0}."})
    private String remove_svc_identity;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.ModifyService",
        description="Set service attribute values of an identity",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "idname|i|s|Name of identity.",
            "idtype|t|s|Type of Identity such as User, Role and Group.",
            "servicename|s|s|Name of service."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains attribute values data."},
        resourceStrings={
            "idrepo-modify-service-succeed=Attribute values of service, {3} of identity {2} of type, {1} in realm, {0} was modified."})
    private String set_identity_svc_attrs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.SetAttributeValues",
        description="Set attribute values of an identity",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "idname|i|s|Name of identity.",
            "idtype|t|s|Type of Identity such as User, Role and Group."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains attribute values data."},
        resourceStrings={
            "idrepo-set-attribute-values-succeed=Attribute values of identity, {2} of type, {1} in realm, {0} was modified."})
    private String set_identity_attrs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.GetPrivileges",
        description="Show privileges assigned to an identity",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "idname|i|s|Name of identity.",
            "idtype|t|s|Type of Identity such Role and Group."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "privilege-result={0}",
            "identity-does-not-exist=Identity {0} of type {1} does not exist.",
            "no-privileges=There were no privileges."})
    private String show_privileges;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.AddPrivileges",
        description="Add privileges to an identity. To add a privilege to all authenticated users, use the "
            + "\"All Authenticated Users\" idname with \"role\" idtype.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "idname|i|s|Name of identity.",
            "idtype|t|s|Type of Identity such as Role and Group.",
            "privileges|g|m|Name of privileges to be added."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "idrepo-add-privileges-do-not-exist=Identity {0} of type {1} did not exist.",
            "idrepo-add-privileges-succeed=Privileges were add to identity, {2} of type, {1} in realm, {0}.",
            "delegation-already-has-privilege={0} already had privilege, {1}"})
    private String add_privileges;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.idrepo.RemovePrivileges",
        description="Remove privileges from an identity",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "idname|i|s|Name of identity.",
            "idtype|t|s|Type of Identity such as Role and Group.",
            "privileges|g|m|Name of privileges to be removed."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "idrepo-remove-privileges-succeed=Privileges were removed from identity, {2} of type, {1} in realm, {0}.",
            "delegation-does-not-have-privilege={0} did not have privilege, {1}"})
    private String remove_privileges;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.authentication.ListAuthInstances",
        description="List authentication instances",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "authentication-list-auth-instance=Authentication Instances:",
            "authentication-list-auth-instance-empty=There were no authentication instances.",
            "authentication-list-auth-instance-entry={0}, [type={1}]"}
    )
    private String list_auth_instances;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.authentication.CreateAuthInstance",
        description="Create authentication instance",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "name|m|s|Name of authentication instance.",
            "authtype|t|s|Type of authentication instance e.g. LDAP, DataStore."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "authentication-created-auth-instance-succeeded=Authentication Instance was created."}
    )
    private String create_auth_instance;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.authentication.DeleteAuthInstances",
        description="Delete authentication instances",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "names|m|m|Name of authentication instances."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "authentication-delete-auth-instance-succeeded=Authentication Instance was deleted.",
            "authentication-delete-auth-instances-succeeded=Authentication Instances were deleted."
        }
    )
    private String delete_auth_instances;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.authentication.UpdateAuthInstance",
        description="Update authentication instance values",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "name|m|s|Name of authentication instance."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains attribute values data."},
        resourceStrings={
            "authentication-update-auth-instance-succeeded=Authentication Instance was updated.",
            "authentication-update-auth-instance-not-found=Authentication Instance was not found."}
    )
    private String update_auth_instance;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.authentication.GetAuthInstance",
        description="Get authentication instance values",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "name|m|s|Name of authentication instance."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "authentication-get-auth-instance-succeeded=Authentication Instance profile:",
            "authentication-get-auth-instance-result={0}={1}",
            "authentication-get-auth-instance-no-values=There were no attribute values.",
            "authentication-get-auth-instance-not-found=Authentication Instance was not found."}
    )
    private String get_auth_instance;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.authentication.ListAuthConfigurations",
        description="List authentication configurations",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "authentication-list-auth-configurations-succeeded=Authentication Configurations:",
            "authentication-list-auth-configurations-no-configurations=There were no configurations."
        }
    )
    private String list_auth_cfgs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.authentication.CreateAuthConfiguration",
        description="Create authentication configuration",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "name|m|s|Name of authentication configuration."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "authentication-created-auth-configuration-succeeded=Authentication Configuration was created."}
    )
    private String create_auth_cfg;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.authentication.DeleteAuthConfigurations",
        description="Delete authentication configurations",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "names|m|m|Name of authentication configurations."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "authentication-delete-auth-configuration-succeeded=Authentication Configuration was deleted.",
            "authentication-delete-auth-configurations-succeeded=Authentication Configurations were deleted."
        }
    )
    private String delete_auth_cfgs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.authentication.GetAuthConfigurationEntries",
        description="Get authentication configuration entries",
        webSupport="true",

        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "name|m|s|Name of authentication configuration."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "authentication-get-auth-config-entries-succeeded=Authentication Configuration's entries:",
            "authentication-get-auth-config-entries-entry=[name={0}] [flag={1}] [options={2}]",
            "authentication-get-auth-config-entries-no-values=There were no entries.",
            "authentication-get-auth-config-entries-not-found=Authentication Configuration was not found."}
    )
    private String get_auth_cfg_entr;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.authentication.AddAuthConfigurationEntry",
        description="Add authentication configuration entry",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "name|m|s|Name of authentication configuration.",
            "modulename|o|s|Module Name.",
            "criteria|c|s|Criteria for this entry. Possible values are REQUIRED, OPTIONAL, SUFFICIENT, REQUISITE"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "options|t|s|Options for this entry.",
            "position|p|s|Position where the new entry is to be added. This is option is not set, entry shall be added to the end of the list. If value of this option is 0, it will be inserted to the front of the list. If value is greater of the length of the list, entry shall be added to the end of the list."},
        resourceStrings={
            "authentication-add-auth-config-entry-criteria.invalid=Invalid value for criteria.",
            "authentication-add-auth-config-entry-position.invalid=Invalid value for position. Value must be either 0 or a positive integer.",
            "authentication-add-auth-config-entry-succeeded=Authentication Configuration's entry was created",
            "authentication-add-auth-config-entry-not-found=Authentication Configuration {0} was not found."}
    )
    private String add_auth_cfg_entr;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.authentication.UpdateAuthConfigurationEntries",
        description="Set authentication configuration entries",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "name|m|s|Name of authentication configuration."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "entries|a|m|formatted authentication configuration entries in this format name&pipe;flag&pipe;options. option can be REQUIRED, OPTIONAL, SUFFICIENT, REQUISITE. e.g. myauthmodule&pipe;REQUIRED&pipe;my options.",
            "datafile|D|s|Name of file that contains formatted authentication configuration entries in this format name&pipe;flag&pipe;options. option can be REQUIRED, OPTIONAL, SUFFICIENT, REQUISITE. e.g. myauthmodule&pipe;REQUIRED&pipe;my options."},
        resourceStrings={
            "authentication-set-auth-config-entries-succeeded=Authentication Configuration's entries were updated",
            "authentication-set-auth-config-entries-not-found=Authentication Configuration was not found.",
            "authentication-set-auth-config-entries-instance-not-found=Authentication instance {0} was not found.",
            "authentication-set-auth-config-entries-missing-data=Entries and datafile were missing."}
    )
    private String update_auth_cfg_entr;

    @SubCommandInfo(
        implClassName="org.forgerock.openam.cli.authentication.UpdateAuthConfigProperties",
        description="Set authentication configuration properties",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "name|m|s|Name of authentication configuration."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|authentication configuration properties, valid configuration keys are: iplanet-am-auth-login-failure-url, iplanet-am-auth-login-success-url and iplanet-am-auth-post-login-process-class.",
            "datafile|D|s|Name of file that contains authentication configuration properties."},
        resourceStrings={
            "authentication-set-auth-config-props-succeeded=Authentication Configuration properties were updated",
            "authentication-set-auth-config-props-invalid-key=Invalid configuration property key provided: {0}",
            "authentication-set-auth-config-props-missing-data=Entries and datafile were missing."}
    )
    private String update_auth_cfg_props;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.datastore.ListDataStoreTypes",
        description="List the supported data store types",
        webSupport="true",
        mandatoryOptions={},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "datastore-list-datastore-types-desc=Description",
            "datastore-list-datastore-types-type=Type",
            "datastore-list-datastore-types-succeeded=Supported Datastore Types:",
            "datastore-list-datastore-types-no-entries=There were no supported datastore types."
        }
    )
    private String list_datastore_types;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.datastore.ListDataStores",
        description="List data stores under a realm",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "datastore-list-datastores-succeeded=Datastore:",
            "datastore-list-datastores-no-entries=There were no datastores."
        }
    )
    private String list_datastores;

@SubCommandInfo(
        implClassName="com.sun.identity.cli.datastore.AddAMSDKIdRepoPlugin",
        description="Create AMSDK IdRepo Plug-in",
        webSupport="true",
        mandatoryOptions={
            "directory-servers|s|m|directory servers <protocol>://<hostname>:<port>. Can have multiple entries.",
            "binddn|e|s|Directory Server bind distinguished name.",
            "bind-password-file|m|s|File that contains password of bind password.",
            "basedn|b|s|Directory Server base distinguished name.",
            "dsame-password-file|x|s|File that contains password of the dsameuser",
            "puser-password-file|p|s|File that contains password of the puser"},
        optionAliases={ },
        macro="authentication",
        optionalOptions={
            "user|a|s|User objects naming attribute (defaults to 'uid')",
            "org|o|s|Organization objects naming attribute (defaults to 'o')"},
        resourceStrings={
            "datastore-add-amsdk-idrepo-plugin-succeeded=AMSDK Plugin created successfully.",
            "datastore-add-amsdk-idrepo-plugin-failed=AMSDK Plugin creation failed",
            "datastore-add-amsdk-idrepo-plugin-policies-failed=Adding Delegation policies failed"
        }
    )
    private String add_amsdk_idrepo_plugin;


    @SubCommandInfo(
        implClassName="com.sun.identity.cli.datastore.CreateDataStore",
        description="Create data store under a realm",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "name|m|s|Name of datastore.",
            "datatype|t|s|Type of datastore. Use the list-datastore-types subcommand to get a list of supported datastore types."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. sunIdRepoClass=com.sun.identity.idm.plugins.files.FilesRepo.",
            "datafile|D|s|Name of file that contains attribute values data."},
        resourceStrings={
            "datastore-create-datastore-succeeded=Datastore was created.",
            "datastore-create-datastore-missing-data=Attribute values and datafile were missing."
        }
    )
    private String create_datastore;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.datastore.DeleteDataStores",
        description="Delete data stores under a realm",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "names|m|m|Names of datastore."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "datastore-delete-datastore-not-found=Datastores were not found.",
            "datastore-delete-datastore-succeeded=Datastore was deleted.",
            "datastore-delete-datastores-succeeded=Datastores were deleted."
        }
    )
    private String delete_datastores;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.datastore.UpdateDataStore",
        description="Update data store profile.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "name|m|s|Name of datastore."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. sunIdRepoClass=com.sun.identity.idm.plugins.files.FilesRepo.",
            "datafile|D|s|Name of file that contains attribute values data."},
        resourceStrings={
            "datastore-update-datastore-succeeded=Datastore profile was updated.",
            "datastore-update-datastore-not-found=Datastore was not found.",
            "datastore-update-datastore-missing-data=Attribute values and datafile were missing."
        }
    )
    private String update_datastore;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.datastore.ShowDataStore",
        description="Show data store profile.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "name|m|s|Name of datastore."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "datastore-show-datastore-not-found=Datastore was not found."
        }
    )
    private String show_datastore;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.ExportServiceConfiguration",
        description="Export service configuration.",
        webSupport="false",
        mandatoryOptions={
            "encryptsecret|e|s|Secret key for encrypting password."},
        optionAliases={},
        macro="authentication",
        optionalOptions={"outfile|o|s|Filename where configuration was written."},
        resourceStrings={
            "export-service-configuration-succeeded=Service Configuration was exported."
        }
    )
    private String export_svc_cfg;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.ImportServiceConfiguration",
        description="Import service configuration.",
        webSupport="false",
        mandatoryOptions={
            "encryptsecret|e|s|Secret key for decrypting password.",
            "xmlfile|X|s|XML file that contains configuration data."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "import-service-configuration-unknown-ds=Unable to import service configuration because we are unable to recognize the data store type. We support Sun Directory Server and Embedded OpenDJ as service configuration data store.",
            "import-service-configuration-processing=Please wait while we import the service configuration...",
            "import-service-configuration-succeeded=Service Configuration was imported.",
            "import-service-configuration-invalid-ds-type=Invalid datastore type.",
            "import-service-configuration-invalid-port=Invalid port number.",
            "import-service-configuration-not-connect-to-ds=Unable to connect to directory server.",
            "import-service-configuration-connecting-to-ds=Connecting to directory server.",
            "import-service-configuration-connected-to-ds=Connected to directory server.",
            "import-service-configuration-prompt-delete=Directory Service contains existing data. Do you want to delete it? [y|N]",
            "import-service-configuration-cannot-load-lidf=Could not locate LDIF, {0}.",
            "import-service-configuration-unable-to-locate-hash-secret=Cannot locate hashed encryption key, please make sure that the last line of importing XML file have it.",
            "import-service-configuration-secret-key=The provided encryptsecret is invalid."
        }
    )
    private String import_svc_cfg;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.schema.CreateServerConfigXML",
        description="Create serverconfig.xml file. No options are required for flat file configuration data store.",
        webSupport="false",
        mandatoryOptions={},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "dshost|t|s|Directory Server host name",
            "dsport|p|s|Directory Server port number",
            "basedn|b|s|Directory Server base distinguished name.",
            "dsadmin|a|s|Directory Server administrator distinguished name",
            "dspassword-file|x|s|File that contains Directory Server administrator password",
            "outfile|o|s|File name where serverconfig XML is written."
        },
        resourceStrings={
            "create-serverconfig-xml-succeeded=Server Configuration XML was created."
        }
    )
    private String create_svrcfg_xml;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.GetServerConfigXML",
        description="Get server configuration XML from centralized data store",
        webSupport="true",
        mandatoryOptions={
            "servername|s|s|Server name, e.g. http://www.example.com:8080/fam"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "outfile|o|s|File name where serverconfig XML is written."
        },
        resourceStrings={
             "server-config-port-missing=Port number in server name is required.",
            "server-config-uri-missing=URI in server name is required.",
            "get-server-config-xml-no-result-no-results=Could not locate server configuration XML for this server.",
            "get-serverconfig-xml-succeeded=Server Configuration XML was returned."
        }
    )
    private String get_svrcfg_xml;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.SetServerConfigXML",
        description="Set server configuration XML to centralized data store",
        webSupport="true",
        mandatoryOptions={
            "servername|s|s|Server name, e.g. http://www.example.com:8080/fam",
            "xmlfile|X|m|XML file that contains configuration."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "set-serverconfig-xml-succeeded=Server Configuration XML was set."
        }
    )
    private String set_svrcfg_xml;


    @SubCommandInfo(
        implClassName="com.sun.identity.cli.agentconfig.CreateAgent",
        description="Create a new agent configuration.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "agentname|b|s|Name of agent.",
            "agenttype|t|s|Type of agent. Possible values: J2EEAgent, WebAgent, WSCAgent, WSPAgent, STSAgent, DiscoveryAgent, 2.2_Agent, SharedAgent, OAuth2Client"
            },
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "serverurl|s|s|Server URL. e.g. http://www.example.com:58080/openam. This option is valid for J2EEAgent and"
                + " WebAgent. This parameter is required if the agent is created without datafile/attributes.",
            "agenturl|g|s|Agent URL. e.g. http://www.agent.example:8080/agent. WebAgent does not take URL with path. "
                + "e.g. http://www.agent.example:8080. This option is valid for J2EEAgent and WebAgent. This parameter "
                + "is required if the agent is created without datafile/attributes.",
            "attributevalues|a|m|Properties e.g. sunIdentityServerDeviceKeyValue=https://agent.example.com:443/",
            "datafile|D|s|Name of file that contains properties."},
        resourceStrings={
            "server-url-missing=Server URL is needed.",
            "agent-url-missing=Agent URL is needed.",
            "does-not-support-server-url=Server URL is only supported for J2EEAgent and WebAgent.",
            "does-not-support-agent-url=Agent URL is only supported for J2EEAgent and WebAgent.",
            "server-url-invalid=Server URL is invalid.",
            "agent-url-invalid=Agent URL is invalid.",
            "does-not-support-agent-creation={0} did not support agent creation.",
            "agent-creation-pwd-needed=An agent password is required when you create the agent configuration. Either datafile or attributevalues must include a value for the userpassword attribute.",
            "create-agent-succeeded=Agent configuration was created.",
            "missing-urls=Server URL and Agent URL must be provided when attributes/datafile are not available."
        }
    )
    private String create_agent;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.agentconfig.DeleteAgents",
        description="Delete agent configurations.",
        webSupport="true",
        mandatoryOptions={"realm|e|s|Name of realm."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "agentnames|s|m|Names of agent.",
            "file|D|s|Name of file that contains the agent names to be deleted."},
        resourceStrings={
            "missing-agent-names=Agent names need to be provided either with --agentnames or --file option.",
            "delete-agent-succeeded=The following agents were deleted."
        }
    )
    private String delete_agents;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.agentconfig.UpdateAgent",
        description="Update agent configuration.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "agentname|b|s|Name of agent."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "set|s|u|Set this flag to overwrite properties values.",
            "attributevalues|a|m|Properties e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains properties."},
        resourceStrings={
            "update-agent-does-not-exist=Agent {0} did not exist.",
            "update-agent-succeeded=Agent configuration was updated."
        }
    )
    private String update_agent;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.agentconfig.RemoveAgentProperties",
        description="Remove agent's properties.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "agentname|b|s|Name of agent.",
            "attributenames|a|m|properties name(s)."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "agent-remove-properties-succeeded=Properties were removed."
        }
    )
    private String agent_remove_props;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.agentconfig.ListAgents",
        description="List agent configurations.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "filter|x|s|Filter (Pattern).",
            "agenttype|t|s|Type of agent. e.g. J2EEAgent, WebAgent"},
        resourceStrings={
            "search-agent-no-entries=There were no agents.",
            "format-search-agent-results={0} ({1})"
        }
    )
    private String list_agents;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.agentconfig.ShowAgent",
        description="Show agent profile.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "agentname|b|s|Name of agent."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "outfile|o|s|Filename where configuration is written to.",
            "inherit|i|u|Set this to inherit properties from parent group."},
        resourceStrings={
            "show-agent-agent-does-not-exist=Agent {0} does not exist.",
            "show-agent-to-file=Agent properties were written to file.",
            "show-agent-no-attributes=There were no attribute values."
        }
    )
    private String show_agent;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.agentconfig.ShowAgentTypes",
        description="Show agent types.",
        webSupport="true",
        mandatoryOptions={},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "show-agent-type-no-results=There were no supported agent types."
        }
    )
    private String show_agent_types;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.agentconfig.ShowAgentGroup",
        description="Show agent group profile.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "agentgroupname|b|s|Name of agent group."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "outfile|o|s|Filename where configuration is written to."},
        resourceStrings={
            "show-agent-group-does-not-exist=Agent group {0} does not exist.",
            "show-agent-group-to-file=Agent group properties were written to file.",
            "show-agent-group-no-attributes=There were no attribute values."
        }
    )
    private String show_agent_grp;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.agentconfig.CreateAgentGroup",
        description="Create a new agent group.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "agentgroupname|b|s|Name of agent group.",
            "agenttype|t|s|Type of agent group. e.g. J2EEAgent, WebAgent"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "serverurl|s|s|Server URL. e.g. http://www.example.com:58080/openam. This option is valid for J2EEAgent and WebAgent.",
            "attributevalues|a|m|Properties e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains properties."},
        resourceStrings={
            "does-not-support-agent-group-creation={0} did not support agent group creation.",
            "create-agent-group-succeeded=Agent group was created."
        }
    )
    private String create_agent_grp;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.agentconfig.DeleteAgentGroups",
        description="Delete agent groups.",
        webSupport="true",
        mandatoryOptions={"realm|e|s|Name of realm."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "agentgroupnames|s|m|Names of agent group.",
            "file|D|s|Name of file that contains the agent group names to be deleted."},
        resourceStrings={
            "missing-agent-group-names=Agent group names need to be provided either with --agentgroupnames or --file option.",
            "delete-agent-group-succeeded=The following agent groups were deleted."
        }
    )
    private String delete_agent_grps;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.agentconfig.ListAgentGroups",
        description="List agent groups.",
          webSupport="true",
          mandatoryOptions={
            "realm|e|s|Name of realm."},
          optionAliases={},
          macro="authentication",
        optionalOptions={
            "filter|x|s|Filter (Pattern).",
            "agenttype|t|s|Type of agent. e.g. J2EEAgent, WebAgent"},
        resourceStrings={
            "search-agent-group-no-entries=There were no agent groups.",
            "format-search-agent-group-results={0} ({1})"
        }
    )
    private String list_agent_grps;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.agentconfig.ListAgentGroupMembers",
        description="List agents in agent group.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "agentgroupname|b|s|Name of agent group."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "filter|x|s|Filter (Pattern)."},
        resourceStrings={
            "list-agent-group-member-group-does-not-exist=Agent group {0} did not exist.",
            "list-agent-group-members-no-members=There were no members.",
             "format-list-agent-group-members-results={0} ({1})"
        }
    )
    private String list_agent_grp_members;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.agentconfig.ListAgentMembership",
        description="List agent's membership.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "agentname|b|s|Name of agent."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "list-agent-membership-succeeded=Agent belongs to {0} ({1}).",
            "list-agent-membership-agent-not-found={0} did not exist.",
            "list-agent-membership-no-members=Agent had no memberships."
        }
    )
    private String show_agent_membership;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.agentconfig.AddAgentsToGroup",
        description="Add agents to a agent group.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "agentgroupname|b|s|Name of agent group.",
            "agentnames|s|m|Names of agents."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "add-agent-to-group-succeeded=Agent was added to group.",
            "add-agent-to-group-succeeded-pural=Agents were added to group."
        }
    )
    private String add_agent_to_grp;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.agentconfig.RemoveAgentsFromGroup",
        description="Remove agents from a agent group.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "agentgroupname|b|s|Name of agent group.",
            "agentnames|s|m|Names of agents."},
        optionAliases={},
        macro="authentication",
          optionalOptions={},
          resourceStrings={
            "remove-agent-to-group-succeeded=Agent was removed from group.",
            "remove-agent-to-group-agent-invalid-group={0} did not exist.",
            "remove-agent-to-group-agent-not-member={0} was not a member of {1}.",
            "remove-agent-to-group-succeeded-pural=Agents were removed from group."
          }
      )
    private String remove_agent_from_grp;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.agentconfig.UpdateAgentGroup",
        description="Update agent group configuration.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "agentgroupname|b|s|Name of agent group."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "set|s|u|Set this flag to overwrite properties values.",
            "attributevalues|a|m|Properties e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains properties."},
        resourceStrings={
            "update-agent-group-does-not-exist=Agent group {0} did not exist.",
            "update-agent-group-succeeded=Agent group configuration was updated."
        }
    )
    private String update_agent_grp;


    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.ListServerConfig",
        description="List server configuration.",
        webSupport="true",
        mandatoryOptions={
            "servername|s|s|Server name, e.g. http://www.example.com:8080/fam or enter default to list default server configuration."
        },
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "withdefaults|w|u|Set this flag to get default configuration."
            },
        resourceStrings={
            "list-server-site-name=Site Name: {0}",
            "list-server-id=Server ID: {0}",
            "list-server-config-no-results=There were no configuration."
        }
    )
    private String list_server_cfg;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.UpdateServerConfig",
        description="Update server configuration.",
        webSupport="true",
        mandatoryOptions={
            "servername|s|s|Server name, e.g. http://www.example.com:8080/fam or enter default to update default server configuration."
        },
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains attribute values data."},
        resourceStrings={
            "update-server-config-unknown=Update succeeded with unknown property values.",
            "update-server-config-succeeded=The configuration of {0} was updated.",
            "update-server-config-does-not-exists={0} did not exist."
        }
    )
    private String update_server_cfg;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.RemoveServerConfig",
        description="Remove server configuration.",
        webSupport="true",
        mandatoryOptions={
            "servername|s|s|Server name, e.g. http://www.example.com:8080/fam or enter default to remove default server configuration.",
            "propertynames|a|m|Name of properties to be removed."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "remove-server-config-succeeded=Properties were removed.",
            "remove-server-config-does-not-exists={0} did not exist."
        }
    )
    private String remove_server_cfg;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.CreateServer",
        description="Create a server instance.",
        webSupport="true",
        mandatoryOptions={
            "servername|s|s|Server name, e.g. http://www.example.com:8080/fam",
            "serverconfigxml|X|s|Server Configuration XML file name."},
        optionAliases={},
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. homeaddress=here.",
            "datafile|D|s|Name of file that contains attribute values data."},
        macro="authentication",
        resourceStrings={
            "create-server-config-succeeded=Server was created.",
            "create-server-config-already-exists=Server already existed."
        }
    )
    private String create_server;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.DeleteServer",
        description="Delete a server instance.",
        webSupport="true",
        mandatoryOptions={
            "servername|s|s|Server name, e.g. http://www.example.com:8080/fam"},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "delete-server-config-succeeded=Server was deleted.",
            "delete-server-config-dont-exists=Server did not exist."
        }
    )
    private String delete_server;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.ListServers",
        description="List all server instances.",
        webSupport="true",
        mandatoryOptions={},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "list-servers-no-instances=There were no servers."
        }
    )
    private String list_servers;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.EmbeddedStatus",
        description="Status of embedded store.",
        webSupport="true",
        mandatoryOptions={"port|p|s|Embedded store port"},
        optionAliases={},
        optionalOptions={"password|w|s|Embedded store password"},
        macro="authentication",
        resourceStrings={
            "embedded-status-status=STATUS: {0}"}
    )
    private String embedded_status;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.CreateSite",
        description="Create a site.",
        webSupport="true",
        mandatoryOptions={
            "sitename|s|s|Site name, e.g. mysite",
            "siteurl|i|s|Site's primary URL, e.g. http://www.example.com:8080"},
        optionAliases={},
        optionalOptions={"secondaryurls|a|m|Secondary URLs"},
        macro="authentication",
        resourceStrings={
            "create-site-succeeded=Site was created.",
            "create-site-already-exists=Site already existed."
        }
    )
    private String create_site;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.DeleteSite",
        description="Delete a site.",
        webSupport="true",
        mandatoryOptions={
            "sitename|s|s|Site name, e.g. mysite"},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "delete-site-succeeded=Site was deleted.",
            "delete-site-no-exists=Site did not exist."
        }
    )
    private String delete_site;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.ListSites",
        description="List all sites.",
        webSupport="true",
        mandatoryOptions={},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "list-sites-no-instances=There were no sites."
        }
    )
    private String list_sites;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.ShowSiteMembers",
        description="Display members of a site.",
        webSupport="true",
        mandatoryOptions={"sitename|s|s|Site name, e.g. mysite"},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "show-site-members-no-members=There were no members."
        }
    )
    private String show_site_members;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.AddSiteMembers",
        description="Add members to a site.",
        webSupport="true",
        mandatoryOptions={"sitename|s|s|Site name, e.g. mysite",
            "servernames|e|m|Server names, e.g. http://www.example.com:8080/fam"},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "add-site-members-succeeded=Servers were added to site",
            "add-site-members-site-not-exist=Site did not exist."
        }
    )
    private String add_site_members;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.RemoveSiteMembers",
        description="Remove members from a site.",
        webSupport="true",
        mandatoryOptions={"sitename|s|s|Site name, e.g. mysite",
            "servernames|e|m|Server names, e.g. http://www.example.com:8080/fam"},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "remove-site-members-succeeded=Servers were removed from site",
            "remove-site-members-site-not-exist=Site did not exist."
        }
    )
    private String remove_site_members;

    @SubCommandInfo(
        implClassName="org.forgerock.openam.cli.serverconfig.SetSiteID",
        description="Set the ID of a site.",
        webSupport="true",
        mandatoryOptions={
            "sitename|s|s|Site name, e.g. mysite",
            "siteid|i|s|Site's ID, e.g. 10"},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "set-site-id-succeeded=Site ID was modified.",
            "set-site-id-no-exists=Site did not exist."
        }
    )
    private String set_site_id;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.SetSitePrimaryURL",
        description="Set the primary URL of a site.",
        webSupport="true",
        mandatoryOptions={
            "sitename|s|s|Site name, e.g. mysite",
            "siteurl|i|s|Site's primary URL, e.g. http://site.www.example.com:8080"},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "set-site-primary-url-succeeded=Site primary URL was modified.",
            "set-site-primary-url-no-exists=Site did not exist."
        }
    )
    private String set_site_pri_url;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.ShowSite",
        description="Show site profile.",
        webSupport="true",
        mandatoryOptions={
            "sitename|s|s|Site name, e.g. mysite"},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "show-site-primaryURL=Site primary URL: {0}.",
            "show-site-ID=Site ID: {0}.",
            "show-site-no-secondaryURL=There were no secondary URLs.",
            "show-site-secondaryURL=Site secondary URLs:",
            "show-site-no-exists=Site did not exist."
        }
    )
    private String show_site;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.SetSiteFailoverURLs",
        description="Set Site Secondary URLs.",
        webSupport="true",
        mandatoryOptions={
            "sitename|s|s|Site name, e.g. mysite",
            "secondaryurls|a|m|Secondary URLs"},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "set-site-secondary-urls-succeeded=Site secondary URLs were set.",
            "set-site-secondary-urls-no-exists=Site did not exist."
        }
    )
    private String set_site_sec_urls;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.AddSiteFailoverURLs",
        description="Add Site Secondary URLs.",
        webSupport="true",
        mandatoryOptions={
            "sitename|s|s|Site name, e.g. mysite",
            "secondaryurls|a|m|Secondary URLs"},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "add-site-secondary-urls-succeeded=Site secondary URLs were added.",
            "add-site-secondary-urls-no-exists=Site did not exist."
        }
    )
    private String add_site_sec_urls;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.RemoveSiteFailoverURLs",
        description="Remove Site Secondary URLs.",
        webSupport="true",
        mandatoryOptions={
            "sitename|s|s|Site name, e.g. mysite",
            "secondaryurls|a|m|Secondary URLs"},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "remove-site-secondary-urls-succeeded=Site secondary URLs were removed.",
            "remove-site-secondary-urls-no-exists=Site did not exist."
        }
    )
    private String remove_site_sec_urls;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.CloneServer",
        description="Clone a server instance.",
        webSupport="true",
        mandatoryOptions={
            "servername|s|s|Server name",
            "cloneservername|o|s|Clone server name"},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "clone-server-succeeded=Server was cloned.",
            "clone-server-exists=Clone server already existed.",
            "clone-server-no-exists=Server did not exist."
        }
    )
    private String clone_server;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.ExportServer",
        description="Export a server instance.",
        webSupport="true",
        mandatoryOptions={
            "servername|s|s|Server name"},
        optionAliases={},
        optionalOptions={"outfile|o|s|Filename where configuration was written."},
        macro="authentication",
        resourceStrings={
            "export-server-succeeded=Server was exported.",
            "export-server-no-exists=Server did not exist."
        }
    )
    private String export_server;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.serverconfig.ImportServer",
        description="Import a server instance.",
        webSupport="true",
        mandatoryOptions={
            "servername|s|s|Server name",
            "xmlfile|X|m|XML file that contains configuration."},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "import-server-succeeded=Server was imported.",
            "import-server-already-exists=Server already existed."
        }
    )
    private String import_server;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.realm.GetSupportedAuthModules",
        description="Show the supported authentication modules in the system.",
        webSupport="true",
        mandatoryOptions={},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "get-supported-no-supported-authtype=There were no supported authentication modules."})
    private String show_auth_modules;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.authentication.RegisterAuthModule",
        description="Registers authentication module.",
        webSupport="true",
        mandatoryOptions={
            "authmodule|a|s|Java class name of authentication module."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "register-auth-module-succeeded=Authentication module was registered."
        }
    )
    private String register_auth_module;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.authentication.UnregisterAuthModule",
        description="Unregisters authentication module.",
        webSupport="true",
        mandatoryOptions={
            "authmodule|a|s|Java class name of authentication module."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "unregister-auth-module-succeeded=Authentication module was unregistered.",
            "unregister-auth-module-notfound=Authentication module was not registered."
        }
    )
    private String unregister_auth_module;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.CreateApplication",
        description="Create application.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Realm name",
            "applicationtype|t|s|Application type name",
            "name|m|s|Application name"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. applicationType=iPlanetAMWebAgentService.",
            "datafile|D|s|Name of file that contains attribute values data. Mandatory attributes are resources, subjects, conditions and entitlementCombiner. Optional ones are actions, searchIndexImpl, saveIndexImpl, resourceComparator, subjectAttributeNames."},
        resourceStrings={
            "create-application-succeeded={0} was created."})
    private String create_appl;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.ListApplicationTypes",
        description="List application types.",
        webSupport="true",
        mandatoryOptions={},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "list-applications-type-no-entries=There were no application types."})
    private String list_appl_types;

    @SubCommandInfo(
        implClassName="org.forgerock.openam.cli.entitlement.ShowApplicationType",
        description="Show application type details.",
        webSupport="true",
        mandatoryOptions={
            "name|m|s|Application Type name"},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "show-application-type-not-found=Application Type {0} did not exist."})
    private String show_appl_type;

    @SubCommandInfo(
        implClassName="org.forgerock.openam.cli.entitlement.DeleteApplicationTypes",
        description="Delete application types.",
        webSupport="true",
        mandatoryOptions={
            "names|m|m|Application Type names"},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "delete-application-types-succeeded=Application Types {0} were deleted."})
    private String delete_appl_types;

    @SubCommandInfo(
        implClassName="org.forgerock.openam.cli.entitlement.CreateApplicationType",
        description="Create application type.",
        webSupport="true",
        mandatoryOptions={
            "name|m|s|Application Type name"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Application Type attribute values e.g. actions=enabled=true.",
            "datafile|D|s|Name of file that contains attribute type values data. Mandatory attributes are actions, searchIndexImpl and saveIndexImpl. Optional are resourceComparator."},
        resourceStrings={
            "create-application-type-succeeded=Application Type {0} was created."})
    private String create_appl_type;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.ListApplications",
        description="List applications in a realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Realm name"},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "list-applications-no-entries=There were no applications."})
    private String list_appls;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.ShowApplication",
        description="Show application attributes.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Realm name",
            "name|m|s|Application name"},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "show-application-not-found={0} did not exist."})
    private String show_appl;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.SetApplication",
        description="Set application attributes.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Realm name",
            "name|m|s|Application name"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. applicationType=iPlanetAMWebAgentService.",
            "datafile|D|s|Name of file that contains attribute values data. Possible attributes are resources, subjects, conditions, actions, searchIndexImpl, saveIndexImpl, resourceComparator, subjectAttributeNames and entitlementCombiner."},
        resourceStrings={
            "set-application-not-found={0} did not exist.",
            "set-application-modified={0} was modified."})
    private String set_appl;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.DeleteApplications",
        description="Delete applications.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Realm name",
            "names|m|m|Application names"},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "delete-applications-succeeded=Applications were deleted."})
    private String delete_appls;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.ShowConfigurations",
        description="Display entitlements service configuration",
        webSupport="true",
        mandatoryOptions={},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={"get-attr-values-of-entitlement-service={0}={1}"})
    private String show_entitlement_conf;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.SetConfigurations",
        description="Set entitlements service configuration",
        webSupport="true",
        mandatoryOptions={},
        optionAliases={},
        optionalOptions={
            "attributevalues|a|m|Attribute values e.g. evalThreadSize=4.",
            "datafile|D|s|Name of file that contains attribute values data. Possible attributes are evalThreadSize, searchThreadSize, policyCacheSize and indexCacheSize."},
        macro="authentication",
        resourceStrings={
            "set-entitlement-config-unidentified-attr={0} was unidentified.",
            "set-entitlement-config-succeeded=Entitlements Service configuration is updated."})
    private String set_entitlement_conf;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.CreateApplicationPrivilege",
        description="Add an application privilege to delegate resources of a given application.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Realm name",
            "name|m|s|Name for the this delegation",
            "application|t|s|Application name",
            "actions|a|s|Possible values are READ, MODIFY, DELEGATE, ALL",
            "subjecttype|b|s|Possible values are User or Group",
            "subjects|s|m|Subject name"},
        optionAliases={},
        optionalOptions={
            "description|p|s|Description for the this delegation.",
            "resources|r|m|Resources to delegate, All resources in the applications will be delegated if this option is absent."},
        macro="authentication",
        resourceStrings={
            "privilege-application-action-invalid={0} was invalid. Supported ones are READ, MODIFY, DELEGATE, ALL.",
            "privilege-application-subject-type-invalid={0} was invalid. Supported ones are User and Group",
            "privilege-application-application-invalid={0} was invalid. Either the application did not exist or you did not have permissions to delegate it.",
            "create-application-privilege-succeeded={0} was added."})
    private String add_app_priv;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.DeleteApplicationPrivilege",
        description="Remove an application privileges.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Realm name",
            "names|m|m|Names of application privilege to be removed"},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "delete-application-privilege-succeeded=Privilege was removed",
            "delete-application-privileges-succeeded=Privileges were removed"})
    private String remove_app_privs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.ShowApplicationPrivilege",
        description="Show application privilege.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Realm name",
            "name|m|s|Name of application privilege"},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "show-application-privilege-output-name=Privilege name: {0}",
            "show-application-privilege-output-description=Description: {0}",
            "show-application-privilege-output-actions=Actions: {0}",
            "show-application-privilege-output-subjects=Subject: {0} ({1})",
            "show-application-privilege-output-resources=Resource: {0} ({1})"})
    private String show_app_priv;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.ListApplicationPrivileges",
        description="List application privileges in a realm.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Realm name"},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "list-application-privileges-no-privileges=There were no privileges."})
    private String list_app_privs;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.UpdateApplicationPrivilege",
        description="Update an application privilege.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Realm name",
            "name|m|s|Name for the this delegation"},
        optionAliases={},
        optionalOptions={
            "actions|a|s|Possible values are READ, MODIFY, DELEGATE, ALL",
            "description|p|s|Description for the this delegation."},
        macro="authentication",
        resourceStrings={
            "update-application-privilege-invalid=description or actions option is needed..",
            "update-application-privilege-succeeded={0} was updated."})
    private String update_app_priv;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.SetApplicationPrivilegeSubjects",
        description="Set application privilege subjects.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Realm name",
            "name|m|s|Name for the this delegation",
            "subjecttype|b|s|Possible values are User or Group",
            "subjects|s|m|Subject name"},
        optionAliases={},
        optionalOptions={
            "add|p|u|Subjects are added to this application if this option is set. Otherwise, subjects in the current application privilege will be overwritten."},
        macro="authentication",
        resourceStrings={})
    private String update_app_priv_subjects;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.SetApplicationPrivilegeResources",
        description="Set application privilege resources.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Realm name",
            "name|m|s|Name for the this delegation",
            "application|t|s|Application name"},
        optionAliases={},
        optionalOptions={
            "add|p|u|Resources are added to this application if this option is set. Otherwise, resources in the current application privilege will be overwritten.",
            "resources|r|m|Resources to delegate, All resources in the applications will be delegated if this option is absent."},
        macro="authentication",
        resourceStrings={})
    private String update_app_priv_resources;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.RemoveApplicationPrivilegeSubjects",
        description="Remove application privilege subjects.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Realm name",
            "name|m|s|Name for the this delegation",
            "subjecttype|b|s|Possible values are User or Group",
            "subjects|s|m|Subject name"},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={
            "remove-application-privilege-subjects-emptied-subjects=Unable to processed this request because you have removed all the subjects in the application privilege."})
    private String remove_app_priv_subjects;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.RemoveApplicationPrivilegeResources",
        description="Remove application privilege resources.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Realm name",
            "name|m|s|Name for the this delegation",
            "application|t|s|Application name"},
        optionAliases={},
        optionalOptions={
            "resources|r|m|Resources to removed, All resources in the applications will be removed if this option is absent."},
        macro="authentication",
        resourceStrings={
            "remove-application-privilege-resources-emptied-resources=Unable to processed this request because you have removed all the resources in the application privilege."})
    private String remove_app_priv_resources;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.ListXACML",
        description="export policies in realm as XACML.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "policynames|p|m|Names of policy. This can be a wildcard. All policy definition in the realm will be returned if this option is not provided.",
            "outfile|o|s|Filename where policy definition will be printed to. Definition will be printed in standard output if this option is not provided.",
            "namesonly|n|u|Returns only names of matching policies. Policies are not returned." },
        resourceStrings={
            "list-xacml-not-supported-in-legacy-policy-mode=list-xacml not supported in legacy policy mode",
            "get-policy-names-in-realm-succeed=Policy names were returned under realm, {0}.",
            "get-policy-names-in-realm-no-policies=There were not matching policy names under realm, {0}.",
            "get-policy-in-realm-succeed=Policy definitions were returned under realm, {0}.",
            "get-policy-in-realm-no-policies=There were not matching policies under realm, {0}."})
    private String list_xacml;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.CreateXACML",
        description="Create policies in a realm with XACML input.",
        webSupport="true",
        mandatoryOptions={
            "realm|e|s|Name of realm.",
            "xmlfile|X|s|Name of file that contains policy XACML definition."},
        optionAliases={},
        macro="authentication",
        optionalOptions={},
        resourceStrings={
            "create-xacml-not-supported-in-legacy-policy-mode=add-xacml not supported in legacy policy mode",
            "subcmd-create-policies-__web__-xmlfile=Policy XML",
            "create-policy-in-realm-succeed=Policies were created under realm, {0}."})
    private String create_xacml;

    @SubCommandInfo(
        implClassName="com.sun.identity.cli.entitlement.DeleteXACML",
        description="Delete XACML policies from a realm.",
        webSupport="true",
        mandatoryOptions={"realm|e|s|Name of realm."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "policynames|p|m|Names of policy to be deleted.",
            "file|D|s|Name of file that contains the policy names to be deleted."},
        resourceStrings={
            "delete-xacml-not-supported-in-legacy-policy-mode=delete-xacml not supported in legacy policy mode",
            "missing-policy-names=Policy names need to be provided either with --policynames or --file option",
            "delete-policy-in-realm-succeed=Policies were deleted under realm, {0}."})
    private String delete_xacml;

}

