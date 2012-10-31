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
 * $Id: FSSAMLTrustedPartnersViewBeanBase.java,v 1.6 2009/01/16 19:30:24 asyhuang Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPipeDelimitAttrTokenizer;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.federation.model.FSSAMLServiceModel;
import com.sun.identity.console.federation.model.FSSAMLServiceModelImpl;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.EncryptAction;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public abstract class FSSAMLTrustedPartnersViewBeanBase
    extends AMPrimaryMastHeadViewBean {
    private static final String PGTITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String PGATTR_INDEX = "tblIndex";
    static final String PROFILES = "samlProfiles";
    
    private static Map errorMessages = new HashMap();
    private static Map exactAttributeNames = new HashMap();
    
    static {
        errorMessages.put(SAMLConstants.PARTNERNAME,
            "saml.profile.trustedPartner.missing.partnerName.message");
        errorMessages.put(SAMLConstants.SOURCEID,
            "saml.profile.trustedPartner.missing.sourceid.message");
        errorMessages.put(SAMLConstants.TARGET,
            "saml.profile.trustedPartner.missing.target.message");
        errorMessages.put(SAMLConstants.POSTURL,
            "saml.profile.trustedPartner.missing.postURL.message");
        errorMessages.put(SAMLConstants.SOAPUrl,
            "saml.profile.trustedPartner.missing.soapURL.message");
        errorMessages.put(SAMLConstants.ISSUER,
            "saml.profile.trustedPartner.missing.issuer.message");
        errorMessages.put(SAMLConstants.HOST_LIST,
            "saml.profile.trustedPartner.missing.hostList.message");
        errorMessages.put(SAMLConstants.SAMLURL,
            "saml.profile.trustedPartner.missing.samlURL.message");
    }
    
    static {
        exactAttributeNames.put(SAMLConstants.PARTNERNAME.toLowerCase(),
            SAMLConstants.PARTNERNAME);
        exactAttributeNames.put(SAMLConstants.SOURCEID.toLowerCase(),
            SAMLConstants.SOURCEID);
        exactAttributeNames.put(SAMLConstants.TARGET.toLowerCase(),
            SAMLConstants.TARGET);
        exactAttributeNames.put(SAMLConstants.SAMLURL.toLowerCase(),
            SAMLConstants.SAMLURL);
        exactAttributeNames.put(SAMLConstants.HOST_LIST.toLowerCase(),
            SAMLConstants.HOST_LIST);
        exactAttributeNames.put(SAMLConstants.SITEATTRIBUTEMAPPER.toLowerCase(),
            SAMLConstants.SITEATTRIBUTEMAPPER);
        exactAttributeNames.put(SAMLConstants.NAMEIDENTIFIERMAPPER.toLowerCase(),
            SAMLConstants.NAMEIDENTIFIERMAPPER);
        exactAttributeNames.put(SAMLConstants.VERSION.toLowerCase(),
            SAMLConstants.VERSION);
        exactAttributeNames.put(SAMLConstants.POSTURL.toLowerCase(),
            SAMLConstants.POSTURL);
        exactAttributeNames.put(SAMLConstants.SOAPUrl.toLowerCase(),
            SAMLConstants.SOAPUrl);
        exactAttributeNames.put(SAMLConstants.ACCOUNTMAPPER.toLowerCase(),
            SAMLConstants.ACCOUNTMAPPER);
        exactAttributeNames.put(SAMLConstants.AUTHTYPE.toLowerCase(),
            SAMLConstants.AUTHTYPE);
        exactAttributeNames.put(SAMLConstants.AUTH_UID.toLowerCase(),
            SAMLConstants.AUTH_UID);
        exactAttributeNames.put(SAMLConstants.AUTH_PASSWORD.toLowerCase(),
            SAMLConstants.AUTH_PASSWORD);
        exactAttributeNames.put(SAMLConstants.CERTALIAS.toLowerCase(),
            SAMLConstants.CERTALIAS);
        exactAttributeNames.put(SAMLConstants.ISSUER.toLowerCase(),
            SAMLConstants.ISSUER);
        exactAttributeNames.put(SAMLConstants.ATTRIBUTEMAPPER.toLowerCase(),
            SAMLConstants.ATTRIBUTEMAPPER);
        exactAttributeNames.put(SAMLConstants.ACTIONMAPPER.toLowerCase(),
            SAMLConstants.ACTIONMAPPER);
    }
    
    protected CCPageTitleModel ptModel;
    protected AMPropertySheetModel propertySheetModel;
    protected boolean populateValues = false;
    
    public FSSAMLTrustedPartnersViewBeanBase(
        String pageName,
        String defaultDisplayURL
        ) {
        super(pageName);
        setDefaultDisplayURL(defaultDisplayURL);
        createPageTitleModel();
    }
    
    protected void initialize() {
        if (!initialized) {
            if (createPropertyModel()) {
                registerChildren();
                initialized = true;
                super.initialize();
            }
        }
    }
    
    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE, CCPageTitle.class);
        ptModel.registerChildren(this);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        propertySheetModel.registerChildren(this);
    }
    
    protected View createChild(String name) {
        View view = null;
        if (name.equals(PGTITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if ((propertySheetModel != null) &&
            propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else {
            view = super.createChild(name);
        }
        
        return view;
    }
    
    public void populateValues(String idx) {
        setPageSessionAttribute(PGATTR_INDEX, idx);
        populateValues = true;
        
    }
    
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        super.beginDisplay(event);
        if (populateValues) {
            int index = Integer.parseInt((String)
            getPageSessionAttribute(PGATTR_INDEX));
            List list = (List) getPageSessionAttribute("samlPropertyAttributes");
            setValues(AMPipeDelimitAttrTokenizer.getInstance().tokenizes(
                (String)list.get(index)));
        }
        
        Set attributeNames = getAttributeNames();
        if (attributeNames.contains(SAMLConstants.VERSION)) {
            CCDropDownMenu menu = (CCDropDownMenu)getChild(
                SAMLConstants.VERSION);
            String version = (String)menu.getValue();
            if ((version == null) || (version.length() == 0)) {
                menu.setValue("1.1");
            }
        }
    }
    
    private boolean createPropertyModel() {
        List profiles = (List)getPageSessionAttribute(PROFILES);
        if (profiles != null) {
            SAMLPropertyXMLBuilder builder =
                SAMLPropertyXMLBuilder.getInstance();
            propertySheetModel = new AMPropertySheetModel(
                builder.getXML(profiles, !isCreateViewBean()));
            propertySheetModel.clear();
            return true;
        }
        return false;
    }
    
    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new FSSAMLServiceModelImpl(req, getPageSessionAttributes());
    }
    
    /**
     * Handles cancel request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event)
    throws ModelControlException {
        forwardToFederationView();
    }
    
    public void handleButton1Request(RequestInvocationEvent event)
    throws ModelControlException {
        Map values = new HashMap();
        try {
            String errorMsg = getValues(values);
            
            if (errorMsg != null) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    errorMsg);
                forwardTo();
            } else {
                handleButton1Request(values);
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }
    
    /**
     * Set value of a entry from the cache value in page session.
     *
     * @param idx Index of the entry in the cache Set.
     */
    protected void setValues(int idx) {
        Map mapAttrs = (Map)getPageSessionAttribute(
            FSSAMLServiceViewBean.PROPERTY_ATTRIBUTE);
        OrderedSet set = (OrderedSet)mapAttrs.get(
            FederationViewBean.TABLE_TRUSTED_PARTNERS);
        setValues(AMPipeDelimitAttrTokenizer.getInstance().tokenizes(
            (String)set.get(idx)));
    }
    
    private Map correctCaseOfAttributeNames(Map map) {
        Map corrected = new HashMap(map.size() *2);
        for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            String ckey = (String)exactAttributeNames.get(key.toLowerCase());
            if (ckey != null) {
                corrected.put(ckey, map.get(key));
            }
        }
        return corrected;
    }
    
    protected void setValues(Map map) {
        Map values = correctCaseOfAttributeNames(map);
        for (Iterator iter = values.keySet().iterator(); iter.hasNext(); ) {
            String attr = (String)iter.next();
            
            if (attr.equals(SAMLConstants.AUTH_PASSWORD)) {
                String pwd = (String)AccessController.doPrivileged(
                    new DecodeAction((String)values.get(attr)));
                propertySheetModel.setValue(SAMLConstants.AUTH_PASSWORD, pwd);
                propertySheetModel.setValue(
                    SAMLConstants.AUTH_PASSWORD +
                    SAMLPropertyTemplate.CONFIRM_SUFFIX, pwd);
            } else {
                propertySheetModel.setValue(attr, values.get(attr));
            }
        }
    }
    
    protected String getValues(Map map)
    throws AMConsoleException {
        String errorMessage = null;
        Set attributeNames = getAttributeNames();
        Map values = new HashMap(map.size() *2);
        
        for (Iterator iter = attributeNames.iterator(); iter.hasNext(); ) {
            String attrName = (String)iter.next();
            String value = (String)propertySheetModel.getValue(attrName);
            
            if (attrName.equals(SAMLConstants.AUTH_PASSWORD)) {
                if ((value != null) && (value.length() > 0)) {
                    String pwdConfirm = (String)propertySheetModel.getValue(
                        SAMLConstants.AUTH_PASSWORD +
                        SAMLPropertyTemplate.CONFIRM_SUFFIX);
                    if ((pwdConfirm == null) || !pwdConfirm.equals(value)) {
                        throw new AMConsoleException(
                            "saml.profile.trustedPartners.user.password.mismatchedmessage");
                    }
                }
            }
            
            if (value != null) {
                value = value.trim();
            } else {
                value = "";
            }
            values.put(attrName, value);
        }
        
        Set mandatory = getMandatoryAttributeNames();
        for (Iterator iter = mandatory.iterator(); iter.hasNext(); ) {
            String attrName = (String)iter.next();
            String value = (String)values.remove(attrName);
            map.put(attrName, value);
            
            if ((errorMessage == null) && (value.length() == 0)) {
                errorMessage = (String)errorMessages.get(attrName);
            }
        }
        
        // deal with the optional ones
        for (Iterator iter = values.keySet().iterator(); iter.hasNext(); ) {
            String attrName = (String)iter.next();
            String value = (String)values.get(attrName);
            if (value.length() > 0) {
                if (attrName.equals(SAMLConstants.AUTH_PASSWORD)) {
                    map.put(attrName,
                        (String)AccessController.doPrivileged(
                        new EncryptAction(value)));
                } else {
                    map.put(attrName, value);
                }
            }
        }
        return errorMessage;
    }
    
    /**
     * Edits an entry to the cache value in page session.
     *
     * @param values Map of attribute name to value.
     * @throws AMConsoleException if there are duplicate entries.
     */
    protected void editEntry(Map values)
    throws AMConsoleException {
        List list = (List) getPageSessionAttribute("samlPropertyAttributes");
        int index = Integer.parseInt((String)
        getPageSessionAttribute(PGATTR_INDEX));
        
        String value =
            AMPipeDelimitAttrTokenizer.getInstance().deTokenizes(values);
        int count = 0;
        
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            String v = (String)i.next();
            if ((count != index) && v.equals(value)) {
                throw new AMConsoleException(
                    "saml.profile.trustedPartner.already.exists");
            }
            count++;
        }
        list.set(index, value);
        Set set = new HashSet(list);
        try{
            FSSAMLServiceModel model = (FSSAMLServiceModel)getModel();
            model.modifyTrustPartners(set);
            setInlineAlertMessage(
                CCAlert.TYPE_INFO,
                "message.information",
                "saml.message.trusted.partner.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        forwardTo();
    }
    
    protected void forwardToFederationView() {
        FederationViewBean vb = (FederationViewBean)
        getViewBean(FederationViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
    protected Set getAttributeNames() {
        List profiles = (List)getPageSessionAttribute(PROFILES);
        SAMLPropertyXMLBuilder builder = SAMLPropertyXMLBuilder.getInstance();
        return builder.getAttributeNames(profiles);
    }
    
    private Set getMandatoryAttributeNames() {
        List profiles = (List)getPageSessionAttribute(PROFILES);
        SAMLPropertyXMLBuilder builder = SAMLPropertyXMLBuilder.getInstance();
        return builder.getMandatoryAttributeNames(profiles);
    }
    
    protected abstract void createPageTitleModel();
    protected abstract void handleButton1Request(Map values)
    throws AMConsoleException;
    protected abstract boolean isCreateViewBean();
}
