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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 * Portions Copyrighted 2025 3A Systems LLC.
 */
package org.forgerock.openam.core.guice;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Provider;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.iplanet.dpro.session.SessionID;

/**
 * CTS Jackson Object Mapper.
 * <p>
 * Use a static singleton as per <a href="http://wiki.fasterxml.com/JacksonBestPracticesPerformance">performance
 * best practice.</a>
 */
public class CTSObjectMapperProvider implements Provider<ObjectMapper> {
    @Override
    public ObjectMapper get() {
        ObjectMapper mapper = new ObjectMapper()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS, true);

        /**
         * @see http://stackoverflow.com/questions/7105745/how-to-specify-jackson-to-only-use-fields-preferably-globally
         */
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        SimpleModule customModule = new SimpleModule("openam", Version.unknownVersion());
        customModule.addKeyDeserializer(SessionID.class, new SessionIDKeyDeserialiser());
        mapper.registerModule(customModule);
        mapper.addHandler(new CompatibilityProblemHandler());
        return mapper;
    }

    /**
     * This simple {@link KeyDeserializer} implementation allows us to use the {@link SessionID#toString()} value as a
     * map key instead of a whole {@link SessionID} object. During deserialization this class will reconstruct the
     * original SessionID object from the session ID string.
     */
    private static class SessionIDKeyDeserialiser extends KeyDeserializer {

        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
            return new SessionID(key);
        }
    }

    /**
     * This extension allows us to ignore now unmapped fields within InternalSession and its sub-objects.
     *
     * Each field ignored is now calculated dynamically. See field JavaDoc for detail on why the field
     * is ignored and how it is generated.
     */
    private static class CompatibilityProblemHandler extends DeserializationProblemHandler {

        /**
         * InternalSession#restrictedTokensByRestriction, this legacy field is now calculated based on the
         * restrictedTokensBySid map.
         */
        private static final String RESTRICTED_TOKENS_BY_RESTRICTION = "restrictedTokensByRestriction";

        /**
         * SessionID#isParsed, is no longer persisted because of the dynamic nature of server/site configuration
         * it is now not safe to assume that a persisted SessionID has valid S1/SI values.
         */
        private static final String IS_PARSED = "isParsed";
        /**
         * SessionID#extensionPart, is not stored because it is extracted from the encryptedString.
         */
        private static final String EXTENSION_PART = "extensionPart";

        /**
         * SessionID#extensions, is not stored because it is calculated as part of parsing a SessionID.
         */
        private static final String EXTENSIONS = "extensions";

        /**
         * SessionID#tail, is not stored because it is calculated as part of parsing a SessionID.
         */
        private static final String TAIL = "tail";

        private static final Set<String> skipList = new HashSet<>(
                Arrays.asList(RESTRICTED_TOKENS_BY_RESTRICTION, IS_PARSED,
                        EXTENSION_PART, EXTENSIONS, TAIL));

        @Override
        public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser jp,
                JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException {
            if (skipList.contains(propertyName)) {
                ctxt.getParser().skipChildren();
                return true;
            }
            return false;
        }
    }
}
