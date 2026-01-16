package net.mbi.wcloud.dispatch.solver.framework.lock;

public interface DistributedLock {

    boolean tryLock(String key, int ttlSeconds);

    void unlock(String key);
}
