package xyz.elidom.sys.rest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.util.DateUtil;
import xyz.elidom.util.ValueUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/log_management")
@ServiceDesc(description="Log Viewer Service API")
public class LogManagementController {
	/**
	 * Logger
	 */
	protected Logger logger = LoggerFactory.getLogger(LogManagementController.class);
	/**
	 * 로그 파일 날짜 포맷 
	 */
	private static final String LOG_FILE_DATE_FORMAT = "yyyy-MM-dd";
	/**
	 * 로그 파일 이름 
	 */
	private static final String LOG_FILE_NAME = "application.";
	/**
	 * 로그 파일 확장자  
	 */
	private static final String LOG_FILE_EXT = ".log";
	/**
	 * 로그 파일 유지 날수  
	 */
	private static final String LOG_FILE_KEEP_DATES = "log.file.keep.dates";
	/**
	 * log directory path
	 */
	private String logDirectoryPath;
	
	/**
	 * 로그 파일 리스트 
	 * 
	 * @return
	 */
	@GetMapping(value = "/list/files", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Map<String, Object>> getLogFileList() {
		String appLogDirPath = this.getLogDirectoryPath();
		File logDir = new File(appLogDirPath);
		List<Map<String, Object>> fileList = new ArrayList<Map<String, Object>>();
		
		if(logDir.isDirectory()) {
			File[] logFiles = logDir.listFiles();
			for(File logFile : logFiles) {
				String fileName = logFile.getName();
				
				if(fileName.endsWith(LOG_FILE_EXT)) {
					String logPath = logFile.getAbsolutePath();
					Long fileSize = logFile.length();
					Map<String, Object> logData = ValueUtil.newMap("id,name,size", logPath, fileName, fileSize); 
					fileList.add(logData);
				}
			}
		}
		
		fileList.sort(new LogFileComparator());
		return fileList;
	}
	
	/**
	 * 오래된 로그 파일 삭제  
	 */
	@DeleteMapping(value = "/delete/old_files", produces = MediaType.APPLICATION_JSON_VALUE)
	public void deleteOldLogFiles() {		
		int keepDate = ValueUtil.toInteger(SettingUtil.getValue(LOG_FILE_KEEP_DATES, "32"));
		Date stdDate = DateUtil.addDate(new Date(), -1 * keepDate); 
		
		List<Map<String, Object>> fileList = this.getLogFileList();
		for(Map<String, Object> fileInfo : fileList) {
			String filePath = fileInfo.get(SysConstants.ENTITY_FIELD_ID).toString();
			String dateStr = filePath.substring(filePath.indexOf(SysConstants.DOT) + 1, filePath.lastIndexOf(SysConstants.DOT));
			Date logDate = DateUtil.parse(dateStr, LOG_FILE_DATE_FORMAT);

			if(stdDate.getTime() > logDate.getTime()) {
				File logFile = new File(filePath);
				if(logFile.delete()) {
					this.logger.info("================================================");
					this.logger.info("File [" + filePath + "] Deleted");
					this.logger.info("================================================");
				} else {
					this.logger.info("================================================");
					this.logger.info("Failed to Delete Log File [" + filePath + "]");
					this.logger.info("================================================");			
				}					
			}
		}
		
		this.logger.info("==================================");
		this.logger.info("Finished delete Old log files!");
		this.logger.info("==================================");
	}
	
	/**
	 * 로그 파일 (fileName) 삭제  
	 * 
	 * @param fileName
	 * @return
	 */
	@DeleteMapping(value = "/delete/{file_name}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Boolean deleteLogFile(@PathVariable("file_name") String fileName) {
		String filePath = this.getLogFullPath(fileName);
		File logFile = new File(filePath);
		
		if(logFile.exists() && logFile.isFile()) {
			return logFile.delete();
		}
		
		return false;
	}
	
	/**
	 * 로그 파일을 읽어서 내용을 리턴
	 * 
	 * @param fileName
	 * @param lines
	 * @return 로그 파일의 내용을 읽어서 리턴  
	 */
	@GetMapping(value = "/read/{file_name}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> readLog(
			@PathVariable("file_name") String fileName, 
			@RequestParam(name = "lines", required = false) Integer lines) {
		
		String logPath = this.getLogFullPath(fileName);
		String content = (lines == null || lines == 0) ? 
				this.readAllLines(logPath) : 
				this.readLastLines(new File(logPath), lines);

		return ValueUtil.newStringMap("id,log", "1", content);
	}

	/**
	 * 최신 로그 파일을 읽어서 내용을 리턴
	 * 
	 * @param lines
	 * @return 최신 로그의 내용을 리턴 
	 */
	@GetMapping(value = "/read/latest_log", produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> readLatestLog(@RequestParam(name = "lines", required = false) Integer lines) {
		int beforeDate = -1;
		String fileName = null;
		File logFile = null;
		
		while(logFile == null || !logFile.exists()) {
			beforeDate++;
			fileName = this.getLatestLogFileName(beforeDate);
			String fileFullPath = this.getLogFullPath(fileName);
			logFile = new File(fileFullPath);
			
			if(beforeDate > 5) {
				break;
			}
		}
		
		return logFile.exists() ? this.readLog(fileName, lines) : ValueUtil.newStringMap("id,log", "1", SysConstants.EMPTY_STRING);
	}
	
	/**
	 * 로그 파일을 다운로드 
	 * 
	 * @param fileName
	 * @return filePath에 해당하는 로그의 내용을 리턴 
	 */
	@GetMapping(value = "/download/{file_name}", produces = MediaType.APPLICATION_JSON_VALUE)
	public boolean downloadLog(HttpServletRequest req, HttpServletResponse res, @PathVariable("file_name") String fileName) {
		String logPath = (fileName == null) ? this.getLatestLogFileName(0) : this.getLogFullPath(fileName);
		File file = new File(logPath);
		res.setCharacterEncoding(SysConstants.CHAR_SET_UTF8);
		res.setContentType("text/plain;charset=UTF-8");
		res.addHeader("Content-Type", "application/octet-stream");
		res.addHeader("Content-Transfer-Encoding", "binary;");
		res.addHeader("Content-Length", Long.toString(file.length()));
		res.setHeader("Pragma", "cache");
		res.setHeader("Cache-Control", "public");
		
		ServletOutputStream outStream = null;
		ByteArrayInputStream inStream = null;
		byte[] buffer = new byte[4096];
		
		try {
			res.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(file.getName(), SysConstants.CHAR_SET_UTF8));
			outStream = res.getOutputStream();
			inStream = new ByteArrayInputStream(FileUtils.readFileToByteArray(file));
			int byteCount = 0;
			
			do {
				byteCount = inStream.read(buffer);
				if(byteCount == -1) {
					break;
				}
				
				outStream.write(buffer, 0, byteCount);
				outStream.flush();
			} while(true);

		} catch (Exception e) {
			throw new RuntimeException("Failed to File Download!", e);
			
		} finally {
			try {
				inStream.close();
			} catch (IOException e) {
			}
			
			try {
				outStream.close();
			} catch (IOException e) {
			}
		}
		
		return true;		
	}
	
	/**
	 * 로그 파일을 다운로드 
	 * 
	 * @param req
	 * @param res
	 * @return 오늘의 로그의 내용을 리턴 
	 */
	@GetMapping(value = "/download/latest_log", produces = MediaType.APPLICATION_JSON_VALUE)
	public boolean downloadLog(HttpServletRequest req, HttpServletResponse res) {
		return this.downloadLog(req, res, null);
	}
	
	/**
	 * Log File Comparator
	 * 
	 * @author shortstop
	 */
	class LogFileComparator implements Comparator<Map<String, Object>> {
		@Override
		public int compare(Map<String, Object> data1, Map<String, Object> data2) {
			String name1 = data1.get(SysConstants.ENTITY_FIELD_NAME).toString();
			String name2 = data2.get(SysConstants.ENTITY_FIELD_NAME).toString();
			return name2.compareTo(name1);
		}
	}
	
	/**
	 * Log 디렉토리 Path를 리턴 
	 * 
	 * @return
	 */
	public String getLogDirectoryPath() {
		if(this.logDirectoryPath == null) {
			this.logDirectoryPath = System.getProperty("user.dir") + File.separator + "logs";
		}
		
		return this.logDirectoryPath;
	}
	
	/**
	 * 로그 파일의 Full Path를 리턴 
	 * 
	 * @param fileName
	 * @return
	 */
	public String getLogFullPath(String fileName) {
		return new StringBuffer(this.getLogDirectoryPath()).append(File.separator).append(fileName).append(fileName.endsWith(LOG_FILE_EXT) ? SysConstants.EMPTY_STRING : LOG_FILE_EXT).toString();
	}
	
	/**
	 * 최근 로그 파일 이름을 찾는다.
	 * 
	 * @param minusDateFromToday
	 * @return
	 */
	public String getLatestLogFileName(int minusDateFromToday) {
		Date today = (minusDateFromToday == 0) ? new Date() : DateUtil.addDate(new Date(), (-1 * minusDateFromToday)); 
		StringBuffer logPath = new StringBuffer();
		logPath.append(LOG_FILE_NAME).append(DateUtil.dateTimeStr(today, LOG_FILE_DATE_FORMAT)).append(LOG_FILE_EXT);
		return logPath.toString();
	}
	
	/**
	 * 마지막 lines 라인을 읽어 리턴 
	 * 
	 * @param file
	 * @param lines
	 * @return
	 */
	public String readLastLines(File file, int lines) {
		StringBuilder builder = new StringBuilder();
		ReversedLinesFileReader rlfr = null;
		
		try {
			rlfr = new ReversedLinesFileReader(file, Charset.forName(SysConstants.CHAR_SET_UTF8));
			String temp = null;
			int counter = 0;

			while(counter < lines) {
				try {
					temp = rlfr.readLine();
				} catch (NullPointerException npe) {
					break;
				}
				
				if(temp != null) {
					builder.insert(0, temp + SysConstants.LINE_SEPARATOR);
					counter++;
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read log file!", e);
			
		} finally {
			try {
				rlfr.close();
			} catch (IOException e) {
			}
		}
		
		return builder.toString();
	}
	
	/**
	 * 파일의 모든 라인 읽어 리턴 
	 * 
	 * @param logPath
	 * @return
	 */
	public String readAllLines(String logPath) {
		StringBuffer content = new StringBuffer();
		FileReader fReader = null;
		BufferedReader bReader = null;

		try {
			fReader = new FileReader(logPath);
			bReader = new BufferedReader(fReader);
			String temp = null;
			
			while((temp = bReader.readLine()) != null) {
			    content.append(temp).append(SysConstants.LINE_SEPARATOR);
			}
			
			return content.toString();
			
		} catch(FileNotFoundException e) {
			throw new RuntimeException("Log file [" + logPath + "] not found!", e);
			
		} catch (Exception e) {
			throw new RuntimeException("Log file [" + logPath + "] not found!", e);
			
		} finally {
			if(bReader != null) {
				try {
					bReader.close();
				} catch (IOException e) {
				}
			}
			if(fReader != null) {
				try {
					fReader.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
}
