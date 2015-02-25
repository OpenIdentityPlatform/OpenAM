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
import com.sun.identity.entitlement.StaticAttributes;
import com.sun.identity.entitlement.opensso.PolicyResponseProvider;
import com.sun.identity.entitlement.xacml3.core.AdviceExpression;
import com.sun.identity.entitlement.xacml3.core.AdviceExpressions;
import com.sun.identity.entitlement.xacml3.core.AttributeAssignmentExpression;
import com.sun.identity.entitlement.xacml3.core.AttributeValue;
import com.sun.identity.entitlement.xacml3.core.EffectType;
import org.forgerock.openam.utils.CollectionUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Unit tests for {@link com.sun.identity.entitlement.xacml3.XACMLSchemaFactory}.
 *
 * @since 12.0.0
 */
public class XACMLSchemaFactoryTest {

    private Set<PolicyResponseProvider> rpSet;
    private AdviceExpressions aes;

    private ResourceAttributeUtil resourceAttributeUtil;
    private XACMLSchemaFactory xacmlSchemaFactory;

    @BeforeMethod
    public void setup() throws EntitlementException {

        xacmlSchemaFactory = new XACMLSchemaFactory();
        resourceAttributeUtil = new ResourceAttributeUtil();

        PolicyResponseProvider rp1 = createPolicyResponseProvider(1);
        PolicyResponseProvider rp2 = createPolicyResponseProvider(2);
        PolicyResponseProvider rp3 = createPolicyResponseProvider(3);
        PolicyResponseProvider rp4 = createPolicyResponseProvider(4);

        rpSet = CollectionUtils.asSet(rp1, rp2, rp3, rp4);

        AdviceExpression ae1 = createAdviceExpression(rp1);
        AdviceExpression ae2 = createAdviceExpression(rp2);
        AdviceExpression ae3 = createAdviceExpression(rp3);
        AdviceExpression ae4 = createAdviceExpression(rp4);

        aes = new AdviceExpressions();
        aes.getAdviceExpression().addAll(Arrays.asList(ae1, ae2, ae3, ae4));
    }

    @Test
    public void shouldConvertResourceAttributesToAndFrom() throws EntitlementException {

        for (ResourceAttribute ra : rpSet) {
            // When...
            AdviceExpression ae = xacmlSchemaFactory.resourceAttributeToAdviceExpression(ra);
            ResourceAttribute transformed = xacmlSchemaFactory.adviceExpressionToResourceAttribute(ae);

            // Then...
            assertEqualResourceAttributeValues(ra, transformed);
        }
    }

    @Test
    public void shouldConvertAdviceExpressionToAndFrom() throws EntitlementException{

        for (AdviceExpression ae : aes.getAdviceExpression()) {
            // When...
            ResourceAttribute ra = xacmlSchemaFactory.adviceExpressionToResourceAttribute(ae);
            AdviceExpression transformed = xacmlSchemaFactory.resourceAttributeToAdviceExpression(ra);

            // Then...
            assertEqualAdviceExpressionValues(ae, transformed);
        }
    }

    @Test
    public void shouldConvertAdviceExpressionsToAndFrom() throws EntitlementException {

        // When...
        Set<ResourceAttribute> raSet = xacmlSchemaFactory.adviceExpressionsToResourceAttributes(aes);
        AdviceExpressions expressions = xacmlSchemaFactory.resourceAttributesToAdviceExpressions(raSet);

        // Then...
        assertEqualAdviceExpressions(aes, expressions);
    }

