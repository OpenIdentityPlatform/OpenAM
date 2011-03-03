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
 * $Id: AdminXMLParser.java,v 1.2 2008/06/25 05:52:24 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.am.util.ServiceUtil;
import com.iplanet.services.ldap.Attr;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.admin.AdminInterfaceUtils;
import com.sun.identity.idm.IdType;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLHandler;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.PluginSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSSchema;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

class AdminXMLParser{
    
    private static ResourceBundle bundle = AdminResourceBundle.getResources();
    private Document document;
    private String fileName = "";
    private String outputFileName = null;
    private static boolean continueFlag = false;
    Debug debug = Debug.getInstance("amAdmin");
    
    private static final int CSO = 0;       //CreateSubOrganization
    private static final int CPC = 1;       //CreatePeopleContainer
    private static final int CG = 2;        //CreateGroup
    private static final int CR = 3;        //CreateRole
    private static final int GSO = 4;       //GetSubOrganizations
    private static final int GPC = 5;       //GetPeopleContainers
    private static final int GG = 6;        //GetGroups
    private static final int GR = 7;        //GetRoles
    private static final int GU = 8;        //GetUsers
    private static final int DSO = 9;       //DeleteSubOrganizations
    private static final int DPC = 10;      //DeletePeopleContainers
    private static final int DG = 11;       //DeleteGroups
    private static final int DR = 12;       //DeleteRoles
    private static final int CSPC = 13;     //CreateSubPeopleContainer
    private static final int CU = 14;       //CreateUser
    private static final int DSPC = 15;     //DeleteSubPeopleContainers
    private static final int DU = 16;       //DeleteUsers
    private static final int GNOU = 17;     //GetNumberOfUsers
    private static final int GSPC = 18;     //GetSubPeopleContainers
    private static final int AU = 19;       //AddUsers
    private static final int CSG = 20;      //CreateSubGroup
    private static final int DSG = 21;      //DeleteSubGroups
    private static final int GSG = 22;      //GetSubGroups
    private static final int RS = 23;       //RegisterServices
    private static final int URS = 24;      //UnregisterServices
    private static final int GRS = 27;      //GetRegisteredServiceNames
    private static final int GNOS = 29;     //GetNumberOfServices
    private static final int CSC = 30;      //CreateSubContainer
    private static final int GSC = 31;      //GetSubContainers
    private static final int DSC = 32;      //DeleteSubContainers
    private static final int CP = 33;       //CreatePolicy
    private static final int DP = 34;       //DeletePolicy
    private static final int MSO = 35;      //ModifySubOrganization
    private static final int MPC = 36;      //ModifyPeopleContainer
    private static final int MR = 37;       //ModifyRole
    private static final int CST = 38;      //CreateServiceTemplate
    private static final int MST = 39;      //ModifyServiceTemplate
    private static final int CRST = 42;     //CreateroleSTemplate
    private static final int MPCST = 43;    //ModifyePCSTemplate
    private static final int MRST = 44;     //ModifyeRoleSTemplate
    private static final int MSC = 45;      //ModifySubContainer
    private static final int MU = 49;       //ModifyUser
    private static final int MSG = 50;      //ModifySubGroup
    private static final int RDV = 51;      //RemoveDefaultValues
    private static final int MDV = 52;      //ModifyDefaultValues
    private static final int ATEV = 53;      //AddDefaultValues
    private static final int DST = 54;      //DeleteServiceTemplate
    private static final int DSV = 55;      //GetServiceDefaultValues
    private static final int ASC = 56;      //AddSubConfiguration
    private static final int RSC = 57;      //DeleteSubConfiguration
    private static final int ACV = 58;      //AddChoiceValues
    private static final int DCV = 59;      //RemoveChoiceValues
    private static final int DASC = 71;     //DeleteAllServiceConfiguration
    private static final int CC = 72;       //CreateContainer
    private static final int MT = 73;       //ModifyType
    private static final int MUIT = 74;     //ModifyUIType
    private static final int MIK = 75;      //Modifyi18nKey
    private static final int MS = 76;       //ModifySyntax
    private static final int APVB = 77;     //AddPropertiesViewBean
    private static final int ASR = 78;      //AddStartRange
    private static final int AER = 79;      //AddEndRange
    private static final int ASSC = 80;            //AddSubSchema
    private static final int AAS = 81;             //AddAttributeSchema
    private static final int RSS = 82;             //RemoveSubSchema
    private static final int RAS = 83;             //RemoveAttributeSchema
    private static final int CGC = 84;      //CreateGroupContainer
    private static final int RU = 85;      //RemoveUsers
    private static final int GST = 86;      //GetServiceTemplate
    private static final int RSTAV = 87;
        //RemoveServiceTemplateAttributeValues
    private static final int ASTAV = 88;
        //AddServiceTemplateAttributeValues
    private static final int RPDV = 89;      //RemovePartialDefaultValues
    private static final int CE = 90;        //CreateEntity
    private static final int DE = 91;        //DeleteEntities
    private static final int ME = 92;        //ModifyEntity
    private static final int GE = 93;        //GetEntities
    private static final int ANG = 94;       //AddNestedGroups
    private static final int RNG = 95;       //RemoveNestedGroups
    private static final int GNG = 96;       //GetNestedGroups
    private static final int GNONG = 97;     //GetNumberOfNestedGroups
    private static final int SPVBU = 98;     //SetPropertiesViewBeanURL
    private static final int MAA = 99;             //Modify Any Attribute
    private static final int GSRN = 100;     //GetServiceRevisionNumber
    private static final int SSRN = 101;     //SetServiceRevisionNumber
    private static final int MSCF = 102;     //ModifySubConfiguration
    private static final int MIA = 103;      //ModifyInheritanceAttribute
    private static final int SI18N = 104;    //SetI18nKey
    private static final int SCV = 105;      //SetChoiceValues
    private static final int GP = 106;       //GetPolicies
    private static final int ADD_PI = 107;   //AddPluginInterface
    private static final int SAV = 108;      //SetValidator
    private static final int GSCF = 109;     //GetSubConfiguration
    
    /* Federated Pack related constants:
     * Added the following constants:
     *       + CRPROV ==> CreateRemoteProvider
     *       + DPROV  ==> DeleteProvider
     *       + MRPROV  ==> ModifyRemoteProvider
     *       + CAD    ==> CreateAuthenticationDomain
     *       + MAD    ==> ModifyAuthenticationDomain
     *       + DAD    ==> DeleteAuthenticationDomain
     *       + GAD    ==> GetAuthenticationDomain
     *       + GPROV  ==> GetProvider
     *       + MHPROV ==> ModifyHostedProvider
     *       + LACCTS ==> ListAccts
     *
     * Dated: 20th Aug 2002
     * */
    private static final int CRPROV = 60;    //CreateRemoteProvider
    private static final int DPROV = 61;     //DeleteProvider
    private static final int CAD = 62;       //CreateAuthenticationDomain
    private static final int MAD = 63;       //ModifyAuthenticationDomain
    private static final int DAD = 64;       //DeleteAuthenticationDomain
    private static final int GAD = 65;       //GetAuthenticationDomain
    private static final int GPROV = 66;     //GetProvider
    private static final int MRPROV = 67;     //ModifyRemoteProvider
    private static final int MHPROV = 68;     //ModifyHostedProvider
    private static final int CHPROV = 69;     //CreateHostedProvider
    private static final int LACCTS = 70;     //ListAccts

    private static final int DSREALM = 110;   // DeleteRealm - Realm
    private static final int CSREALM = 111;   // CreateRealm - Realm
    private static final int GSREALM = 113;   // GetSubRealmNames - Realm
    private static final int GABSREALM = 114; // GetAssignableServices -Realm
    private static final int GADSREALM = 115; // GetAssignedServices - Realm
    private static final int RASREALM = 116;  // AssignService - Realm
    private static final int RUSREALM = 117;  // UnassignService - Realm
    private static final int GAREALM = 118;   // GetAttributes - Realm
    private static final int GSAREALM = 119;  // GetAttributes - Realm
    private static final int RMAREALM = 120;  // RemoveAttribute - Realm
    private static final int RMSREALM = 121;  // ModifyService - Realm
    private static final int RAAVREALM = 122; // AddAttributeValues - Realm
    private static final int RRAVREALM = 123; // RemoveAttr*Values - Realm
    private static final int RSAREALM = 124;  // SetAttributes - Realm
    private static final int CPREALM = 125;   // CreatePolicy - Realm
    private static final int DPREALM = 126;   // DeletePolicy - Realm
    private static final int GPREALM = 127;   // GetPolicies - Realm

    private static final int IDCRID = 130;    // CreateIdentity - IdRepo
    private static final int IDCRIDS = 131;   // CreateIdentities - IdRepo
    private static final int IDDELIDS = 132;  // DeleteIdentities - IdRepo
    private static final int IDSRCHIDS = 133; // SearchIdentities - IdRepo
    private static final int IDGAIDOPS = 134; // GetAllowedOperations - IdRepo
    private static final int IDGSIDTYPS = 135; // GetSupportedIdTypes - IdRepo

    private static final int IDGABSVCS = 140; // GetAssignableServices - IdRepo
    private static final int IDGADSVCS = 141; // GetAssignedServices - IdRepo
    private static final int IDGSVCATTR = 142; // GetServiceAttrs - IdRepo
    private static final int IDGETATTRS = 143; // GetAttributes - IdRepo
    private static final int IDGETMBRSHPS = 144; // GetMemberships - IdRepo
    private static final int IDISMEMBER = 145; // IsMember - IdRepo
    private static final int IDISACTIVE = 146; // IsActive - IdRepo
    private static final int IDGETMEMBERS = 147; // GetMembers - IdRepo
    private static final int IDADDMEMBER = 148; // AddMember - IdRepo
    private static final int IDRMMEMBER = 149; // RemoveMember - IdRepo
    private static final int IDASGNSVC = 150;  // AssignService - IdRepo
    private static final int IDUASGNSVC = 151; // UnassignService - IdRepo
    private static final int IDMODIFYSVC = 152; // ModifyService - IdRepo
    private static final int IDSETATTRS = 153; // SetAttributes - IdRepo
    private static final int DELEGATE_GET_PRIVILEGES = 154;
        // GetPrivileges - Delegation
    private static final int DELEGATE_ADD_PRIVILEGES = 155;
        // AddPrivileges - Delegation
    private static final int DELEGATE_REMOVE_PRIVILEGES = 156;
        // RemovePrivileges - Delegation

    //Set propertiesViewBeanURL for PluginSchema
    private static final int SET_PVU_FOR_PS = 160; 

    // Set boolean values
    private static final int SBV = 170;

    private static final String POLICY_NAME = "policyName";
    private static final String POLICY_XML_BEGIN = "<Policy";

    private static final String PI_NAME = "name";
    private static final String PI_INTERFACE_NAME = "interface";
    private static final String PI_I18N_KEY = "i18nKey";
    
    private static Map requestTypes = new HashMap();
    
    static {
        requestTypes.put("CreateSubOrganization", new Integer(CSO));
        requestTypes.put("CreatePeopleContainer", new Integer(CPC));
        requestTypes.put("CreateGroupContainer", new Integer(CGC));
        requestTypes.put("CreateGroup", new Integer(CG));
        requestTypes.put("CreateRole", new Integer(CR));
        requestTypes.put("GetSubOrganizations", new Integer(GSO));
        requestTypes.put("GetPeopleContainers", new Integer(GPC));
        requestTypes.put("GetGroups", new Integer(GG));
        requestTypes.put("GetRoles", new Integer(GR));
        requestTypes.put("GetUsers", new Integer(GU));
        requestTypes.put("DeleteSubOrganizations", new Integer(DSO));
        requestTypes.put("DeletePeopleContainers", new Integer(DPC));
        requestTypes.put("DeleteGroups", new Integer(DG));
        requestTypes.put("DeleteRoles", new Integer(DR));
        requestTypes.put("CreateSubPeopleContainer", new Integer(CSPC));
        requestTypes.put("CreateUser", new Integer(CU));
        requestTypes.put("ModifyUser", new Integer(MU));
        requestTypes.put("DeleteSubPeopleContainers", new Integer(DSPC));
        requestTypes.put("DeleteUsers", new Integer(DU));
        requestTypes.put("RemoveUsers", new Integer(RU));
        requestTypes.put("GetNumberOfUsers", new Integer(GNOU));
        requestTypes.put("GetSubPeopleContainers", new Integer(GSPC));
        requestTypes.put("AddUsers", new Integer(AU));
        requestTypes.put("CreateSubGroup", new Integer(CSG));
        requestTypes.put("ModifySubGroups", new Integer(MSG));
        requestTypes.put("RemoveDefaultValues", new Integer(RDV));
        requestTypes.put("RemovePartialDefaultValues", new Integer(RPDV));
        requestTypes.put("ModifyDefaultValues", new Integer(MDV));
        requestTypes.put("AddDefaultValues", new Integer(ATEV));
        requestTypes.put("ModifyType", new Integer(MT));
        requestTypes.put("AddPropertiesViewBean", new Integer(APVB));
        requestTypes.put("AddStartRange", new Integer(ASR));
        requestTypes.put("AddEndRange", new Integer(AER));
        requestTypes.put("ModifyUIType", new Integer(MUIT));
        requestTypes.put("SetBooleanValues", new Integer(SBV));
        requestTypes.put("ModifySyntax", new Integer(MS));
        requestTypes.put("ModifyAny", new Integer(MAA));
        requestTypes.put("Modifyi18nKey", new Integer(MIK));
        requestTypes.put("AddSubSchema", new Integer(ASSC));
        requestTypes.put("AddPluginInterface", new Integer(ADD_PI));
        requestTypes.put("AddAttributeSchema", new Integer(AAS));
        requestTypes.put("RemoveSubSchema", new Integer(RSS));
        requestTypes.put("RemoveAttributeSchema", new Integer(RAS));
        requestTypes.put("AddChoiceValues", new Integer(ACV));
        requestTypes.put("SetChoiceValues", new Integer(SCV));
        requestTypes.put("RemoveChoiceValues", new Integer(DCV));
        requestTypes.put("SetValidator", new Integer(SAV));
        requestTypes.put("DeleteSubGroups", new Integer(DSG));
        requestTypes.put("GetSubGroups", new Integer(GSG));
        requestTypes.put("RegisterServices", new Integer(RS));
        requestTypes.put("UnregisterServices", new Integer(URS));
        requestTypes.put("GetRegisteredServiceNames", new Integer(GRS));
        requestTypes.put("GetNumberOfServices", new Integer(GNOS));
        requestTypes.put("CreateSubContainer", new Integer(CSC));
        requestTypes.put("ModifySubContainer", new Integer(MSC));
        requestTypes.put("GetSubContainers", new Integer(GSC));
        requestTypes.put("DeleteSubContainers", new Integer(DSC));
        requestTypes.put("CreatePolicy", new Integer(CP));
        requestTypes.put("DeletePolicy", new Integer(DP));
        requestTypes.put("ModifySubOrganization", new Integer(MSO));
        requestTypes.put("ModifyPeopleContainer", new Integer(MPC));
        requestTypes.put("ModifyRole", new Integer(MR));
        requestTypes.put("CreateServiceTemplate", new Integer(CST));
        requestTypes.put("CreateRoleSTemplate", new Integer(CRST));
        requestTypes.put("GetServiceTemplate", new Integer(GST));
        requestTypes.put("ModifyServiceTemplate", new Integer(MST));
        requestTypes.put("AddServiceTemplateAttributeValues",
            new Integer(ASTAV));
        requestTypes.put("RemoveServiceTemplateAttributeValues",
            new Integer(RSTAV));
        requestTypes.put("DeleteServiceTemplate", new Integer(DST));
        requestTypes.put("GetServiceDefaultValues", new Integer(DSV));
        requestTypes.put("AddSubConfiguration", new Integer(ASC));
        requestTypes.put("ModifySubConfiguration", new Integer(MSCF));
        requestTypes.put("DeleteSubConfiguration", new Integer(RSC));
        requestTypes.put("GetSubConfiguration", new Integer(GSCF));
        requestTypes.put("DeleteAllServiceConfiguration", new Integer(DASC));
        requestTypes.put("ModifyPCSTemplate", new Integer(MPCST));
        requestTypes.put("ModifyRoleSTemplate", new Integer(MRST));
        requestTypes.put("CreateContainer", new Integer(CC));
        requestTypes.put("CreateEntity", new Integer(CE));
        requestTypes.put("DeleteEntities", new Integer(DE));
        requestTypes.put("ModifyEntity", new Integer(ME));
        requestTypes.put("GetEntities", new Integer(GE));
        requestTypes.put("AddNestedGroups", new Integer(ANG));
        requestTypes.put("RemoveNestedGroups", new Integer(RNG));
        requestTypes.put("GetNestedGroups", new Integer(GNG));
        requestTypes.put("GetNumberOfNestedGroups", new Integer(GNONG));
        requestTypes.put("SetPropertiesViewBeanURL", new Integer(SPVBU));
        requestTypes.put("GetServiceRevisionNumber", new Integer(GSRN));
        requestTypes.put("SetServiceRevisionNumber", new Integer(SSRN));
        requestTypes.put("ModifyInheritanceAttribute", new Integer(MIA));
        requestTypes.put("SetI18nKey", new Integer(SI18N));
        
        requestTypes.put("CreateRemoteProvider", new Integer(CRPROV));
        requestTypes.put("DeleteProvider", new Integer(DPROV));      
        requestTypes.put("CreateAuthenticationDomain", new Integer(CAD));
        requestTypes.put("ModifyAuthenticationDomain", new Integer(MAD));
        requestTypes.put("DeleteAuthenticationDomain", new Integer(DAD));
        requestTypes.put("GetAuthenticationDomain", new Integer(GAD));
        requestTypes.put("GetProvider", new Integer(GPROV));
        requestTypes.put("ModifyRemoteProvider", new Integer(MRPROV));
        requestTypes.put("ModifyHostedProvider", new Integer(MHPROV));
        requestTypes.put("CreateHostedProvider", new Integer(CHPROV));
        requestTypes.put("ListAccts", new Integer(LACCTS));
        requestTypes.put("GetPolicies", new Integer(GP));

        requestTypes.put("CreateRealm", new Integer(CSREALM));
        requestTypes.put("DeleteRealm", new Integer(DSREALM));
        requestTypes.put("GetSubRealmNames", new Integer(GSREALM));
        requestTypes.put("GetAssignableServices", new Integer(GABSREALM));
        requestTypes.put("GetAssignedServices", new Integer(GADSREALM));
        requestTypes.put("AssignService", new Integer(RASREALM));
        requestTypes.put("UnassignService", new Integer(RUSREALM));
        requestTypes.put("GetAttributes", new Integer(GAREALM));
        requestTypes.put("GetServiceAttributes", new Integer(GSAREALM));
        requestTypes.put("RemoveAttribute", new Integer(RMAREALM));
        requestTypes.put("ModifyService", new Integer(RMSREALM));
        requestTypes.put("AddAttributeValues", new Integer(RAAVREALM));
        requestTypes.put("RemoveAttributeValues", new Integer(RRAVREALM));
        requestTypes.put("SetAttributes", new Integer(RSAREALM));
        requestTypes.put("RealmCreatePolicy", new Integer(CPREALM));
        requestTypes.put("RealmDeletePolicy", new Integer(DPREALM));
        requestTypes.put("RealmGetPolicies", new Integer(GPREALM));
        requestTypes.put("CreateIdentity", new Integer(IDCRID));
        requestTypes.put("CreateIdentities", new Integer(IDCRIDS));
        requestTypes.put("DeleteIdentities", new Integer(IDDELIDS));
        requestTypes.put("SearchIdentities", new Integer(IDSRCHIDS));
        requestTypes.put("GetAllowedIdOperations", new Integer(IDGAIDOPS));
        requestTypes.put("GetSupportedIdTypes", new Integer(IDGSIDTYPS));
        requestTypes.put("IdGetAssignableServices", new Integer(IDGABSVCS));
        requestTypes.put("IdGetAssignedServices", new Integer(IDGADSVCS));
        requestTypes.put("IdGetServiceAttrs", new Integer(IDGSVCATTR));
        requestTypes.put("IdGetAttributes", new Integer(IDGETATTRS));
        requestTypes.put("IdGetMemberships", new Integer(IDGETMBRSHPS));
        requestTypes.put("IdIsMember", new Integer(IDISMEMBER));
        requestTypes.put("IdIsActive", new Integer(IDISACTIVE));
        requestTypes.put("IdGetMembers", new Integer(IDGETMEMBERS));
        requestTypes.put("IdAddMember", new Integer(IDADDMEMBER));
        requestTypes.put("IdRemoveMember", new Integer(IDRMMEMBER));
        requestTypes.put("IdAssignService", new Integer(IDASGNSVC));
        requestTypes.put("IdUnassignService", new Integer(IDUASGNSVC));
        requestTypes.put("IdModifyService", new Integer(IDMODIFYSVC));
        requestTypes.put("IdSetAttributes", new Integer(IDSETATTRS));
        requestTypes.put("SetPropertiesViewBeanURLForPluginSchema", 
                new Integer(SET_PVU_FOR_PS));
        requestTypes.put("GetPrivileges", new Integer(DELEGATE_GET_PRIVILEGES));
        requestTypes.put("AddPrivileges", new Integer(DELEGATE_ADD_PRIVILEGES));
        requestTypes.put("RemovePrivileges",
            new Integer(DELEGATE_REMOVE_PRIVILEGES));
    }
    
    
    /**
     * @param arg is the string representation of the request type
     * @return int is the integer value corresponding to the
     * string representation of the request type
     */
    private static int getToken(String arg){
        return(((Integer)requestTypes.get(arg)).intValue());
    }
    
    
    /**
     * @param strFileName is the filename  of the XML file to be parsed
     * @return Document is the DOM object obtained by parsing an XML file
     *
     * This method parses the XML file and converts the parsed data
     * into a DOM object.
     */
    private Document parseXML(String strFileName)
        throws AdminException
    {
        String docTypeName = "Requests";
        Document domObject = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(true);
        
        try{
            DocumentBuilder  builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ValidationErrorHandler());
            builder.setEntityResolver(new XMLHandler());
            domObject = builder.parse(new File(strFileName));
            
            DocumentType docType = domObject.getDoctype();
            String docTypeXML = docType.getName();
            if (!docTypeXML.equalsIgnoreCase(docTypeName)) {
                throw new AdminException(bundle.getString("parseerr") +
                    fileName + "\n" + bundle.getString("doctypeerror"));
            }
        } catch(SAXParseException spe) {
            //Error generated by the Parser
            if (AdminUtils.logEnabled() && (AdminUtils.debugEnabled))
                AdminUtils.log(bundle.getString("parseerr") + fileName +
                    "/n" + spe);
            throw new AdminException(bundle.getString("parseerr") + fileName +
                "/n" + spe);
        } catch(SAXException se) {
            //Error genearted by this application OR a
            //parser-initailization error
            if (AdminUtils.logEnabled() && (AdminUtils.debugEnabled))
                AdminUtils.log(bundle.getString("parseiniterr") + "\n" + se);
            throw new AdminException(bundle.getString("parseiniterr") +
                "\n" + se);
        } catch(ParserConfigurationException pce) {
            //Error, if Parser with specified options can not be built
            if (AdminUtils.logEnabled() && (AdminUtils.debugEnabled))
                AdminUtils.log(bundle.getString("parsebuilterr") + "n" +pce);
            throw new AdminException(bundle.getString("parsebuilterr") +
                "\n" + pce);
        } catch(IOException ioe) {
            if (AdminUtils.logEnabled() && (AdminUtils.debugEnabled))
                AdminUtils.log(bundle.getString("ioexception") + fileName +
                    " .", ioe);
            throw new AdminException(ioe);
        }
        
