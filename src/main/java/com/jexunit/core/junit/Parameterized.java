package com.jexunit.core.junit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import com.jexunit.core.GevoTestBase;
import com.jexunit.core.GevoTestCase;
import com.jexunit.core.commands.TestCommandMethodScanner;

import eu.infomas.annotation.AnnotationDetector;

/**
 * This is an "extension" of the {@link org.junit.runners.Parameterized} JUnit-Runner to run each
 * excel-worksheet as a single unit-test. Additionally there is a new annotation ({@link ExcelFile}
 * to define the name of the excel-file in the test-class.
 * 
 * @author fabian
 * 
 */
public class Parameterized extends Suite {

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD, ElementType.METHOD })
	public static @interface ExcelFile {
	}

	/**
	 * This is the same as the one in the {@code org.junit.runners.Parameterized}. It has to be
	 * copied because it's a private class.
	 * 
	 * @see org.junit.runners.Parameterized
	 */
	private class TestClassRunnerForParameters extends BlockJUnit4ClassRunner {

		private final Object[] fParameters;
		private final String fName;
		private final Class<?> testType;

		TestClassRunnerForParameters(Class<?> type, Object[] parameters, String name,
				Class<?> testType) throws InitializationError {
			super(type);
			fParameters = parameters;
			fName = name;
			this.testType = testType;
		}

		@Override
		public Object createTest() throws Exception {
			if (fieldsAreAnnotated()) {
				return createTestUsingFieldInjection();
			} else {
				return createTestUsingConstructorInjection();
			}
		}

		private Object createTestUsingConstructorInjection() throws Exception {
			return getTestClass().getOnlyConstructor().newInstance(fParameters);
		}

		private Object createTestUsingFieldInjection() throws Exception {
			List<FrameworkField> annotatedFieldsByParameter = getAnnotatedFieldsByParameter();
			if (annotatedFieldsByParameter.size() != fParameters.length) {
				throw new Exception(
						String.format(
								"Wrong number of parameters and @Parameter fields. @Parameter fields counted: %s, available parameters: %s.",
								annotatedFieldsByParameter.size(), fParameters.length));
			}
			Object testClassInstance = getTestClass().getJavaClass().newInstance();
			if (getTestClass().getJavaClass() == GevoTestBase.class) {
				((GevoTestBase) testClassInstance).setTestType(testType);
			}
			for (FrameworkField each : annotatedFieldsByParameter) {
				Field field = each.getField();
				Parameter annotation = field.getAnnotation(Parameter.class);
				int index = annotation.value();
				try {
					if (field.isAccessible()) {
						field.set(testClassInstance, fParameters[index]);
					} else {
						field.setAccessible(true);
						field.set(testClassInstance, fParameters[index]);
						field.setAccessible(false);
					}
				} catch (IllegalArgumentException iare) {
					throw new Exception(
							String.format(
									"%s: Trying to set %s with the value %s that is not the right type (%s instead of %s).",
									getTestClass().getName(), field.getName(), fParameters[index],
									fParameters[index].getClass().getSimpleName(), field.getType()
											.getSimpleName()), iare);
				}
			}
			return testClassInstance;
		}

		@Override
		protected String getName() {
			return fName;
		}

		@Override
		protected String testName(FrameworkMethod method) {
			return method.getName() + getName();
		}

		@Override
		protected void validateConstructor(List<Throwable> errors) {
			validateOnlyOneConstructor(errors);
			if (fieldsAreAnnotated()) {
				validateZeroArgConstructor(errors);
			}
		}

		@Override
		protected void validateFields(List<Throwable> errors) {
			super.validateFields(errors);
			if (fieldsAreAnnotated()) {
				List<FrameworkField> annotatedFieldsByParameter = getAnnotatedFieldsByParameter();
				int[] usedIndices = new int[annotatedFieldsByParameter.size()];
				for (FrameworkField each : annotatedFieldsByParameter) {
					int index = each.getField().getAnnotation(Parameter.class).value();
					if (index < 0 || index > annotatedFieldsByParameter.size() - 1) {
						errors.add(new Exception(
								String.format(
										"Invalid @Parameter value: %s. @Parameter fields counted: %s. Please use an index between 0 and %s.",
										index, annotatedFieldsByParameter.size(),
										(annotatedFieldsByParameter.size() - 1))));
					} else {
						usedIndices[index]++;
					}
				}
				for (int index = 0; index < usedIndices.length; index++) {
					int numberOfUse = usedIndices[index];
					if (numberOfUse == 0) {
						errors.add(new Exception(String.format("@Parameter(%s) is never used.",
								index)));
					} else if (numberOfUse > 1) {
						errors.add(new Exception(String.format(
								"@Parameter(%s) is used more than once (%s).", index, numberOfUse)));
					}
				}
			}
		}

		@Override
		protected Statement classBlock(RunNotifier notifier) {
			return childrenInvoker(notifier);
		}

		@Override
		protected Annotation[] getRunnerAnnotations() {
			return new Annotation[0];
		}
	}

	private static final List<Runner> NO_RUNNERS = Collections.<Runner> emptyList();
	private final ArrayList<Runner> runners = new ArrayList<Runner>();

	static {
		// scan classes for test commands
		final AnnotationDetector detector = new AnnotationDetector(new TestCommandMethodScanner());
		try {
			detector.detect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Only called reflectively. Do not use programmatically.
	 */
	public Parameterized(Class<?> klass) throws Throwable {
		super(klass, NO_RUNNERS);

		String excelFile = getExcelFileName();
		Parameters parameters = getParametersMethod().getAnnotation(Parameters.class);
		createRunnersForParameters(allParameters(excelFile), parameters.name());
	}

	public Parameterized(Class<?> klass, String excelFile) throws Throwable {
		super(klass, NO_RUNNERS);

		Parameters parameters = getParametersMethod().getAnnotation(Parameters.class);
		createRunnersForParameters(allParameters(excelFile), parameters.name());
	}

	public Parameterized(Class<?> klass, String excelFile, Class<?> testType) throws Throwable {
		super(klass, NO_RUNNERS);

		Parameters parameters = getParametersMethod().getAnnotation(Parameters.class);
		createRunnersForParameters(allParameters(excelFile), parameters.name(), testType);
	}

	@Override
	protected List<Runner> getChildren() {
		return runners;
	}

	@SuppressWarnings("unchecked")
	private Iterable<Object[]> allParameters(String excelFile) throws Throwable {
		Object parameters = getParametersMethod().invokeExplosively(null, excelFile);
		if (parameters instanceof Iterable) {
			return (Iterable<Object[]>) parameters;
		} else {
			throw parametersMethodReturnedWrongType();
		}
	}

	private FrameworkMethod getParametersMethod() throws Exception {
		List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(Parameters.class);
		for (FrameworkMethod each : methods) {
			if (each.isStatic() && each.isPublic()) {
				return each;
			}
		}

		throw new Exception("No public static parameters method on class "
				+ getTestClass().getName());
	}

	/**
	 * Get the excel-file-name from the test-class. It should be read from a static field or a
	 * static method returning a string, both annotated with {@code @ExcelFile}.
	 * 
	 * @return the name of the excel-file to use for the test
	 * @throws Exception
	 */
	private String getExcelFileName() throws Exception {
		List<FrameworkField> fields = getTestClass().getAnnotatedFields(ExcelFile.class);
		for (FrameworkField each : fields) {
			if (each.isStatic() && each.getType() == String.class) {
				Field field = each.getField();
				if (field.isAccessible()) {
					return (String) field.get(getTestClass());
				} else {
					field.setAccessible(true);
					String retVal = (String) field.get(getTestClass());
					field.setAccessible(false);
					return retVal;
				}
			}
		}

		// check, if there is a method annotated with @ExcelFile
		List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(ExcelFile.class);
		for (FrameworkMethod each : methods) {
			if (each.isStatic() && each.isPublic() && each.getReturnType() == String.class) {
				return (String) each.getMethod().invoke(null);
			}
		}

		throw new Exception(
				"No excel-file definition found (static string-field or public static method annotated with @ExcelFile) in class "
						+ getTestClass().getName());
	}

	private void createRunnersForParameters(Iterable<Object[]> allParameters, String namePattern)
			throws InitializationError, Exception {
		createRunnersForParameters(allParameters, namePattern, null);
	}

	private void createRunnersForParameters(Iterable<Object[]> allParameters, String namePattern,
			Class<?> testType) throws InitializationError, Exception {
		try {
			int i = 0;
			for (Object[] parametersOfSingleTest : allParameters) {
				String name = nameFor(namePattern, i, parametersOfSingleTest);
				TestClassRunnerForParameters runner = new TestClassRunnerForParameters(
						getTestClass().getJavaClass(), parametersOfSingleTest, name, testType);
				runners.add(runner);
				++i;
			}
		} catch (ClassCastException e) {
			throw parametersMethodReturnedWrongType();
		}
	}

	private String nameFor(String namePattern, int index, Object[] parameters) {
		String finalPattern = namePattern.replaceAll("\\{index\\}", Integer.toString(index));
		String name;
		if (parameters != null && parameters.length > 0 && parameters[0] instanceof List
				&& !((List<?>) parameters[0]).isEmpty()
				&& ((List<?>) parameters[0]).get(0) instanceof GevoTestCase) {
			name = MessageFormat.format(finalPattern, ((List<?>) parameters[0]).get(0));
		} else {
			name = MessageFormat.format(finalPattern, parameters);
		}
		return "[" + name + "]";
	}

	private Exception parametersMethodReturnedWrongType() throws Exception {
		String className = getTestClass().getName();
		String methodName = getParametersMethod().getName();
		String message = MessageFormat.format("{0}.{1}() must return an Iterable of arrays.",
				className, methodName);
		return new Exception(message);
	}

	private List<FrameworkField> getAnnotatedFieldsByParameter() {
		return getTestClass().getAnnotatedFields(Parameter.class);
	}

	private boolean fieldsAreAnnotated() {
		return !getAnnotatedFieldsByParameter().isEmpty();
	}
}