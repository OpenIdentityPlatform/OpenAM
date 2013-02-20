/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [2012] [Forgerock AS]"
 */

package org.forgerock.openam.authentication.modules.passphrase.security.passphrase;

import java.util.ArrayList;
import java.util.Random;

import org.forgerock.openam.authentication.modules.passphrase.common.utility.CommonUtilities;
import org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

/**
 * This util class is used to generate three random positions based on the
 * user passphrase total characters length.
 */
public class PassphraseGenerator {

	private static Debug debug = Debug.getInstance("CustomModule");
	
	public PassphraseGenerator(AMIdentity user) throws IllegalArgumentException {
		passphraseLength = getPassphraseLength(user);
		generateRandomDigits();
	}
	
	private int passphraseLength = 0;

	ArrayList<Integer> randomPositions = new ArrayList<Integer>(3);

	/**
	 * This method is used to generate three distinct random positions which
	 * lies within the total characters length of the passphrase.
	 */
	private void generateRandomDigits() {
		Random rnd = new Random();
		while (randomPositions.size() < 3) {
			int position = rnd.nextInt(passphraseLength);
			if (position > 0 && !randomPositions.contains(position))
				randomPositions.add(position);
		}
	}

	/**
	 * This method returns a formated string representing the positions of the
	 * generated random positions.
	 */
	public String getPositionString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < randomPositions.size(); i++) {
			int position = randomPositions.get(i);
			sb.append(position);
			
			switch (position) {
			case 1:
				sb.append("st");
				break;
			case 2:
				sb.append("nd");
				break;
			case 3:
				sb.append("rd");
				break;

			default:
				sb.append("th");
			}
			if (i == 0)
				sb.append(", ");
			else if (i == 1)
				sb.append(" & ");
		}
		return sb.toString();
	}

	public String getPositions() {
		return randomPositions.toString();
	}
	
	/**
	 * This method returns the length of the logged in user's passphrase.
	 * 
	 * @param user
	 * @return
	 * @throws InvalidArgumentException
	 */
	private int getPassphraseLength(AMIdentity user) throws IllegalArgumentException {
		String passphrase = null;
		try {
			passphrase = CollectionHelper.getMapAttr(user.getAttributes(), CommonUtilities.getProperty(PassphraseConstants.USER_PASSPHRASE_ATTRIBUTE));
		} catch (Exception e) {
			debug.error("Error in getting the passphrase attribute: ", e);
		}
		if (passphrase != null && passphrase.length() > 3)
			return passphrase.length();
		else
			throw new IllegalArgumentException("The passphrase is either null or less the 3 characters.");
	}
}