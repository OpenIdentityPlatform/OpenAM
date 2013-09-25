/**
 * Copyright 2013 ForgeRock AS.
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
package org.forgerock.openam.cts.utils.blob.strategies;

import javax.inject.Inject;
import com.iplanet.dpro.session.service.InternalSession;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.cts.utils.blob.BlobStrategy;
import org.forgerock.openam.cts.utils.blob.TokenBlobUtils;
import org.forgerock.openam.cts.utils.blob.TokenStrategyFailedException;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Responsible for performing a specialised JSON compression based on the
 * attribute name being stored in the JSON.
 *
 * The compression is a somewhat simple reduction of each Attribute Name
 * to its initials. This works because we know the fields within the
 * InternalSession.
 *
 * This approach is however brittle and only recommended if it will make
 * the required difference in performance.
 *
 * @author robert.wapshott@forgerock.com
 */
public class AttributeCompressionStrategy implements BlobStrategy {
    // Injected
    private final TokenBlobUtils blobUtils;

    private final BidiMap replacement = new DualHashBidiMap();

    @Inject
    public AttributeCompressionStrategy(TokenBlobUtils blobUtils) {
        this.blobUtils = blobUtils;

        for (Field f : getAllValidFields(InternalSession.class)) {
            String fieldName = f.getName();
            replacement.put(fieldName, getInitials(fieldName));
        }
    }

    /**
     * Ensures the Token is a Session Token and performs the compression.
     * @param token {@inheritDoc}
     * @throws org.forgerock.openam.cts.utils.blob.TokenStrategyFailedException {@inheritDoc}
     */
    public void perform(Token token) throws TokenStrategyFailedException {
        if (!isTokenValidForCompression(token)) {
            return;
        }

        performUpdate(token, replacement);
    }

    /**
     * Ensures the Token is a Session Token and reverses the compression.
     * @param token {@inheritDoc}
     * @throws TokenStrategyFailedException {@inheritDoc}
     */
    public void reverse(Token token) throws TokenStrategyFailedException {
        if (!isTokenValidForCompression(token)) {
            return;
        }

        performUpdate(token, replacement.inverseBidiMap());
    }

    /**
     * A simple heuristic for determining the Token is a Session Token containing JSON.
     *
     * @param token Non null Token to examine.
     * @return True if it starts and ends with curly brackets.
     */
    private boolean isTokenValidForCompression(Token token) {
        if (token.getType() != TokenType.SESSION) {
            return false;
        }

        if (token.getBlob() == null) {
            return false;
        }

        String contents = blobUtils.getBlobAsString(token);
        return contents.startsWith("{") && contents.endsWith("}");
    }

    /**
     * Update the Token with the compressed contents.
     * @param token Non null
     * @param map Non null
     * @throws TokenStrategyFailedException If any error occured.
     */
    private void performUpdate(Token token, BidiMap map) throws TokenStrategyFailedException {
        String contents = blobUtils.getBlobAsString(token);
        String replace = applyReplacement(map, contents);
        blobUtils.setBlobFromString(token, replace);
    }

    /**
     * Perform the keyword substitution.
     *
     * @param replacement A mapping of one value to another to perform.
     * @param contents String to modify.
     */
    private String applyReplacement(final BidiMap replacement, String contents) {
        for (Object key : replacement.keySet()) {
            String attributeName = (String) key;
            String attributeInitials = (String) replacement.get(key);
            contents = contents.replaceAll(
                    JSONSerialisation.jsonAttributeName(attributeName),
                    JSONSerialisation.jsonAttributeName(attributeInitials));
        }
        return contents;
    }

    /**
     * Generate the initials from the given name.
     *
     * Note: This function is intended to operate against Java field name syntax.
     * As such it simply picks out the first character and all subsequent upper
     * case characters from the String.
     *
     * @param name Non null string that follows Java field name syntax.
     * @return The initials of the field.
     */
    public static String getInitials(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name was null");
        }
        StringBuilder r = new StringBuilder();
        boolean start = true;
        for (int ii = 0; ii < name.length(); ii++) {
            char c = name.charAt(ii);
            if (start) {
                r.append(c);
                start = false;
                continue;
            }
            if (Character.isUpperCase(c)) {
                r.append(c);
            }
        }
        return r.toString();
    }

    /**
     * Examines the class using reflection for all declared fields which are suitable
     * for serialisation.
     *
     * @param c Non null class to examine.
     *
     * @return A non null but possibly empty collection of Fields.
     */
    public static Collection<Field> getAllValidFields(Class c) {
        List<Field> r = new ArrayList<Field>();
        for (Field f : c.getDeclaredFields()) {
            int modifiers = f.getModifiers();
            if (Modifier.isStatic(modifiers)) continue;
            if (Modifier.isVolatile(modifiers)) continue;
            if (Modifier.isTransient(modifiers)) continue;
            r.add(f);
        }
        return r;
    }
}
