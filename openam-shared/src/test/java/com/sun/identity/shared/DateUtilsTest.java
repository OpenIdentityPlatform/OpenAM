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
 * Copyright 2014 ForgeRock AS.
 */
package com.sun.identity.shared;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.fest.assertions.Assertions.*;

/**
 * Created by tonybamford on 31/10/2014.
 */
public class DateUtilsTest {

    private Date dateWithoutMilliseconds;
    private Date dateWithMilliseconds;

    @BeforeTest
    public void setup() {
        Calendar calendar = new GregorianCalendar(2013,1,28,13,24,56);

        dateWithoutMilliseconds = calendar.getTime();

        calendar.set(Calendar.MILLISECOND, 666);
        dateWithMilliseconds = calendar.getTime();
    }

    @Test
    public void testDateToString() {
        String have = DateUtils.dateToString(dateWithoutMilliseconds);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String expect = sdf.format(dateWithoutMilliseconds);

        assertThat(have).isEqualTo(expect);
    }

    @Test
    public void testToUTCDateFormat() {
        String have = DateUtils.toUTCDateFormat(dateWithoutMilliseconds);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String expect = sdf.format(dateWithoutMilliseconds);

        assertThat(have).isEqualTo(expect);
    }

    @Test
    public void testToUTCDateFormatWithMilliseconds() {
        String have = DateUtils.toUTCDateFormatWithMilliseconds(dateWithoutMilliseconds);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String expect = sdf.format(dateWithoutMilliseconds);

        assertThat(have).isEqualTo(expect);

        have = DateUtils.toUTCDateFormatWithMilliseconds(dateWithMilliseconds);
        expect = sdf.format(dateWithMilliseconds);

        assertThat(have).isEqualTo(expect);
        assertThat(DateUtils.toUTCDateFormatWithMilliseconds(dateWithoutMilliseconds))
                .isNotEqualTo(DateUtils.toUTCDateFormatWithMilliseconds(dateWithMilliseconds));
    }

    @Test
    public void testToFullLocalDateFormat() {
        String have = DateUtils.toFullLocalDateFormat(dateWithoutMilliseconds);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String expect = sdf.format(dateWithoutMilliseconds);

        // test needs improving
        assertThat(have.startsWith(expect)).isTrue();
    }

    @Test
    public void testStringToDate() throws ParseException {
        SimpleDateFormat formatNoMilliseconds = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        SimpleDateFormat formatWithMilliseconds = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        SimpleDateFormat formatNoMillisecondsZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        SimpleDateFormat formatWithMillisecondsZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        String zeroMillisecondDateWithoutMilliseconds = formatNoMilliseconds.format(dateWithoutMilliseconds);
        String zeroMillisecondDateWithoutMillisecondsZ = formatNoMillisecondsZ.format(dateWithoutMilliseconds);
        String nonzeroMillisecondDateWithoutMilliseconds = formatNoMilliseconds.format(dateWithMilliseconds);
        String nonzeroMillisecondDateWithoutMillisecondsZ = formatNoMillisecondsZ.format(dateWithMilliseconds);
        String nonzeroMillisecondDateWithMilliseconds = formatWithMilliseconds.format(dateWithMilliseconds);
        String nonzeroMillisecondDateWithMillisecondsZ = formatWithMillisecondsZ.format(dateWithMilliseconds);

        Date reconstructedDate = DateUtils.stringToDate(zeroMillisecondDateWithoutMilliseconds);
        assertThat(reconstructedDate).isEqualTo(dateWithoutMilliseconds);

        reconstructedDate = DateUtils.stringToDate(zeroMillisecondDateWithoutMillisecondsZ);
        assertThat(reconstructedDate).isEqualTo(dateWithoutMilliseconds);

        reconstructedDate = DateUtils.stringToDate(nonzeroMillisecondDateWithoutMilliseconds);
        assertThat(reconstructedDate).isEqualTo(dateWithoutMilliseconds);

        reconstructedDate = DateUtils.stringToDate(nonzeroMillisecondDateWithoutMillisecondsZ);
        assertThat(reconstructedDate).isEqualTo(dateWithoutMilliseconds);

        reconstructedDate = DateUtils.stringToDate(nonzeroMillisecondDateWithMilliseconds);
        assertThat(reconstructedDate).isEqualTo(dateWithMilliseconds);

        reconstructedDate = DateUtils.stringToDate(nonzeroMillisecondDateWithMillisecondsZ);
        assertThat(reconstructedDate).isEqualTo(dateWithMilliseconds);

        Date reconstructedDate1 = DateUtils.stringToDate(zeroMillisecondDateWithoutMillisecondsZ);
        Date reconstructedDate2 = DateUtils.stringToDate(nonzeroMillisecondDateWithMillisecondsZ);
        assertThat(reconstructedDate1).isNotEqualTo(reconstructedDate2);
    }
}
