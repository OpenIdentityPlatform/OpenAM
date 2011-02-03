/* The contents of this file are subject to the terms
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
 * $Id: FederationManagerCLIConstants.java,v 1.18 2009/07/28 02:49:28 srivenigan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.cli;

/**
 * <code>FederationManagerCLIConstants</code> contains strings for the required 
 * and optional arguments used in the ssoadm CLI.
 */
public interface FederationManagerCLIConstants {
    
    /**
     * Administrator ID argument/option  
     */
    String ARGUMENT_ADMIN_ID = "adminid";
    
    /**
     * Administrator ID short argument/option
     */
    String SHORT_ARGUMENT_ADMIN_ID = "u";
    
    /**
     * Password file argument/option
     */
    String ARGUMENT_PASSWORD_FILE = "password-file";
    
    /**
     * Password file short argument/option
     */
    String SHORT_ARGUMENT_PASSWORD_FILE = "f";
    
    /**
     * Realm argument/option
     */
    String REALM_ARGUMENT = "realm";
    
    /**
     * Realm short argument/option
     */
    String SHORT_REALM_ARGUMENT = "e";
    
    /**
     * Recursive argument/option
     */
    String RECURSIVE_ARGUMENT = "recursive";
    
    /**
     * Short recursive argument/option
     */
    String SHORT_RECURSIVE_ARGUMENT = "r";
    
    /**
     * Filter argument/option
     */
    String FILTER_ARGUMENT = "filter";
    
    /**
     * Short filter argument/option
     */
    String SHORT_FILTER_ARGUMENT = "x";
    
    /** 
     * Attribute names argument/option
     */
    String ATTRIBUTE_NAMES_ARGUMENT = "attributenames";
    
    /**
     * Short attribute names argument/option
     */
    String SHORT_ATTRIBUTE_NAMES_ARGUMENT = "a";
    
    /**
     * Idtype argument/option
     */
    String ID_TYPE_ARGUMENT = "idtype";
    
    /**
     * Short idtype argument/option
     */
    String SHORT_ID_TYPE_ARGUMENT = "t";
    
    /**
     * Idname argument/option
     */
    String ID_NAME_ARGUMENT = "idname";
    
    /**
     * Short idname argument/option
     */
    String SHORT_ID_NAME_ARGUMENT = "i";
    
    /**
     * Attributevalues argument/option
     */
    String ATTRIBUTE_VALUES_ARGUMENT = "attributevalues";
    
    /**
     * Short attributevalues argument/option
     */
    String SHORT_ATTRIBUTE_VALUES_ARGUMENT = "a";
    
    /**
     * Datafile argument/option
     */
    String DATA_FILE_ARGUMENT = "datafile";
    
    /**
     * Short datafile argument/option
     */
    String SHORT_DATA_FILE_ARGUMENT = "D";
    
    /**
     * Idnames argument/option
     */
    String ID_NAMES_ARGUMENT = "idnames";
    
    /**
     * Memberidname argument/option
     */
    String MEMBER_ID_NAME_ARGUMENT = "memberidname";
    
    /**
     * Short memberidname argument/option
     */
    String SHORT_MEMBER_ID_NAME_ARGUMENT = "m";
    
    /**
     * Memberidtype argument/option
     */
    String MEMBER_ID_TYPE_ARGUMENT = "memberidtype";
    
    /**
     * Short memberidtype argument/option
     */
    String SHORT_MEMBER_ID_TYPE_ARGUMENT = "y";
    
    /**
     * Membershipidtype argument/option
     */
    String MEMBERSHIP_ID_TYPE_ARGUMENT = "membershipidtype";
    
    /**
     * Short membershipidtype argument/option
     */
    String SHORT_MEMBERSHIP_ID_TYPE_ARGUMENT = "m";
    
    /**
     * Servicename argument/option
     */
    String SERVICENAME_ARGUMENT = "servicename";
    
