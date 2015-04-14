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
* Copyright 2015 ForgeRock AS.
*/

package org.forgerock.openam.utils.qr;

/**
 * Represents the error correction level provided by a QR code. At present the four levels are
 *
 * LOW (Low)            7% of textual information can be restored
 * MEDIUM (Medium)      15% of textual information can be restored
 * QUARTILE (Quartile)  25% of textual information can be restored
 * HIGH (High)          30% of textual information can be restored
 *
 * See http://en.wikipedia.org/wiki/QR_code § "Error correction" for more information.
 *
 * Note: Given that the QR codes we are talking about here will be displayed on a digital device's screen, there is
 * unlikely to be a need for error correction at any level above the minimum ("LOW"). However if the QR code is to be
 * printed and is likely to be in an environment where it could be damaged, then the use of higher error correction
 * levels may be advisable.
 *
 * @since 13.0.0
 */
public enum ErrorCorrectionLevel {

    LOW("L"), MEDIUM("M"), QUARTILE("Q"), HIGH("H");

    private final String letterCode;

    ErrorCorrectionLevel(String letterCode) {
        this.letterCode = letterCode;
    }

    public String getLetterCode() {
        return letterCode;
    }

}