        return domObject;
    }
    
    /**
     * @param strFileName is the File name of the XML file to be parsed.
     * @param connection AMStoreConnection to the Sun Java System Identity
     *        Server SDK
     * @param oFileName is the output File Name.  Can be null.
     *
     * This method takes the name of XML file, process each request object
     * one by one immediately after parsing.
     */
    public void processAdminReqs(String strFileName,
        AMStoreConnection connection, SSOToken ssot, boolean continueFlag,
        String oFileName)
        throws AdminException
    {
        fileName = strFileName;
        outputFileName = oFileName;
        document = parseXML(strFileName);
        AdminXMLParser.continueFlag = continueFlag;
        System.gc();
        
        // topElement is <Requests>
        Element topElement = document.getDocumentElement();
        NodeList childElements = topElement.getChildNodes();
        int numChildElements = childElements.getLength();

        for (int i = 0; i < numChildElements; i++) {
            Node node = childElements.item(i);

            if ((node != null) &&  (node.getNodeType() == Node.ELEMENT_NODE)) {
                String elementName = node.getNodeName();

                try {
                    if (elementName.equals("OrganizationRequests")) {
                        doOrganizationRequests(node, connection, ssot);
                    } else if (elementName.equals("ContainerRequests")) {
                        doContainerRequests(node, connection, ssot);
                    } else if (elementName.equals("PeopleContainerRequests")) {
                        doPeopleContainerRequests(node, connection, ssot);
                    } else if (elementName.equals("RoleRequests")) {
                        doRoleRequests(node, connection, ssot);
                    } else if (elementName.equals("GroupRequests")) {
                        doGroupRequests(node, connection);
                    } else if (elementName.equals("SchemaRequests")) {
                        doSchemaRequests(node, ssot);
                    } else if (elementName.equals("SchemaRootNodeRequests")) {
                        doSchemaRootNodeRequests(node, ssot);
                    } else if (elementName.equals(
                        "ServiceConfigurationRequests")
                    ) {
                        doServiceConfigurationRequests(node, ssot);
/* Federation: Commented out
                    } else if (elementName.equals("ListAccts")){
                        doListAccts(node);
*/
                    } else if (elementName.equals("UserRequests")) {
                        doUserRequests(node, connection);
                    } else if (elementName.equals("RealmRequests")) {
                        doRealmRequests(node, ssot);
                    } else if (elementName.equals("IdentityRequests")) {
                        doIdentityRequests(node, ssot);
                    } else if (elementName.equals("DelegationRequests")) {
                        doDelegationRequests(node, ssot);
                    }
                } catch (AdminException ae) {
                    String[] args = {elementName, ae.getMessage()};
                    AdminUtils.logOperation(AdminUtils.LOG_ERROR,
                        Level.INFO, AdminUtils.ADMIN_EXCEPTION, args);

                    if (ae.isFatal() || !continueFlag) {
                        throw ae;
                    } else if (AdminUtils.logEnabled()) {
                        AdminUtils.log(ae.toString());
                    }
                }
            }
        }
    }

    private void doOrganizationRequests(Node node, AMStoreConnection connection,
        SSOToken ssot)
        throws AdminException
    {
        Node organizationNode = node;
        String orgDN =((Element)organizationNode).getAttribute("DN");
        validateObjectType(connection, orgDN, AMObject.ORGANIZATION);
        NodeList orgChildNodesList = organizationNode.getChildNodes();
        int orgChildNodesListLength = orgChildNodesList.getLength();

        for (int i = 0; i < orgChildNodesListLength; i++) {
            Node childNode = orgChildNodesList.item(i);

            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                String strChildNodeName = childNode.getNodeName();
                int opt = getToken(strChildNodeName);

                try {
                    switch(opt){
                    case CSO:
                        createSubOrganizationInOrganization(orgDN, childNode,
                            connection);
                        break;
                    case MSO:
                        modifySubOrganizationInOrganization(orgDN, childNode,
                            connection);
                        break;
                    case CP:
                        createPolicy(connection, orgDN, childNode, ssot);
                        break;
                    case DP:
                        deletePolicy(orgDN, childNode, ssot);
                        break;
                    case CPC:
                        createPeopleContainerInOrganization(orgDN, childNode,
                            connection);
                        break;
                    case CGC:
                        createGroupContainerInOrganization(orgDN, childNode,
                            connection);
                        break;
                    case MPC:
                        modifyPeopleContainerInOrganization(orgDN, childNode,
                            connection);
                        break;
                    case GST:
                        getServiceTemplateInOrganization(orgDN, childNode,
                            connection);
                        break;
                    case MST:
                        modifyServiceTemplateInOrganization(orgDN, childNode,
                            connection, ssot);
                        break;
                    case ASTAV:
                        addServiceTemplateAttrValuesInOrganization(orgDN,
                            childNode, connection, ssot);
                        break;
                    case RSTAV:
                        removeServiceTemplateAttrValuesInOrganization(orgDN,
                            childNode, connection, ssot);
                        break;
                    case DST:
                        deleteServiceTemplateInOrganization(orgDN, childNode,
                            connection, ssot);
                        break;
                    case CG:
                        createGroupInOrganization(orgDN, childNode, connection);
                        break;
                    case CU:
                        createUserInOrganization(
                            orgDN, childNode, connection, ssot);
                        break;
                    case CR:
                        createRoleInOrganization(orgDN, childNode, connection);
                        break;
                    case MR:
                        modifyRole(orgDN, childNode, connection);
                        break;
                    case GSO:
                        getSubOrganizationsInOrganization(orgDN, childNode,
                            connection);
                        break;
                    case GPC:
                        getPeopleContainersInOrganization(orgDN, childNode,
                            connection);
                        break;
                    case GG:
                        getGroupsInOrganization(orgDN, childNode, connection);
                        break;
                    case GR:
                        getRolesInOrganization(orgDN, childNode, connection);
                        break;
                    case GU:
                        getUserInOrganization(orgDN, childNode, connection);
                        break;
                    case DSO:
                        deleteSubOrganizationsInOrganization(orgDN, childNode,
                            connection);
                        break;
                    case DPC:
                        deletePeopleContainersInOrganization(orgDN, childNode,
                            connection);
                        break;
                    case DG:
                        deleteGroupsInOrganization(orgDN, childNode,
                            connection);
                        break;
                    case DR:
                        deleteRolesInOrganization(orgDN, childNode,
                            connection);
                        break;
                    case RS:
                        registerServicesInOrganization(orgDN, childNode,
                            connection);
                        break;
                    case URS:
                        unregisterServicesInOrganization(orgDN, childNode,
                            connection);
                        break;
                    case GRS:
                        getRegisteredServicesInOrganization(orgDN, connection);
                        break;
                    case GNOS:
                        getNumberOfServicesInOrganization(orgDN, connection);
                        break;
                    case CST:
                        createServiceTemplatesInOrganization(orgDN, childNode,
                            connection, ssot);
                        break;
                    case CE:
                        createEntityInOrganization(orgDN, childNode,
                            connection, ssot);
                        break;
                    case ME:
                        modifyEntityInOrganization(orgDN, childNode, connection);
                        break;
                    case DE:
                        deleteEntitiesInOrganization(orgDN, childNode, connection);
                        break;
                    case GE:
                        getEntitiesInOrganization(orgDN, childNode, connection);
                        break;
                    case CC:
                        createSubContainersInOrganization(orgDN, childNode,
                            connection);
                        break;
                    case ASC:
                        String serviceName = ((Element)childNode).getAttribute(
                            "serviceName");
                        addSubConfigurationsInOrganization(orgDN, serviceName,
                            childNode, ssot);
                        break;
                    case MSCF:
                        modifySubConfigurationsInOrganization(orgDN, childNode,
                            ssot);
                        break;
                    case RSC:
                        String sName = ((Element)childNode).getAttribute(
                            "serviceName");
                        removeSubConfigurationsInOrganization(orgDN, sName,
                            childNode, ssot);
                        break;
/* Federation: Commented out
                    case CRPROV:
                        createRemoteProvider(orgDN, childNode, ssot);
                        break;
                    case MRPROV:
                        modifyRemoteProvider(orgDN, childNode, ssot);
                        break;
                    case MHPROV:
                        modifyHostedProvider(orgDN, childNode, ssot);
                        break;
                    case CHPROV:
                        createHostedProvider(orgDN, childNode, ssot);
                        break;
                    case DPROV:
                        deleteHostedProvider(orgDN, childNode, ssot);
                        break;
                    case GPROV:
                        getFederationProvider(orgDN, childNode, ssot);
                        break;
                    case CAD:
                        createFederationAuthenticationDomain(orgDN, childNode,
                            ssot);
                        break;
                    case DAD:
                        deleteFederationAuthenticationDomain(orgDN, childNode,
                            ssot);
                        break;
                    case GAD:
                        getFederationAuthenticationDomain(orgDN, childNode,
                            ssot);
                        break;
                    case MAD:
                        modifyFederationAuthenticationDomain(orgDN, childNode,
                            ssot);
                        break;
*/
                    case GP:
                        getPoliciesInOrganization(orgDN, childNode, ssot);
                        break;
                    }
                } catch (AdminException ae) {
                    if (!continueFlag) {
                        throw ae;
                    }
                }
            }
        }
    }

    private void createSubOrganizationInOrganization(String orgDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        OrgCreateSubOrgReq dpOrgCreateSubOrgReq =
            new OrgCreateSubOrgReq(orgDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element)childNode).getAttribute("createDN");
        dpOrgCreateSubOrgReq.addSubOrgReq(createDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgCreateSubOrgReq.toString());
        }

        dpOrgCreateSubOrgReq.process(connection);
    }

    private void modifySubOrganizationInOrganization(String orgDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        OrgModifySubOrgReq dpOrgModifySubOrgReq = new OrgModifySubOrgReq(orgDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String modifyDN = ((Element)childNode).getAttribute("modifyDN");
        dpOrgModifySubOrgReq.addSubOrgReq(modifyDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgModifySubOrgReq.toString());
        }

        dpOrgModifySubOrgReq.process(connection);
    }

    private void createPolicy(AMStoreConnection connection,
        String orgDN, Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        doCreatePolicy(connection, childNode, ssoToken);
        String policyName = XMLUtils.getNodeAttributeValue(
            XMLUtils.getChildNode(childNode,"Policy"), "name");
        String[] args = {policyName, orgDN};
//      AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//          MessageFormat.format(bundle.getString("create-policy"), args));
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, AdminUtils.CREATE_POLICY, args);
    }

    private void deletePolicy(String orgDN, Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        doDeletePolicy(childNode, ssoToken);
        String policyName = XMLUtils.getNodeAttributeValue(
            XMLUtils.getChildNode(childNode,"PolicyName"), "name");
        String[] args = {policyName, orgDN};
//      AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//          MessageFormat.format(bundle.getString("delete-policy"), args));
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, AdminUtils.DELETE_POLICY, args);
    }

    private void createPeopleContainerInOrganization(String orgDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        OrgCreatePCReq dpOrgCreatePCReq = new OrgCreatePCReq(orgDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element)childNode).getAttribute("createDN");
        dpOrgCreatePCReq.addPCReq(createDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgCreatePCReq.toString());
        }

        dpOrgCreatePCReq.process(connection);
    }

    private void createGroupContainerInOrganization(String orgDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        OrgCreateGroupContainerReq request =
            new OrgCreateGroupContainerReq(orgDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element)childNode).getAttribute("createDN");
        request.addGroupContainerRequest(createDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(request.toString());
        }

        request.process(connection);
    }

    private void modifyPeopleContainerInOrganization(String orgDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        OrgModifyPCReq dpOrgModifyPCReq = new OrgModifyPCReq(orgDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String modifyDN = ((Element)childNode).getAttribute("modifyDN");
        dpOrgModifyPCReq.addPCReq(modifyDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgModifyPCReq.toString());
        }

        dpOrgModifyPCReq.process(connection);
    }

    private void getServiceTemplateInOrganization(String orgDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        OrgGetServiceTemplateReq dpOrgGetServiceTemplateReq =
            new OrgGetServiceTemplateReq(orgDN);
        String serviceName = ((Element)childNode).getAttribute("serviceName");
        String schemaType = ((Element)childNode).getAttribute("schemaType");
        dpOrgGetServiceTemplateReq.setServiceNameSchemaType(
            serviceName, schemaType);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgGetServiceTemplateReq.toString());
        }

        dpOrgGetServiceTemplateReq.process(connection);
    }

    private void modifyServiceTemplateInOrganization(String orgDN,
        Node childNode, AMStoreConnection connection, SSOToken ssoToken)
        throws AdminException
    {
        OrgModifyServiceTemplateReq dpOrgModifyServiceTemplateReq =
            new OrgModifyServiceTemplateReq(orgDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String serviceName = ((Element)childNode).getAttribute("serviceName");
        String schemaType = ((Element)childNode).getAttribute("schemaType");
        String level = ((Element)childNode).getAttribute("level");
        String roleTemplate = ((Element)childNode).getAttribute("roleTemplate");

        if (!schemaType.equalsIgnoreCase("dynamic") &&
            roleTemplate.equalsIgnoreCase("true")
        ) {
            throw new UnsupportedOperationException(
                bundle.getString("roletemplateexception"));
        }

        dpOrgModifyServiceTemplateReq.addServiceTemplateReq(serviceName,
            schemaType, level, roleTemplate, map, ssoToken);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgModifyServiceTemplateReq.toString());
        }

        dpOrgModifyServiceTemplateReq.process(connection);
    }

    private void addServiceTemplateAttrValuesInOrganization(
        String orgDN,
        Node childNode,
        AMStoreConnection connection,
        SSOToken ssoToken
    ) throws AdminException
    {
        OrgAddServiceTemplateAttrValuesReq request =
            new OrgAddServiceTemplateAttrValuesReq(orgDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String serviceName = ((Element)childNode).getAttribute("serviceName");
        String schemaType = ((Element)childNode).getAttribute("schemaType");
        String level = ((Element)childNode).getAttribute("level");
        String roleTemplate = ((Element)childNode).getAttribute("roleTemplate");

        if (!schemaType.equalsIgnoreCase("dynamic") &&
            roleTemplate.equalsIgnoreCase("true"))
        {
            throw new UnsupportedOperationException(
                bundle.getString("roletemplateexception"));
        }

        request.addRequest(serviceName, schemaType, level, roleTemplate,
            map, ssoToken);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(request.toString());
        }

        request.process(connection);
    }

    private void removeServiceTemplateAttrValuesInOrganization(
        String orgDN,
        Node childNode,
        AMStoreConnection connection,
        SSOToken ssoToken
    ) throws AdminException
    {
        OrgRemoveServiceTemplateAttrValuesReq request =
            new OrgRemoveServiceTemplateAttrValuesReq(orgDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String serviceName = ((Element)childNode).getAttribute("serviceName");
        String schemaType = ((Element)childNode).getAttribute("schemaType");
        String level = ((Element)childNode).getAttribute("level");
        String roleTemplate = ((Element)childNode).getAttribute("roleTemplate");

        if (!schemaType.equalsIgnoreCase("dynamic") &&
            roleTemplate.equalsIgnoreCase("true"))
        {
            throw new UnsupportedOperationException(
                bundle.getString("roletemplateexception"));
        }

        request.addRequest(serviceName, schemaType, level, roleTemplate,
            map, ssoToken);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(request.toString());
        }

        request.process(connection);
    }

    private void deleteServiceTemplateInOrganization(String orgDN,
        Node childNode, AMStoreConnection connection, SSOToken ssoToken)
        throws AdminException
    {
        OrgDeleteServiceTemplateReq dpOrgDeleteServiceTemplateReq
            = new OrgDeleteServiceTemplateReq(orgDN);
        String serviceName = ((Element)childNode).getAttribute("serviceName");
        String schemaType = ((Element)childNode).getAttribute("schemaType");
        dpOrgDeleteServiceTemplateReq.setServiceTemplateReq(
            serviceName, schemaType);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgDeleteServiceTemplateReq.toString());
        }

        dpOrgDeleteServiceTemplateReq.process(connection, ssoToken);
    }

    private void createGroupInOrganization(String orgDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        OrgCreateGroupReq dpOrgCreateGroups = new OrgCreateGroupReq(orgDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element)childNode).getAttribute("createDN");
        String groupType = ((Element)childNode).getAttribute("groupType");
        dpOrgCreateGroups.addGroupReq(createDN, groupType, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgCreateGroups.toString());
        }

        dpOrgCreateGroups.process(connection);
    }

    private void createUserInOrganization(
        String orgDN,
        Node childNode,
        AMStoreConnection connection,
        SSOToken ssoToken
    ) throws AdminException 
    {
        OrgCreateUserReq dpOrgCreateUser = new OrgCreateUserReq(
            orgDN, ssoToken);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element) childNode).getAttribute("createDN");
        dpOrgCreateUser.addUserReq(createDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgCreateUser.toString());
        }
        dpOrgCreateUser.process(connection);
    }

    private void createRoleInOrganization(String orgDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        OrgCreateRoleReq reqHandler = null;
        String roleType = ((Element)childNode).getAttribute("roleType");

        if (roleType.equals(AdminReq.ROLE_TYPE_FILTERED)) {
            reqHandler = new OrgCreateFilteredRoleReq(orgDN);
        } else {
            reqHandler = new OrgCreateRoleReq(orgDN);
        }

        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element)childNode).getAttribute("createDN");
        reqHandler.addRoleReq(createDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(reqHandler.toString());
        }

        reqHandler.process(connection);
    }

    private void modifyRole(String orgDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        OrgModifyRoleReq dpOrgModifyRoles = new OrgModifyRoleReq(orgDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String modifyDN = ((Element)childNode).getAttribute("modifyDN");
        dpOrgModifyRoles.addRoleReq(modifyDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgModifyRoles.toString());
        }

        dpOrgModifyRoles.process(connection);
    }

    private void getSubOrganizationsInOrganization(String orgDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        OrgGetSubOrgReq dpOrgGetSubOrgReq = new OrgGetSubOrgReq(orgDN);
        String strDNsOnly = ((Element)childNode).getAttribute("DNsOnly");
        boolean DNsOnly = Boolean.valueOf(strDNsOnly).booleanValue();
        dpOrgGetSubOrgReq.setDNsOnly(DNsOnly);
        dpOrgGetSubOrgReq.setFilter(
            ((Element)childNode).getAttribute("filter"));
        dpOrgGetSubOrgReq.setSizeLimit(
            ((Element)childNode).getAttribute("sizeLimit"));
        dpOrgGetSubOrgReq.setTimeLimit(
            ((Element)childNode).getAttribute("timeLimit"));
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            dpOrgGetSubOrgReq.addSubOrgDNs((String)iter.next());
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgGetSubOrgReq.toString());
        }

        dpOrgGetSubOrgReq.process(connection);
    }

    private void getPeopleContainersInOrganization(String orgDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        OrgGetPCReq dpOrgGetPCReq = new OrgGetPCReq(orgDN);
        String strDNsOnly = ((Element)childNode).getAttribute("DNsOnly");
        boolean DNsOnly = Boolean.valueOf(strDNsOnly).booleanValue();
        dpOrgGetPCReq.setDNsOnly(DNsOnly);
        dpOrgGetPCReq.setFilter(((Element)childNode).getAttribute("filter"));
        dpOrgGetPCReq.setSizeLimit(
            ((Element)childNode).getAttribute("sizeLimit"));
        dpOrgGetPCReq.setTimeLimit(
            ((Element)childNode).getAttribute("timeLimit"));
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            dpOrgGetPCReq.addPCDNs((String)iter.next());
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgGetPCReq.toString());
        }

        dpOrgGetPCReq.process(connection);
    }

    private void getGroupsInOrganization(String orgDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        OrgGetGroupReq dpOrgGetGroups = new OrgGetGroupReq(orgDN);
        dpOrgGetGroups.setLevel(validateLevel(childNode));
        dpOrgGetGroups.setFilter(((Element)childNode).getAttribute("filter"));
        dpOrgGetGroups.setSizeLimit(
            ((Element)childNode).getAttribute("sizeLimit"));
        dpOrgGetGroups.setTimeLimit(
            ((Element)childNode).getAttribute("timeLimit"));

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgGetGroups.toString());
        }

        dpOrgGetGroups.process(connection);
    }

    private void getRolesInOrganization(String orgDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        OrgGetRoleReq dpOrgGetRoles = new OrgGetRoleReq(orgDN);
        dpOrgGetRoles.setLevel(validateLevel(childNode));
        dpOrgGetRoles.setFilter(((Element)childNode).getAttribute("filter"));
        dpOrgGetRoles.setSizeLimit(
            ((Element)childNode).getAttribute("sizeLimit"));
        dpOrgGetRoles.setTimeLimit(
            ((Element)childNode).getAttribute("timeLimit"));

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgGetRoles.toString());
        }

        dpOrgGetRoles.process(connection);
    }

    private void getUserInOrganization(String orgDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        OrgGetUserReq dpOrgGetUsers = new OrgGetUserReq(orgDN);
        String strDNsOnly = ((Element)childNode).getAttribute("DNsOnly");
        boolean DNsOnly = Boolean.valueOf(strDNsOnly).booleanValue();
        dpOrgGetUsers.setDNsOnly(DNsOnly);
        dpOrgGetUsers.setFilter(((Element)childNode).getAttribute("filter"));
        dpOrgGetUsers.setSizeLimit(
            ((Element)childNode).getAttribute("sizeLimit"));
        dpOrgGetUsers.setTimeLimit(
            ((Element)childNode).getAttribute("timeLimit"));

        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            dpOrgGetUsers.addUserDNs((String)iter.next());
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgGetUsers.toString());
        }

        dpOrgGetUsers.process(connection);
    }

    private void getPoliciesInOrganization(String orgDN, Node childNode,
        SSOToken token)
        throws AdminException
    {

        if (debug.messageEnabled()) {
            debug.message("AdminXMLParser.getPoliciesInOrganization():"
                    + "entering with orgDN=" + orgDN);
        }
        try {

            Set policyNames = new HashSet();

            //TODO:add logging

            if (debug.messageEnabled()) {
                debug.message("AdminXMLParser.getPoliciesInOrganization():"
                        + "principal=" + token.getPrincipal().getName());
            }

            PolicyManager pm = new PolicyManager(token, orgDN);
            Map map = XMLUtils.parseAttributeValuePairTags(childNode);
            Set pNames = (Set)map.get(POLICY_NAME);

            if(pNames == null) {
                //print all policies
                policyNames = pm.getPolicyNames();
            } else {
                policyNames = new HashSet();
                Iterator iter = pNames.iterator();
                while (iter.hasNext()) {
                    String pName = (String)iter.next();
                    policyNames.addAll(pm.getPolicyNames(pName));
                }
            }
            
            FileOutputStream fout = null;
            PrintWriter pwout = null;
            if ( (policyNames != null) && !policyNames.isEmpty()) {
                Iterator iter = policyNames.iterator();
                if (outputFileName != null) {
                    try {
                        fout = new FileOutputStream(outputFileName, true);
                        pwout = new PrintWriter(fout, true);
                    } catch (FileNotFoundException fnfex) {
                        debug.error("AdminXMLParser:" +
                            "getPolicyInOrganization():error opening file: " +
                            outputFileName + "; " + fnfex.getMessage());
                        System.err.println("AdminXMLParser:" +
                            "getPolicyInOrganization():error opening file: " +
                            outputFileName + "; " + fnfex.getMessage());
                        System.err.println("Output only to stdout.");
                        outputFileName = null;
                        fout = null;
                    } catch (SecurityException secex) {
                        debug.error("AdminXMLParser:" +
                            "getPolicyInOrganization():error writing: " +
                            outputFileName + "; " + secex.getMessage());
                        System.err.println("AdminXMLParser:" +
                            "getPolicyInOrganization():error writing: " +
                            outputFileName + "; " + secex.getMessage());
                        System.err.println("Output only to stdout.");
                        outputFileName = null;
                        fout.close();
                        fout = null;
                    }
                }
                while (iter.hasNext()) {
                    String policyName = (String)iter.next();
                     if (debug.messageEnabled()) {
                        debug.message("AdminXMLParser."
                                + "getPolicyInOrganization():"
                                + "printing policy : " 
                                + policyName);
                     }
                    Policy policy = pm.getPolicy(policyName);

                    //TODO:add logging
                    String policyXML = policy.toXML();

                    // exclude the xml prolog line
                    System.out.println(
                            policyXML.substring(policyXML.indexOf(
                            POLICY_XML_BEGIN)));
                    System.out.println(); //for readability of output

                    // if outputFileName specified, output it there, too

                    if (pwout != null) {
                        pwout.write(policyXML);
                    }

                }
            }
            if (pwout != null) {
                pwout.close();
            }
            if (fout != null) {
                fout.close();
            }

        } catch (Exception e) {
            //print error message and exit
            debug.error("AdminXMLParser.getPoliciyInOrganization():"
                    + "reported exception");
            System.err.println(bundle.getString(
                    "exception_while_exporting_policy"));
            e.printStackTrace();
            throw new AdminException(e);
        }
        if (debug.messageEnabled()) {
            debug.message("AdminXMLParser.getPoliciesInOrganization():"
                    + "returning");
        }
    }

    private void addPluginInterface(String serviceName, Node childNode,
        SSOToken token)
        throws AdminException
    {

        if (debug.messageEnabled()) {
            debug.message("AdminXMLParser.addPluginInterface():entering");
        }
        try {

            String name = ((Element)childNode).getAttribute(PI_NAME);
            String interfaceName = ((Element)childNode)
                    .getAttribute(PI_INTERFACE_NAME);
            String i18nKey = ((Element)childNode).getAttribute(PI_I18N_KEY);
            ServiceSchemaManager ssm = new ServiceSchemaManager(
                serviceName, token);
            if (debug.messageEnabled()) {
                debug.message("AdminXMLParser.addPluginInterface():"
                        + "adding plugin interface: " + name
                        + " ,interfaceName=" + interfaceName
                        + " ,i18nKey=" + i18nKey);
            }
            ssm.addPluginInterface(name, interfaceName, i18nKey);

        } catch (Exception e) {
            //print error message and exit
            debug.error("AdminXMLParser.addPluginInterface():"
                    + "reported exception");
            System.err.println(bundle.getString(
                    "exception_while_adding_plugin_interface"));
            e.printStackTrace();
            throw new AdminException(e);
        }
        if (debug.messageEnabled()) {
            debug.message("AdminXMLParser.addPluginInterface():"
                    + "returning");
        }
    }

    private void getEntitiesInOrganization(
        String orgDN,
        Node childNode,
        AMStoreConnection connection
    ) throws AdminException
    {
        OrgGetEntitiesReq handle = new OrgGetEntitiesReq(orgDN);
        String strDNsOnly = ((Element)childNode).getAttribute("DNsOnly");
        String entityType = ((Element)childNode).getAttribute("entityType");
        boolean DNsOnly = Boolean.valueOf(strDNsOnly).booleanValue();
        handle.setDNsOnly(DNsOnly);
        handle.setEntityType(entityType);
        handle.setFilter(((Element)childNode).getAttribute("filter"));
        handle.setSizeLimit(((Element)childNode).getAttribute("sizeLimit"));
        handle.setTimeLimit(((Element)childNode).getAttribute("timeLimit"));
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());
        handle.addDNs(set);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(handle.toString());
        }

        handle.process(connection);
    }


    private void getEntitiesInContainer(
        String containerDN,
        Node childNode,
        AMStoreConnection connection
    ) throws AdminException
    {
        ContGetEntitiesReq handle = new ContGetEntitiesReq(containerDN);
        String strDNsOnly = ((Element)childNode).getAttribute("DNsOnly");
        String entityType = ((Element)childNode).getAttribute("entityType");
        boolean DNsOnly = Boolean.valueOf(strDNsOnly).booleanValue();
        handle.setDNsOnly(DNsOnly);
        handle.setEntityType(entityType);
        handle.setFilter(((Element)childNode).getAttribute("filter"));
        handle.setSizeLimit(((Element)childNode).getAttribute("sizeLimit"));
        handle.setTimeLimit(((Element)childNode).getAttribute("timeLimit"));

        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());
        handle.addDNs(set);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(handle.toString());
        }

        handle.process(connection);
    }

    private void deleteSubOrganizationsInOrganization(String orgDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        OrgDeleteSubOrgReq dpOrgDeleteSubOrgReq = new OrgDeleteSubOrgReq(orgDN);
        String strDeleteRecursively =
            ((Element)childNode).getAttribute("deleteRecursively");
        boolean deleteRecursively =
            Boolean.valueOf(strDeleteRecursively).booleanValue();
        dpOrgDeleteSubOrgReq.setRecursiveDelete(deleteRecursively);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpOrgDeleteSubOrgReq.toString());
            }

            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpOrgDeleteSubOrgReq.addDNSet((String)iter.next());
            }

            dpOrgDeleteSubOrgReq.process(connection);
        }
    }

    private void deletePeopleContainersInOrganization(String orgDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        OrgDeletePCReq dpOrgDeletePCReq = new OrgDeletePCReq(orgDN);
        String strDeleteRecursively =
            ((Element)childNode).getAttribute("deleteRecursively");
        boolean deleteRecursively =
            Boolean.valueOf(strDeleteRecursively).booleanValue();
        dpOrgDeletePCReq.setRecursiveDelete(deleteRecursively);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpOrgDeletePCReq.toString());
            }

            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpOrgDeletePCReq.addDNSet((String)iter.next());
            }

            dpOrgDeletePCReq.process(connection);
        }
    }

    private void deleteGroupsInOrganization(String orgDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        OrgDeleteGroupReq dpOrgDeleteGroupReq = new OrgDeleteGroupReq(orgDN);
        String strDeleteRecursively =
            ((Element)childNode).getAttribute("deleteRecursively");
        boolean deleteRecursively =
            Boolean.valueOf(strDeleteRecursively).booleanValue();
        dpOrgDeleteGroupReq.setRecursiveDelete(deleteRecursively);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpOrgDeleteGroupReq.toString());
            }

            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpOrgDeleteGroupReq.addDNSet((String)iter.next());
            }

            dpOrgDeleteGroupReq.process(connection);
        }
    }

    private void deleteRolesInOrganization(String orgDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        OrgDeleteRoleReq dpOrgDeleteRoleReq = new OrgDeleteRoleReq(orgDN);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            if (AdminUtils.logEnabled()) {
              AdminUtils.log(dpOrgDeleteRoleReq.toString());
            }

            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpOrgDeleteRoleReq.addDNSet((String)iter.next());
            }

            dpOrgDeleteRoleReq.process(connection);
        }
    }

    private void registerServicesInOrganization(String orgDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        OrgRegisterServicesReq dpOrgRegisterServices =
            new OrgRegisterServicesReq(orgDN);
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpOrgRegisterServices.toString());
            }

            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpOrgRegisterServices.registerServicesReq((String)iter.next());
            }

            dpOrgRegisterServices.process(connection);
        }
    }

    private void unregisterServicesInOrganization(String orgDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        OrgUnregisterServicesReq dpOrgUnregisterServices =
            new OrgUnregisterServicesReq(orgDN);
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpOrgUnregisterServices.toString());
            }

            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpOrgUnregisterServices.unregisterServicesReq(
                    (String)iter.next());
            }

            dpOrgUnregisterServices.process(connection);
        }
    }

    private void getRegisteredServicesInOrganization(String orgDN,
        AMStoreConnection connection)
        throws AdminException
    {
        OrgGetRegisteredServicesReq dpOrgGetRegisteredServices =
            new OrgGetRegisteredServicesReq(orgDN);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgGetRegisteredServices.toString());
        }

        dpOrgGetRegisteredServices.process(connection);
    }

    private void getNumberOfServicesInOrganization(String orgDN,
        AMStoreConnection connection)
        throws AdminException
    {
        OrgGetNumOfServicesReq dpOrgGetNumOfServicesReq =
            new OrgGetNumOfServicesReq(orgDN);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpOrgGetNumOfServicesReq.toString());
        }

        dpOrgGetNumOfServicesReq.process(connection);
    }

    private void createServiceTemplatesInOrganization(String orgDN,
        Node childNode, AMStoreConnection connection, SSOToken ssoToken)
        throws AdminException
    {
        OrgCreateServiceTemplateReq dpOrgCreateServiceTemplate =
            new OrgCreateServiceTemplateReq(orgDN);
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpOrgCreateServiceTemplate.addServiceTmplReq(
                    (String)iter.next());
            }

            dpOrgCreateServiceTemplate.process(connection, ssoToken);

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpOrgCreateServiceTemplate.toString());
            }
        }
    }

    private void createSubContainersInOrganization(String orgDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        OrgCreateSubContReq regHandler = new OrgCreateSubContReq(orgDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element)childNode).getAttribute("createDN");
        regHandler.addSubContReq(createDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(regHandler.toString());
        }

        regHandler.process(connection);
    }

    private void modifySubConfigurationsInOrganization(
        String orgDN,
        Node childNode,
        SSOToken ssoToken
    ) throws AdminException
    {
        String serviceName = ((Element)childNode).getAttribute("serviceName");

        if ((serviceName == null) || (serviceName.trim().length() == 0)) {
            throw new AdminException(bundle.getString("missingServiceName"));
        }

        String operation = ((Element)childNode).getAttribute("operation");
        if ((operation == null) || (operation.length() == 0)) {
            operation = "set";
        }

        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                serviceName, ssoToken);
            String subConfigName = ((Element)childNode).getAttribute(
                "subConfigName");
            ServiceConfig sc = scm.getOrganizationConfig(orgDN, null);
            if (sc != null) {
                StringTokenizer tokenizer = new StringTokenizer(
                    subConfigName, "/");

                while (tokenizer.hasMoreTokens()) {
                    String s = SMSSchema.unescapeName(tokenizer.nextToken());
                    sc = sc.getSubConfig(s);
                }

                Map map = XMLUtils.parseAttributeValuePairTags(childNode);

                if (operation.equals("set")) {
                    sc.setAttributes(map);
                } else if (operation.equals("add")) {
                    for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
                        String attrName = (String)i.next();
                        sc.addAttribute(attrName, (Set)map.get(attrName));
                    }
                } else {
                    for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
                        sc.removeAttribute((String)i.next());
                    }
                }

                String logMsg = bundle.getString(
                    "modified-sub-configuration-in-orgnaization");
                String[] params = {subConfigName, serviceName, orgDN};
