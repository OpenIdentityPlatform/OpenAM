/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: DefaultValues.java,v 1.3 2008/06/25 05:44:03 qcheng Exp $
 *
 */

package com.sun.identity.sm;

import java.util.Map;
import java.util.Set;
import org.w3c.dom.Node;

/**
 * The abstract class <code>DefaultValues</code> provides a mechanism for
 * services to obtain their default values dynamically instead of being
 * statically defined in the service XML file stored in the directory.
 * <p>
 * An implementation of this class must be specified in the service
 * configuration XML file in the definition of the respective attribute schema.
 * Instead of providing the default values in the XML configuration file, the
 * class name must be specified within the XML node
 * <code>DefaultValuesClassName</code>.
 *
 * @supported.all.api
 */
public abstract class DefaultValues {

    /**
     * Abstract method that must be implemented by a class extending this
     * interface, and should return the default values for the attribute.
     * 
     * @return defaults values for the attribute as a <code>java.util.Set</code>
     */
    public abstract Set getDefaultValues();

    /**
     * Returns a Set of default values for the attribute, given the environment
     * parameters. The default implementation calls the interface
     * <code>getDefaultValues()</code> which takes no parameters. The class
     * extending this class can override this method to take advantage of the
     * additional environment parameters.
     * 
     * @return defaults values for the attribute as a <code>java.util.Set</code>
     */
    public Set getDefaultValues(Map envParams) {
        return (getDefaultValues());
    }

    /**
     * Returns the name of the attribute for which the default values will be
     * returned.
     * 
     * @return the name of attribute for which the default values are returned
     */
    public final String getAttributeName() {
        return (attributeSchema.getName());
    }

    /**
     * Returns the configured key-value pairs for the class in the service's
     * configuration file
     * 
     * @return key-value pairs configured for this class in the service schema
     *         XML file
     */
    public final Map getConfiguredKeyValues() {
        return (keyValues);
    }

    /**
     * Returns the XML <code>AttributeSchema</code> node associated with this
     * attribute
     * 
     * @return XML node of <code>AttributeSchema</code>
     */
    public final Node getAttributeSchemaNode() {
        return (parentNode);
    }

    /**
     * Set the <code>AttributeSchema</code> for which the default values are
     * being obtained
     */
    final void setAttributeSchema(AttributeSchemaImpl as) {
        attributeSchema = as;
    }

    /**
     * Sets the key-values pairs configured for this object
     */
    final void setKeyValues(Node node) {
        keyValues = CreateServiceConfig.getAttributeValuePairs(node);
    }

    /**
     * Sets the <code>AttributeSchema</code> node of the XML schema
     */
    final void setParentNode(Node n) {
        parentNode = n;
    }

    // Pointer to AttributeSchema, key-value pairs and parent node
    AttributeSchemaImpl attributeSchema;

    Map keyValues;

    Node parentNode;
}
