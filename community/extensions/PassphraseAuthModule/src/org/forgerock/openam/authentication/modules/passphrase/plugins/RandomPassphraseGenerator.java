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
 *    "Portions Copyrighted [2012] [Forgerock AS]"
 */

package org.forgerock.openam.authentication.modules.passphrase.plugins;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.authentication.modules.passphrase.ui.model.PPResetException;
import com.sun.identity.idm.AMIdentity;

/**
 * <code>RandomPassphraseGenerator</code> defines a set of methods that are
 * required to generate a new passphrase for a user.
 */
public class RandomPassphraseGenerator implements PassphraseGenerator {
	private static SecureRandom rnd = new SecureRandom();
	private static final int NUM_OF_LETTERS = 26;
	private static final int NUMBERS_RANGE = 10;
	private static final int LOWER_A = 'a';
	private static final int UPPER_A = 'A';
	private static final int PASSPHRASE_LENGTH = 10;
	private static final int SIZE = 62;

	private static List<Integer> values = new ArrayList<Integer>(SIZE);

	/**
	 * Constructs a random passphrase generator object.
	 */
	public RandomPassphraseGenerator() {
		if (values == null || values.isEmpty()) {
			initialize();
		}
	}

	/**
	 * Initializes the list with the values ranges (letter and digits) for
	 * random class to generate a new passphrase for a user.
	 */
	private static void initialize() {
		addItemsToList(0, NUMBERS_RANGE);
		addItemsToList(UPPER_A, UPPER_A + NUM_OF_LETTERS);
		addItemsToList(LOWER_A, LOWER_A + NUM_OF_LETTERS);
	}

	/**
	 * Adds the items to the list. Items added to the list are numbers and letters.
	 * 
	 * @param startIndex begin index of the item to be added
	 * @param endIndex end index of the item to be added
	 */
	private static void addItemsToList(int startIndex, int endIndex) {
		for (int i = startIndex; i < endIndex; i++) {
			values.add(new Integer(i));
		}
	}

	/**
	 * Generates new passphrase for user.
	 * 
	 * @param user User object.
	 * @return new passphrase for user.
	 * @throws PPResetException if passphrase cannot be generated.
	 */
	public String generatePassphrase(AMIdentity user) throws PPResetException {
		StringBuffer buf = new StringBuffer(PASSPHRASE_LENGTH);

		/*
		 * Generates a new passphrase value which contains letters (upper & lower
		 * case) and digits(0-9).
		 */
		for (int i = 0; i < PASSPHRASE_LENGTH; i++) {
			int nextNum = rnd.nextInt(SIZE);
			Integer value = (Integer) values.get(nextNum);
			int num = value.intValue();
			if (num < NUMBERS_RANGE) {
				buf.append(String.valueOf(num));
			} else {
				char c = (char) num;
				buf.append(String.valueOf(c));
			}
		}
		return buf.toString();
	}
}