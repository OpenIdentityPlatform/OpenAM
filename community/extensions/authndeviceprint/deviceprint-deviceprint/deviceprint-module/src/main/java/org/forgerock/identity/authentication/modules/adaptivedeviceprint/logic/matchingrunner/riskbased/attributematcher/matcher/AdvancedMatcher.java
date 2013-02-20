/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 */

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.matcher;

import java.util.Set;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.ComparisonResult;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.RiskBasedDevicePrintComparator;

import com.sun.identity.shared.debug.Debug;

/**
 * @author jdabrowski
 */
public final class AdvancedMatcher {
	
	private AdvancedMatcher() {
		super();
	}

	private static final Debug debug = Debug
			.getInstance(RiskBasedDevicePrintComparator.class.getName());

	/**
	 * * Rebuilds both attributes using set of AttributeRebuilder and redirect
	 * comparison to
	 * {@link AttributeComparatorHelper#compareAttribute(Object, Object, Long)}
	 * 
	 * @param currentAttribute
	 * @param storedAttribute
	 * @param rebuilders
	 * @param penaltyPoints
	 * @return
	 */
	public static ComparisonResult getComparationResult(
			String currentAttribute, String storedAttribute,
			Set<AttributeRebuilder> rebuilders, long penaltyPoints) {

		if (resultDoesNotMatter(penaltyPoints))
			return ComparisonResult.ZERO_PENALTY_POINTS;

		Set<AttributeRebuilder> rebuildPatterns = rebuilders;
		currentAttribute = rebuildUsingPatterns(currentAttribute,
				rebuildPatterns);
		storedAttribute = rebuildUsingPatterns(storedAttribute, rebuildPatterns);

		if (debug.messageEnabled()) {
			debug.message("After rebuild based on patterns comparing "
					+ currentAttribute + " and " + storedAttribute);
		}

		return AttributeComparatorHelper.compareAttribute(currentAttribute,
				storedAttribute, penaltyPoints);
	}

	private static String rebuildUsingPatterns(String attribute,
			Set<AttributeRebuilder> rebuildPatterns) {
		if (rebuildPatterns != null) {
			for (AttributeRebuilder pattern : rebuildPatterns) {
				try {
					attribute = pattern.apply(attribute);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return attribute;
	}

	private static boolean resultDoesNotMatter(long penaltyPoints) {
		return penaltyPoints == 0l;
	}

}
