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
 * $Id: SAMLPropertyTemplate.java,v 1.5 2008/12/19 18:58:45 farble1670 Exp $
 *
 */

package com.sun.identity.console.federation;

import com.sun.identity.saml.common.SAMLConstants;
import java.util.HashMap;
import java.util.Map;

public class SAMLPropertyTemplate {
    private static Map templates = new HashMap();
    private static Map readonlyTemplates = new HashMap();
    private static Map sections = new HashMap();
    
    public static final String CONFIRM_SUFFIX = "_confirm";
    
    static {
        templates.put(SAMLConstants.PARTNERNAME,
            "<property required=\"true\"><label name=\"lblPartnerName\" defaultValue=\"saml.profile.trustedPartners.partnerName.label\" labelFor=\"" + SAMLConstants.PARTNERNAME+ "\" /><cc name=\"" + SAMLConstants.PARTNERNAME + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc><fieldhelp name=\"helpPartnerName\" defaultValue=\"saml.profile.trustedPartners.partnerName.help\"/></property>");
        readonlyTemplates.put(SAMLConstants.PARTNERNAME,
            "<property><label name=\"lblPartnerName\" defaultValue=\"saml.profile.trustedPartners.partnerName.label\" labelFor=\"" + SAMLConstants.PARTNERNAME+ "\" /><cc name=\"" + SAMLConstants.PARTNERNAME + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /><fieldhelp name=\"helpPartnerName\" defaultValue=\"saml.profile.trustedPartners.partnerName.help\" /></property>");

        templates.put(SAMLConstants.SOURCEID,
            "<property required=\"true\"><label name=\"lblSourceid\" defaultValue=\"saml.profile.trustedPartners.sourceID.label\" labelFor=\"" + SAMLConstants.SOURCEID+ "\" /><cc name=\"" + SAMLConstants.SOURCEID + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc><fieldhelp name=\"helpSourceid\" defaultValue=\"saml.profile.trustedPartners.sourceID.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.SOURCEID,
            "<property><label name=\"lblSourceid\" defaultValue=\"saml.profile.trustedPartners.sourceID.label\" labelFor=\"" + SAMLConstants.SOURCEID+ "\" /><cc name=\"" + SAMLConstants.SOURCEID + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /><fieldhelp name=\"helpSourceid\" defaultValue=\"saml.profile.trustedPartners.sourceID.help\" /></property>");
        
        templates.put(SAMLConstants.TARGET,
            "<property required=\"true\"><label name=\"lblTarget\" defaultValue=\"saml.profile.trustedPartners.target.label\" labelFor=\"" + SAMLConstants.TARGET + "\" /><cc name=\"" + SAMLConstants.TARGET + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc><fieldhelp name=\"helpTarget\" defaultValue=\"saml.profile.trustedPartners.target.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.TARGET,
            "<property><label name=\"lblTarget\" defaultValue=\"saml.profile.trustedPartners.target.label\" labelFor=\"" + SAMLConstants.TARGET + "\" /><cc name=\"" + SAMLConstants.TARGET + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /><fieldhelp name=\"helpTarget\" defaultValue=\"saml.profile.trustedPartners.target.help\" /></property>");
        
        templates.put(SAMLConstants.POSTURL,
            "<property required=\"true\"><label name=\"lblPostUrl\" defaultValue=\"saml.profile.trustedPartners.postURL.label\" labelFor=\"" + SAMLConstants.POSTURL + "\" /><cc name=\"" + SAMLConstants.POSTURL + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc><fieldhelp name=\"helpPostURL\" defaultValue=\"saml.profile.trustedPartners.postURL.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.POSTURL,
            "<property><label name=\"lblPostUrl\" defaultValue=\"saml.profile.trustedPartners.postURL.label\" labelFor=\"" + SAMLConstants.POSTURL + "\" /><cc name=\"" + SAMLConstants.POSTURL + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /><fieldhelp name=\"helpPostURL\" defaultValue=\"saml.profile.trustedPartners.postURL.help\" /></property>");
        
        templates.put(SAMLConstants.ATTRIBUTEMAPPER,
            "<property><label name=\"lblAttributeMapper\" defaultValue=\"saml.profile.trustedPartners.attributeMapper.label\" labelFor=\"" + SAMLConstants.ATTRIBUTEMAPPER + "\" /><cc name=\"" + SAMLConstants.ATTRIBUTEMAPPER + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc><fieldhelp name=\"helpAttributeMapper\" defaultValue=\"saml.profile.trustedPartners.attributeMapper.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.ATTRIBUTEMAPPER,
            "<property><label name=\"lblAttributeMapper\" defaultValue=\"saml.profile.trustedPartners.attributeMapper.label\" labelFor=\"" + SAMLConstants.ATTRIBUTEMAPPER + "\" /><cc name=\"" + SAMLConstants.ATTRIBUTEMAPPER + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /><fieldhelp name=\"helpAttributeMapper\" defaultValue=\"saml.profile.trustedPartners.attributeMapper.help\" /></property>");
        
        templates.put(SAMLConstants.ACTIONMAPPER,
            "<property><label name=\"lblActionMapper\" defaultValue=\"saml.profile.trustedPartners.actionMapper.label\" labelFor=\"" + SAMLConstants.ACTIONMAPPER + "\" /><cc name=\"" + SAMLConstants.ACTIONMAPPER + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc><fieldhelp name=\"helpActionMapper\" defaultValue=\"saml.profile.trustedPartners.actionMapper.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.ACTIONMAPPER,
            "<property><label name=\"lblActionMapper\" defaultValue=\"saml.profile.trustedPartners.actionMapper.label\" labelFor=\"" + SAMLConstants.ACTIONMAPPER + "\" /><cc name=\"" + SAMLConstants.ACTIONMAPPER + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /><fieldhelp name=\"helpActionMapper\" defaultValue=\"saml.profile.trustedPartners.actionMapper.help\" /></property>");
        
        templates.put(SAMLConstants.SITEATTRIBUTEMAPPER,
            "<property><label name=\"lblSiteAttributeMapper\" defaultValue=\"saml.profile.trustedPartners.siteAttributeMapper.label\" labelFor=\"" + SAMLConstants.SITEATTRIBUTEMAPPER + "\" /><cc name=\"" + SAMLConstants.SITEATTRIBUTEMAPPER + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc><fieldhelp name=\"helpSiteAttributeMapper\" defaultValue=\"saml.profile.trustedPartners.siteAttributeMapper.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.SITEATTRIBUTEMAPPER,
            "<property><label name=\"lblSiteAttributeMapper\" defaultValue=\"saml.profile.trustedPartners.siteAttributeMapper.label\" labelFor=\"" + SAMLConstants.SITEATTRIBUTEMAPPER + "\" /><cc name=\"" + SAMLConstants.SITEATTRIBUTEMAPPER + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /><fieldhelp name=\"helpSiteAttributeMapper\" defaultValue=\"saml.profile.trustedPartners.siteAttributeMapper.help\" /></property>");
        
        templates.put(SAMLConstants.NAMEIDENTIFIERMAPPER,
            "<property><label name=\"lblNameIdentifierMapper\" defaultValue=\"saml.profile.trustedPartners.nameIdentifierMapper.label\" labelFor=\"" + SAMLConstants.NAMEIDENTIFIERMAPPER + "\" /><cc name=\"" + SAMLConstants.NAMEIDENTIFIERMAPPER + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc><fieldhelp name=\"helpNameIdentifierMapper\" defaultValue=\"saml.profile.trustedPartners.nameIdentifierMapper.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.NAMEIDENTIFIERMAPPER,
            "<property><label name=\"lblNameIdentifierMapper\" defaultValue=\"saml.profile.trustedPartners.nameIdentifierMapper.label\" labelFor=\"" + SAMLConstants.NAMEIDENTIFIERMAPPER + "\" /><cc name=\"" + SAMLConstants.NAMEIDENTIFIERMAPPER + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /><fieldhelp name=\"helpNameIdentifierMapper\" defaultValue=\"saml.profile.trustedPartners.nameIdentifierMapper.help\" /></property>");
        
        templates.put(SAMLConstants.SOAPUrl,
            "<property required=\"true\"><label name=\"lblSoapUrl\" defaultValue=\"saml.profile.trustedPartners.soapURL.label\" labelFor=\"" + SAMLConstants.SOAPUrl + "\" /><cc name=\"" + SAMLConstants.SOAPUrl + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc><fieldhelp name=\"helpSoapURL\" defaultValue=\"saml.profile.trustedPartners.soapURL.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.SOAPUrl,
            "<property required=\"true\"><label name=\"lblSoapUrl\" defaultValue=\"saml.profile.trustedPartners.soapURL.label\" labelFor=\"" + SAMLConstants.SOAPUrl + "\" /><cc name=\"" + SAMLConstants.SOAPUrl + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc><fieldhelp name=\"helpSoapURL\" defaultValue=\"saml.profile.trustedPartners.soapURL.help\" /></property>");
        
        templates.put(SAMLConstants.ACCOUNTMAPPER,
            "<property><label name=\"lblAccountMapper\" defaultValue=\"saml.profile.trustedPartners.accountMapper.label\" labelFor=\"" + SAMLConstants.ACCOUNTMAPPER + "\" /><cc name=\"" + SAMLConstants.ACCOUNTMAPPER + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc><fieldhelp name=\"helpAccountMapper\" defaultValue=\"saml.profile.trustedPartners.accountMapper.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.ACCOUNTMAPPER,
            "<property><label name=\"lblAccountMapper\" defaultValue=\"saml.profile.trustedPartners.accountMapper.label\" labelFor=\"" + SAMLConstants.ACCOUNTMAPPER + "\" /><cc name=\"" + SAMLConstants.ACCOUNTMAPPER + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /><fieldhelp name=\"helpAccountMapper\" defaultValue=\"saml.profile.trustedPartners.accountMapper.help\" /></property>");
        
        templates.put(SAMLConstants.AUTHTYPE,
            "<property><label name=\"lblAuthenticationType\" defaultValue=\"saml.profile.trustedPartners.authenticationType.label\" labelFor=\"" + SAMLConstants.AUTHTYPE + "\" /><cc name=\"" + SAMLConstants.AUTHTYPE + "\" tagclass=\"com.sun.web.ui.taglib.html.CCRadioButtonTag\"><option label=\"saml.profile.trustedPartners.authenticationType.option.none\" value=\"" + SAMLConstants.NOAUTH + "\" /><option label=\"saml.profile.trustedPartners.authenticationType.option.basic\" value=\"" + SAMLConstants.BASICAUTH + "\" /><option label=\"saml.profile.trustedPartners.authenticationType.option.ssl\" value=\"" + SAMLConstants.SSL + "\" /><option label=\"saml.profile.trustedPartners.authenticationType.option.sslWithBasic\" value=\"" + SAMLConstants.SSLWITHBASICAUTH + "\" /></cc><fieldhelp name=\"helpAuthenticationType\" defaultValue=\"saml.profile.trustedPartners.authenticationType.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.AUTHTYPE,
            "<property><label name=\"lblAuthenticationType\" defaultValue=\"saml.profile.trustedPartners.authenticationType.label\" labelFor=\"" + SAMLConstants.AUTHTYPE + "\" /><cc name=\"" + SAMLConstants.AUTHTYPE + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /><fieldhelp name=\"helpAuthenticationType\" defaultValue=\"saml.profile.trustedPartners.authenticationType.help\" /></property>");
        
        templates.put(SAMLConstants.AUTH_UID,
            "<property><label name=\"lblUser\" defaultValue=\"saml.profile.trustedPartners.user.label\" labelFor=\"" + SAMLConstants.AUTH_UID + "\" /><cc name=\"" + SAMLConstants.AUTH_UID + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc><fieldhelp name=\"helpUser\" defaultValue=\"saml.profile.trustedPartners.user.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.AUTH_UID,
            "<property><label name=\"lblUser\" defaultValue=\"saml.profile.trustedPartners.user.label\" labelFor=\"" + SAMLConstants.AUTH_UID + "\" /><cc name=\"" + SAMLConstants.AUTH_UID + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /><fieldhelp name=\"helpUser\" defaultValue=\"saml.profile.trustedPartners.user.help\" /></property>");
        
        templates.put(SAMLConstants.AUTH_PASSWORD,
            "<property><label name=\"lblPassword\" defaultValue=\"saml.profile.trustedPartners.password.label\" labelFor=\"" + SAMLConstants.AUTH_PASSWORD + "\" /><cc name=\"" + SAMLConstants.AUTH_PASSWORD + "\" tagclass=\"com.sun.web.ui.taglib.html.CCPasswordTag\" /><fieldhelp name=\"helpPassword\" defaultValue=\"saml.profile.trustedPartners.password.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.AUTH_PASSWORD,
            "<property><label name=\"lblPassword\" defaultValue=\"saml.profile.trustedPartners.password.label\" labelFor=\"" + SAMLConstants.AUTH_PASSWORD + "\" /><cc name=\"" + SAMLConstants.AUTH_PASSWORD + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\"><attribute name=\"defaultValue\" value=\"********\" /></cc><fieldhelp name=\"helpPassword\" defaultValue=\"saml.profile.trustedPartners.password.help\" /></property>");
        
        templates.put(SAMLConstants.AUTH_PASSWORD + CONFIRM_SUFFIX,
            "<property><label name=\"lblUser\" defaultValue=\"saml.profile.trustedPartners.passwordConfirm.label\" labelFor=\"" + SAMLConstants.AUTH_PASSWORD + CONFIRM_SUFFIX + "\" /><cc name=\"" + SAMLConstants.AUTH_PASSWORD + CONFIRM_SUFFIX + "\" tagclass=\"com.sun.web.ui.taglib.html.CCPasswordTag\" /></property>");
        readonlyTemplates.put(SAMLConstants.AUTH_PASSWORD + CONFIRM_SUFFIX, "");
        
        templates.put(SAMLConstants.CERTALIAS,
            "<property><label name=\"lblCertificate\" defaultValue=\"saml.profile.trustedPartners.certificate.label\" labelFor=\"" + SAMLConstants.CERTALIAS + "\" /><cc name=\"" + SAMLConstants.CERTALIAS + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc><fieldhelp name=\"helpCertificate\" defaultValue=\"saml.profile.trustedPartners.certificate.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.CERTALIAS,
            "<property><label name=\"lblCertificate\" defaultValue=\"saml.profile.trustedPartners.certificate.label\" labelFor=\"" + SAMLConstants.CERTALIAS + "\" /><cc name=\"" + SAMLConstants.CERTALIAS + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /><fieldhelp name=\"helpCertificate\" defaultValue=\"saml.profile.trustedPartners.certificate.help\" /></property>");
        
        templates.put(SAMLConstants.ISSUER,
            "<property required=\"true\"><label name=\"lblIssuer\" defaultValue=\"saml.profile.trustedPartners.issuer.label\" labelFor=\"" + SAMLConstants.ISSUER + "\" /><cc name=\"" + SAMLConstants.ISSUER + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc><fieldhelp name=\"helpIssuer\" defaultValue=\"saml.profile.trustedPartners.issuer.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.ISSUER,
            "<property><label name=\"lblIssuer\" defaultValue=\"saml.profile.trustedPartners.issuer.label\" labelFor=\"" + SAMLConstants.ISSUER + "\" /><cc name=\"" + SAMLConstants.ISSUER + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /><fieldhelp name=\"helpIssuer\" defaultValue=\"saml.profile.trustedPartners.issuer.help\" /></property>");
        
        templates.put(SAMLConstants.SAMLURL,
            "<property required=\"true\"><label name=\"lblSamlUrl\" defaultValue=\"saml.profile.trustedPartners.samlURL.label\" labelFor=\"" + SAMLConstants.SAMLURL + "\" /><cc name=\"" + SAMLConstants.SAMLURL + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc><fieldhelp name=\"helpSamlURL\" defaultValue=\"saml.profile.trustedPartners.samlURL.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.SAMLURL,
            "<property><label name=\"lblSamlUrl\" defaultValue=\"saml.profile.trustedPartners.samlURL.label\" labelFor=\"" + SAMLConstants.SAMLURL + "\" /><cc name=\"" + SAMLConstants.SAMLURL + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /><fieldhelp name=\"helpSamlURL\" defaultValue=\"saml.profile.trustedPartners.samlURL.help\" /></property>");
        
        templates.put(SAMLConstants.HOST_LIST,
            "<property required=\"true\"><label name=\"lblHostlist\" defaultValue=\"saml.profile.trustedPartners.hostList.label\" labelFor=\"" + SAMLConstants.HOST_LIST + "\" /><cc name=\"" + SAMLConstants.HOST_LIST + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc><fieldhelp name=\"helpHostList\" defaultValue=\"saml.profile.trustedPartners.hostList.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.HOST_LIST,
            "<property><label name=\"lblHostlist\" defaultValue=\"saml.profile.trustedPartners.hostList.label\" labelFor=\"" + SAMLConstants.HOST_LIST + "\" /><cc name=\"" + SAMLConstants.HOST_LIST + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /><fieldhelp name=\"helpHostList\" defaultValue=\"saml.profile.trustedPartners.hostList.help\" /></property>");
        
        templates.put(SAMLConstants.VERSION,
            "<property><label name=\"lblVersion\" defaultValue=\"saml.profile.trustedPartners.version.label\" labelFor=\"" + SAMLConstants.VERSION + "\" /><cc name=\"" + SAMLConstants.VERSION + "\" tagclass=\"com.sun.web.ui.taglib.html.CCDropDownMenuTag\"><option label=\"saml.profile.trustedPartners.version.option.1.0\" value=\"1.0\" /><option label=\"saml.profile.trustedPartners.version.option.1.1\" value=\"1.1\" /></cc><fieldhelp name=\"helpVersion\" defaultValue=\"saml.profile.trustedPartners.version.help\" /></property>");
        readonlyTemplates.put(SAMLConstants.VERSION,
            "<property><label name=\"lblVersion\" defaultValue=\"saml.profile.trustedPartners.version.label\" labelFor=\"" + SAMLConstants.VERSION + "\" /><cc name=\"" + SAMLConstants.VERSION + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /><fieldhelp name=\"helpVersion\" defaultValue=\"saml.profile.trustedPartners.version.help\" /></property>");
        
        sections.put(SAMLProperty.COMMON_SETTINGS,
            "<section name=\"sectionCommon\" defaultValue=\"saml.profile.trustedPartners.selectType.profile.common.label\">");
        sections.put(SAMLProperty.ROLE_DESTINATION,
            "<section name=\"sectionDestination\" defaultValue=\"saml.profile.trustedPartners.selectType.profile.destination.label\">");
        sections.put(SAMLProperty.ROLE_SOURCE,
            "<section name=\"sectionSource\" defaultValue=\"saml.profile.trustedPartners.selectType.profile.source.label\">");
        sections.put(SAMLProperty.METHOD_ARTIFACT,
            "<subsection name=\"sectionArtifact\" defaultValue=\"saml.profile.trustedPartners.selectType.profile.artifact.label\">");
        sections.put(SAMLProperty.METHOD_POST,
            "<subsection name=\"sectionPost\" defaultValue=\"saml.profile.trustedPartners.selectType.profile.post.label\">");
        sections.put(SAMLProperty.METHOD_SOAP,
            "<subsection name=\"sectionSOAP\" defaultValue=\"saml.profile.trustedPartners.selectType.profile.soap.label\">");
    }
    