    @Test
    public void shouldEnsureAdviceIsForPermitEffect() throws Exception {
        // Given
        StaticAttributes testAttribute = new StaticAttributes();

        // When
        AdviceExpression result = xacmlSchemaFactory.resourceAttributeToAdviceExpression(testAttribute);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAppliesTo()).isEqualTo(EffectType.PERMIT);
    }

    /**
     * Create a PolicyResponseProvider object
     * @param i Value to stick on the end of various strings
     * @return the created PolicyResponseProvider object
     */
    private PolicyResponseProvider createPolicyResponseProvider(int i) {
        PolicyResponseProvider result = new PolicyResponseProvider();
        result.setPResponseProviderName("responseProvider" + i);
        result.setPropertyName("propertyName" + i);

        Set<String> values = new HashSet<String>();
        for (int j = 0; j < 5 * i; j++) {
            values.add("value" + j);
        }
        result.setPropertyValues(values);
        return result;
    }

    /**
     * Create an advice expression using the values in the specified resource attribute
     * @param ra the specified resource attribute
     * @return an advice expression
     * @throws EntitlementException if there are JSON errors
     */
    private AdviceExpression createAdviceExpression(final ResourceAttribute ra) throws EntitlementException {
        AdviceExpression result = new AdviceExpression();

        AttributeValue attributeValue = new AttributeValue();
        attributeValue.setDataType(XACMLConstants.XS_STRING);

        // We bypass much of the grief of conversion by getting JSON to do the heavy lifting for us.
        attributeValue.getContent().add(resourceAttributeUtil.toJSON(ra));
        JAXBElement<AttributeValue> jaxbElement = new JAXBElement<AttributeValue>(
                QName.valueOf(AttributeValue.class.getSimpleName()), AttributeValue.class, null, attributeValue);

        AttributeAssignmentExpression attributeAssignmentExpression = new AttributeAssignmentExpression();
        attributeAssignmentExpression.setExpression(jaxbElement);
        attributeAssignmentExpression.setAttributeId(XACMLConstants.JSON_RESOURCE_ATTRIBUTE_ADVICE_ID + ":" + ra
                .getClass().getName() + ":" + ra.getPropertyName());
        result.getAttributeAssignmentExpression().add(attributeAssignmentExpression);
        result.setAppliesTo(EffectType.PERMIT);

        result.setAdviceId(XACMLConstants.JSON_RESOURCE_ATTRIBUTE_ADVICE_ID + ":" + ra.getClass().getName());

        return result;
    }

    /**
     * Assert the specified resource attribute objects contain the same values.
     * @param ra1 the first specified resource attribute object
     * @param ra2 the second specified resource attribute object
     */
    private void assertEqualResourceAttributeValues(final ResourceAttribute ra1, final ResourceAttribute ra2) {
        assertThat(checkEqualResourceAttributes(ra1, ra2)).isTrue();
    }

    /**
     * Check the specified resource attribute objects contain the same values, returning true if so, false otherwise.
     * @param ra1 the first specified resource attribute object
     * @param ra2 the second specified resource attribute object
     */
    private boolean checkEqualResourceAttributes(final ResourceAttribute ra1, final ResourceAttribute ra2) {
        if (ra1 == ra2) {
            return true;
        }
        if (ra1 == null && ra2 == null) {
            return true;
        }
        if (ra1 == null || ra2 == null) {
            return false;
        }
        return compareStrings(ra1.getPResponseProviderName(), ra2.getPResponseProviderName())
                && compareStrings(ra1.getPropertyName(), ra2.getPropertyName())
                && compareSetsOfStrings(ra1.getPropertyValues(), ra2.getPropertyValues());
    }

    /**
     * Assert that two advice expression objects contain the same values.
     * @param ae1 the first advice expression object
     * @param ae2 the second advice expression object
     */
    private void assertEqualAdviceExpressionValues(final AdviceExpression ae1, final AdviceExpression ae2) {
        assertThat(checkEqualAdviceExpressionValues(ae1, ae2)).isTrue();
    }

    /**
     * Check that two advice expression objects contain the same values, return true if so, false otherwise.
     * @param ae1 the first advice expression object
     * @param ae2 the second advice expression object
     * @return true if the advice expression objects contain the same values, false otherwise.
     */
    private boolean checkEqualAdviceExpressionValues(final AdviceExpression ae1, final AdviceExpression ae2) {
        if (ae1 == ae2) {
            return true;
        }
        if (ae1 == null || ae2 == null) {
            return false;
        }
        if (!compareStrings(ae1.getAdviceId(), ae2.getAdviceId())) {
            return false;
        }
        EffectType effectType1 = ae1.getAppliesTo();
        EffectType effectType2 = ae2.getAppliesTo();

        if (effectType1 == null && effectType2 != null || effectType1 != null && effectType2 == null) {
            return false;
        }
        if (effectType1 != null && effectType2 != null && !effectType1.equals(effectType2)) {
            return false;
        }
        return (compareListsOfAttributeAssignmentExpression(ae1.getAttributeAssignmentExpression(),
                ae2.getAttributeAssignmentExpression()));
    }

    /**
     * Compare two sets of strings, returning true if they hold the same values, false otherwise
     * @param one The first set of strings
     * @param two The second set of strings
     * @return true if both sets hold the same values, false otherwise.
     */
    private boolean compareSetsOfStrings(Set<String> one, Set<String> two) {
        if (one == two) {
            return true;
        }
        if (one == null && two == null) {
            return true;
        }
        if (one == null || two == null) {
            return false;
        }
        if (one.size() != two.size()) {
            return false;
        }
        for (String oneString : one) {
            if (!two.contains(oneString)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compare two strings, either or both of which may be null.
     * @param one First string
     * @param two Second string
     * @return true if the strings are the same, false otherwise.
     */
    private boolean compareStrings(String one, String two) {
        if (one == null && two == null) {
            return true;
        }
        if (one == null || two == null) {
            return false;
        }
        return one.equals(two);
    }

    /**
     * Compare two lists of AttributeAssignmentExpression objects.  If the lists contain the same values, return
     * true, otherwise return false.
     * @param list1 The first list.
     * @param list2 The second list.
     * @return true if the lists contain the same values, false otherwise.
     */
    private boolean compareListsOfAttributeAssignmentExpression(List<AttributeAssignmentExpression> list1,
                                                                List<AttributeAssignmentExpression> list2) {

        if (list1.size() != list2.size()) {
            return false;
        }
        for (AttributeAssignmentExpression aae : list1) {
            boolean found = false;
            for (AttributeAssignmentExpression otherAae : list2) {
                if (checkEqualAttributeAssignmentExpressions(aae, otherAae)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param first First attribute assignment expression to check
     * @param second Second attribute assignment expression to check
     * @return true if both attribute assignment expressions contain the same values, false otherwise
     */
    private boolean checkEqualAttributeAssignmentExpressions(AttributeAssignmentExpression first,
                                                             AttributeAssignmentExpression second) {
        if (first == null && second == null) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        JAXBElement<?> firstJaxbElement = first.getExpression();
        JAXBElement<?> secondJaxbElement = second.getExpression();
        Object firstObject = firstJaxbElement.getValue();
        Object secondObject = secondJaxbElement.getValue();

        if (firstObject == null && secondObject == null) {
            return true;
        }
        if (firstObject == null || secondObject == null) {
            return false;
        }
        if (!(firstObject instanceof AttributeValue) || !(secondObject instanceof AttributeValue)) {
            return false;
        }
        AttributeValue firstAttributeValue = (AttributeValue) firstObject;
        AttributeValue secondAttributeValue = (AttributeValue) secondObject;
        return checkEqualAttributeValues(firstAttributeValue, secondAttributeValue);
    }

    /**
     * Compare two AttributeValue objects, returning true if they contain equal values, false otherwise.
     * @param first The first attribute value.
     * @param second The second attribute value.
     * @return true if the objects contain the same values, false otherwise.
     */
    private boolean checkEqualAttributeValues(AttributeValue first, AttributeValue second) {
        if (first == null && second == null) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        List<Object> firstList = first.getContent();
        List<Object> secondList = second.getContent();
        if (firstList == null && secondList == null) {
            return true;
        }
        if (firstList == null || secondList == null) {
            return false;
        }
        if (firstList.size() != secondList.size()) {
            return false;
        }
        for (Object firstObject : firstList) {
            boolean found = false;
            for (Object secondObject : secondList) {
                if (firstObject instanceof String && secondObject instanceof String) {
                    String firstString = (String) firstObject;
                    String secondString = (String) secondObject;

                    try {
                        ResourceAttribute firstRA = resourceAttributeUtil.fromJSON(firstString);
                        ResourceAttribute secondRA = resourceAttributeUtil.fromJSON(secondString);

                        found = checkEqualResourceAttributes(firstRA, secondRA);
                        if (found) {
                            break;
                        }
                    } catch (EntitlementException ignored) {
                    }
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * Assert that two advice expressions objects contain the same values, i.e. that the advice expression objects
     * they contain, contain the same values.
     * @param aes1 the first advice expressions object
     * @param aes2 the second advice expressions object
     */
    private void assertEqualAdviceExpressions(final AdviceExpressions aes1, final AdviceExpressions aes2) {

        assertThat(aes1.getAdviceExpression().size()).isEqualTo(aes2.getAdviceExpression().size());

        for (AdviceExpression ae : aes1.getAdviceExpression()) {
            boolean found = false;
            for (AdviceExpression other : aes2.getAdviceExpression()) {
                if (checkEqualAdviceExpressionValues(ae, other)) {
                    found = true;
                    break;
                }
            }
            assertThat(found).isTrue();
        }
    }
}
