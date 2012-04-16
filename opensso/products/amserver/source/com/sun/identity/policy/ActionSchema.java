/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ActionSchema.java,v 1.3 2008/06/25 05:43:43 qcheng Exp $
 *
 */



package com.sun.identity.policy;

import java.util.*;

import org.w3c.dom.*;

import com.sun.identity.sm.*;
import com.sun.identity.shared.xml.XMLUtils;
import com.iplanet.ums.validation.*;


/**
 * The class <code>ActionSchema</code> provides interfaces to
 * obtain meta information about service action values.
 * For example, information about the syntax of the action values,
 * defaults values, choice values, etc.
 */
public class ActionSchema extends AttributeSchemaImpl {

    protected static final String ACTION_SCHEMA = "AttributeSchema";
    protected static final String ACTION_VALUE = "AttributeValue";
    protected static final String VALUE = "Value";
    protected static final String I18N_KEY = "i18nKey";
    protected static final String RESOURCE_NAME = "ResourceName";

    private Node actionNode = null;

    /**
     *  Constructor with the action schema node
     *  @param node <code>Node</code> representing w3c DOM representation
        of the object.
     */
    protected ActionSchema(Node node) {
        super(node);
        actionNode = node;
    }

    /**
     * Returns a <code>Set</code> of possible action values if the action 
     * schema is of choice type ie <code>SINGLE_CHOICE</code> or 
     * <code>MULTIPLE_CHOICE</code> in the service schema definition. 
     * The choice values are sorted alphabetically in the ascending order. 
     * If the action values are not of choice type, this method return an 
     * empty <code>Set</code> and not <code>null</code>.
     *
     * @return choice values for action values
     */
    public Set getActionValues() {
        return(getChoiceValuesSet());
    }

    /**
     * Returns the I18N key for the action value. This method can
     * be used only when the action schema is of type <code>SINGLE_CHOICE</code>
     * or <code>MULTIPLE_CHOICE</code>. Also each action value must have 
     * defined its <code>i18nKey</code> in the XML.
     * 
     * @return i18n key for the action value if present in the
     * service XML; <code>null</code> otherwise
     */
    public String getActionValueI18NKey(String actionValue) {
        return(getChoiceValueI18NKey(actionValue));
    }

    /**
     * Returns <code>true</code> if the action requires a resource name.
     * An action can have a resource name only if its type is either
     * <code>SINGLE_CHOICE</code> or <code>MULTIPLE_CHOICE</code>, or if 
     * its <code>syntax</code> is boolean.
     *
     * @return <code>true</code> if the action name requires a resource name;
     * <code>false</code> otherwise
     */
    public boolean requiresResourceName() {
        return(isResourceNameAllowed());
    }    

