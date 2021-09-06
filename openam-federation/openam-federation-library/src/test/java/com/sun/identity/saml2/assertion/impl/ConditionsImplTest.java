/**
 * DO NOT ALTER OR REMOVE THIS HEADER.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at LICENSE.md
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 */
package com.sun.identity.saml2.assertion.impl;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

import java.time.Instant;
import java.util.Date;

import org.testng.annotations.Test;

import com.sun.identity.saml2.assertion.Conditions;

public class ConditionsImplTest {

	private static final int SKEW_60S = 60; // 60 s of clock skew

	private final Instant validFrom = Instant.now();
	private final Instant validTo = validFrom.plusSeconds(180);

	@Test
	public void testVoid() {
		Conditions conditions = new ConditionsImpl();

		assertTrue(conditions.checkDateValidity(Instant.now().toEpochMilli()));
	}

	@Test
	public void testSuccess() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));

		long instant = validFrom.plusSeconds(30).toEpochMilli();
		assertTrue(conditions.checkDateValidity(instant));
	}

	@Test
	public void testTooEarly() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));

		long instant = validFrom.minusSeconds(30).toEpochMilli();
		assertFalse(conditions.checkDateValidity(instant));
	}

	@Test
	public void testNotTooEarlyWithSkew() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));

		long instant = validFrom.minusSeconds(30).toEpochMilli();
		assertTrue(conditions.checkDateValidity(instant, SKEW_60S));
	}

	@Test
	public void testTooEarlyWithSkew() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));

		long instant = validFrom.minusSeconds(80).toEpochMilli();
		assertFalse(conditions.checkDateValidity(instant, SKEW_60S));
	}

	@Test
	public void testTooLate() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));
		
		long instant = validTo.plusSeconds(30).toEpochMilli();
		assertFalse(conditions.checkDateValidity(instant));
	}

	@Test
	public void testNotTooLateWithSkew() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));
		
		long instant = validTo.plusSeconds(30).toEpochMilli();
		assertTrue(conditions.checkDateValidity(instant, SKEW_60S));
	}

	@Test
	public void testTooLateWithSkew() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));
		
		long instant = validTo.plusSeconds(80).toEpochMilli();
		assertFalse(conditions.checkDateValidity(instant, SKEW_60S));
	}
	
	@Test
	public void testTooEarlyForRange() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));

		long instant = validFrom.minusSeconds(30).toEpochMilli();
		assertFalse(conditions.checkDateValidity(instant));
	}

	@Test
	public void testNotTooEarlyWithSkewForRange() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));

		long instant = validFrom.minusSeconds(30).toEpochMilli();
		assertTrue(conditions.checkDateValidity(instant, SKEW_60S));
	}

	@Test
	public void testTooEarlyWithSkewForRange() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));

		long instant = validFrom.minusSeconds(80).toEpochMilli();
		assertFalse(conditions.checkDateValidity(instant, SKEW_60S));
	}

	@Test
	public void testTooLateForRange() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));
		
		long instant = validTo.plusSeconds(30).toEpochMilli();
		assertFalse(conditions.checkDateValidity(instant));
	}

	@Test
	public void testNotTooLateWithSkewForRange() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));
		
		long instant = validTo.plusSeconds(30).toEpochMilli();
		assertTrue(conditions.checkDateValidity(instant, SKEW_60S));
	}

	@Test
	public void testTooLateWithSkewForRange() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));
		
		long instant = validTo.plusSeconds(80).toEpochMilli();
		assertFalse(conditions.checkDateValidity(instant, SKEW_60S));
	}

	@Test
	public void testSpotOn() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));
		
		long instant = validFrom.toEpochMilli();
		assertTrue(conditions.checkDateValidity(instant));
	}
	
	@Test
	public void testJustInTime() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));
		
		long instant = validTo.minusSeconds(1).toEpochMilli();
		assertTrue(conditions.checkDateValidity(instant));
	}
}
