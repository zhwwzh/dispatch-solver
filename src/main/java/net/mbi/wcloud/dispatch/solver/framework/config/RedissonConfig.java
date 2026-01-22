package net.mbi.wcloud.dispatch.solver.framework.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@RefreshScope
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:127.0.0.1}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Value("${dispatch.solver.lock-watchdog-timeout-ms:600000}")
    private long lockWatchdogTimeoutMs;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        log.info("Initializing RedissonClient, address={}, db={}, lockWatchdogTimeoutMs={}", address, database,
                lockWatchdogTimeoutMs);

        config.useSingleServer()
                .setAddress(address)
                .setDatabase(database)
                .setPassword(password == null || password.isBlank() ? null : password);

        config.setLockWatchdogTimeout(lockWatchdogTimeoutMs);

        return Redisson.create(config);
    }
}