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
package org.forgerock.openam.forgerockrest.entitlements.wrappers;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.ReferralPrivilege;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.utils.JsonValueBuilder;

/**
 * Wrapper for the jsonification of {@link ReferralPrivilege} objects, for
 * interaction via the REST endpoint {@link org.forgerock.openam.forgerockrest.entitlements.ReferralsResource}.
 */
public class ReferralWrapper implements Comparable<ReferralWrapper> {

    @JsonIgnore
    private final ReferralPrivilege referralPrivilege;

    public ReferralWrapper(ReferralPrivilege referralPrivilege) {
        this.referralPrivilege = referralPrivilege;
    }

    /**
     * Necessary default constructor for Json
     */
    public ReferralWrapper() {
        referralPrivilege = new ReferralPrivilege();
    }

    @JsonProperty("name")
    public String getName() {
        return referralPrivilege.getName();
    }

    @JsonProperty("name")
    public void setName(String name) {
        referralPrivilege.setName(name);
    }

    @JsonProperty("resources")
    public Map<String, Set<String>> getResources() {
        return referralPrivilege.getMapApplNameToResources();
    }

    @JsonProperty("resources")
    public void setResources(Map<String, Set<String>> resourceMap) throws EntitlementException {
        referralPrivilege.setMapApplNameToResources(resourceMap);
    }

    @JsonProperty("realms")
    public Set<String> getRealms() {
        return referralPrivilege.getRealms();
    }

    @JsonProperty("realms")
    public void setRealms(Set<String> realms) throws EntitlementException {
        referralPrivilege.setRealms(realms);
    }

    @JsonIgnore
    public ReferralPrivilege getReferral() {
        return referralPrivilege;
    }

    /**
     * Focus of this class. Calling this function will return a transportable {@link org.forgerock.json.fluent.JsonValue}
     * representing the contained instantiation of {@link ReferralPrivilege}.
     *
     * @return JsonValue representing the contained ReferralPrivilege
     * @throws java.io.IOException if there were issues writing the value out.
     */
    public JsonValue toJsonValue() throws IOException {
        final ObjectMapper mapper = JsonValueBuilder.getObjectMapper();
        return JsonValueBuilder.toJsonValue(mapper.writeValueAsString(this));
    }

    @Override
    public int compareTo(ReferralWrapper that) {
        return this.getName().compareTo(that.getName());
    }

    @JsonProperty("createdBy")
    public void setCreatedBy(String createdBy) {
        referralPrivilege.setCreatedBy(createdBy);
    }

    @JsonProperty("createdBy")
    public String getCreatedBy() {
        return referralPrivilege.getCreatedBy();
    }

    @JsonProperty("lastModifiedBy")
    public void setLastModifiedBy(String lastModifiedBy) {
        referralPrivilege.setLastModifiedBy(lastModifiedBy);
    }

    @JsonProperty("lastModifiedBy")
    public String getLastModifiedBy() {
        return referralPrivilege.getLastModifiedBy();
    }

    @JsonProperty("creationDate")
    public void setCreationDate(long creationDate) {
        referralPrivilege.setCreationDate(creationDate);
    }

    @JsonProperty("creationDate")
    public long getCreationDate() {
        return referralPrivilege.getCreationDate();
    }

    @JsonProperty("lastModifiedDate")
    public void setLastModifiedDate(long lastModifiedDate) {
        referralPrivilege.setLastModifiedDate(lastModifiedDate);
    }

    @JsonProperty("lastModifiedDate")
    public long getLastModifiedDate() {
        return referralPrivilege.getLastModifiedDate();
    }
}
