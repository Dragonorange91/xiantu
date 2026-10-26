package com.longju.xiantu.client;

public final class InnerDemonClientState {
    private static volatile boolean locked;
    private InnerDemonClientState() {}
    public static boolean locked() { return locked; }
    public static void setLocked(boolean value) { locked = value; }
}
