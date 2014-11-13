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

package org.forgerock.openam.entitlement;

import com.sun.identity.entitlement.AndCondition;
import com.sun.identity.entitlement.AndSubject;
import com.sun.identity.entitlement.DenyOverride;
import com.sun.identity.entitlement.EntitlementCombiner;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.NoSubject;
import com.sun.identity.entitlement.NotCondition;
import com.sun.identity.entitlement.NotSubject;
import com.sun.identity.entitlement.OrCondition;
import com.sun.identity.entitlement.OrSubject;
import com.sun.identity.entitlement.ResourceAttribute;
import com.sun.identity.entitlement.StaticAttributes;
import com.sun.identity.entitlement.UserAttributes;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Provides methods for discovering and loading entitlements conditions and subject implementations. Builds upon the
 * standard Java {@link ServiceLoader} mechanism to allow additional entitlement condition and subject implementations
 * to be registered by client extensions (see {@link EntitlementModule}).
 *
 * @since 12.0.0
 * @supported.all.api
 */
public final class EntitlementRegistry {

    private final ConcurrentMap<String, Class<? extends EntitlementCondition>> conditions =
            new ConcurrentHashMap<String, Class<? extends EntitlementCondition>>();
    private final ConcurrentMap<String, Class<? extends EntitlementSubject>> subjects =
            new ConcurrentHashMap<String, Class<? extends EntitlementSubject>>();
    private final ConcurrentMap<String, Class<? extends ResourceAttribute>> attributes =
            new ConcurrentHashMap<String, Class<? extends ResourceAttribute>>();
    private final ConcurrentMap<String, Class<? extends EntitlementCombiner>> combiners =
            new ConcurrentHashMap<String, Class<? extends EntitlementCombiner>>();

    /**
     * Lazy initialisation holder for loading and caching module instances. We use lazy loading to reduce startup
     * overhead and also to ensure that classpaths are fully initialised before loading services.
     */
    private enum ServiceLoaderHolder {
        INSTANCE;
        final ServiceLoader<EntitlementModule> loader = ServiceLoader.load(EntitlementModule.class);
    }

    /**
     * Loads all available {@link EntitlementModule} instances and registers them with a new entitlement registry.
     * Each invocation of this method will attempt to load any known entitlement modules as per
     * {@link ServiceLoader#load(Class)}. Previously loaded modules will be cached but any newly available modules
     * will be loaded.
     *
     * @return an entitlement registry populated with all known entitlement modules.
     */
    public static EntitlementRegistry load() {

        EntitlementRegistry registry = new EntitlementRegistry();

        // Register standard logical condition and subject types.
        registry.registerConditionType("AND", AndCondition.class);
        registry.registerConditionType("OR", OrCondition.class);
        registry.registerConditionType("NOT", NotCondition.class);

        registry.registerSubjectType("AND", AndSubject.class);
        registry.registerSubjectType("OR", OrSubject.class);
        registry.registerSubjectType("NOT", NotSubject.class);

        /* These conditions are not tested and were removed for OpenAM 12 release.
           They might be reintroduced in a future release.

        // Standard OpenAM entitlement conditions (policy conditions will be loaded later)
        registry.registerConditionType(NumericAttributeCondition.class);
        registry.registerConditionType(AttributeLookupCondition.class);
        registry.registerConditionType(StringAttributeCondition.class);
        // Standard OpenAM subjects
        registry.registerSubjectType(AttributeSubject.class);
        */

        // Standard OpenAM subjects
        registry.registerSubjectType("NONE", NoSubject.class);

        // Standard OpenAM resource attribute types
        registry.registerAttributeType("User", UserAttributes.class);
        registry.registerAttributeType("Static", StaticAttributes.class);

        // Standard OpenAM Decision Combiners
        registry.registerDecisionCombiner(DenyOverride.class);

        ServiceLoader<ConditionTypeRegistry> conditionTypeRegistries = ServiceLoader.load(ConditionTypeRegistry.class);
        for (ConditionTypeRegistry conditionTypeRegistry : conditionTypeRegistries) {
            for (Class<? extends EntitlementCondition> condition : conditionTypeRegistry.getEnvironmentConditions()) {
                registry.registerConditionType(condition);
            }

            for (Class<? extends EntitlementSubject> condition : conditionTypeRegistry.getSubjectConditions()) {
                registry.registerSubjectType(condition);
            }
        }

        for (EntitlementModule provider : ServiceLoaderHolder.INSTANCE.loader) {
            provider.registerCustomTypes(registry);
        }

        return registry;
    }