//                AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//                    MessageFormat.format(logMsg, params));
                AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                    Level.INFO, AdminUtils.MODIFY_SUB_CONFIG_IN_ORG, params);
            } else {
                String[] param = {subConfigName, serviceName};
                throw new AdminException(
                    MessageFormat.format(bundle.getString("subconfigNotFound"),
                    param));
            }
        } catch (SSOException e) {
            throw new AdminException(e);
        } catch (SMSException e) {
            throw new AdminException(e);
        }
    }

    private void addSubConfigurationsInOrganization(
        String orgDN,
        String serviceName,
        Node childNode,
        SSOToken ssoToken
    ) throws AdminException
    {
        try {
            if ((serviceName != null) && (serviceName.length() > 0)) {
                ServiceConfigManager scm = new ServiceConfigManager(
                    serviceName, ssoToken);
                ServiceConfig sc = scm.getOrganizationConfig(orgDN, null);

                if (sc == null) {
                    sc = scm.createOrganizationConfig(orgDN, null);
                }

                Map map = XMLUtils.parseAttributeValuePairTags(childNode);
                String subConfigName = ((Element)childNode).getAttribute(
                    "subConfigName");
                String subConfigId = ((Element)childNode).getAttribute(
                    "subConfigId");
                StringTokenizer tokenizer = new StringTokenizer(
                    subConfigName, "/");
                int tokenCount = tokenizer.countTokens();

                for (int i = 1; i <= tokenCount; i++) {
                    String scn = SMSSchema.unescapeName(tokenizer.nextToken());

                    if (i != tokenCount) {
                        sc = sc.getSubConfig(scn);
                    } else {
                        if (subConfigId.length() == 0) {
                            subConfigId=scn;
                            debug.message("subConfigId is null");
                        }
                        sc.addSubConfig(scn,subConfigId, 0, map);

                        String[] params = {subConfigName, serviceName, orgDN};
                        String logMsg = bundle.getString(
                            "added-sub-configuration-in-orgnaization");
//                        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//                            MessageFormat.format(logMsg, params));
                        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                            Level.INFO,
                            AdminUtils.ADD_SUB_CONFIG_IN_ORG, params);
                    }
                }
            }
        } catch (Exception e){
            throw new AdminException(e.toString());
        }
    }

    private void removeSubConfigurationsInOrganization(
        String orgDN,
        String serviceName,
        Node childNode,
        SSOToken ssoToken
    ) throws AdminException
    {
        try {
            if ((serviceName != null) && (serviceName.length() > 0)) {

                String subConfigName = ((Element)childNode).getAttribute(
                    "subConfigName");
                ServiceConfigManager scm = new ServiceConfigManager(
                    serviceName, ssoToken);
                ServiceConfig sc = scm.getOrganizationConfig(orgDN, null);
                StringTokenizer tokenizer = new StringTokenizer(
                    subConfigName, "/");
                int tokenCount = tokenizer.countTokens();

                for (int i = 1; i <= tokenCount; i++) {
                    String scn = tokenizer.nextToken();

                    if (i != tokenCount) {
                        sc = sc.getSubConfig(scn);
                    } else {
                        sc.removeSubConfig(scn);

                        String[] params = {subConfigName, serviceName, orgDN};
                        String logMsg = bundle.getString(
                            "deleted-sub-configuration-in-orgnaization");
                        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                            Level.INFO, AdminUtils.DELETE_SUB_CONFIG_IN_ORG,
                            params);
                    }
                }
            }
        } catch (Exception ex) {
            throw new AdminException(ex.toString());
        }
    }