    /**
     * Short servicename argument/option
     */
    String SHORT_SERVICENAME_ARGUMENT = "s";
    
    /**
     * Attributename argument/option
     */
    String ATTRIBUTE_NAME_ARGUMENT = "attributename"; 
    
    /**
     * Authtype argument/option
     */
    String AUTHTYPE_ARGUMENT = "authtype";
    
    /**
     * Short authtype argument/option
     */
    String SHORT_AUTHTYPE_ARGUMENT = "t";
    
    /**
     * Name argument/option
     */
    String NAME_ARGUMENT = "name";
    
    /**
     * Names argument/option
     */
    String NAMES_ARGUMENT = "names";
    
    /**
     * Short names argument/option
     */
    String SHORT_NAMES_ARGUMENT = "m";
    
    /**
     * Append argument/option
     */
    String APPEND_ARGUMENT = "append";
    
    /**
     * Short append argument/option
     */
    String SHORT_APPEND_ARGUMENT = "p";
    
    /**
     * Revision number argument/option
     */
    String REVISION_NO_ARGUMENT = "revisionnumber";

    /**
     * Short revision number argument/option
     */
    String SHORT_REVISION_NO_ARGUMENT = "r";
    
    /**
     * Xml File argument/option
     */
    String XML_FILE_ARGUMENT = "xmlfile";
    
    /**
     * Short xml file argument/option
     */
    String SHORT_XML_FILE_ARGUMENT = "X";
    
    /**
     * agenttype argument/option
     */
    String AGENTTYPE_ARGUMENT = "agenttype";
    
    /**
     * Short agenttype argument/option
     */
    String SHORT_AGENTTYPE_ARGUMENT = "t";

    /**
     * agenttype argument/option
     */
    String AGENTNAME_ARGUMENT = "agentname";
    
    /**
     * Short agenttype argument/option
     */
    String SHORT_AGENTNAME_ARGUMENT = "b";
    
    /**
     * agenttype argument/option
     */
    String AGENTNAMES_ARGUMENT = "agentnames";
    
    /**
     * Short agenttype argument/option
     */
    String SHORT_AGENTNAMES_ARGUMENT = "s";

    /**
     * Continue argument/option
     */
    String CONTINUE_ARGUMENT = "continue";
    
    /**
     * Short continue argument/option
     */
    String SHORT_CONTINUE_ARGUMENT = "c";
    
    /**
     * Delete policy rule argument/option
     */
    String DELETE_POLICY_RULE = "deletepolicyrule";
    
    /**
     * Short delete policy rule argument/option
     */
    String SHORT_DELETE_POLICY_RULE = "r";
    
    /**
     * Schema type argument/option
     */
    String SCHEMA_TYPE_ARGUMENT = "schematype";
    
    /**
     * Short schema type argument/option
     */
    String SHORT_SCHEMA_TYPE_ARGUMENT = "t";
    
    /**
     * Sub schema name argument/option
     */
    String SUB_SCHEMA_NAME_ARGUMENT = "subschemaname";
    
    /**
     * Short sub schema name argument/option
     */
    String SHORT_SUB_SCHEMA_NAME_ARGUMENT = "c";
    
    /**
     * Datastore name argument/option 
     */
    String DATASTORE_NAME_ARG = "name";
    
    /**
     * Datastore name(s) argument/option
     */
    String DATASTORE_NAMES_ARG = "names";
    
    /**
     * Short datastore name argument/option
     */
    String SHORT_DATASTORE_NAME_ARG = "m";
    
    /**
     * Datastore type argument/option
     */
    String DATASTORE_TYPE_ARG = "datatype";
    
    /**
     * Short datastore type argument/option
     */
    String SHORT_DATASTORE_TYPE_ARG = "t";
    
    /**
     * Circle of trust argument/option
     */
    String COT_ARGUMENT = "--cot";
    
    /**
     * Short circle of trust argument/option
     */
    String SHORT_COT_ARGUMENT = "-t";
    