    /**
     * Registers an entitlement condition type with the given short name (used in RESTful API calls and in the UI).
     * Note: short names must be unique across all condition types.
     *
     * @param name the short name of the condition type.
     * @param type the condition type to register.
     * @throws NameAlreadyRegisteredException if the short name is already registered.
     */
    public void registerConditionType(String name, Class<? extends EntitlementCondition> type) {
        register(name, conditions, type);
    }

    /**
     * Registers an entitlement condition type using a short name generated from the type name. The short name is
     * generated as the simple name of the class minus any {@code Condition} suffix. For example, a condition
     * type {@code org.forgerock.openam.entitlement.TestCondition} would be registered with the short name {@code Test}.
     *
     * @param type the condition type to register.
     * @throws NameAlreadyRegisteredException if the short name is already registered.
     */
    public void registerConditionType(Class<? extends EntitlementCondition> type) {
        String name = type.getSimpleName().replace("Condition", "");
        registerConditionType(name, type);
    }

    /**
     * Returns the condition type associated with the given short name, or null if no such condition is registered.
     *
     * @param name the short name of the condition type to get.
     * @return the associated condition type or null if no matching condition type is registered.
     */
    public Class<? extends EntitlementCondition> getConditionType(String name) {
        return conditions.get(name);
    }

    /**
     * Registers an entitlement combiner.
     *
     * @param type the condition type to register.
     * @throws NameAlreadyRegisteredException if the short name is already registered.
     */
    public void registerDecisionCombiner(Class<? extends EntitlementCombiner> type) {
        register(type.getSimpleName(), combiners, type);
    }

    /**
     * Registers an entitlement combiner with a given name.
     *
     * @param type the combiner type to register.
     * @throws NameAlreadyRegisteredException if the short name is already registered.
     */
    public void registerDecisionCombiner(String name, Class<? extends EntitlementCombiner> type) {
        register(name, combiners, type);
    }

    /**
     * Returns the combiner associated with the given short name.
     *
     * @param name the short name of the combiner type to get.
     * @return the associated combiner type or null if no matching combiner type is registered.
     */
    public Class<? extends EntitlementCombiner> getCombinerType(String name) {
        return combiners.get(name);
    }

    /**
     * Registers an entitlement subject type with the given short name (used in RESTful API calls and in the UI).
     * Note: short names must be unique across all subject types.
     *
     * @param name the short name of the subject type.
     * @param type the subject type to register.
     * @throws NameAlreadyRegisteredException if the short name is already registered.
     */
    public void registerSubjectType(String name, Class<? extends EntitlementSubject> type) {
        register(name, subjects, type);
    }

    /**
     * Registers an entitlement subject type using a short name generated from the type name. The short name is
     * generated as the simple name of the class minus any {@code Subject} suffix. For example, a subject
     * type {@code org.forgerock.openam.entitlement.TestSubject} would be registered with the short name {@code Test}.
     *
     * @param type the subject type to register.
     * @throws NameAlreadyRegisteredException if the short name is already registered.
     */
    public void registerSubjectType(Class<? extends EntitlementSubject> type) {
        String name = type.getSimpleName().replace("Subject", "");
        registerSubjectType(name, type);
    }

    /**
     * Returns the subject type associated with the given short name, or null if no such subject is registered.
     *
     * @param name the short name of the subject type to get.
     * @return the associated subject type or null if no matching subject type is registered.
     */
    public Class<? extends EntitlementSubject> getSubjectType(String name) {
        return subjects.get(name);
    }

    /**
     * Registers a resource attribute type with the given short name (used in RESTful API calls and in the UI).
     * Note: short names must be unique across all resource attribute types.
     *
     * @param name the short name of the attribute type.
     * @param type the attribute type to register.
     * @throws NameAlreadyRegisteredException if the short name is already registered.
     */
    public void registerAttributeType(String name, Class<? extends ResourceAttribute> type) {
        register(name, attributes, type);
    }