/* Federation: Commented out
    private void createRemoteProvider(String orgDN, Node childNode,
        SSOToken ssoToken)
        throws AdminException
    {
        String provToCreate =
            (((Element)childNode).getAttributeNode("id")).getValue();

        try {
            FedServicesRequestHandler fedReq =
                new FedServicesRequestHandler(orgDN, ssoToken);
            fedReq.createRemoteProvider(childNode);
        } catch (Exception fsae) {
            debug.error("AdminXMLParser::Error in alliance manager:", fsae);
            throw new AdminException(fsae);
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log("AdminXMLParser::Created Remote Provider" +
                provToCreate);
        }

        String[] args = {provToCreate, orgDN};
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, AdminUtils.CREATE_REMOTE_PROV, args);
    }

    private void modifyRemoteProvider(String orgDN, Node childNode,
        SSOToken ssoToken)
        throws AdminException
    {
        String provToModify =
            (((Element)childNode).getAttributeNode("id")).getValue();

        try {
            FedServicesRequestHandler fedReq =
                new FedServicesRequestHandler(orgDN, ssoToken);
            fedReq.modifyRemoteProvider(childNode);
        } catch(Exception fsae){
            debug.error("AdminXMLParser::Error in alliance manager:", fsae);
            throw new AdminException(fsae);
        }

        if (AdminUtils.logEnabled()){
            AdminUtils.log("AdminXMLParser::Modified Remote Provider" +
                provToModify);
        }

        String[] args = {provToModify, orgDN};
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, AdminUtils.MODIFY_REMOTE_PROV, args);
    }

    private void modifyHostedProvider(String orgDN, Node childNode,
        SSOToken ssoToken)
        throws AdminException
    {
        String hostedProvToModify =
            (((Element)childNode).getAttributeNode("id")).getValue();

        try {
            FedServicesRequestHandler fedReq =
                new FedServicesRequestHandler(orgDN, ssoToken);
            fedReq.modifyHostedProvider(childNode);
        } catch(FSInvalidNameException fsine) {
            debug.error("AdminXMLParser::Error in alliance manager:", fsine);
            throw new AdminException(fsine);
        } catch(FSAllianceManagementException fsae) {
            debug.error("AdminXMLParser::Error in alliance manager:", fsae);
            throw new AdminException(fsae);
        }

        if (AdminUtils.logEnabled()){
            AdminUtils.log("AdminXMLParser::Modified Remote Provider" +
                hostedProvToModify);
        }

        String[] args = {hostedProvToModify, orgDN};
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, AdminUtils.MODIFY_HOSTED_PROV, args);
    }

    private void createHostedProvider(String orgDN, Node childNode,
        SSOToken ssoToken)
        throws AdminException
    {
        String hostedProvToCreate =
            (((Element)childNode).getAttributeNode("id")).getValue();

        try {
            FedServicesRequestHandler fedReq =
                new FedServicesRequestHandler(orgDN, ssoToken);
            fedReq.createHostedProvider(childNode);
        } catch(FSInvalidNameException fsine) {
            debug.error("AdminXMLParser::Error in alliance manager:", fsine);
            throw new AdminException(fsine);
        } catch(FSAllianceManagementException fsae) {
            debug.error("AdminXMLParser::Error in alliance manager:", fsae);
            throw new AdminException(fsae);
        }

        if (AdminUtils.logEnabled()){
            AdminUtils.log("AdminXMLParser::Created Hosted Provider" +
                hostedProvToCreate);
        }

        String[] args = {hostedProvToCreate, orgDN};
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, AdminUtils.CREATE_HOSTED_PROV, args);
    }

    private void deleteHostedProvider(String orgDN, Node childNode,
        SSOToken ssoToken)
        throws AdminException
    {
        String provToDelete =
            (((Element)childNode).getAttributeNode("id")).getValue();

        try {
            FedServicesRequestHandler fedReq =
                new FedServicesRequestHandler(orgDN, ssoToken);
            fedReq.deleteProvider(provToDelete);
        } catch (FSAllianceManagementException fsae) {
            debug.error("AdminXMLParser::Error in alliance manager:", fsae);
            throw new AdminException(fsae);
        }

        if (AdminUtils.logEnabled()){
            AdminUtils.log("AdminXMLParser::Deleted Provider:" +
                provToDelete);
        }

        String[] args = {provToDelete, orgDN};
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, AdminUtils.DELETE_PROV, args);
    }

    private void getFederationProvider(String orgDN, Node childNode,
        SSOToken ssoToken)
        throws AdminException
    {
        String provToGet =
            (((Element)childNode).getAttributeNode("id")).getValue();

        try{
            FedServicesRequestHandler fedReq =
                new FedServicesRequestHandler(orgDN, ssoToken);
            fedReq.getProvider(provToGet);
        } catch (FSAllianceManagementException fsae){
            debug.error("AdminXMLParser::Error in alliance manager:", fsae);
            throw new AdminException(fsae);
        }

        if (AdminUtils.logEnabled()){
            AdminUtils.log("AdminXMLParser::Read Provider " + provToGet);
        }
    }

    private void createFederationAuthenticationDomain(String orgDN,
        Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        String cotToCreate =
            (((Element)childNode).getAttributeNode("name")).getValue();

        try {
            FedServicesRequestHandler fedReq =
                new FedServicesRequestHandler(orgDN, ssoToken);
            fedReq.createAuthenticationDomain(childNode);
        } catch(FSInvalidNameException fsine) {
            debug.error("AdminXMLParser::Error in alliance manager:", fsine);
            throw new AdminException(fsine);
        } catch(FSAllianceManagementException fsae) {
            debug.error("AdminXMLParser::Error in alliance manager:", fsae);
            throw new AdminException(fsae);
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log("AdminXMLParser::Created Circle of Trust " +
                cotToCreate);
        }

        String[] args = {cotToCreate, orgDN};
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, AdminUtils.CREATE_COT, args);
    }

    private void deleteFederationAuthenticationDomain(String orgDN,
        Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        String cotToDelete =
            (((Element)childNode).getAttributeNode("name")).getValue();

        try {
            FedServicesRequestHandler fedReq =
                new FedServicesRequestHandler(orgDN, ssoToken);
            fedReq.deleteAuthenticationDomain(cotToDelete);
        } catch (FSInvalidNameException fsine) {
            debug.error("AdminXMLParser::Error in alliance manager:", fsine);
            throw new AdminException(fsine);
        } catch (FSAllianceManagementException fsae) {
            debug.error("AdminXMLParser::Error in alliance manager:", fsae);
            throw new AdminException(fsae);
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log("AdminXMLParser::Deleted Authentication Domain " +
                cotToDelete);
        }

        String[] args = {cotToDelete, orgDN};
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, AdminUtils.DELETE_COT, args);
    }

    private void getFederationAuthenticationDomain(String orgDN, Node childNode,
        SSOToken ssoToken)
        throws AdminException
    {
        String cotToGet =
            (((Element)childNode).getAttributeNode("name")).getValue();

        try {
            FedServicesRequestHandler fedReq =
                new FedServicesRequestHandler(orgDN, ssoToken);
            fedReq.getAuthenticationDomain(cotToGet);
        } catch(FSInvalidNameException fsine) {
            debug.error("AdminXMLParser::Error in alliance manager:", fsine);
            throw new AdminException(fsine);
        } catch(FSAllianceManagementException fsae){
            debug.error("AdminXMLParser::Error in alliance manager:", fsae);
            throw new AdminException(fsae);
        }

        if (AdminUtils.logEnabled()){
            AdminUtils.log("AdminXMLParser::Read Auth Domain " + cotToGet);
        }
    }

    private void modifyFederationAuthenticationDomain(String orgDN,
        Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        String cotToModify =
            (((Element)childNode).getAttributeNode("name")).getValue();

        try {
            FedServicesRequestHandler fedReq =
                new FedServicesRequestHandler(orgDN, ssoToken);
            fedReq.modifyAuthenticationDomain(childNode);
        } catch(FSInvalidNameException fsine) {
            debug.error("AdminXMLParser::Error in alliance manager:", fsine);
            throw new AdminException(fsine);
        } catch(FSAllianceManagementException fsae) {
            debug.error("AdminXMLParser::Error in alliance manager:", fsae);
            throw new AdminException(fsae);
        }

        if (AdminUtils.logEnabled()){
            AdminUtils.log("AdminXMLParser::Modified Auth Domain " +
                cotToModify);
        }

        String args[] = {cotToModify, orgDN};
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, AdminUtils.MODIFY_COT, args);
    }
*/

    private Set getSubNodeElementNodeDNValues(NodeList list) {
        int len = list.getLength();
        Set set = new HashSet(len);

        for (int i = 0; i < len; i++) {
            Node subchildnode = list.item(i);

            if ((subchildnode.getNodeType() == Node.ELEMENT_NODE) &&
                subchildnode.getNodeName().equals("DN")
            ) {
                Text firstChild = (Text)subchildnode.getFirstChild();
                set.add(firstChild.getNodeValue());
            }
        }

        return set;
    }

    private Set getSubNodeElementNodeValues(NodeList list) {
        int len = list.getLength();
        Set set = new HashSet(len);

        for (int i = 0; i < len; i++) {
            Node subchildnode = list.item(i);

            if (subchildnode.getNodeType() == Node.ELEMENT_NODE) {
                Text firstChild = (Text)subchildnode.getFirstChild();
                set.add(firstChild.getNodeValue());
            }
        }

        return set;
    }

    private SchemaType getSchemaType(String schemaTypeName){
        SchemaType schemaType = null;

        if (schemaTypeName.equalsIgnoreCase("global")) {
            schemaType = SchemaType.GLOBAL;
        } else if (schemaTypeName.equalsIgnoreCase("organization")) {
            schemaType = SchemaType.ORGANIZATION;
        } else if (schemaTypeName.equalsIgnoreCase("dynamic")) {
            schemaType = SchemaType.DYNAMIC;
        } else if (schemaTypeName.equalsIgnoreCase("user")) {
            schemaType = SchemaType.USER;
        } else if (schemaTypeName.equalsIgnoreCase("policy")) {
            schemaType = SchemaType.POLICY;
        }

        return schemaType;
    }
    
    /**
     * This method parses the SubSchema string and return
     * the <ServiceSchema> object for the specified level.
     */
    private ServiceSchema parseSubSchema(String serviceName, String schemaType,
        String subSchema, SSOToken ssot)
        throws AdminException
    {
        ServiceSchema ss = null;

        if ((serviceName != null) && (schemaType != null)) {
            try {
                SchemaType schematype = getSchemaType(schemaType);
                ServiceSchemaManager ssm = new ServiceSchemaManager(
                    serviceName, ssot);
                ss = ssm.getSchema(schematype);

                if (subSchema != null) {
                    boolean done = false;
                    StringTokenizer st = new StringTokenizer(subSchema, "/");

                    while (st.hasMoreTokens() && !done) {
                        String str = st.nextToken();

                        if (str != null) {
                            ss = ss.getSubSchema(str);

                            if (ss == null) {
                                debug.error(
                                    "SubSchema" + str + "does not exist");
                                String[] args = {str};
                                String exMsg = MessageFormat.format(
                                    bundle.getString(
                                        "subSchemaStringDoesNotExist"),
                                        args);
                                throw new AdminException(exMsg);
                            }
                        } else {
                            done = true;
                        }
                    }
                }
            } catch (SMSException e) {
                throw new AdminException(e);
            } catch (SSOException e) {
                throw new AdminException(e);
            } catch (Exception e) {
                throw new AdminException(e);
            }
        }

        if (ss == null) {
            String[] args = {serviceName, schemaType};
            throw new AdminException(
                MessageFormat.format(
                    bundle.getString("serviceschemaexception"), args));
        }

        return ss;
    }

    private void doSchemaRootNodeRequests(Node node, SSOToken ssot)
        throws AdminException
    {
        ServiceSchema serviceSchema = null;
        String serviceName = ((Element)node).getAttribute("serviceName");

        NodeList schemaChildNodesList = node.getChildNodes();
        int schemaChildNodesListLength = schemaChildNodesList.getLength();

        for (int i = 0; i < schemaChildNodesListLength; i++) {
            Node childNode = schemaChildNodesList.item(i);

            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                String strChildNodeName = childNode.getNodeName();
                int opt = getToken(strChildNodeName);

                try {
                    switch(opt) {
                    case SI18N:
                        setI18nKeyonSchema(childNode, serviceName, ssot);
                        break;
                    case SPVBU:
                        setPropertiesViewBeanURLonSchema(
                            childNode, serviceName, ssot);
                        break;
                    case GSRN:
                        getServiceRevisionNumber(serviceName, ssot);
                        break;
                    case SSRN:
                        setServiceRevisionNumber(
                            childNode, serviceName, ssot);
                        break;
                    }
                } catch (AdminException ae) {
                    if (!continueFlag) {
                        throw ae;
                    }
                }
            }
        }
    }

    private void logServiceSchemaModified(String serviceName) {
        String[] param = {serviceName};
        String logMessage = bundle.getString("modified-service-schema");
//        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//            MessageFormat.format(logMessage, param));
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, AdminUtils.MODIFY_SERVICE_SCHEMA, param);
    }

    private void setI18nKeyonSchema(
        Node childNode,
        String serviceName,
        SSOToken ssoToken
    ) throws AdminException
    {
        String i18nKey = ((Element)childNode).getAttribute("i18nKey");

        try {
            ServiceSchemaManager serviceSchemaMgr =
                new ServiceSchemaManager(serviceName, ssoToken);
            serviceSchemaMgr.setI18NKey(i18nKey);
            logServiceSchemaModified(serviceName);
        } catch (SMSException e) {
            throw new AdminException(e);
        } catch (SSOException e) {
            throw new AdminException(e);
        }
    }

    private void setPropertiesViewBeanURLonSchema(
        Node childNode,
        String serviceName,
        SSOToken ssoToken
    ) throws AdminException
    {
        String url = ((Element)childNode).getAttribute("url");

        try {
            ServiceSchemaManager serviceSchemaMgr =
                new ServiceSchemaManager(serviceName, ssoToken);
            serviceSchemaMgr.setPropertiesViewBeanURL(url);
            logServiceSchemaModified(serviceName);
        } catch (SMSException e) {
            throw new AdminException(e);
        } catch (SSOException e) {
            throw new AdminException(e);
        }
    }

    private void setServiceRevisionNumber(
        Node childNode,
        String serviceName,
        SSOToken ssoToken
    ) throws AdminException
    {
        String number = ((Element)childNode).getAttribute("number");
        int revision = 0;

        try {
            revision = Integer.parseInt(number);
        } catch (NumberFormatException e) {
            String[] params = {number};
            String msg = MessageFormat.format(
                bundle.getString("invalidServiceRevisionNumber"), params);
            throw new AdminException(msg);
        }

        try {
            ServiceSchemaManager serviceSchemaMgr =
                new ServiceSchemaManager(serviceName, ssoToken);
            serviceSchemaMgr.setRevisionNumber(revision);
            logServiceSchemaModified(serviceName);

            String[] params = {serviceName, number};
            System.out.println(
                MessageFormat.format(
                    bundle.getString("setServiceRevisionNumber"), params));
        } catch (SMSException e) {
            throw new AdminException(e);
        } catch (SSOException e) {
            throw new AdminException(e);
        }
    }

    private void getServiceRevisionNumber(String serviceName, SSOToken ssoToken)
        throws AdminException
    {
        try {
            ServiceSchemaManager serviceSchemaMgr =
                new ServiceSchemaManager(serviceName, ssoToken);
            int revisionNumber = serviceSchemaMgr.getRevisionNumber();
            System.out.println(Integer.toString(revisionNumber));
        } catch (SMSException e) {
            throw new AdminException(e);
        } catch (SSOException e) {
            throw new AdminException(e);
        }
    }

    private void doSchemaRequests(Node node, SSOToken ssot)
        throws AdminException
    {
        ServiceSchema serviceSchema = null;
        String serviceName = ((Element)node).getAttribute("serviceName");
        String schemaType = ((Element)node).getAttribute("SchemaType");
        String i18nKey = ((Element)node).getAttribute("i18nKey");
        String subSchemaName = ((Element)node).getAttribute("SubSchema");
                
        if ((subSchemaName != null) && (subSchemaName.length() > 0) &&
            !schemaType.equalsIgnoreCase("global") &&
            !schemaType.equalsIgnoreCase("organization")
        ) {
            String msg = bundle.getString("subschemaexception");

            if (!continueFlag) {
                throw new AdminException(msg);
            } 
        }

        NodeList schemaChildNodesList = node.getChildNodes();
        int schemaChildNodesListLength = schemaChildNodesList.getLength();

        for (int i = 0; i < schemaChildNodesListLength; i++) {
            Node childNode = schemaChildNodesList.item(i);

            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                String strChildNodeName = childNode.getNodeName();
                int opt = getToken(strChildNodeName);

                try {
                    switch(opt) {
                    case RDV:
                    case MDV:
                    case ATEV:
                    case DSV:
                        processServiceSchemaRequest(opt, childNode,
                            schemaType, serviceName, subSchemaName, ssot);
                        break;

                    case ACV:
                        /* need to support backward compatibility i.e.
                         * support AttributevValuePair and ChoiceValue
                         * element
                         */
                        if (!processAttributeSchemaRequest(opt, childNode,
                            schemaType, serviceName, subSchemaName, ssot,
                            i18nKey)
                        ) {
                            processAttributeSchemaChoiceValueRequest(
                                opt, childNode, schemaType, serviceName,
                                subSchemaName, ssot);
                        }
                        break;

                    case SCV:
                        processAttributeSchemaChoiceValueRequest(opt, childNode,
                            schemaType, serviceName, subSchemaName, ssot);
                        break;

                    case DCV:
                    case MT:
                    case MUIT:
                    case MIK:
                    case APVB:
                    case ASR:
                    case AER:
                    case MS:
                    case MAA:
                    case RPDV:
                    case SAV:
                        processAttributeSchemaRequest(opt, childNode,
                            schemaType, serviceName, subSchemaName, ssot,
                            i18nKey);
                        break;

                    case SBV:
                        processSetBooleanValuesRequest(opt, childNode,
                            schemaType, serviceName, subSchemaName, ssot);
                        break;

                    case ASSC:
                        addSubSchema(childNode, serviceName, schemaType,
                            subSchemaName, ssot);
                        break;

                    case AAS:
                        addAttributeSchema(childNode, serviceName, schemaType,
                            subSchemaName, ssot);
                        break;

                    case RAS:
                        serviceSchema = parseSubSchema(
                            serviceName, schemaType, subSchemaName, ssot);
                        removeAttributeSchema(serviceName, serviceSchema,
                            XMLUtils.parseAttributesTag(childNode));
                        break;

                    case RSS:
                        serviceSchema = parseSubSchema(
                            serviceName, schemaType, subSchemaName, ssot);
                        removeSubSchema(serviceName, serviceSchema,
                            XMLUtils.parseAttributesTag(childNode));
                        break;

                    case MIA:
                        serviceSchema = parseSubSchema(
                            serviceName, schemaType, subSchemaName, ssot);
                        modifyInheritance(serviceName, serviceSchema,
                            childNode);
                        break;
                    case ADD_PI:
                        addPluginInterface(serviceName, childNode, ssot);
                        break;
                    case SET_PVU_FOR_PS:
                        setPropertiesViewBeanURLForPS(serviceName, childNode, 
                                ssot);
                        break;
                    }
                } catch (AdminException ae) {
                    if (!continueFlag) {
                        throw ae;
                    }
                }
            }
        }
    }

    private void removeAttributeSchema(
        String serviceName,
        ServiceSchema serviceSchema,
        Set set
    ) throws AdminException
    {
        if ((set != null) && !set.isEmpty()) {
            try {
                for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                    serviceSchema.removeAttributeSchema((String)iter.next());
                }
                logServiceSchemaModified(serviceName);
            } catch (Exception ex) {
                throw new AdminException(ex);
            }
        }
    }

    private void removeSubSchema(
        String serviceName,
        ServiceSchema serviceSchema,
        Set set
    ) throws AdminException
    {
        if ((set != null) && !set.isEmpty()) {
            String logMsg = bundle.getString("deleted-service-subschema");

            try {
                for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                    String name = (String)iter.next();
                    serviceSchema.removeSubSchema(name);
                    String[] params = {name, serviceName};
//                    AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//                        MessageFormat.format(logMsg, params));
                    AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                        Level.INFO, AdminUtils.DELETE_SERVICE_SUBSCHEMA,
                        params);
                }
            } catch (Exception ex) {
                throw new AdminException(ex);
            }
        }
    }

    private void modifyInheritance(
        String serviceName,
        ServiceSchema serviceSchema,
        Node childNode
    ) throws AdminException
    {
        String inheritance = ((Element)childNode).getAttribute("inheritance");
        try {
            serviceSchema.setInheritance(inheritance);
            logServiceSchemaModified(serviceName);
        } catch (SMSException e) {
            throw new AdminException(e);
        } catch (SSOException e) {
            throw new AdminException(e);
        }
    }

    private void addSubSchema(Node childNode, String serviceName,
        String schemaTypeName, String subSchemaName, SSOToken ssoToken)
        throws AdminException
    {
        if (!schemaTypeName.equalsIgnoreCase("global") &&
            !schemaTypeName.equalsIgnoreCase("organization")
        ) {
            throw new AdminException(bundle.getString("subschemaexception"));
        }

        ServiceSchema serviceSchema = parseSubSchema(serviceName,
            schemaTypeName, subSchemaName, ssoToken);
        fileName = ((Element)childNode).getAttribute("fileName");

        try {
            serviceSchema.addSubSchema(new FileInputStream(fileName));
            String[] param = {serviceName};
            String logMessage = bundle.getString("added-service-subschema");
//            AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//                MessageFormat.format(logMessage, param));
            AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                Level.INFO, AdminUtils.ADD_SERVICE_SUBSCHEMA, param);
        } catch (Exception ex) {
            throw new AdminException(ex);
        }
    }
    
    private void addAttributeSchema(Node childNode, String serviceName,
        String schemaTypeName, String subSchemaName, SSOToken ssoToken)
        throws AdminException
    {
        ServiceSchema serviceSchema = parseSubSchema(serviceName,
            schemaTypeName, subSchemaName, ssoToken);
        fileName = ((Element)childNode).getAttribute("fileName");

        try {
            serviceSchema.addAttributeSchema(new FileInputStream(fileName));
            logServiceSchemaModified(serviceName);
        } catch (Exception ex) {
            throw new AdminException(ex);
        }
    }

    private void processServiceSchemaRequest(
        int option,
        Node childNode,
        String schemaTypeName,
        String serviceName,
        String subSchemaName,
        SSOToken ssoToken
    ) throws AdminException {
        ServiceSchema serviceSchema = parseSubSchema(serviceName,
            schemaTypeName, subSchemaName, ssoToken);

        if (serviceSchema != null) {
            processServiceSchemaRequest(option, childNode, serviceSchema);
            logServiceSchemaModified(serviceName);
        }
    }

    private void processAttributeSchemaChoiceValueRequest(
        int option,
        Node childNode,
        String schemaTypeName,
        String serviceName,
        String subSchemaName,
        SSOToken ssoToken
    ) throws AdminException
    {
            ServiceSchema serviceSchema = parseSubSchema(serviceName,
            schemaTypeName, subSchemaName, ssoToken);
        Map map = parseChoiceValueTags(childNode);

        try {
            for (Iterator iter = map.keySet().iterator(); iter.hasNext(); ) {
                String attrName = (String)iter.next();
                AttributeSchema attrSchema =
                    serviceSchema.getAttributeSchema(attrName);
                Set values = (Set)map.get(attrName);

                for (Iterator i = values.iterator(); i.hasNext(); ) {
                    String[] i18nKeyValuePair = (String[])i.next();

                    // delete the choice value and add it to simulate modify.
                    if (option == SCV) {
                        attrSchema.removeChoiceValue(i18nKeyValuePair[1]);
                    }

                    attrSchema.addChoiceValue(i18nKeyValuePair[1],
                        i18nKeyValuePair[0]);
                }
            }
        } catch (SMSException e) {
            throw new AdminException(e);
        } catch (SSOException e) {
            throw new AdminException(e);
        }

        logServiceSchemaModified(serviceName);
    }

    private void processSetBooleanValuesRequest(
        int option,
        Node node,
        String schemaTypeName,
        String serviceName,
        String subSchemaName,
        SSOToken ssoToken
    ) throws AdminException {
        try {
            ServiceSchema serviceSchema = parseSubSchema(serviceName,
                schemaTypeName, subSchemaName, ssoToken);
            String attrName = ((Element)node).getAttribute("name");
            String trueValue = ((Element)node).getAttribute("trueValue");
            String trueI18nKey = ((Element)node).getAttribute("trueI18nKey");
            String falseValue = ((Element)node).getAttribute("falseValue");
            String falseI18nKey = ((Element)node).getAttribute("falseI18nKey");
            AttributeSchema attrSchema = serviceSchema.getAttributeSchema(
                attrName);
            attrSchema.setBooleanValues(trueValue, trueI18nKey, falseValue,
                falseI18nKey);
            logServiceSchemaModified(serviceName);
        } catch (SMSException e) {
            throw new AdminException(e);
        } catch (SSOException e) {
            throw new AdminException(e);
        }
    }

    private boolean processAttributeSchemaRequest(int option, Node childNode,
        String schemaTypeName, String serviceName, String subSchemaName, 
        SSOToken ssoToken, String i18nKey)
        throws AdminException
    {
        boolean processed = false;
        ServiceSchema serviceSchema = parseSubSchema(serviceName,
            schemaTypeName, subSchemaName, ssoToken);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);

        if ((map != null) && !map.isEmpty()) {
            for (Iterator iter = map.keySet().iterator(); iter.hasNext(); ) {
                String attrName = (String)iter.next();
                AttributeSchema attrSchema =
                    serviceSchema.getAttributeSchema(attrName);
                Set values = (Set)map.get(attrName);
                processAttributeSchemaRequest(
                    option, attrSchema, values, i18nKey);
            }

            logServiceSchemaModified(serviceName);
            processed = true;
        }

        return processed;
    }

    private ServiceSchema getServiceSchema(String schemaTypeName,
        String serviceName, SSOToken ssoToken)
        throws AdminException
    {
        ServiceSchema serviceSchema = null;
        SchemaType schemaType = getSchemaType(schemaTypeName);

        try {
            ServiceSchemaManager serviceSchemaMgr =
                new ServiceSchemaManager(serviceName, ssoToken);
            serviceSchema = serviceSchemaMgr.getSchema(schemaType);
        } catch (Exception e) {
            throw new AdminException(e);
        }
 
        return serviceSchema;
    }
 
    private void processServiceSchemaRequest(int option, Node childNode,
        ServiceSchema serviceSchema)
        throws AdminException
    {
        try {
            switch (option) {
            case RDV:
                serviceSchema.removeAttributeDefaults(
                    XMLUtils.parseAttributesTag(childNode));
                break;
            case MDV:
                serviceSchema.setAttributeDefaults(
                    XMLUtils.parseAttributeValuePairTags(childNode));
                break;
            case ATEV:
                addDefaultValuesToServiceSchema(childNode, serviceSchema);
                break;
            case DSV:
                getServiceDefaultValues(childNode, serviceSchema);
                break;
            }
        } catch (Exception e) {
            throw new AdminException(e);
        }
    }
 
    private void processAttributeSchemaRequest(int option,
        AttributeSchema attrSchema, Set values, String i18nKey)
        throws AdminException
    {
        try {
            switch (option) {
            // need to support add choice value for backward compatibility.
            case ACV:
                addAttributeSchemaChoiceValues(attrSchema, values, i18nKey);
                break;
            case DCV:
                removeAttributeSchemaChoiceValues(attrSchema, values);
                break;
            case MT:
                modifyAttributeSchemaTypes(attrSchema, values);
                break;
            case MUIT:
                modifyAttributeSchemaUITypes(attrSchema, values);
                break;
            case MIK:
                modifyAttributeSchemaI18nKeys(attrSchema, values);
                break;
            case APVB:
                modifyAttributeSchemaPropertiesViewBean(attrSchema, values);
                break;
            case ASR:
                modifyAttributeSchemaStartRange(attrSchema, values);
                break;
            case AER:
                modifyAttributeSchemaEndRange(attrSchema, values);
                break;
            case MS:
                modifyAttributeSchemaSyntax(attrSchema, values);
                break;
            case MAA:
                modifyAttributeSchemaAnyAttribute(attrSchema, values);
                break;
            case RPDV:
                removePartialDefaultValues(attrSchema, values);
                break;
            case SAV:
                setAttributeValidator(attrSchema, values);
                break;
            }
        } catch (Exception e) {
            throw new AdminException(e);
        }
    }
 
    private void addDefaultValuesToServiceSchema(Node childNode,
        ServiceSchema serviceSchema)
        throws AdminException
    {
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
 
        try {
            Map mapOldValues = serviceSchema.getAttributeDefaults();
 
            for (Iterator iter = map.keySet().iterator(); iter.hasNext(); ) {
                String attrName = (String)iter.next();
                Set oldValues = (Set)mapOldValues.get(attrName);
                Set newValues = ((oldValues == null) || oldValues.isEmpty()) ?
                    new HashSet() : new HashSet(oldValues);
                newValues.addAll((Set)map.get(attrName));
                serviceSchema.setAttributeDefaults(attrName, newValues);
            }
        } catch (Exception e) {
            throw new AdminException(e);
        }
    }
 
    private void getServiceDefaultValues(Node childNode,
        ServiceSchema serviceSchema
    ) {
        Set set = XMLUtils.parseAttributesTag(childNode);
        Map attrValuesMap = serviceSchema.getAttributeDefaults();
 
        if ((set == null) || set.isEmpty()) {
            set = attrValuesMap.keySet();
        }
 
        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            String attrName = (String)iter.next();
            Set values = (Set)attrValuesMap.get(attrName);
 
            if (values != null) {
                AttributeSchema attrSchema =
                    serviceSchema.getAttributeSchema(attrName);
                AttributeSchema.Syntax syntax = attrSchema.getSyntax();
 
                if (syntax == AttributeSchema.Syntax.PASSWORD) {
                    values = maskPasswordField(values);
                }
 
                System.out.println(attrName + "=" + values.toString());
            }
        }
    }
 
    private Set maskPasswordField(Set pwdValues) {
        int size = pwdValues.size();
        Set masked = new HashSet(size);
 
        for (int i = 0; i < size; i++) {
            masked.add("********");
        }
 
        return masked;
    }

    private void addAttributeSchemaChoiceValues(
        AttributeSchema attrSchema,
        Set values,
        String i18nKey
    ) throws AdminException {
        try {
            for (Iterator iter = values.iterator(); iter.hasNext(); ) {
                attrSchema.addChoiceValue((String)iter.next(), i18nKey);
            }
        } catch (SMSException e) {
            throw new AdminException(e);
        } catch (SSOException e) {
            throw new AdminException(e);
        }
    }

 
    private void removeAttributeSchemaChoiceValues(AttributeSchema attrSchema,
        Set values)
        throws AdminException
    {
        try {
            for (Iterator iter = values.iterator(); iter.hasNext(); ) {
                attrSchema.removeChoiceValue((String)iter.next());
            }
        } catch (Exception e) {
            throw new AdminException(e);
        }
    }
 
    private void setAttributeValidator(AttributeSchema attrSchema,
        Set values)
        throws AdminException
    {
        try {
            for (Iterator iter = values.iterator(); iter.hasNext(); ) {
                attrSchema.setValidator((String)iter.next());
            }
        } catch (Exception e) {
            throw new AdminException(e);
        }
    }
 
    private void modifyAttributeSchemaTypes(AttributeSchema attrSchema,
        Set values)
        throws AdminException
    {
        try {
            for (Iterator iter = values.iterator(); iter.hasNext(); ) {
                attrSchema.setType((String)iter.next());
            }
        } catch (Exception e) {
            throw new AdminException(e);
        }
    }
 
    private void modifyAttributeSchemaUITypes(AttributeSchema attrSchema,
        Set values)
        throws AdminException
    {
        try {
            for (Iterator iter = values.iterator(); iter.hasNext(); ) {
                attrSchema.setUIType((String)iter.next());
            }
        } catch (Exception e) {
            throw new AdminException(e);
        }
    }
 
    private void modifyAttributeSchemaI18nKeys(AttributeSchema attrSchema,
        Set values)
        throws AdminException
    {
        try {
            for (Iterator iter = values.iterator(); iter.hasNext(); ) {
                attrSchema.setI18NKey((String)iter.next());
            }
        } catch (Exception e) {
            throw new AdminException(e);
        }
    }
 
    private void modifyAttributeSchemaPropertiesViewBean(
        AttributeSchema attrSchema, Set values)
        throws AdminException
    {
        try {
            for (Iterator iter = values.iterator(); iter.hasNext(); ) {
                attrSchema.setPropertiesViewBeanUR((String)iter.next());
            }
        } catch (Exception e) {
            throw new AdminException(e);
        }
    }
 
    private void modifyAttributeSchemaStartRange(
        AttributeSchema attrSchema, Set values)
        throws AdminException
    {
        try {
            for (Iterator iter = values.iterator(); iter.hasNext(); ) {
                attrSchema.setStartRange((String)iter.next());
            }
        } catch (Exception e) {
            throw new AdminException(e);
        }
    }
 
    private void modifyAttributeSchemaEndRange(
        AttributeSchema attrSchema, Set values)
        throws AdminException
    {
        try {
            for (Iterator iter = values.iterator(); iter.hasNext(); ) {
                attrSchema.setEndRange((String)iter.next());
            }
        } catch (Exception e) {
            throw new AdminException(e);
        }
    }
 
    private void modifyAttributeSchemaSyntax(
        AttributeSchema attrSchema, Set values)
        throws AdminException
    {
        try {
            for (Iterator iter = values.iterator(); iter.hasNext(); ) {
                attrSchema.setSyntax((String)iter.next());
            }
        } catch (Exception e) {
            throw new AdminException(e);
        }
    }

    private void modifyAttributeSchemaAnyAttribute(
        AttributeSchema attrSchema,
        Set values
    ) throws AdminException
    {
        try {
            if ((values != null) && !values.isEmpty()) {
                attrSchema.setAny((String)values.iterator().next());
            }
        } catch (SSOException e) {
            throw new AdminException(e);
        } catch (SMSException e) {
            throw new AdminException(e);
        }
    }


    private void removePartialDefaultValues(
        AttributeSchema attrSchema,
        Set values
    ) throws AdminException
    {
        try {
            for (Iterator iter = values.iterator(); iter.hasNext(); ) {
                attrSchema.removeDefaultValue((String)iter.next());
            }
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        } catch (SMSException smse) {
            throw new AdminException(smse);
        }
    }

    private void doServiceConfigurationRequests(Node node, SSOToken ssot)
        throws AdminException
    {
        Node subConfigNode = node;
        String serviceName =((Element)subConfigNode).getAttribute(
            "serviceName");
        String realmName = ((Element)subConfigNode).getAttribute("realm");
        NodeList subConfigChildNodesList = subConfigNode.getChildNodes();
        int len = subConfigChildNodesList.getLength();

        for (int i = 0; i < len; i++) {
            Node childNode = subConfigChildNodesList.item(i);

            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                String strChildNodeName = childNode.getNodeName();
                int opt = getToken(strChildNodeName);
                try {
                    switch(opt){
                    case ASC :
                        if ((realmName == null) || (realmName.length() == 0)) {
                            addSubConfigToDefault(serviceName, childNode,
                                ssot);
                        } else {
                            addSubConfigurationsInOrganization(realmName,
                                serviceName, childNode, ssot);
                        }
                        break;
                    case MSCF :
                        if ((realmName == null) || (realmName.length() == 0)) {
                            modifySubConfigToDefault(
                                serviceName, childNode, ssot);
                        } else {
                            modifySubConfigurationsInOrganization(realmName,
                                childNode, ssot);
                        }
                        break;
                    case RSC:
                        if ((realmName == null) || (realmName.length() == 0)) {
                            removeSubConfigFromDefault(serviceName, childNode,
                                ssot);
                        } else {
                            removeSubConfigurationsInOrganization(realmName, 
                                serviceName, childNode, ssot);
                        }
                        break;
                    case GSCF:
                        getSubConfiguration(realmName, serviceName, childNode,
                            ssot);
                        break;
                    case DASC:
                        deleteAllServiceConfiguration(serviceName, childNode,
                            ssot);
                        break;
                    }
                } catch (AdminException ae) {
                    if (!continueFlag) {
                        throw ae;
                    }
                }
            }
        }
    }

    private void addSubConfigToDefault(String serviceName, Node childNode,
        SSOToken ssoToken)
        throws AdminException
    {
        try {
            String subConfigName = ((Element)childNode).getAttribute(
                "subConfigName");
            String subConfigId = ((Element)childNode).getAttribute(
                "subConfigId");
            Map map = XMLUtils.parseAttributeValuePairTags(childNode);
            ServiceConfigManager scm = new ServiceConfigManager(
                serviceName, ssoToken);
            ServiceConfig sc = scm.getGlobalConfig(null);
            StringTokenizer tokenizer = new StringTokenizer(subConfigName, "/");
            int tokenCount = tokenizer.countTokens();

            for (int i = 1; i <= tokenCount; i++) {
                String scn = SMSSchema.unescapeName(tokenizer.nextToken());

                if (i != tokenCount) {
                    sc = sc.getSubConfig(scn);
                } else {
                    if (subConfigId == null) {
                        subConfigId = subConfigName;
                    }

                    sc.addSubConfig(scn, subConfigId, 0, map);

                    String[] params = {subConfigName, serviceName};
                    String logMsg = bundle.getString(
                        "added-sub-configuration-to-default");
//                    AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//                        MessageFormat.format(logMsg, params));
                    AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                        Level.INFO,
                        AdminUtils.ADD_SUB_CONFIGURATION_TO_DEFAULT, params);
                }
            }
        } catch (Exception e) {
            debug.error("AdminXMLParse.addSubConfigToDefault", e);
            throw new AdminException(e.toString());
        }
    }

    private void modifySubConfigToDefault(
        String serviceName,
        Node childNode,
        SSOToken ssoToken
    ) throws AdminException {
        String subConfigName = ((Element)childNode).getAttribute(
            "subConfigName");

        String operation = ((Element)childNode).getAttribute("operation");
        if ((operation == null) || (operation.length() == 0)) {
            operation = "set";
        }

        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                serviceName, ssoToken);
            ServiceConfig sc = scm.getGlobalConfig(null);

            if (sc != null) {
                StringTokenizer tokenizer = new StringTokenizer(
                    subConfigName, "/");
                while (tokenizer.hasMoreTokens()) {
                    String scn = SMSSchema.unescapeName(tokenizer.nextToken());
                    sc = sc.getSubConfig(scn);
                }

                Map map = XMLUtils.parseAttributeValuePairTags(childNode);

                if (operation.equals("set")) {
                    sc.setAttributes(map);
                } else if (operation.equals("add")) {
                    for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
                        String attrName = (String)i.next();
                        sc.addAttribute(attrName, (Set)map.get(attrName));
                    }
                } else {
                    for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
                        sc.removeAttribute((String)i.next());
                    }
                }

                String[] params = {subConfigName, serviceName};
                String logMsg = bundle.getString(
                    "modified-sub-configuration-to-default");
