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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.rest.dashboard;

import static org.forgerock.json.fluent.JsonValue.*;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.openam.rest.resource.RealmContext;

/**
 * DAO for handling the retrieval and saving of a user's trusted devices.
 *
 * @since 13.0.0
 */
public class UserDevicesDao {

    private static final int NO_LIMIT = 0;

    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
    private final String attributeName;

    public UserDevicesDao(String attributeName) {
        this.attributeName = attributeName;
    }

    /**
     * Gets a user's device profiles. The JSON returned will correspond to the objects utilised
     * by {@link OathDeviceSettings}.
     *
     * @param username User whose profiles to return.
     * @param realm Realm in which we are operating.
     * @return A list of device profiles.
     * @throws InternalServerErrorException If there is a problem retrieving the device profiles.
     */
    public List<JsonValue> getDeviceProfiles(String username, String realm) throws InternalServerErrorException {

        List<JsonValue> devices = new ArrayList<JsonValue>();

        AMIdentity identity = getIdentity(username, realm);

        try {
            Set<String> set = (Set<String>) identity.getAttribute(attributeName);

            for (String profile : set) {
                devices.add(json(mapper.readValue(profile, Map.class))); //todo update based on AME-6128/AME-6129
            }

            return devices;

        } catch (SSOException | IOException | IdRepoException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

    /**
     * Saves a user's trusted device profiles.
     *
     * @param username User whose profiles to return.
     * @param realm Realm in which we are operating.
     * @param profiles The user's trusted device profiles to store.
     * @throws InternalServerErrorException If there is a problem storing the device profiles.
     */
    public void saveDeviceProfiles(String username, String realm, List<JsonValue> profiles)
            throws InternalServerErrorException {

        AMIdentity identity = getIdentity(username, realm);

        Set<String> vals = new HashSet<String>();

        for (JsonValue profile : profiles) {
            vals.add(profile.toString());
        }

        Map<String, Set> attrMap = new HashMap<String, Set>();
        attrMap.put(attributeName, vals);

        try {
            identity.setAttributes(attrMap);
            identity.store();
        } catch (SSOException | IdRepoException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

    /**
     * Gets the {@code AMIdentity} for the authenticated user.
     *
     * @param userName The user's name.
     * @param realm The user's realm.
     * @return An {@code AMIdentity}.
     * @throws InternalServerErrorException If there is a problem getting the users identity.
     */
    private AMIdentity getIdentity(String userName, String realm) throws InternalServerErrorException {
        final AMIdentity amIdentity;
        final AMIdentityRepository amIdRepo = AuthD.getAuth().getAMIdentityRepository(realm);

        final IdSearchControl idsc = new IdSearchControl();
        idsc.setAllReturnAttributes(true);

        Set<AMIdentity> results = Collections.emptySet();

        try {
            idsc.setMaxResults(NO_LIMIT);
            IdSearchResults searchResults = amIdRepo.searchIdentities(IdType.USER, userName, idsc);
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }

            if (results.isEmpty()) {
                throw new IdRepoException("getIdentity : User " + userName + " is not found");
            } else if (results.size() > 1) {
                throw new IdRepoException("getIdentity : More than one user found for the userName " + userName);
            }

            amIdentity = results.iterator().next();
        } catch (IdRepoException | SSOException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }

        return amIdentity;
    }
}