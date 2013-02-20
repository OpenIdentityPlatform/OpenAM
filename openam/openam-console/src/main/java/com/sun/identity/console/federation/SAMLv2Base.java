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
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.federation.model.EntityModel;
import com.sun.identity.console.federation.model.SAMLv2ModelImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public abstract class SAMLv2Base extends EntityPropertiesBase {
    protected static final String PROPERTIES = "propertyAttributes";
    
    protected static List AUTH_CONTEXT_REF_NAMES = new ArrayList();    
    
    public static final int AUTH_CONTEXT_REF_COUNT= 25;

    public static final String InternetProtocol =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocol";
    public static final String InternetProtocolPassword =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocolPassword";
    public static final String Kerberos =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:Kerberos";
    public static final String MobileOneFactorUnregistered =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileOneFactorUnregistered";
    public static final String MobileTwoFactorUnregistered =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorUnregistered";
    public static final String MobileOneFactorContract =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileOneFactorContract";
    public static final String MobileTwoFactorContract =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorContract";
    public static final String Password =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:Password";
    public static final String PasswordProtectedTransport =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport";
    public static final String PreviousSession =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:PreviousSession";
    public static final String X509 =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:X509";
    public static final String PGP =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:PGP";
    public static final String SPKI =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:SPKI";
    public static final String XMLDSig =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig";
    public static final String Smartcard =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:Smartcard";
    public static final String SmartcardPKI =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:SmartcardPKI";
    public static final String SoftwarePKI =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:SoftwarePKI";
    public static final String Telephony =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:Telephony";
    public static final String NomadTelephony =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:NomadTelephony";
    public static final String PersonalTelephony =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:PersonalTelephony";
    public static final String AuthenticatedTelephony =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony";
    public static final String SecureRemotePassword =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:SecureRemotePassword";
    public static final String TLSClient =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:TLSClient";
    public static final String TimeSyncToken =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken";
    public static final String unspecified =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified";
    
    static {
        AUTH_CONTEXT_REF_NAMES.add(InternetProtocol);
        AUTH_CONTEXT_REF_NAMES.add(InternetProtocolPassword);
        AUTH_CONTEXT_REF_NAMES.add(Kerberos);
        AUTH_CONTEXT_REF_NAMES.add(MobileOneFactorUnregistered);
        AUTH_CONTEXT_REF_NAMES.add(MobileTwoFactorUnregistered);
        AUTH_CONTEXT_REF_NAMES.add(MobileOneFactorContract);
        AUTH_CONTEXT_REF_NAMES.add(MobileTwoFactorContract);
        AUTH_CONTEXT_REF_NAMES.add(Password);
        AUTH_CONTEXT_REF_NAMES.add(PasswordProtectedTransport);
        AUTH_CONTEXT_REF_NAMES.add(PreviousSession);
        AUTH_CONTEXT_REF_NAMES.add(X509);
        AUTH_CONTEXT_REF_NAMES.add(PGP);
        AUTH_CONTEXT_REF_NAMES.add(SPKI);
        AUTH_CONTEXT_REF_NAMES.add(XMLDSig);
        AUTH_CONTEXT_REF_NAMES.add(Smartcard);
        AUTH_CONTEXT_REF_NAMES.add(SmartcardPKI);
        AUTH_CONTEXT_REF_NAMES.add(SoftwarePKI);
        AUTH_CONTEXT_REF_NAMES.add(Telephony);
        AUTH_CONTEXT_REF_NAMES.add(NomadTelephony);
        AUTH_CONTEXT_REF_NAMES.add(PersonalTelephony);
        AUTH_CONTEXT_REF_NAMES.add(AuthenticatedTelephony);
        AUTH_CONTEXT_REF_NAMES.add(SecureRemotePassword);
        AUTH_CONTEXT_REF_NAMES.add(TLSClient);
        AUTH_CONTEXT_REF_NAMES.add(TimeSyncToken);
        AUTH_CONTEXT_REF_NAMES.add(unspecified);
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
}
