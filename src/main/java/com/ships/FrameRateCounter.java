package com.ships;

final class FrameRateCounter {
    private static final long WINDOW_NANOS = 1_000_000_000L;
    private long windowStartedAt;
    private int framesInWindow;
    private int displayedFrames;

    FrameRateCounter(long nowNanos) {
        windowStartedAt = nowNanos;
    }

    void reset(long nowNanos) {
        windowStartedAt = nowNanos;
        framesInWindow = 0;
        displayedFrames = 0;
    }

    boolean frame(long nowNanos) {
        framesInWindow++;
        if (nowNanos - windowStartedAt < WINDOW_NANOS) return false;
        displayedFrames = framesInWindow;
        framesInWindow = 0;
        windowStartedAt = nowNanos;
        return true;
    }

    int displayedFrames() {
        return displayedFrames;
    }
}
