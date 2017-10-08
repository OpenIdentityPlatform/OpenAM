/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2016 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package com.sun.identity.sm;

import com.sun.identity.security.EncodeAction;
import com.sun.identity.shared.xml.XMLUtils;

import java.security.AccessController;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;

import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implement this class to manually upgrade schema attributes.
 */
public abstract class AbstractUpgradeHelper implements UpgradeHelper {

    private static final String DEFAULT_VALUES_BEGIN = "<"
            + SMSUtils.ATTRIBUTE_DEFAULT_ELEMENT + ">";    
    private static final String DEFAULT_VALUES_END = "</"
            + SMSUtils.ATTRIBUTE_DEFAULT_ELEMENT + ">";

    private static final String EXAMPLE_VALUES_BEGIN = "<"
            + SMSUtils.ATTRIBUTE_EXAMPLE_ELEMENT + ">";
    private static final String EXAMPLE_VALUES_END = "</"
            + SMSUtils.ATTRIBUTE_EXAMPLE_ELEMENT + ">";

    private static final String IS_OPTIONAL_BEGIN = "<"
            + SMSUtils.ATTRIBUTE_OPTIONAL + ">";
    private static final String IS_OPTIONAL_END = "</"
            + SMSUtils.ATTRIBUTE_OPTIONAL + ">";

    private static final String VALUE_BEGIN = "<" + SMSUtils.ATTRIBUTE_VALUE + ">";
    private static final String VALUE_END = "</" + SMSUtils.ATTRIBUTE_VALUE + ">";


    protected final Set<String> attributes = new HashSet<String>();

    /**
     * Update the optional value of an attribute schema
     * @param attribute the attribute schema
     * @param isOptional true if this attribute is optional
     * @return the attribute schema modified
     * @throws UpgradeException If there was an error while performing the attribute upgrade.
     */
    protected AttributeSchemaImpl updateOptional(AttributeSchemaImpl attribute, boolean isOptional)
            throws UpgradeException {
        StringBuilder sb = new StringBuilder();

        if (isOptional) {
            sb.append(IS_OPTIONAL_BEGIN);
            sb.append(IS_OPTIONAL_END);
        }
        Document doc = XMLUtils.toDOMDocument(sb.toString(), null);

        Node attributeNode = updateNode(doc, SMSUtils.ATTRIBUTE_OPTIONAL, attribute.getAttributeSchemaNode());
        attribute.update(attributeNode);

        return attribute;
    }

    /**
     * Update the choice values of an attribute schema
     * @param attribute the attribute schema
     * @param choiceValues the new choice values
     * @return the attribute schema modified
     * @throws UpgradeException If there was an error while performing the attribute upgrade.
     */
    protected AttributeSchemaImpl updateChoiceValues(AttributeSchemaImpl attribute, Collection<String> choiceValues)
            throws UpgradeException {
        try {
            final Document choiceValuesDoc = XMLUtils.newDocument();
            final Element choiceValuesElement = choiceValuesDoc.createElement(SMSUtils.ATTRIBUTE_CHOICE_VALUES_ELEMENT);
            choiceValuesDoc.appendChild(choiceValuesElement);
            for (String choiceValue : choiceValues) {
                final Element choiceValueElement = choiceValuesDoc.createElement(SMSUtils
                        .ATTRIBUTE_CHOICE_VALUE_ELEMENT);
                choiceValueElement.appendChild(choiceValuesDoc.createTextNode(choiceValue));
                choiceValuesElement.appendChild(choiceValueElement);
            }

            final Node attributeNode = updateNode(choiceValuesDoc, SMSUtils.ATTRIBUTE_CHOICE_VALUES_ELEMENT,
                    attribute.getAttributeSchemaNode());
            attribute.update(attributeNode);
        } catch (ParserConfigurationException e) {
            throw new UpgradeException(e);
        }
        return attribute;
    }

    /**
     * Update the default values of an attribute schema
     * @param attribute the attribute schema
     * @param defaultValues the new default values
     * @return the attribute schema modified
     * @throws UpgradeException If there was an error while performing the attribute upgrade.
     */
    protected AttributeSchemaImpl updateDefaultValues(AttributeSchemaImpl attribute, Set<String> defaultValues)
            throws UpgradeException {
        return updateListValues(attribute, defaultValues, DEFAULT_VALUES_BEGIN, DEFAULT_VALUES_END,
                SMSUtils.ATTRIBUTE_DEFAULT_ELEMENT);
    }

    /**
     * Update the example values of an attribute schema
     * @param attribute the attribute schema
     * @param exampleValues the new examples values
     * @return the attribute schema modified
     * @throws UpgradeException If there was an error while performing the attribute upgrade.
     */
    protected AttributeSchemaImpl updateExampleValues(AttributeSchemaImpl attribute, Set<String> exampleValues)
            throws UpgradeException {
        return updateListValues(attribute, exampleValues, EXAMPLE_VALUES_BEGIN, EXAMPLE_VALUES_END,
                SMSUtils.ATTRIBUTE_EXAMPLE_ELEMENT);
    }

