/**
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
 * Copyright 2014-2015 ForgeRock AS.
 */
package com.sun.identity.shared.debug;

import java.security.InvalidParameterException;

/**
 * Debug level enum
 */
public enum DebugLevel {

    /**
     * flags the disabled debug state.
     */
    OFF(0, "off"),

    /**
     * flags the state where error debugging is enabled. When debugging is set
     * to less than <code>ERROR</code>, error debugging is also disabled.
     */
    ERROR(1, "error"),

    /**
     * flags the state where warning debugging is enabled, but message debugging
     * is disabled. When debugging is set to less than <code>WARNING</code>,
     * warning debugging is also disabled.
     */
    WARNING(2, "warning"),

    /**
     * This state enables debugging of messages, warnings and errors.
     */
    MESSAGE(3, "message"),

    /**
     * flags the enabled debug state for warnings, errors and messages. Printing
     * to a file is disabled. All printing is done on System.out.
     */
    ON(4, "on");


    private int level;
    private String name;

    DebugLevel(int level, String name) {
        this.level = level;
        this.name = name;
    }

    /**
     * Get Debug level from its name
     *
     * @param strName name in string
     * @return debug level associated to this name
     * @throws IllegalArgumentException if no debug names are associated to this name,
     *                                  an illegal argument exception is throw
     */
    public static DebugLevel fromName(String strName) {

        return valueOf(strName.toUpperCase());
    }

    /**
     * Get Debug level from its name
     *
     * @param strLevel level in string
     * @return debug level associated to this level
     * @throws IllegalArgumentException if no debug names are associated to this level,
     *                                  an illegal argument exception is throw
     */
    public static DebugLevel fromLevel(int strLevel) throws InvalidParameterException {

        if (OFF.level == strLevel) {
            return OFF;
        } else if (ERROR.level == strLevel) {
            return ERROR;
        } else if (WARNING.level == strLevel) {
            return WARNING;
        } else if (MESSAGE.level == strLevel) {
            return MESSAGE;
        } else if (ON.level == strLevel) {
            return ON;
        }
        throw new IllegalArgumentException("level '" + strLevel + "' isn't defined");

    }

    /**
     * Get level that can be used for level comparison
     *
     * @return level number
     */
    public int getLevel() {
        return this.level;
    }

    /**
     * Get Name associated with this debug level
     *
     * @return name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Compares the debug level with the specified debug for order. Returns a negative integer, zero,
     * or a positive integer as this level is less than, equal to, or greater than the specified object.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     * the specified object.
     */
    public int compareLevel(DebugLevel o) {
        if (this.level == o.level) {
            return 0;
        } else if (this.level < o.level) {
            return -1;
        } else {
            return 1;
        }
    }

    @Override
    public String toString() {
        return this.name;
    }
}
