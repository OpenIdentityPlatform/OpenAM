/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FederationManager.java,v 1.40 2009/10/29 00:03:51 exu Exp $
 *
 */

/*
 * Portions copyright 2013 ForgeRock, Inc.
 */
package com.sun.identity.federation.cli.definition;

import com.sun.identity.cli.annotation.DefinitionClassInfo;
import com.sun.identity.cli.annotation.Macro;
import com.sun.identity.cli.annotation.SubCommandInfo;
import com.sun.identity.cli.annotation.ResourceStrings;

public class FederationManager {
    @DefinitionClassInfo(
        productName="OpenAM",
        logName="ssoadm",
        resourceBundle="FederationManagerCLI")
    private String product;

    @ResourceStrings(
        string="cannot-write-to-file=Unable to write to file, {0}\nfile-not-found=File not found, {0}\nunsupported-specification=Unsupported specification."
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
        implClassName="com.sun.identity.federation.cli.CreateMetaDataTemplate",
        description="Create new metadata template.",
        webSupport="true",
        mandatoryOptions={
            "entityid|y|s|Entity ID"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "meta-data-file|m|s|c|Specify file name for the standard metadata to be created. XML will be displayed on terminal if this file name is not provided.",
            "extended-data-file|x|s|c|Specify file name for the extended metadata to be created. XML will be displayed on terminal if this file name is not provided.",
            "serviceprovider|s|s|Specify metaAlias for hosted service provider to be created. The format must be <realm name>/<identifier>.",
            "identityprovider|i|s|Specify metaAlias for hosted identity provider to be created. The format must be <realm name>/<identifier>.",
            "attrqueryprovider|S|s|i|Specify metaAlias for hosted attribute query provider to be created. The format must be <realm name>/<identifier>.",
            "attrauthority|I|s|i|Specify metaAlias for hosted attribute authority to be created. The format must be <realm name>/<identifier>.",
            "authnauthority|C|s|i|Specify metaAlias for hosted authentication authority to be created. The format must be <realm name>/<identifier>.",
            "xacmlpep|e|s|Specify metaAlias for policy enforcement point to be created. The format must be <realm name>/<identifier>.",
            "xacmlpdp|p|s|Specify metaAlias for policy decision point to be created. The format must be <realm name>/<identifier>.",
            "affiliation|F|s|i|Specify metaAlias for hosted affiliation. to be created. The format must be <realm name>/<identifier>",
            "affiownerid|N|s|i|Affiliation Owner ID", 
            "affimembers|M|m|Affiliation members",
            "spscertalias|a|s|Service provider signing certificate alias",
            "idpscertalias|b|s|Identity provider signing certificate alias",
            "attrqscertalias|A|s|i|Attribute query provider signing certificate alias",
            "attrascertalias|B|s|i|Attribute authority signing certificate alias",
            "authnascertalias|D|s|i|Authentication authority signing certificate alias",
            "affiscertalias|J|s|i|Affiliation signing certificate alias",
            "xacmlpdpscertalias|t|s|Policy decision point signing certificate alias",
            "xacmlpepscertalias|k|s|Policy enforcement point signing certificate alias",
            "specertalias|r|s|Service provider encryption certificate alias",
            "idpecertalias|g|s|Identity provider encryption certificate alias.",
            "attrqecertalias|R|s|i|Attribute query provider encryption certificate alias",
            "attraecertalias|G|s|i|Attribute authority encryption certificate alias.",
            "authnaecertalias|E|s|i|Authentication authority encryption certificate alias.",
            "affiecertalias|K|s|i|Affiliation encryption certificate alias",
            "xacmlpdpecertalias|j|s|Policy decision point encryption certificate alias",
            "xacmlpepecertalias|z|s|Policy enforcement point encryption certificate alias",
            "spec|c|s|Specify metadata specification, either wsfed, idff or saml2, defaults to saml2"},
        resourceStrings={
            "create-meta-template-exception-role-null=Identity or Service Provider or Policy Enforcement Point or Policy Decision Point or Attribute Query Provider or Attribute Authority or Authentication Authority or Affiliation are required.",
            "create-meta-template-exception-affi-conflict=Affiliation and other providers can't coexist.",
            "create-meta-template-exception-affi-ownerid-empty=Affiliation Owner ID is required.", 
            "create-meta-template-exception-affi-members-empty=Affiliation members is required.",
            "create-meta-template-exception-affi-null-with-cert-alias=Affiliation Certificate Alias was provided without Affiliation Name.",
            "create-meta-template-exception-idp-null-with-cert-alias=Identity Provider Certificate Alias was provided without Identity Provider Name.",
            "create-meta-template-exception-sp-null-with-cert-alias=Service Provider Certificate Alias was provided without Service Provider Name.",
            "create-meta-template-exception-attra-null-with-cert-alias=Attribute Authority Certificate Alias was provided without Attribute Authority Name.",
            "create-meta-template-exception-attrq-null-with-cert-alias=Attribute Query Provider Certificate Alias was provided without Attribute Query Provider Name.",
            "create-meta-template-exception-authna-null-with-cert-alias=Authentication Authority Certificate Alias was provided without Authentication Authority Name.",
            "create-meta-template-exception-pdp-null-with-cert-alias=Policy Decision Point Certificate Alias was provided without Policy Decision Point Name",
            "create-meta-template-exception-pep-null-with-cert-alias=Policy Enforcement Point Certificate Alias was provided without Policy Enforcement Point Name",
            "create-meta-template-exception-protocol-not-found=Protocol was not found in configuration file.",
            "create-meta-template-exception-host-not-found=Host was not found in configuration file.",
            "create-meta-template-exception-port-not-found=Port was not found in configuration file.",
            "create-meta-template-exception-deploymentURI-not-found=Deployment URI was not found in configuration file.",
            "create-meta-template-created-descriptor-template=Hosted entity descriptor was written to {0}.",
            "create-meta-template-created-configuration-template=Hosted entity configuration was written to {0}."})
    private String create_metadata_templ;