    /**
     * Returns the default resource names associated with the
     * action value. If it is not configured, it returns an
     * empty <code>Set</code>.
     *
     * @return default resource names associated with the action value
     */
    public Set getResourceNames(String actionValue) {
        // Get the child nodes
        NodeList children = actionNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            // Obtain the action value nodes
            if (node.getNodeName().equalsIgnoreCase(ACTION_VALUE)) {
                NodeList nl = node.getChildNodes();
                for (int j = 0; j < nl.getLength(); j++) {
                    Node n = nl.item(j);
                    // Check for Value node
                    if (n.getNodeName().equalsIgnoreCase(VALUE)) {
                        String value = XMLUtils.getValueOfValueNode(n);
                        // Check if it matchs actionValue
                        if (actionValue.equalsIgnoreCase(value.toString())) {
                            // Get the resource names from ActionValue node
                            HashSet answer = new HashSet();
                            NodeList rl = node.getChildNodes();
                            for (int k = 0; k < rl.getLength(); k++) {
                                Node r = rl.item(k);
                                if (r.getNodeName().equalsIgnoreCase(
                                    RESOURCE_NAME)) {
                                    answer.add(XMLUtils.getValueOfValueNode(r));
                                }
                            }
                            return (answer);
                        }
                    }
                }
            }
        }
        return (Collections.EMPTY_SET);
    }

    /**
     * Returns the I18N key for displaying resource names associated
     * with the action value. If it is not configured, it returns
     * <code>null</code>.
     *
     * @return String representing <code>i18nKey</code> for displaying 
     * resource names.
     */
    public String getResourceNameI18NKey(String actionValue) {
        // Get the child nodes
        NodeList children = actionNode.getChildNodes();
        int numNodes = children.getLength();
        for (int i = 0; i < numNodes; i++) {
            Node node = children.item(i);
            // Obtain the action value nodes
            if (node.getNodeName().equalsIgnoreCase(ACTION_VALUE)) {
                NodeList nl = node.getChildNodes();
                int numOfNodeList = nl.getLength();
                for (int j = 0; j < numOfNodeList; j++) {
                    Node n = nl.item(j);
                    // Check for Value node
                    if (n.getNodeName().equalsIgnoreCase(VALUE)) {
                        String value = XMLUtils.getValueOfValueNode(n);
                        // Check if it matchs actionValue
                        if (actionValue.equalsIgnoreCase(value)) {
                            // Get the resource names from ActionValue node
                            NodeList rl = node.getChildNodes();
                            int rlLength = rl.getLength();
                            for (int k = 0; k < rlLength; k++) {
                                Node r = rl.item(k);
                                if (r.getNodeName().equalsIgnoreCase(
                                    RESOURCE_NAME)) {
                                    // Get the i18n key attribute
                                    String i18nKey =
                                        XMLUtils.getNodeAttributeValue(
                                        r, I18N_KEY);
                                    if (i18nKey != null) {
                                        return (i18nKey);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return (null);
    }

    /**
     *  Validates the values against the action schema 
     */
    static void validate(ActionSchema as, Set values)
        throws InvalidNameException {
        if (!validateType(as, values) ||
            !validateSyntax(as, values)) {
            // throw an exception
            PolicyManager.debug.error(
                "In validate action name: invalid values");
            throw (new InvalidNameException(ResBundleUtils.rbName,
                "invalid_action_value", null, as.getName(),
                PolicyException.POLICY));
        }
    }
        
    /**
     * Checks the validity of action values against the action 's
     * type as defined in the action schema.
     */

    static boolean validateType(ActionSchema as, Set values) {
        if ((values == null) || values.isEmpty()) {
            // It is OK to have no values set for an action
            return (true);
        }

        // Get the type
        AttributeSchema.Type type = as.getType();
        boolean checkType = false;

        // Check for single values
        if (type.equals(AttributeSchema.Type.SINGLE) ||
            type.equals(AttributeSchema.Type.SINGLE_CHOICE)) {
            checkType = true;
            if (values.size() > 1) {
                return (false);
            }
        }

        // Check for choice values
        if (type.equals(AttributeSchema.Type.SINGLE_CHOICE) ||
            type.equals(AttributeSchema.Type.MULTIPLE_CHOICE)) {
            checkType = true;
            // Get possible choice values and check them
            Set validValues = as.getActionValues();
            Iterator givenValues = values.iterator();
            while (givenValues.hasNext()) {
                if (!validValues.contains(givenValues.next())) {
                    return (false);
                }
            }
        }

        // Check the type, other types SINGLE & CHOICE have been checked
        if (!checkType && !type.equals(AttributeSchema.Type.LIST)) {
            return (false);
        }
        return (true);
    }

    /**
     * Checks the validity of action values against the action 's
     * syntax as defined in the action schema.
     */
    static boolean validateSyntax(ActionSchema as, Set values) {
            AttributeSchema.Syntax syntax = as.getSyntax();
        // Check for String syntax
        boolean answer = false;
        if (syntax.equals(AttributeSchema.Syntax.STRING) ||
            syntax.equals(AttributeSchema.Syntax.PASSWORD)) {
            // Anything is allowed
            answer = true;
        }

        if (syntax.equals(AttributeSchema.Syntax.BOOLEAN)) {
            if (values.size() == 1) {
                Iterator it = values.iterator();
                while (it.hasNext()) {
                    String test = (String) it.next();
                    if ( as.getTrueValue().equals(test)  ||
                             as.getFalseValue().equals(test) ) {
                        answer = true;
                    }
                }
            }
        }

        if (syntax.equals(AttributeSchema.Syntax.EMAIL)) {
            Iterator it = values.iterator();
            while (it.hasNext()) {
                answer = true;
                if (!mailValidator.validate((String) it.next())) {
                    answer = false;
                    break;
                }
            }
        }

        if (syntax.equals(AttributeSchema.Syntax.URL)) {
            Iterator it = values.iterator();
            while (it.hasNext()) {
                answer = true;
                if (!urlValidator.validate((String) it.next())) {
                    answer = false;
                    break;
                }
            }
        }

        if (syntax.equals(AttributeSchema.Syntax.NUMERIC) || 
            syntax.equals(AttributeSchema.Syntax.NUMBER)) {
            Iterator it = values.iterator();
            while (it.hasNext()) {
                answer = true;
                if (!numberValidator.validate((String) it.next())) {
                    answer = false;
                    break;
                }
            }
        }

        if (syntax.equals(AttributeSchema.Syntax.PERCENT) ||
           syntax.equals(AttributeSchema.Syntax.DECIMAL_NUMBER)) {
            Iterator it = values.iterator();
            while (it.hasNext()) {
                answer = true;
                if (!floatValidator.validate((String) it.next())) {
                    answer = false;
                    break;
                }
            }
        }

        if (syntax.equals(AttributeSchema.Syntax.NUMBER_RANGE)) {
            Iterator it = values.iterator();
            while (it.hasNext()) {
                answer = true;
                String s = (String) it.next();
                Integer i = Integer.valueOf(s);
                Integer start = Integer.valueOf(as.getStartRange());
                Integer end = Integer.valueOf(as.getEndRange());
                
                if ((i == null) || (start == null) || (end == null)) {
                    answer = false;
                    break;
                }
                if (!(i.intValue() >= start.intValue()) ||
                    !(i.intValue() <= end.intValue())) {
                    answer = false;
                    break;
                }
            }
        }

        if (syntax.equals(AttributeSchema.Syntax.DN)) {
            Iterator it = values.iterator();
            while (it.hasNext()) {
                answer = true;
                if (!dnValidator.validate((String) it.next())) {
                    answer = false;
                    break;
                }
            }
        }
        return (answer);
    }

    // Validators from UMS service
    static final MailAddressValidator mailValidator = 
        new MailAddressValidator();
    static final BooleanValidator boolValidator = new BooleanValidator();
    static final NumberValidator numberValidator = new NumberValidator();
    static final URLValidator urlValidator = new URLValidator();
    static final FloatValidator floatValidator = new FloatValidator();
    static final DNValidator dnValidator = new DNValidator();
}
