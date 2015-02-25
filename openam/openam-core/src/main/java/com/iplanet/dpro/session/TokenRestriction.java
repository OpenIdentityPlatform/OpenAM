/**
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
 * $Id: TokenRestriction.java,v 1.2 2008/06/25 05:41:29 qcheng Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.iplanet.dpro.session;

import java.io.Serializable;
import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * Interface to handle the different token restriction(s).
 * TokenRestrictions are serialized and deserialized during session failover and recovery as they are stored within
 * {@link com.iplanet.dpro.session.service.InternalSession} objcets. The {@link JsonTypeInfo} annotation makes sure of
 * saving type information as part of the serialized JSON, and provides a default implementation in case the type
 * information is missing (e.g. with old tokens that were generated without type info).
 */
@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = DNOrIPAddressListTokenRestriction.class)
public interface TokenRestriction extends Serializable {

    /**
     * Implements standard equals() semantics as mandated by Object.
     *
     * @param other The object to be used for comparison.
     * @return <code>true</code> if this object is the same as the other argument; <code>false</code> otherwise.
     */
    public boolean equals(Object other);

    /**
     * Returns a hash code for this object.
     *
     * @return A hash code value for this object.
     */
    public int hashCode();

    /**
     * Returns a true if the restriction matches the context for which it was set, otherwise it returns false.
     *
     * @param context The context from which the restriction needs to be checked.
     * @return boolean <code>true</code> if the restriction is satisfied, <code>false</code> otherwise.
     * @throws Exception If the there was an error.
     */
    public boolean isSatisfied(Object context) throws Exception;
}