    /**
     * Registers a resource attribute type using a short name generated from the type name. The short name is
     * generated as the simple name of the class minus any {@code Attribute} suffix. For example, an attribute
     * type {@code org.forgerock.openam.entitlement.TestAttribute} would be registered with the short name {@code Test}.
     *
     * @param type the attribute type to register.
     * @throws NameAlreadyRegisteredException if the short name is already registered.
     */
    public void registerAttributeType(Class<? extends ResourceAttribute> type) {
        String name = type.getSimpleName().replace("Attribute", "");
        registerAttributeType(name, type);
    }

    /**
     * Returns the attribute type associated with the given short name, or null if no such attribute is registered.
     *
     * @param name the short name of the attribute type to get.
     * @return the associated attribute type or null if no matching attribute type is registered.
     */
    public Class<? extends ResourceAttribute> getAttributeType(String name) {
        return attributes.get(name);
    }

    /**
     * Returns the short name that the given condition is registered under. If the condition is registered under
     * multiple names then an arbitrary name is returned. If the condition type is not registered then null is returned.
     *
     * @param condition the condition to get a short name for.
     * @return the short type name of the given condition or null if not registered.
     */
    public String getConditionName(EntitlementCondition condition) {
        for (Map.Entry<String, Class<? extends EntitlementCondition>> candidate : conditions.entrySet()) {
            if (candidate.getValue() == condition.getClass()) {
                return candidate.getKey();
            }
        }
        return null;
    }

    /**
     * Returns the short name that the given subject is registered under. If the subject is registered under
     * multiple names then an arbitrary name is returned. If the subject type is not registered then null is returned.
     *
     * @param subject the subject to get a short name for.
     * @return the short type name of the given subject or null if not registered.
     */
    public String getSubjectName(EntitlementSubject subject) {
        for (Map.Entry<String, Class<? extends EntitlementSubject>> candidate : subjects.entrySet()) {
            if (candidate.getValue() == subject.getClass()) {
                return candidate.getKey();
            }
        }
        return null;
    }

    /**
     * Returns the short name that the given attribute is registered under. If the attribute is registered under
     * multiple names then an arbitrary name is returned. If the attribute type is not registered then null is returned.
     *
     * @param attribute the attribute to get a short name for.
     * @return the short type name of the given attribute or null if not registered.
     */
    public String getAttributeName(ResourceAttribute attribute) {
        for (Map.Entry<String, Class<? extends ResourceAttribute>> candidate : attributes.entrySet()) {
            if (candidate.getValue() == attribute.getClass()) {
                return candidate.getKey();
            }
        }
        return null;
    }

    /**
     * Registers the given type under the given short name in the given registry map. If the map already contains an
     * entry for this short name and it is not identical to the given type then an exception is thrown and the map is
     * not updated.
     *
     * @param shortName the short name to register the type under.
     * @param map the map to register the type in.
     * @param type the type to register.
     * @param <T> the type of types :-)
     * @throws NameAlreadyRegisteredException if a different type is already registered under this short name.
     */
    private <T> void register(String shortName, ConcurrentMap<String, Class<? extends T>> map,
                              Class<? extends T> type) {
        Class<? extends T> previous = map.putIfAbsent(shortName, type);
        if (previous != null && previous != type) {
            throw new NameAlreadyRegisteredException(shortName);
        }
    }

    /**
     * Returns all the short names of {@link EntitlementCondition}s currently registered in
     * this {@link EntitlementRegistry}.
     *
     * @return A set of strings containing all the unqiue EntitlementConditions registered at point of query.
     */
    public Set<String> getConditionsShortNames() {
        return conditions.keySet();
    }

    /**
     * Returns all the short names of {@link EntitlementSubject}s currently registered in
     * this {@link EntitlementRegistry}.
     *
     * @return A set of strings containing all the unqiue EntitlementSubject registered at point of query.
     */
    public Set<String> getSubjectsShortNames() {
        return subjects.keySet();
    }

    /**
     * Returns all the short names of {@link ResourceAttribute}s currently registered in
     * this {@link EntitlementRegistry}.
     *
     * @return A set of strings containing all the unqiue ResourceAttribute registered at point of query.
     */
    public Set<String> getAttributesShortNames() {
        return attributes.keySet();
    }

    /**
     * Returns all the short names of {@link EntitlementCombiner}s currently registered in
     * this {@link EntitlementRegistry}.
     *
     * @return A set of strings containing all the unqiue EntitlementCombiners registered at point of query.
     */
    public Set<String> getCombinersShortNames() {
        return combiners.keySet();
    }


}
