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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts;

import org.forgerock.json.fluent.JsonValue;

/**
 * Defines the concerns of JsonMarshalling for a particular type. The T is a token type, produced by the CXF-STS engine.
 * Driving the REST-STS via the CXF-STS means that tokens produced by the CXF-STS need to be returned in json format, and
 * that token invocations need to be marshaled from json format.
 */
public interface JsonMarshaller<T> {
    /**
     *
     * @param jsonValue the json representation of token instance of type T
     * @return the token instance of type T
     * @throws TokenMarshalException if the marshalling could not be performed.
     */
    T fromJson(JsonValue jsonValue) throws TokenMarshalException;

    /**
     *
     * @param instance the token instance of type T
     * @return the json representation of the token instance
     * @throws TokenMarshalException if the marshalling could not be performed.
     */
    JsonValue toJson(T instance) throws TokenMarshalException;
}
