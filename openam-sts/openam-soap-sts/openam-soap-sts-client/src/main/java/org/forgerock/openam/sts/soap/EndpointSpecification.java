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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap;

import org.forgerock.openam.sts.AMSTSConstants;

import javax.xml.namespace.QName;

/**
 * Encapsulates the service and port specification of a single sts instance, with a convenience method (getStandardEndpointSpecification)
 * to target the standard set of sts instances published by OpenAM.
 */
public class EndpointSpecification {
    final QName serviceQName;
    final QName portQName;

    public EndpointSpecification(QName portQName, QName serviceQName) {
        this.serviceQName = serviceQName;
        this.portQName = portQName;
    }

    public static EndpointSpecification getStandardEndpointSpecification() {
        return new EndpointSpecification(
                AMSTSConstants.STANDARD_STS_PORT_QNAME,
                AMSTSConstants.STANDARD_STS_SERVICE_NAME);
    }

    public QName getServiceQName() {
        return serviceQName;
    }

    public QName getPortQName() {
        return portQName;
    }

    @Override
    public boolean equals(Object other) {
        if (this ==  other) {
            return true;
        }
        if (other instanceof EndpointSpecification) {
            EndpointSpecification otherSpec = (EndpointSpecification)other;
            return serviceQName.equals(otherSpec.serviceQName) && portQName.equals(otherSpec.portQName);
        }
        return false;
    }

    @Override
    public String toString() {
        return new StringBuilder("service name: ").append(serviceQName).append("; port name: ")
                .append(portQName).toString();
    }
}