    /**
     * Trusted providers argument/option
     */
    String TRUSTEDPROVIDERS_ARGUMENT = "--trustedproviders";
    
    /**
     * Short trusted provider argument/option
     */
    String SHORT_TRUSTEDPROVIDERS_ARGUMENT = "-k";
    
    /**
     * Prefix argument/option
     */
    String PREFIX_ARGUMENT = "--prefix";
    
    /**
     * Short prefix argument/option
     */
    String SHORT_PREFIX_ARGUMENT = "-p";

    /**
     * Batch file argument/option
     */
    static final String BATCH_FILE_ARGUMENT = "batchfile";

    /**
     * Short batch file argument/option
     */
    static final String SHORT_BATCH_FILE_ARGUMENT = "D";

    /**
     * Batch status argument/option
     */
    static final String BATCH_STATUS_ARGUMENT = "batchstatus";

    /**
     * Short batch status argument/option
     */
    static final String SHORT_BATCH_STATUS_ARGUMENT = "b";

    /**
     * Site name argument/option
     */
    static final String SITE_NAME_ARGUMENT = "sitename";

    /**
     * Short site name argument/option
     */
    static final String SHORT_SITE_NAME_ARGUMENT = "s";

    /**
     * Site url argument/option
     */
    static final String SITE_URL_ARGUMENT = "siteurl";

    /**
     * Short site name argument/option
     */
    static final String SHORT_SITE_URL_ARGUMENT = "i";

    /**
     * Site secondary urls argument/option
     */
    static final String SECONDARY_URLS_ARGUMENT = "secondaryurls";

    /**
     * Short site secondary urls argument/option
     */
    static final String SHORT_SECONDARY_URLS_ARGUMENT = "a";

    /**
     * Short server names argument/option
     */
    static final String SHORT_SERVER_NAMES_ARGUMENT = "e";

    /**
     * Server names argument/option
     */
    static final String SERVER_NAMES_ARGUMENT = "servernames";

    /**
     * Mandatory argument/option
     */
    static final String MANDATORY_ARGUMENT = "mandatory";
    
    /**
     * Short mandatory argument/option
     */
    static final String SHORT_MANDATORY_ARGUMENT = "y";
    
    /**
     * Entity ID argument/option
     */
    String ENTITYID_ARGUMENT = "--entityid";

    /**
     * Short Entity ID argument/option
     */
    String SHORT_ENTITYID_ARGUMENT = "-y";
    
    /**
     * Standard metadata argument/option
     */
    String METADATAFILE_ARGUMENT = "--meta-data-file";

    /**
     * Short Standard metadata argument/option
     */
    String SHORT_METADATAFILE_ARGUMENT = "-m";

    /**
     * Extended metadata argument/option
     */
    String EXTENDEDDATAFILE_ARGUMENT = "--extended-data-file";

    /**
     * Short Extended metadata argument/option
     */
    String SHORT_EXTENDEDDATAFILE_ARGUMENT = "-x";

    /**
     * Service Provider metaalias argument/option
     */
    String SP_METAALIAS_ARGUMENT = "--serviceprovider";

    /**
     * Short Service Provider metaalias argument/option
     */
    String SHORT_SP_METAALIAS_ARGUMENT = "-s";
                 
    /**
     * Identity Provider metaalias argument/option
     */
    String IDP_METAALIAS_ARGUMENT = "--identityprovider";

    /**
     * Short Identity Provider metaalias argument/option
     */
    String SHORT_IDP_METAALIAS_ARGUMENT = "-i";

    /**
     * Attribute Query Provider metaalias argument/option
     */
    String ATTRQP_METAALIAS_ARGUMENT = "--attrqueryprovider";

    /**
     * Short Attribute Query Provider metaalias argument/option
     */
    String SHORT_ATTRQP_METAALIAS_ARGUMENT = "-S";

