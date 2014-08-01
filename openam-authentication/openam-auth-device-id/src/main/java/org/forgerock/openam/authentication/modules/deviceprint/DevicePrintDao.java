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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DAO for getting and storing Device Print Profiles for a given user.
 *
 * @since 12.0.0
 */
public class DevicePrintDao {

    private static final String DEBUG_NAME = "amAuthDeviceIdSave";
    private static final Debug DEBUG = Debug.getInstance(DEBUG_NAME);

    static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String LDAP_DEVICE_PRINT_ATTRIBUTE_NAME = "devicePrintProfiles";

    /**
     * Gets the Device Print Profiles for the specified user.
     *
     * @param amIdentity The user's identity.
     * @return A {@code List} of the user's device print profiles.
     * @throws IdRepoException If there is a problem getting the device print profiles from LDAP.
     * @throws SSOException If there is a problem getting the device print profiles from LDAP.
     * @throws IOException If there is a problem parsing the device print profiles.
     */
    List<Map<String, Object>> getProfiles(AMIdentityWrapper amIdentity) throws IdRepoException, SSOException,
            IOException {

        Set<String> set = (Set<String>) amIdentity.getAttribute(LDAP_DEVICE_PRINT_ATTRIBUTE_NAME);
        List<Map<String, Object>> profiles = new ArrayList<Map<String, Object>>();
        for (String profile : set) {
            profiles.add(MAPPER.readValue(profile, Map.class));
        }
        return profiles;
    }

    /**
     * Stores the given Device Print Profiles for the specified user.
     *
     * @param amIdentity The user's identity.
     * @param profiles The {@code List} of the user's device print profiles.
     */
    void saveProfiles(AMIdentityWrapper amIdentity, List<Map<String, Object>> profiles) {
        try {
            Set<String> vals = new HashSet<String>();
            for (Map<String, Object> profile : profiles) {
                StringWriter writer = new StringWriter();
                MAPPER.writeValue(writer, profile);
                vals.add(writer.toString());
            }

            Map<String, Set> profilesMap = new HashMap<String, Set>();
            profilesMap.put(LDAP_DEVICE_PRINT_ATTRIBUTE_NAME, vals);

            amIdentity.setAttributes(profilesMap);
            amIdentity.store();
            DEBUG.message("Profiles stored");
        } catch (Exception e) {
            DEBUG.error("Could not store profiles. " + e);
        }
    }
}
