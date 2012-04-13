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
 * $Id: FederationManagerCLI.java,v 1.23 2009/07/28 02:57:13 srivenigan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common.cli;

import com.sun.identity.qatest.cli.CLIConstants;
import com.sun.identity.qatest.cli.FederationManagerCLIConstants;
import com.sun.identity.qatest.cli.GlobalConstants;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;

/**
 * <code>AccessMangerCLI</code> is a utility class which allows the user
 * to invoke the ssoadm CLI to perform operations which correspond to supported
 * sub-commands of ssoadm (e.g. create-realm, delete-realm, list-realms,
 * create-identity, delete-identity, list-identities, etc.).
 */
public class FederationManagerCLI extends CLIUtility 
        implements CLIConstants, FederationManagerCLIConstants, 
        GlobalConstants, CLIExitCodes {
	private boolean useDebugOption;
    private boolean useVerboseOption;
    private boolean useLongOptions;
    private long commandTimeout;
    private static int SUBCOMMAND_VALUE_INDEX = 1;
    private static int ADMIN_ID_ARG_INDEX = 2;
    private static int ADMIN_ID_VALUE_INDEX = 3;
    private static int PASSWORD_ARG_INDEX = 4;
    private static int PASSWORD_VALUE_INDEX = 5;
    private static int DEFAULT_COMMAND_TIMEOUT = 20;
    
    /**
     * Create a new instance of <code>FederationManagerCLI</code>
     * @param useDebug - a flag indicating whether to add the debug option
     * @param useVerbose - a flag indicating whether to add the verbose option
     * @param useLongOpts - a flag indicating whether long options 
     * (e.g. --realm) should be used
     */
    public FederationManagerCLI(boolean useDebug, boolean useVerbose, 
            boolean useLongOpts)
    throws Exception {
        super(new StringBuffer(cliPath).
                append(System.getProperty("file.separator")).append(uri).
                append(System.getProperty("file.separator")).append("bin").
                append(System.getProperty("file.separator")).append("ssoadm").
                toString());    
        useLongOptions = useLongOpts;
        try {
            addAdminUserArgs();
            addPasswordArgs();
            useDebugOption = useDebug;
            useVerboseOption = useVerbose;
            commandTimeout = (new Long(timeout).longValue()) * 1000;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;         
        }        
    }

    /**
     * Creates valid test service if validServiceXml attribute is 
     * true and invalid test service if the validServiceXml attribute is true.
     * 
     * @param serviceXmlName - Name(s) of the service xml.
     * @param initRevisionNo - Revision number(s) of the service to be created.
     * @param validServiceXml - Flag to generate valid or invalid service xml.
     * @param continueAddingService - if "true" adds one or more 
     * services continuously.
     * @return int - exit status of "create-svc" command.
     */
    public int createService(String serviceXmlName, String initRevisionNo, 
            boolean validServiceXml, boolean continueAddingService) 
    throws Exception {
        String xmlFileArg; 
        if (useLongOptions) {
            xmlFileArg = PREFIX_ARGUMENT_LONG + XML_FILE_ARGUMENT;
        } else {
            xmlFileArg = PREFIX_ARGUMENT_SHORT + SHORT_XML_FILE_ARGUMENT;
        }   
        addArgument(xmlFileArg);
        setSubcommand(CREATE_SERVICE_SUBCOMMAND);
        // check if service name argument is empty.
        if (!serviceXmlName.trim().equals("")) {
            String[] createServices = serviceXmlName.trim().split(",");
            String[] revisionNumbers = initRevisionNo.split(",");
            List serviceList = new LinkedList(Arrays.asList(createServices));
            List revisionNumberList = new LinkedList(
                    Arrays.asList(revisionNumbers));
            // generate revision numbers for test services if not supplied.
            while (revisionNumberList.size() < serviceList.size()) {
                revisionNumberList.add("10");
            } 
            if (validServiceXml) {
                for (int i=0; i < serviceList.size(); i++) {
                    addServiceXmlArg((String)serviceList.get(i),
                            (String)revisionNumberList.get(i));   
                }
            } else {
                for (int i=0; i < serviceList.size(); i++) {
                    addInvalidServiceXmlArg((String)serviceList.get(i));
                }
            }
        }
        if (continueAddingService) 
            addContinueArgument();
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Creates a test service by generating xml with given service name 
     * and revision number.
     * 
     * @param serviceXmlName - Name of the service xml to be created. 
     * @param initRevisionNo - Revision number initial value 
     * @return the exit status of the "create-svc" command
     */
    public int createService(String serviceXmlName, String initRevisionNo) 
    throws Exception {
        setSubcommand(CREATE_SERVICE_SUBCOMMAND);
        addXMLFileArguments(serviceXmlName, initRevisionNo);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }

    /**
     * Get the attribute default values of the service with given schema type
     * schema name and attribute names as list.
     * 
     * @param serviceName - Name of service.
     * @param schemaType - Type of schema.
     * @param subSchemaName -  Name of sub schema.
     * @param attNames - Attribute name(s).
     * @return the exit status of the "get-attr-defs" command
     * @throws Exception
     */
    public int getAttrDefs(String serviceName, String schemaType, 
            String subSchemaName, List attNameList) 
    throws Exception {
    	String attNames = "";
        setSubcommand(GET_ATTR_DEFS_SUBCOMMAND);
        addServiceNameArguments(serviceName);
        addSchemaTypeArguments(schemaType);
        addSubSchemaNameArguments(subSchemaName);
        for (int i=0; i < attNameList.size(); i++) {
        	attNames = attNames + (String)attNameList.get(i) + ";";
        }
        addAttributeNamesArguments(attNames);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }

    /**
     * 
     * @param serviceName - Service name for which attributes are removed.
     * @param schemaType - Schema type.
     * @param subSchemaName - Name of sub schema. 
     * @param attNames - Attribute values e.g. homeaddress=here.
     * @return exit status 
     * @throws java.lang.Exception
     */
    public int removeAttrDefs(String serviceName, String schemaType, 
            String subSchemaName, String attNames) 
    throws Exception {
        setSubcommand(REMOVE_ATTR_DEFS_SUBCOMMAND);
        addServiceNameArguments(serviceName);
        addSchemaTypeArguments(schemaType);
        addSubSchemaNameArguments(subSchemaName);
        addAttributeNamesArguments(attNames);
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Sets the "--schematype" and schema type in the argument list.
     * @param schemaType
     */
    private void addSchemaTypeArguments(String schemaType) {
        String schemaTypeArg;
        if (useLongOptions) {
            schemaTypeArg = PREFIX_ARGUMENT_LONG +
                    SCHEMA_TYPE_ARGUMENT;
        } else {
            schemaTypeArg = PREFIX_ARGUMENT_SHORT +
                    SHORT_SCHEMA_TYPE_ARGUMENT;
        }
        addArgument(schemaTypeArg);
        addArgument(schemaType);
	}
    
    /**
     * Sets the "--subschemaname" and sub-schema name in the argument list. 
     * @param subSchemaName
     */
    private void addSubSchemaNameArguments(String subSchemaName) {
    	String subSchemaNameArg;
        if (!subSchemaName.trim().equals("")) {
            if (useLongOptions) {
                subSchemaNameArg = PREFIX_ARGUMENT_LONG + 
                        SUB_SCHEMA_NAME_ARGUMENT;
            } else {
    		subSchemaNameArg = PREFIX_ARGUMENT_SHORT + 
                        SHORT_SUB_SCHEMA_NAME_ARGUMENT;
            }
            addArgument(subSchemaNameArg);
            addArgument(subSchemaName);
        }
    }

    /**
     * Generates invalid test service xml by taking name of the service.
     * @param serviceName - Name of the service
     * @throws java.lang.Exception
     */
    private void addInvalidServiceXmlArg(String serviceName) 
    throws Exception {
        ResourceBundle rb_amconfig = 
                ResourceBundle.getBundle(TestConstants.TEST_PROPERTY_AMCONFIG);
        String attFileDir = getBaseDir() + fileseparator + 
                rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME) + 
                fileseparator + "built" + fileseparator + "classes" + 
                fileseparator + "cli" + fileseparator;
        String attFile = attFileDir + serviceName + ".xml"; 
        BufferedWriter out = new BufferedWriter(new FileWriter(attFile));
        // now generate the invalid service xml file
        out.write("<!DOCTYPE ServicesConfiguration");
        out.write(newline);
        out.write("PUBLIC \"=//iPlanet//Service Management " +
        		"Services (SMS) 1.0 DTD//EN\"" + newline);
        out.write("\"jar://com/sun/identity/sm/sms.dtd\">" + newline);
        out.write("<ServicesConfiguration>" + newline);
        out.write("<Service name=\"" + serviceName + "\" version=\"1.0\">");
        out.write(newline);
        out.write("<Schema " + newline);
        out.write("revisionNumber=\"\">");
        out.write(newline);
        out.write("<Global>");
        out.write(newline);
        out.write("<AttributeSchema name=\"mock\"" + newline);
        out.write("type=\"single\"" + newline);
        out.write("syntax=\"string\"" + newline);
        out.write("i18nKey=\"\">" + newline);
        out.write("</AttributeSchema>" + newline);
        out.write("<AttributeSchema name=\"mock-boolean\"" + newline);
        out.write("type=\"single\"" + newline);
        out.write("syntax=\"boolean\"" + newline);
        out.write("i18nKey=\"\">" + newline);
        out.write("</AttributeSchema>" + newline);       
        out.close();
        
        addArgument(attFile);
    }
    
    /**
     * Adds the "--cot" argument and cot value to the argument list
     * @param cot - name of circle of trust to add to argument list
     */
    public void addCotArguments(String cot) {
        String cotArg;
        if (useLongOptions) {
            cotArg = COT_ARGUMENT;
        } else {
            cotArg = SHORT_COT_ARGUMENT;
        }      
        addArgument(cotArg);
        addArgument(cot);
    }

    /**
     * Adds the "--entityid" argument and entityid value to the argument list
     * @param entityid - entity id
     */
    public void addEntityIDArguments(String entityid) {
        String entityidArg;
        if (useLongOptions) {
            entityidArg = ENTITYID_ARGUMENT;
        } else {
            entityidArg = SHORT_ENTITYID_ARGUMENT;
        }
        addArgument(entityidArg);
        addArgument(entityid);
    }

    /**
     * Adds the "--meta-data-file" argument and metadatafile value to
     * the argument list
     * @param metadatafile - Standard metadata file
     */
    public void addMetadataFileArguments(String metadatafile) {
        String metadatafileArg;
        if (useLongOptions) {
            metadatafileArg = METADATAFILE_ARGUMENT;
        } else {
            metadatafileArg = SHORT_METADATAFILE_ARGUMENT;
        }
        addArgument(metadatafileArg);
        addArgument(metadatafile);
    }

    /**
     * Adds the "--extended-data-file" argument and entityid value to
     * the argument list
     * @param extendeddatafile - Extended metadata file
     */
    public void addExtendeddataFileArguments(String extendeddatafile) {
        String extendeddatafileArg;
        if (useLongOptions) {
            extendeddatafileArg = EXTENDEDDATAFILE_ARGUMENT;
        } else {
            extendeddatafileArg = SHORT_EXTENDEDDATAFILE_ARGUMENT;
        }
        addArgument(extendeddatafileArg);
        addArgument(extendeddatafile);
    }

    /**
     * Adds the "--identityprovider" argument and identityprovider value to
     * the argument list
     * @param identityprovider - metaalias of identity provider
     */
    public void addIDPMetaaliasArguments(String idpmetaalias) {
        String idpmetaaliasArg;
        if (useLongOptions) {
            idpmetaaliasArg = IDP_METAALIAS_ARGUMENT;
        } else {
            idpmetaaliasArg = SHORT_IDP_METAALIAS_ARGUMENT;
        }
        addArgument(idpmetaaliasArg);
        addArgument(idpmetaalias);
    }

    /**
     * Adds the "--serviceprovider" argument and serviceprovider value to
     * the argument list
     * @param serviceprovider - metaalias of service provider
     */
    public void addSPMetaaliasArguments(String spmetaalias) {
        String spmetaaliasArg;
        if (useLongOptions) {
            spmetaaliasArg = SP_METAALIAS_ARGUMENT;
        } else {
            spmetaaliasArg = SHORT_SP_METAALIAS_ARGUMENT;
        }
        addArgument(spmetaaliasArg);
        addArgument(spmetaalias);
    }

    /**
     * Adds the "--attrqueryprovider" argument and attrqueryprovider value to
     * the argument list
     * @param attrqueryprovider - metaalias for attribute query provider
     */
    public void addAttrQueryProviderArguments(String attrqueryprovider) {
        String attrqueryproviderArg;
        if (useLongOptions) {
            attrqueryproviderArg = ATTRQP_METAALIAS_ARGUMENT;
        } else {
            attrqueryproviderArg = SHORT_ATTRQP_METAALIAS_ARGUMENT;
        }
        addArgument(attrqueryproviderArg);
        addArgument(attrqueryprovider);
    }

    /**
     * Adds the "--attrauthority" argument and attrauthority value to
     * the argument list
     * @param attrauthority - metaalias for attribute authority
     */
    public void addAttrAuthorityArguments(String attrauthority) {
        String attrauthorityArg;
        if (useLongOptions) {
            attrauthorityArg = ATTRAUTH_METAALIAS_ARGUMENT;
        } else {
            attrauthorityArg = SHORT_ATTRAUTH_METAALIAS_ARGUMENT;
        }
        addArgument(attrauthorityArg);
        addArgument(attrauthority);
    }

    /**
     * Adds the "--authnauthority" argument and authnauthority value to
     * the argument list
     * @param authnauthority - metaalias for authn authority
     */
    public void addAuthNAuthorityArguments(String authnauthority) {
        String authnauthorityArg;
        if (useLongOptions) {
            authnauthorityArg = AUTHNAUTH_METAALIAS_ARGUMENT;
        } else {
            authnauthorityArg = SHORT_AUTHNAUTH_METAALIAS_ARGUMENT;
        }
        addArgument(authnauthorityArg);
        addArgument(authnauthority);
    }

    /**
     * Adds the "--xacmlpep" argument and xacmlpep value to
     * the argument list
     * @param xacmlpep - metaalias for xacml policy enforcement point
     */
    public void addXacmlPEPArguments(String xacmlpep) {
        String xacmlpepArg;
        if (useLongOptions) {
            xacmlpepArg = XACMLPEP_METAALIAS_ARGUMENT;
        } else {
            xacmlpepArg = SHORT_XACMLPEP_METAALIAS_ARGUMENT;
        }
        addArgument(xacmlpepArg);
        addArgument(xacmlpep);
    }

    /**
     * Adds the "--xacmlpdp" argument and xacmlpdp value to
     * the argument list
     * @param xacmlpdp - metaalias for xacml policy decision point
     */
    public void addXacmlPDPArguments(String xacmlpdp) {
        String xacmlpdpArg;
        if (useLongOptions) {
            xacmlpdpArg = XACMLPDP_METAALIAS_ARGUMENT;
        } else {
            xacmlpdpArg = SHORT_XACMLPDP_METAALIAS_ARGUMENT;
        }
        addArgument(xacmlpdpArg);
        addArgument(xacmlpdp);
    }

    /**
     * Adds the "--affiliation" argument and affiliation value to
     * the argument list
     * @param affiliation - metaalias for hosted affiliation
     */
    public void addAffiliationArguments(String affiliation) {
        String affiliationArg;
        if (useLongOptions) {
            affiliationArg = AFFILIATION_METAALIAS_ARGUMENT;
        } else {
            affiliationArg = SHORT_AFFILIATION_METAALIAS_ARGUMENT;
        }
        addArgument(affiliationArg);
        addArgument(affiliation);
    }

    /**
     * Adds the "--affiownerid" argument and affiownerid value to
     * the argument list
     * @param affiownerid - Affiliation Owner ID
     */
    public void addAffiOwnerIDArguments(String affiownerid) {
        String affiowneridArg;
        if (useLongOptions) {
            affiowneridArg = AFFILIATION_OWNERID_ARGUMENT;
        } else {
            affiowneridArg = SHORT_AFFILIATION_OWNERID_ARGUMENT;
        }
        addArgument(affiowneridArg);
        addArgument(affiownerid);
    }
    
    /**
     * Adds the "--affimembers" argument and affimembers value to
     * the argument list
     * @param affiownerid - Affiliation members
     */
    public void addAffiMembersArguments(String affimembers) {
        String affimembersArg;
        if (useLongOptions) {
            affimembersArg = AFFILIATION_MEMBERS_ARGUMENT;
        } else {
            affimembersArg = SHORT_AFFILIATION_MEMBERS_ARGUMENT;
        }
        addArgument(affimembersArg);
        addArgument(affimembers);
    }

    /**
     * Adds the "--spscertalias" argument and spscertalias value to
     * the argument list
     * @param spscertalias - Service provider signing certificate alias
     */
    public void addSPSCertAliasArguments(String spscertalias) {
        String spscertaliasArg;
        if (useLongOptions) {
            spscertaliasArg = SP_SCERTALIAS_ARGUMENT;
        } else {
            spscertaliasArg = SHORT_SP_SCERTALIAS_ARGUMENT;
        }
        addArgument(spscertaliasArg);
        addArgument(spscertalias);
    }
    
    /**
     * Adds the "--idpscertalias" argument and idpscertalias value to
     * the argument list
     * @param idpscertalias - Identity provider signing certificate alias
     */
    public void addIDPSCertAliasArguments(String idpscertalias) {
        String idpscertaliasArg;
        if (useLongOptions) {
            idpscertaliasArg = IDP_SCERTALIAS_ARGUMENT;
        } else {
            idpscertaliasArg = SHORT_IDP_SCERTALIAS_ARGUMENT;
        }
        addArgument(idpscertaliasArg);
        addArgument(idpscertalias);
    }

    /**
     * Adds the "--attrqscertalias" argument and attrqscertalias value to
     * the argument list
     * @param attrqscertalias - Attribute query provider signing
     * certificate alias
     */
    public void addAttrQSCertAliasArguments(String attrqscertalias) {
        String attrqscertaliasArg;
        if (useLongOptions) {
            attrqscertaliasArg = ATTRQSCERTALIAS_ARGUMENT;
        } else {
            attrqscertaliasArg = SHORT_ATTRQSCERTALIAS_ARGUMENT;
        }
        addArgument(attrqscertaliasArg);
        addArgument(attrqscertalias);
    }

    /**
     * Adds the "--attrascertalias" argument and attrascertalias value to
     * the argument list
     * @param attrascertalias - Attribute authority signing certificate alias
     */
    public void addAttrASCertAliasArguments(String attrascertalias) {
        String attrascertaliasArg;
        if (useLongOptions) {
            attrascertaliasArg = ATTRASCERTALIAS_ARGUMENT;
        } else {
            attrascertaliasArg = SHORT_ATTRASCERTALIAS_ARGUMENT;
        }
        addArgument(attrascertaliasArg);
        addArgument(attrascertalias);
    }

    /**
     * Adds the "--authnascertalias" argument and authnascertalias value to
     * the argument list
     * @param authnascertalias - Authentication authority signing
     * certificate alias
     */
    public void addAuthNASCertAliasArguments(String authnascertalias) {
        String authnascertaliasArg;
        if (useLongOptions) {
            authnascertaliasArg = AUTHNASCERTALIAS_ARGUMENT;
        } else {
            authnascertaliasArg = SHORT_AUTHNASCERTALIAS_ARGUMENT;
        }
        addArgument(authnascertaliasArg);
        addArgument(authnascertalias);
    }
    
    /**
     * Adds the "--affiscertalias" argument and affiscertalias value to
     * the argument list
     * @param affiscertalias - Affiliation signing certificate alias
     */
    public void addAffiSCertAliasArguments(String affiscertalias) {
        String affiscertaliasArg;
        if (useLongOptions) {
            affiscertaliasArg = AFFISCERTALIAS_ARGUMENT;
        } else {
            affiscertaliasArg = SHORT_AFFISCERTALIAS_ARGUMENT;
        }
        addArgument(affiscertaliasArg);
        addArgument(affiscertalias);
    }

    /**
     * Adds the "--xacmlpdpscertalias" argument and xacmlpdpscertalias value to
     * the argument list
     * @param xacmlpdpscertalias - Policy decision point signing
     * certificate alias
     */
    public void addXacmlPDPSCertAliasArguments(String xacmlpdpscertalias) {
        String xacmlpdpscertaliasArg;
        if (useLongOptions) {
            xacmlpdpscertaliasArg = XACMLPDPSCERTALIAS_ARGUMENT;
        } else {
            xacmlpdpscertaliasArg = SHORT_XACMLPDPSCERTALIAS_ARGUMENT;
        }
        addArgument(xacmlpdpscertaliasArg);
        addArgument(xacmlpdpscertalias);
    }

    /**
     * Adds the "--xacmlpepscertalias" argument and xacmlpepscertalias value to
     * the argument list
     * @param xacmlpepscertalias - Policy decision point signing
     * certificate alias
     */
    public void addXacmlPEPSCertAliasArguments(String xacmlpepscertalias) {
        String xacmlpepscertaliasArg;
        if (useLongOptions) {
            xacmlpepscertaliasArg = XACMLPEPSCERTALIAS_ARGUMENT;
        } else {
            xacmlpepscertaliasArg = SHORT_XACMLPEPSCERTALIAS_ARGUMENT;
        }
        addArgument(xacmlpepscertaliasArg);
        addArgument(xacmlpepscertalias);
    }
    
    /**
     * Adds the "--specertalias" argument and specertalias value to
     * the argument list
     * @param specertalias - Service provider encryption certificate alias
     */
    public void addSPECertAliasArguments(String specertalias) {
        String specertaliasArg;
        if (useLongOptions) {
            specertaliasArg = SP_ECERTALIAS_ARGUMENT;
        } else {
            specertaliasArg = SHORT_SP_ECERTALIAS_ARGUMENT;
        }
        addArgument(specertaliasArg);
        addArgument(specertalias);
    }

    /**
     * Adds the "--idpecertalias" argument and idpecertalias value to
     * the argument list
     * @param idpecertalias - Identity provider encryption certificate alias
     */
    public void addIDPECertAliasArguments(String idpecertalias) {
        String idpecertaliasArg;
        if (useLongOptions) {
            idpecertaliasArg = IDP_ECERTALIAS_ARGUMENT;
        } else {
            idpecertaliasArg = SHORT_IDP_ECERTALIAS_ARGUMENT;
        }
        addArgument(idpecertaliasArg);
        addArgument(idpecertalias);
    }

    /**
     * Adds the "--attrqecertalias" argument and attrqecertalias value to
     * the argument list
     * @param attrqecertalias - Attribute query provider encryption
     * certificate alias
     */
    public void addAttrQECertAliasArguments(String attrqecertalias) {
        String attrqecertaliasArg;
        if (useLongOptions) {
            attrqecertaliasArg = ATTRQECERTALIAS_ARGUMENT;
        } else {
            attrqecertaliasArg = SHORT_ATTRQECERTALIAS_ARGUMENT;
        }
        addArgument(attrqecertaliasArg);
        addArgument(attrqecertalias);
    }

    /**
     * Adds the "--attraecertalias" argument and attraecertalias value to
     * the argument list
     * @param attraecertalias - Attribute authority encryption certificate alias
     */
    public void addAttrAECertAliasArguments(String attraecertalias) {
        String attraecertaliasArg;
        if (useLongOptions) {
            attraecertaliasArg = ATTRAECERTALIAS_ARGUMENT;
        } else {
            attraecertaliasArg = SHORT_ATTRAECERTALIAS_ARGUMENT;
        }
        addArgument(attraecertaliasArg);
        addArgument(attraecertalias);
    }

    /**
     * Adds the "--authnaecertalias" argument and authnaecertalias value to
     * the argument list
     * @param authnaecertalias - Authentication authority encryption
     * certificate alias
     */
    public void addAuthNAECertAliasArguments(String authnaecertalias) {
        String authnaecertaliasArg;
        if (useLongOptions) {
            authnaecertaliasArg = AUTHNAECERTALIAS_ARGUMENT;
        } else {
            authnaecertaliasArg = SHORT_AUTHNAECERTALIAS_ARGUMENT;
        }
        addArgument(authnaecertaliasArg);
        addArgument(authnaecertalias);
    }

    /**
     * Adds the "--affiecertalias" argument and affiecertalias value to
     * the argument list
     * @param affiecertalias - Affiliation encryption certificate alias
     */
    public void addAffiECertAliasArguments(String affiecertalias) {
        String affiecertaliasArg;
        if (useLongOptions) {
            affiecertaliasArg = AFFIECERTALIAS_ARGUMENT;
        } else {
            affiecertaliasArg = SHORT_AFFIECERTALIAS_ARGUMENT;
        }
        addArgument(affiecertaliasArg);
        addArgument(affiecertalias);
    }

    /**
     * Adds the "--xacmlpdpecertalias" argument and xacmlpdpecertalias value to
     * the argument list
     * @param xacmlpdpecertalias - Policy decision point encryption
     * certificate alias
     */
    public void addXacmlPDPECertAliasArguments(String xacmlpdpecertalias) {
        String xacmlpdpecertaliasArg;
        if (useLongOptions) {
            xacmlpdpecertaliasArg = XACMLPDPECERTALIAS_ARGUMENT;
        } else {
            xacmlpdpecertaliasArg = SHORT_XACMLPDPECERTALIAS_ARGUMENT;
        }
        addArgument(xacmlpdpecertaliasArg);
        addArgument(xacmlpdpecertalias);
    }

    /**
     * Adds the "--xacmlpepecertalias" argument and xacmlpepecertalias value to
     * the argument list
     * @param xacmlpepecertalias - Policy enforcement point encryption
     * certificate alias
     */
    public void addXacmlPEPECertAliasArguments(String xacmlpepecertalias) {
        String xacmlpepecertaliasArg;
        if (useLongOptions) {
            xacmlpepecertaliasArg = XACMLPEPECERTALIAS_ARGUMENT;
        } else {
            xacmlpepecertaliasArg = SHORT_XACMLPEPECERTALIAS_ARGUMENT;
        }
        addArgument(xacmlpepecertaliasArg);
        addArgument(xacmlpepecertalias);
    }

    /**
     * Adds the "--spec" argument and spec value to the argument list
     * @param spec - specification of the metadata
     */
    public void addSpecArguments(String spec) {
        String specArg;
        if (useLongOptions) {
            specArg = SPEC_ARGUMENT;
        } else {
            specArg = SHORT_SPEC_ARGUMENT;
        }
        addArgument(specArg);
        addArgument(spec);
    }

    /**
     * Adds the "--sign" argument and sign value to the argument list
     * @param sign - Flag to sign the metadata
     */
    public void addSignArguments(String sign) {
        String signArg;
        if (useLongOptions) {
            signArg = SIGN_ARGUMENT;
        } else {
            signArg = SHORT_SIGN_ARGUMENT;
        }
        addArgument(signArg);
        addArgument(sign);
    }

    /**
     * Adds the "--prefix" argument and prefix value to argument list
     * @param prefix - prefix string.
     */
    public void addPrefixArguments(String prefix) {
        String prefixArg;
        if (useLongOptions) {
            prefixArg = PREFIX_ARGUMENT;
        } else {
            prefixArg = SHORT_PREFIX_ARGUMENT;
        }      
        addArgument(prefixArg);
        addArgument(prefix);        
    }
    
    /**
     * Adds the "--trustedproviders" argument and trusted providers arguments to
     * the argument list
     * @param trustedproviders - a semi-colon delimited list of trustedproviders
     */
    public void addTrustedProvidersArguments(String trustedproviders) {
        String trustedprovidersArg;
        if (useLongOptions) {
            trustedprovidersArg = TRUSTEDPROVIDERS_ARGUMENT;
        } else {
            trustedprovidersArg = SHORT_TRUSTEDPROVIDERS_ARGUMENT;
        }      
        addArgument(trustedprovidersArg);
        String[] providerList = trustedproviders.split(";");
        for (int i=0; i < providerList.length; i++) {
            addArgument(trustedproviders);
        }
    }
    
    /**
     * Creates circle of trust.
     * @param cot - Name of circle of trust.
     * @param realm - Name of realm.
     * @param trustedproviders - Name(s) of trusted providers.
     * @param prefix - Prefix URL for idp discovery reader and writer URL.
     * @return int - command status 
     * @throws java.lang.Exception
     */
    public int createCot(String cot, String realm, String trustedproviders,
            String prefix) throws Exception {
        setSubcommand(CREATE_COT);
        addCotArguments(cot);
        addRealmArguments(realm);
        addTrustedProvidersArguments(trustedproviders);
        addPrefixArguments(prefix);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Creates circle of trust.
     * @param cot - Name of circle of trust.
     * @param realm - Name of realm.    
     * @return int - command status 
     * @throws java.lang.Exception
     */
    public int createCot(String cot, String realm) throws Exception {
        setSubcommand(CREATE_COT);
        addCotArguments(cot);
        addRealmArguments(realm); 
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }

    /**
     * Creates metadata template
     * @param entityid - Name of entityid.
     * @param metadatafile - Location of standard metadata file
     * @param extendeddatafile - Location of extended data file
     * @param midp - metaalias of identity provider
     * @param msp - metaalias of service provider
     * @param spec - specification of metadata
     * @return int - command status
     * @throws java.lang.Exception
     */
    public int createMetadataTempl(String entityid,
            String metadatafile, String extendeddatafile, String midp,
            String msp, String spec) throws Exception {
        setSubcommand(CREATE_METADATA_TEMPLATE_SUBCOMMAND);
        addEntityIDArguments(entityid);
        addMetadataFileArguments(metadatafile);
        addExtendeddataFileArguments(extendeddatafile);
        if (midp.equals("")) {
            addSPMetaaliasArguments(msp);
        } else if (msp.equals("")) {
            addIDPMetaaliasArguments(midp);
        } else {
            addIDPMetaaliasArguments(midp);
            addSPMetaaliasArguments(msp);
        }
        addSpecArguments(spec);      
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }

    /**
     * Creates metadata template
     * @param entityid - Name of entityid.
     * @param metadatafile - Location of standard metadata file
     * @param extendeddatafile - Location of extended data file
     * @param midp - metaalias of identity provider
     * @param msp - metaalias of service provider
     * @param optionalattributes - optional attributes of the metadata
     * @param spec - specification of metadata
     * @return int - command status
     * @throws java.lang.Exception
     */
    public int createMetadataTempl(String entityid,
            String metadatafile, String extendeddatafile, String midp,
            String msp, String optionalattributes,
            String spec) throws Exception {
        Map map = new HashMap();
        setSubcommand(CREATE_METADATA_TEMPLATE_SUBCOMMAND);
        addEntityIDArguments(entityid);
        addMetadataFileArguments(metadatafile);
        addExtendeddataFileArguments(extendeddatafile);
        if (midp.equals("")) {
            addSPMetaaliasArguments(msp);
        } else if (msp.equals("")) {
            addIDPMetaaliasArguments(midp);
        } else {
            addIDPMetaaliasArguments(midp);
            addSPMetaaliasArguments(msp);
        }
        map = parseStringToRegularMap(optionalattributes, ",");
        createMetaTemplOptionalAttributes(map);
        addSpecArguments(spec);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }

    /**
     * Imports the metadata to create a entity
     * @param realm - Realm where the cot is created
     * @param metadatafile - Location of standard metadata file
     * @param extendeddatafile - Location of extended data file
     * @param cot - name of the circle of trust
     * @param spec - specification of metadata
     * @return int - command status
     * @throws java.lang.Exception
     */
    public int importEntity(String realm,
            String metadatafile, String extendeddatafile, String cot,
            String spec) throws Exception {
        setSubcommand(IMPORT_ENTITY_SUBCOMMAND);
        addRealmArguments(realm);
        addMetadataFileArguments(metadatafile);
        addExtendeddataFileArguments(extendeddatafile);
        if (!cot.equals("")) {
            addCotArguments(cot);
        }        
        addSpecArguments(spec);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }

    /**
     * Exports the metadata's in the form of xml's
     * @param entityid - Name of entityid.
     * @param realm - Realm where the cot is created
     * @param sign - To sign the metadata
     * @param metadatafile - Location of standard metadata file
     * @param extendeddatafile - Location of extended data file     
     * @param spec - specification of metadata
     * @return int - command status
     * @throws java.lang.Exception
     */
    public int exportEntity(String entityid, String realm, String sign,
            String metadatafile, String extendeddatafile,
            String spec) throws Exception {
        setSubcommand(EXPORT_ENTITY_SUBCOMMAND);
        addEntityIDArguments(entityid);
        addRealmArguments(realm);
        if (sign.equalsIgnoreCase("true")) {
            addSignArguments(sign);
        }
        addMetadataFileArguments(metadatafile);
        addExtendeddataFileArguments(extendeddatafile);        
        addSpecArguments(spec);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }

    /**
     * Lists the entities
     * @param realm - Realm where the cot is created     
     * @param spec - specification of metadata
     * @return int - command status
     * @throws java.lang.Exception
     */
    public int listEntities(String realm, String spec) throws Exception {
        setSubcommand(LIST_ENTITIES_SUBCOMMAND);
        addRealmArguments(realm);
        addSpecArguments(spec);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }

    /**
     * Deletes the entity
     * @param entityid - Name of the entity
     * @param realm - Realm where the cot is created
     * @param extendeddatafile - Location of extended data file
     * @param extendedonly - Flag to delete extended file only
     * @param spec - specification of metadata
     * @return int - command status
     * @throws java.lang.Exception
     */
    public int deleteEntity(String entityid, String realm,
            String extendeddatafile, String extendedonly,
            String spec) throws Exception {
        setSubcommand(DELETE_ENTITY_SUBCOMMAND);
        addEntityIDArguments(entityid);
        addRealmArguments(realm);
        if (extendedonly.equalsIgnoreCase("true")) {
            addExtendeddataFileArguments(extendeddatafile);
        }
        addSpecArguments(spec);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }

    /**
     * Adds the entity to the cot
     * @param cot - name of the circle of trust
     * @param entityid - Name of the entity
     * @param realm - Realm where the cot is created          
     * @param spec - specification of metadata
     * @return int - command status
     * @throws java.lang.Exception
     */
    public int addCotMember(String cot, String entityid, String realm,
            String spec) throws Exception {
        setSubcommand(ADD_CIRCLE_OF_TRUST_MEMBER_SUBCOMMAND);
        addCotArguments(cot);
        addEntityIDArguments(entityid);
        addRealmArguments(realm);
        addSpecArguments(spec);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }

    /**
     * Lists the members of cot
     * @param cot - name of the circle of trust     
     * @param realm - Realm where the cot is created
     * @param spec - specification of metadata
     * @return int - command status
     * @throws java.lang.Exception
     */
    public int listCotMembers(String cot, String realm,
            String spec) throws Exception {
        setSubcommand(LIST_CIRCLE_OF_TRUST_MEMBERS_SUBCOMMAND);
        addCotArguments(cot);        
        addRealmArguments(realm);
        addSpecArguments(spec);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }

    /**
     * Removes the entity from the cot
     * @param cot - name of the circle of trust
     * @param entityid - Name of the entity
     * @param realm - Realm where the cot is created
     * @param spec - specification of metadata
     * @return int - command status
     * @throws java.lang.Exception
     */
    public int removeCotMember(String cot, String entityid, String realm,
            String spec) throws Exception {
        setSubcommand(REMOVE_CIRCLE_OF_TRUST_MEMBER_SUBCOMMAND);
        addCotArguments(cot);
        addEntityIDArguments(entityid);
        addRealmArguments(realm);
        addSpecArguments(spec);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }

    /**
     * Creates circle of trust.
     * @param cot - Name of circle of trust.
     * @param realm - Name of realm.
     * @param prefix - Prefix URL for idp discovery reader and writer URL.
     * @return int - command status
     * @throws java.lang.Exception
     */
    public int createCot(String cot, String realm,
            String prefix) throws Exception {
        setSubcommand(CREATE_COT);
        addCotArguments(cot);
        addRealmArguments(realm);
        addPrefixArguments(prefix);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }

    /**
     * Lists circle of trust.
     * @param realm - Name of realm.
     * @return int - command status
     * @throws java.lang.Exception
     */
    public int listCots(String realm) throws Exception {
        setSubcommand(LIST_COTS);
        addRealmArguments(realm);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Deletes circle of trust.
     * 
     * @param cot - Name of circle of trust.
     * @param realm - Name of realm.
     * @return int - command status.
     * @throws java.lang.Exception
     */
    public int deleteCot(String cot, String realm) 
            throws Exception {
        setSubcommand(DELETE_COT);
        addCotArguments(cot);
        addRealmArguments(realm);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**     
     * @param map - Map of attributes with its values
     * @throws java.lang.Exception
     */
    public void createMetaTemplOptionalAttributes (Map map)
            throws Exception {

        // attributes Map
        Set set = map.keySet();
        Iterator iter = set.iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            String value = (String) map.get(key);

            // calling individual attribute function
            if (key.equals("--attrqueryprovider") || key.equals("-S")) {
                addAttrQueryProviderArguments(value);
            } else if (key.equals("--attrauthority") || key.equals("-I")) {
                addAttrAuthorityArguments(value);
            } else if (key.equals("--authnauthority") || key.equals("-C")) {
                addAuthNAuthorityArguments(value);
            } else if (key.equals("--xacmlpep") || key.equals("-e")) {
                addXacmlPEPArguments(value);
            } else if (key.equals("--xacmlpdp") || key.equals("-p")) {
                addXacmlPDPArguments(value);
            } else if (key.equals("--affiliation") || key.equals("-F")) {
                addAffiliationArguments(value);
            } else if (key.equals("--affiownerid") || key.equals("-N")) {
                addAffiOwnerIDArguments(value);
            } else if (key.equals("--affimembers") || key.equals("-M")) {
                addAffiMembersArguments(value);
            } else if (key.equals("--spscertalias") || key.equals("-a")) {
                addSPSCertAliasArguments(value);
            } else if (key.equals("--idpscertalias") || key.equals("-b")) {
                addIDPSCertAliasArguments(value);
            } else if (key.equals("--attrqscertalias") || key.equals("-A")) {
                addAttrQSCertAliasArguments(value);
            } else if (key.equals("--attrascertalias") || key.equals("-B")) {
                addAttrASCertAliasArguments(value);
            } else if (key.equals("--authnascertalias") || key.equals("-D")) {
                addAuthNASCertAliasArguments(value);
            } else if (key.equals("--affiscertalias") || key.equals("-J")) {
                addAffiSCertAliasArguments(value);
            } else if (key.equals("--xacmlpdpscertalias") || key.equals("-t")) {
                addXacmlPDPSCertAliasArguments(value);
            } else if (key.equals("--xacmlpepscertalias") || key.equals("-k")) {
                addXacmlPEPSCertAliasArguments(value);
            } else if (key.equals("--specertalias") || key.equals("-r")) {
                addSPECertAliasArguments(value);
            } else if (key.equals("--idpecertalias") || key.equals("-g")) {
                addIDPECertAliasArguments(value);
            } else if (key.equals("--attrqecertalias") || key.equals("-R")) {
                addAttrQECertAliasArguments(value);
            } else if (key.equals("--attraecertalias") || key.equals("-G")) {
                addAttrAECertAliasArguments(value);
            } else if (key.equals("--authnaecertalias") || key.equals("-E")) {
                addAuthNAECertAliasArguments(value);
            } else if (key.equals("--affiecertalias") || key.equals("-K")) {
                addAffiECertAliasArguments(value);
            } else if (key.equals("--xacmlpdpecertalias") || key.equals("-j")) {
                addXacmlPDPECertAliasArguments(value);
            } else if (key.equals("--xacmlpepecertalias") || key.equals("-z")) {
                addXacmlPEPECertAliasArguments(value);
            } else {

            } 
        }       
    }
    
    /**
     * Deletes the service with a given service name.
     * @param serviceName - Name of the service to be deleted.
     * @return the exit status of the "delete-svc" command
     */    
    public int deleteService(String serviceName) 
    throws Exception {
        setSubcommand(DELETE_SERVICE_SUBCOMMAND);
        addServiceNameArguments(serviceName);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }

    /**
     * Deletes one or more service(s) continuously. 
     * @param serviceName - Name of the service to be deleted
     * @param continueDeletingService - Flag to specify whether to delete 
     *                      services recursively.
     * @return int - Exit status of the command.
     * @throws java.lang.Exception
     */
    public int deleteService(String serviceName, boolean continueDeletingService,
            boolean deletePolicyRule) 
    throws Exception {
        setSubcommand(DELETE_SERVICE_SUBCOMMAND);
        addServiceNameArguments(serviceName);
        if (continueDeletingService) {
            addContinueArgument();
        }
        if(deletePolicyRule) {
            addDeletePolicyRule();
        }
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Get the absolute path to the ssoadm CLI.
     *
     * @return - the absolute path to ssoadm.
     */
    public String getCliPath() {
        return(new StringBuffer(cliPath).
                append(System.getProperty("file.separator")).append(uri).
                append(System.getProperty("file.separator")).append("bin").
                append(System.getProperty("file.separator")).append("ssoadm").
                toString());
    }
    
    /**
     * Sets the "--continue" argument in the argument list.
     */
    private void addContinueArgument() {
        String continueArg;
        if (useLongOptions) {
            continueArg = PREFIX_ARGUMENT_LONG + CONTINUE_ARGUMENT;
        } else {
            continueArg = PREFIX_ARGUMENT_SHORT + SHORT_CONTINUE_ARGUMENT;
        }
        addArgument(continueArg);
    }
     
    /**
     * Sets the "--deletepolicyrule" argument in the argument list.
     */
    private void addDeletePolicyRule() {
        String deletePolicyRule;
        if (useLongOptions) {
            deletePolicyRule = PREFIX_ARGUMENT_LONG + DELETE_POLICY_RULE;
        } else {
            deletePolicyRule = PREFIX_ARGUMENT_SHORT + SHORT_DELETE_POLICY_RULE;
        }
        addArgument(deletePolicyRule);
    }
    
    /**
     * Sets the "--adminid" and admin user ID arguments in the argument list.
     */
    private void addAdminUserArgs() {
        String adminArg;
        if (useLongOptions) {
            adminArg = PREFIX_ARGUMENT_LONG + ARGUMENT_ADMIN_ID;
        } else {
            adminArg = PREFIX_ARGUMENT_SHORT + SHORT_ARGUMENT_ADMIN_ID;
        }
        setArgument(ADMIN_ID_ARG_INDEX, adminArg);
        setArgument(ADMIN_ID_VALUE_INDEX, adminUser);
    }
    
    /**
     * Sets "--passwordfile" and the password file path in the argument list.
     */
    private void addPasswordArgs() {
        String passwordArg;
        
        if (useLongOptions) {
            passwordArg = PREFIX_ARGUMENT_LONG + ARGUMENT_PASSWORD_FILE;
        } else {
            passwordArg = PREFIX_ARGUMENT_SHORT + SHORT_ARGUMENT_PASSWORD_FILE;
        }
        
        setArgument(PASSWORD_ARG_INDEX, passwordArg);
        setArgument(PASSWORD_VALUE_INDEX, passwdFile);
    }
    
    /**
     * Adds the "--debug" argument to the argument list.
     */
    private void addDebugArg() {
        String debugArg;
        if (useLongOptions) {
            debugArg = PREFIX_ARGUMENT_LONG + DEBUG_ARGUMENT;
        } else {
            debugArg = PREFIX_ARGUMENT_SHORT + SHORT_DEBUG_ARGUMENT;
        }
        addArgument(debugArg);
    }
    
    /**
     * Adds the "--verbose" argument to the arugment list.
     */
    private void addVerboseArg() {
        String verboseArg;
        if (useLongOptions) {
            verboseArg = PREFIX_ARGUMENT_LONG + VERBOSE_ARGUMENT;
        } else {
            verboseArg = PREFIX_ARGUMENT_SHORT + SHORT_VERBOSE_ARGUMENT;
        }
        addArgument(verboseArg);
    }
    
    /**
     * Adds the "--locale" arugment and the locale value to the argument list.
     */
    private void addLocaleArgs() {
        String localeArg;
        if (useLongOptions) {
            localeArg = PREFIX_ARGUMENT_LONG + LOCALE_ARGUMENT;
        } else {
            localeArg = PREFIX_ARGUMENT_SHORT + SHORT_LOCALE_ARGUMENT;
        }
        addArgument(localeArg);
        addArgument(localeValue);
    }
    
    /**
     * Adds the global arguments (--debug, --verbose, --locale) to the argument
     * list if they are specified.
     */
    private void addGlobalOptions() {
        if (useDebugOption) {
            addDebugArg();
        }
        if (useVerboseOption) {
            addVerboseArg();
        }
        if (localeValue != null) {
            addLocaleArgs();
        }
    }
    
    /**
     * Adds the "--realm" argument and realm value to the argument list
     * @param realm - the realm value to add to the argument list
     */
    private void addRealmArguments(String realm) {
        String realmArg;
        if (useLongOptions) {
            realmArg = PREFIX_ARGUMENT_LONG + REALM_ARGUMENT;
        } else {
            realmArg = PREFIX_ARGUMENT_SHORT + SHORT_REALM_ARGUMENT;
        }
        addArgument(realmArg);
        addArgument(realm);
    }
    
    /**
     * Adds the "--recursive" argument to the argument list.
     */
    private void addRecursiveArgument() {
        String recursiveArg;
        if (useLongOptions) {
            recursiveArg = PREFIX_ARGUMENT_LONG + RECURSIVE_ARGUMENT;
        } else {
            recursiveArg = PREFIX_ARGUMENT_SHORT + SHORT_RECURSIVE_ARGUMENT;
        }
        addArgument(recursiveArg);
    }
    
    /**
     * Adds the "--filter" argument to the argument list.
     */
    private void addFilterArguments(String filter) {
        String filterArg;
        if (useLongOptions) {
            filterArg = PREFIX_ARGUMENT_LONG + FILTER_ARGUMENT;
        } else {
            filterArg = PREFIX_ARGUMENT_SHORT + SHORT_FILTER_ARGUMENT;
        }
        addArgument(filterArg);
        addArgument(filter);
    }
    
    /**
     * Adds the "--idname" and identity name arguments to the argument list.
     */
    private void addIdnameArguments(String name) {
        String idnameArg;
        if (useLongOptions) {
            idnameArg = PREFIX_ARGUMENT_LONG + ID_NAME_ARGUMENT;
        } else {
            idnameArg = PREFIX_ARGUMENT_SHORT + SHORT_ID_NAME_ARGUMENT;
        }
        addArgument(idnameArg);
        addArgument(name);
    }
    
    /**
     * Adds the "--idtype" and identity type arguments to the argument list.
     */
    private void addIdtypeArguments(String type) {
        String idtypeArg;
        if (useLongOptions) {
            idtypeArg = PREFIX_ARGUMENT_LONG + ID_TYPE_ARGUMENT;
        } else {
            idtypeArg = PREFIX_ARGUMENT_SHORT + SHORT_ID_TYPE_ARGUMENT;
        }
        addArgument(idtypeArg);
        addArgument(type);
    }
    
    /**
     * Adds the "--memberidname" and member identity name arguments to the 
     * argument list.
     */
    private void addMemberIdnameArguments(String name) {
        String idnameArg;
        if (useLongOptions) {
            idnameArg = PREFIX_ARGUMENT_LONG + MEMBER_ID_NAME_ARGUMENT;
        } else {
            idnameArg = PREFIX_ARGUMENT_SHORT + SHORT_MEMBER_ID_NAME_ARGUMENT;
        }
        addArgument(idnameArg);
        addArgument(name);        
    }
    
    /**
     * Adds the "--memberidtype" and member identity type arguments to the 
     * argument list.
     */
    private void addMemberIdtypeArguments(String type) {
        String idtypeArg;
        if (useLongOptions) {
            idtypeArg = PREFIX_ARGUMENT_LONG + MEMBER_ID_TYPE_ARGUMENT;
        } else {
            idtypeArg = PREFIX_ARGUMENT_SHORT + SHORT_MEMBER_ID_TYPE_ARGUMENT;
        }
        addArgument(idtypeArg);
        addArgument(type);        
    }
    
    /**
     * Adds the "--membershipidtype" and membership identity type arguments to 
     * the argument list.
     *
     * @param type - the identity type for the member
     */
    private void addMembershipIdtypeArguments(String type) {
        String idtypeArg;
        if (useLongOptions) {
            idtypeArg = PREFIX_ARGUMENT_LONG + MEMBERSHIP_ID_TYPE_ARGUMENT;
        } else {
            idtypeArg = PREFIX_ARGUMENT_SHORT + 
                    SHORT_MEMBERSHIP_ID_TYPE_ARGUMENT;
        }
        addArgument(idtypeArg);
        addArgument(type);        
    }  
    
    /**
     * Adds the "--servicename" and the servicename arguments to the argument 
     * list.
     *
     * @param name - the name of the service
     */
    private void addServiceNameArguments(String names) {
        String serviceNameArg;
        if (useLongOptions) {
            serviceNameArg = PREFIX_ARGUMENT_LONG + SERVICENAME_ARGUMENT;
        } else {
            serviceNameArg = PREFIX_ARGUMENT_SHORT + SHORT_SERVICENAME_ARGUMENT;
        }
        addArgument(serviceNameArg);
        String[] name = names.split(",");
        for (int i=0; i < name.length; i++) {
            addArgument(name[i]);
        }
    }
    
    /**  
     * Add the "--attributename" and the attribute name arguments to the 
     * argument list.
     *
     * @param name - the attribute name
     */
    private void addAttributeNameArguments(String name) {
        String attrNameArg;
        if (useLongOptions) {
            attrNameArg = PREFIX_ARGUMENT_LONG + ATTRIBUTE_NAME_ARGUMENT;
        } else {
            attrNameArg = PREFIX_ARGUMENT_SHORT + 
                    SHORT_ATTRIBUTE_NAMES_ARGUMENT;
        }
        addArgument(attrNameArg);
        addArgument(name);
    }
    
    /**
     * Add the "--authtype" and the authentication type arguments to the 
     * argument list.
     *
     * @param type - the authentication type
     */
    private void addAuthtypeArguments(String type) {
        String authTypeArg;
        if (useLongOptions) {
            authTypeArg = PREFIX_ARGUMENT_LONG + AUTHTYPE_ARGUMENT;
        } else {
            authTypeArg = PREFIX_ARGUMENT_SHORT + SHORT_AUTHTYPE_ARGUMENT;
        }
        addArgument(authTypeArg);
        addArgument(type);
    }
    
    
    /**  
     * Add the "--attributenames" and the attribute name arguments to the 
     * argument list.
     *
     * @param names - a semi-colon delimited list of attribute names
     */
    private void addAttributeNamesArguments(String names) {
        String attrNameArg;
        if (useLongOptions) {
            attrNameArg = PREFIX_ARGUMENT_LONG + ATTRIBUTE_NAMES_ARGUMENT;
        } else {
            attrNameArg = PREFIX_ARGUMENT_SHORT + 
                    SHORT_ATTRIBUTE_NAMES_ARGUMENT;
        }
        addArgument(attrNameArg);
        String[] nameList = names.split(";");
        for (int i=0; i < nameList.length; i++) {
            addArgument(nameList[i]);
        }
    }
 
    /**
     * Add the "--name" and the auth instance name arguments to the argument
     * list.
     *
     * @param name - the auth instance name
     */
    private void addNameArguments(String name) {
        String nameArg;
        if (useLongOptions) {
            nameArg = PREFIX_ARGUMENT_LONG + NAME_ARGUMENT;
        } else {
            nameArg = PREFIX_ARGUMENT_SHORT + SHORT_NAMES_ARGUMENT;
        }
        addArgument(nameArg);
        addArgument(name);
    }    
    
    /**
     * Add the "--names" and the auth instance name arguments to the argument
     * list.
     *
     * @param names - a semi-colon delimited list of auth instance names
     */
    private void addNamesArguments(String names) {
        String namesArg;
        if (useLongOptions) {
            namesArg = PREFIX_ARGUMENT_LONG + NAMES_ARGUMENT;
        } else {
            namesArg = PREFIX_ARGUMENT_SHORT + SHORT_NAMES_ARGUMENT;
        }
        addArgument(namesArg);
        String[] nameList = names.split(";");
        for (String name: nameList) {
            addArgument(name);
        }
    }
    
    /**
     * Add the "--append" argument to the argument list.
     */
    private void addAppendArgument() {
        String appendArg;
        if (useLongOptions) {
            appendArg = PREFIX_ARGUMENT_LONG + APPEND_ARGUMENT;
        } else {
            appendArg = PREFIX_ARGUMENT_SHORT + SHORT_APPEND_ARGUMENT;
        }
        addArgument(appendArg);           
    }
    
    /**
     * Add the  --revisionnumber and revision number arguments to the 
     * argument list.
     */
   private void addRevisionNumberArgument(String revisionNumber) {
       String revArg;
       if(useLongOptions) {
           revArg = PREFIX_ARGUMENT_LONG + REVISION_NO_ARGUMENT;
       } else {
           revArg = PREFIX_ARGUMENT_SHORT + SHORT_REVISION_NO_ARGUMENT;
       }
       addArgument(revArg);
       addArgument(revisionNumber);
   }
   
    /** 
     * Adds the "--type" argument and type value to the argument list
     * @param agentType - the agent type value to add to the argument list
     */
    private void addAgentTypeArguments(String agentType) {
        String agentTypeArg;

        if (useLongOptions) {
            agentTypeArg = PREFIX_ARGUMENT_LONG + AGENTTYPE_ARGUMENT;
        } else {
            agentTypeArg = PREFIX_ARGUMENT_SHORT + SHORT_AGENTTYPE_ARGUMENT;
        }
        addArgument(agentTypeArg);
        addArgument(agentType);
    }

    /**
     * Adds the "--agentname" or (-b) and agent name arguments to the argument 
     * list.
     * @param name - the name of the agent to add to the argument list
     */
    private void addAgentNameArguments(String name) {
        String agentnameArg;
        if (useLongOptions) {
            agentnameArg = PREFIX_ARGUMENT_LONG + AGENTNAME_ARGUMENT;
        } else {
            agentnameArg = PREFIX_ARGUMENT_SHORT + SHORT_AGENTNAME_ARGUMENT;
        }
        addArgument(agentnameArg);
        addArgument(name);
    }
    
    /**
     * Add the "--agentnames" argument and value to the argument list
     * @param name - the names of the agent to add to the argument list
     */
    private void addAgentNamesArguments(String names) {
        String agentNamesArg;
        if (useLongOptions) {
            agentNamesArg = PREFIX_ARGUMENT_LONG + AGENTNAMES_ARGUMENT;
        } else {
            agentNamesArg = PREFIX_ARGUMENT_SHORT + SHORT_AGENTNAMES_ARGUMENT;
        }
        addArgument(agentNamesArg);
        StringTokenizer tokenizer = new StringTokenizer(names);
        while (tokenizer.hasMoreTokens()) {
            addArgument(tokenizer.nextToken());
        }
    }
    
    /**
     * Create a new realm.
     * 
     * @param realmToCreate - the name of the realm to be created
     * @return the exit status of the "create-realm" command
     */
    public int createRealm(String realmToCreate) 
    throws Exception {
        setSubcommand(CREATE_REALM_SUBCOMMAND);
        addRealmArguments(realmToCreate);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Get revision number.
     * 
     * @param serviceName - the name of the service.
     * @return the exit status of the "get-revision-number" command
     */
    public int getRevisionNumber(String serviceName)
    throws Exception {
        setSubcommand(GET_REVISION_NUMBER_SUBCOMMAND);
        addServiceNameArguments(serviceName);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Set revision number.
     * 
     * @param serviceName - the name of the service.
     * @return the exit status of the "set-revision-number" command
     */
    public int setRevisionNumber(String serviceName, String revisionNumber)
    throws Exception {
        setSubcommand(SET_REVISION_NUMBER_SUBCOMMAND);
        addServiceNameArguments(serviceName);
        addRevisionNumberArgument(revisionNumber);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Create multiple realms from a list of realms.
     *
     * @param realmList - a list of realms to create separated by semi-colons.
     * @return a boolean indicating whether all the realms are created 
     * successfully.
     */
    public boolean createRealms(String realmList)
    throws Exception {
        boolean allRealmsCreated = true;
        List commandList = new ArrayList();
        int exitStatus = -1;
        Map argMap = new HashMap();
        List argList = new ArrayList();
        if (realmList != null) {
            if (realmList.length() > 0) {
                String[] realms = realmList.split(";");

                //Create single realm without "do-batch"
                if (realms.length == 1) {
                    exitStatus = createRealm(realms[0]);
                    logCommand("createRealms");
                    resetArgList();
                } else {
                    for (int i = 0; i < realms.length; i++) {
                        argMap.put(PREFIX_ARGUMENT_LONG + REALM_ARGUMENT,
                                realms[i]);
                        argList.add(argMap);
                        commandList.add(createBatchCommand(
                                CREATE_REALM_SUBCOMMAND, argList));
                        argList.clear();
                        argMap.clear();
                    }
                    String attFile = createBatchfile(commandList);
                    exitStatus = doBatch(attFile, "", false);
                    logCommand("createRealms");
                    resetArgList();
                }
                if (exitStatus != SUCCESS_STATUS) {
                    allRealmsCreated = false;
                    log(Level.SEVERE, "createRealms", "The ssoadm do-batch " +
                            "command failed to create realms.");
                }
            } else {
                allRealmsCreated = false;
                log(Level.SEVERE, "createRealms",
                        "The list of realms is empty.");
            }
        } else {
            allRealmsCreated = false;
            log(Level.SEVERE, "createRealms", "The list of realms is null.");
        }
        return (allRealmsCreated);
    }
       
    /**
     * Delete a realm.
     *
     * @param realmToDelete - the name of the realm to be deleted
     * @param recursiveDelete - a flag indicating whether the realms beneath 
     * realmToDelete should be recursively deleted as well
     * @return the exit status of the "delete-realm" command
     */
    public int deleteRealm(String realmToDelete, boolean recursiveDelete) 
    throws Exception {
        setSubcommand(DELETE_REALM_SUBCOMMAND);
        addRealmArguments(realmToDelete);
        if (recursiveDelete) {
            addRecursiveArgument();
        }
        addGlobalOptions();
        return (executeCommand(commandTimeout)); 
    }
    
    /**
     * Delete a realm without using recursion.
     * @param realmToDelete - the name of the realm to be deleted.
     * @return the exit status of the "delete-realm" command
     */
    public int deleteRealm(String realmToDelete) 
    throws Exception {
        return (deleteRealm(realmToDelete, false));
    }
    
    /**
     * List the realms which exist under a realm.
     *
     * @param startRealm - the realm from which to start the search
     * @param filter - a string containing a filter which will be used to 
     * restrict the realms that are returned (e.g. "*realms")
     * @param recursiveSearch - a boolean which should be set to "false" to 
     * perform a single level search or "true" to perform a recursive search
     * @return the exit status of the "list-realms" command
     */
    public int listRealms(String startRealm, String filter, 
            boolean recursiveSearch) 
    throws Exception {
        setSubcommand(LIST_REALMS_SUBCOMMAND);
        addRealmArguments(startRealm);        
        if (filter != null) {
            addFilterArguments(filter);
        }
        if (recursiveSearch) {
            addRecursiveArgument();
        }
        addGlobalOptions();
        return (executeCommand(commandTimeout)); 
    }
    
    /**
     * Perform a listing of realms with no filter
     * @param startRealm - the realm from which to start the search
     * @param recursiveSearch - a boolean which should be set to "false" to 
     * perform a single level search or "true" to perform a recursive search
     * @return the exit status of the "list-realms" command
     */
    public int listRealms (String startRealm, boolean recursiveSearch) 
    throws Exception {
        return listRealms(startRealm, null, recursiveSearch);
    }
    
    /**
     * Perform a non-recursive listing of realms with no filter
     * @param startRealm - the realm from which to start the search
     * @return the exit status of the "list-realms" command
     */
    public int listRealms (String startRealm) 
    throws Exception {
        return listRealms(startRealm, null, false);
    }
    
    /**
     * Create an identity in a realm
     * @param realm - the realm in which to create the identity
     * @param name - the name of the identity to be created
     * @param type - the type of identity to be created (e.g. "User", "Role", 
     * and "Group")
     * @param attributeValues - a string containing the attribute values for the 
     * identity to be created
     * @param useAttributeValues - a boolean flag indicating whether the 
     * "--attributevalues" option should be used 
     * @param useDatafile - a boolean flag indicating whether the attribute 
     * values should be written to a file and passed to the CLI using the 
     * "--datafile <file-path>" arguments
     * @return the exit status of the "create-identity" command
     */
    public int createIdentity(String realm, String name, String type, 
            List attributeValues, boolean useAttributeValues, 
            boolean useDatafile) 
    throws Exception {
        setSubcommand(CREATE_IDENTITY_SUBCOMMAND);
        addRealmArguments(realm);
        addIdnameArguments(name);
        addIdtypeArguments(type);
        
        if (attributeValues != null) {
            if (useAttributeValues) {
                addAttributevaluesArguments(attributeValues);
            }
            if (useDatafile) {
                addDatafileArguments(attributeValues, "attrValues", ".txt");
            }
        }
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }       
    
    /**
     * Create an identity in a realm
     * @param realm - the realm in which to create the identity
     * @param name - the name of the identity to be created
     * @param type - the type of identity to be created (e.g. "User", "Role", 
     * and "Group")
     * @param attributeValues - a string containing the attribute values for the 
     * identity to be created
     * @param useAttributeValues - a boolean flag indicating whether the 
     * "--attributevalues" option should be used 
     * @param useDatafile - a boolean flag indicating whether the attribute 
     * values should be written to a file and passed to the CLI using the 
     * "--datafile <file-path>" arguments
     * @return the exit status of the "create-identity" command
     */
    public int createIdentity(String realm, String name, String type, 
            String attributeValues, boolean useAttributeValues, 
            boolean useDatafile) 
    throws Exception {
        setSubcommand(CREATE_IDENTITY_SUBCOMMAND);
        addRealmArguments(realm);
        addIdnameArguments(name);
        addIdtypeArguments(type);
        
        if (attributeValues != null) {
            
            if (useAttributeValues) {
                addAttributevaluesArguments(attributeValues);
            } 
            if (useDatafile) {
                addDatafileArguments(attributeValues, "attrValues", ".txt");
            }
        }
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Create an identity in a realm using the "--attribute-values" argument
     * @param realm - the realm in which to create the identity
     * @param name - the name of the identity to be created
     * @param type - the type of identity to be created (e.g. "User", "Role", 
     * and "Group")
     * @param attributeValues - a semi-colon delimited string containing the 
     * attribute values for the identity to be created
     * @return the exit status of the "create-identity" command
     */
    public int createIdentity(String realm, String name, String type, 
            String attributeValues)
    throws Exception {
        return (createIdentity(realm, name, type, attributeValues, true, 
                false));
    }
    
    /**
     * Create an identity in a realm using the "--attribute-values" argument
     * @param realm - the realm in which to create the identity
     * @param name - the name of the identity to be created
     * @param type - the type of identity to be created (e.g. "User", "Role", 
     * and "Group")
     * @return the exit status of the "create-identity" command
     */
    public int createIdentity(String realm, String name, String type)
    throws Exception {
        String emptyString = null;
        return (createIdentity(realm, name, type, emptyString, false, false));
    }
    
    /**
     * Create multiple identities from a list of identities.
     *
     * @param idList - a list of identities to create separated by semi-colons.
     * @return a boolean indicating whether all the realms are created 
     * successfully.
     */
    public boolean createIdentities(String idList)
    throws Exception {
        boolean allIdsCreated = true;
        List commandsList = new ArrayList();
        int exitStatus = -1;

        if (idList != null) {
            if (idList.length() > 0) {
                String[] ids = idList.split("\\|");

                //Create single identity without "do-batch"
                if (ids.length == 1) {
                    String[] idArgs = ids[0].split("\\,");
                    if (idArgs.length >= 3) {
                        String idRealm = idArgs[0];
                        String idName = idArgs[1];
                        String idType = idArgs[2];
                        log(Level.FINEST, "createIdentities", "Realm for id: " +
                                idRealm);
                        log(Level.FINEST, "createIdentities", "Name for id: " +
                                idName);
                        log(Level.FINEST, "createIdentities", "Type for id: " +
                                idType);
                        exitStatus = createIdentity(idRealm, idName, idType);

                        if (idArgs.length > 3) {
                            String idAttributes = idArgs[3];
                            exitStatus = createIdentity(idRealm, idName,
                                    idType, idAttributes);
                        }
                        logCommand("createIdentities");
                        resetArgList();
                    }
                } else {
                    for (int i = 0; i < ids.length; i++) {
                        log(Level.FINE, "createIdentities", "Creating id " +
                                ids[i]);
                        String[] idArgs = ids[i].split("\\,");
                        if (idArgs.length >= 3) {
                            String idRealm = idArgs[0];
                            String idName = idArgs[1];
                            String idType = idArgs[2];
                            log(Level.FINEST, "createIdentities",
                                    "Realm for id: " + idRealm);
                            log(Level.FINEST, "createIdentities",
                                    "Name for id: " + idName);
                            log(Level.FINEST, "createIdentities",
                                    "Type for id: " + idType);
                            List argNameValueList = new ArrayList();
                            Map argMap = new HashMap();
                            argMap.put(PREFIX_ARGUMENT_LONG + REALM_ARGUMENT,
                                    idRealm);
                            argMap.put(PREFIX_ARGUMENT_LONG + ID_NAME_ARGUMENT,
                                    idName);
                            argMap.put(PREFIX_ARGUMENT_LONG + ID_TYPE_ARGUMENT,
                                    idType);
                            if (idArgs.length > 3) {
                                String idAttributes = null;
                                idAttributes = idArgs[3];
                                idAttributes = idAttributes.replace(";", " ");
                                argMap.put(PREFIX_ARGUMENT_LONG +
                                        ATTRIBUTE_VALUES_ARGUMENT,
                                        idAttributes);
                            }
                            argNameValueList.add(argMap);
                            commandsList.add(createBatchCommand(
                                    CREATE_IDENTITY_SUBCOMMAND,
                                    argNameValueList));
                        } else {
                            allIdsCreated = false;
                            log(Level.SEVERE, "createIdentities",
                                    "The identity " + ids[i] + " must have a " +
                                    "realm, an identity name, and an " +
                                    "identity type");
                        }
                    }
                    String batchFile = createBatchfile(commandsList);
                    exitStatus = doBatch(batchFile, "", false);
                    logCommand("createIdentities");
                    resetArgList();
                }
                if (exitStatus != SUCCESS_STATUS) {
                    allIdsCreated = false;
                    log(Level.SEVERE, "createIdentities",
                            "The creation of identities in batch failed with " +
                            "exit status: " + exitStatus + ".");
                }
            } else {
                allIdsCreated = false;
                log(Level.SEVERE, "createIdentities",
                        "The identity list is empty.");
            }
        } else {
            allIdsCreated = false;
            log(Level.SEVERE, "createIdentities", "The identity list is null.");
        }
        return(allIdsCreated);
    }
    
    /**
     * Delete one or more identities in a realm
     * @param realm - the realm from which the identies should be deleted
     * @param names - one or more identity names to be deleted
     * @param type - the type of the identity (identities) to be deleted
     * @return the exit status of the "delete-identities" command
     */
    public int deleteIdentities(String realm, String names, String type)
    throws Exception {
        setSubcommand(DELETE_IDENTITIES_SUBCOMMAND);
        addRealmArguments(realm);
        addIdnamesArguments(names);
        addIdtypeArguments(type);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * List the identities in a particular realm
     * @param realm - the realm in which to start the search for identities
     * @param filter - the filter to apply in the search for identities
     * @param idtype - the type of identities (e.g. "User", "Group", "Role") for
     * which the search sould be performed
     * @return the exit status of the "list-identities" command
     */
    public int listIdentities(String realm, String filter, String idtype)
    throws Exception {
        setSubcommand(LIST_IDENTITIES_SUBCOMMAND);
        addRealmArguments(realm);
        addFilterArguments(filter);
        addIdtypeArguments(idtype);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Get the attributes of an identity.
     *
     * @param realm - the realm in which the identity exists.
     * @param idName - the name of the identity for which the attributes should 
     * be retrieved.
     * @param idType - the type of the identity for which the attributes should 
     * be retrieved.
     * @param attributeNames - the name or names of the attributes that should 
     * be retrieved.
     * @return the exit status of the "get-identity" command.
     */
    public int getIdentity(String realm, String idName, String idType, 
            String attributeNames)
    throws Exception {
        setSubcommand(GET_IDENTITY_SUBCOMMAND);
        addRealmArguments(realm);
        addIdnameArguments(idName);
        addIdtypeArguments(idType);
        if (attributeNames.length() > 0) {
            addAttributeNamesArguments(attributeNames);
        }
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
            
    /**
     * Add an identity as a member of another identity.
     * @param realm - the realm in which the member identity should be added.
     * @param memberName - the name of the identity which should be added as a
     * member.
     * @param memberType - the type of the identity which should be added as a 
     * member.
     * @param idName - the name of the identity in which the member should be 
     * added.
     * @param idType - the type of the identity in which the member should be 
     * added.
     * @return the exit status of the "add-member" command
     */
    public int addMember(String realm, String memberName, String memberType, 
            String idName, String idType)
    throws Exception {
        setSubcommand(ADD_MEMBER_SUBCOMMAND);
        addRealmArguments(realm);
        addMemberIdnameArguments(memberName);
        addMemberIdtypeArguments(memberType);
        addIdnameArguments(idName);
        addIdtypeArguments(idType);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Remove an identity as a member of another identity.
     * @param realm - the realm in which the member identity should be removed.
     * @param memberName - the name of the identity which should be removed as a
     * member.
     * @param memberType - the type of the identity which should be removed as a 
     * member.
     * @param idName - the name of the identity in which the member should be 
     * removed.
     * @param idType - the type of the identity in which the member should be 
     * removed.
     * @return the exit status of the "remove-member" command
     */
    public int removeMember(String realm, String memberName, String memberType, 
            String idName, String idType)
    throws Exception {
        setSubcommand(REMOVE_MEMBER_SUBCOMMAND);
        addRealmArguments(realm);
        addMemberIdnameArguments(memberName);
        addMemberIdtypeArguments(memberType);
        addIdnameArguments(idName);
        addIdtypeArguments(idType);
        addGlobalOptions();
        return (executeCommand(commandTimeout));       
    }
    
    /**
     * Display the member identities of an identity.
     * @param realm - the realm in which the identity exists.
     * @param membershipType - the identity type of the member which should be
     * displayed.
     * @param idName - the name of the identity for which the members should be 
     * displayed.
     * @param idType - the type of the identity for which the members should be 
     * displayed.
     * @return the exit status of the "show-members" command.
     */
    public int showMembers(String realm, String membershipType, String idName, 
            String idType)
    throws Exception {
        setSubcommand(SHOW_MEMBERS_SUBCOMMAND);
        addRealmArguments(realm);
        addMembershipIdtypeArguments(membershipType);
        addIdnameArguments(idName);
        addIdtypeArguments(idType);
        addGlobalOptions();
        return (executeCommand(commandTimeout));            
    } 
    
    /**
     * Display the identities of which an identity is a member.
     * @param realm - the realm in which the identity exists.
     * @param membershipType - the type of membership which should be displayed.
     * @param idName - the identity name for which the memberships should be 
     * displayed.
     * @param idType - the identity type for which the memberships should be 
     * displayed.
     * @return the exit status of the "show-memberships" command.
     */
    public int showMemberships(String realm, String membershipType, 
            String idName, String idType)
    throws Exception {
        setSubcommand(SHOW_MEMBERSHIPS_SUBCOMMAND);
        addRealmArguments(realm);
        addMembershipIdtypeArguments(membershipType);
        addIdnameArguments(idName);
        addIdtypeArguments(idType);
        addGlobalOptions();
        return (executeCommand(commandTimeout));             
    }
    
    /**
     * Add an attribute to a realm.
     * @param realm - the realm in which the identity exists.
     * @param serviceName - the name of the service in which to add the 
     * attribute.
     * @param attributeValues - a semi-colon delimited string containing the 
     * attribute values for the identity to be created.
     * @param useDatafile - a boolean indicating whether a datafile should be 
     * used.  If true, a datafile will be created and the "--datafile" argument
     * will be used.  If false, the "--attributevalues" argument and a list of 
     * attribute name/value pairs will be used.
     * @return the exit status of the "add-realm-attributes" command.
     */
    public int addRealmAttributes(String realm, String serviceName, 
            String attributeValues, boolean useDatafile)
    throws Exception {
        setSubcommand(SET_REALM_ATTRIBUTES_SUBCOMMAND);
        addRealmArguments(realm);
        addServiceNameArguments(serviceName);
        addAppendArgument();
        
        if (!useDatafile) {
            addAttributevaluesArguments(attributeValues);
        } else {
            addDatafileArguments(attributeValues, "attr", ".txt");
        }
    
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Delete an attribute from a realm.
     *
     * @param realm - the realm in which the identity exists.
     * @param serviceName - the name of the service in which to add the 
     * attribute.
     * @param attributeName - the name of the attribute to be deleted.
     * @return the exit status of the "delete-realm-attribute" command.
     */
    public int deleteRealmAttribute(String realm, String serviceName, 
            String attributeName)
    throws Exception {
        setSubcommand(DELETE_REALM_ATTRIBUTE_SUBCOMMAND);
        addRealmArguments(realm);
        addServiceNameArguments(serviceName);
        addAttributeNameArguments(attributeName);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Set an attribute in a realm.
     * @param realm - the realm in which the identity exists.
     * @param serviceName - the name of the service in which to add the 
     * attribute.
     * @param attributeValues - a semi-colon delimited string containing the 
     * attribute values for the identity to be created.
     * @param useDatafile - a boolean indicating whether a datafile should be 
     * used.  If true, a datafile will be created and the "--datafile" argument
     * will be used.  If false, the "--attributevalues" argument and a list of 
     * attribute name/value pairs will be used.
     * @return the exit status of the "set-realm-attributes" command.
     */
    public int setRealmAttributes(String realm, String serviceName, 
            String attributeValues, boolean useDatafile)
    throws Exception {
        setSubcommand(SET_REALM_ATTRIBUTES_SUBCOMMAND);
        addRealmArguments(realm);
        addServiceNameArguments(serviceName); 
        
        if (!useDatafile) {
            addAttributevaluesArguments(attributeValues);
        } else {
            addDatafileArguments(attributeValues, "attr", ".txt");
        }
    
        addGlobalOptions();
        return (executeCommand(commandTimeout));        
    }
    
    /**
     * Retrive the attributes of a realm.
     *
     * @param realm - the realm in which the identity exists.
     * @param serviceName - the name of the service in which to add the 
     * attribute.
     * @return the exit status of the "get-realm" command.
     */
    public int getRealm(String realm, String serviceName) 
    throws Exception {
        setSubcommand(GET_REALM_SUBCOMMAND);
        addRealmArguments(realm);
        addServiceNameArguments(serviceName);
        
        addGlobalOptions();
        return (executeCommand(commandTimeout));         
    }
    
    /**
     * Create an authentication instance.
     * 
     * @param realm - the realm in which the authentication instance should be 
     * created.
     * @param name - the name of the authentication instance to be created.
     * @param type - the type of the authentication instance to be created.
     * @return the exit status of the "create-auth-instance" command.
     */
    public int createAuthInstance(String realm, String name, String type)
    throws Exception {
        setSubcommand(CREATE_AUTH_INSTANCE_SUBCOMMAND);
        addRealmArguments(realm);
        addNameArguments(name);
        addAuthtypeArguments(type);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Create multiple auth instances from a list of instances.
     *
     * @param instanceList - a list of instances to create separated by pipes 
     * ('|') in the following format 
     * (<realm1>,<auth-instance-name1>,<auth-instance-type1>|
     * <realm2>,<auth-instance-name2>,<auth-instance-type2>)
     * @return a boolean indicating whether all the instances are created 
     * successfully.
     */
    public boolean createAuthInstances(String instanceList)
    throws Exception {
        boolean allInstancesCreated = true;
        
        if (instanceList != null) {
            if (instanceList.length() > 0) {
                String [] instances = instanceList.split("\\|");
                for (String instance: instances) {
                    String[] instanceInfo = instance.split(",");
                    if (instanceInfo.length == 3) {
                        String authRealm = instanceInfo[0];
                        String instanceName = instanceInfo[1];
                        String authType = instanceInfo[2];
                        log(Level.FINE, "createAuthInstances", "Creating auth " + 
                                "instance " + instance);
                        int exitStatus = createAuthInstance(authRealm, 
                                instanceName, authType);
                        logCommand("createAuthInstances");
                        resetArgList();
                        if (exitStatus != SUCCESS_STATUS) {
                            allInstancesCreated = false;
                            log(Level.SEVERE, "createAuthInstances", 
                                    "The instance " + instance + 
                                    " failed to be created.");
                        }
                    } else {
                        allInstancesCreated = false;
                        log(Level.SEVERE, "createAuthInstances", 
                                "The instance to be created must contain a " + 
                                "realm, an instance name, and an instance " +
                                "type.");
                    }
                }
            } else {
                allInstancesCreated = false;
                log(Level.SEVERE, "createAuthInstances", 
                        "The list of instances is empty.");
            }
        } else {
            allInstancesCreated = false;
            log(Level.SEVERE, "createAuthInstances", 
                    "The list of instances is null.");
        }
        return (allInstancesCreated);
    }
    
    /**
     * Delete one or more authentication instances.
     *
     * @param realm - the realm in which the authentication instance should be
     * deleted.
     * @param names - the name(s) of the authentication instance(s) to be
     * deleted.
     * @return the exit status of the "delete-auth-instance" command.
     */
    public int deleteAuthInstances(String realm, String names)
    throws Exception {
        setSubcommand(DELETE_AUTH_INSTANCES_SUBCOMMAND);
        addRealmArguments(realm);
        addNamesArguments(names);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Get the attribute values for an authentication instance.
     * 
     * @param realm - the realm in which the authentication instance exists.
     * @param name - the name of the authentication instance for which the 
     * instance should be retrieved.
     * @return the exit status of the "get-auth-instance" command.
     */
    public int getAuthInstance(String realm, String name) 
    throws Exception {
        setSubcommand(GET_AUTH_INSTANCE_SUBCOMMAND);
        addRealmArguments(realm);
        addNameArguments(name);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * List the authentication instances for a realm.
     *
     * @param realm - the realm in which the authentication instances should be
     * displayed.
     * @return the exit status of the "list-auth-instances" command.
     */
    public int listAuthInstances(String realm)
    throws Exception {
        setSubcommand(LIST_AUTH_INSTANCES_SUBCOMMAND);
        addRealmArguments(realm);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Update attribute values in an authentication instance.
     *
     * @param realm - the realm which contains the authentication instance to be
     * updated.
     * @param name - the name of the authentication instance which should be 
     * updated.
     * @param attributevalues - a semi-colon (';') delimited list of attribute
     * name / value pairs.
     * @param useDatafile - a boolean value indicating whether the datafile 
     * option should be used.
     * @return the exit status of the "update-auth-instance" command.
     */
    public int updateAuthInstance(String realm, String name, 
            String attributeValues, boolean useDatafile)
    throws Exception {
        setSubcommand(UPDATE_AUTH_INSTANCE_SUBCOMMAND);
        addRealmArguments(realm);
        addNameArguments(name);
        if (!useDatafile) {
            addAttributevaluesArguments(attributeValues);
        } else {
            addDatafileArguments(attributeValues, "attr", ".txt");
        }
    
        addGlobalOptions();
        return(executeCommand(commandTimeout));
    }
    
    /**
     * Set attribute values in multiple auth instances.
     *
     * @param instanceList - a list of instances to create separated by pipes 
     * ('|') in the following format 
     * (<realm1>,<auth-instance-name1>,<attribute-name1>=<attribute-value1>;
     * <attribute-name2>=<attribute-value2>|
     * <realm2>,<auth-instance-name2>,<attribute-name1>=<attribute-value1>;
     * <attribute-name2>=<attribute-value2>)
     * @return a boolean indicating whether all the instances are updated 
     * successfully.
     */
    public boolean updateAuthInstances(String instanceList)
    throws Exception {
        boolean allInstancesUpdated = true;
        
        if (instanceList != null) {
            if (instanceList.length() > 0) {
                String [] instances = instanceList.split("\\|");
                for (String instance: instances) {
                    String[] instanceInfo = instance.split(",", 3);
                    if (instanceInfo.length == 3) {
                        String authRealm = instanceInfo[0];
                        String instanceName = instanceInfo[1];
                        String attributeValues = instanceInfo[2];
                        log(Level.FINE, "updateAuthInstances", "Creating auth " 
                                + "instance " + instance);
                        int exitStatus = updateAuthInstance(authRealm, 
                                instanceName, attributeValues, false);
                        logCommand("updateAuthInstances");
                        resetArgList();
                        if (exitStatus != SUCCESS_STATUS) {
                            allInstancesUpdated = false;
                            log(Level.SEVERE, "updateAuthInstances", 
                                    "The instance " + instance + 
                                    " failed to be updated.");
                        }
                    } else {
                        allInstancesUpdated = false;
                        log(Level.SEVERE, "updateAuthInstances", 
                                "The instance to be created must contain a " + 
                                "realm, an instance name, and an instance " +
                                "type.");
                    }
                }
            } else {
                allInstancesUpdated = false;
                log(Level.SEVERE, "updateAuthInstnces", 
                        "The list of instances is empty.");
            }
        } else {
            allInstancesUpdated = false;
            log(Level.SEVERE, "updateAuthInstances", 
                    "The list of instances is null.");
        }
        return (allInstancesUpdated);
    }
    
    
    /**
     * Set service attribute values in a realm.
     *
     * @param realm - the realm in which the attribute should be set.
     * @param serviceName - the name of the service in which to set the 
     * attribute.
     * @param attributeValues - a semi-colon delimited string containing the 
     * attribute values to be set in the service.
     * @param useDatafile - a boolean indicating whether a datafile should be 
     * used.  If true, a datafile will be created and the "--datafile" argument
     * will be used.  If false, the "--attributevalues" argument and a list of 
     * attribute name/value pairs will be used.
     * @return the exit status of the "set-service-attributes" command.
     */
    public int setServiceAttributes(String realm, String service, 
            String attributeValues, boolean useDatafile) 
    throws Exception {
        setSubcommand(SET_SERVICE_ATTRIBUTES_SUBCOMMAND);
        addRealmArguments(realm);
        addServiceNameArguments(service);
        if (!useDatafile) {
            addAttributevaluesArguments(attributeValues);
        } else {
            addDatafileArguments(attributeValues, "attr", ".txt");
        }
        addGlobalOptions();
        return(executeCommand(commandTimeout));
    }
    
    /**
     * Execute the command specified by the subcommand and teh argument list
     * @param subcommand - the CLI subcommand to be executed
     * @param argList - a String containing a list of arguments separated by 
     * semicolons (';').
     * @return the exit status of the CLI command
     */
    public int executeCommand(String subcommand, String argList)
    throws Exception {
        setSubcommand(subcommand);
        String [] args = argList.split(";");
        for (String arg: args) {
            addArgument(arg);
        }
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Iterate through a list containing attribute values and add the 
     * "--attributevalues" argument and a list of one attribute name/value pairs
     * to the argument list
     * @param valueList - a List object containing one or more attribute 
     * name/value pairs.
     */
    private void addAttributevaluesArguments(List valueList) 
    throws Exception {
       String attributesArg;
       if (useLongOptions) {
           attributesArg = PREFIX_ARGUMENT_LONG + ATTRIBUTE_VALUES_ARGUMENT;
       } else {
           attributesArg = PREFIX_ARGUMENT_SHORT + 
                   SHORT_ATTRIBUTE_VALUES_ARGUMENT;
       }
       addArgument(attributesArg);
       
       Iterator i = valueList.iterator();
       while (i.hasNext()) {
           addArgument((String)i.next());
       }
    }    
    
    
    /**
     * Parse a string containing attribute values and add the 
     * "--attributevalues" and a list of one attribute name/value pairs to the
     * argument list
     * 
     */
    private void addAttributevaluesArguments(String values) 
    throws Exception {
       StringTokenizer tokenizer = new StringTokenizer(values, ";");        
       ArrayList attList = new ArrayList(tokenizer.countTokens());
       
       while (tokenizer.hasMoreTokens()) {
           attList.add(tokenizer.nextToken());
       }
       addAttributevaluesArguments(attList);
    }

    /**
     * Create a datafile and add the "--datafile" and file path arguments to the
     * argument list.
     * @param valueList - a list containing attribute name value pairs separated 
     * by semi-colons (';')
     * @param filePrefix - a string containing a prefix for the datafile that 
     * will be created
     * @param fileSuffix - a string containing a suffix for the datafile that 
     * will be created
     */
    private void addDatafileArguments(List valueList, String filePrefix,
            String fileSuffix)
    throws Exception {
        StringBuffer valueBuffer = new StringBuffer();
        Iterator i = valueList.iterator();
        while (i.hasNext()) {
            valueBuffer.append((String)i.next());
            if (i.hasNext()) {
                valueBuffer.append(";");
            }
        }
        String values = valueBuffer.toString();
        addDatafileArguments(values, filePrefix, fileSuffix);
    }    
    
    /**
     * Create a datafile and add the "--datafile" and file path arguments to the
     * argument list.
     * @param values - a string containing attribute name value pairs separated 
     * by semi-colons (';')
     * @param filePrefix - a string containing a prefix for the datafile that 
     * will be created
     * @param fileSuffix - a string containing a suffix for the datafile that 
     * will be created
     */
    private void addDatafileArguments(String values, String filePrefix,
            String fileSuffix)
    throws Exception {
        Map attributeMap = parseStringToMap(values.replaceAll("\"",""));
        ResourceBundle rb_amconfig = 
                ResourceBundle.getBundle(TestConstants.TEST_PROPERTY_AMCONFIG);
        String attFileDir = getBaseDir() + fileseparator + 
                rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME) + 
                fileseparator + "built" + fileseparator + "classes" + 
                fileseparator;
        String attFile = attFileDir + filePrefix + 
                (new Integer(new Random().nextInt())).toString() + fileSuffix;
        String[] valueArray = values.split(";");
        StringBuffer buff = new StringBuffer();
        for (String value: valueArray) {
            buff.append(value + newline);
        }
        buff.append(newline);
        BufferedWriter out = new BufferedWriter(new FileWriter(attFile));
        out.write(buff.toString());
        out.close();
        String dataFileArg;
        if (useLongOptions) {
            dataFileArg = PREFIX_ARGUMENT_LONG + DATA_FILE_ARGUMENT;
        } else {
            dataFileArg = PREFIX_ARGUMENT_SHORT + SHORT_DATA_FILE_ARGUMENT;
        }
        addArgument(dataFileArg);
        addArgument(attFile);
    }

    /**
     * Add the "--idnames" argument and value to the argument list
     */
    private void addIdnamesArguments(String names) {
        String idnamesArg;
        if (useLongOptions) {
            idnamesArg = PREFIX_ARGUMENT_LONG + ID_NAMES_ARGUMENT;
        } else {
            idnamesArg = PREFIX_ARGUMENT_SHORT + SHORT_ID_NAME_ARGUMENT;
        }

        addArgument(idnamesArg);
        
        StringTokenizer tokenizer = new StringTokenizer(names);
        while (tokenizer.hasMoreTokens()) {
            addArgument(tokenizer.nextToken());
        }
    }

    /**
     * Add the "--xmlfile" argument and XML file to the argument list
     * @param xmlFileArg
     */
    private void addXMLFileArguments(String serviceXmlFileName,
            String initRevisionNo) throws Exception {
        String xmlFileArg;
        if (useLongOptions) {
            xmlFileArg = PREFIX_ARGUMENT_LONG + XML_FILE_ARGUMENT;
        } else {
            xmlFileArg = PREFIX_ARGUMENT_SHORT + SHORT_XML_FILE_ARGUMENT;
        }
        addArgument(xmlFileArg);
        addServiceXmlArg(serviceXmlFileName, initRevisionNo);
    }

    /**
     * Generates the service xml file and adds to the argument list
     * Location of generated xml file will be
     * <QATEST_HOME>/<SERVER_NAME>/built/classes/cli/<xml-file-name>.xml
     * @param serviceName - Name of the service xml
     * @param initRevisionNo - Initial revision number to be assigned
     * @throws java.lang.Exception
     */
    private void addServiceXmlArg(String serviceName, String initRevisionNo) 
    throws Exception {
        ResourceBundle rb_amconfig = 
                ResourceBundle.getBundle(TestConstants.TEST_PROPERTY_AMCONFIG);
        String attFileDir = getBaseDir() + fileseparator + 
                rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME) + 
                fileseparator + "built" + fileseparator + "classes" + 
                fileseparator + "cli" + fileseparator;
        String attFile = attFileDir + serviceName + ".xml";
        
        BufferedWriter out = new BufferedWriter(new FileWriter(attFile));
        // now generate the service xml file
        out.write("<!DOCTYPE ServicesConfiguration");
        out.write(newline);
        out.write("PUBLIC \"=//iPlanet//Service Management " +
        		"Services (SMS) 1.0 DTD//EN\"" + newline);
        out.write("\"jar://com/sun/identity/sm/sms.dtd\">" + newline);
        out.write("<ServicesConfiguration>" + newline);
        out.write("<Service name=\"" + serviceName + "\" version=\"1.0\">");
        out.write(newline);
        out.write("<Schema " + newline);
        out.write("revisionNumber=\"" + initRevisionNo + "\">");
        out.write(newline);
        out.write("<Global>");
        out.write(newline);
        out.write("<AttributeSchema name=\"mock\"" + newline);
        out.write("type=\"single\"" + newline);
        out.write("syntax=\"string\"" + newline);
        out.write("i18nKey=\"\">" + newline);
        out.write("</AttributeSchema>" + newline);
        out.write("<AttributeSchema name=\"mock-boolean\"" + newline);
        out.write("type=\"single\"" + newline);
        out.write("syntax=\"boolean\"" + newline);
        out.write("i18nKey=\"\">" + newline);
        out.write("</AttributeSchema>" + newline);
        out.write("<AttributeSchema name=\"mock-single-choice\"");
        out.write(newline);
        out.write("type=\"single_choice\"" + newline);
        out.write("syntax=\"string\"" + newline);
        out.write("i18nKey=\"\">" + newline);
        out.write("</AttributeSchema>" + newline);
        out.write("<AttributeSchema name=\"mock-number\"" + newline);
        out.write("type=\"single\"" + newline);
        out.write("syntax=\"number_range\"" + newline);
        out.write("rangeStart=\"0\" rangeEnd=\"1\"" + newline);
        out.write("i18nKey=\"\">" + newline);
        out.write("</AttributeSchema>" + newline);
        out.write("<SubSchema name=\"subschemaY\" inheritance=\"single\">");
        out.write(newline);
        out.write("<AttributeSchema name=\"mock\"" + newline);
        out.write("type=\"single\"" + newline);
        out.write("syntax=\"string\"" + newline);
        out.write("i18nKey=\"\">" + newline);
        out.write("</AttributeSchema>" + newline);
        out.write("<AttributeSchema name=\"mock-boolean\"" + newline);
        out.write("type=\"single\"" + newline);
        out.write("syntax=\"boolean\"" + newline);
        out.write("i18nKey=\"\">" + newline);
        out.write("</AttributeSchema>" + newline);
        out.write("<AttributeSchema name=\"mock-single-choice\"" + newline);
        out.write("type=\"single_choice\"" + newline);
        out.write("syntax=\"string\"" + newline);
        out.write("i18nKey=\"\">" + newline);
        out.write("</AttributeSchema>" + newline);
        out.write("<AttributeSchema name=\"mock-number\"" + newline);
        out.write("type=\"single\"" + newline);
        out.write("syntax=\"number_range\"" + newline);
        out.write("rangeStart=\"0\" rangeEnd=\"1\"" + newline);
        out.write("i18nKey=\"\">" + newline);
        out.write("</AttributeSchema>" + newline);
        out.write("</SubSchema>" + newline);
        out.write("<SubSchema name=\"subschema-inheritance\" " +
        		"inheritance=\"single\">" + newline);
        out.write("<AttributeSchema name=\"attr\"" + newline);
        out.write("type=\"single\"" + newline);
        out.write("syntax=\"string\" />" + newline);
        out.write("</SubSchema>" + newline);
        out.write("<SubSchema name=\"subschemaX\" " +
        		"inheritance=\"multiple\">" + newline);
        out.write("<AttributeSchema name=\"attr1\"" + newline);
        out.write("type=\"single\"" + newline);
        out.write("syntax=\"string\" />" + newline);
        out.write("<AttributeSchema name=\"attr2\"" + newline);
        out.write("type=\"single\"" + newline);
        out.write("syntax=\"string\" />" + newline);
        out.write("<AttributeSchema name=\"attr3\"" + newline);
        out.write("type=\"single\"" + newline);
        out.write("syntax=\"string\" />" + newline);
        out.write("</SubSchema>" + newline);
        out.write("</Global>" + newline);
        out.write("</Schema>" + newline);
        out.write("<Configuration>" + newline);
        out.write("<GlobalConfiguration>" + newline);
        out.write("<SubConfiguration name=\"subschemaX\">" + newline);
        out.write("</SubConfiguration>" + newline);
        out.write("</GlobalConfiguration>" + newline);
        out.write("</Configuration>" + newline);
        out.write("</Service>" + newline);
        out.write("</ServicesConfiguration>" + newline);
        out.close();
        
        addArgument(attFile);
    }

       
    /**
     * Sets the sub-command in the second argument of the argument list
     * @param command - the sub-command value to be stored
     */
    private void setSubcommand(String command) { 
        setArgument(SUBCOMMAND_VALUE_INDEX, command);
    }
    
    /**
     * Sets the user ID of the user that will execute the CLI.
     * @param  user - the user ID of the CLI
     */
    private void setAdminUser(String user) { adminUser = user; }
    
    /**
     * Sets the password for the admin user that will execute the CLI
     * @param passwd - the value of the admin user's password
     */
    private void setAdminPassword(String passwd) { adminPassword = passwd; }
    
    /**
     * Sets the member variable passwdFile to the name of the file containing
     * the CLI user's password for use with the "--passwordfile" argument.
     * @param fileName - the file containing the CLI user's password
     */
    private void setPasswordFile(String fileName) { passwdFile = fileName; }
    
    /**
     * Clear all arguments following the value of the admin user's password
     * or the password file.  Removes all sub-command specific arguments.
     */
    public void resetArgList() {
        clearArguments(PASSWORD_VALUE_INDEX);
    }

    /**
     * Returns a regular map.
     * @param str. The format of the string is key1=val1,key2=val2 where ',' is
     * the token
     * @param mTok. Seperator for single key-value pair
     * @return Map containing key-value pairs
     * @throws java.lang.Exception
     */
    private Map parseStringToRegularMap(String str, String mTok)
    throws Exception {
        entering("parseStringToRegularMap", null);
        Map map = new HashMap();
        StringTokenizer st = new StringTokenizer(str, mTok);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int idx = token.indexOf("=");
            if (idx != -1) {
                String attrName = token.substring(0, idx);
                String attrValue = token.substring(idx+1, token.length());
                map.put(attrName, attrValue);
            }
        }
        exiting("parseStringToRegularMap");
        return map;
    }
    
    /**
     * Check to see if a realm exists using the "ssoadm list-realms" command
     * @param realmsToFind - the realm or realms to find in the output of 
     * "ssoadm list-realms".  Multiple realms should be separated by semi-colons
     * (';').
     * @return a boolean value of true if the realm(s) is(are) found and false 
     * if one or more realms is not found.
     */
    public boolean findRealms(String startRealm, String filter, 
            boolean recursiveSearch, String realmsToFind)
    throws Exception {
        boolean realmsFound = true;
        
        if ((realmsToFind != null) && (realmsToFind.length() > 0)) {
            if (listRealms(startRealm, filter, recursiveSearch) == 
                    SUCCESS_STATUS) {                    
                StringTokenizer tokenizer = new StringTokenizer(realmsToFind, 
                        ";");
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    if (token != null) {
                        if (token.length() > 1) {
                            String searchRealm = token.substring(1);
                            if (!findStringInOutput(searchRealm)) {
                                log(logLevel, "findRealms", "Realm " + 
                                        searchRealm + " was not found.");
                                realmsFound = false;
                            } else {
                                log(logLevel, "findRealms", "Realm " + 
                                  searchRealm + " was found.");
                            }
                        } else {
                            log(Level.SEVERE, "findRealms", "Realm " + token + 
                                    " should be longer than 1 character.");
                            realmsFound = false;
                        }
                    } else {
                        log(Level.SEVERE, "findRealms", "Realm " + token + 
                                " in realmsToFind is null.");
                        realmsFound = false;
                    }
                }
            } else {
                log(Level.SEVERE, "findRealms", 
                        "ssoadm list-realms command failed");
                realmsFound = false;
            }
            logCommand("findRealms");
        } else {
            log(Level.SEVERE, "findRealms", "realmsToFind is null or empty");
            realmsFound = false;
        }
        return realmsFound;
    }
    
    /**
     * Check to see if a realm exists using the "ssoadm list-realms" command
     * @param realmsToFind - the realm or realms to find in the output of 
     * "ssoadm list-realms".  Multiple realms should be separated by semi-colons
     * (';').
     * @return a boolean value of true if the realm(s) is(are) found and false 
     * if one or more realms is not found.
     */
    public boolean findRealms(String realmsToFind)
    throws Exception {
        return(findRealms(TestCommon.realm, "*", true, realmsToFind));
    }  
    
    /**
     * Check to see if an identity exists using the "ssoadm list-identities" 
     * command.
     * @param startRealm - the realm in which to find identities
     * @param filter - the filter that will be applied in the search
     * @param type - the type of identities (User, Group, Role) for which the 
     * search will be performed
     * @param idsToFind - the identity or identities to find in the output of 
     * "ssoadm list-identities".  Multiple identities should be separated by 
     * a space (' ').
     * @return a boolean value of true if the identity(ies) is(are) found and 
     * false if one or more identities is not found.
     */
    public boolean findIdentities(String startRealm, String filter, String type,
            String idsToFind)
    throws Exception {
        boolean idsFound = true;
        
        if ((idsToFind != null) && (idsToFind.length() > 0)) {
            if (listIdentities(startRealm, filter, type) == SUCCESS_STATUS) {
                String [] ids = idsToFind.split(";");
                for (int i=0; i < ids.length; i++) {
                    String token = ids[i];
                    String rootDN = "";
                    if (token != null) {
                        if (startRealm.equals(TestCommon.realm)) {
                            rootDN = TestCommon.basedn; 
                        } else {
                            String [] realms = startRealm.split("/");
                            StringBuffer buffer = new StringBuffer();
                            for (int j = realms.length-1; j >= 0; j--) {
                                if (realms[j].length() > 0) {
                                    buffer.append("o=" + realms[j] + ",");
                                }
                            }
                            buffer.append("ou=services,").
                                    append(TestCommon.basedn);
                            rootDN = buffer.toString();
                        }
                        if (token.length() > 0) {
                            String idString = token + " (id=" + token + ",ou=" + 
                                    type.toLowerCase() + "," + rootDN + ")";
                            if (!findStringInOutput(idString)) {
                                log(logLevel, "findIdentities", "String \'" + 
                                        idString + "\' was not found.");
                                idsFound = false;
                            } else {
                                log(logLevel, "findIdentities", type + 
                                        " identity " + token + " was found.");
                            }
                        } else {
                            log(Level.SEVERE, "findIdentities", 
                                    "The identity to find is empty.");
                            idsFound = false;
                        }
                    } else {
                        log(Level.SEVERE, "findIdentities", 
                                "Identity in idsToFind is null.");
                        idsFound = false;
                    }
                }
            } else {
                log(Level.SEVERE, "findIdentities", 
                        "ssoadm list-identities command failed");
                idsFound = false;
            }
            logCommand("findIdentities");
        } else {
            log(Level.SEVERE, "findIdentities", "idsToFind is null or empty");
            idsFound = false;
        }
        return idsFound;
    }

   /**
     * Check to see if an identity exists using the "ssoadm show-members" 
     * command.
     * @param startRealm - the realm in which to find identities
     * @param filter - the filter that will be applied in the search
     * @param type - the type of identities (User, Group, Role) for which the 
     * search will be performed
     * @param membersToFind - the member identity or identities to find in the 
     * output of "ssoadm show-members".  Multiple identities should be separated 
     * by a semicolon (';').
     * @return a boolean value of true if the member(s) is(are) found and 
     * false if one or more members is not found.
     */
    public boolean findMembers(String startRealm, String idName, String idType,
            String memberType, String membersToFind)
    throws Exception {
        boolean membersFound = true;
        
        if ((membersToFind != null) && (membersToFind.length() > 0)) {
            if (showMembers(startRealm, memberType, idName, idType) == 
                    SUCCESS_STATUS) {
                String [] members = membersToFind.split(";");
                for (int i=0; i < members.length; i++) {
                    String rootDN = "";
                    if (members[i] != null) {
                        if (startRealm.equals(TestCommon.realm)) {
                            rootDN = TestCommon.basedn; 
                        } else {
                            String [] realms = startRealm.split("/");
                            StringBuffer realmBuffer = new StringBuffer();
                            for (int j = realms.length-1; j >= 0; j--) {
                                if (realms[j].length() > 0) {
                                    realmBuffer.append("o=" + realms[j] + ",");
                                }
                            }
                            realmBuffer.append("ou=services,").
                                    append(TestCommon.basedn);
                            rootDN = realmBuffer.toString();
                        }
                        if (members[i].length() > 0) {
                            StringBuffer idBuffer = 
                                    new StringBuffer(members[i]);
                            idBuffer.append(" (id=").append(members[i]).
                                    append(",ou=").
                                    append(memberType.toLowerCase()).
                                    append(",").append(rootDN).append(")");
                            if (!findStringInOutput(idBuffer.toString())) {
                                log(Level.FINEST, "findMember", 
                                        "String \'" + idBuffer.toString() + 
                                        "\' was not found.");
                                membersFound = false;
                            } else {
                                log(Level.FINEST, "findMembers", memberType + 
                                        " identity " + members[i] + 
                                        " was found.");
                            }
                        } else {
                            log(Level.SEVERE, "findMembers", 
                                    "The member to find is empty.");
                            membersFound = false;
                        }
                    } else {
                        log(Level.SEVERE, "findMembers", 
                                "Identity in membersToFind is null.");
                        membersFound = false;
                    }
                }
            } else {
                log(Level.SEVERE, "findMembers", 
                        "ssoadm show-members command failed");
                membersFound = false;
            }
            logCommand("findMembers");
        } else {
            log(Level.SEVERE, "findMembers", "membersToFind is null or empty");
            membersFound = false;
        }
        return membersFound;
    }    

    /**
     * Check to see if a member exists using the "ssoadm show-memberships" 
     * command.
     * @param startRealm - the realm in which to find identities
     * @param filter - the filter that will be applied in the search
     * @param type - the type of identities (User, Group, Role) for which the 
     * search will be performed
     * @param membersToFind - the member identity or identities to find in the 
     * output of "ssoadm show-memberships".  Multiple memberships should be 
     * separated by a semicolon (';').
     * @return a boolean value of true if the membership(s) is(are) found and 
     * false if one or more memberships is not found.
     */
    public boolean findMemberships(String startRealm, String idName,
            String idType, String membershipType, String membershipsToFind)
    throws Exception {
        boolean membershipsFound = true;

        if ((membershipsToFind != null) && (membershipsToFind.length() > 0)) {
            if (showMemberships(startRealm, membershipType, idName, idType) == 
                    SUCCESS_STATUS) {
                String [] memberships = membershipsToFind.split(";");
                for (int i=0; i < memberships.length; i++) {
                    String rootDN = "";
                    if (memberships[i] != null) {
                        if (startRealm.equals(TestCommon.realm)) {
                            rootDN = TestCommon.basedn; 
                        } else {
                            String [] realms = startRealm.split("/");
                            StringBuffer realmBuffer = new StringBuffer();
                            for (int j = realms.length-1; j >= 0; j--) {
                                if (realms[j].length() > 0) {
                                    realmBuffer.append("o=" + realms[j] + ",");
                                }
                            }
                            realmBuffer.append("ou=services,").
                                    append(TestCommon.basedn);
                            rootDN = realmBuffer.toString();
                        }
                        if (memberships[i].length() > 0) {
                            StringBuffer idBuffer = 
                                    new StringBuffer(memberships[i]);
                            idBuffer.append(" (id=").append(memberships[i]).
                                    append(",ou=").
                                    append(membershipType.toLowerCase()).
                                    append(",").append(rootDN).append(")");
                            if (!findStringInOutput(idBuffer.toString())) {
                                log(Level.FINEST, "findMemberships", 
                                        "String \'" + idBuffer.toString() + 
                                        "\' was not found.");
                                membershipsFound = false;
                            } else {
                                log(Level.FINEST, "findMemberships", 
                                        membershipType + " identity " + 
                                        memberships[i] + " was found.");
                            }
                        } else {
                            log(Level.SEVERE, "findMemberships", 
                                    "The membership to find is empty.");
                            membershipsFound = false;
                        }
                    } else {
                        log(Level.SEVERE, "findMemberships", 
                                "Identity in membersToFind is null.");
                        membershipsFound = false;
                    }
                }
            } else {
                log(Level.SEVERE, "findMemberships", 
                        "ssoadm show-memberships command failed");
                membershipsFound = false;
            }
            logCommand("findMemberships");
        } else {
            log(Level.SEVERE, "findMemberships", 
                    "membersToFind is null or empty");
            membershipsFound = false;
        }
        return membershipsFound;
    }
    
    /**
     * Search through the attributes of a realm to verify that certain list of
     * attributes name/value pairs are present.
     *
     * @param realm - the realm for which the attributes should be retrieved.
     * @param serviceName - the name of service for which the attributes should 
     * be retrieved.
     * @param attributeValues - a semi-colon delimited list of attribute 
     * name/value pairs.
     * @return a boolean flag indicating whether all of the attribute name/value
     * pairs are found in the output of the "ssoadm get-realm" command.
     */
    public boolean findRealmAttributes(String realm, String serviceName, 
            String attributeValues)
    throws Exception {
        boolean attributesFound = true;
        int commandStatus = -1;
        
        if (realm != null && !realm.equals("")) {
            if (serviceName != null && !serviceName.equals("")) {
                if (attributeValues != null && !attributeValues.equals("")) {
                    commandStatus = getRealm(realm, serviceName);
                    if (commandStatus == SUCCESS_STATUS) {
                        attributesFound = findStringsInOutput(attributeValues, 
                                ";");
                    } else {
                        log(Level.SEVERE, "findRealmAttributes", 
                                "The ssoadm get-realm command returned " + 
                                commandStatus + " as an exit status");
                        attributesFound = false;
                    }
                } else {
                    log(Level.SEVERE, "findRealmAttributes", 
                            "The attribute value list is not valid");
                    attributesFound = false;
                }
            } else {
                log(Level.SEVERE, "findRealmAttributes", 
                        "The service name is not valid");
                attributesFound = false;
            }
        } else {
            log(Level.SEVERE, "findRealmAttributes", 
                    "The realm name is not valid");
            attributesFound = false;
        }
        return attributesFound;
    }
    
    /**
     * Search through the attributes of a realm to verify that certain list of
     * attributes name/value pairs are present.
     *
     * @param realm - the realm in which the identity exists.
     * @param idName - the name of the identity for which the attributes should 
     * be retrieved.
     * @param idType - the type of the identity for which the attributes should 
     * be retrieved.
     * @param attributeNames - the name or names of the attributes that should 
     * be retrieved.
     * @param attributeValues - the attribute name/value pair or pairs which 
     * should be found.
     * @return a boolean flag indicating whether all of the attribute name/value
     * pairs are found in the output of the "ssoadm get-identity" command.
     */
    public boolean findIdentityAttributes(String realm, String idName, 
            String idType, String attributeNames, String attributeValues)
    throws Exception {
        boolean attributesFound = true;
        int commandStatus = -1;
        
        if (realm != null && !realm.equals("")) {
            if (idName != null && !idName.equals("")) {
                if (idType != null && !idType.equals("")) {
                    commandStatus = getIdentity(realm, idName, idType, 
                            attributeNames);
                    if (commandStatus == SUCCESS_STATUS) {
                        attributesFound = findStringsInOutput(attributeValues, 
                                ";");
                    } else {
                        log(Level.SEVERE, "findIdentityAttributes", 
                                "The ssoadm get-identity command returned " + 
                                commandStatus + " as an exit status");
                        attributesFound = false;
                    }
                } else {
                    log(Level.SEVERE, "findIdentityAttributes", 
                            "The attribute value list is not valid");
                    attributesFound = false;
                }
            } else {
                log(Level.SEVERE, "findIdentityAttributes", 
                        "The service name is not valid");
                attributesFound = false;
            }
        } else {
            log(Level.SEVERE, "findIdentityAttributes", 
                    "The realm name is not valid");
            attributesFound = false;
        }
        return attributesFound;
    }
    
    /**
     * Search through the attributes of a realm to verify that certain list of
     * attributes name/value pairs are present.
     *
     * @param realm - the realm in which the auth instances should be listed.
     * @param instanceName - a pipe ('|') separated list of auth instances in 
     * the following format: 
     * <auth-instance-name1>,<auth-instance-type1>|<auth-instance-name2>,
     * <auth-instance-type2>
     * @return a boolean flag indicating whether all of the auth instances were
     * found in the output of the "ssoadm list-auth-instances command.
     */
    public boolean findAuthInstances(String realm, String instanceNames)
    throws Exception {
        boolean instancesFound = true;
        int commandStatus = -1;
        
        if (realm != null && !realm.equals("")) {
            commandStatus = listAuthInstances(realm);
            logCommand("findAuthInstances");
            if (commandStatus == SUCCESS_STATUS) {
                StringBuffer instanceList = new StringBuffer();
                String[] instances = instanceNames.split("\\|");
                for (int i=0; i < instances.length; i++) {
                    String[] instanceInfo = instances[i].split(",");
                    if (instanceInfo.length == 2) {
                        String name = instanceInfo[0];
                        String type = instanceInfo[1];
                        instanceList.append(name).append(", [type=").
                                append(type).append("]");
                        if (i < instances.length - 1) {
                            instanceList.append(";");
                        }
                    } else {
                        log(Level.SEVERE, "findAuthInstances", 
                                "The instance list must contain a name and " +
                                "type for each instance");
                        instancesFound = false;
                    }
                }
                if (instancesFound) {
                    log(Level.FINE, "findAuthInstances", "Searching for the " + 
                            "following instances: " + instanceList);
                    instancesFound = 
                            findStringsInOutput(instanceList.toString(), ";");
                }
            } else {
                log(Level.SEVERE, "findAuthInstances", 
                        "The ssoadm list-auth-instances command returned " + 
                        commandStatus + " as an exit status");
                instancesFound = false;
            }
        } else {
            log(Level.SEVERE, "findIdentityAttributes", 
                    "The realm name is not valid");
            instancesFound = false;
        }
        return instancesFound;
    }
    
    /**
     * Search through the attributes of a realm to verify that certain list of
     * attributes name/value pairs are present.
     *
     * @param realm - the realm in which the identity exists.
     * @param instanceName - the name of the authentication instance for which 
     * the attributes should be retrieved.
     * @param attributeValues - the attribute name/value pair or pairs which 
     * should be found.
     * @return a boolean flag indicating whether all of the attribute name/value
     * pairs are found in the output of the "ssoadm get-auth-instance" command.
     */
    public boolean findAuthInstanceAttributes(String realm, 
            String instanceName, String attributeValues)
    throws Exception {
        boolean attributesFound = true;
        int commandStatus = -1;
        
        if (realm != null && !realm.equals("")) {
            if (instanceName != null && !instanceName.equals("")) {
                if (attributeValues != null && !attributeValues.equals("")) {
                    commandStatus = getAuthInstance(realm, instanceName);
                    if (commandStatus == SUCCESS_STATUS) {
                        attributesFound = findStringsInOutput(attributeValues, 
                                ";");
                    } else {
                        log(Level.SEVERE, "findAuthInstanceAttributes", 
                                "The ssoadm get-auth-instance command returned "
                                + commandStatus + " as an exit status");
                        attributesFound = false;
                    }
                } else {
                    log(Level.SEVERE, "findAuthInstanceAttributes", 
                            "The attribute value list is not valid");
                    attributesFound = false;
                }                        
            } else {
                log(Level.SEVERE, "findAuthInstanceAttributes", 
                        "The instance name is not valid");
                attributesFound = false;
            }
        } else {
            log(Level.SEVERE, "findAuthInstanceAttributes", 
                    "The realm name is not valid");
            attributesFound = false;
        }
        return attributesFound;
    }    

    /**
     * Create an agent in a realm
     * @param realm - the realm in which to create the agent
     * @param name - the name of the agent to be created
     * @param type - the type of agent to be created (e.g. "J2EEAgent", 
     * "WebAgent", "2.2_Agent")
     * @param attributeValues - a string containing the attribute values for the 
     * agent to be created
     * @param useAttributeValues - a boolean flag indicating whether the 
     * "--attributevalues" option should be used 
     * @param useDatafile - a boolean flag indicating whether the attribute 
     * values should be written to a file and passed to the CLI using the 
     * "--datafile <file-path>" arguments
     * @return the exit status of the "create-agent" command
     */
    public int createAgent(String realm, String name, String type, 
            String attributeValues, boolean useAttributeValues, 
            boolean useDatafile) 
    throws Exception {
        setSubcommand(CREATE_AGENT_SUBCOMMAND);
        addRealmArguments(realm);
        addAgentTypeArguments(type);
        addAgentNameArguments(name);
        
        if (attributeValues != null) {
            if (useAttributeValues) {
                addAttributevaluesArguments(attributeValues);
            } 
            if (useDatafile) {
                addDatafileArguments(attributeValues, "agentAttrValues", 
                        ".txt");
            }
        }
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }

    /**
     * Create an agent in a realm
     * @param realm - the realm in which to create the agent
     * @param name - the name of the agent to be created
     * @param type - the type of agent to be created (e.g. "J2EEAgent", 
     * "WebAgent", "2.2_Agent")
     * @param attributeValues - a string containing the attribute values for the 
     * agent to be created
     */
    public int createAgent(String realm, String name, String type, 
            String attributeValues) 
    throws Exception {
        return createAgent(realm, name, type, attributeValues, true, false);
    }
 
    /**
     * List the agents in a particular realm
     * @param realm - the realm in which to start the search for agents
     * @param filter - the filter to apply in the search for agents
     * @param agentType - the type of agents (e.g. "J2EEAgent", "WebAgent", 
     * "2.2_Agent") for which the search sould be performed
     * @return the exit status of the "list-agents" command
     */
    public int listAgents(String realm, String agentType, String filter)
    throws Exception {
        setSubcommand(LIST_AGENT_SUBCOMMAND);
        addRealmArguments(realm);
        if (filter != null) {
            addFilterArguments(filter);
        }
        if (agentType != null) {
            addAgentTypeArguments(agentType);            
        }

        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Delete one or more agents in a realm
     * @param realm - the realm from which the agents should be deleted
     * @param names - one or more agents names to be deleted
     * @param type - the type of the agent(s) to be deleted
     * @return the exit status of the "delete-agents" command
     */
    public int deleteAgents(String realm, String names, String type)
    throws Exception {
        setSubcommand(DELETE_AGENTS_SUBCOMMAND);
        addRealmArguments(realm);
        addAgentNamesArguments(names);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }

    /**
     * Check to see if an agent exists using the "ssoadm list-agents" 
     * command.
     * @param startRealm - the realm in which to find agents
     * @param filter - the filter that will be applied in the search
     * @param type - the type of agents (e.g. "J2EEAgent", "WebAgent", 
     * "2.2_Agent") for which the search will be performed
     * @param agentsToFind - the agent or agents to find in the output of 
     * "ssoadm list-agents".  Multiple agents should be separated by 
     * a space (' ').
     * @return a boolean value of true if the agent(s) is(are) found and 
     * false if one or more agents is not found.
     * Eg: agent in root realm
     * test1 (id=test1,ou=agentonly,dc=red,dc=iplanet,dc=com)
     * Eg: agent in secondary realm
     * test1 (id=test1,ou=agentonly,o=showmembersrealm1,ou=services,
     * dc=red,dc=iplanet,dc=com)
     */
    public boolean findAgents(String startRealm, String filter, String type,
            String agentsToFind)
    throws Exception {
        boolean agentsFound = true;
        
        if ((agentsToFind != null) && (agentsToFind.length() > 0)) {
            if (listAgents(startRealm, type, filter) == SUCCESS_STATUS) {
                String [] ids = agentsToFind.split(";");
                for (int i=0; i < ids.length; i++) {
                    String token = ids[i];
                    String rootDN = "";
                    if (token != null) {
                        if (startRealm.equals(TestCommon.realm)) {
                            rootDN = TestCommon.basedn; 
                        } else {
                            String [] realms = startRealm.split("/");
                            StringBuffer buffer = new StringBuffer();
                            for (int j = realms.length-1; j >= 0; j--) {
                                if (realms[j].length() > 0) {
                                    buffer.append("o=" + realms[j] + ",");
                                }
                            }
                            buffer.append("ou=services,").
                                    append(TestCommon.basedn);
                            rootDN = buffer.toString();
                        }
                        if (token.length() > 0) {
                            String idString = token + " (id=" + token + ",ou=" + 
                                    "agentonly" + "," + rootDN + ")";
                            log(Level.SEVERE, "findAgents", "idString=" + 
                                    idString);
                            if (!findStringInOutput(idString)) {
                                log(Level.SEVERE, "findAgents", "String \'" + 
                                        idString + "\' was not found.");
                                agentsFound = false;
                            } else {
                                log(Level.FINE, "findAgents", type + 
                                        " agents " + token + " was found.");
                            }
                        } else {
                            log(Level.SEVERE, "findAgents", 
                                    "The agents to find is empty.");
                            agentsFound = false;
                        }
                    } else {
                        log(Level.SEVERE, "findAgents", 
                                "Agent in agentsToFind is null.");
                        agentsFound = false;
                    }
                }
            } else {
                log(Level.SEVERE, "findAgents", 
                        "ssoadm list-agents command failed");
                agentsFound = false;
            }
            logCommand("findAgents");            
        } else {
            log(Level.SEVERE, "findAgents", "agentsToFind is null or empty");
            agentsFound = false;
        }
        return agentsFound;
 }

    /**
     * Adds the attribute default values of the service with given schema type
     * schema name and attribute values.
     * 
     * @param serviceName - name of the service.
     * @param schemaType - type of the schema used.
     * @param attributesToFind - values of the attributes to be found.
     * @param useDataFile - flag to specify whether to use data file or not.
     * @param subSchema - name of the subschema used.
     * @return exit status of "add-att-defs" subcommand.
     */
    public int addAttrDefs(String serviceName, String schemaType,
            String attributesToFind, boolean useDataFile,
            String subSchema) 
    throws Exception {
        String[] attrs = attributesToFind.split(";");
        List argList = Arrays.asList(attrs);
        setSubcommand(ADD_ATTR_DEFS_SUBCOMMAND);
        addServiceNameArguments(serviceName);
        addSchemaTypeArguments(schemaType);
        if (useDataFile) {
            addDatafileArguments(argList, "attrVals", "txt");
        } else {
            addAttributevaluesArguments(argList);
        }
        addSubSchemaNameArguments(subSchema);
        addGlobalOptions();
        return executeCommand(commandTimeout);
    }

    /**
     * Sets the attribute default values of the service with given schema type
     * schema name and attribute values.
     * 
     * @param serviceName - name of the service.
     * @param schemaType - type of the schema used.
     * @param attributesToFind - values of the attributes to be found.
     * @param useDataFile - flag to specify whether to use data file or not.
     * @param subSchema - name of the subschema used.
     * @return exit status of "set-attr-defs" subcommand.
     */
    public int setAttrDefs(String serviceName, String schemaType,
            String attributesToFind, boolean useDataFile,
            String subSchema) 
    throws Exception {
        String[] attrs = attributesToFind.split(";");
        List argList = Arrays.asList(attrs);
        setSubcommand(SET_ATTR_DEFS_SUBCOMMAND);
        addServiceNameArguments(serviceName);
        addSchemaTypeArguments(schemaType);
        if (useDataFile) {
            addDatafileArguments(argList, "attrVals", "txt");
        } else {
            addAttributevaluesArguments(argList);
        }
        addSubSchemaNameArguments(subSchema);
        addGlobalOptions();
        return executeCommand(commandTimeout);
    }
    
    /**
     * Creates datastore in a realm given the realm name, datastore name,
     * datastore type and attribute values.
     * 
     * @param realmName - name of the realm
     * @param datastoreName - name of the datastore
     * @param datastoreType - type of the datastore
     * @param attrValues - attribute values
     * @param useDataFile - flag to specify whether to use data file or not
     * @return exit status of "create-datastore" subcommand
     * @throws java.lang.Exception
     */
    public int createDatastore(String realmName, String datastoreName, 
            String datastoreType, String attrValues, boolean useDataFile)
    throws Exception {
        String[] attrs = attrValues.split(";");
        List argList = Arrays.asList(attrs);
        setSubcommand(CREATE_DATASTORE_SUBCOMMAND);
        addRealmArguments(realmName);
        addDataStoreNameArguments(datastoreName);
        addDataStoreTypeArguments(datastoreType);
        if (useDataFile) {
            addDatafileArguments(argList, "attrVals", "txt");
        } else {
            addAttributevaluesArguments(argList);
        }
        addGlobalOptions();
        return executeCommand(commandTimeout);
    }
    
    /**
     * Adds the "--datatype" or (-t) and datastore type arguments 
     * to argument list.
     * @param datastoreType  - tyep of datastore added to argument list.
     */
    public void addDataStoreNameArguments(String datastoreType) {
    	String datastoreTypeArg;
    	if (useLongOptions) {
            datastoreTypeArg = PREFIX_ARGUMENT_LONG + DATASTORE_NAME_ARG;
    	} else {
            datastoreTypeArg = PREFIX_ARGUMENT_SHORT + SHORT_DATASTORE_NAME_ARG;
    	}
    	addArgument(datastoreTypeArg);
    	addArgument(datastoreType);
    }

    /**
     * Adds the "--names" or (-m) and datastore name arguments to argument list.
     * @param datastoreNames  - names of datastores added to argument list.
     */
    public void addDataStoreNamesArguments(String datastoreNames) {
    	String datastoreNamesArg;
    	if (useLongOptions) {
    		datastoreNamesArg = PREFIX_ARGUMENT_LONG + DATASTORE_NAMES_ARG;
    	} else {
    		datastoreNamesArg = PREFIX_ARGUMENT_SHORT + 
                        SHORT_DATASTORE_NAME_ARG;
    	}
    	addArgument(datastoreNamesArg);
        String[] datastoreStrings = datastoreNames.split(",");
        for(int i=0; i < datastoreStrings.length; i++) {
            addArgument(datastoreStrings[i]);
        }
    }    
    
    /**
     * Adds the "--datatype" or (-t) and datastore type arguments to 
     * argument list.
     * @param datastoreType - type of datastore added to argument list.
     */
    public void addDataStoreTypeArguments(String datastoreType) {
    	String datastoreTypeArg;
    	if (useLongOptions) {
            datastoreTypeArg = PREFIX_ARGUMENT_LONG + DATASTORE_TYPE_ARG;
    	} else {
    	    datastoreTypeArg = PREFIX_ARGUMENT_SHORT + SHORT_DATASTORE_TYPE_ARG;
    	}
    	addArgument(datastoreTypeArg);
    	addArgument(datastoreType);
    }
    
    /**
     * Deletes datastore(s) in realm given realm name and datastore name(s).
     * 
     * @param realmName - name of the realm
     * @param datastoreNames - name(s) of the datastores
     * @return exit status of "delete-datastores" subcommand
     * @throws java.lang.Exception
     */
    public int deleteDatastores(String realmName, String datastoreNames)
    throws Exception {
        setSubcommand(DELETE_DATASTORE_SUBCOMMAND);
        addRealmArguments(realmName);
        addDataStoreNamesArguments(datastoreNames);
        addGlobalOptions();        
        return executeCommand(commandTimeout);
    }

    /**
     * Updates datastore in a realm given the realm name, datastore name 
     * and attribute values.
     * 
     * @param realmName - name of the realm
     * @param datastoreName - name of the datastore
     * @param attrValues - attribute values
     * @param useDataFile - flag to specify whether to use data file or not
     * @return exit status of "create-datastore" subcommand
     * @throws java.lang.Exception
     */
    public int updateDatastore(String realmName, String datastoreName, 
            String attrValues, boolean useDataFile )
    throws Exception {
        String[] attrs = attrValues.split(";");
        List argList = Arrays.asList(attrs);
        setSubcommand(UPDATE_DATASTORE_SUBCOMMAND);
        addRealmArguments(realmName);
        addDataStoreNameArguments(datastoreName);
        if (useDataFile) {
            addDatafileArguments(argList, "attrVals", "txt");
        } else {
            addAttributevaluesArguments(argList);
        }
        addGlobalOptions();        
        return executeCommand(commandTimeout);
    }
    
    /**
     * Lists the datastores in a realm.
     * 
     * @param realmName - name of the realm in which datastores are listed.
     * @return - exit status of "list-datastores" subcommand.
     * @throws java.lang.Exception
     */
    public int listDatastores(String realmName) 
    throws Exception {
        setSubcommand(LIST_DATASTORE_SUBCOMMAND);
        addRealmArguments(realmName);
        addGlobalOptions();        
        return executeCommand(commandTimeout);
    }
    
    /**
     * Shows the datastore profile.
     * 
     * @param realmName - name of the realm in which datastores are listed.
     * @param datastoreName - name of the datastore
     * @return - exit status of "show-datastore" subcommand.
     * @throws java.lang.Exception
     */
    public int showDatastore(String realmName, String datastoreName)
    throws Exception {
    	setSubcommand(SHOW_DATASTORE_SUBCOMMAND);
        addRealmArguments(realmName);
        addDataStoreNameArguments(datastoreName);
        addGlobalOptions();        
        return executeCommand(commandTimeout);    	
    }

    /**
     * Creates a batch command.
     *
     * @param subCommand - name of sub-command. For e.g. create-realm,
     *        create-identity, etc.
     * @param argNameValueList - list with arguments in map for the command to
     *        be created.
     * @return commandString - is entire command as string.
     */
    private String createBatchCommand(String subCommand,
            List<Map> argNameValueList)
            throws Exception {
        StringBuffer commandBuffer = new StringBuffer();
        String commandString = "";
        if (argNameValueList.size() > 0) {
            commandBuffer.append(subCommand);
            commandBuffer.append(" ");
            Iterator i = argNameValueList.iterator();
            while (i.hasNext()) {
                Map argMap = (Map)i.next();
                Set s = argMap.keySet();
                Iterator it = s.iterator();
                while(it.hasNext()) {
                    String key = (String)it.next();
                    String value = (String)argMap.get(key);
                    commandBuffer.append(key + " " + value + " ");
                }
            }
            commandString = commandBuffer.toString();
        }
        return commandString;
    }

    /**
     * Creates a batchfile.
     *
     * @param commandList - list containing commands to be executed in batch.
     * @return batchFile - a string with path of batch file location
     */
    private String createBatchfile(List commandList)
            throws Exception {
        StringBuffer commandsBuffer = new StringBuffer();
        ResourceBundle rb_amconfig =
                ResourceBundle.getBundle(
                TestConstants.TEST_PROPERTY_AMCONFIG);
        String attFileDir = getBaseDir() + fileseparator +
                rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME) +
                fileseparator + "built" + fileseparator + "classes" +
                fileseparator + "cli" + fileseparator;
        String attFile = attFileDir + "batchFile" +
                (new Integer(new Random().nextInt())).toString() + ".txt";
        BufferedWriter out = new BufferedWriter(new FileWriter(attFile));
        if (commandList.size() > 0) {
            Iterator i = commandList.iterator();
            while (i.hasNext()) {
                commandsBuffer.append((String) i.next());
                if (i.hasNext()) {
                    commandsBuffer.append(newline);
                }
            }
        }
        String commandsString = commandsBuffer.toString();
        log(Level.FINEST, "createBatchfile", "List of commands executed " +
                "in batch: \n\n" + commandsString + "\n");
        out.write(commandsString);
        out.close();
        return attFile;
    }

    /**
     * Adds batch file arguments to "do-batch" sub-command.
     *
     * @param batchFile - Name of file that contains commands and options.
     */
    private void addBatchFileArguments(String batchFile) {
    	String batchFileArg;
        if (!batchFile.trim().equals("")) {
            if (useLongOptions) {
                batchFileArg = PREFIX_ARGUMENT_LONG + BATCH_FILE_ARGUMENT;
            } else {
    		batchFileArg = PREFIX_ARGUMENT_SHORT +
                        SHORT_BATCH_FILE_ARGUMENT;
            }
            addArgument(batchFileArg);
            addArgument(batchFile);
        }
    }

    /**
     * Adds batch status arguments to "do-batch" sub-command.
     *
     * @param batchStatus - name of the batch status file.
     */
    private void addBatchStatusArguments(String batchStatus) {
        String batchStatusArg;
        if (!batchStatus.trim().equals("")) {
            if (useLongOptions) {
                batchStatusArg = PREFIX_ARGUMENT_LONG + BATCH_STATUS_ARGUMENT;
            } else {
                batchStatusArg = PREFIX_ARGUMENT_SHORT +
                        SHORT_BATCH_STATUS_ARGUMENT;
            }
            addArgument(batchStatusArg);
            addArgument(batchStatus);
        }
    }

    /**
     * Executes commands in batch.
     *
     * @param batchFile - Name of file that contains commands and options.
     * @param batchStatus - name of the batch status file.
     * @param continueDeletingService - Continue processing the rest of the
     *        request when preceeding request was erroneous.
     * @return exit status of "show-datastore" subcommand.
     * @throws java.lang.Exception
     */
    public int doBatch(String batchFile, String batchStatus,
    		boolean continueDeletingService)
    throws Exception {
        setSubcommand(DO_BATCH_SUBCOMMAND);
        addBatchFileArguments(batchFile);
        addBatchStatusArguments(batchStatus);
        if (continueDeletingService) {
            addContinueArgument();
        }
        addGlobalOptions();
        return executeCommand(commandTimeout);
    }

    /**
     * Creates a site
     *
     * @param siteName - Site name, e.g. mysite
     * @param siteUrl - Site primary URL, e.g. http://www.example.com:8080
     * @param secondaryUrls - Secondary URLs
     * @return exit status of "create-site" subcommand.
     * @throws java.lang.Exception
     */
    public int createSite(String siteName, String siteUrl,
    		String secondaryUrls)
    throws Exception {
        setSubcommand(CREATE_SITE_SUBCOMMAND);
        addSiteNameArguments(siteName);
        addSiteUrlArguments(siteUrl);
        if (!secondaryUrls.equals("") || secondaryUrls != null) {
            addSecondaryUrlArguments(secondaryUrls);
        }
        addGlobalOptions();
        return executeCommand(commandTimeout);
    }

    /**
     * Adds site name arguments.
     *
     * @param siteName - Site name, e.g. mysite
     */
    private void addSiteNameArguments(String siteName) {
        String siteNameArg;
        if (!siteName.equals("")) {
            if (useLongOptions) {
            	siteNameArg = PREFIX_ARGUMENT_LONG + SITE_NAME_ARGUMENT;
            } else {
            	siteNameArg = PREFIX_ARGUMENT_SHORT + SHORT_SITE_NAME_ARGUMENT;
            }
            addArgument(siteNameArg);
            addArgument(siteName);
        }
    }

    /**
     * Adds site url arguments.
     *
     * @param siteUrl - Sites primary URL, e.g. http://www.example.com:8080
     */
    private void addSiteUrlArguments(String siteUrl) {
        String siteUrlArg;
        if (!siteUrl.equals("")) {
            if (useLongOptions) {
            	siteUrlArg = PREFIX_ARGUMENT_LONG + SITE_URL_ARGUMENT;
            } else {
            	siteUrlArg = PREFIX_ARGUMENT_SHORT + SHORT_SITE_URL_ARGUMENT;
            }
            addArgument(siteUrlArg);
            addArgument(siteUrl);
        }
    }

    /**
     * Adds site name arguments.
     *
     * @param secondaryUrls - Secondary URLs
     */
    private void addSecondaryUrlArguments(String secondaryUrls) {
        String secondaryUrlsArg;
        if (!secondaryUrls.equals("")) {
            if (useLongOptions) {
            	secondaryUrlsArg = PREFIX_ARGUMENT_LONG +
                        SECONDARY_URLS_ARGUMENT;
            } else {
            	secondaryUrlsArg = PREFIX_ARGUMENT_SHORT +
                        SHORT_SECONDARY_URLS_ARGUMENT;
            }
            addArgument(secondaryUrlsArg);
            String[] secStrings = secondaryUrls.split(",");
            for (String s : secStrings) {
                addArgument(s);
            }
        }
    }

    /**
     * Shows site
     *
     * @param siteName - Site name, e.g. mysite
     * @return exit status of "show-site" subcommand.
     * @throws java.lang.Exception
     */
    public int showSite(String siteName)
    throws Exception {
        setSubcommand(SHOW_SITE_SUBCOMMAND);
        addSiteNameArguments(siteName);
        addGlobalOptions();
        return executeCommand(commandTimeout);
    }

    /**
     * List sites
     *
     * @return exit status of "list-sites" subcommand.
     * @throws java.lang.Exception
     */
    public int listSites()
    throws Exception {
        setSubcommand(LIST_SITES_SUBCOMMAND);
        addGlobalOptions();
        return executeCommand(commandTimeout);
    }

    /**
     * Delete site
     *
     * @param siteName - Site name, e.g. mysite
     * @return exit status of "delete-site" subcommand.
     * @throws java.lang.Exception
     */
    public int deleteSite(String siteName)
    throws Exception {
        setSubcommand(DELETE_SITE_SUBCOMMAND);
        addSiteNameArguments(siteName);
        addGlobalOptions();
        return executeCommand(commandTimeout);
    }

    /**
     * Creates a site.
     *
     * @param sitesArgs - Names of the sites
     * @return
     * @throws java.lang.Exception
     */
    public boolean createSites(String sitesArgs)
    throws Exception {
    	int exitStatus = -1;
    	boolean allSitesCreated = true;
        String[] sites = sitesArgs.split("\\|");
        Map argMap = new HashMap();
        List argList = new ArrayList();
        List commandList = new ArrayList();

    	if (sites.length > 0) {
            if (sites.length == 1) {
                String[] siteArgs = sites[0].split(";");
                if (siteArgs.length >= 2) {
                    String siteName = siteArgs[0];
                    String siteUrl = siteArgs[1];
                    log(Level.FINEST, "createSites", "Creating site with args: "
                            + siteName + ", " + siteUrl);
                    exitStatus = createSite(siteName, siteUrl, "");
                    if (siteArgs.length > 2) {
                        String secUrls = siteArgs[2];
                        log(Level.FINEST, "createSites", "Creating site with " +
                                "args: " + siteName + ", " + siteUrl + ", "
                                + secUrls);
                        exitStatus = createSite(siteName, siteUrl, secUrls);
                    }
                }
                logCommand("createSites");
                resetArgList();
            } else {
                for (String site : sites) {
                    String[] siteArgs = site.split(";");
                    if (siteArgs.length >= 2) {
                        String siteName = siteArgs[0];
                        String siteUrl = siteArgs[1];
                        argMap.put(PREFIX_ARGUMENT_LONG + SITE_NAME_ARGUMENT,
                        		siteName);
                        argMap.put(PREFIX_ARGUMENT_LONG + SITE_URL_ARGUMENT,
                        		siteUrl);
                        if (siteArgs.length > 2) {
                            String secUrls = siteArgs[2];
                            secUrls = secUrls.replace(",", " ");
                            argMap.put(PREFIX_ARGUMENT_LONG +
                                    SECONDARY_URLS_ARGUMENT, secUrls);
                        }
                        argList.add(argMap);
                        commandList.add(createBatchCommand(
                                CREATE_SITE_SUBCOMMAND, argList));
                        argList.clear();
                        argMap.clear();
                    } else {
                        allSitesCreated = false;
                        log(Level.SEVERE, "createSites", "Incorrect site " +
                                "argument list passed.");
                    }
                }
                String attFile = createBatchfile(commandList);
                exitStatus = doBatch(attFile, "", false);
                logCommand("createSites");
                resetArgList();
            }
            if (exitStatus != SUCCESS_STATUS) {
            	allSitesCreated = false;
                log(Level.SEVERE, "createSites", "The ssoadm command " +
                        " failed to create sites.");
            }
        } else {
        	allSitesCreated = false;
            log(Level.SEVERE, "createSites",
                    "The list of sites is empty.");
        }
    	return allSitesCreated;
    }

    /**
     *
     * @param siteNames
     * @return
     * @throws java.lang.Exception
     */
    public boolean deleteSites(String siteNames)
    throws Exception {
    	int exitStatus = -1;
    	boolean allSitesDeleted = true;
        String[] siteStrings = siteNames.split(";");
        Map argMap = new HashMap();
        List argList = new ArrayList();
        List commandList = new ArrayList();

    	if (siteStrings.length > 0) {
    		if (siteStrings.length == 1) {
    			exitStatus = deleteSite(siteStrings[0]);
    			logCommand("deleteSites");
    			resetArgList();
    		} else {
    			for (int i=0; i < siteStrings.length; i++) {
                    argMap.put(PREFIX_ARGUMENT_LONG + SITE_NAME_ARGUMENT,
                            siteStrings[i]);
                    argList.add(argMap);
                    commandList.add(createBatchCommand(
                            DELETE_SITE_SUBCOMMAND, argList));
                    argList.clear();
                    argMap.clear();
                }
                String attFile = createBatchfile(commandList);
                exitStatus = doBatch(attFile, "", false);
                logCommand("deleteSites");
                resetArgList();
            }
            if (exitStatus != SUCCESS_STATUS) {
            	allSitesDeleted = false;
                log(Level.SEVERE, "deleteSites", "The ssoadm command " +
                        " failed to delete sites.");
            }
        } else {
        	allSitesDeleted = false;
            log(Level.SEVERE, "deleteSites",
                    "The list of sites is empty.");
        }
    	return allSitesDeleted;
    }

    /**
     * Add server names arguments to the command.
     *
     * @param serverNames - names of the servers separated by ","
     */
    private void addServerNamesArguments(String serverNames) {
        String serverNamesArg;
        if (!serverNames.equals("")) {
            if (useLongOptions) {
            	//serverNamesArg = PREFIX_ARGUMENT_LONG + SERVER_NAMES_ARGUMENT;
            	serverNamesArg = "--servernames";
            } else {
            	serverNamesArg = PREFIX_ARGUMENT_SHORT +
                        SHORT_SERVER_NAMES_ARGUMENT;
            }
            addArgument(serverNamesArg);
            String[] servers = serverNames.split(",");
            for (String s : servers)
                addArgument(s);
        }
    }

    /**
     * Add members to a site.
     *
     * @param siteName - name of the site as string.
     * @param serverNames - servernames to be removed separated by ","
     * @return int - exitstatus of the subcommand "add-site-members"
     * @throws java.lang.Exception
     */
    public int addSiteMembers(String siteName, String serverNames)
    throws Exception {
        setSubcommand(ADD_SITE_MEMBERS_SUBCOMMAND);
        addSiteNameArguments(siteName);
        addServerNamesArguments(serverNames);
        addGlobalOptions();
        return executeCommand(commandTimeout);
    }

    /**
     * Display members of a site.
     *
     * @param siteName - name of the site as string.
     * @return int - exitstatus of the subcommand "show-site-members"
     * @throws java.lang.Exception
     */
    public int showSiteMembers(String siteName)
    throws Exception {
        setSubcommand(SHOW_SITE_MEMBERS_SUBCOMMAND);
        addSiteNameArguments(siteName);
        addGlobalOptions();
        return executeCommand(commandTimeout);
    }

    /**
     * Remove members of a site.
     *
     * @param siteName - name of the site as string.
     * @param serverNames - servernames to be removed separated by ","
     * @return int - exitstatus of the subcommand "remove-site-members"
     * @throws java.lang.Exception
     */
    public int removeSiteMembers(String siteName, String serverNames)
    throws Exception {
        setSubcommand(REMOVE_SITE_MEMBERS_SUBCOMMAND);
        addSiteNameArguments(siteName);
        addServerNamesArguments(serverNames);
        addGlobalOptions();
        return executeCommand(commandTimeout);
    }

    /**
     * Adds site secondary urls to site.
     *
     * @param siteName - name of the site as string.
     * @param siteSecUrls - siteSecUrls to be removed separated by ","
     * @return int - exitstatus of the subcommand "add-site-sec-urls"
     * @throws java.lang.Exception
     */
    public int addSiteSecUrls(String siteName, String siteSecUrls)
    throws Exception {
    	setSubcommand(ADD_SITE_SEC_URLS_SUBCOMMAND);
    	addSiteNameArguments(siteName);
    	addSecondaryUrlArguments(siteSecUrls);
    	addGlobalOptions();
    	return executeCommand(commandTimeout);
    }

    /**
     * Sets site secondary urls to site.
     *
     * @param siteName - name of the site as string.
     * @param siteSecUrls - siteSecUrls to be set separated by ","
     * @return int - exitstatus of the subcommand "set-site-sec-urls"
     * @throws java.lang.Exception
     */
    public int setSiteSecUrls(String siteName, String siteSecUrls)
    throws Exception {
    	setSubcommand(SET_SITE_SEC_URLS_SUBCOMMAND);
    	addSiteNameArguments(siteName);
    	addSecondaryUrlArguments(siteSecUrls);
    	addGlobalOptions();
    	return executeCommand(commandTimeout);
    }

    /**
     * Removes site secondary urls from site.
     *
     * @param siteName - name of the site as string.
     * @param siteSecUrls - siteSecUrls to be removed separated by ","
     * @return int - exitstatus of the subcommand "remove-site-sec-urls"
     * @throws java.lang.Exception
     */
    public int removeSiteSecUrls(String siteName, String siteSecUrls)
    throws Exception {
    	setSubcommand(REMOVE_SITE_SEC_URLS_SUBCOMMAND);
    	addSiteNameArguments(siteName);
    	addSecondaryUrlArguments(siteSecUrls);
    	addGlobalOptions();
    	return executeCommand(commandTimeout);
    }

    /**
     * Sets site url to site.
     *
     * @param siteName - name of the site as string.
     * @param siteUrl - siteUrl to be removed separated by ","
     * @return int - exitstatus of the subcommand "set-site-pri-url"
     * @throws java.lang.Exception
     */
    public int setSitePriUrl(String siteName, String siteUrl)
    throws Exception {
    	setSubcommand(SET_SITE_PRI_URL_SUBCOMMAND);
    	addSiteNameArguments(siteName);
    	addSiteUrlArguments(siteUrl);
    	addGlobalOptions();
    	return executeCommand(commandTimeout);
    }
    /**
     * Adds service to realm.
     * 
     * @param realmName - name of the realm.
     * @param serviceName - name of the service.
     * @param attributeValues - attribute value list as string.
     * @param useDatafile - if "true" creates attributes in form of list 
     *        and adds to the file. 
     * @return int - exit status of the command.
     * @throws Exception
     */
    public int addSvcRealm(String realmName, String serviceName,
    		String attributeValues, boolean useDatafile)
    throws Exception {
    	setSubcommand(ADD_SVC_REALM_SUBCOMMAND);
    	addRealmArguments(realmName);
    	addServiceNameArguments(serviceName);
        if (attributeValues != null) {
            if (useDatafile) {
                addDatafileArguments(attributeValues, "addSvcRealmAttrValues", 
                        ".txt");
            } else {
            	addAttributevaluesArguments(attributeValues);
            }
        }
        addGlobalOptions();
    	return executeCommand(commandTimeout);
    }
    
    /**
     * Get realm's service attribute values.
     * 
     * @param realmName - Name of realm.
     * @param serviceName - Name of service. 
     * @return int - exit status of the "get-realm-svc-attrs" sub-command. 
     * @throws Exception
     */
    public int getRealmSvcAttributes(String realmName, String serviceName)
    throws Exception {
    	setSubcommand(GET_REALM_SVC_ATTRS_SUBCOMMAND);
    	addRealmArguments(realmName);
    	addServiceNameArguments(serviceName);
    	addGlobalOptions();
    	return executeCommand(commandTimeout);
    }
    
    /**
     * List the assignable services to a realm.
     * 
     * @param realmName - Name of realm.
     * @return int - exit status of the "list-realm-assignable-svcs" sub-command.
     * @throws Exception
     */
    public int listRealmAssignableSvcs(String realmName)
    throws Exception {
    	setSubcommand(LIST_REALM_ASSIGNABLE_SVCS_SUBCOMMAND);
    	addRealmArguments(realmName);
    	addGlobalOptions();
    	return executeCommand(commandTimeout);
    }
    
    /**
     * Show services in a realm.
     * 
     * @param realmName - Name of the realm.
     * @param useMandatoryOption - if "true" shows mandatory services. 
     * @return int - exit status of the show-realm-svcs subcommand.
     * @throws Exception
     */
    public int showRealmSvcs(String realmName, boolean useMandatoryOption)
    throws Exception {
    	setSubcommand(SHOW_REALM_SVCS_SUBCOMMAND);
    	addRealmArguments(realmName);
    	addMandatoryArguments(useMandatoryOption);
    	addGlobalOptions();
    	return executeCommand(commandTimeout);
    }

    /**
     * Add mandatory argument/option.
     * 
     * @param mandatoryOption - Boolean value of mandatory option.
     * @throws Exception
     */
    private void addMandatoryArguments(boolean mandatoryOption)
    throws Exception {
    	String mandatoryArg;
    	if (mandatoryOption) {
    		if (useLongOptions) {
    			mandatoryArg = PREFIX_ARGUMENT_LONG + MANDATORY_ARGUMENT;
    		} else {
    			mandatoryArg = PREFIX_ARGUMENT_SHORT + SHORT_MANDATORY_ARGUMENT; 
    		}
    		addArgument(mandatoryArg);
    	}
    }
    
    /**
     * Remove service from a realm. 
     * 
     * @param realmName - Name of the realm.
     * @param serviceName - Name of the service.
     * @return int - exit status of the remove-realm-svcs subcommand.  
     * @throws Exception
     */
    public int removeSvcRealm(String realmName, String serviceName)
    throws Exception {
    	setSubcommand(REMOVE_SVC_REALM_SUBCOMMAND);
    	addRealmArguments(realmName);
    	addServiceNameArguments(serviceName);
    	addGlobalOptions();
    	return executeCommand(commandTimeout);
    }
}

