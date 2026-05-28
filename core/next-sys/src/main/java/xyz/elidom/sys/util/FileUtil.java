/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.SysConstants;

/**
 * File 관련 Utilities
 * 
 * @author shortstop
 */
public class FileUtil {

	/**
	 * File 생성.
	 * 
	 * @param dirPath
	 * @param fileName
	 * @param content
	 */
	public static void createFile(String dirPath, String fileName, String content) {
		// 1. Directory 체크 후 존재하지 않으면 생성
		File dir = new File(dirPath);
		
		if (!dir.exists()) {
			dir.mkdirs();
		}
		
		// 
		if (!dirPath.endsWith(File.separator)) {
			dirPath += File.separator;
		}
		
		File file = new File(dirPath, fileName);
		try (FileWriter fileWriter = new FileWriter(file); BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
			bufferedWriter.write(content);
		} catch (IOException e) {
			throw ThrowUtil.newFailToCreateFile();
		}
	}

	/**
	 * File 읽어서 내용을 String으로 리턴
	 * 
	 * @param dirPath
	 * @param fileName
	 * @return
	 */
	public static String readFileContent(String dirPath, String fileName) {
		return FileUtil.readFileContent(dirPath, fileName, null);
	}

	/**
	 * dirPath내 파일명이 fileName인 파일을 인코딩을 encoding으로 적용하여 읽어서 내용을 리턴
	 * 
	 * @param dirPath
	 * @param fileName
	 * @param encoding
	 * @return
	 */
	public static String readFileContent(String dirPath, String fileName, String encoding) {
		File file = new File(dirPath, fileName);
		if (!file.exists()) {
			throw ThrowUtil.newNotFoundFile(file.getPath());
		}

		try {
			return new String(Files.readAllBytes(Paths.get(file.toString())), encoding == null ? SysConstants.CHAR_SET_UTF8 : encoding);
		} catch (IOException e) {
			throw ThrowUtil.newFailToReadFileContent(file.getPath());
		}
	}

	/**
	 * path 내의 파일 내용 불러오기.
	 *
	 * @param path
	 * @return
	 */
	public static String readClassPathResource(String path) {
		if (SysValueUtil.isEmpty(path)) {
			return null;
		}

		try {
			return IOUtils.toString(new ClassPathResource(path.trim()).getInputStream(), SysConstants.CHAR_SET_UTF8);
		} catch(IOException ioe) {
			throw ThrowUtil.newFailToReadFileContent(path); 
		}
	}

	/**
	 * Classpath 상에 있는 파일 packageName, fileName으로 파일을 찾아 파일 내용을 읽어 리턴
	 * 
	 * @param packageName
	 * @param fileName
	 * @return
	 */
	public static String readClasspathFile(String packageName, String fileName) {
		String classpath = packageName + OrmConstants.SLASH + fileName;
		return readClassPathResource(classpath);
	}
	
}