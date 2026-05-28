package xyz.elidom.sys.task;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.context.DomainContext;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.util.ValueUtil;

@Service
public class ServiceLogRemoveTask {

	@Autowired
	private Environment env;
	
	private Logger logger = LoggerFactory.getLogger(ServiceLogRemoveTask.class);

	@Scheduled(cron = "0 0 0 * * ?")
	public void executeTask() {
		// 수정1. 시스템 도메인의 설정에서 Keep할 날짜를 추출
		Domain systemDomain = Domain.systemDomain();
		DomainContext.setCurrentDomain(systemDomain);
		try {
			this.deleteLogFiles(systemDomain);
		} catch(Exception e) {
			this.logger.error("Failed to delete log file!", e);
		} finally {
			DomainContext.unsetAll();
		}
	}
	
	/**
	 * Keep할 시간이 넘어간 로그 파일을 삭제
	 *  
	 * @param domain
	 */
	private void deleteLogFiles(Domain domain) {
		String defaultPath = "./logs/service,./logs/http_invoke,./logs";
		String values = env.getProperty(SysConfigConstants.SERVICE_LOG_DELETE_DIRECTORIES, defaultPath);
		List<String> paths = Arrays.asList(StringUtils.tokenizeToStringArray(values, SysConstants.COMMA));
		
		// FIXME SysConfigConstants.SERVICE_LOG_DELETE_PERIOD_DAY 설정도 SysConfigConstants.SERVICE_LOG_DELETE_DIRECTORIES처럼 Environment에서 관리하도록 수정 
		int period = ValueUtil.toInteger(SettingUtil.getValue(domain.getId(), SysConfigConstants.SERVICE_LOG_DELETE_PERIOD_DAY), 32);

		for (String path : paths) {
			try {
				File logFiles = new File(path);
				if (!logFiles.exists()) {
					continue;
				}

				// 수정2. logFiles.isDirectory() -> !logFiles.isDirectory()
				if (!logFiles.isDirectory()) {
					continue;
				}

				/*
				 * 설정된 기한 이전에 생성된 파일 목록 추출.
				 */
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(new Date());
				calendar.add(Calendar.DAY_OF_MONTH, -period);
				Date baseDate = calendar.getTime();

				File[] files = logFiles.listFiles(new FileFilter() {
					@Override
					public boolean accept(File file) {
						return baseDate.compareTo(new Date(file.lastModified())) > 0;
					}
				});
				
				/*
				 * File 삭제
				 */
				for (File file : files) {
					try {
						file.delete();
					} catch (Exception e) {
						this.logger.error("Failed to delete log file [" + file.getName() + "]!", e);
					}
				}
			} catch (Exception e) {
				this.logger.error("Failed to delete log file!", e);
			}
		}
	}
}
