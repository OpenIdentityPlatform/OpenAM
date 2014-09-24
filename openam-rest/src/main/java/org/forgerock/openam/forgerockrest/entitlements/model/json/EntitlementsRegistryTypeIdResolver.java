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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.forgerockrest.entitlements.model.json;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.forgerock.openam.entitlement.EntitlementRegistry;

/**
 * Abstract base class for type id resolvers based on the {@link org.forgerock.openam.entitlement.EntitlementRegistry}.
 *
 * @since 12.0.0
 */
public abstract class EntitlementsRegistryTypeIdResolver<T> implements TypeIdResolver {
    private final EntitlementRegistry registry = EntitlementRegistry.load();

    private volatile JavaType baseType;


    @Override
    public void init(JavaType baseType) {
        this.baseType = baseType;
    }

    /**
     * Get the short name for the given type.
     * @param registry the registry to lookup the short name in.
     * @param value the value to lookup.
     * @return the short name of the given type.
     */
    protected abstract String getShortName(EntitlementRegistry registry, T value);

    /**
     * Get the concrete sub-type to use for the given short name.
     * @param registry the registry to use to lookup the type.
     * @param shortName the short name of the type.
     * @return the concrete sub-type to use.
     */
    protected abstract Class<? extends T> getType(EntitlementRegistry registry, String shortName);

    @Override
    @SuppressWarnings("unchecked")
    public String idFromValue(Object value) {
        return getShortName(registry, (T) value);
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        return idFromValue(value);
    }

    @Override
    public JavaType typeFromId(String id) {
        Class<? extends T> subType = getType(registry, id);
        if (subType == null) {
            throw new IllegalArgumentException("No such type: '" + id + "'");
        }
        return TypeFactory.defaultInstance().constructSpecializedType(baseType, subType);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }
}
