/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.reflect.FieldUtils;

/**
 * Class 관련 유틸리티 클래스
 * 
 * @author shortstop
 */
public class ClassUtil {

	/**
	 * class load by className
	 * 
	 * @param className
	 * @return
	 */
	public static Class<?> forName(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Class (" + className + ") not found.", e);
		}
	}

	/**
	 * new instance by clazz
	 * 
	 * @param clazz
	 * @return
	 */
	public static Object newInstance(Class<?> clazz) {
		try {
			//deprecated
//			return clazz.newInstance();
			return clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Class could not be instantiated (" + clazz.getName() + ")", e);
		}
	}

	/**
	 * new instance by clazz name
	 * 
	 * @param className
	 * @return
	 */
	public static Object newInstance(String className) {
		return newInstance(forName(className));
	}

	/**
	 * clazz에 필드 fieldName가 존재하는지 체크
	 * 
	 * @param clazz
	 * @param fieldName
	 * @return
	 */
	public static boolean hasField(Class<?> clazz, String fieldName) {
		boolean isExist = false;

		try {
			clazz.getDeclaredField(fieldName);
			isExist = true;
		} catch (Exception e) {
		}

		if (!isExist && clazz.getSuperclass() != null) {
			isExist = hasField(clazz.getSuperclass(), fieldName);
		}

		return isExist;
	}

	/**
	 * Class에 소속된 필드를 찾아 리턴
	 * 
	 * @param clazz
	 * @param fieldName
	 * @return
	 */
	public static Field getField(Class<?> clazz, String fieldName) {
		return FieldUtils.getField(clazz, fieldName, true);
	}

	/**
	 * Get All Fields
	 * 
	 * @param clazz
	 * @return
	 */
	public static List<Field> getAllFields(Class<?> clazz) {
		return ClassUtil.getAllFields(new ArrayList<Field>(), clazz);
	}

	/**
	 * clazz의 모든 필드를 찾아서 fields 리스트에 추가 
	 * 
	 * @param fields
	 * @param clazz
	 * @return
	 */
	public static List<Field> getAllFields(List<Field> fields, Class<?> clazz) {
		Field[] declaredFields = clazz.getDeclaredFields();
		List<Field> fieldList = Arrays.asList(declaredFields);
		fields.addAll(fieldList);

		if (clazz.getSuperclass() != null) {
			getAllFields(fields, clazz.getSuperclass());
		}

		return fields;
	}

	/**
	 * clazz와 자신의 super class의 모든 declared fields들 중에 annotation과 매치되는 annotation을 가지는 모든 필드들을 추출
	 * 
	 * @param annotatedFields
	 * @param clazz
	 * @param annotation
	 * @return
	 */
	public static <A extends Annotation> List<Field> extractAllAnnotatedFields(List<Field> annotatedFields, Class<?> clazz, Class<A> annotation) {
		Field[] declaredFields = clazz.getDeclaredFields();

		for (int i = 0; i < declaredFields.length; i++) {
			Field field = declaredFields[i];
			if (field.isAnnotationPresent(annotation)) {
				annotatedFields.add(field);
			}
		}

		if (clazz.getSuperclass() != null) {
			extractAllAnnotatedFields(annotatedFields, clazz.getSuperclass(), annotation);
		}

		return annotatedFields;
	}

	/**
	 * 객체 내의 Field 값 가져오기를 실행.
	 * 
	 * @param instance
	 * @param fieldName
	 * @return
	 */
	public static Object getFieldValue(Object instance, String fieldName) {
		return getFieldValue(instance, instance.getClass(), fieldName);
	}

	/**
	 * 객체 내의 Field 값 가져오기를 실행.
	 * 
	 * @param instance
	 * @param sourceClass
	 * @param fieldName
	 * @return
	 */
	public static Object getFieldValue(Object instance, Class<?> sourceClass, String fieldName) {
		Field field = FieldUtils.getField(sourceClass, fieldName, true);
		field = ValueUtil.checkValue(field, FieldUtils.getField(sourceClass, ValueUtil.toCamelCase(fieldName, '_'), true));
		return (field == null) ? null : getFieldValue(instance, field);
	}

	/**
	 * 객체 내의 Field 값 가져오기를 실행.
	 * 
	 * @param instance
	 * @param field
	 * @return
	 */
	public static Object getFieldValue(Object instance, Field field) {
		try {
			field.setAccessible(true);
			return field.get(instance);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to assign value from field (" + field.getName() + ") of class (" + instance.getClass().getName() + ")", e);
		}
	}

	/**
	 * instance의 fieldName에 value를 설정한다.
	 * 
	 * @param instance
	 * @param fieldName
	 * @param value
	 */
	public static void setFieldValue(Object instance, String fieldName, Object value) {
		try {
			FieldUtils.writeField(instance, fieldName, value, true);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Failed to assign value from field (" + fieldName + ") of class (" + instance.getClass().getName() + ")", e);
		}
	}

	/**
	 * instance의 fieldName에 value를 설정한다.
	 * 
	 * @param instance
	 * @param field
	 * @param value
	 */
	public static void setFieldValue(Object instance, Field field, Object value) {
		try {
			FieldUtils.writeField(field, instance, value, true);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Failed to assign value to field (" + field.getName() + ") of class (" + instance.getClass().getName() + ")", e);
		}
	}
}