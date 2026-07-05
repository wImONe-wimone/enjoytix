package com.wimone.enjoytix.framework.cache.config;

import io.netty.channel.socket.DatagramChannel;
import io.netty.resolver.AddressResolverGroup;
import io.netty.resolver.DefaultAddressResolverGroup;
import io.netty.resolver.dns.DnsServerAddressStreamProvider;
import org.redisson.config.Config;
import org.redisson.connection.AddressResolverGroupFactory;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.net.InetSocketAddress;

@AutoConfiguration(beforeName = "org.redisson.spring.starter.RedissonAutoConfiguration")
@ConditionalOnClass({Config.class, RedissonAutoConfigurationCustomizer.class, DefaultAddressResolverGroup.class})
@ConditionalOnProperty(
        prefix = "framework.cache.redis.redisson",
        name = "address-resolver",
        havingValue = "jvm",
        matchIfMissing = true
)
public class RedissonDnsResolverAutoConfiguration {

    @Bean
    public RedissonAutoConfigurationCustomizer redissonJvmDnsResolverCustomizer() {
        return config -> config.setAddressResolverGroupFactory(new JvmDnsAddressResolverGroupFactory());
    }

    private static final class JvmDnsAddressResolverGroupFactory implements AddressResolverGroupFactory {

        @Override
        public AddressResolverGroup<InetSocketAddress> create(
                Class<? extends DatagramChannel> channelType,
                DnsServerAddressStreamProvider nameServerProvider) {
            return DefaultAddressResolverGroup.INSTANCE;
        }
    }
}
