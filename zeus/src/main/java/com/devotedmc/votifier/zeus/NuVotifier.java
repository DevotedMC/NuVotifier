package com.devotedmc.votifier.zeus;

import com.github.maxopoly.zeus.plugin.ZeusLoad;
import com.github.maxopoly.zeus.plugin.ZeusPlugin;
import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierServerBootstrap;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;

import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

@ZeusLoad(name = "NuVotifier", version = "@version@", description = "Vote listener plugin for the central authority program Zeus.")
public class NuVotifier extends ZeusPlugin implements VoteHandler, VotifierPlugin {

    private static NuVotifier instance;

    public static NuVotifier getInstance() {
        return instance;
    }

    private VotifierServerBootstrap bootstrap;
    private KeyPair keyPair;
    private boolean debug;
    private Map<String, Key> tokens = new HashMap<>();
    private VotifierScheduler scheduler;
    private LoggingAdapter pluginLogger;

    @Override
    public boolean onEnable() {
        instance = this;

        return true;
    }

    @Override
    public void onDisable() {

    }

    @Override
    public Map<String, Key> getTokens() {
        return tokens;
    }

    @Override
    public KeyPair getProtocolV1Key() {
        return keyPair;
    }

    @Override
    public LoggingAdapter getPluginLogger() {
        return pluginLogger;
    }

    @Override
    public VotifierScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public void onVoteReceived(Vote vote, VotifierSession.ProtocolVersion protocolVersion, String remoteAddress) throws Exception {

    }

    @Override
    public void onError(Throwable throwable, boolean voteAlreadyCompleted, String remoteAddress) {

    }
}
