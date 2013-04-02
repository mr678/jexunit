/**
 * 
 */
package com.jexunit.core.commands;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import eu.infomas.annotation.AnnotationDetector.MethodReporter;

/**
 * @author fabian
 * 
 */
public class TestCommandMethodScanner implements MethodReporter {

	private static final Map<String, Method> methods = new HashMap<>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.infomas.annotation.AnnotationDetector.Reporter#annotations()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends Annotation>[] annotations() {
		return new Class[] { TestCommand.class };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.infomas.annotation.AnnotationDetector.MethodReporter#reportMethodAnnotation(java.lang.
	 * Class, java.lang.String, java.lang.String)
	 */
	@Override
	public void reportMethodAnnotation(Class<? extends Annotation> annotation, String className,
			String methodName) {
		try {
			Class<?> clazz = getClass().getClassLoader().loadClass(className);
			if (annotation.isAnnotation() && annotation == TestCommand.class) {
				for (Method m : clazz.getDeclaredMethods()) {
					TestCommand tc = m.getAnnotation(TestCommand.class);
					if (tc != null) {
						for (String command : tc.value()) {
							methods.put(command, m);
						}
					}
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static Map<String, Method> getTestCommandMethods() {
		return methods;
	}
}