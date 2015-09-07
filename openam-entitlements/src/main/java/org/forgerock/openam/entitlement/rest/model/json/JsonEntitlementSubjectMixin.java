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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.rest.model.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.sun.identity.entitlement.EntitlementSubject;

import java.util.Map;
import java.util.Set;

/**
 * Jackson JSON mixin class that renames/ignores various attributes in entitlement subject implementations to provide
 * a nicer JSON representation.
 *
 * @since 12.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type", include = JsonTypeInfo.As.PROPERTY)
@JsonTypeIdResolver(EntitlementSubjectTypeIdResolver.class)
public abstract class JsonEntitlementSubjectMixin {

    @JsonIgnore
    public abstract String getState();

    @JsonIgnore
    public abstract Map<String, Set<String>> getSearchIndexAttributes();

    @JsonIgnore
    public abstract Set<String> getRequiredAttributeNames();

    @JsonIgnore
    public abstract boolean isIdentity();

    @JsonIgnore
    public abstract boolean isExclusive();

    @JsonProperty("subjectName")
    public abstract String getPSubjectName();

    @JsonProperty("subjects")
    public abstract Set<EntitlementSubject> getESubjects();

    @JsonProperty("subject")
    public abstract EntitlementSubject getESubject();
}
