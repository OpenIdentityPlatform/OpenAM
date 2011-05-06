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
 * $Id: IDPPConstants.java,v 1.2 2008/06/25 05:47:15 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.idpp.common;

public class IDPPConstants {

    public static final String XMLNS_IDPP = "urn:liberty:id-sis-pp:2003-08";
    public static final String IDPP_JAXB_PKG = 
                  "com.sun.identity.liberty.ws.idpp.jaxb";
    public static final String IDPP_PLUGIN_JAXB_PKG =
                  "com.sun.identity.liberty.ws.idpp.plugin.jaxb";
    public static final String XMLSIG_JAXB_PKG =
                  "com.sun.identity.liberty.ws.common.jaxb.xmlsig";
    public static final String IDPP_SERVICE = 
                  "sunIdentityServerLibertyPPService";
    public static final String NAME_SCHEME_MIDDLE =
                  "urn:liberty:id-sis-pp:nameScheme:firstmiddlelast";
    public static final String CONTAINER = "container";
    public static final String EXTENSION = "extension";
    public static final String PP_EXT_XML_NS = 
                  "http://www.sun.com/identity/liberty/pp";
    public static final String XML_NS = "xmlns:";
    public static final String PP_EXTENSION_ELEMENT = "PPISExtension";

    public static final String CONSENT = "Consent";
    //will need to change to the right values when the interaction supports
    public static final String CONSENT_DENY = "no";
    public static final String CONSENT_ALLOW = "yes";
    public static final String VALUE = "Value";
    public static final String QUESTION = "Question";
    public static final String PP_INTERACTION_PROP_FILE = 
                               "amLibertyPPInteraction";

    public static final String INTERACT_FOR_CONSENT = "interactForConsent";
    public static final String INTERACT_FOR_VALUE = "interactForValue";
    public static final String AUTHZ_ALLOW = "allow";
    public static final String AUTHZ_DENY = "deny";
    public static final String INTERACTION_TITLE = "interactionTitle";
    public static final String COMMON_QUERY_CONSENT_QUESTION = 
                               "commonQueryConsentQuestion";
    public static final String COMMON_MODIFY_CONSENT_QUESTION = 
                               "commonModifyConsentQuestion";