    /**
     * Attribute Authority metaalias argument/option
     */
    String ATTRAUTH_METAALIAS_ARGUMENT = "--attrauthority";

    /**
     * Short Attribute Authority metaalias argument/option
     */
    String SHORT_ATTRAUTH_METAALIAS_ARGUMENT = "-I";

    /**
     * AuthN Authority metaalias argument/option
     */
    String AUTHNAUTH_METAALIAS_ARGUMENT = "--authnauthority";

    /**
     * Short AuthN Authority metaalias argument/option
     */
    String SHORT_AUTHNAUTH_METAALIAS_ARGUMENT = "-C";

    /**
     * Policy Enforcement Point metaalias argument/option
     */
    String XACMLPEP_METAALIAS_ARGUMENT = "--xacmlpep";

    /**
     * Short Policy Enforcement Point metaalias argument/option
     */
    String SHORT_XACMLPEP_METAALIAS_ARGUMENT = "-e";

    /**
     * Policy Decision Point metaalias argument/option
     */
    String XACMLPDP_METAALIAS_ARGUMENT = "--xacmlpdp";

    /**
     * Short Policy Decision Point metaalias argument/option
     */
    String SHORT_XACMLPDP_METAALIAS_ARGUMENT = "-p";

    /**
     * Hosted Affiliation metaalias argument/option
     */
    String AFFILIATION_METAALIAS_ARGUMENT = "--affiliation";

    /**
     * Short Hosted Affiliation metaalias argument/option
     */
    String SHORT_AFFILIATION_METAALIAS_ARGUMENT = "-F";

    /**
     * Affiliation Owner ID argument/option
     */
    String AFFILIATION_OWNERID_ARGUMENT = "--affiownerid";

    /**
     * Short Affiliation Owner ID argument/option
     */
    String SHORT_AFFILIATION_OWNERID_ARGUMENT = "-N";

    /**
     * Affiliation members argument/option
     */
    String AFFILIATION_MEMBERS_ARGUMENT = "--affimembers";

    /**
     * Short Affiliation members argument/option
     */
    String SHORT_AFFILIATION_MEMBERS_ARGUMENT = "-M";

    /**
     * Service provider signing certificate alias argument/option
     */
    String SP_SCERTALIAS_ARGUMENT = "--spscertalias";

    /**
     * Short Service provider signing certificate alias argument/option
     */
    String SHORT_SP_SCERTALIAS_ARGUMENT = "-a";

    /**
     * Identity provider signing certificate alias argument/option
     */
    String IDP_SCERTALIAS_ARGUMENT = "--idpscertalias";

    /**
     * Short Identity provider signing certificate alias argument/option
     */
    String SHORT_IDP_SCERTALIAS_ARGUMENT = "-b";

    /**
     * Attribute query provider signing certificate alias argument/option
     */
    String ATTRQSCERTALIAS_ARGUMENT = "--attrqscertalias";

    /**
     * Short Attribute query provider signing certificate alias argument/option
     */
    String SHORT_ATTRQSCERTALIAS_ARGUMENT = "-A";

    /**
     * Attribute authority signing certificate alias argument/option
     */
    String ATTRASCERTALIAS_ARGUMENT = "--attrascertalias";

    /**
     * Short Attribute authority signing certificate alias argument/option
     */
    String SHORT_ATTRASCERTALIAS_ARGUMENT = "-B";

    /**
     * Authentication authority signing certificate alias argument/option
     */
    String AUTHNASCERTALIAS_ARGUMENT = "--authnascertalias";

    /**
     * Short Authentication authority signing certificate alias argument/option
     */
    String SHORT_AUTHNASCERTALIAS_ARGUMENT = "-D";

    /**
     * Affiliation signing certificate alias argument/option
     */
    String AFFISCERTALIAS_ARGUMENT = "--affiscertalias";

    /**
     * Short Affiliation signing certificate alias argument/option
     */
    String SHORT_AFFISCERTALIAS_ARGUMENT = "-J";

