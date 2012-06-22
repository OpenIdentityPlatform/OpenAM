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
 * $Id: RandomPasswordGenerator.java,v 1.2 2008/06/25 05:43:41 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.password.plugins;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.password.ui.model.PWResetException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>RandomPasswordGenerator</code> defines a set of methods
 * that are required to generate a new password for a user.
 */
public class RandomPasswordGenerator implements PasswordGenerator {
    private static SecureRandom rnd = new SecureRandom();
    private static final int NUM_OF_LETTERS = 26;
    private static final int NUMBERS_RANGE = 10;
    private static final int LOWER_A = 'a';
    private static final int UPPER_A = 'A';
    private static final int PASSWORD_LENGTH = 8;
    private static final int SIZE = 62;
 
    private static List<Integer> values = new ArrayList<Integer>(SIZE);

    /**
     * Constructs a random password generator object.
     */
    public RandomPasswordGenerator() {
        if (values == null || values.isEmpty()) {
            initialize();
        }
    }
    
    /**
     * Initializes the list with the values ranges (letter and digits)
     * for random class to generate a new password for a user.
     */
    private static void initialize() {
        addItemsToList(0, NUMBERS_RANGE);
        addItemsToList(UPPER_A, UPPER_A + NUM_OF_LETTERS);
        addItemsToList(LOWER_A, LOWER_A + NUM_OF_LETTERS);
    }

    /**
     * Adds the items to the list. Items added to the list are
     * numbers and letters.
     *
     * @param startIndex begin index of the item to be added
     * @param endIndex end index of the item to be added
     */
    private static void addItemsToList(int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex;i++) {
            values.add(new Integer(i));
        }
    }

    /**
     * Generates new password for user.
     *
     * @param user User object.
     * @return new password for user.
     * @throws PWResetException if password cannot be generated.
     */
    public String generatePassword(AMIdentity user) 
    throws PWResetException {
        StringBuilder buf = new StringBuilder(PASSWORD_LENGTH);
        
        /*
         * Generates a new password value which contains
         * letters (upper & lower case) and digits(0-9).
         */
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int nextNum = rnd.nextInt(SIZE);
            Integer value= values.get(nextNum);
            int num = value.intValue();
            if (num < NUMBERS_RANGE) {
                buf.append(String.valueOf(num));
            } else {
                char c = (char)num;
                buf.append(String.valueOf(c));
            }
        }
        return buf.toString();
    }
}
