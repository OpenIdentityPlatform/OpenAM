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
	public void testTooEarly() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));

		long now = new Date(validFrom.minusSeconds(30).toEpochMilli()).getTime();
		assertFalse(conditions.checkDateValidity(now));
	}

	@Test
	public void testNotTooEarlyWithSkew() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));

		long now = new Date(validFrom.minusSeconds(30).toEpochMilli()).getTime();
		assertTrue(conditions.checkDateValidity(now, SKEW_60S));
	}

	@Test
	public void testTooEarlyWithSkew() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));

		long now = new Date(validFrom.minusSeconds(80).toEpochMilli()).getTime();
		assertFalse(conditions.checkDateValidity(now, SKEW_60S));
	}

	@Test
	public void testTooLate() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));
		
		long now = new Date(validTo.plusSeconds(30).toEpochMilli()).getTime();
		assertFalse(conditions.checkDateValidity(now));
	}

	@Test
	public void testNotTooLateWithSkew() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));
		
		long now = new Date(validTo.plusSeconds(30).toEpochMilli()).getTime();
		assertTrue(conditions.checkDateValidity(now, SKEW_60S));
	}

	@Test
	public void testTooLateWithSkew() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));
		
		long now = new Date(validTo.plusSeconds(80).toEpochMilli()).getTime();
		assertFalse(conditions.checkDateValidity(now, SKEW_60S));
	}
	
	@Test
	public void testTooEarlyForRange() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));

		long now = new Date(validFrom.minusSeconds(30).toEpochMilli()).getTime();
		assertFalse(conditions.checkDateValidity(now));
	}

	@Test
	public void testNotTooEarlyWithSkewForRange() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));

		long now = new Date(validFrom.minusSeconds(30).toEpochMilli()).getTime();
		assertTrue(conditions.checkDateValidity(now, SKEW_60S));
	}

	@Test
	public void testTooEarlyWithSkewForRange() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));

		long now = new Date(validFrom.minusSeconds(80).toEpochMilli()).getTime();
		assertFalse(conditions.checkDateValidity(now, SKEW_60S));
	}

	@Test
	public void testTooLateForRange() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));
		
		long now = new Date(validTo.plusSeconds(30).toEpochMilli()).getTime();
		assertFalse(conditions.checkDateValidity(now));
	}

	@Test
	public void testNotTooLateWitkSkewForRange() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));
		
		long now = new Date(validTo.plusSeconds(30).toEpochMilli()).getTime();
		assertTrue(conditions.checkDateValidity(now, SKEW_60S));
	}

	@Test
	public void testTooLateWitkSkewForRange() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));
		
		long now = new Date(validTo.plusSeconds(80).toEpochMilli()).getTime();
		assertFalse(conditions.checkDateValidity(now, SKEW_60S));
	}

	@Test
	public void testSpotOn() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));
		
		long now = new Date(validFrom.toEpochMilli()).getTime();
		assertTrue(conditions.checkDateValidity(now));
	}
	
	@Test
	public void testJustInTime() throws Exception {
		Conditions conditions = new ConditionsImpl();
		conditions.setNotBefore(new Date(validFrom.toEpochMilli()));
		conditions.setNotOnOrAfter(new Date(validTo.toEpochMilli()));
		
		long now = new Date(validTo.minusSeconds(1).toEpochMilli()).getTime();
		assertTrue(conditions.checkDateValidity(now));
	}
}