    @SubCommandInfo(
        implClassName="com.sun.identity.federation.cli.UpdateMetadataKeyInfo",
        description="Update XML signing and encryption key information in hosted entity metadata.",
        webSupport="true",
        mandatoryOptions={
            "entityid|y|s|Entity ID"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "realm|e|s|Realm where entity resides.",
            "spscertalias|a|s|Service provider signing certificate alias",
            "idpscertalias|b|s|Identity provider signing certificate alias",
            "specertalias|r|s|Service provider encryption certificate alias",
            "idpecertalias|g|s|Identity provider encryption certificate alias.",
            "spec|c|s|Specify metadata specification, either wsfed, idff or saml2, defaults to saml2"},
        resourceStrings={
            "update-keyinfo-succeeded=Update entity keyinfo succeeded : {0}",
            "update-meta-keyinfo-exception-alias-null=Singing or encryption certificate alias for Identity or Service Provider is required.",
            "update-meta-keyinfo-exception-entity-not-exist=Entity {0} does not exist under realm {1}.",
            "update-meta-keyinfo-exception-invalid-option=Encryption cert alias option not supported for WS-Federation protocol."})
    private String update_entity_keyinfo;

    @SubCommandInfo(
        implClassName="com.sun.identity.federation.cli.ImportMetaData",
        description="Import entity.",
        webSupport="true",
        mandatoryOptions={},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "realm|e|s|Realm where entity resides.",
            "meta-data-file|m|s|t|Specify file name for the standard metadata to be imported.<web>Standard metadata to be imported.",
            "extended-data-file|x|s|t|Specify file name for the extended entity configuration to be imported.<web>Extended entity configuration to be imported.",
            "cot|t|s|Specify name of the Circle of Trust this entity belongs.",
            "spec|c|s|Specify metadata specification, either wsfed, idff or saml2, defaults to saml2"},
        resourceStrings={
            "import-entity-exception-cot-no-exist=Circle of Trust did not exist.",
            "import-entity-exception-no-datafile=metadata or extended data file is required.",
            "import-entity-exception-invalid-descriptor-file=Entity descriptor in file, {0} had invalid syntax.",
            "import-entity-succeeded=Import file, {0}.",
            "import-entity-exception-invalid-config-file=Entity config in file, {0} had invalid syntax."})
    private String import_entity;
    
    @SubCommandInfo(
        implClassName="com.sun.identity.federation.cli.ExportMetaData",
        description="Export entity.",
        webSupport="true",
        mandatoryOptions={
            "entityid|y|s|Entity ID"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "realm|e|s|Realm where data resides",
            "sign|g|u|Set this flag to sign the metadata",
            "meta-data-file|m|s|c|Metadata",
            "extended-data-file|x|s|c|Extended data",
            "spec|c|s|Specify metadata specification, either wsfed, idff or saml2, defaults to saml2"},
        resourceStrings={
            "export-entity-exception-no-datafile=Missing export files, metadata or extended option needs to be set.",
            "export-entity-exception-entity-descriptor-not-exist=Entity descriptor, {0} under realm, {1} did not exist.",
            "export-entity-exception-invalid_descriptor=Entity descriptor, {0} under realm, {1} had invalid syntax.",
            "export-entity-export-descriptor-succeeded=Entity descriptor was exported to file, {0}.",
            "export-entity-export-config-succeeded=Entity configuration was exported to file, {0}.",
            "export-entity-exception-entity-config-not-exist=Entity configuration, {0} did not exist under realm, {1}",
            "export-entity-exception-invalid-config=Entity configuration, {0} under realm, {1} had invalid syntax."})
    private String export_entity;

    @SubCommandInfo(
        implClassName="com.sun.identity.federation.cli.DeleteMetaData",
        description="Delete entity.",
        webSupport="true",
        mandatoryOptions={
            "entityid|y|s|Entity ID"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "realm|e|s|Realm where data resides",
            "extendedonly|x|u|Set to flag to delete only extended data.",
            "spec|c|s|Specify metadata specification, either wsfed, idff or saml2, defaults to saml2"},
        resourceStrings={
            "delete-entity-entity-not-exist=Entity, {0} did not exist.",
            "delete-entity-config-deleted=Configuration was deleted for entity, {0}.",
            "delete-entity-descriptor-deleted=Descriptor was deleted for entity, {0}."})
    private String delete_entity;

    @SubCommandInfo(
        implClassName="com.sun.identity.federation.cli.ListEntities",
        description="List entities under a realm.",
        webSupport="true",
        mandatoryOptions={},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "realm|e|s|Realm where entities reside.",
            "spec|c|s|Specify metadata specification, either wsfed, idff or saml2, defaults to saml2"},
        resourceStrings={
            "list-entities-no-entities=There are no entities.",
            "list-entities-entity-listing=List of entity IDs:"})
    private String list_entities;

    @SubCommandInfo(
        implClassName="com.sun.identity.federation.cli.CreateCircleOfTrust",
        description="Create circle of trust.",
        webSupport="true",
        mandatoryOptions={
            "cot|t|s|Circle of Trust"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "realm|e|s|Realm where circle of trust resides",
            "trustedproviders|k|m|Trusted Providers",
            "prefix|p|s|Prefix URL for idp discovery reader and writer URL."},
        resourceStrings={
            "create-circle-of-trust-succeeded=Circle of trust, {0} was created."})
    private String create_cot;

    @SubCommandInfo(
        implClassName="com.sun.identity.federation.cli.DeleteCircleOfTrust",
        description="Delete circle of trust.",
        webSupport="true",
        mandatoryOptions={
            "cot|t|s|Circle of Trust"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "realm|e|s|Realm where circle of trust resides"},
        resourceStrings={
            "delete-circle-of-trust-succeeded=Circle of trust, {0} was deleted."})
    private String delete_cot;

    @SubCommandInfo(
        implClassName="com.sun.identity.federation.cli.ListCircleOfTrusts",
        description="List circles of trust.",
        webSupport="true",
        mandatoryOptions={},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "realm|e|s|Realm where circle of trusts reside"},
        resourceStrings={
            "list-circles-of-trust-no-members=There are no circles of trust.",
            "list-circles-of-trust-members=Followings are the circles of trust."})
    private String list_cots;

    @SubCommandInfo(
        implClassName="com.sun.identity.federation.cli.ListCircleOfTrustMembers",
        description="List the members in a circle of trust.",
        webSupport="true",
        mandatoryOptions={
            "cot|t|s|Circle of Trust"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "realm|e|s|Realm where circle of trust resides",
            "spec|c|s|Specify metadata specification, either wsfed, idff or saml2, defaults to saml2"},
        resourceStrings={
            "list-circle-of-trust-members-no-members=There are no trusted entities in the circle of trust, {0}.",
            "list-circle-of-trust-members-cot-does-not-exists=Circle of trust, {0} did not exist.",
            "list-circle-of-trust-members-members=List of trusted entities (entity IDs) in the circle of trust, {0}:"})
    private String list_cot_members;

    @SubCommandInfo(
        implClassName="com.sun.identity.federation.cli.RemoveCircleOfTrustMembers",
        description="Remove a member from a circle of trust.",
        webSupport="true",
        mandatoryOptions={
            "cot|t|s|Circle of Trust",
            "entityid|y|s|Entity ID"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "realm|e|s|Realm where circle of trust resides",
            "spec|c|s|Specify metadata specification, either wsfed, idff or saml2, defaults to saml2"},
        resourceStrings={
            "remove-circle-of-trust-member-succeeded=Entity, {1} was removed from the circle of trust, {0}."})
    private String remove_cot_member;

    @SubCommandInfo(
        implClassName="com.sun.identity.federation.cli.AddCircleOfTrustMembers",
        description="Add a member to a circle of trust.",
        webSupport="true",
        mandatoryOptions={
            "cot|t|s|Circle of Trust",
            "entityid|y|s|Entity ID"},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "realm|e|s|Realm where circle of trust resides",
            "spec|c|s|Specify metadata specification, either wsfed, idff or saml2, defaults to saml2"},
        resourceStrings={
            "add-circle-of-trust-member-succeeded=Entity, {2} was added to the circle of trust, {1}, in realm {3}."})
    private String add_cot_member;

    @SubCommandInfo(
        implClassName="com.sun.identity.federation.cli.BulkFederation",
        description="Perform bulk federation.",
        webSupport="false",
        mandatoryOptions={
            "metaalias|m|s|Specify metaAlias for local provider.",
            "remoteentityid|r|s|Remote entity Id",
            "useridmapping|g|s|File name of local to remote user Id mapping. Format <local-user-id>&pipe;<remote-user-id>",
            "nameidmapping|e|s|Name of file that will be created by this sub command. It contains remote user Id to name identifier. It shall be used by remote provider to update user profile."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "spec|c|s|Specify metadata specification, either idff or saml2, defaults to saml2"},
        resourceStrings={
            "bulk-federation-succeeded=Bulk Federation for this host was completed. To complete the federation, name Id mapping file should be loaded to remote provider.",
            "bulk-federation-infile-do-not-exists=User Id Mapping file, {0} did not exist.",
            "bulk-federation-outfile-exists=Name Id mapping file, {0} already exists.",
            "bulk-federation-outfile-cannot-write=Name Id mapping file, {0} was not writable.",
            "bulk-federation-wrong-format=Wrong format, {0} in User Id Mapping file, {1}.",
            "bulk-federation-cannot-generate-name-id=Could not generate name identifier.",
            "bulk-federation-unknown-metaalias=Meta Alias, {0} was unknown.",
            "bulk-federation-cannot-federate=Could not federate user, {0}"
            })
    private String do_bulk_federation;

    @SubCommandInfo(
        implClassName="com.sun.identity.federation.cli.ImportBulkFederationData",
        description="Import bulk federation data which is generated by 'do-bulk-federation' sub command.",
        webSupport="false",
        mandatoryOptions={
            "metaalias|m|s|Specify metaAlias for local provider.",
            "bulk-data-file|g|s|File name of  bulk federation data which is generated by 'do-bulk-federation' sub command."},
        optionAliases={},
        macro="authentication",
        optionalOptions={
            "spec|c|s|Specify metadata specification, either idff or saml2, defaults to saml2"},
        resourceStrings={
            "import-bulk-federation-data-succeeded=Bulk Federation for this host was completed.",
            "import-bulk-federation-data-unknown-metaalias=Meta Alias, {0} was unknown.",
            "import-bulk-federation-data-incorrect-entity-id=Entity Id in data file did not match with the entity Id of given meta alias.",
            "import-bulk-federation-data-incorrect-file-format=Incorrect file format.",
            "import-bulk-federation-data-incorrect-data-format=Incorrect data format, {0}.",
            "import-bulk-federation-data-incorrect-role=Incorrect role. The role in data file differs from the role of provider metaalias.",
            "import-bulk-federation-data-incorrect-spec=Incorrect specification. The specification in data file differs from the entered specification",
            "import-bulk-federation-data-cannot-federate=Could not federate user, {0}"
            })
    private String import_bulk_fed_data;
}
