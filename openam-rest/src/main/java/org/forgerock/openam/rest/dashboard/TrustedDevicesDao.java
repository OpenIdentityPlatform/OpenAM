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

package org.forgerock.openam.rest.dashboard;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.openam.rest.resource.RealmContext;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.json;

/**
 * Dao for handling the retrieval and saving of a user's trusted devices.
 *
 * @since 12.0.0
 */
@Singleton
public class TrustedDevicesDao {

    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);

    /**
     * Gets a user's trusted device profiles.
     *
     * @param context The CREST context.
     * @return A list of trusted device profiles.
     * @throws InternalServerErrorException If there is a problem retrieving the device profiles.
     */
    List<JsonValue> getDeviceProfiles(Context context) throws InternalServerErrorException {

        List<JsonValue> devices = new ArrayList<JsonValue>();

        AMIdentity identity = getIdentity(getUser(context), getRealm(context));

        try {
            Set<String> set = (Set<String>) identity.getAttribute("devicePrintProfiles");

            for (String profile : set) {
                devices.add(json(mapper.readValue(profile, Map.class)));
            }

            return devices;

        } catch (SSOException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        } catch (JsonParseException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        } catch (JsonMappingException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        } catch (IdRepoException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        } catch (IOException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

    /**
     * Saves a user's trusted device proiles.
     *
     * @param context The CREST context.
     * @param profiles The user's trusted device profiles to store.
     * @throws InternalServerErrorException If there is a problem storing the device profiles.
     */
    void saveDeviceProfiles(Context context, List<JsonValue> profiles) throws InternalServerErrorException {

        AMIdentity identity = getIdentity(getUser(context), getRealm(context));

        Set<String> vals = new HashSet<String>();

        for (JsonValue profile : profiles) {
            vals.add(profile.toString());
        }

        Map<String, Set> attrMap = new HashMap<String, Set>();
        attrMap.put("devicePrintProfiles", vals);

        try {
            identity.setAttributes(attrMap);
            identity.store();
        } catch (SSOException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        } catch (IdRepoException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

    /**
     * Gets the authenticated user's name.
     *
     * @param context The CREST context.
     * @return The user name.
     */
    private String getUser(Context context) {
        RouterContext routerContext = context.asContext(RouterContext.class);
        return routerContext.getUriTemplateVariables().get("user");
    }

    /**
     * Gets the realm of the authenticated user.
     *
     * @param context The CREST context.
     * @return The user's realm.
     */
    private String getRealm(Context context) {
        return context.asContext(RealmContext.class).getResolvedRealm();
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
        AMIdentity amIdentity = null;
        AMIdentityRepository amIdRepo = AuthD.getAuth().getAMIdentityRepository(realm);

        IdSearchControl idsc = new IdSearchControl();
        idsc.setAllReturnAttributes(true);
        Set<AMIdentity> results = Collections.EMPTY_SET;

        try {
            idsc.setMaxResults(0);
            IdSearchResults searchResults = amIdRepo.searchIdentities(IdType.USER, userName, idsc);
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }

            if (results.isEmpty()) {
                throw new IdRepoException("getIdentity : User " + userName
                        + " is not found");
            } else if (results.size() > 1) {
                throw new IdRepoException(
                        "getIdentity : More than one user found for the userName "
                                + userName);
            }

            amIdentity = results.iterator().next();
        } catch (IdRepoException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        } catch (SSOException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }

        return amIdentity;
    }
}
