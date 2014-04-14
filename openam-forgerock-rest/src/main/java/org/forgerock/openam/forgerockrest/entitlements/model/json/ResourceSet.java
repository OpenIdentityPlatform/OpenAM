/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.forgerockrest.entitlements.model.json;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashSet;
import java.util.Set;

/**
 * Provides a container around a set of included and excluded resource names. Exists primarily to provide a better
 * JSON structure.
 *
 * @since 12.0.0
 */
public final class ResourceSet {
    private final Set<String> included;
    private final Set<String> excluded;

    @JsonCreator
    public ResourceSet(@JsonProperty("included") Set<String> included,
                       @JsonProperty("excluded") Set<String> excluded) {
        this.included = included;
        this.excluded = excluded;
    }

    public ResourceSet() {
        this(new HashSet<String>(), new HashSet<String>());
    }

    public Set<String> getIncluded() {
        return included;
    }

    public Set<String> getExcluded() {
        return excluded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceSet)) {
            return false;
        }

        ResourceSet that = (ResourceSet) o;
        return excluded.equals(that.excluded) && included.equals(that.included);
    }

    @Override
    public int hashCode() {
        int result = included.hashCode();
        result = 31 * result + excluded.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ResourceSet{" +
                "included=" + included +
                ", excluded=" + excluded +
                '}';
    }
}