    static final String DESTINATION_SOAP_ISSUER_XML =
        "<property><label name=\"lblIssuer\" defaultValue=\"saml.profile.trustedPartners.issuer.label\" labelFor=\"" + SAMLConstants.ISSUER + "\" /><cc name=\"" + SAMLConstants.ISSUER + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\"><attribute name=\"size\" value=\"75\" /><attribute name=\"autoSubmit\" value=\"false\"/></cc></property>";
    static final String DESTINATION_SOAP_ISSUER_XML_READONLY =
        "<property><label name=\"lblIssuer\" defaultValue=\"saml.profile.trustedPartners.issuer.label\" labelFor=\"" + SAMLConstants.ISSUER + "\" /><cc name=\"" + SAMLConstants.ISSUER + "\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\" /></property>";
    
    static final String EDIT_SETTING_XML =
        "<property><label name=\"lblEditSettting\" defaultValue=\"saml.profile.trustedPartners.manage.profile.message\" labelFor=\"btnModifyProfile\" /><cc name=\"btnModifyProfile\" tagclass=\"com.sun.web.ui.taglib.html.CCButtonTag\"><attribute name=\"defaultValue\" value=\"saml.profile.trustedPartners.setType.button\" /></cc></property>";
    
    static final String NO_ATTRIBUTE_CC =
        "<property span=\"true\"><cc name=\"noAttr\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\"><attribute name=\"defaultValue\" value=\"saml.profile.trustedPartners.noattribute.message\" /></cc></property>";
    
    private SAMLPropertyTemplate() {
    }
    
    static String getAttribute(String name, boolean readonly) {
        return (readonly) ?
            (String)readonlyTemplates.get(name) :
            (String)templates.get(name);
    }
    
    static String getSection(String name) {
        return (String)sections.get(name);
    }
}
