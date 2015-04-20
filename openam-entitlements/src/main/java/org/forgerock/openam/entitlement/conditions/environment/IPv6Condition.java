/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS
 */

package org.forgerock.openam.entitlement.conditions.environment;

import com.googlecode.ipv6.IPv6Address;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.shared.debug.Debug;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

import static com.sun.identity.entitlement.EntitlementException.INVALID_PROPERTY_VALUE;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.*;

/**
 * An <code>EntitlementCondition</code> that can be used to enable/disable an authorization policy
 * based on the IP address and DNS name of the originating client requesting access to a resource.
 */
public class IPv6Condition extends IPvXCondition<IPv6Address> {

    /**
     * Constructs a new IPv6Condition instance.
     */
    public IPv6Condition() {
        this(PrivilegeManager.debug);
    }

    /**
     * Constructs a new IPv6Condition instance.
     *
     * @param debug A Debug instance.
     */
    IPv6Condition(Debug debug) {
        super(debug, null, null, IPVersion.IPV6);
    }

    /**
     * JSON deserialization constructor used to ensure fields are set in an order
     * that allows inter-field validation to pass.
     *
     * @throws EntitlementException If any of the provided properties fail validation
     */
    @JsonCreator
    public IPv6Condition(@JsonProperty(START_IP) String startIp,
                         @JsonProperty(END_IP) String endIp,
                         @JsonProperty(IP_RANGE) List<String> ipRange,
                         @JsonProperty(DNS_NAME) List<String> dnsName) throws EntitlementException {

        super(PrivilegeManager.debug, null, null, IPVersion.IPV6,
                startIp, endIp, ipRange, dnsName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IPv6Address stringToIp(String ip) throws EntitlementException {
        try {
            return IPv6Address.fromString(ip);
        } catch (IllegalArgumentException ex) {
            throw new EntitlementException(INVALID_PROPERTY_VALUE, new String[]{"ip", ip});
        }
    }

}
