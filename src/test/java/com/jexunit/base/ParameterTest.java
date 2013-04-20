package com.jexunit.base;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ParameterTest {

	private int numberOfTweets;
	private double expectedFee;

	@Parameters
	public static Collection data() {
		return Arrays.asList(new Object[][] { { 0, 0.00 }, { 50, 5.00 },
				{ 99, 9.90 }, { 100, 10.00 }, { 101, 10.1 }, { 200, 20 },
				{ 499, 49.9 }, { 500, 50 }, { 501, 50.1 }, { 1000, 100 },
				{ 10000, 1000 }, });
	}

	public ParameterTest(int numberOfTweets, double expectedFee) {
		super();
		this.numberOfTweets = numberOfTweets;
		this.expectedFee = expectedFee;
	}


	@Test
	public void shouldCalculateCorrectFee() {
		MyComplexService myService = new MyComplexService();
		double calculatedFees = myService.calculateFees(numberOfTweets);
		assertTrue(calculatedFees == expectedFee);
	}
}
