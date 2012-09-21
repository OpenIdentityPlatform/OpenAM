/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Entitlement.java,v 1.2 2009/06/22 19:09:03 veiming Exp $
 *
 */

package com.sun.identity.entitlement.opensso.cli.definition;


import com.sun.identity.cli.annotation.DefinitionClassInfo;
import com.sun.identity.cli.annotation.Macro;
import com.sun.identity.cli.annotation.SubCommandInfo;
import com.sun.identity.cli.annotation.ResourceStrings;

public class Entitlement {
    @DefinitionClassInfo(
        productName="OpenSSO",
        logName="ssoadm",
        resourceBundle="EntitlementCLI")
    private String product;

    @ResourceStrings(string = {
        "missing-attributevalues=attributevalues and datafile options are missing.",
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
    })
    private String resourcestrings;

    @Macro(
        mandatoryOptions={
            "adminid|u|s|Administrator ID of running the command.",
            "password-file|f|s|File name that contains password of administrator."},
        optionalOptions={},
        optionAliases={})
    private String authentication;

    @SubCommandInfo(
        implClassName="com.sun.identity.entitlement.opensso.cli.CreateApplication",
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
        implClassName="com.sun.identity.entitlement.opensso.cli.ListApplicationTypes",
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
        implClassName="com.sun.identity.entitlement.opensso.cli.ListApplications",
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
        implClassName="com.sun.identity.entitlement.opensso.cli.ShowApplication",
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
        implClassName="com.sun.identity.entitlement.opensso.cli.SetApplication",
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
        implClassName="com.sun.identity.entitlement.opensso.cli.DeleteApplications",
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
        implClassName="com.sun.identity.entitlement.opensso.cli.ShowConfigurations",
        description="Display entitlements service configuration",
        webSupport="true",
        mandatoryOptions={},
        optionAliases={},
        optionalOptions={},
        macro="authentication",
        resourceStrings={"get-attr-values-of-entitlement-service={0}={1}"})
    private String show_entitlement_conf;

    @SubCommandInfo(
        implClassName="com.sun.identity.entitlement.opensso.cli.SetConfigurations",
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
}

