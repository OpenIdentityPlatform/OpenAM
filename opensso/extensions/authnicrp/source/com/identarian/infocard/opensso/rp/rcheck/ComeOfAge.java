/* The contents of this file are subject to the terms
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
 * $Id: ComeOfAge.java,v 1.3 2009/10/28 08:35:24 ppetitsm Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2008 Patrick Petit Consulting
 */
package com.identarian.infocard.opensso.rp.rcheck;

import com.identarian.infocard.opensso.rp.InfocardClaims;
import com.identarian.infocard.opensso.rp.InfocardIdentity;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 * @author Patrick
 */
public class ComeOfAge implements RoleCheckPlugin {

    @Override
    public boolean isIdentityMatchingRole(InfocardIdentity identity, String role) {

        String claim;

        if (identity.isClaimSupplied(InfocardClaims.getAGE_OVER_18_URI())) {
            claim = identity.getClaimValue(InfocardClaims.getAGE_OVER_18_URI());
            if (claim != null && claim.length() > 0) {
                if (Boolean.valueOf(claim).booleanValue()) {
                    return true;
                }
            }
        }

        if (identity.isClaimSupplied(InfocardClaims.getCOPPA_CERTIFIED_ADULTE_URI())) {
            claim = identity.getClaimValue(InfocardClaims.getCOPPA_CERTIFIED_ADULTE_URI());
            if (claim != null && claim.length() > 0) {
                if (Boolean.valueOf(claim).booleanValue()) {
                    return true;
                }
            }
        }

        if (identity.isClaimSupplied(InfocardClaims.getDATE_OF_BIRTH_URI())) {
            claim = identity.getClaimValue(InfocardClaims.getDATE_OF_BIRTH_URI());
            if (claim != null) {
                GregorianCalendar birthDate = getDate(claim);
                birthDate.add(GregorianCalendar.YEAR, 18);
                GregorianCalendar now = rightNow();
                if (birthDate.before(now)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private static GregorianCalendar getDate(String dt) {
        String[] dateTime = dt.split("T");
        String date = dateTime[0];
        String[] ymd = date.split("-");
        int year = Integer.parseInt(ymd[0]);
        int month = Integer.parseInt(ymd[1]) - 1;
        int day = Integer.parseInt(ymd[2]);
        TimeZone tz = TimeZone.getTimeZone("GMT+00:00");
        GregorianCalendar cal = new GregorianCalendar(tz, Locale.US);
        cal.set(year, month, day);
        return cal;
    }

    private static GregorianCalendar rightNow() {
        TimeZone tz = TimeZone.getTimeZone("GMT+00:00");
        GregorianCalendar cal = new GregorianCalendar(tz, Locale.US);
        return cal;
    }
}