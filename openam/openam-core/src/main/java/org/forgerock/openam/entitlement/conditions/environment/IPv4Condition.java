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
 * Copyright 2006 Sun Microsystems Inc
 */
/*
 * Portions Copyright 2011-2014 ForgeRock AS
 */

package org.forgerock.openam.entitlement.conditions.environment;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.shared.debug.Debug;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;
import java.util.StringTokenizer;

import static com.sun.identity.entitlement.EntitlementException.INVALID_PROPERTY_VALUE;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.*;

/**
 * An <code>EntitlementCondition</code> that can be used to enable/disable an authorization policy
 * based on the IP address and DNS name of the originating client requesting access to a resource.
 */
public class IPv4Condition extends IPvXCondition<Long> {

    /**
     * Constructs a new IPv4Condition instance.
     */
    public IPv4Condition() {
        this(PrivilegeManager.debug);
    }

    /**
     * Constructs a new IPv4Condition instance.
     *
     * @param debug A Debug instance.
     */
    IPv4Condition(Debug debug) {
        super(debug, Long.MAX_VALUE, Long.MAX_VALUE, IPVersion.IPV4);
    }

    /**
     * JSON deserialization constructor used to ensure fields are set in an order
     * that allows inter-field validation to pass.
     *
     * @throws EntitlementException If any of the provided properties fail validation
     */
    @JsonCreator
    public IPv4Condition(@JsonProperty(START_IP) String startIp,
                         @JsonProperty(END_IP) String endIp,
                         @JsonProperty(IP_RANGE) List<String> ipRange,
                         @JsonProperty(DNS_NAME) List<String> dnsName) throws EntitlementException {

        super(PrivilegeManager.debug, Long.MAX_VALUE, Long.MAX_VALUE, IPVersion.IPV4,
                startIp, endIp, ipRange, dnsName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Long stringToIp(String ip) throws EntitlementException {
        StringTokenizer st = new StringTokenizer(ip, ".");
        int tokenCount = st.countTokens();
        if (tokenCount != 4) {
            throw new EntitlementException(INVALID_PROPERTY_VALUE, new String[]{"ip", ip});
        }
        long ipValue = 0L;
        while (st.hasMoreElements()) {
            String s = st.nextToken();
            short ipElement;
            try {
                ipElement = Short.parseShort(s);
            } catch (Exception e) {
                throw new EntitlementException(INVALID_PROPERTY_VALUE, new String[]{"ip", ip});
            }
            if (ipElement < 0 || ipElement > 255) {
                throw new EntitlementException(INVALID_PROPERTY_VALUE, new String[]{"ip", s});
            }
            ipValue = ipValue * 256L + ipElement;
        }
        return ipValue;
    }
}
