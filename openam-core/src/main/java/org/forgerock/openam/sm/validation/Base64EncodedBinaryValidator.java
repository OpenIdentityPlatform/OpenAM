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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.sm.validation;

import java.util.Set;

import jakarta.xml.bind.DatatypeConverter;

import org.forgerock.openam.utils.CollectionUtils;

import com.sun.identity.sm.ServiceAttributeValidator;

/**
 * Validates that an attribute is set to a single base64-encoded
 */
public class Base64EncodedBinaryValidator implements ServiceAttributeValidator {
    private static final int _128_BITS_IN_BYTES = 128 / 8;

    @Override
    public boolean validate(final Set<String> values) {
        if (values == null || values.size() != 1) {
            return false;
        }

        try {
            // Use DatatypeConverter as it actually validates that the argument is base64-encoded
            final byte[] decoded = DatatypeConverter.parseBase64Binary(CollectionUtils.getFirstItem(values));
            return decoded != null && decoded.length >= getMinimumSize();
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            // Value is not base64-encoded
            return false;
        }
    }

    /**
     * Determines the minimum size of the base64-decoded binary value, in bytes. Defaults to 16 bytes (128 bits).
     *
     * @return the minimum required size of the binary data in bytes.
     */
    protected int getMinimumSize() {
        return _128_BITS_IN_BYTES;
    }
}
