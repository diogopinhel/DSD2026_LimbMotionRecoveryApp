package com.dsd.s1.model;

public class ServiceConfig {
    public final int reconnectDelaySec;
    public final int timeoutSec;

    public ServiceConfig() {
        this(2, 10);
    }

    public ServiceConfig(int reconnectDelaySec, int timeoutSec) {
        this.reconnectDelaySec = reconnectDelaySec;
        this.timeoutSec = timeoutSec;
    }
}