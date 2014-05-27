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

import org.w3c.dom.Element;

/**
 * Defines the concerns of XML Marshalling for a particular type. Driving the REST-STS with the CXF-STS engine means that
 * tokens are ultimately represented as XML Elements, in keeping with SOAP. This interface defines the ability to
 * marshal validated or produced tokens to and from XML.
 */
public interface XmlMarshaller<T> {
    /**
     *
     * @param element The XML representation of token T
     * @return the token instance of type T represented by element
     * @throws TokenMarshalException if the marshalling cannot be performed
     */
    T fromXml(Element element) throws TokenMarshalException;

    /**
     *
     * @param instance the Token instance
     * @return The XML representation of token T
     * @throws TokenMarshalException if the marshalling cannot be performed
     */
    Element toXml(T instance) throws TokenMarshalException;
}
