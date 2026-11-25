package com.cmpt276.frasercare;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

// Caches Health Canada drug lookups; expiry is set by spring.cache.caffeine.spec
@Configuration
@EnableCaching
public class CacheConfig {
}
