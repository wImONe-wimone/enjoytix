package com.wimone.enjoytix.framework.web.config;

import com.wimone.enjoytix.framework.base.trace.TraceConstants;
import com.wimone.enjoytix.framework.web.GlobalExceptionHandler;
import com.wimone.enjoytix.framework.web.filter.TraceIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
public class WebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistrationBean() {
        FilterRegistrationBean<TraceIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TraceIdFilter());
        registrationBean.setOrder(Integer.MIN_VALUE);
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }

    @Bean
    @ConditionalOnClass(WebMvcConfigurer.class)
    @ConditionalOnProperty(prefix = "enjoytix.web.cors", name = "enabled", havingValue = "true", matchIfMissing = true)
    public WebMvcConfigurer enjoyTixCorsConfigurer() {
        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders(TraceConstants.REQUEST_ID_HEADER)
                        .maxAge(3600);
            }
        };
    }
}