    /**
     * Policy enforcement point signing certificate alias argument/option
     */
    String XACMLPEPSCERTALIAS_ARGUMENT = "--xacmlpepscertalias";

    /**
     * Short Policy enforcement point signing certificate alias argument/option
     */
    String SHORT_XACMLPEPSCERTALIAS_ARGUMENT = "-k";

    /**
     * Policy decision point signing certificate alias argument/option
     */
    String XACMLPDPSCERTALIAS_ARGUMENT = "--xacmlpdpscertalias";

    /**
     * Short Policy decision point signing certificate alias argument/option
     */
    String SHORT_XACMLPDPSCERTALIAS_ARGUMENT = "-t";

    /**
     * Service provider encryption certificate alias argument/option
     */
    String SP_ECERTALIAS_ARGUMENT = "--specertalias";

    /**
     * Short Service provider encryption certificate alias argument/option
     */
    String SHORT_SP_ECERTALIAS_ARGUMENT = "-r";

    /**
     * Identity provider encryption certificate alias argument/option
     */
    String IDP_ECERTALIAS_ARGUMENT = "--idpecertalias";
                                       
    /**
     * Short Identity provider encryption certificate alias argument/option
     */
    String SHORT_IDP_ECERTALIAS_ARGUMENT = "-g";

    /**
     * Attribute query provider encryption certificate alias argument/option
     */
    String ATTRQECERTALIAS_ARGUMENT = "--attrqecertalias";

    /**
     * Short Attribute query provider encryption certificate alias 
     * argument/option
     */
    String SHORT_ATTRQECERTALIAS_ARGUMENT = "-R";

    /**
     * Attribute authority encryption certificate alias argument/option
     */
    String ATTRAECERTALIAS_ARGUMENT = "--attraecertalias";

    /**
     * Short Attribute authority encryption certificate alias argument/option
     */
    String SHORT_ATTRAECERTALIAS_ARGUMENT = "-G";


    /**
     * Authentication authority encryption certificate alias argument/option
     */
    String AUTHNAECERTALIAS_ARGUMENT = "--authnaecertalias";

    /**
     * Short Authentication authority encryption certificate alias 
     * argument/option
     */
    String SHORT_AUTHNAECERTALIAS_ARGUMENT = "-E";

    /**
     * Affiliation encryption certificate alias argument/option
     */
    String AFFIECERTALIAS_ARGUMENT = "--affiecertalias";
                                        
    /**
     * Short Affiliation encryption certificate alias argument/option
     */
    String SHORT_AFFIECERTALIAS_ARGUMENT = "-K";

    /**
     * Policy decision point encryption certificate alias argument/option
     */
    String XACMLPDPECERTALIAS_ARGUMENT = "--xacmlpdpecertalias";

    /**
     * Short Policy decision point encryption certificate alias argument/option
     */
    String SHORT_XACMLPDPECERTALIAS_ARGUMENT = "-j";

    /**
     * Policy enforcement point encryption certificate alias argument/option
     */
    String XACMLPEPECERTALIAS_ARGUMENT = "--xacmlpepecertalias";

    /**
     * Short Policy enforcement point encryption certificate alias argument/option
     */
    String SHORT_XACMLPEPECERTALIAS_ARGUMENT = "-z";

    /**
     * Specify metadata specification, either idff or saml2, defaults to saml2 
     * argument/option
     */
    String SPEC_ARGUMENT = "--spec";

    /**
     * Short Specify metadata specification, either idff or saml2, defaults to 
     * saml2 argument/option
     */
    String SHORT_SPEC_ARGUMENT = "-c";

    /**
     * Specify to set this flag to sign the metadata argument/option
     */
    String SIGN_ARGUMENT = "--sign";

    /**
     * Short Specify to set this flag to sign the metadata argument/option
     */
    String SHORT_SIGN_ARGUMENT = "-g";
   
}
