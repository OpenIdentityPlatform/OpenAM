/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMDisplayType.java,v 1.2 2008/06/25 05:42:49 qcheng Exp $
 *
 */

package com.sun.identity.console.base.model;

import com.sun.identity.policy.ActionSchema;
import com.sun.identity.policy.Syntax;
import com.sun.identity.sm.AttributeSchema;

/* - NEED NOT LOG - */

public class AMDisplayType {
    private static AMDisplayType instance = new AMDisplayType();

    public static final int UNDEFINED_SYNTAX = -9;
    public static final int NONE_SYNTAX = -1;
    public static final int SYNTAX_TEXT = 0;
    public static final int SYNTAX_BOOLEAN = 1;
    public static final int SYNTAX_TEXTFIELD = 2;
    public static final int SYNTAX_PASSWORD = 3;
    public static final int SYNTAX_ENCRYPTED_PASSWORD = 4;
    public static final int SYNTAX_PARAGRAPH = 5;
    public static final int SYNTAX_RADIO = 6;
    public static final int SYNTAX_LINK = 7;
    public static final int SYNTAX_BUTTON = 8;
    public static final int SYNTAX_NAME_VALUE_LIST = 9;
    public static final int SYNTAX_SINGLE_CHOICE = 10;
    public static final int SYNTAX_MULTIPLE_CHOICE = 11;
    public static final int SYNTAX_LIST = 12;
    public static final int DEFAULT_SYNTAX = SYNTAX_TEXTFIELD;

    public static final int TYPE_SINGLE = 0;
    public static final int TYPE_SINGLE_CHOICE = 1;
    public static final int TYPE_MULTIPLE_CHOICE = 2;
    public static final int TYPE_LIST = 3;
    public static final int DEFAULT_TYPE = TYPE_SINGLE;

    public static AMDisplayType getInstance() {
        return instance;
    }

    private AMDisplayType() {
    }

    /**
     * Returns display type of an action schema.
     *
     * @param actionSchema Action schema
     * @return display syntax of an action schema.
     */
    public static int getDisplayType(ActionSchema actionSchema) {
        int displayType = DEFAULT_TYPE;
        AttributeSchema.Type type = actionSchema.getType();

        if (type.equals(AttributeSchema.Type.LIST)) {
            displayType = TYPE_LIST;
        } else if (type.equals(AttributeSchema.Type.SINGLE_CHOICE)) {
            displayType = TYPE_SINGLE_CHOICE;
        } else if (type.equals(AttributeSchema.Type.MULTIPLE_CHOICE)) {
            displayType =  TYPE_MULTIPLE_CHOICE;
        }

        return displayType;
    }


    /**
     * Returns display syntax of an action schema.
     *
     * @param actionSchema Action schema
     * @return display syntax of an action schema.
     */
    public static int getDisplaySyntax(ActionSchema actionSchema) {
        int displaySyntax = getDisplaySyntaxFromUIType(actionSchema);
        if (displaySyntax == UNDEFINED_SYNTAX) {
           displaySyntax = getDisplaySyntax(actionSchema.getSyntax());
        }
        return displaySyntax;
    }

    /**
     * Returns display syntax of an action schema.
     *
     * @param syntax SM Based Syntax
     * @return display syntax of an action schema.
     */
    public static int getDisplaySyntax(
        com.sun.identity.sm.AttributeSchema.Syntax syntax
    ) {
        int displaySyntax = DEFAULT_SYNTAX;

        if (syntax.equals(AttributeSchema.Syntax.BOOLEAN)) {
            displaySyntax = SYNTAX_BOOLEAN;
        } else if (syntax.equals(AttributeSchema.Syntax.PASSWORD)) {
            displaySyntax = SYNTAX_PASSWORD;
        } else if (syntax.equals(AttributeSchema.Syntax.ENCRYPTED_PASSWORD)) {
            displaySyntax = SYNTAX_ENCRYPTED_PASSWORD;
        } else if (syntax.equals(AttributeSchema.Syntax.PARAGRAPH) ||
            syntax.equals(AttributeSchema.Syntax.XML)
        ) {
            displaySyntax = SYNTAX_PARAGRAPH;
        }

        return displaySyntax;
    }

    /**
     * Returns display syntax of an action schema.
     *
     * @param syntax Policy Based Syntax
     * @return display syntax of an action schema.
     */
    public static int getDisplaySyntax(Syntax syntax) {
        int displaySyntax = NONE_SYNTAX;

        if (syntax.equals(Syntax.ANY_SEARCHABLE)) {
            //TOFIX: Editable list
        } else if (syntax.equals(Syntax.ANY)) {
            displaySyntax = SYNTAX_TEXTFIELD;
        } else if (syntax.equals(Syntax.SINGLE_CHOICE)) {
            displaySyntax = SYNTAX_SINGLE_CHOICE;
        } else if (syntax.equals(Syntax.MULTIPLE_CHOICE)) {
            displaySyntax = SYNTAX_MULTIPLE_CHOICE;
        } else if (syntax.equals(Syntax.LIST)) {
            displaySyntax = SYNTAX_LIST;
        }

        return displaySyntax;
    }

    /**
     * Gets display syntax from UI Type
     *
     * @param actionSchema - action schema
     * @return display syntax from UI Type
     */
    private static int getDisplaySyntaxFromUIType(ActionSchema actionSchema) {
        int displaySyntax = UNDEFINED_SYNTAX;
        AttributeSchema.UIType uiType = actionSchema.getUIType();

        if (uiType != null) {
            if (uiType == AttributeSchema.UIType.RADIO) {
                displaySyntax = SYNTAX_RADIO;
            } else if (uiType == AttributeSchema.UIType.LINK) {
                displaySyntax = SYNTAX_LINK;
            } else if (uiType == AttributeSchema.UIType.BUTTON) {
                displaySyntax = SYNTAX_BUTTON;
            } else if (uiType == AttributeSchema.UIType.NAME_VALUE_LIST) {
                displaySyntax = SYNTAX_NAME_VALUE_LIST;
            }
        }

        return displaySyntax;
    }
}

