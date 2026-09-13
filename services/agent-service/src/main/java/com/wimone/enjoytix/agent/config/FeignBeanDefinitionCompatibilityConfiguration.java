package com.wimone.enjoytix.agent.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

@Configuration(proxyBeanMethods = false)
public class FeignBeanDefinitionCompatibilityConfiguration implements BeanDefinitionRegistryPostProcessor, Ordered {
    private static final String FACTORY_BEAN_OBJECT_TYPE = "factoryBeanObjectType";

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition definition = registry.getBeanDefinition(beanName);
            Object objectType = definition.getAttribute(FACTORY_BEAN_OBJECT_TYPE);
            if (objectType instanceof String className) {
                try {
                    definition.setAttribute(FACTORY_BEAN_OBJECT_TYPE,
                            ClassUtils.forName(className, ClassUtils.getDefaultClassLoader()));
                } catch (ClassNotFoundException ignored) {
                    definition.removeAttribute(FACTORY_BEAN_OBJECT_TYPE);
                }
            }
        }
    }

    @Override
    public void postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) {
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
