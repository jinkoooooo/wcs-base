/* Copyright © Nearsolution Inc. All rights reserved. */
/**
 * 
 */
package xyz.elidom.sys.system.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Cache Configuration 
 * - Redis를 사용하도록 설정되었다면 Redis Cache 설정이 자동으로 되므로 설정이 필요없다.
 * - 다른 Cache를 사용하려면 설정해야 한다.
 * 
 * @author Minu.Kim
 */
@Configuration
@EnableCaching
public class CacheConfig {
}