    private AttributeSchemaImpl updateListValues(AttributeSchemaImpl attribute, Set<String> values, String tabBegin,
             String tabEnd, String elementName) throws UpgradeException {
        StringBuilder sb = new StringBuilder(100);

        if (!values.isEmpty()) {
            sb.append(tabBegin);
            for (String value : values) {
                sb.append(VALUE_BEGIN);
                sb.append(SMSSchema.escapeSpecialCharacters(value));
                sb.append(VALUE_END);
            }
            sb.append(tabEnd);
        }
        Document doc = XMLUtils.toDOMDocument(sb.toString(), null);

        Node attributeNode = updateNode(doc, elementName, attribute.getAttributeSchemaNode());
        attribute.update(attributeNode);

        return attribute;
    }

    /**
     * Encrypts all values in the provided set.
     *
     * <p>To be used when copying default values which need to be stored encrypted.</p>
     *
     * @param values The values to encrypt.
     * @return A Set containing the encrypted values.
     */
    protected Set<String> encryptValues(Set<String> values) {
        if (values.isEmpty()) {
            return values;
        }
        Set<String> encryptedValues = new HashSet<>();
        for (String value : values) {
            encryptedValues.add(AccessController.doPrivileged(new EncodeAction(value)));
        }
        return encryptedValues;
    }

    protected static Node updateNode(Document newValueNode, String element, Node attributeSchemaNode) {

        NodeList childNodes = attributeSchemaNode.getChildNodes();
        
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);

            if (item.getNodeName().equals(element)) {
                attributeSchemaNode.removeChild(item);
            }
        }
        if (newValueNode != null) {
            Node newNode = attributeSchemaNode.getOwnerDocument().importNode(newValueNode.getFirstChild(), true);
            SMSUtils.ATTRIBUTE_SCHEMA_CHILD newSchemaName = SMSUtils.ATTRIBUTE_SCHEMA_CHILD.valueOfName(element);
            NodeList childrens = attributeSchemaNode.getChildNodes();

            boolean isNewNodeInserted = false;

            if (childrens.getLength() > 0) {
                // Insert the new node in the right position: we are looking for the first element that has a higher
                // ordinal than our's, and then we insert the node just in front of that.
                for (int i = 0; i < childrens.getLength(); i++) {
                    Node currentChild = childrens.item(i);

                    SMSUtils.ATTRIBUTE_SCHEMA_CHILD schemaName =
                            SMSUtils.ATTRIBUTE_SCHEMA_CHILD.valueOfName(currentChild.getNodeName());
                    if (schemaName != null && schemaName.compareTo(newSchemaName) > 0) {
                        attributeSchemaNode.insertBefore(newNode, currentChild);
                        isNewNodeInserted = true;
                        break;
                    }
                }
            }

            if (!isNewNodeInserted) {
                //By default, we insert the node at the end of the list. Happens when there are no other children
                //elements and/or the node to be inserted would be the last one.
                attributeSchemaNode.appendChild(newNode);
            }
        }
        return attributeSchemaNode;
    }

    public AttributeSchemaImpl addNewAttribute(Set<AttributeSchemaImpl> existingAttrs, AttributeSchemaImpl newAttr)
            throws UpgradeException {
        return newAttr;
    }

    /**
     * Implement this method to perform modifications to an existing attribute based on custom logic. In order to
     * create a hook for a certain attribute, during upgradehelper initialization the attribute name should be
     * captured in {@link AbstractUpgradeHelper#attributes}.
     *
     * @param oldAttr The attribute schema definition currently specified.
     * @param newAttr The attribute schema definition we are planning to upgrade to.
     * @return If there is nothing to upgrade (i.e. there is no real difference between old and new attribute),
     * implementations MUST return <code>null</code>, otherwise either the amended attribute or the newAttr can be
     * returned directly.
     * @throws UpgradeException If there was an error while performing the attribute upgrade.
     */
    public abstract AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl oldAttr, AttributeSchemaImpl newAttr)
    throws UpgradeException;

    /**
     * Implement this method to perform modifications to a newly added attribute. In order to create a hook for
     * a certain attribute, during upgradehelper initialization the attribute name should be captured in
     * {@link AbstractUpgradeHelper#attributes}.
     *
     * @param newAttr The attribute schema definition we are planning to upgrade to.
     * @return If there is nothing to upgrade, implementations MUST return <code>null</code>,
     * otherwise the amended attribute can be returned directly.
     * @throws UpgradeException If there was an error while performing the attribute upgrade.
     */
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl newAttr) throws UpgradeException {
        return null;
    }

    @Override
    public final Set<String> getAttributes() {
        return attributes;
    }
}
