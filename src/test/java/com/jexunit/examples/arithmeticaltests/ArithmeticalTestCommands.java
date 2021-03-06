/**
 * 
 */
package com.jexunit.examples.arithmeticaltests;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jexunit.core.GevoTestCase;
import com.jexunit.core.GevoTestObjectHelper;
import com.jexunit.core.commands.TestCommand;

/**
 * Provide the Test-Commands for the Arithmetical tests.
 * 
 * @author fabian
 * 
 */
public class ArithmeticalTestCommands {

	private static final Logger log = Logger.getLogger(ArithmeticalTestCommands.class.getName());

	public static void runCommand(GevoTestCase testCase) throws Exception {
		Double val1, val2, result;
		val1 = Double.parseDouble(testCase.getValues().get("param1").getValue());
		val2 = Double.parseDouble(testCase.getValues().get("param2").getValue());
		result = Double.parseDouble(testCase.getValues().get("result").getValue());
		log.log(Level.INFO, "run command (testCase: {0})", testCase);
		switch (testCase.getTestCommand()) {
		case "ADD":
			assertThat(val1 + val2, equalTo(result));
			break;
		case "SUB":
			assertThat(val1 - val2, equalTo(result));
			break;
		case "MUL":
			assertThat(val1 * val2, equalTo(result));
			break;
		case "DIV":
			assertThat(val1 / val2, equalTo(result));
			break;
		}
	}

	public static void runCommandWithObject(GevoTestCase testCase) throws Exception {
		ArithmeticalTestObject obj = GevoTestObjectHelper.createObject(testCase,
				ArithmeticalTestObject.class);
		log.log(Level.INFO, "run comand with object (testCase: {0})", testCase);
		switch (testCase.getTestCommand()) {
		case "ADD":
			assertThat(obj.getParam1() + obj.getParam2(), equalTo(obj.getResult()));
			break;
		case "SUB":
			assertThat(obj.getParam1() - obj.getParam2(), equalTo(obj.getResult()));
			break;
		case "MUL":
			assertThat(obj.getParam1() * obj.getParam2(), equalTo(obj.getResult()));
			break;
		case "DIV":
			assertThat(obj.getParam1() / obj.getParam2(), equalTo(obj.getResult()));
			break;
		}
	}

	@TestCommand("add")
	public static void runAddCommand(GevoTestCase testCase) throws Exception {
		log.info("in test command: ADD!");
		ArithmeticalTestObject obj = GevoTestObjectHelper.createObject(testCase,
				ArithmeticalTestObject.class);
		assertThat(obj.getParam1() + obj.getParam2(), equalTo(obj.getResult()));
	}

	@TestCommand(value = "sub")
	public static void runSubCommand(GevoTestCase testCase, ArithmeticalTestObject testObject)
			throws Exception {
		log.info("in test command: SUB!");
		assertThat(testObject.getParam1() - testObject.getParam2(), equalTo(testObject.getResult()));
	}
}