     // IDPP element constants;
    public static final String IDPP_ELEMENT = "PP";
    public static final int IDPP_ELEMENT_INT = 1;
    public static final String INFORMAL_NAME_ELEMENT = "InformalName";
    public static final int INFORMAL_NAME_ELEMENT_INT = 2;
    public static final String LINFORMAL_NAME_ELEMENT = "LInformalName";
    public static final int LINFORMAL_NAME_ELEMENT_INT = 3;
    public static final String COMMON_NAME_ELEMENT = "CommonName";
    public static final int COMMON_NAME_ELEMENT_INT = 4;
    public static final String LEGAL_IDENTITY_ELEMENT = "LegalIdentity";
    public static final int LEGAL_IDENTITY_ELEMENT_INT = 5;
    public static final String EMPLOYMENT_IDENTITY_ELEMENT = 
                        "EmploymentIdentity";
    public static final int EMPLOYMENT_IDENTITY_ELEMENT_INT = 6;
    public static final String ADDRESS_CARD_ELEMENT = "AddressCard";
    public static final int ADDRESS_CARD_ELEMENT_INT = 7;
    public static final String MSG_CONTACT_ELEMENT = "MsgContact";
    public static final int MSG_CONTACT_ELEMENT_INT = 8;
    public static final String FACADE_ELEMENT = "Facade";
    public static final int FACADE_ELEMENT_INT = 9;
    public static final String DEMOGRAPHICS_ELEMENT = "Demographics";
    public static final int DEMOGRAPHICS_ELEMENT_INT = 10;
    public static final String SIGN_KEY_ELEMENT = "SignKey";
    public static final int SIGN_KEY_ELEMENT_INT = 11;
    public static final String ENCRYPT_KEY_ELEMENT = "EncryptKey";
    public static final int ENCRYPT_KEY_ELEMENT_INT = 12;
    public static final String EMERGENCY_CONTACT_ELEMENT = "EmergencyContact";
    public static final int EMERGENCY_CONTACT_ELEMENT_INT = 13;
    public static final String LEMERGENCY_CONTACT_ELEMENT =
                        "LEmergencyContact" ;
    public static final int LEMERGENCY_CONTACT_ELEMENT_INT = 14;
    public static final String FN_ELEMENT = "FN";
    public static final int FN_ELEMENT_INT = 15;
    public static final String MN_ELEMENT = "MN";
    public static final int MN_ELEMENT_INT = 16;
    public static final String CN_ELEMENT = "CN";
    public static final int CN_ELEMENT_INT = 17;
    public static final String PT_ELEMENT = "PersonalTitle";
    public static final int PT_ELEMENT_INT = 18;
    public static final String SN_ELEMENT = "SN";
    public static final int SN_ELEMENT_INT = 19;
    public static final String ANALYZED_NAME_ELEMENT = "AnalyzedName";
    public static final int ANALYZED_NAME_ELEMENT_INT = 20;
    public static final String ALT_CN_ELEMENT = "AltCN";
    public static final int ALT_CN_ELEMENT_INT = 21;
    public static final String LEGAL_NAME_ELEMENT = "LegalName";
    public static final int LEGAL_NAME_ELEMENT_INT = 22;
    public static final String DOB_ELEMENT = "DOB";
    public static final int DOB_ELEMENT_INT = 23;
    public static final String GENDER_ELEMENT = "Gender";
    public static final int GENDER_ELEMENT_INT = 24;
    public static final String MARITAL_STATUS_ELEMENT = "MaritalStatus";
    public static final int MARITAL_STATUS_ELEMENT_INT = 25;
    public static final String ALT_ID_ELEMENT = "AltID";
    public static final int ALT_ID_ELEMENT_INT = 26;
    public static final String ID_TYPE_ELEMENT = "IDType";
    public static final int ID_TYPE_ELEMENT_INT = 27;
    public static final String ID_VALUE_ELEMENT = "IDValue";
    public static final int ID_VALUE_ELEMENT_INT = 28;
    public static final String VAT_ELEMENT = "VAT";
    public static final int VAT_ELEMENT_INT = 29;
    public static final String ALT_ID_TYPE_ELEMENT = "AltIDType";
    public static final String ALT_ID_VALUE_ELEMENT = "AltIDValue";
    public static final String JOB_TITLE_ELEMENT = "JobTitle";
    public static final int JOB_TITLE_ELEMENT_INT = 30;
    public static final String O_ELEMENT = "O";
    public static final int O_ELEMENT_INT = 31;
    public static final String ALT_O_ELEMENT = "AltO";
    public static final int ALT_O_ELEMENT_INT = 32;
    public static final String EXTENSION_ELEMENT = "Extension";
    public static final int EXTENSION_ELEMENT_INT = 33;
    public static final String MUGSHOT_ELEMENT = "MugShot";
    public static final String WEBSITE_ELEMENT = "WebSite";
    public static final String NAME_PRONOUNCED_ELEMENT = "NamePronounced";
    public static final String GREET_SOUND_ELEMENT = "GreetSound";
    public static final String GREET_ME_SOUND_ELEMENT = "GreetMeSound";
    public static final String DEMO_GRAPHICS_DISPLAY_LANG_ELEMENT = 
           "DisplayLanguage";
    public static final String DEMO_GRAPHICS_LANGUAGE_ELEMENT = "Language";
    public static final String DEMO_GRAPHICS_BIRTH_DAY_ELEMENT = "Birthday";
    public static final String DEMO_GRAPHICS_AGE_ELEMENT = "Age";
    public static final String DEMO_GRAPHICS_TIME_ZONE_ELEMENT = "TimeZone";

    public static final String QUERY_TYPE = "Query";
    public static final String MODIFY_TYPE = "Modify";
    public static final String ATTRIBUTE_SEPARATOR = "|";
    public static final String PLUGIN = "plugin";
}