//                AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//                    MessageFormat.format(logMsg, params));
                AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                    Level.INFO,
                    AdminUtils.MODIFY_SUB_CONFIGURATION_TO_DEFAULT, params);

            } else {
                String[] param = {subConfigName, serviceName};
                throw new AdminException(
                    MessageFormat.format(bundle.getString("subconfigNotFound"),
                        param));
            }
        } catch (SSOException e) {
            throw new AdminException(e);
        } catch (SMSException e) {
            throw new AdminException(e);
        }
    }

    private void removeSubConfigFromDefault(String serviceName, Node childNode,
        SSOToken ssoToken)
        throws AdminException
    {
        try {
            String subConfigName = ((Element)childNode).getAttribute(
                "subConfigName");
            ServiceConfigManager scm = new ServiceConfigManager(
                serviceName, ssoToken);
            ServiceConfig sc = scm.getGlobalConfig(null);
            StringTokenizer tokenizer = new StringTokenizer(subConfigName, "/");
            int tokenCount = tokenizer.countTokens();

            for (int i = 1; i <= tokenCount; i++) {
                String scn = tokenizer.nextToken();

                if (i != tokenCount) {
                    sc = sc.getSubConfig(scn);
                } else {
                    sc.removeSubConfig(scn);

                    String[] params = {subConfigName, serviceName};
                    String logMsg = bundle.getString(
                        "deleted-sub-configuration-to-default");
                    AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                        Level.INFO,
                        AdminUtils.DELETE_SUB_CONFIGURATION_TO_DEFAULT,
                        params);
                }
            }
        } catch (Exception e) {
            debug.error("AdminXMLParse.removeSubConfigFromDefault", e);
            throw new AdminException(e);
        }
    }

    private void getSubConfiguration(
        String realm,
        String serviceName,
        Node childNode,
        SSOToken ssoToken
    ) throws AdminException {
        String subConfigName = ((Element)childNode).getAttribute(
            "subConfigName");
        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                serviceName, ssoToken);
            ServiceConfig sc = null;

            if ((realm != null) && (realm.length() > 0)) {
                sc = scm.getOrganizationConfig(realm, null);
            } else {
                sc = scm.getGlobalConfig(null);
            }

            // there may be not organization configuration
            if (sc == null) {
                System.out.println(bundle.getString(
                    "no-organization-sub-configuration"));
            } else {
                StringTokenizer st =  new StringTokenizer(subConfigName, "/");

                while (st.hasMoreTokens()) {
                    sc =sc.getSubConfig(SMSSchema.unescapeName(st.nextToken()));
                }

                StringWriter stringWriter = new StringWriter();
                PrintWriter prnWriter = new PrintWriter(stringWriter);
                PrintUtils prnUtl = new PrintUtils(prnWriter);

                String[] params = {subConfigName, serviceName};
                String logsMsg = bundle.getString("get-sub-configuration");
                prnWriter.println(MessageFormat.format(logsMsg, params));

                prnUtl.printAVPairs(sc.getAttributes());
                prnWriter.flush();
                System.out.println(stringWriter.toString());

                AdminUtils.logOperation(AdminUtils.LOG_ACCESS, Level.INFO,
                    AdminUtils.GET_SUB_CONFIGURATION, params);
            }
        } catch (SSOException e) {
            debug.error("AdminXMLParser.getSubConfiguration", e);
            throw new AdminException(e);
        } catch (SMSException e) {
            debug.error("AdminXMLParser.getSubConfiguration", e);
            throw new AdminException(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteAllServiceConfiguration(String serviceName,
        Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        if(serviceName != null) {
            String userAttribute = ((Element)childNode).getAttribute("userAtt");
            boolean bUserAttribute =
                Boolean.valueOf(userAttribute).booleanValue();

            try {
                ServiceManager sm = new ServiceManager(ssoToken);

                if (AdminUtils.logEnabled()) {
                    AdminUtils.log(bundle.getString("statusmsg12") +
                        serviceName);
                }

                if (serviceName.equalsIgnoreCase(Main.AUTH_CORE_SERVICE)) {
                    sm.deleteService(serviceName);
                } else {
                    Set versions = sm.getServiceVersions(serviceName);

                    for (Iterator iter = versions.iterator(); iter.hasNext(); )
                    {
                        String version = (String)iter.next();
                        ServiceUtil sutil = new ServiceUtil(ssoToken, debug);

                        if (sutil.deleteService(
                            serviceName, bUserAttribute, version)
                        ) {
                            sm.removeService(serviceName, version);
                        } else {
                            debug.error("Service Config deletion failed");
                        }
                    }
                }

                String[] param = {serviceName};
                String logMessage = bundle.getString(
                    "deleted-all-configurations");
                AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
                    Level.INFO, AdminUtils.DELETE_ALL_CONFIGURATIONS, param);
            } catch(Exception e) {
                debug.error("AdminXMLParser.deleteAllServiceConfiguration", e);
                throw new AdminException(e.toString());
            }
        }
    }

    private void doUserRequests(Node node, AMStoreConnection connection)
        throws AdminException
    {
        String userDN =((Element)node).getAttribute("DN");
        validateObjectType(connection, userDN, AMObject.USER);
        NodeList list = node.getChildNodes();
        int len = list.getLength();

        for (int i = 0; i < len; i++) {
            Node childNode = list.item(i);

            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                String strChildNodeName = childNode.getNodeName();
                int opt = getToken(strChildNodeName);

                try {
                    switch(opt){
                    case RS:
                        registerServicesToUser(userDN, childNode, connection);
                        break;
                    case URS:
                        unregisterServicesToUser(userDN, childNode, connection);
                        break;
                  }
                } catch (AdminException ae) {
                    if (!continueFlag) {
                      throw ae;
                    }
                }
            }
        }
    }

    private void registerServicesToUser(String userDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        UserRegisterServicesReq dpUserRegisterServices =
            new UserRegisterServicesReq(userDN);
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpUserRegisterServices.registerServicesReq((String)iter.next());
            }

            if (AdminUtils.logEnabled())  {
                AdminUtils.log(dpUserRegisterServices.toString());
            }

            dpUserRegisterServices.process(connection);
        }
    }

    private void unregisterServicesToUser(String userDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        UserUnregisterServicesReq dpUserUnregisterServices =
            new UserUnregisterServicesReq(userDN);
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpUserUnregisterServices.unregisterServicesReq(
                    (String)iter.next());
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpUserUnregisterServices.toString());
            }

            dpUserUnregisterServices.process(connection);
        }
    }

    private void doContainerRequests(Node node, AMStoreConnection connection,
        SSOToken ssot)
        throws AdminException
    {
        Node containerNode = node;
        String containerDN = ((Element)containerNode).getAttribute("DN");
        validateObjectType(connection, containerDN,
            AMObject.ORGANIZATIONAL_UNIT);
        NodeList nodeList = containerNode.getChildNodes();
        int len = nodeList.getLength();

        for (int i = 0; i < len; i++) {
            Node childNode = nodeList.item(i);

            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                int opt = getToken(childNode.getNodeName());

                try {
                    switch(opt) {
                    case CSC:
                        createSubContainerInContainer(containerDN, childNode,
                            connection);
                        break;
                    case MSC:
                        modifySubContainerInContainer(containerDN, childNode,
                            connection);
                        break;
                    case GST:
                        getServiceTemplateInContainer(containerDN, childNode,
                            connection);
                        break;
                    case MST:
                        modifyServiceTemplateInContainer(containerDN, childNode,
                            connection, ssot);
                        break;
                    case ASTAV:
                        addServiceTemplateAttrValuesInContainer(containerDN,
                            childNode, connection, ssot);
                        break;
                    case RSTAV:
                        removeServiceTemplateAttrValuesInContainer(containerDN,
                            childNode, connection, ssot);
                        break;
                    case CPC:
                        createPeopleContainerInContainer(containerDN, childNode,
                            connection);
                        break;
                    case CGC:
                        createGroupContainerInContainer(containerDN, childNode,
                            connection);
                        break;
                    case MPC:
                        modifyPeopleContainerInContainer(containerDN, childNode,
                            connection);
                        break;
                    case CG:
                        createGroupsInContainer(containerDN, childNode,
                            connection);
                        break;
                    case CU:
                        createUsersInContainer(containerDN, childNode,
                            connection, ssot);
                        break;
                    case CR:
                        createRolesInContainer(containerDN, childNode,
                            connection);
                        break;
                    case MR:
                        modifyRolesInContainer(containerDN, childNode,
                            connection);
                        break;
                    case GSC:
                        getSubContainersInContainer(containerDN, childNode,
                            connection);
                        break;
                    case GPC:
                        getPeopleContainersInContainer(containerDN, childNode,
                            connection);
                        break;
                    case GG:
                        getGroupsInContainer(containerDN, childNode,
                            connection);
                        break;
                    case GR:
                        getRolesInContainer(containerDN, childNode, connection);
                        break;
                    case GU:
                        getUsersInContainer(containerDN, childNode, connection);
                        break;
                    case DSC:
                        deleteSubContainersInContainer(containerDN, childNode,
                            connection);
                        break;
                    case DPC:
                        deletePeopleContainersInContainer(containerDN,
                            childNode, connection);
                        break;
                    case DG:
                        deleteGroupsInContainer(containerDN, childNode,
                            connection);
                        break;
                    case DR:
                        deleteRolesInContainer(containerDN, childNode,
                            connection);
                        break;
                    case DST:
                        deleteServiceTemplateInContainer(containerDN, childNode,
                            connection, ssot);
                        break;
                    case RS:
                        registerServicesInContainer(containerDN, childNode,
                            connection);
                        break;
                    case URS:
                        unregisterServicesInContainer(containerDN, childNode,
                            connection);
                        break;
                    case GRS:
                        getRegisteredServicesInContainer(containerDN,
                            connection);
                        break;
                    case GNOS:
                        getNumberOfServicesInContainer(containerDN,
                            connection);
                        break;
                    case CST:
                        createServiceTemplatesInContainer(containerDN,
                            childNode, connection, ssot);
                        break;
                    case CE:
                        createEntityInContainer(containerDN,
                            childNode, connection, ssot);
                        break;
                    case ME:
                        modifyEntityInContainer(containerDN,
                            childNode, connection);
                        break;
                    case DE:
                        deleteEntitiesInContainer(containerDN,
                            childNode, connection);
                        break;
                    case GE:
                        getEntitiesInContainer(containerDN, childNode,
                            connection);
                        break;
                    }
                } catch (AdminException ae) {
                    if (!continueFlag) {
                        throw ae;
                    }
                }
            }
        }
    }

    private void createSubContainerInContainer(String containerDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        ContCreateSubContReq dpContCreateSubContReq =
            new ContCreateSubContReq(containerDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element)childNode).getAttribute("createDN");
        dpContCreateSubContReq.addSubContReq(createDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContCreateSubContReq.toString());
        }

        dpContCreateSubContReq.process(connection);
    }

    private void modifySubContainerInContainer(String containerDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        ContModifySubContReq dpContModifySubContReq =
            new ContModifySubContReq(containerDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String modifyDN = ((Element)childNode).getAttribute("modifyDN");
        dpContModifySubContReq.addSubContReq(modifyDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContModifySubContReq.toString());
        }

        dpContModifySubContReq.process(connection);
    }

    private void getServiceTemplateInContainer(String containerDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        ContGetServiceTemplateReq dpContGetServiceTmplReq
            = new ContGetServiceTemplateReq(containerDN);

        String serviceName = ((Element)childNode).getAttribute("serviceName");
        String schemaType = ((Element)childNode).getAttribute("schemaType");
        dpContGetServiceTmplReq.setServiceNameSchemaType(
            serviceName, schemaType);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContGetServiceTmplReq.toString());
        }

        dpContGetServiceTmplReq.process(connection);
    }

    private void modifyServiceTemplateInContainer(String containerDN,
        Node childNode, AMStoreConnection connection, SSOToken ssoToken)
        throws AdminException
    {
        ContModifyServiceTemplateReq dpContModifyServiceTmplReq
            = new ContModifyServiceTemplateReq(containerDN);

        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String serviceName = ((Element)childNode).getAttribute("serviceName");
        String schemaType = ((Element)childNode).getAttribute("schemaType");
        String level = ((Element)childNode).getAttribute("level");
        String roleTemplate = ((Element)childNode).getAttribute("roleTemplate");

        if (!schemaType.equalsIgnoreCase("dynamic") &&
            roleTemplate.equalsIgnoreCase("true")
        ) {
            throw new UnsupportedOperationException(
                "\n"+bundle.getString("roletemplateexception"));
        }

        dpContModifyServiceTmplReq.addServiceTemplateReq(serviceName,
            schemaType, level, roleTemplate, map, ssoToken);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContModifyServiceTmplReq.toString());
        }

        dpContModifyServiceTmplReq.process(connection);
    }

    private void addServiceTemplateAttrValuesInContainer(
        String containerDN,
        Node childNode,
        AMStoreConnection connection,
        SSOToken ssoToken
    ) throws AdminException
    {
        ContAddServiceTemplateAttrValuesReq request =
            new ContAddServiceTemplateAttrValuesReq(containerDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String serviceName = ((Element)childNode).getAttribute("serviceName");
        String schemaType = ((Element)childNode).getAttribute("schemaType");
        String level = ((Element)childNode).getAttribute("level");
        String roleTemplate = ((Element)childNode).getAttribute("roleTemplate");

        if (!schemaType.equalsIgnoreCase("dynamic") &&
            roleTemplate.equalsIgnoreCase("true"))
        {
            throw new UnsupportedOperationException(
                bundle.getString("roletemplateexception"));
        }

        request.addRequest(serviceName, schemaType, level, roleTemplate,
            map, ssoToken);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(request.toString());
        }

        request.process(connection);
    }


    private void removeServiceTemplateAttrValuesInContainer(
        String containerDN,
        Node childNode,
        AMStoreConnection connection,
        SSOToken ssoToken
    ) throws AdminException
    {
        ContRemoveServiceTemplateAttrValuesReq request =
            new ContRemoveServiceTemplateAttrValuesReq(containerDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String serviceName = ((Element)childNode).getAttribute("serviceName");
        String schemaType = ((Element)childNode).getAttribute("schemaType");
        String level = ((Element)childNode).getAttribute("level");
        String roleTemplate = ((Element)childNode).getAttribute("roleTemplate");

        if (!schemaType.equalsIgnoreCase("dynamic") &&
            roleTemplate.equalsIgnoreCase("true"))
        {
            throw new UnsupportedOperationException(
            bundle.getString("roletemplateexception"));
        }

        request.addRequest(serviceName, schemaType, level, roleTemplate,
            map, ssoToken);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(request.toString());
        }

        request.process(connection);
    }

    private void createPeopleContainerInContainer(String containerDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        ContCreatePCReq dpContCreatePCReq = new ContCreatePCReq(containerDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element)childNode).getAttribute("createDN");
        dpContCreatePCReq.addPCReq(createDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContCreatePCReq.toString());
        }

        dpContCreatePCReq.process(connection);
    }

    private void createGroupContainerInContainer(String containerDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        ContCreateGroupContainerReq request =
            new ContCreateGroupContainerReq(containerDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element)childNode).getAttribute("createDN");
        request.addGroupContainerRequest(createDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(request.toString());
        }

        request.process(connection);
    }

    private void modifyPeopleContainerInContainer(String containerDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        ContModifyPCReq dpContModifyPCReq = new ContModifyPCReq(containerDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String modifyDN = ((Element)childNode).getAttribute("modifyDN");
        dpContModifyPCReq.addPCReq(modifyDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContModifyPCReq.toString());
        }

        dpContModifyPCReq.process(connection);
    }

    private void createUsersInContainer(
        String containerDN,
        Node childNode,
        AMStoreConnection connection,
        SSOToken ssoToken
    ) throws AdminException 
    {
        ContCreateUserReq dpContCreateUsers =
            new ContCreateUserReq(containerDN, ssoToken);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element) childNode).getAttribute("createDN");
        dpContCreateUsers.addUserReq(createDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContCreateUsers.toString());
        }

        dpContCreateUsers.process(connection);
    }

    private void createEntityInOrganization(
        String orgDN,
        Node childNode,
        AMStoreConnection connection,
        SSOToken ssoToken
    ) throws AdminException
    {
        OrgCreateEntityReq handle = new OrgCreateEntityReq(orgDN, ssoToken);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element)childNode).getAttribute("createDN");
        String entityType = ((Element)childNode).getAttribute("entityType");
        handle.addRequest(createDN, map);
        handle.setEntityType(entityType);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(handle.toString(connection));
        }

        handle.process(connection);
    }

    private void createEntityInContainer(
        String containerDN,
        Node childNode,
        AMStoreConnection connection,
        SSOToken ssoToken
    ) throws AdminException
    {
        ContCreateEntityReq handle = new ContCreateEntityReq(
            containerDN, ssoToken);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element)childNode).getAttribute("createDN");
        String entityType = ((Element)childNode).getAttribute("entityType");
        handle.setEntityType(entityType);
        handle.addEntity(createDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(handle.toString(connection));
        }

        handle.process(connection);
    }

    private void modifyEntityInOrganization(
        String orgDN,
        Node childNode,
        AMStoreConnection connection
    ) throws AdminException
    {
        OrgModifyEntityReq handle = new OrgModifyEntityReq(orgDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String modifyDN = ((Element)childNode).getAttribute("modifyDN");
        String entityType = ((Element)childNode).getAttribute("entityType");
        handle.setEntityType(entityType);
        handle.addRequest(modifyDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(handle.toString());
        }

        handle.process(connection);
    }

    private void modifyEntityInContainer(
        String containerDN,
        Node childNode,
        AMStoreConnection connection
    ) throws AdminException
    {
        ContModifyEntityReq handle = new ContModifyEntityReq(containerDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String modifyDN = ((Element)childNode).getAttribute("modifyDN");
        String entityType = ((Element)childNode).getAttribute("entityType");
        handle.setEntityType(entityType);
        handle.addRequest(modifyDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(handle.toString());
        }

        handle.process(connection);
    }

    private void deleteEntitiesInOrganization(
        String orgDN,
        Node childNode,
        AMStoreConnection connection
    ) throws AdminException
    {
        OrgDeleteEntitiesReq handle = new OrgDeleteEntitiesReq(orgDN);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());
        String entityType = ((Element)childNode).getAttribute("entityType");
        handle.setEntityType(entityType);

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                handle.addDNSet((String)iter.next());
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(handle.toString());
            }

            handle.process(connection);
        }
    }

    private void deleteEntitiesInContainer(
        String containerDN,
        Node childNode,
        AMStoreConnection connection
    ) throws AdminException
    {
        ContDeleteEntitiesReq handle = new ContDeleteEntitiesReq(containerDN);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());
        String entityType = ((Element)childNode).getAttribute("entityType");
        handle.setEntityType(entityType);

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                handle.addDNSet((String)iter.next());
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(handle.toString());
            }

            handle.process(connection);
        }
    }

    private void createGroupsInContainer(String containerDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        ContCreateGroupReq dpContCreateGroups = new ContCreateGroupReq(
            containerDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element)childNode).getAttribute("createDN");
        String groupType = ((Element)childNode).getAttribute("groupType");
        dpContCreateGroups.addGroupReq(createDN, groupType, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContCreateGroups.toString());
        }

        dpContCreateGroups.process(connection);
    }

    private void createRolesInContainer(String containerDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        ContCreateRoleReq reqHandler = null;
        String roleType = ((Element)childNode).getAttribute("roleType");

        if (roleType.equals(AdminReq.ROLE_TYPE_FILTERED)) {
            reqHandler = new ContCreateFilteredRoleReq(containerDN);
        } else {
            reqHandler = new ContCreateRoleReq(containerDN);
        }

        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element)childNode).getAttribute("createDN");
        reqHandler.addRoleReq(createDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(reqHandler.toString());
        }

        reqHandler.process(connection);
    }

    private void modifyRolesInContainer(String containerDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        ContModifyRoleReq dpContModifyRoles =
            new ContModifyRoleReq(containerDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String modifyDN = ((Element)childNode).getAttribute("modifyDN");
        dpContModifyRoles.addRoleReq(modifyDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContModifyRoles.toString());
        }

        dpContModifyRoles.process(connection);
    }

    private void getSubContainersInContainer(String containerDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        ContGetSubContReq dpContGetSubContReq =
            new ContGetSubContReq(containerDN);
        String strDNsOnly = ((Element)childNode).getAttribute("DNsOnly");
        boolean bDNsOnly = Boolean.valueOf(strDNsOnly).booleanValue();
        dpContGetSubContReq.setDNsOnly(bDNsOnly);
        dpContGetSubContReq.setFilter(
            ((Element)childNode).getAttribute("filter"));
        dpContGetSubContReq.setSizeLimit(
            ((Element)childNode).getAttribute("sizeLimit"));
        dpContGetSubContReq.setTimeLimit(
            ((Element)childNode).getAttribute("timeLimit"));
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            dpContGetSubContReq.addSubContDNs((String)iter.next());
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContGetSubContReq.toString());
        }

        dpContGetSubContReq.process(connection);
    }

    private void getPeopleContainersInContainer(String containerDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        ContGetPCReq dpContGetPCReq = new ContGetPCReq(containerDN);
        String strDNsOnly = ((Element)childNode).getAttribute("DNsOnly");
        boolean bDNsOnly = Boolean.valueOf(strDNsOnly).booleanValue();
        dpContGetPCReq.setDNsOnly(bDNsOnly);
        dpContGetPCReq.setFilter(
            ((Element)childNode).getAttribute("filter"));
        dpContGetPCReq.setSizeLimit(
            ((Element)childNode).getAttribute("sizeLimit"));
        dpContGetPCReq.setTimeLimit(
            ((Element)childNode).getAttribute("timeLimit"));
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            dpContGetPCReq.addPCDNs((String)iter.next());
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContGetPCReq.toString());
        }

        dpContGetPCReq.process(connection);
    }

    private void getGroupsInContainer(String containerDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        ContGetGroupReq dpContGetGroups = new ContGetGroupReq(containerDN);
        dpContGetGroups.setLevel(validateLevel(childNode));
        dpContGetGroups.setFilter(
            ((Element)childNode).getAttribute("filter"));
        dpContGetGroups.setSizeLimit(
            ((Element)childNode).getAttribute("sizeLimit"));
        dpContGetGroups.setTimeLimit(
            ((Element)childNode).getAttribute("timeLimit"));

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContGetGroups.toString());
        }

        dpContGetGroups.process(connection);
    }

    private void getRolesInContainer(String containerDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        ContGetRoleReq dpContGetRoles = new ContGetRoleReq(containerDN);
        dpContGetRoles.setLevel(validateLevel(childNode));
        dpContGetRoles.setFilter(
            ((Element)childNode).getAttribute("filter"));
        dpContGetRoles.setSizeLimit(
            ((Element)childNode).getAttribute("sizeLimit"));
        dpContGetRoles.setTimeLimit(
            ((Element)childNode).getAttribute("timeLimit"));

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContGetRoles.toString());
        }

        dpContGetRoles.process(connection);
    }

    private void getUsersInContainer(String containerDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        ContGetUserReq dpContGetUsers = new ContGetUserReq(containerDN);
        String strDNsOnly = ((Element)childNode).getAttribute("DNsOnly");
        boolean bDNsOnly = Boolean.valueOf(strDNsOnly).booleanValue();
        dpContGetUsers.setDNsOnly(bDNsOnly);
        dpContGetUsers.setFilter(((Element)childNode).getAttribute("filter"));
        dpContGetUsers.setSizeLimit(
            ((Element)childNode).getAttribute("sizeLimit"));
        dpContGetUsers.setTimeLimit(
            ((Element)childNode).getAttribute("timeLimit"));

        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            dpContGetUsers.addUserDNs((String)iter.next());
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContGetUsers.toString());
        }

        dpContGetUsers.process(connection);
    }

    private void deleteSubContainersInContainer(String containerDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        ContDeleteSubContReq dpContDeleteSubContReq =
            new ContDeleteSubContReq(containerDN);
        String strRecursive = ((Element)childNode).getAttribute(
            "deleteRecursively");
        boolean recursive = Boolean.valueOf(strRecursive).booleanValue();
        dpContDeleteSubContReq.setRecursiveDelete(recursive);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpContDeleteSubContReq.addDNSet((String)iter.next());
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpContDeleteSubContReq.toString());
            }

            dpContDeleteSubContReq.process(connection);
        }
    }

    private void deletePeopleContainersInContainer(String containerDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        ContDeletePCReq dpContDeletePCReq = new ContDeletePCReq(containerDN);
        String strRecursive = ((Element)childNode).getAttribute(
            "deleteRecursively");
        boolean recursive = Boolean.valueOf(strRecursive).booleanValue();
        dpContDeletePCReq.setRecursiveDelete(recursive);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpContDeletePCReq.addDNSet((String)iter.next());
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpContDeletePCReq.toString());
            }

            dpContDeletePCReq.process(connection);
        }
    }

    private void deleteGroupsInContainer(String containerDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        ContDeleteGroupReq dpContDeleteGroupReq =
            new ContDeleteGroupReq(containerDN);
        String strRecursive = ((Element)childNode).getAttribute(
            "deleteRecursively");
        boolean recursive = Boolean.valueOf(strRecursive).booleanValue();
        dpContDeleteGroupReq.setRecursiveDelete(recursive);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpContDeleteGroupReq.addDNSet((String)iter.next());
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpContDeleteGroupReq.toString());
            }

            dpContDeleteGroupReq.process(connection);
        }
    }

    private void deleteRolesInContainer(String containerDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        ContDeleteRoleReq dpContDeleteRoleReq =
            new ContDeleteRoleReq(containerDN);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpContDeleteRoleReq.addDNSet((String)iter.next());
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpContDeleteRoleReq.toString());
            }

            dpContDeleteRoleReq.process(connection);
        }
    }

    private void deleteServiceTemplateInContainer(String containerDN,
        Node childNode, AMStoreConnection connection, SSOToken ssoToken)
        throws AdminException
    {
        ContDeleteServiceTemplateReq dpContDeleteServiceTemplateReq
            = new ContDeleteServiceTemplateReq(containerDN);
        String serviceName = ((Element)childNode).getAttribute("serviceName");
        String schemaType = ((Element)childNode).getAttribute("schemaType");
        dpContDeleteServiceTemplateReq.setServiceTemplateReq(
            serviceName, schemaType);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContDeleteServiceTemplateReq.toString());
        }

        dpContDeleteServiceTemplateReq.process(connection, ssoToken);
    }

    private void registerServicesInContainer(String containerDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        ContRegisterServicesReq dpContRegisterServices =
            new ContRegisterServicesReq(containerDN);
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpContRegisterServices.registerServicesReq((String)iter.next());
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpContRegisterServices.toString());
            }

            dpContRegisterServices.process(connection);
        }
    }

    private void unregisterServicesInContainer(String containerDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        ContUnregisterServicesReq dpContUnregisterServices =
            new ContUnregisterServicesReq(containerDN);
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpContUnregisterServices.unregisterServicesReq(
                    (String)iter.next());
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpContUnregisterServices.toString());
            }

            dpContUnregisterServices.process(connection);
        }
    }

    private void getRegisteredServicesInContainer(String containerDN,
        AMStoreConnection connection)
        throws AdminException
    {
        ContGetRegisteredServicesReq dpContGetRegisteredServices =
            new ContGetRegisteredServicesReq(containerDN);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContGetRegisteredServices.toString());
        }

        dpContGetRegisteredServices.process(connection);
    }

    private void getNumberOfServicesInContainer(String containerDN,
        AMStoreConnection connection)
        throws AdminException
    {
        ContGetNumOfServicesReq dpContGetNumOfServicesReq =
            new ContGetNumOfServicesReq(containerDN);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpContGetNumOfServicesReq.toString());
        }

        dpContGetNumOfServicesReq.process(connection);
    }

    private void createServiceTemplatesInContainer(String containerDN,
        Node childNode, AMStoreConnection connection, SSOToken ssoToken)
        throws AdminException
    {
        ContCreateServiceTemplateReq dpContCreateServiceTemplate =
            new ContCreateServiceTemplateReq(containerDN);
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpContCreateServiceTemplate.addContServiceTmplReq(
                    (String)iter.next());
            }

            dpContCreateServiceTemplate.process(connection, ssoToken);

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpContCreateServiceTemplate.toString());
            }
        }
    }

    /**
     * @param nodeList is the list of all PeopleContainerRequests nodes
     * @param connection AMStoreConnection to the Sun Java System Identity
     *        Server SDK
     * @param ssot Single-Sign-On token.
     *
     * This method traverses through the PeopleContainerRequests nodes
     * and processes corresponding request objects
     */
    private void doPeopleContainerRequests(
        Node node,
        AMStoreConnection connection,
        SSOToken ssot
    ) throws AdminException
    {
        Node peopleContainerNode = node;
        String pcDN = ((Element)peopleContainerNode).getAttribute("DN");
        validateObjectType(connection, pcDN, AMObject.PEOPLE_CONTAINER);
        NodeList nodeList = peopleContainerNode.getChildNodes();
        int len = nodeList.getLength();

        for (int i = 0; i < len; i++) {
            Node childNode = nodeList.item(i);

            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                String strChildNodeName = childNode.getNodeName();
                int opt = getToken(strChildNodeName);

                try {
                    switch(opt){
                    case CSPC:
                        createSubPeopleContainerInPeopleContainer(pcDN,
                            childNode, connection);
                        break;
                    case MPC:
                        modifySubPeopleContainerInPeopleContainer(pcDN,
                            childNode, connection);
                        break;
                    case CU:
                        createUserInPeopleContainer(pcDN, childNode,
                            connection, ssot);
                        break;
                    case MU:
                        modifyUserInPeopleContainer(pcDN, childNode,
                            connection);
                        break;
                    case DSPC:
                        deleteSubPeopleContainersInPeopleContainer(pcDN,
                            childNode, connection);
                        break;
                    case DU:
                        deleteUsersInPeopleContainer(pcDN, childNode,
                            connection);
                        break;
                    case GNOU:
                        getNumberOfUsersInPeopleContainer(pcDN, connection);
                        break;
                    case GU:
                        getUsersInPeopleContainer(pcDN, childNode, connection);
                        break;
                    case GSPC:
                        getSubPeopleContainersInPeopleContainer(pcDN, childNode,
                            connection);
                        break;
                    }
                } catch (AdminException ae) {
                    if (!continueFlag) {
                        throw ae;
                    }
                }
            }
        }
    }

    private void createSubPeopleContainerInPeopleContainer(String pcDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        PCCreateSubPCReq dpPCCreateSubPCReq =
            new PCCreateSubPCReq(pcDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element)childNode).getAttribute("createDN");
        dpPCCreateSubPCReq.addSubPCReq(createDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpPCCreateSubPCReq.toString());
        }
        
        dpPCCreateSubPCReq.process(connection);
    }
    
    private void modifySubPeopleContainerInPeopleContainer(String pcDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        PCModifySubPCReq dpPCModifySubPCReq =
            new PCModifySubPCReq(pcDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String modifyDN = ((Element)childNode).getAttribute("modifyDN");
        dpPCModifySubPCReq.addSubPCReq(modifyDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpPCModifySubPCReq.toString());
        }
        
        dpPCModifySubPCReq.process(connection);
    }

    private void createUserInPeopleContainer(
        String pcDN,
        Node childNode,
        AMStoreConnection connection,
        SSOToken ssoToken
    ) throws AdminException
    {
        PCCreateUserReq dpPCCreateUserReq = new PCCreateUserReq(pcDN, ssoToken);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element)childNode).getAttribute("createDN");
        dpPCCreateUserReq.addUserReq(createDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpPCCreateUserReq.toString());
        }
        
        dpPCCreateUserReq.process(connection);
    }

    private void modifyUserInPeopleContainer(String pcDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        PCModifyUserReq dpPCModifyUserReq = new PCModifyUserReq(pcDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String modifyDN = ((Element)childNode).getAttribute("modifyDN");
        dpPCModifyUserReq.addUserReq(modifyDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpPCModifyUserReq.toString());
        }
        
        dpPCModifyUserReq.process(connection);
    }
    
    private void deleteSubPeopleContainersInPeopleContainer(String pcDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        PCDeleteSubPCReq dpPCDeleteSubPCReq = new PCDeleteSubPCReq(pcDN);
        String strRecursive =
            ((Element)childNode).getAttribute("deleteRecursively");
        boolean recursive = Boolean.valueOf(strRecursive).booleanValue();
        dpPCDeleteSubPCReq.setRecursiveDelete(recursive);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());
        
        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpPCDeleteSubPCReq.addDNSet((String)iter.next());
            }
            
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpPCDeleteSubPCReq.toString());
            }
            
            dpPCDeleteSubPCReq.process(connection);
        }
    }
    
    private void deleteUsersInPeopleContainer(String pcDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        PCDeleteUserReq dpPCDeleteUserReq = new PCDeleteUserReq(pcDN);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());
        
        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpPCDeleteUserReq.addDNSet((String)iter.next());
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpPCDeleteUserReq.toString());
            }
            
            dpPCDeleteUserReq.process(connection);
        }
    }
    
    private void getNumberOfUsersInPeopleContainer(String pcDN,
        AMStoreConnection connection)
        throws AdminException
    {
        PCGetNumOfUserReq dpPCGetNumOfUserReq = new PCGetNumOfUserReq(pcDN);
        
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpPCGetNumOfUserReq.toString());
        }
        
        dpPCGetNumOfUserReq.process(connection);
    }
    
    private void getUsersInPeopleContainer(String pcDN, Node childNode,
        AMStoreConnection connection)
        throws AdminException
    {
        PCGetUserReq dpPCGetUserReq = new PCGetUserReq(pcDN);

        String strDNsOnly = ((Element)childNode).getAttribute("DNsOnly");
        boolean DNsOnly = Boolean.valueOf(strDNsOnly).booleanValue();
        dpPCGetUserReq.setDNsOnly(DNsOnly);
        dpPCGetUserReq.setFilter(((Element)childNode).getAttribute("filter"));
        dpPCGetUserReq.setTimeLimit(
            ((Element)childNode).getAttribute("timeLimit"));
        dpPCGetUserReq.setSizeLimit(
            ((Element)childNode).getAttribute("sizeLimit"));

        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());
        
        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpPCGetUserReq.addUserDNs((String)iter.next());
            }
        }
            
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpPCGetUserReq.toString());
        }

        dpPCGetUserReq.process(connection);
    }
    
    private void getSubPeopleContainersInPeopleContainer(String pcDN,
        Node childNode, AMStoreConnection connection)
        throws AdminException
    {
        PCGetSubPCReq dpPCGetSubPCReq = new PCGetSubPCReq(pcDN);
        dpPCGetSubPCReq.setLevel(validateLevel(childNode));
        String strDNsOnly = ((Element)childNode).getAttribute("DNsOnly");
        boolean DNsOnly = Boolean.valueOf(strDNsOnly).booleanValue();
        dpPCGetSubPCReq.setDNsOnly(DNsOnly);
        dpPCGetSubPCReq.setFilter(((Element)childNode).getAttribute("filter"));
        dpPCGetSubPCReq.setSizeLimit(
            ((Element)childNode).getAttribute("sizeLimit"));
        dpPCGetSubPCReq.setTimeLimit(
            ((Element)childNode).getAttribute("timeLimit"));
        
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());
        
        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpPCGetSubPCReq.addSubPCDNs((String)iter.next());
            }
        }
        
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpPCGetSubPCReq.toString());
        }
            
        dpPCGetSubPCReq.process(connection);
    }
    
    
    /**
     * @param nodeList is the list of all RoleRequests nodes
     * @param connection AMStoreConnection to the Sun Java System Identity
     *        Server SDK
     *
     * This method traverses through the RoleRequests nodes
     * and processes corresponding request objects
     */
    private void doRoleRequests(Node node, AMStoreConnection connection,
        SSOToken ssot)
        throws AdminException
    {
        Node roleNode = node;
        String roleDN =((Element)roleNode).getAttribute("DN");
        int objectType = validateObjectType(connection, roleDN, AMObject.ROLE);
        NodeList nodeList = roleNode.getChildNodes();
        int len = nodeList.getLength();

        for(int i = 0; i < len; i++){
            Node childNode = nodeList.item(i);

            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                String strChildNodeName = childNode.getNodeName();
                int opt = getToken(strChildNodeName);

                try {
                    switch(opt){
                    case CST:
                        createServiceTemplateInRole(roleDN, childNode,
                            connection, ssot);
                        break;
                    case GNOU:
                        getNumberOfUsersInRole(roleDN, connection, objectType);
                        break;
                    case GST:
                        getServiceTemplateInRole(roleDN, childNode, connection,
                            objectType);
                        break;
                    case MST:
                        modifyServiceTemplateInRole(roleDN, childNode,
                            connection, objectType);
                        break;
                    case GU:
                        getUsersInRole(roleDN, childNode, connection,
                            objectType);
                        break;
                    case RU:
                        removeUsersInRole(roleDN, childNode, connection);
                        break;
                    case AU:
                        addUsersToRole(roleDN, childNode, connection,
                            objectType);
                        break;
                    }
                } catch (AdminException ae) {
                    if (!continueFlag) {
                        throw ae;
                    }
                }
            }
        }
    }

    private void createServiceTemplateInRole(String roleDN, Node childNode,
        AMStoreConnection connection, SSOToken ssoToken)
        throws AdminException
    {
        RoleCreateServiceTemplateReq dpRoleCreateServiceTemplate
            = new RoleCreateServiceTemplateReq(roleDN);
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());
        
        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpRoleCreateServiceTemplate.addRoleServiceTmplReq(
                    (String)iter.next());
                dpRoleCreateServiceTemplate.process(connection, ssoToken);

            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpRoleCreateServiceTemplate.toString());
            }
        }
    }
    
    private void getNumberOfUsersInRole(String roleDN, 
        AMStoreConnection connection, int objectType)
        throws AdminException
    {
        RoleGetNumOfUserReq dpRoleGetNumOfUserReq =
            (objectType == AMObject.ROLE)
            ? new RoleGetNumOfUserReq(roleDN)
            : new FilteredRoleGetNumOfUserReq(roleDN);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpRoleGetNumOfUserReq.toString());
        }
        
        dpRoleGetNumOfUserReq.process(connection);
    }

    private void getServiceTemplateInRole(String roleDN, Node childNode,
        AMStoreConnection connection, int objectType)
        throws AdminException
    {
        RoleGetServiceTemplateReq dpRoleGetServiceTmplReq =
            (objectType == AMObject.ROLE)
            ? new RoleGetServiceTemplateReq(roleDN)
            : new FilteredRoleGetServiceTemplateReq(roleDN);
                           
        String serviceName = ((Element)childNode).getAttribute("serviceName");
        String schemaType = ((Element)childNode).getAttribute("schemaType");
        dpRoleGetServiceTmplReq.setServiceNameSchemaType(
            serviceName, schemaType);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpRoleGetServiceTmplReq.toString());
        }
        
        dpRoleGetServiceTmplReq.process(connection);
    }

    private void modifyServiceTemplateInRole(String roleDN, Node childNode,
        AMStoreConnection connection, int objectType)
        throws AdminException
    {
        RoleModifyServiceTemplateReq dpRoleModifyServiceTmplReq =
            (objectType == AMObject.ROLE)
            ? new RoleModifyServiceTemplateReq(roleDN)
            : new FilteredRoleModifyServiceTemplateReq(roleDN);
                           
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String serviceName = ((Element)childNode).getAttribute("serviceName");
        dpRoleModifyServiceTmplReq.addServiceTemplateReq(serviceName, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpRoleModifyServiceTmplReq.toString());
        }
        
        dpRoleModifyServiceTmplReq.process(connection);
    }

    private void getUsersInRole(String roleDN, Node childNode,
        AMStoreConnection connection, int objectType)
        throws AdminException
    {
        RoleGetUserReq dpRoleGetUserReq = (objectType == AMObject.ROLE)
            ? new RoleGetUserReq(roleDN) : new FilteredRoleGetUserReq(roleDN);

        String strDNsOnly = ((Element)childNode).getAttribute("DNsOnly");
        boolean DNsOnly = Boolean.valueOf(strDNsOnly).booleanValue();
        dpRoleGetUserReq.setDNsOnly(DNsOnly);
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());
        
        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpRoleGetUserReq.addUserDNs((String)iter.next());
            }
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpRoleGetUserReq.toString());
        }
            
        dpRoleGetUserReq.process(connection);
    }

    private void removeUsersInRole(
        String roleDN,
        Node childNode,
        AMStoreConnection connection
    ) throws AdminException
    {
        RoleDeleteUserReq request = new RoleDeleteUserReq(roleDN);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                request.addDNSet((String)iter.next());
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(request.toString());
            }

            request.process(connection);
        }
    }

    private void addUsersToRole(String roleDN, Node childNode,
        AMStoreConnection connection, int objectType)
        throws AdminException
    {
        if (objectType == AMObject.FILTERED_ROLE) {
            throw new AdminException(
                bundle.getString("cannotAddUsersToFilteredRole"));
        }

        RoleAddUserReq dpRoleAddUserReq = new RoleAddUserReq(roleDN);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());
        
        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpRoleAddUserReq.addDNSet((String)iter.next());
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(dpRoleAddUserReq.toString());
            }

            dpRoleAddUserReq.process(connection);
        }
    }
    
    private void doGroupRequests(Node node, AMStoreConnection connection)
        throws AdminException
    {
        Node groupNode = node;
        String groupDN = ((Element)groupNode).getAttribute("DN");
        int objectType = validateObjectType(
            connection, groupDN, AMObject.GROUP);
        NodeList nodeList = groupNode.getChildNodes();
        int len = nodeList.getLength();
        for (int i = 0; i < len; i++) {
            Node childNode = nodeList.item(i);

            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                String strChildNodeName = childNode.getNodeName();
                int opt = getToken(strChildNodeName);

                try {
                    switch (opt) {
                    case CSG:
                        createSubGroupInGroup(groupDN, childNode, connection,
                            objectType);
                        break;
                    case MSG:
                        modifySubGroupInGroup(groupDN, childNode, connection);
                        break;
                    case DSG:
                        deleteSubGroupInGroup(groupDN, childNode, connection);
                        break;
                    case GSG:
                        getSubGroupInGroup(groupDN, childNode, connection);
                        break;
                    case GNOU:
                        getNumberOfUsersInGroup(groupDN, connection,
                            objectType);
                        break;
                    case GU:
                        getUsersInGroup(groupDN, childNode, connection,
                            objectType);
                        break;
                    case RU:
                        removeUsersInGroup(groupDN, childNode, connection);
                        break;
                    case AU:
                        addUsersToGroup(groupDN, childNode, connection,
                            objectType);
                        break;
                    case ANG:
                        addNestedGroupsToGroup(groupDN, childNode, connection,
                            objectType);
                        break;
                    case GNG:
                        getNestedGroupsInGroup(groupDN, childNode, connection,
                            objectType);
                        break;
                    case GNONG:
                        getNumberOfNestedGroupsInGroup(groupDN, connection,
                            objectType);
                        break;
                    case RNG:
                        removeNestedGroupsFromGroup(groupDN, childNode,
                            connection, objectType);
                        break;
                    }
                } catch (AdminException ae) {
                    if (!continueFlag) {
                        throw ae;
                    }
                }
            }
        }
    }

    private void createSubGroupInGroup(String groupDN, Node childNode,
        AMStoreConnection connection, int objectType)
        throws AdminException
    {
        GroupCreateSubGroupReq reqHandler = null;

        if (objectType == AMObject.DYNAMIC_GROUP) {
            reqHandler = new DynamicGroupCreateSubGroupReq(groupDN);
        } else if (objectType == AMObject.ASSIGNABLE_DYNAMIC_GROUP) {
            reqHandler = new AssignableDynamicGroupCreateSubGroupReq(groupDN);
        } else {
            reqHandler = new GroupCreateSubGroupReq(groupDN);
        }

        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String createDN = ((Element)childNode).getAttribute("createDN");
        String groupType = ((Element)childNode).getAttribute("groupType");
        reqHandler.addSubGroupReq(createDN, groupType, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(reqHandler.toString());
        }
        
        reqHandler.process(connection);
    }
    
    private void modifySubGroupInGroup(
        String groupDN,
        Node childNode,
        AMStoreConnection connection
    ) throws AdminException
    {
        GroupModifySubGroupReq reqHandler = new GroupModifySubGroupReq(groupDN);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);
        String modifyDN = ((Element)childNode).getAttribute("modifyDN");
        reqHandler.addSubGroupReq(modifyDN, map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(reqHandler.toString());
        }
        
        reqHandler.process(connection);
    }
    
    private void deleteSubGroupInGroup(
        String groupDN,
        Node childNode,
        AMStoreConnection connection
    ) throws AdminException
    {
        GroupDeleteSubGroupReq reqHandler = new GroupDeleteSubGroupReq(groupDN);

        String strRecursive =
            ((Element)childNode).getAttribute("deleteRecursively");
        boolean recursive = Boolean.valueOf(strRecursive).booleanValue();
        reqHandler.setRecursiveDelete(recursive);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                reqHandler.addDNSet((String)iter.next());
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(reqHandler.toString());
            }
            
            reqHandler.process(connection);
        }
    }

    private void getSubGroupInGroup(
        String groupDN,
        Node childNode,
        AMStoreConnection connection
    ) throws AdminException
    {
        GroupGetSubGroupReq dpGroupGetSubGroupReq =
            new GroupGetSubGroupReq(groupDN);
        dpGroupGetSubGroupReq.setLevel(validateLevel(childNode));
        String strDNsOnly = ((Element)childNode).getAttribute("DNsOnly");
        boolean DNsOnly = Boolean.valueOf(strDNsOnly).booleanValue(); 
        dpGroupGetSubGroupReq.setDNsOnly(DNsOnly);
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                dpGroupGetSubGroupReq.addSubGroupDNs((String)iter.next());
            }
        }
        
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(dpGroupGetSubGroupReq.toString());
        }
            
        dpGroupGetSubGroupReq.process(connection);
    }
    
    private void getNumberOfUsersInGroup(String groupDN,
        AMStoreConnection connection, int objectType)
        throws AdminException
    {
        GroupGetNumOfUserReq reqHandler = null;

        if (objectType == AMObject.DYNAMIC_GROUP) {
            reqHandler = new DynamicGroupGetNumOfUserReq(groupDN);
        } else if (objectType == AMObject.ASSIGNABLE_DYNAMIC_GROUP) {
            reqHandler = new AssignableDynamicGroupGetNumOfUserReq(groupDN);
        } else {
            reqHandler = new GroupGetNumOfUserReq(groupDN);
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(reqHandler.toString());
        }
        
        reqHandler.process(connection);
    }

    private void getNumberOfNestedGroupsInGroup(
        String groupDN,
        AMStoreConnection connection,
        int objectType
    ) throws AdminException
    {
        GroupGetNumOfNestedGroupsReq reqHandler = null;

        switch (objectType) {
        case AMObject.DYNAMIC_GROUP:
            reqHandler = new DynamicGroupGetNumOfNestedGroupsReq(groupDN);
            break;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            reqHandler = new AssignableDynamicGroupGetNumOfNestedGroupsReq(
                groupDN);
            break;
        default:
            reqHandler = new GroupGetNumOfNestedGroupsReq(groupDN);
            break;
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(reqHandler.toString());
        }

        reqHandler.process(connection);
    }

    private void removeUsersInGroup(
        String groupDN,
        Node childNode,
        AMStoreConnection connection
    ) throws AdminException
    {
        GroupDeleteUserReq request = new GroupDeleteUserReq(groupDN);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                request.addDNSet((String)iter.next());
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(request.toString());
            }

            request.process(connection);
        }
    }

    private void getUsersInGroup(String groupDN, Node childNode,
        AMStoreConnection connection, int objectType)
        throws AdminException
    {
        GroupGetUserReq reqHandler = null;

        if (objectType == AMObject.DYNAMIC_GROUP) {
            reqHandler = new DynamicGroupGetUserReq(groupDN);
        } else if (objectType == AMObject.ASSIGNABLE_DYNAMIC_GROUP) {
            reqHandler = new AssignableDynamicGroupGetUserReq(groupDN);
        } else {
            reqHandler = new GroupGetUserReq(groupDN);
        }

        String strDNsOnly = ((Element)childNode).getAttribute("DNsOnly");
        boolean DNsOnly = Boolean.valueOf(strDNsOnly).booleanValue();
        reqHandler.setDNsOnly(DNsOnly);
        reqHandler.setFilter(((Element)childNode).getAttribute("filter"));
        reqHandler.setSizeLimit(((Element)childNode).getAttribute("sizeLimit"));
        reqHandler.setTimeLimit(((Element)childNode).getAttribute("timeLimit"));

        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                reqHandler.addUserDNs((String)iter.next());
            }
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(reqHandler.toString());
        }
            
        reqHandler.process(connection);
    }

    private void getNestedGroupsInGroup(
        String groupDN,
        Node childNode,
        AMStoreConnection connection,
        int objectType
    ) throws AdminException
    {
        GroupGetNestedGroupReq reqHandler = null;

        switch (objectType) {
        case AMObject.DYNAMIC_GROUP:
            reqHandler = new DynamicGroupGetNestedGroupReq(groupDN);
            break;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            reqHandler = new AssignableDynamicGroupGetNestedGroupReq(groupDN);
            break;
        default:
            reqHandler = new GroupGetNestedGroupReq(groupDN);
            break;
        }

        String strDNsOnly = ((Element)childNode).getAttribute("DNsOnly");
        boolean DNsOnly = Boolean.valueOf(strDNsOnly).booleanValue();
        reqHandler.setDNsOnly(DNsOnly);

        Set set = getSubNodeElementNodeValues(childNode.getChildNodes());

        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            reqHandler.addNestedGroupDNs((String)iter.next());
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(reqHandler.toString());
        }

        reqHandler.process(connection);
    }
    
    private void addUsersToGroup(String groupDN, Node childNode,
        AMStoreConnection connection, int objectType)
        throws AdminException
    {
        GroupAddUserReq reqHandler = null;

        if (objectType == AMObject.DYNAMIC_GROUP) {
            throw new UnsupportedOperationException(
                bundle.getString("cannotAddUsersToDynamicGroup"));
        } else if (objectType == AMObject.ASSIGNABLE_DYNAMIC_GROUP) {
            reqHandler = new AssignableDynamicGroupAddUserReq(groupDN);
        } else {
            reqHandler = new GroupAddUserReq(groupDN);
        }

        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                reqHandler.addDNSet((String)iter.next());
            }
        
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(reqHandler.toString());
            }
            
            reqHandler.process(connection);
        }
    }

    private void addNestedGroupsToGroup(
        String groupDN,
        Node childNode,
        AMStoreConnection connection,
        int objectType
    ) throws AdminException {
        GroupAddNestedGroupReq reqHandler = null;

        switch (objectType) {
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            reqHandler = new AssignableDynamicGroupAddNestedGroupReq(groupDN);
            break;
        case AMObject.GROUP:
        case AMObject.STATIC_GROUP:
            reqHandler = new GroupAddNestedGroupReq(groupDN);
            break;
        case AMObject.DYNAMIC_GROUP:
            reqHandler = new DynamicGroupAddNestedGroupReq(groupDN);
            break;
        }

        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                reqHandler.addDNSet((String)iter.next());
            }
        
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(reqHandler.toString());
            }
            
            reqHandler.process(connection);
        }
    }

    private void removeNestedGroupsFromGroup(
        String groupDN,
        Node childNode,
        AMStoreConnection connection,
        int objectType
    ) throws AdminException {
        GroupRemoveNestedGroupReq reqHandler = new GroupRemoveNestedGroupReq(
            groupDN);
        Set set = getSubNodeElementNodeDNValues(childNode.getChildNodes());

        if (!set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                reqHandler.addDNSet((String)iter.next());
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(reqHandler.toString());
            }

            reqHandler.process(connection);
        }
    }
    
    private PolicyManager getPolicyManager(SSOToken ssoToken, String orgDN,
        String taskI18nKey)
        throws AdminException
    {
        try {
            return new PolicyManager(ssoToken, orgDN);
        } catch (SSOException sse) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg30"),sse);
            }

            throw new AdminException(bundle.getString(taskI18nKey) +
                "\n\n" + sse.getLocalizedMessage());
        } catch (PolicyException pe) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg31"),pe);
            }

            throw new AdminException(bundle.getString(taskI18nKey) +
                "\n\n" + pe.getLocalizedMessage());
        }
    }

    private void doCreatePolicy(AMStoreConnection connection,
        Node childNode, SSOToken ssot)
        throws AdminException 
    {
        String orgDN = ((Element)childNode).getAttribute("createDN");

        try {
            AMOrganization org = connection.getOrganization(orgDN);
            if (AdminInterfaceUtils.inOrganizationalUnit(
                debug, connection, org)
            ) {
                throw new AdminException(bundle.getString(
                    "cannotCreatePolicyUnderContainer"));
            }

            PolicyManager pm = getPolicyManager(ssot, orgDN,
                "policycreatexception");
            NodeList nodeList = childNode.getChildNodes();
            int len = nodeList.getLength();

            for (int i = 0; i < len; i++) {
                Node policyChildNode = nodeList.item(i);

                if (policyChildNode.getNodeType() == Node.ELEMENT_NODE) {
                    addPolicy(orgDN, pm, policyChildNode);
                }
            }
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    private void addPolicy(String orgDN, PolicyManager pm, Node policyNode)
        throws AdminException
    {
        try {
            pm.addPolicy(new Policy(pm, policyNode));

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg32"));
                AdminUtils.log(orgDN);
            }
        } catch (SSOException sse) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg30"), sse);
            }

            throw new AdminException(bundle.getString("policycreatexception") +
                "\n\n" + sse.getLocalizedMessage());
        } catch (PolicyException pe) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg31"), pe);
            }

            throw new AdminException(bundle.getString("policycreatexception") +
                "\n\n" + pe.getLocalizedMessage());
        }
    }

