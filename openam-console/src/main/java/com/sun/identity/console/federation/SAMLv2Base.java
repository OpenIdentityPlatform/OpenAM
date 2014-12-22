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
 * "Portions Copyrighted [year] [name of copyright owner]
 *
 * $Id: SAMLv2Base.java,v 1.8 2008/06/25 05:49:37 qcheng Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS.
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.federation.model.EntityModel;
import com.sun.identity.console.federation.model.SAMLv2ModelImpl;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.view.html.CCDropDownMenu;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public abstract class SAMLv2Base extends EntityPropertiesBase {
    protected static final String PROPERTIES = "propertyAttributes";

    // Used to store the default list of supported contexts
    private static final List<String> DEFAULT_AUTH_CONTEXT_REF_NAMES;

    public static final String CHILD_AUTH_CONTEXT_TILED_VIEW = "tableTiledView";
    public static final String TBL_AUTHENTICATION_CONTEXTS = "tblAuthenticationContext";
    public static final String TBL_COL_SUPPORTED = "tblColSupported";
    public static final String TBL_DATA_SUPPORTED = "tblDataSupported";
    public static final String TBL_COL_CONTEXT_REFERENCE = "tblColContextReference";
    public static final String TBL_DATA_CONTEXT_REFERENCE = "tblDataContextReference";
    public static final String TBL_DATA_LABEL = "tblDataLabel";
    public static final String TBL_COL_KEY = "tblColKey";
    public static final String TBL_DATA_KEY = "tblDataKey";
    public static final String TBL_COL_VALUE = "tblColValue";
    public static final String TBL_DATA_VALUE = "tblDataValue";
    public static final String TBL_COL_LEVEL = "tblColLevel";
    public static final String TBL_DATA_LEVEL = "tblDataLevel";

    static {
        List<String> defaultContextNames = new ArrayList<String>();
        // The default set of context's shown in the Hosted IDP/SP pages
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocol");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocolPassword");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:Kerberos");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:MobileOneFactorUnregistered");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorUnregistered");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:MobileOneFactorContract");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorContract");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:Password");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:PreviousSession");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:X509");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:PGP");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:SPKI");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:Smartcard");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:SmartcardPKI");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:SoftwarePKI");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:Telephony");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:NomadTelephony");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:PersonalTelephony");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:SecureRemotePassword");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:TLSClient");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken");
        defaultContextNames.add("urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified");

        DEFAULT_AUTH_CONTEXT_REF_NAMES = Collections.unmodifiableList(defaultContextNames);
    }
    
    public SAMLv2Base(String name) {
        super(name);
    }
    
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        super.beginDisplay(event);
        
    }
    
    protected String getProfileName() {
        return EntityModel.SAMLV2;
    }
    
    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new SAMLv2ModelImpl(req, getPageSessionAttributes());
    }
    
    protected abstract void createPropertyModel();
    /**
     * Converts the List to Set.
     *
     * @param list the list to be converted.
     * @return the corresponding Set.
     */
    protected Set convertListToSet(List list) {
        Set s = new HashSet();
        s.addAll(list);
        return s;
    }
    
    /**
     * Return empty set if value is null
     *
     * @param set the set to be checked for null.
     * @return the EMPTY_SET if value is null.
     */
    protected Set returnEmptySetIfValueIsNull(Set set) {
        return (set != null) ? set : Collections.EMPTY_SET;
    }

    /**
     * For the given set of authContexts read from the extended metadata, populate the table and dropdown menu
     * @param authContexts The set of SAMLv2AuthContexts read from the extended metadata
     * @param tblAuthContextsModel The table model to populate
     * @param dropdownContextRef The name of the context dropdown menu component to populate
     */
    protected void populateAuthenticationContext(SAMLv2AuthContexts authContexts,
                                                 CCActionTableModel tblAuthContextsModel, String dropdownContextRef) {

        // Create lists from defaults that can be updated as there maybe custom entries from the extended metadata.
        Set<String> contextNames = new LinkedHashSet<String>(DEFAULT_AUTH_CONTEXT_REF_NAMES);
        OptionList options = new OptionList();
        // Used to indicate no default context
        options.add(getLabel("none"), "none");
        for (String name : contextNames) {
            options.add(getLabel(name), name);
        }

        // Need to compare the list from the metadata to the default list and any that are missing should be added
        // to the set being shown in the console as they will be custom entries
        Map<String, SAMLv2AuthContexts.SAMLv2AuthContext> contexts = authContexts.getCollections();
        for (SAMLv2AuthContexts.SAMLv2AuthContext value : contexts.values()) {
            if (contextNames.add(value.name)) {
                options.add(getLabel(value.name), value.name);
            }
        }

        CCDropDownMenu ac = (CCDropDownMenu) getChild(dropdownContextRef);
        ac.setOptions(options);

        tblAuthContextsModel.clear();
        int i = 0;
        for (String name : contextNames) {
            populateAuthenticationContext(name, authContexts, i++, tblAuthContextsModel, dropdownContextRef);
        }
    }

    private void populateAuthenticationContext(String name, SAMLv2AuthContexts authContexts, int index,
        CCActionTableModel tblAuthContextsModel, String dropdownContextRef) {

        if (index != 0) {
            tblAuthContextsModel.appendRow();
        }

        tblAuthContextsModel.setValue(TBL_DATA_CONTEXT_REFERENCE, name);
        tblAuthContextsModel.setValue(TBL_DATA_LABEL, getLabel(name));

        SAMLv2AuthContexts.SAMLv2AuthContext authContextObj = null;
        if (authContexts != null) {
            authContextObj = authContexts.get(name);
        }

        if (authContextObj == null) {
            tblAuthContextsModel.setValue(TBL_DATA_SUPPORTED, "");
            tblAuthContextsModel.setValue(TBL_DATA_LEVEL, "0");
        } else {
            tblAuthContextsModel.setValue(TBL_DATA_SUPPORTED, authContextObj.supported);
            tblAuthContextsModel.setValue(TBL_DATA_LEVEL, authContextObj.level);
            if (authContextObj.isDefault) {
                setDisplayFieldValue(dropdownContextRef, authContextObj.name);
            }
        }
    }

    private String getLabel(String name) {

        String key = getAuthContextI18nKey(name);
        String label = getModelInternal().getLocalizedString(key);
        // If the label ends up being the same as the key then there was no corresponding localized string so very
        // likely a custom context, just use the context name instead.
        if (label.equals(key)) {
            label = name;
        }

        return label;
    }

    private String getAuthContextI18nKey(String name) {
        int idx = name.lastIndexOf(":");
        String key = (idx != -1) ? name.substring(idx + 1) : name;
        return "samlv2.authenticationContext." + key + ".label";
    }
}
