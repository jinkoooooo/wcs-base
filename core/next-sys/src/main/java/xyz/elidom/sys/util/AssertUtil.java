/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.util;

import java.util.Collection;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import xyz.elidom.exception.client.ElidomInvalidParamsException;
import xyz.elidom.exception.server.ElidomValidationException;
import xyz.elidom.sys.SysMessageConstants;

/**
 * Assert를 위한 Utility 클래스 : Spring Assert + Message 기능이 혼합
 * 
 * @author shortstop
 */
public class AssertUtil {

	/**
	 * Assert Not Empty
	 * 
	 * @param name
	 * @param value
	 */
	public static void assertNotEmpty(String name, Object value) {
		if (SysValueUtil.isEmpty(value)) {
			throw new ElidomValidationException(SysMessageConstants.INVALID_PARAM, SysMessageConstants.EMPTY_PARAM, MessageUtil.params(name));
		}
	}

	/**
	 * Invalid Parameters 예외를 던짐
	 * 
	 * @param name
	 * @param value
	 */
	public static void throwInvalidParams(String name, Object value) {
		throw new ElidomInvalidParamsException(SysMessageConstants.INVALID_PARAM, SysMessageConstants.INVALID_PARAM, MessageUtil.params(name, SysValueUtil.toString(value)));
	}

	/**
	 * Assert that the given text does not contain the given substring.
	 * 
	 * <pre class="code">
	 * Assert.doesNotContain(name, "rod");
	 * </pre>
	 * 
	 * @param textToSearch
	 *            the text to search
	 * @param substring
	 *            the substring to find within the text
	 * @throws IllegalArgumentException
	 *             if the text contains the substring
	 */
	public static void doesNotContain(String textToSearch, String substring) {
		doesNotContain(textToSearch, substring, SysMessageConstants.A_CONTAINS_B);
	}

