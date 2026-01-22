package net.mbi.wcloud.dispatch.solver.framework.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonDistributedLock implements DistributedLock {

    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "dispatch:solver:lock:";

    @Override
    public boolean tryLock(String key, int ttlSeconds) {
        String lockKey = LOCK_PREFIX + key;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // waitTime=0：不等待（立即返回）
            // leaseTime:
            // - ttlSeconds > 0：使用固定 TTL，避免死锁
            // - ttlSeconds <= 0：使用 watchdog 自动续期（-1）
            long leaseTime = ttlSeconds > 0 ? ttlSeconds : -1L;
            return lock.tryLock(0L, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Lock interrupted: {}", lockKey, e);
            return false;
        } catch (Exception e) {
            log.error("Lock error: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public void unlock(String key) {
        String lockKey = LOCK_PREFIX + key;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            } else {
                log.warn("Unlock skipped (not held by current thread): {}", lockKey);
            }
        } catch (Exception e) {
            log.error("Unlock error: {}", lockKey, e);
        }
    }
}
