package com.example.demo.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class RedisReload {
    @Autowired
    private RedisProperties redisProperties;

    public GenericObjectPoolConfig<?> genericObjectPoolConfig(RedisProperties.Pool properties) {
        GenericObjectPoolConfig<?> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(properties.getMaxActive());
        config.setMaxIdle(properties.getMaxIdle());
        config.setMinIdle(properties.getMinIdle());
        if (properties.getTimeBetweenEvictionRuns() != null) {
            config.setTimeBetweenEvictionRunsMillis(properties.getTimeBetweenEvictionRuns().toMillis());
        }
        if (properties.getMaxWait() != null) {
            config.setMaxWaitMillis(properties.getMaxWait().toMillis());
        }
        return config;
    }

    @Bean(destroyMethod = "destroy")
    public LettuceConnectionFactory lettuceConnectionFactory() {

        //开启 自适应集群拓扑刷新和周期拓扑刷新
        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                // 开启全部自适应刷新
                .enableAllAdaptiveRefreshTriggers() // 开启自适应刷新,自适应刷新不开启,Redis集群变更时将会导致连接异常
                // 自适应刷新超时时间(默认30秒)
                .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(15)) //默认关闭开启后时间为30秒
                // 开周期刷新
                .enablePeriodicRefresh(Duration.ofSeconds(15))  // 默认关闭开启后时间为60秒 ClusterTopologyRefreshOptions.DEFAULT_REFRESH_PERIOD 60  .enablePeriodicRefresh(Duration.ofSeconds(2)) = .enablePeriodicRefresh().refreshPeriod(Duration.ofSeconds(2))
                .build();

        // https://github.com/lettuce-io/lettuce-core/wiki/Client-Options
        ClientOptions clientOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(clusterTopologyRefreshOptions)
                .build();

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(genericObjectPoolConfig(redisProperties.getLettuce().getPool()))
                .readFrom(ReadFrom.MASTER_PREFERRED)
                .clientOptions(clientOptions)
                .commandTimeout(redisProperties.getTimeout()) //默认RedisURI.DEFAULT_TIMEOUT 60
                .build();

        Set<RedisNode> nodes = new HashSet<RedisNode>();
        List<String> clusterNodes = redisProperties.getCluster().getNodes();
        clusterNodes.forEach(address -> nodes.add(new RedisNode(address.split(":")[0].trim(), Integer.valueOf(address.split(":")[1]))));
        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();
        clusterConfiguration.setClusterNodes(nodes);
        clusterConfiguration.setPassword(RedisPassword.of(redisProperties.getPassword()));
        clusterConfiguration.setMaxRedirects(redisProperties.getCluster().getMaxRedirects());

        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(clusterConfiguration, clientConfig);
        lettuceConnectionFactory.setShareNativeConnection(false); //是否允许多个线程操作共用同一个缓存连接，默认true，false时每个操作都将开辟新的连接
        lettuceConnectionFactory.resetConnection(); // 重置底层共享连接, 在接下来的访问时初始化
        return lettuceConnectionFactory;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