	/**
	 * Assert that the given text does not contain the given substring.
	 * 
	 * <pre class="code">
	 * Assert.doesNotContain(name, "rod", "Name must not contain 'rod'");
	 * </pre>
	 * 
	 * @param textToSearch
	 *            the text to search
	 * @param substring
	 *            the substring to find within the text
	 * @param message
	 *            the exception message to use if the assertion fails
	 * @throws IllegalArgumentException
	 *             if the text contains the substring
	 */
	public static void doesNotContain(String textToSearch, String substring, String message) {
		if (StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) && textToSearch.contains(substring)) {
			throw new ElidomInvalidParamsException(SysMessageConstants.INVALID_PARAM, SysMessageConstants.A_CONTAINS_B, SysValueUtil.newStringList(textToSearch, substring));
		}
	}
	
	/**
	 * Assert that the given text contain the given substring.
	 * 
	 * <pre class="code">
	 * Assert.doesNotContain(name, "rod");
	 * </pre>
	 * 
	 * @param textToSearch
	 *            the text to search
	 * @param substring
	 *            the substring to find within the text
	 * @throws IllegalArgumentException
	 *             if the text contains the substring
	 */
	public static void contains(String textToSearch, String substring) {
		contains(textToSearch, substring, SysMessageConstants.A_DOES_NOT_CONTAINS_B);
	}

	/**
	 * Assert that the given text does not contain the given substring.
	 * 
	 * <pre class="code">
	 * Assert.doesNotContain(name, "rod", "Name must not contain 'rod'");
	 * </pre>
	 * 
	 * @param textToSearch
	 *            the text to search
	 * @param substring
	 *            the substring to find within the text
	 * @param message
	 *            the exception message to use if the assertion fails
	 * @throws IllegalArgumentException
	 *             if the text contains the substring
	 */
	public static void contains(String textToSearch, String substring, String message) {
		if (StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) && !textToSearch.contains(substring)) {
			throw new ElidomInvalidParamsException(SysMessageConstants.INVALID_PARAM, message, SysValueUtil.newStringList(textToSearch, substring));
		}
	}

	/**
	 * Assert that the given String is not empty; that is, it must not be
	 * {@code null} and not the empty String.
	 * 
	 * <pre class="code">
	 * Assert.hasLength(name);
	 * </pre>
	 * 
	 * @param text
	 *            the String to check
	 * @see StringUtils#hasLength
	 * @throws IllegalArgumentException
	 *             if the text is empty
	 */
	public static void hasLength(String text) {
		hasLength(text, SysMessageConstants.VALUE_IS_EMPTY);
	}

	/**
	 * Assert that the given String is not empty; that is, it must not be
	 * {@code null} and not the empty String.
	 * 
	 * <pre class="code">
	 * Assert.hasLength(name, "Name must not be empty");
	 * </pre>
	 * 
	 * @param text
	 *            the String to check
	 * @param message
	 *            the exception message to use if the assertion fails
	 * @see StringUtils#hasLength
	 * @throws IllegalArgumentException
	 *             if the text is empty
	 */
	public static void hasLength(String text, String message) {
		if (!StringUtils.hasLength(text)) {
			throw new ElidomInvalidParamsException(SysMessageConstants.INVALID_PARAM, message, SysValueUtil.newStringList(text));
		}
	}

	/**
	 * Assert that {@code superType.isAssignableFrom(subType)} is {@code true}.
	 * 
	 * <pre class="code">
	 * Assert.isAssignable(Number.class, myClass);
	 * </pre>
	 * 
	 * @param superType
	 *            the super type to check
	 * @param subType
	 *            the sub type to check
	 * @throws IllegalArgumentException
	 *             if the classes are not assignable
	 */
	public static void isAssignable(Class<?> superType, Class<?> subType) {
		isAssignable(superType, subType, SysMessageConstants.TYPEA_IS_NOT_ASSIGNABLE_OF_TYPEB);
	}

	/**
	 * Assert that {@code superType.isAssignableFrom(subType)} is {@code true}.
	 * 
	 * <pre class="code">
	 * Assert.isAssignable(Number.class, myClass);
	 * </pre>
	 * 
	 * @param superType
	 *            the super type to check against
	 * @param subType
	 *            the sub type to check
	 * @param message
	 *            a message which will be prepended to the message produced by
	 *            the function itself, and which may be used to provide context.
	 *            It should normally end in ":" or "." so that the generated
	 *            message looks OK when appended to it.
	 * @throws IllegalArgumentException
	 *             if the classes are not assignable
	 */
	public static void isAssignable(Class<?> superType, Class<?> subType, String message) {
		try {
			Assert.isAssignable(superType, subType);
		} catch (IllegalArgumentException iae) {
			throw new ElidomInvalidParamsException(SysMessageConstants.INVALID_PARAM, message, SysValueUtil.newStringList(superType.getName(),subType.getName()));
		}
	}

	/**
	 * Assert that the provided object is an instance of the provided class.
	 * 
	 * <pre class="code">
	 * Assert.instanceOf(Foo.class, foo);
	 * </pre>
	 * 
	 * @param clazz
	 *            the required class
	 * @param obj
	 *            the object to check
	 * @throws IllegalArgumentException
	 *             if the object is not an instance of clazz
	 * @see Class#isInstance
	 */
	public static void isInstanceOf(Class<?> type, Object obj) {
		isInstanceOf(type, obj, SysMessageConstants.OBJECT_IS_NOT_INSTANCEOF_CLASS);
	}

	/**
	 * Assert that the provided object is an instance of the provided class.
	 * 
	 * <pre class="code">
	 * Assert.instanceOf(Foo.class, foo);
	 * </pre>
	 * 
	 * @param type
	 *            the type to check against
	 * @param obj
	 *            the object to check
	 * @param message
	 *            a message which will be prepended to the message produced by
	 *            the function itself, and which may be used to provide context.
	 *            It should normally end in ":" or "." so that the generated
	 *            message looks OK when appended to it.
	 * @throws IllegalArgumentException
	 *             if the object is not an instance of clazz
	 * @see Class#isInstance
	 */
	public static void isInstanceOf(Class<?> type, Object obj, String message) {
		try {
			Assert.isInstanceOf(type, obj);
		} catch (IllegalArgumentException iae) {
			throw new ElidomInvalidParamsException(SysMessageConstants.INVALID_PARAM, message, SysValueUtil.newStringList(obj.getClass().getName(), type.getName()));
		}
	}

	/**
	 * Assert that an object is {@code null} .
	 * 
	 * <pre class="code">
	 * Assert.isNull(value);
	 * </pre>
	 * 
	 * @param object
	 *            the object to check
	 * @throws IllegalArgumentException
	 *             if the object is not {@code null}
	 */
	public static void isNull(Object value) {
		isNull(value, SysMessageConstants.VALUE_IS_NOT_EMPTY);
	}

	/**
	 * Assert that an object is {@code null} .
	 * 
	 * <pre class="code">
	 * Assert.isNull(value);
	 * </pre>
	 * 
	 * @param object
	 *            the object to check
	 * @param message
	 * @throws IllegalArgumentException
	 *             if the object is not {@code null}
	 */
	public static void isNull(Object value, String message) {
		if (value != null) {
			throw new ElidomInvalidParamsException(SysMessageConstants.INVALID_PARAM, message, SysValueUtil.newStringList(value.getClass().getName()));
		}
	}

	/**
	 * Assert a boolean expression, throwing {@code IllegalArgumentException} if
	 * the test result is {@code false}.
	 * 
	 * <pre class="code">
	 * Assert.isTrue(i &gt; 0);
	 * </pre>
	 * 
	 * @param expression
	 *            a boolean expression
	 * @throws IllegalArgumentException
	 *             if expression is {@code false}
	 */
	public static void isTrue(Boolean expression) {
		isTrue(expression, SysMessageConstants.BOOLEAN_IS_FALSE);
	}

	/**
	 * Assert a boolean expression, throwing {@code IllegalArgumentException} if
	 * the test result is {@code false}.
	 * 
	 * <pre class="code">
	 * Assert.isTrue(i &gt; 0, "The value must be greater than zero");
	 * </pre>
	 * 
	 * @param expression
	 *            a boolean expression
	 * @param message
	 *            the exception message to use if the assertion fails
	 * @throws IllegalArgumentException
	 *             if expression is {@code false}
	 */
	public static void isTrue(Boolean expression, String message) {
		if (!expression) {
			throw new ElidomInvalidParamsException(SysMessageConstants.INVALID_PARAM, message, SysValueUtil.newStringList(expression.toString()));
		}
	}

	/**
	 * Assert that an array has no null elements. Note: Does not complain if the
	 * array is empty!
	 * 
	 * <pre class="code">
	 * Assert.noNullElements(array);
	 * </pre>
	 * 
	 * @param array
	 *            the array to check
	 * @throws IllegalArgumentException
	 *             if the object array contains a {@code null} element
	 */
	public static void noNullElements(Object[] array) {
		noNullElements(array, SysMessageConstants.COLLECTION_HAS_NULL_ELEMENT);
	}

	/**
	 * Assert that an array has no null elements. Note: Does not complain if the
	 * array is empty!
	 * 
	 * <pre class="code">
	 * Assert.noNullElements(array, "The array must have non-null elements");
	 * </pre>
	 * 
	 * @param array
	 *            the array to check
	 * @param message
	 *            the exception message to use if the assertion fails
	 * @throws IllegalArgumentException
	 *             if the object array contains a {@code null} element
	 */
	public static void noNullElements(Object[] array, String message) {
		if (array != null) {
			for (Object element : array) {
				if (element == null) {
					throw new ElidomInvalidParamsException(SysMessageConstants.INVALID_PARAM, message);
				}
			}
		}		
	}
	
	/**
	 * Assert Not Empty
	 * 
	 * @param name
	 * @param value
	 */
	public static void notEmpty(String name, Object value) {
		assertNotEmpty(name, value);
	}

	/**
	 * Assert that a collection has elements; that is, it must not be
	 * {@code null} and must have at least one element.
	 * 
	 * <pre class="code">
	 * Assert.notEmpty(collection, "Collection must have elements");
	 * </pre>
	 * 
	 * @param collection
	 *            the collection to check
	 * @throws IllegalArgumentException
	 *             if the collection is {@code null} or has no elements
	 */
	public static void notEmpty(Collection<?> collection) {
		notEmpty(collection, SysMessageConstants.EMPTY_PARAM);
	}

	/**
	 * Assert that a Map has entries; that is, it must not be {@code null} and
	 * must have at least one entry.
	 * 
	 * <pre class="code">
	 * Assert.notEmpty(map);
	 * </pre>
	 * 
	 * @param map
	 *            the map to check
	 * @throws IllegalArgumentException
	 *             if the map is {@code null} or has no entries
	 */
	public static void notEmpty(Map<?, ?> map) {
		notEmpty(map, SysMessageConstants.EMPTY_PARAM);
	}

	/**
	 * Assert that an array has elements; that is, it must not be {@code null}
	 * and must have at least one element.
	 * 
	 * <pre class="code">
	 * Assert.notEmpty(array);
	 * </pre>
	 * 
	 * @param array
	 *            the array to check
	 * @throws IllegalArgumentException
	 *             if the object array is {@code null} or has no elements
	 */
	public static void notEmpty(Object[] array) {
		notEmpty(array, SysMessageConstants.EMPTY_PARAM);
	}

	/**
	 * Assert that a collection has elements; that is, it must not be
	 * {@code null} and must have at least one element.
	 * 
	 * <pre class="code">
	 * Assert.notEmpty(collection, "Collection must have elements");
	 * </pre>
	 * 
	 * @param collection
	 *            the collection to check
	 * @param message
	 *            the exception message to use if the assertion fails
	 * @throws IllegalArgumentException
	 *             if the collection is {@code null} or has no elements
	 */
	public static void notEmpty(Collection<?> collection, String message) {
		if (CollectionUtils.isEmpty(collection)) {
			throw new ElidomInvalidParamsException(SysMessageConstants.INVALID_PARAM, message, SysValueUtil.newStringList("Collection"));
		}
	}

	/**
	 * Assert that a Map has entries; that is, it must not be {@code null} and
	 * must have at least one entry.
	 * 
	 * <pre class="code">
	 * Assert.notEmpty(map, "Map must have entries");
	 * </pre>
	 * 
	 * @param map
	 *            the map to check
	 * @param message
	 *            the exception message to use if the assertion fails
	 * @throws IllegalArgumentException
	 *             if the map is {@code null} or has no entries
	 */
	public static void notEmpty(Map<?, ?> map, String message) {
		if (CollectionUtils.isEmpty(map)) {
			throw new ElidomInvalidParamsException(SysMessageConstants.INVALID_PARAM, message, SysValueUtil.newStringList("Map"));
		}
	}

	/**
	 * Assert that an array has elements; that is, it must not be {@code null}
	 * and must have at least one element.
	 * 
	 * <pre class="code">
	 * Assert.notEmpty(array, "The array must have elements");
	 * </pre>
	 * 
	 * @param array
	 *            the array to check
	 * @param message
	 *            the exception message to use if the assertion fails
	 * @throws IllegalArgumentException
	 *             if the object array is {@code null} or has no elements
	 */
	public static void notEmpty(Object[] array, String message) {
		if (ObjectUtils.isEmpty(array)) {
			throw new ElidomInvalidParamsException(SysMessageConstants.INVALID_PARAM, message, SysValueUtil.newStringList("Array"));
		}
	}

	/**
	 * Assert that an object is not {@code null} .
	 * 
	 * <pre class="code">
	 * Assert.notNull(clazz);
	 * </pre>
	 * 
	 * @param object
	 *            the object to check
	 * @throws IllegalArgumentException
	 *             if the object is {@code null}
	 */
	public static void notNull(Object object) {
		notNull(object, SysMessageConstants.EMPTY_PARAM);
	}

	/**
	 * Assert that an object is not {@code null} .
	 * 
	 * <pre class="code">
	 * Assert.notNull(clazz, "The class must not be null");
	 * </pre>
	 * 
	 * @param object
	 *            the object to check
	 * @param message
	 *            the exception message to use if the assertion fails
	 * @throws IllegalArgumentException
	 *             if the object is {@code null}
	 */
	public static void notNull(Object object, String message) {
		if (object == null) {
			throw new ElidomInvalidParamsException(SysMessageConstants.INVALID_PARAM, message, SysValueUtil.newStringList("Object"));
		}
	}

	/**
	 * Assert a boolean expression, throwing {@link IllegalStateException} if
	 * the test result is {@code false}.
	 * <p>
	 * Call {@link #isTrue(boolean)} if you wish to throw
	 * {@link IllegalArgumentException} on an assertion failure.
	 * 
	 * <pre class="code">
	 * Assert.state(id == null);
	 * </pre>
	 * 
	 * @param expression
	 *            a boolean expression
	 * @throws IllegalStateException
	 *             if the supplied expression is {@code false}
	 */
	public static void state(Boolean expression) {
		isTrue(expression);
	}

	/**
	 * Assert a boolean expression, throwing {@code IllegalStateException} if
	 * the test result is {@code false}. Call isTrue if you wish to throw
	 * IllegalArgumentException on an assertion failure.
	 * 
	 * <pre class="code">
	 * Assert.state(id == null, "The id property must not already be initialized");
	 * </pre>
	 * 
	 * @param expression
	 *            a boolean expression
	 * @param message
	 *            the exception message to use if the assertion fails
	 * @throws IllegalStateException
	 *             if expression is {@code false}
	 */
	public static void state(Boolean expression, String message) {
		isTrue(expression, message);
	}

}