/* Federation: Commented out
    private void doListAccts(Node childNode) {
        FedServicesRequestHandler fedReq = null;

        try {
            fedReq = new FedServicesRequestHandler();
            fedReq.ListAccts(childNode); 

            if (fedReq.errorMessage != null) {
                System.out.println(bundle.getString(fedReq.errorMessage));
            }                
        } catch(FSAccountMgmtException fsae) {
            System.out.println(bundle.getString(fedReq.errorMessage));
            debug.error("AdminXMLParser::Error in account manager:"
                + fsae.getMessage());   
        }

        if (AdminUtils.logEnabled()){           
            AdminUtils.log("AdminXMLParser:: Listed accounts");                
        }  
    }
*/
    
    /** Federation: Commented out
     * Converts the XML from the format that sticks to <code>liberty.xsd</code>
     * to that that sticks to <code>amAdmin.dtd</code>. The transformation is
     * achieved by using the <code>transform.xsl</code>, and thus doing
     * XSLT with the Stylesheet, mentioned.
     * The transformed file, is then used to created the providers.
     *
     public void processLibertyMetaData(
        String fileName,
        SSOToken ssot,
        String provType,
        String urlPrefix
    ) throws AdminException
    {
        //The Style sheet that has the transformation logic.    
         String   transformFile =
             "com/iplanet/am/admin/cli/transformMetaData.xsl";
             
         
        // The following sequence of the parameters are the probable parameters
        // that would be passed to the Style Sheet:
        // xslParam_ProvType ==> The type of the provider
        // xslParam_defaultUrlPrefix ==> The urlPrefix parameter that 
        // would be used for autopopulation
        // xslParam_cookieDomain ==> cookieDomain used for creation of     
        // hosted provider. 
          
        String xslParam_ProvType = "provType"; 
        String xslParam_defaultUrlPrefix = "defaultUrlPrefix";

        String PROVIDER_TYPE_REMOTE = "remote";
        String PROVIDER_TYPE_HOSTED = "hosted";
         
        FedServicesRequestHandler fedReq = null;
         
        try{                
            DocumentBuilderFactory factory =
               DocumentBuilderFactory.newInstance();
            DocumentBuilder  builder = factory.newDocumentBuilder();
            Document document;            
            DOMResult result = new DOMResult();  
            StreamResult result1 = new StreamResult(System.out);
            Node reqNode;
            String orgDN;
             
            document = builder.parse(fileName);
             
            JarFile jarFile = new JarFile("am_services.jar");
            InputStream fileToTransform = jarFile.getInputStream(
                jarFile.getEntry(transformFile));
             
            StreamSource styleSource = new StreamSource(fileToTransform); 
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(styleSource);
            DOMSource source = new DOMSource(document);
             
            transformer.setParameter(xslParam_ProvType, provType);            
             
            // Get the default org DN to perform the creation of operation.
            orgDN = SystemProperties.get("com.iplanet.am.defaultOrg");

            fedReq = new FedServicesRequestHandler(orgDN, ssot);
                   
            if (provType.equalsIgnoreCase(PROVIDER_TYPE_REMOTE)) {
                 transformer.transform(source, result);   
                 reqNode = ((org.w3c.dom.Node)(result.getNode()))
                    .getFirstChild();
                 fedReq.createRemoteProvider(reqNode);
            } 
            if (provType.equalsIgnoreCase(PROVIDER_TYPE_HOSTED)){
                // The parameters that need to be specified for a hosted
                // provider
                // They being :
                //   1) Cookie domain
                //   2) urlPrefix    
                // 
                transformer.setParameter(xslParam_defaultUrlPrefix, urlPrefix);
                transformer.transform(source, result);
                reqNode = ((org.w3c.dom.Node)(result.getNode())).
                    getFirstChild(); 
                fedReq.createHostedProvider(reqNode);
             }
         } catch (TransformerException te) {               
             System.err.println(bundle.getString("failedToProcessXML") + 
                 " "+ fileName+"\n" + te.getMessage());
             System.exit(1);            
         } catch(FSAllianceManagementException fsae){                        
             System.out.println(fsae.getMessage());
             debug.error("AdminXMLParser::Error in alliance manager:"
                        + fsae.getMessage());      
             
         } catch (Exception dae) {               
             System.err.println(bundle.getString("failedToProcessXML") + 
                 " "+ fileName+"\n");
             System.exit(1);
         }
    }
*/

    private void doDeletePolicy(Node childNode, SSOToken ssot)
        throws AdminException
    {
        String orgDN = ((Element)childNode).getAttribute("deleteDN");
        if (orgDN == null || orgDN.length() == 0)  {
            orgDN = ((Element)childNode).getAttribute("realm");
        }
        PolicyManager pm = getPolicyManager(ssot, orgDN,
            "policydeletexception");
        NodeList nodeList = childNode.getChildNodes();
        int len = nodeList.getLength();

        for (int i = 0; i < len; i++){
            Node deleteChildNode = nodeList.item(i);

            if (deleteChildNode.getNodeType() == Node.ELEMENT_NODE) {
                String policyName =
                    ((Element)deleteChildNode).getAttribute("name");
                deletePolicy(orgDN, policyName, pm);
            }
        }
    }

    private void deletePolicy(String orgDN, String policyName,
        PolicyManager pm)
        throws AdminException
    {
        try {
            pm.removePolicy(policyName);

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg33"));
                AdminUtils.log(bundle.getString("policy") + policyName);
                AdminUtils.log(bundle.getString("organization") + orgDN);
            }
        } catch (SSOException sse) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg30"), sse);
            }

            throw new AdminException(bundle.getString("policydeletexception") +
                "\n\n" + sse.getLocalizedMessage());
        } catch (PolicyException pe) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("statusmsg31"), pe);
            }

            throw new AdminException(bundle.getString("policydeletexception") +
                "\n\n" + pe.getLocalizedMessage()+"\n");
        }
    }

    private void doRealmRequests (Node node, SSOToken ssoToken)
        throws AdminException
    {
        Node realmNode = node;
        //
        //  don't know what sort of validation we want to do
        //  at this point.
        //
        NodeList realmChildNodesList = realmNode.getChildNodes();
        int realmChildNodesListLength = realmChildNodesList.getLength();

        for (int i = 0; i < realmChildNodesListLength; i++) {
            Node childNode = realmChildNodesList.item(i);

            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                String strChildNodeName = childNode.getNodeName();

                int opt = getToken(strChildNodeName);

                try {
                    switch(opt){
                        case DSREALM:        // DeleteRealm
                            doDeleteRealm(childNode, ssoToken);
                            break;
                        case CSREALM:        // CreateRealm
                            doCreateRealm(childNode, ssoToken);
                            break;
                        case GSREALM:        // GetSubRealmNames
                            doGetSubRealmNames(childNode, ssoToken);
                            break;
                        case GABSREALM:        // GetAssignableServices
                            doGetAssignableServices (childNode, ssoToken);
                            break;
                        case GADSREALM:        // GetAssignedServices
                            doGetAssignedServices (childNode, ssoToken);
                            break;
                        case RASREALM:        // AssignService
                            doAssignService (childNode, ssoToken);
                            break;
                        case RUSREALM:        // UnassignService
                            doUnassignService (childNode, ssoToken);
                            break;
                        case GAREALM:        // GetAttributes
                            doGetAttributes (childNode, ssoToken);
                            break;
                        case GSAREALM:        // GetServiceAttributes
                            doGetServiceAttributes (childNode, ssoToken);
                            break;
                        case RMAREALM:        // RemoveAttribute
                            doRemoveAttribute (childNode, ssoToken);
                            break;
                        case RMSREALM:        // ModifyService
                            doModifyService (childNode, ssoToken);
                            break;
                        case RAAVREALM:        // AddAttributeValues
                            doAddAttrVals (childNode, ssoToken);
                            break;
                        case RRAVREALM:        // RemoveAttributeValues
                            doRmAttrVals (childNode, ssoToken);
                            break;
                        case RSAREALM:        // SetAttributes
                            doSetAttrs (childNode, ssoToken);
                            break;
                        case CPREALM:        // CreatePolicy
                            doCreatePolicyRealm(childNode, ssoToken);
                            break;
                        case DPREALM:        // DeletePolicy
                            doDeletePolicyRealm(childNode, ssoToken);
                            break;
                        case GPREALM:        // RealmGetPolicies
                            doGetPoliciesRealm(childNode, ssoToken);
                            break;
                    }
                } catch (AdminException ae) {
                    if (!continueFlag) {
                        throw ae;
                    }
                }
            }
        }
    }

    private void doDeleteRealm (Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "DeleteRealm"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String strRecursive =
            ((Element)childNode).getAttribute("deleteRecursively");
        boolean recursive = Boolean.valueOf(strRecursive).booleanValue();


        RealmDeleteRealmReq rdr = new RealmDeleteRealmReq(realmPath);
        rdr.setRecursiveDelete(recursive);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(rdr.toString());
        }

        rdr.process (ssoToken);
    }

    private void doCreateRealm(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "CreateRealm"
        //
        //  map of attributevaluepairs will be in the serviceattributes
        //  node.
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        NodeList crChildNodesList = childNode.getChildNodes();
        int crChildNodesListLength = crChildNodesList.getLength();

        //
        //  need to get the ServiceAttributes node to get
        //  the AttributeValuePairs
        //

        RealmCreateRealmReq rcr = new RealmCreateRealmReq(realmPath);

        Map map = null;
        String svcName = null;
        for (int i = 0; i < crChildNodesListLength; i++) {
            Node cNode = crChildNodesList.item(i);
            String nname = cNode.getNodeName();

            //
            //  can have more than one set of ServiceAttributes
            //
            if (nname.equals("ServiceAttributes")) {
                NamedNodeMap nnm = cNode.getAttributes();
                int length = nnm.getLength();
                for (int ii = 0; ii < length; ii++) {
                    Node nd = nnm.item(ii);
                    String gnnm = nd.getNodeName();
                    if (gnnm.equals("serviceName")) {
                        svcName = ((Attr)nd).getValue();
                        break;
                    }
                }
                map = XMLUtils.parseAttributeValuePairTags(cNode);

                rcr.createRealmReq(svcName, map);
            }
        }

        //
        //  Realm is an attribute to CreateRealm
        //
        String strRealm =
            ((Element)childNode).getAttribute("realm");
        
        //
        //  if ServiceName and AttributeValuePairs specified,
        //  then make a Map of them to pass.  otherwise, null.
        //
        if ((map != null ) && !map.isEmpty()) {
            Set set = map.entrySet();
            for (Iterator it=set.iterator(); it.hasNext(); ) {
                Map.Entry me = (Map.Entry)it.next();
            }
        }

        String parentRealm = getParentRealm(strRealm);
        String childRealm = getChildRealm (strRealm);
        rcr.setParentRealm(parentRealm);
        rcr.setSubRealm(childRealm);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(rcr.toString());
        }

        rcr.process(ssoToken);
    }


    private void doGetSubRealmNames (Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "GetSubRealmNames"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String pattern = ((Element)childNode).getAttribute("pattern");
        String strRecursive =
            ((Element)childNode).getAttribute("recursive");
        boolean recursive = Boolean.valueOf(strRecursive).booleanValue();


        GetSubRealmNamesReq gsr = new GetSubRealmNamesReq(realmPath);
        gsr.setRecursiveSearch(recursive);
        if ((pattern != null) && (pattern.length() > 0)) {
            gsr.setPattern(pattern);
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(gsr.toString());
        }

        gsr.process (ssoToken);
    }


    private void doGetAssignableServices (Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "GetAssignableServices"
        //
        String realmPath =((Element)childNode).getAttribute("realm");

        GetAssignableServicesReq gar = new GetAssignableServicesReq(realmPath);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(gar.toString());
        }

        gar.process (ssoToken);
    }


    private void doGetAssignedServices (Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "GetAssignedServices"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String strIncMandatory =
            ((Element)childNode).getAttribute("includeMandatory");
        boolean incMandatory = Boolean.valueOf(strIncMandatory).booleanValue();


        GetAssignedServicesReq gas = new GetAssignedServicesReq(realmPath);
        gas.setIncMandatory(incMandatory);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(gas.toString());
        }

        gas.process (ssoToken);
    }


    private void doAssignService (Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "AssignService"
        //
        //  AssignService is much like CreateRealm, with the
        //  ServiceAttributes.
        //
        //  map of attributevaluepairs will be in the serviceattributes
        //  node.
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        NodeList crChildNodesList = childNode.getChildNodes();
        int crChildNodesListLength = crChildNodesList.getLength();

        //
        //  need to get the ServiceAttributes node to get
        //  the AttributeValuePairs
        //

        RealmAssignServiceReq rasr = new RealmAssignServiceReq(realmPath);
        Map map = null;
        String svcName = null;
        for (int i = 0; i < crChildNodesListLength; i++) {
            Node cNode = crChildNodesList.item(i);
            String nname = cNode.getNodeName();

            //
            //  can have more than one set of ServiceAttributes
            //
            if (nname.equals("ServiceAttributes")) {
                NamedNodeMap nnm = cNode.getAttributes();
                int length = nnm.getLength();
                for (int ii = 0; ii < length; ii++) {
                    Node nd = nnm.item(ii);
                    String gnnm = nd.getNodeName();
                    if (gnnm.equals("serviceName")) {
                        svcName = ((Attr)nd).getValue();
                        break;
                    }
                }
                map = XMLUtils.parseAttributeValuePairTags(cNode);

                rasr.createAssignSvcReq(svcName, map);

            }
        }

        //
        //  if ServiceName and AttributeValuePairs specified,
        //  then make a Map of them to pass.  otherwise, null.
        //

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(rasr.toString());
        }

        rasr.process(ssoToken);
    }


    private void doUnassignService (Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "UnassignService"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String serviceName = ((Element)childNode).getAttribute("serviceName");

        RealmUnassignServiceReq rus = new RealmUnassignServiceReq(realmPath);
        rus.setServiceName(serviceName);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(rus.toString());
        }

        rus.process (ssoToken);
    }


    private void doGetAttributes (Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "GetAttributes"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String serviceName = ((Element)childNode).getAttribute("serviceName");

        RealmGetAttributesReq rgs = new RealmGetAttributesReq(realmPath);
        rgs.setServiceName(serviceName);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(rgs.toString());
        }

        rgs.process (ssoToken);
    }


    private void doGetServiceAttributes (Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "GetServiceAttributes"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String serviceName = ((Element)childNode).getAttribute("serviceName");

        RealmGetSvcAttributesReq rgs = new RealmGetSvcAttributesReq(realmPath);
        rgs.setServiceName(serviceName);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(rgs.toString());
        }

        rgs.process (ssoToken);
    }


    private void doRemoveAttribute (Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "RemoveAttribute"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String serviceName = ((Element)childNode).getAttribute("serviceName");
        String attrName = ((Element)childNode).getAttribute("attrName");

        RealmRemoveAttributeReq rms = new RealmRemoveAttributeReq(realmPath);
        rms.setServiceName(serviceName);
        rms.setAttributeName(attrName);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(rms.toString());
        }

        rms.process (ssoToken);
    }


    private void doModifyService (Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "ModifyService"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String serviceName = ((Element)childNode).getAttribute("serviceName");
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);

        RealmModifyServiceReq rms = new RealmModifyServiceReq(realmPath);
        rms.setServiceName(serviceName);
        rms.setAttrMap(map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(rms.toString());
        }

        rms.process (ssoToken);
    }


    private void doAddAttrVals(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "AddAttributeValues"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String serviceName =((Element)childNode).getAttribute("serviceName");
        String attrName =((Element)childNode).getAttribute("attrName");

        Set set = getSubNodeElementNodeValues(childNode.getChildNodes(),
            "Value");
        
        RealmAddAttrValsReq raavr = new RealmAddAttrValsReq(realmPath);
        raavr.setServiceName(serviceName);
        raavr.setAttrName(attrName);
        raavr.setValueSet(set);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(raavr.toString());
        }

        raavr.process(ssoToken);
    }


    private void doRmAttrVals(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "RemoveAttributeValues"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String serviceName =((Element)childNode).getAttribute("serviceName");
        String attrName =((Element)childNode).getAttribute("attrName");

        Set set = getSubNodeElementNodeValues(childNode.getChildNodes(),
            "Value");
        
        RealmRmAttrValsReq rravr = new RealmRmAttrValsReq(realmPath);
        rravr.setServiceName(serviceName);
        rravr.setAttrName(attrName);
        rravr.setValueSet(set);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(rravr.toString());
        }

        rravr.process(ssoToken);
    }


    private void doSetAttrs (Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "SetAttributes"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String serviceName = ((Element)childNode).getAttribute("serviceName");
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);

        RealmSetAttributesReq rms = new RealmSetAttributesReq(realmPath);
        rms.setServiceName(serviceName);
        rms.setAttrMap(map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(rms.toString());
        }

        rms.process (ssoToken);
    }


    private void doCreatePolicyRealm (Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "RealmCreatePolicy"
        //
        String orgDN = ((Element)childNode).getAttribute("realm");

        PolicyManager pm = getPolicyManager(ssoToken, orgDN,
            "policycreatexception");
        NodeList nodeList = childNode.getChildNodes();
        int len = nodeList.getLength();

        for (int i = 0; i < len; i++) {
            Node policyChildNode = nodeList.item(i);

            if (policyChildNode.getNodeType() == Node.ELEMENT_NODE) {
                addPolicy(orgDN, pm, policyChildNode);
            }
        }

        String policyName = XMLUtils.getNodeAttributeValue(
            XMLUtils.getChildNode(childNode,"Policy"), "name");
        String[] args = {policyName, orgDN};
//        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//            MessageFormat.format(bundle.getString("create-policy"), args));
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, AdminUtils.CREATE_POLICY, args);
    }


    private void doDeletePolicyRealm (Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "RealmDeletePolicy"
        //
        String orgDN = ((Element)childNode).getAttribute("realm");
        doDeletePolicy(childNode, ssoToken);

        String policyName = XMLUtils.getNodeAttributeValue(
            XMLUtils.getChildNode(childNode,"PolicyName"), "name");
        String[] args = {policyName, orgDN};
//        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
//            MessageFormat.format(bundle.getString("delete-policy"), args));
        AdminUtils.logOperation(AdminUtils.LOG_ACCESS,
            Level.INFO, AdminUtils.DELETE_POLICY, args);
    }


    private void doGetPoliciesRealm (Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "RealmGetPolicies"
        //
        //  needed to add something here to get the DN that would normally
        //  be specified in the OrganizationRequest.  RealmRequest doesn't
        //  have any parameters...
        //
        String orgDN =((Element)childNode).getAttribute("realm");
        getPoliciesInOrganization(orgDN, childNode, ssoToken);
    }


    private void doIdentityRequests (Node node, SSOToken ssoToken)
        throws AdminException
    {
        Node realmNode = node;
        NodeList realmChildNodesList = realmNode.getChildNodes();
        int realmChildNodesListLength = realmChildNodesList.getLength();

        for (int i = 0; i < realmChildNodesListLength; i++) {
            Node childNode = realmChildNodesList.item(i);

            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                String strChildNodeName = childNode.getNodeName();

                int opt = getToken(strChildNodeName);

                try {
                    switch(opt){
                        //
                        //  AMIdentityRepository requests
                        //
                        case IDCRID:
                            createIdentity(childNode, ssoToken);
                            break;
                        case IDCRIDS:
                            createIdentities(childNode, ssoToken);
                            break;
                        case IDDELIDS:
                            deleteIdentities(childNode, ssoToken);
                            break;
                        case IDSRCHIDS:
                            searchIdentities(childNode, ssoToken);
                            break;
                        case IDGAIDOPS:
                            getAllowedIdOperations(childNode, ssoToken);
                            break;
                        case IDGSIDTYPS:
                            getSupportedIdTypes(childNode, ssoToken);
                            break;
                        //
                        //  AMIdentity requests
                        //
                        case IDGABSVCS:
                            idGetAssignableServices(childNode, ssoToken);
                            break;
                        case IDGADSVCS:
                            idGetAssignedServices(childNode, ssoToken);
                            break;
                        case IDGSVCATTR:
                            idGetServiceAttrs(childNode, ssoToken);
                            break;
                        case IDGETATTRS:
                            idGetAttributes(childNode, ssoToken);
                            break;
                        case IDGETMBRSHPS:
                            idGetMemberships(childNode, ssoToken);
                            break;
                        case IDISMEMBER:
                            idIsMember(childNode, ssoToken);
                            break;
                        case IDISACTIVE:
                            idIsActive(childNode, ssoToken);
                            break;
                        case IDGETMEMBERS:
                            idGetMembers(childNode, ssoToken);
                            break;
                        case IDADDMEMBER:
                            idAddMember(childNode, ssoToken);
                            break;
                        case IDRMMEMBER:
                            idRmMember(childNode, ssoToken);
                            break;
                        case IDASGNSVC:
                            idAssignSvc(childNode, ssoToken);
                            break;
                        case IDUASGNSVC:
                            idUnassignSvc(childNode, ssoToken);
                            break;
                        case IDMODIFYSVC:
                            idModifySvc(childNode, ssoToken);
                            break;
                        case IDSETATTRS:
                            idSetAttributes(childNode, ssoToken);
                            break;
                    }
                } catch (AdminException ae) {
                    if (!continueFlag) {
                        throw ae;
                    }
                }
            }
        }
    }


    private void createIdentity (Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "CreateIdentity"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idName = ((Element)childNode).getAttribute("idName");
        String idType = ((Element)childNode).getAttribute("idType");
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);

        IdType idt = convert2IdType(idType);

        IdCreateIdentityReq icir = new IdCreateIdentityReq(realmPath);
        icir.setIdName(idName);
        icir.setIdType(idt);
        icir.setAttrMap(map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(icir.toString());
        }

        icir.process (ssoToken);
    }


    private void createIdentities(Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "CreateIdentities"
        //
        //  map of AttributeValuePairs will be in the IdentityAttributes
        //  node.
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idType =((Element)childNode).getAttribute("idType");
        NodeList crChildNodesList = childNode.getChildNodes();
        int crChildNodesListLength = crChildNodesList.getLength();

        //
        //  need to get the IdentityAttributes node to get
        //  the AttributeValuePairs
        //

        IdCreateIdentitiesReq icis = new IdCreateIdentitiesReq(realmPath);
        IdType idt = convert2IdType(idType);
        icis.setIdType(idt);

        Map map = null;
        String idName = null;
        for (int i = 0; i < crChildNodesListLength; i++) {
            Node cNode = crChildNodesList.item(i);
            String nname = cNode.getNodeName();
            //
            //  can have more than one set of IdentityAttributes
            //
            if (nname.equals("IdentityAttributes")) {
                NamedNodeMap nnm = cNode.getAttributes();
                int length = nnm.getLength();
                for (int ii = 0; ii < length; ii++) {
                    Node nd = nnm.item(ii);
                    String gnnm = nd.getNodeName();
                    if (gnnm.equals("idName")) {
                        idName = ((Attr)nd).getValue();
                        break;
                    }
                }
                map = XMLUtils.parseAttributeValuePairTags(cNode);

                //
                //  got one set of idName + AVPairs
                //
                icis.createIdCreateReq(idName, map);
            }
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(icis.toString());
        }

        icis.process(ssoToken);
    }


    private void deleteIdentities(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "DeleteIdentities"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idType =((Element)childNode).getAttribute("idType");
        Set set = getSubNodeElementNodeValues(childNode.getChildNodes(),
            "IdName");
        
        IdType idt = convert2IdType(idType);

        IdDeleteIdentitiesReq idir = new IdDeleteIdentitiesReq(realmPath);
        idir.setIdType(idt);
        idir.setIdNameSet(set);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(idir.toString());
        }

        idir.process(ssoToken);
    }


    private void searchIdentities(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "SearchIdentities"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idType =((Element)childNode).getAttribute("idType");
        IdType idt = convert2IdType(idType);
        String pattern =((Element)childNode).getAttribute("pattern");
        String strRecursive =
            ((Element)childNode).getAttribute("recursive");
        boolean recursive = Boolean.valueOf(strRecursive).booleanValue();

        IdSearchIdentitiesReq idsr = new IdSearchIdentitiesReq(realmPath);
        idsr.setIdType(idt);
        idsr.setIdPattern(pattern);
        idsr.setRecursive(recursive);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(idsr.toString());
        }

        idsr.process(ssoToken);
    }


    private void getAllowedIdOperations(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "GetAllowedIdOperations"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idType =((Element)childNode).getAttribute("idType");
        IdType idt = convert2IdType(idType);

        IdGetAllowedIdOpsReq idgs = new IdGetAllowedIdOpsReq(realmPath);
        idgs.setIdType(idt);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(idgs.toString());
        }

        idgs.process(ssoToken);
    }


    private void getSupportedIdTypes(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "GetSupportedIdTypes"
        //
        String realmPath =((Element)childNode).getAttribute("realm");

        IdGetSupportedIdTypesReq igsi = new IdGetSupportedIdTypesReq(realmPath);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(igsi.toString());
        }

        igsi.process(ssoToken);
    }


    private void idGetAssignableServices(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "IdGetAssignableServices"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idType =((Element)childNode).getAttribute("idType");
        IdType idt = convert2IdType(idType);
        String idName =((Element)childNode).getAttribute("idName");

        IdGetAssignableServicesReq igas =
            new IdGetAssignableServicesReq(realmPath);
        igas.setIdType(idt);
        igas.setIdName(idName);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(igas.toString());
        }

        igas.process(ssoToken);
    }


    private void idGetAssignedServices(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "IdGetAssignedServices"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idType =((Element)childNode).getAttribute("idType");
        IdType idt = convert2IdType(idType);
        String idName =((Element)childNode).getAttribute("idName");

        IdGetAssignedServicesReq igas =
            new IdGetAssignedServicesReq(realmPath);
        igas.setIdType(idt);
        igas.setIdName(idName);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(igas.toString());
        }

        igas.process(ssoToken);
    }


    private void idGetServiceAttrs(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "IdGetServiceAttrs"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idType =((Element)childNode).getAttribute("idType");
        IdType idt = convert2IdType(idType);
        String idName =((Element)childNode).getAttribute("idName");
        String svcName =((Element)childNode).getAttribute("serviceName");

        IdGetServiceAttrsReq igsa = new IdGetServiceAttrsReq(realmPath);
        igsa.setIdType(idt);
        igsa.setIdName(idName);
        igsa.setSvcName(svcName);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(igsa.toString());
        }

        igsa.process(ssoToken);
    }


    private void idGetAttributes(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "IdGetAttributes"
        //
        //  if no Attribute childnodes, then get all attrs
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idType =((Element)childNode).getAttribute("idType");
        IdType idt = convert2IdType(idType);
        String idName =((Element)childNode).getAttribute("idName");
        NodeList attChildNodesList = childNode.getChildNodes();
        int attChildNodesListLength = attChildNodesList.getLength();

        IdGetAttributesReq igsa = new IdGetAttributesReq(realmPath);
        igsa.setIdType(idt);
        igsa.setIdName(idName);

        Map map = null;
        for (int i = 0; i < attChildNodesListLength; i++) {
            Node cNode = attChildNodesList.item(i);
            String nname = cNode.getNodeName();

            //
            //  can have more than one Attribute name
            //
            if (nname.equals("Attribute")) {
                String attName = ((Element)cNode).getAttribute("name");
                igsa.addAttrName(attName);        // add to Set to get
            }
        }

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(igsa.toString());
        }

        igsa.process(ssoToken);
    }


    private void idGetMemberships(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "IdGetMemberships"
        //
        //  if no Attribute childnodes, then get all attrs
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idType =((Element)childNode).getAttribute("idType");
        IdType idt = convert2IdType(idType);
        String idName =((Element)childNode).getAttribute("idName");
        String targIdType =((Element)childNode).getAttribute("targetIdType");
        IdType tidt = convert2IdType(targIdType);

        IdGetMembershipsReq igsm = new IdGetMembershipsReq(realmPath);
        igsm.setIdType(idt);
        igsm.setIdName(idName);
        igsm.setTargetIdType(tidt);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(igsm.toString());
        }

        igsm.process(ssoToken);
    }


    private void idIsMember(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "IdIsMember"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idType =((Element)childNode).getAttribute("idType");
        IdType idt = convert2IdType(idType);
        String idName =((Element)childNode).getAttribute("idName");
        String targIdType =((Element)childNode).getAttribute("targetIdType");
        IdType tidt = convert2IdType(targIdType);
        String targIdName =((Element)childNode).getAttribute("targetIdName");

        IdIsMemberReq igsm = new IdIsMemberReq(realmPath);
        igsm.setIdType(idt);
        igsm.setIdName(idName);
        igsm.setTargetIdType(tidt);
        igsm.setTargetIdName(targIdName);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(igsm.toString());
        }

        igsm.process(ssoToken);
    }


    private void idIsActive(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "IdIsActive"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idType =((Element)childNode).getAttribute("idType");
        IdType idt = convert2IdType(idType);
        String idName =((Element)childNode).getAttribute("idName");

        IdIsActiveReq igas = new IdIsActiveReq(realmPath);
        igas.setIdType(idt);
        igas.setIdName(idName);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(igas.toString());
        }

        igas.process(ssoToken);
    }


    private void idGetMembers(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "IdGetMembers"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idType =((Element)childNode).getAttribute("idType");
        IdType idt = convert2IdType(idType);
        String idName =((Element)childNode).getAttribute("idName");
        String targIdType =((Element)childNode).getAttribute("targetIdType");
        IdType tidt = convert2IdType(targIdType);

        IdGetMembersReq igsm = new IdGetMembersReq(realmPath);
        igsm.setIdType(idt);
        igsm.setIdName(idName);
        igsm.setTargetIdType(tidt);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(igsm.toString());
        }

        igsm.process(ssoToken);
    }


    private void idAddMember(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "IdAddMember"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idType =((Element)childNode).getAttribute("idType");
        IdType idt = convert2IdType(idType);
        String idName =((Element)childNode).getAttribute("idName");
        String subjIdType =((Element)childNode).getAttribute("subjectIdType");
        IdType sidt = convert2IdType(subjIdType);
        String subjIdName =((Element)childNode).getAttribute("subjectIdName");

        IdAddMemberReq igsm = new IdAddMemberReq(realmPath);
        igsm.setIdType(idt);
        igsm.setIdName(idName);
        igsm.setSubjectIdType(sidt);
        igsm.setSubjectIdName(subjIdName);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(igsm.toString());
        }

        igsm.process(ssoToken);
    }


    private void idRmMember(Node childNode, SSOToken ssoToken)
        throws AdminException
    {

        //
        //  childNode is pointing to "IdRemoveMember"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idType =((Element)childNode).getAttribute("idType");
        IdType idt = convert2IdType(idType);
        String idName =((Element)childNode).getAttribute("idName");
        String subjIdType =((Element)childNode).getAttribute("subjectIdType");
        IdType sidt = convert2IdType(subjIdType);
        String subjIdName =((Element)childNode).getAttribute("subjectIdName");


        IdRemoveMemberReq igsm = new IdRemoveMemberReq(realmPath);
        igsm.setIdType(idt);
        igsm.setIdName(idName);
        igsm.setSubjectIdType(sidt);
        igsm.setSubjectIdName(subjIdName);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(igsm.toString());
        }

        igsm.process(ssoToken);
    }


    private void idAssignSvc(Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "IdAssignService"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idName =((Element)childNode).getAttribute("idName");
        String idType =((Element)childNode).getAttribute("idType");
        IdType idt = convert2IdType(idType);
        String serviceName = ((Element)childNode).getAttribute("serviceName");
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);


        IdAssignServiceReq rms = new IdAssignServiceReq(realmPath);
        rms.setServiceName(serviceName);
        rms.setIdType(idt);
        rms.setIdName(idName);
        rms.setAttrMap(map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(rms.toString());
        }

        rms.process (ssoToken);
    }


    private void idUnassignSvc(Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "IdUnassignService"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idName =((Element)childNode).getAttribute("idName");
        String idType =((Element)childNode).getAttribute("idType");
        IdType idt = convert2IdType(idType);
        String serviceName = ((Element)childNode).getAttribute("serviceName");

        IdUnassignServiceReq rms = new IdUnassignServiceReq(realmPath);
        rms.setServiceName(serviceName);
        rms.setIdType(idt);
        rms.setIdName(idName);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(rms.toString());
        }

        rms.process (ssoToken);
    }


    private void idModifySvc(Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "IdModifyService"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idName =((Element)childNode).getAttribute("idName");
        String idType =((Element)childNode).getAttribute("idType");
        IdType idt = convert2IdType(idType);
        String serviceName = ((Element)childNode).getAttribute("serviceName");
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);


        IdModifyServiceReq rms = new IdModifyServiceReq(realmPath);
        rms.setServiceName(serviceName);
        rms.setIdType(idt);
        rms.setIdName(idName);
        rms.setAttrMap(map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(rms.toString());
        }

        rms.process (ssoToken);
    }


    private void idSetAttributes(Node childNode, SSOToken ssoToken)
        throws AdminException
    {
        //
        //  childNode is pointing to "IdSetAttributes"
        //
        String realmPath =((Element)childNode).getAttribute("realm");
        String idName =((Element)childNode).getAttribute("idName");
        String idType =((Element)childNode).getAttribute("idType");
        IdType idt = convert2IdType(idType);
        Map map = XMLUtils.parseAttributeValuePairTags(childNode);

        IdSetAttributesReq rms = new IdSetAttributesReq(realmPath);
        rms.setIdType(idt);
        rms.setIdName(idName);
        rms.setAttrMap(map);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(rms.toString());
        }

        rms.process (ssoToken);
    }

    private void setPropertiesViewBeanURLForPS(String serviceName, 
            Node childNode, SSOToken ssoToken) throws AdminException
    {
        String pluginSchemaName 
                = ((Element)childNode).getAttribute("pluginSchemaName");
        String pluginInterfaceName 
                = ((Element)childNode).getAttribute("pluginInterfaceName");
        String propertiesViewBeanURL 
                = ((Element)childNode).getAttribute("propertiesViewBeanURL");
        if (debug.messageEnabled()) {
            debug.message("AdminXMLParser.setPropertiesViewBeanURLForPS():"
                    + "entering with "
                    + "serviceName=" + serviceName 
                    + "pluginSchemaName=" + pluginSchemaName 
                    + "pluginInterfaceName=" + pluginInterfaceName 
                    + "propertiesViewBeanURL=" + propertiesViewBeanURL); 
        }
        try {
            ServiceSchemaManager ssm 
                    = new ServiceSchemaManager(serviceName, ssoToken);
            PluginSchema ps = ssm.getPluginSchema(pluginSchemaName, 
                    pluginInterfaceName, "/"); //for root realm/global
            ps.setPropertiesViewBeanURL(propertiesViewBeanURL);
        } catch (Exception e) {
            debug.error("AdminXMLParser.setPropertiesViewBeanURLForPS():"
                    + "got exception:", e);
            System.err.println("AdminXMLParser.setPropertiesViewBeanURLForPS():"
                    + "got exception:" + e.getMessage());
            throw new AdminException(e);
        }
        if (debug.messageEnabled()) {
            debug.message("AdminXMLParser.setPropertiesViewBeanURLForPS():"
                    + "returning");
        }
    }


    private Set getSubNodeElementNodeValues(NodeList list, String nodeName) {
        int len = list.getLength();
        Set set = new HashSet(len);

        for (int i = 0; i < len; i++) {
            Node subchildnode = list.item(i);

            if ((subchildnode.getNodeType() == Node.ELEMENT_NODE) &&
                subchildnode.getNodeName().equals(nodeName)
            ) {
                Text firstChild = (Text)subchildnode.getFirstChild();
                set.add(firstChild.getNodeValue());
            }
        }

        return set;
    }


    private String getParentRealm (String realm) {
        //
        //  if no "/", then return "/"
        //  if just "/", return "/"
        //  if "/<org>" or "/<org>/", return "/"
        //  else do as expected, i.e., return everything
        //  except the last "/" and the string following it.
        //

        String tmpName = realm;
        if (realm.startsWith("/")) {
            tmpName = realm.substring(1);
        }
        if (tmpName.endsWith("/")) {
            tmpName = tmpName.substring(0,tmpName.length());
        }

        //
        // what's left has no leading or ending "/"
        //
        //  if nothing left, or no "/", return "/"
        //

        if ((tmpName.length() == 0) || (tmpName.indexOf("/") == -1)){
            return ("/");
        } else {
            //
            //  should have at least one "/"
            //
            StringTokenizer st = new StringTokenizer(tmpName, "/");
            if (st.countTokens() <= 1) {
                //
                //  shouldn't be, but...
                //
                return ("/");
            }
            int count = st.countTokens();
            StringBuffer sb = new StringBuffer();
            for (int j = 1; j < count; j++) {
                sb.append("/").append(st.nextToken());
            }
        return (sb.toString());
        }
    }


    private String getChildRealm (String realm) {
        //
        //  if no "/", then return null
        //  if just "/", return null
        //  if "/<org>" or "/<org>/", return <org>
        //  else do as expected, i.e., return the
        //  string after the last "/".
        //

        String tmpName = realm;
        if (!realm.startsWith("/")) {
            return null;
        }

        //
        //  delete a trailing "/", if it's not the only one
        //

        if ((tmpName.length() > 1) && tmpName.endsWith("/")) {
            tmpName = tmpName.substring(0,tmpName.length());
        }

        //
        // what's left has a leading, but no ending "/"
        //
        //  if nothing left, or no "/", return "/"
        //

        if (tmpName.length() == 1){
            return ("/");
        } else {
            //
            //  should have at least one "/"
            //
            StringTokenizer st = new StringTokenizer(tmpName, "/");
            if (st.countTokens() <= 0) {
                //
                //  shouldn't be, but...
                //
                return (null);
            }
            String childRealm = null;
            while (st.hasMoreElements()) {
                childRealm = (String)st.nextToken();
            }
        return (childRealm);
        }
    }

    static IdType convert2IdType(String identType)
        throws AdminException
    {
        if (identType.equalsIgnoreCase("AGENT")) {
            return(IdType.AGENT);
        } else if (identType.equalsIgnoreCase("FILTEREDROLE")) {
            return(IdType.FILTEREDROLE);
        } else if (identType.equalsIgnoreCase("GROUP")) {
            return(IdType.GROUP);
        } else if (identType.equalsIgnoreCase("ROLE")) {
            return(IdType.ROLE);
        } else if (identType.equalsIgnoreCase("USER")) {
            return(IdType.USER);
        } else {
            throw new AdminException(bundle.getString("invalidIdType") +
                " " + identType);
        }
    }

    private String validateLevel(Node childNode) {
        String level = ((Element)childNode).getAttribute("level");
        if(!(level.equals("SCOPE_SUB") || level.equals("SCOPE_ONE"))) {
            System.err.println("\n"+bundle.getString("parseerr") + fileName
                               +"\n"+bundle.getString("levelerr"));
            System.exit(1);
            }
        return level;
    }
    
    private int validateObjectType(AMStoreConnection amsc, String dn,
        int objectType)
        throws AdminException
    {
        try {
            int realType = amsc.getAMObjectType(dn);

            if (!equalsObjectType(realType, objectType)) {
                String key = "";

                switch (objectType) {
                case AMObject.ROLE:
                    key = "invalidRoleDN";
                    break;
                case AMObject.GROUP:
                    key = "invalidStaticGroupDN";
                    break;
                case AMObject.ORGANIZATION:
                    key = "invalidOrgDN";
                    break;
                case AMObject.USER:
                    key = "invalidUserDN";
                    break;
                case AMObject.PEOPLE_CONTAINER:
                    key = "invalidPeopleContainerDN";
                    break;
                case AMObject.ORGANIZATIONAL_UNIT:
                    key = "invalidOrgUnitDN";
                    break;
                }

                String errMsg = bundle.getString(key) + " " + dn;

                if (AdminUtils.logEnabled()) {
                    AdminUtils.log(errMsg);
                }

                throw new AdminException(errMsg);
            }

            return realType;
        } catch (Exception ex) {
            String errMsg = bundle.getString("failToGetObjType") + " " + dn;

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(errMsg, ex);
            }

            throw new AdminException(errMsg);
        }
    }

    private boolean equalsObjectType(int actualType, int typeToMatch) {
        boolean matched = false;

        switch (typeToMatch) {
        case AMObject.ROLE:
            matched = (actualType == AMObject.ROLE) ||
                (actualType == AMObject.FILTERED_ROLE);
            break;
        case AMObject.GROUP:
            matched = (actualType == AMObject.GROUP) ||
                (actualType == AMObject.DYNAMIC_GROUP) ||
                (actualType == AMObject.ASSIGNABLE_DYNAMIC_GROUP);
            break;
        default:
            matched = (actualType == typeToMatch);
        }

        return matched;
    }

    private Map parseChoiceValueTags(Node parentNode) {
        NodeList childNodes = parentNode.getChildNodes();
        int numAVPairs = childNodes.getLength();
        Map map = new HashMap(numAVPairs *2);

        for (int i = 0; i < numAVPairs; i++) {
            Node node = childNodes.item(i);

            if ((node.getNodeType() == Node.ELEMENT_NODE) &&
                node.getNodeName().equals("ChoiceValue")
            ) {
                String attrName = ((Element)node).getAttribute("AttributeName");
                String i18nKey = ((Element)node).getAttribute("I18NKey");
                String value = ((Element)node).getAttribute("value");
                String[] i18nKeyValue = new String[2];
                i18nKeyValue[0] = i18nKey;
                i18nKeyValue[1] = value;

                Set set = (Set)map.get(attrName);
                if (set == null) {
                    set = new HashSet();
                    map.put(attrName, set);
                }

                set.add(i18nKeyValue);
            }
        }

        return map;
    }

    private void doDelegationRequests(Node node, SSOToken ssoToken)
        throws AdminException {
        NodeList childNodesList = node.getChildNodes();
        int len = childNodesList.getLength();

        for (int i = 0; i < len; i++) {
            Node childNode = childNodesList.item(i);

            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = childNode.getNodeName();
                int opt = getToken(nodeName);

                try {
                    switch (opt) {
                    case DELEGATE_GET_PRIVILEGES:
                        getPrivileges(childNode, ssoToken);
                        break;
                    case DELEGATE_ADD_PRIVILEGES:
                        addPrivileges(childNode, ssoToken);
                        break;
                    case DELEGATE_REMOVE_PRIVILEGES:
                        removePrivileges(childNode, ssoToken);
                        break;
                    }
                } catch (AdminException ae) {
                    if (!continueFlag) {
                        throw ae;
                    }
                }
            }
        }
    }

    private void getPrivileges(Node childNode, SSOToken ssoToken)
        throws AdminException {
        String realm =((Element)childNode).getAttribute("realm");
        String idName =((Element)childNode).getAttribute("idName");
        String idType =((Element)childNode).getAttribute("idType");
        GetPrivilegesReq req = new GetPrivilegesReq(realm, idName, idType);
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(req.toString());
        }
        req.process(ssoToken);
    }

    private void addPrivileges(Node childNode, SSOToken ssoToken)
        throws AdminException {
        String realm =((Element)childNode).getAttribute("realm");
        String idName =((Element)childNode).getAttribute("idName");
        String idType =((Element)childNode).getAttribute("idType");
        Set privileges = getSubNodeElementNodeValues(
            childNode.getChildNodes(), "name");
        AddPrivilegesReq req = new AddPrivilegesReq(realm, idName, idType,
            privileges);
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(req.toString());
        }
        req.process(ssoToken);
    }

    private void removePrivileges(Node childNode, SSOToken ssoToken)
        throws AdminException {
        String realm =((Element)childNode).getAttribute("realm");
        String idName =((Element)childNode).getAttribute("idName");
        String idType =((Element)childNode).getAttribute("idType");
        Set privileges = getSubNodeElementNodeValues(
            childNode.getChildNodes(), "name");
        RemovePrivilegesReq req = new RemovePrivilegesReq(
            realm, idName, idType, privileges);
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(req.toString());
        }
        req.process(ssoToken);
    }

    /**
     *Inner class to take care of XML validation errors
     */
    class ValidationErrorHandler implements ErrorHandler {
        
        // ignore fatal errors(an exception is guaranteed)
        public void fatalError(SAXParseException spe)
            throws SAXParseException
        {
            System.err.println(bundle.getString("fatalvaliderr") + fileName +
                "\n" + spe.getMessage() +
                "\nLine Number in XML file : " + spe.getLineNumber() +
                "\nColumn Number in XML file : " + spe.getColumnNumber());
            printValidationMessages(spe,
                "Fatal, XML Validation Error - XML file is not a valid file." +
                fileName);
        }
        
        //treat validation errors also as fatal error
        public void error(SAXParseException spe)
            throws SAXParseException
        {
            System.err.println(bundle.getString("nonfatalvaliderr") + fileName +
                "\n" + spe.getMessage() +
                "\nLine Number in XML file : " + spe.getLineNumber() +
                "\nColumn Number in XML file : " + spe.getColumnNumber());
            printValidationMessages(spe,
                "Non-Fatal, XML Validation Error - " +
                "XML file is not a valid file." + fileName);
            throw spe;
        }
        
        // dump warnings too
        public void warning(SAXParseException err)
            throws SAXParseException
        {
            System.err.println(bundle.getString("validwarn") + fileName + "\n" +
                err.getMessage() +
                "\nLine Number in XML file : " + err.getLineNumber() +
                "\nColumn Number in XML file : " + err.getColumnNumber());
            printValidationMessages(err, "XML file Validation Warnings." +
                fileName);
        }
        
        void printValidationMessages(SAXParseException spe, String str) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log("Error is : " + str, spe);
                AdminUtils.log("Detailed Message is : " + spe.getMessage(),
                    null);
                AdminUtils.log("Line Number : " + spe.getLineNumber(), null);
                AdminUtils.log("Column Number : " + spe.getColumnNumber(),
                    null);
            }
        }
    }
}
