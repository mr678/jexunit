package com.jexunit.base;

import java.math.BigDecimal;

public class MyComplexService {
	
	public double calculateFees(double number) {
		BigDecimal bd = new BigDecimal(number);
		bd = bd.multiply(new BigDecimal("0.1"));
		//return number*0.1; <-- floating arithmetic
		return  bd.doubleValue();
	}

}
