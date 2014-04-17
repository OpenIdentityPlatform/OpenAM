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

import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.EntitlementException;
import java.io.IOException;
import java.util.Map;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.utils.JsonValueBuilder;

/**
 * Wrapper for the Jsonification of the ApplicationType class.
 *
 * Taking an instance of an {@link ApplicationType} this class exposes the necessary
 * parts of that class to the Jackson {@link ObjectMapper} via annotations.
 */
public class ApplicationTypeWrapper implements Comparable<ApplicationTypeWrapper> {

    @JsonIgnore
    private final ApplicationType applicationType;

    public ApplicationTypeWrapper(ApplicationType applicationType) {
        this.applicationType = applicationType;
    }

    @JsonProperty("name")
    public String getName() {
        return applicationType.getName();
    }

    @JsonProperty("actions")
    public Map<String, Boolean> getActions() {
        return applicationType.getActions();
    }

    @JsonProperty("resourceComparator")
    public String getResourceComparator() {
        return applicationType.getResourceComparator().getClass().getCanonicalName();
    }

    @JsonProperty("saveIndex")
    public String getSaveIndex() {
        return applicationType.getSaveIndex().getClass().getCanonicalName();
    }


    @JsonProperty("searchIndex")
    public String getSearchIndex() {
        return applicationType.getSearchIndex().getClass().getCanonicalName();
    }

    @JsonProperty("applicationClassName")
    public String getApplicationClassName() {
        try {
            return applicationType.getApplicationClass().getCanonicalName();
        } catch (EntitlementException e) {
            return null;
        }
    }

    /**
     * Focus of this class. Calling this function will return a transportable {@link JsonValue}
     * representing the contained instantiation of {@link ApplicationType}.
     *
     * @return JsonValue representing the contained ApplicationType
     * @throws IOException if there were issues writing the value out.
     */
    public JsonValue toJsonValue() throws IOException {
        final ObjectMapper mapper = JsonValueBuilder.getObjectMapper();
        return JsonValueBuilder.toJsonValue(mapper.writeValueAsString(this));
    }

    @Override
    public int compareTo(ApplicationTypeWrapper that) {
        return this.getName().compareTo(that.getName());
    }

}
