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
import com.googlecode.ipv6.IPv6AddressRange;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.utils.ValidateIPaddress;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import static com.sun.identity.entitlement.EntitlementException.*;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.*;

public class IPv6Condition extends IPvXCondition<IPv6Address> {

    /**
     * Constructs a new IPv6Condition instance.
     */
    public IPv6Condition() {
        this(PrivilegeManager.debug, new CoreWrapper());
    }

    /**
     * Constructs a new IPv6Condition instance.
     *
     * @param debug A Debug instance.
     * @param coreWrapper An instance of the CoreWrapper.
     */
    IPv6Condition(Debug debug, CoreWrapper coreWrapper) {
        super(debug, coreWrapper, null, null, IPVersion.IPV6);
    }

    @Override
    protected IPv6Address stringToIp(String ip) {
        return IPv6Address.fromString(ip);
    }

    @Override
    protected boolean validateIPaddress(String ip) throws EntitlementException {
        return ValidateIPaddress.isIPv6(ip);
    }

}
