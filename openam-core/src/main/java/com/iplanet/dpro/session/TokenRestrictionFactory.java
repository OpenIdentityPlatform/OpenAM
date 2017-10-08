/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: TokenRestrictionFactory.java,v 1.3 2008/06/25 05:41:29 qcheng Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.iplanet.dpro.session;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.util.Set;

import org.forgerock.openam.dpro.session.NoOpTokenRestriction;
import org.forgerock.openam.utils.IOUtils;

import com.iplanet.sso.SSOException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * Factory for creating {@link TokenRestriction} instances.
 */

public class TokenRestrictionFactory {

    private static final String AM_SESSION_SERVICE = "iPlanetAMSessionService";

    /**
     * Serializes the restriction object.
     * 
     * @param tokenRestriction Token Restriction object to be serialized.
     * @return a serialized form of the restriction object.
     * @throws Exception if the there was an error.
     */
    public static String marshal(TokenRestriction tokenRestriction) throws Exception {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bs);
        os.writeObject(tokenRestriction);
        os.flush();
        os.close();
        return Base64.encode(bs.toByteArray());
    }

    /**
     * Deserialize the string into Token Restriction object.
     * 
     * @param data Token Restriction object in the string format.
     * @return a Token Restriction object.
     * @throws Exception if the there was an error.
     */
    public static TokenRestriction unmarshal(String data) throws Exception {
        return IOUtils.deserialise(Base64.decode(data), false);
    }

    /**
     * Create a new instance of {@link NoOpTokenRestriction},
     * which always satisfies the restriction.
     *
     * @return a new instance of {@link NoOpTokenRestriction}.
     */
    public NoOpTokenRestriction createNoOpTokenRestriction() {
        return new NoOpTokenRestriction();
    }

    /**
     * Create a new instance of {@link DNOrIPAddressListTokenRestriction},
     * which handles the restriction of the {@code DN} or {@code IPAddress}.
     *
     * @return a new instance of {@link DNOrIPAddressListTokenRestriction}.
     * @param dn the {@code DN} of the user.
     * @param hostNames the list of host names.
     * @throws UnknownHostException if the host cannot be resolved.
     * @throws SSOException if the single sign on token is invalid or expired.
     * @throws SMSException if an error occurred while trying to perform the operation.
     */
    public DNOrIPAddressListTokenRestriction createDNOrIPAddressListTokenRestriction(
            String dn,
            Set<String> hostNames) throws UnknownHostException, SSOException, SMSException {

        ServiceSchemaManager serviceSchemaManager = new ServiceSchemaManager(
                AM_SESSION_SERVICE,
                AccessController.doPrivileged(AdminTokenAction.getInstance()));
        return new DNOrIPAddressListTokenRestriction(dn, hostNames, serviceSchemaManager);
    }
}
