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
 * $Id: FSSAMLTrustedPartnersAddViewBean.java,v 1.3 2009/01/16 19:30:00 asyhuang Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPipeDelimitAttrTokenizer;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.federation.model.FSSAMLServiceModel;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FSSAMLTrustedPartnersAddViewBean
    extends FSSAMLTrustedPartnersViewBeanBase {
    public static final String DEFAULT_DISPLAY_URL =
        "/console/federation/FSSAMLTrustedPartnersAdd.jsp";
    private static final String SAML_TRUSTED_PARTNERS =
        "iplanet-am-saml-partner-urls";
    
    public FSSAMLTrustedPartnersAddViewBean (
        String pageName,
        String defaultDisplayURL
        ) {
        super (pageName, defaultDisplayURL);
    }
    
    public FSSAMLTrustedPartnersAddViewBean () {
        super ("FSSAMLTrustedPartnersAdd", DEFAULT_DISPLAY_URL);
    }
    
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        super.beginDisplay(event);    
         Set attributeNames = getAttributeNames();
        if (attributeNames.contains(SAMLConstants.SITEATTRIBUTEMAPPER)) {            
            String siteAttrMapper = (String)getDisplayFieldValue(
                SAMLConstants.SITEATTRIBUTEMAPPER);
            if ((siteAttrMapper == null) || (siteAttrMapper.length() == 0)) {                      
                setDisplayFieldValue(SAMLConstants.SITEATTRIBUTEMAPPER,
                    SAMLConstants.SITEATTRIBUTEMAPPERDEFAULT);
            }
        }        
    }
    
    protected void createPageTitleModel () {
        ptModel = new CCPageTitleModel (
            getClass ().getClassLoader ().getResourceAsStream (
            "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setPageTitleText (getPageTitleText ());
        ptModel.setValue ("button1", "button.previous");
        ptModel.setValue ("button2", "button.finish");
        ptModel.setValue ("button3", "button.cancel");
    }
    
    protected String getPageTitleText () {
        return "saml.profile.trustedpartners.create.page.title";
    }
    
    public void handleButton1Request (RequestInvocationEvent event) {
        FSSAMLSelectTrustedPartnerTypeViewBean vb =
            (FSSAMLSelectTrustedPartnerTypeViewBean)getViewBean (
            FSSAMLSelectTrustedPartnerTypeViewBean.class);
        unlockPageTrailForSwapping ();
        passPgSessionMap (vb);
        vb.forwardTo (getRequestContext ());
    }
    
    public void handleButton2Request (RequestInvocationEvent event)
    throws ModelControlException {
        super.handleButton1Request (event);
    }
    
    public void handleButton3Request (RequestInvocationEvent event)
    throws ModelControlException {
        super.handleButton2Request (event);
    }
    
    protected void handleButton1Request (Map values)
    throws AMConsoleException {
        String value = AMPipeDelimitAttrTokenizer.getInstance ().deTokenizes (values);
        
        // get the existing partners here
        FSSAMLServiceModel model = (FSSAMLServiceModel)getModel ();
        Map attrValues = model.getAttributeValues ();
        Set partners = null;
        Iterator iter = attrValues.keySet ().iterator ();
        while ( iter.hasNext () ){
            String name = (String)iter.next ();
            if(name.equals ("iplanet-am-saml-partner-urls")){
                partners = (Set)attrValues.get (name);
            }
        }
        
        if ((partners == null) || partners.isEmpty ()) {
            partners = new HashSet ();
        }
        
        // add new partner to existing partner set
        partners.add (value);
        
        Map map = new HashMap ();
        map.put ("iplanet-am-saml-partner-urls", partners);
        model.setAttributeValues (map);
        
        forwardToFederationView ();
    }
    
    protected boolean isCreateViewBean () {
        return true;
    }
    
    protected String getBreadCrumbDisplayName () {
        return "breadcrumbs.saml.addTrustedPartner";
    }
    
    protected boolean startPageTrail () {
        return false;
    }
}
