package com.devotedmc.votifier.zeus;

import com.vexsoftware.votifier.platform.scheduler.ScheduledVotifierTask;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;

import java.util.concurrent.TimeUnit;

//TODO Zeus scheduler
public class ZeusScheduler implements VotifierScheduler {
    @Override
    public ScheduledVotifierTask sync(Runnable runnable) {
        return null;
    }

    @Override
    public ScheduledVotifierTask onPool(Runnable runnable) {
        return null;
    }

    @Override
    public ScheduledVotifierTask delayedSync(Runnable runnable, int delay, TimeUnit unit) {
        return null;
    }

    @Override
    public ScheduledVotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        return null;
    }

    @Override
    public ScheduledVotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        return null;
    }
}
