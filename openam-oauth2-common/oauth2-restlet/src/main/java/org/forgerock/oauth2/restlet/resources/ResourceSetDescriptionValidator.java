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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.oauth2.restlet.resources;

import static org.forgerock.json.fluent.JsonValue.json;

import javax.inject.Singleton;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.exceptions.BadRequestException;

/**
 * Validates that the resource set description is valid.
 *
 * @since 13.0.0
 */
@Singleton
public class ResourceSetDescriptionValidator {

    /**
     * Validates that the resource set description is valid.
     *
     * @param resourceSetDescription The resource set description to validate.
     * @return The same resource set description.
     * @throws BadRequestException If any part of the resource set description is not valid.
     */
    public Map<String, Object> validate(Map<String, Object> resourceSetDescription) throws BadRequestException {

        JsonValue description = json(resourceSetDescription);

        validateName(description);
        validateUri(description);
        validateType(description);
        validateScopes(description);
        validateIconUri(description);

        return resourceSetDescription;
    }

    private void validateName(JsonValue description) throws BadRequestException {
        try {
            description.get(OAuth2Constants.ResourceSets.NAME).required();
        } catch (JsonValueException e) {
            throw new BadRequestException("Invalid Resource Set Description. Missing required attribute, 'name'.");
        }
        if (!description.get(OAuth2Constants.ResourceSets.NAME).isString()) {
            throw new BadRequestException("Invalid Resource Set Description. Required attribute, 'name', must be a "
                    + "String.");
        }
    }

    private void validateUri(JsonValue description) throws BadRequestException {
        if (!description.get(OAuth2Constants.ResourceSets.URI).isNull()) {
            try {
                description.get(OAuth2Constants.ResourceSets.URI).asURI();
            } catch (JsonValueException e) {
                throw new BadRequestException("Invalid Resource Set Description. Attribute, 'uri', must be a valid "
                        + "URI.");
            }
        }
    }

    private void validateType(JsonValue description) throws BadRequestException {
        if (!description.get(OAuth2Constants.ResourceSets.TYPE).isNull()) {
            if (!description.get(OAuth2Constants.ResourceSets.TYPE).isString()) {
                throw new BadRequestException("Invalid Resource Set Description. Attribute, 'type', must be a String.");
            }
        }
    }

    private void validateScopes(JsonValue description) throws BadRequestException {
        try {
            description.get(OAuth2Constants.ResourceSets.SCOPES).required();
        } catch (JsonValueException e) {
            throw new BadRequestException("Invalid Resource Set Description. Missing required attribute, 'scopes'.");
        }
        try {
            description.get(OAuth2Constants.ResourceSets.SCOPES).asSet(String.class);
        } catch (JsonValueException e) {
            throw new BadRequestException("Invalid Resource Set Description. Required attribute, 'scopes', must be an "
                    + "array of Strings.");
        }
    }

    private void validateIconUri(JsonValue description) throws BadRequestException {
        if (!description.get(OAuth2Constants.ResourceSets.ICON_URI).isNull()) {
            try {
                description.get(OAuth2Constants.ResourceSets.ICON_URI).asURI();
            } catch (JsonValueException e) {
                throw new BadRequestException("Invalid Resource Set Description. Attribute, 'icon_uri', must be a "
                        + "valid URI.");
            }
        }
    }
}
