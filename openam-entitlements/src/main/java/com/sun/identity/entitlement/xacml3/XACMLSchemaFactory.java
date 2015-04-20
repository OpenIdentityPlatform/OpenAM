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
 * Copyright 2014 ForgeRock AS.
 */
package com.sun.identity.entitlement.xacml3;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.ResourceAttribute;
import com.sun.identity.entitlement.xacml3.core.AdviceExpression;
import com.sun.identity.entitlement.xacml3.core.AdviceExpressions;
import com.sun.identity.entitlement.xacml3.core.AttributeAssignmentExpression;
import com.sun.identity.entitlement.xacml3.core.AttributeValue;
import com.sun.identity.entitlement.xacml3.core.EffectType;
import com.sun.identity.entitlement.xacml3.core.ObjectFactory;

import javax.xml.bind.JAXBElement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Trying to add some sanity to the JAXB Generated code. A collection of factory methods
 * that will take in a value and return the thing you actually wanted.
 */
public class XACMLSchemaFactory {

    private final ObjectFactory factory;
    private final ResourceAttributeUtil resourceAttributeUtil;

    public XACMLSchemaFactory() {
        factory = new ObjectFactory();
        resourceAttributeUtil = new ResourceAttributeUtil();
    }

    /**
     * Convert a set of {@link com.sun.identity.entitlement.ResourceAttribute} objects to a single
     * {@link com.sun.identity.entitlement.xacml3.core.AdviceExpression} object.
     * @param resourceAttributes The set of ResourceAttribute objects.
     * @return The AdviceExpression object.
     */
    public AdviceExpressions resourceAttributesToAdviceExpressions(Set<ResourceAttribute> resourceAttributes)
            throws EntitlementException {
        AdviceExpressions result = new AdviceExpressions();

        if (resourceAttributes != null && !resourceAttributes.isEmpty()) {
            List<AdviceExpression> adviceExpressionList = result.getAdviceExpression();

            for (ResourceAttribute resourceAttribute : resourceAttributes) {
                AdviceExpression adviceExpression = resourceAttributeToAdviceExpression(resourceAttribute);
                adviceExpressionList.add(adviceExpression);
            }
        }
        return result;
    }

    /**
     * Convert one {@link com.sun.identity.entitlement.ResourceAttribute} object into an
     * {@link com.sun.identity.entitlement.xacml3.core.AdviceExpression} object.
     *
     * @param resourceAttribute The resource attribute
     * @return the advice expression
     * @throws com.sun.identity.entitlement.EntitlementException on JSON conversion errors
     */
    public AdviceExpression resourceAttributeToAdviceExpression(ResourceAttribute resourceAttribute)
            throws EntitlementException {

        // A pseudo-urn to use for advice/attribute id
        final String adviceId = XACMLConstants.JSON_RESOURCE_ATTRIBUTE_ADVICE_ID + ":" + resourceAttribute.getClass()
                .getName();

        AdviceExpression result = new AdviceExpression();

        AttributeValue attributeValue = factory.createAttributeValue();
        attributeValue.setDataType(XACMLConstants.XS_STRING);

        // We bypass much of the grief of conversion by getting JSON to do the heavy lifting for us.
        attributeValue.getContent().add(resourceAttributeUtil.toJSON(resourceAttribute));
        JAXBElement<AttributeValue> jaxbElement = factory.createAttributeValue(attributeValue);

        AttributeAssignmentExpression attributeAssignmentExpression = factory.createAttributeAssignmentExpression();
        attributeAssignmentExpression.setExpression(jaxbElement);
        attributeAssignmentExpression.setAttributeId(adviceId + ":" + resourceAttribute.getPropertyName());
        result.getAttributeAssignmentExpression().add(attributeAssignmentExpression);

        // Resource Attributes are returned on successful policy decisions
        result.setAppliesTo(EffectType.PERMIT);

        // Set an AdviceId to be in strict compliance with the schema
        result.setAdviceId(adviceId);

        return result;
    }

    /**
     * Convert the specified {@link com.sun.identity.entitlement.xacml3.core.AdviceExpressions} object into a set of
     * {@link com.sun.identity.entitlement.ResourceAttribute} objects.
     *
     * @param adviceExpressions The advice expressions to convert
     * @return Set of Resource Attribute objects.
     * @throws com.sun.identity.entitlement.EntitlementException if JSON exceptions occur
     */
    public Set<ResourceAttribute> adviceExpressionsToResourceAttributes(AdviceExpressions adviceExpressions)
            throws EntitlementException {

        Set<ResourceAttribute> result = new HashSet<ResourceAttribute>();

        if (adviceExpressions != null) {
            for (AdviceExpression adviceExpression : adviceExpressions.getAdviceExpression()) {
                ResourceAttribute ra = adviceExpressionToResourceAttribute(adviceExpression);
                if (ra != null) {
                    result.add(ra);
                }
            }
        }
        return result;
    }

    /**
     * Convert the specified {@link com.sun.identity.entitlement.xacml3.core.AdviceExpression} object into a
     * {@link com.sun.identity.entitlement.ResourceAttribute}.
     *
     * @param adviceExpression The specified advice expression
     * @return The resource attribute
     * @throws com.sun.identity.entitlement.EntitlementException if JSON exceptions occur
     */
    public ResourceAttribute adviceExpressionToResourceAttribute(AdviceExpression adviceExpression)
            throws EntitlementException {

        for (AttributeAssignmentExpression attributeAssignmentExpression
                                                            : adviceExpression.getAttributeAssignmentExpression()) {
            JAXBElement<?> jaxbElement = attributeAssignmentExpression.getExpression();
            Object value = jaxbElement.getValue();
            if (value instanceof AttributeValue) {
                AttributeValue attributeValue = (AttributeValue) value;
                for (Object content : attributeValue.getContent()) {
                    if (content instanceof String) {
                        return resourceAttributeUtil.fromJSON((String) content);
                    }
                }
            }
        }
        return null;
    }
}
