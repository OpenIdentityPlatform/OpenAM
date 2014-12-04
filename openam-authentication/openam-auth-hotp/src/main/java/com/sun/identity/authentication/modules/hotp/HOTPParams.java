/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: HOTP.java,v 1.1 2009/03/24 23:52:12 pluo Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock AS
 */

package com.sun.identity.authentication.modules.hotp;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Class to hold the authentication modules HOTP configuration settings.
 */
public class HOTPParams {

    private final String gatewaySMSImplClass;
    private final long codeValidityDuration;
    private final String telephoneLdapAttributeName;
    private final String carrierLdapAttributeName;
    private final String emailLdapAttributeName;
    private final String codeDelivery;
    private final Map<?, ?> config;
    private final int codeLength;
    private final String messageSubject;
    private final String messageContent;
    private final String fromAddressAttributeName;
    private final Set<String> userSearchAttributes;

    public HOTPParams(String gatewaySMSImplClass, long codeValidityDuration, String telephoneLdapAttributeName,
            String carrierLdapAttributeName, String emailLdapAttributeName, String codeDelivery, Map<?, ?> config,
            int codeLength, String messageSubject, String messageContent, String fromAddressAttributeName) {
        this(gatewaySMSImplClass, codeValidityDuration, telephoneLdapAttributeName, carrierLdapAttributeName,
        emailLdapAttributeName, codeDelivery, config, codeLength, messageSubject, messageContent, fromAddressAttributeName,
        Collections.EMPTY_SET);
    }
    
    public HOTPParams(final String gatewaySMSImplClass, final long codeValidityDuration,
            final String telephoneLdapAttributeName, final String carrierLdapAttributeName,
            final String emailLdapAttributeName, final String codeDelivery, 
            final Map<?, ?> config, final int codeLength, final String messageSubject,
            final String messageContent, final String fromAddressAttributeName,
            final Set<String> userSearchAttributes) {
        this.gatewaySMSImplClass = gatewaySMSImplClass;
        this.codeValidityDuration = codeValidityDuration;
        this.telephoneLdapAttributeName = telephoneLdapAttributeName;
        this.carrierLdapAttributeName = carrierLdapAttributeName;
        this.emailLdapAttributeName = emailLdapAttributeName;
        this.codeDelivery = codeDelivery;
        this.config = config;
        this.codeLength = codeLength;
        this.messageSubject = messageSubject;
        this.messageContent = messageContent;
        this.fromAddressAttributeName = fromAddressAttributeName;
        this.userSearchAttributes = userSearchAttributes;
    }    

    public String getGatewaySMSImplClass() {
        return gatewaySMSImplClass;
    }

    public long getCodeValidityDuration() {
        return codeValidityDuration;
    }

    public String getTelephoneLdapAttributeName() {
        return telephoneLdapAttributeName;
    }

    public String getCarrierLdapAttributeName() {
        return carrierLdapAttributeName;
    }

    public String getEmailLdapAttributeName() {
        return emailLdapAttributeName;
    }

    public String getCodeDelivery() {
        return codeDelivery;
    }

    public Map<?, ?> getConfig() {
        return config;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public String getMessageSubject() {
        return messageSubject;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public String getFromAddressAttributeName() {
        return fromAddressAttributeName;
    }
    
    public Set<String> getUserSearchAttributes() {
        return Collections.unmodifiableSet(userSearchAttributes);
    }
}
