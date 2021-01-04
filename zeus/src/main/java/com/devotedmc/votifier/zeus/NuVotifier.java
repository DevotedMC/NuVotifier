package com.devotedmc.votifier.zeus;

import com.devotedmc.votifier.zeus.events.VotifierEvent;
import com.devotedmc.votifier.zeus.listeners.KiraPublishVoteListener;
import com.github.maxopoly.zeus.ZeusMain;
import com.github.maxopoly.zeus.model.yaml.ConfigSection;
import com.github.maxopoly.zeus.plugin.ZeusLoad;
import com.github.maxopoly.zeus.plugin.ZeusPlugin;
import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierServerBootstrap;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.util.KeyCreator;
import com.vexsoftware.votifier.util.TokenUtil;

import java.io.File;
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

        //TODO make a getCommandHandler method and register our commands here
        registerPluginlistener(new KiraPublishVoteListener());

        return loadAndBind();
    }

    @Override
    public void onDisable() {
        halt();
    }

    public boolean loadAndBind() {
        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdir()) {
                throw new RuntimeException("Unable to create the plugin data folder " + getDataFolder());
            }
        }

        // Handle configuration.
        ConfigSection config = getConfig().getConfig();
        File rsaDirectory = new File(getDataFolder() , "rsa");
        getConfig().saveDefaultConfig();

        if (true) { //TODO fix this
            try {
                // First time run - do some initialization.
                logger.info("Configuring Votifier for the first time...");

                String token = TokenUtil.newToken();
                config.getConfigSection("tokens").putString("default", token);
                getConfig().saveConfig();

                /*
                 * Remind hosted server admins to be sure they have the right
                 * port number.
                 */
                logger.info("------------------------------------------------------------------------------");
                logger.info("Assigning NuVotifier to listen on port 8192. If you are hosting Zeus on a");
                logger.info("shared server please check with your hosting provider to verify that this port");
                logger.info("is available for your use. Chances are that your hosting provider will assign");
                logger.info("a different port, which you need to specify in config.yml");
                logger.info("------------------------------------------------------------------------------");
                logger.info("Assigning NuVotifier to listen to interface 0.0.0.0. This is usually alright,");
                logger.info("however, if you want NuVotifier to only listen to one interface for security ");
                logger.info("reasons (or you use a shared host), you may change this in the config.yml.");
                logger.info("------------------------------------------------------------------------------");
                logger.info("Your default Votifier token is " + token + ".");
                logger.info("You will need to provide this token when you submit your server to a voting");
                logger.info("list.");
                logger.info("------------------------------------------------------------------------------");
            } catch (Exception ex) {
                throw new RuntimeException("Unable to create configuration file", ex);
            }
        }

        /*
         * Create RSA directory and keys if it does not exist; otherwise, read
         * keys.
         */
        try {
            if (!rsaDirectory.exists()) {
                if (!rsaDirectory.mkdir()) {
                    throw new RuntimeException("Unable to create the RSA key folder " + rsaDirectory);
                }
                keyPair = RSAKeygen.generate(2048);
                RSAIO.save(rsaDirectory, keyPair);
            } else {
                keyPair = RSAIO.load(rsaDirectory);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error reading RSA tokens", ex);
        }

        // Load Votifier tokens.
        ConfigSection tokenSection = config.getConfigSection("tokens");

        if (tokenSection != null) {
            for (String s : tokenSection.dump().keySet()) {
                tokens.put(s, KeyCreator.createKeyFrom(tokenSection.getString(s)));
                logger.info("Loaded token for website: " + s);
            }
        } else {
            String token = TokenUtil.newToken();
            config.createConfigSection("tokens").putString("default", token);
            tokens.put("default", KeyCreator.createKeyFrom(token));
            getConfig().saveConfig();
            logger.info("------------------------------------------------------------------------------");
            logger.info("No tokens were found in your configuration, so we've generated one for you.");
            logger.info("Your default Votifier token is " + token + ".");
            logger.info("You will need to provide this token when you submit your server to a voting");
            logger.info("list.");
            logger.info("------------------------------------------------------------------------------");
        }

        // Initialize the receiver.
        final String host = config.getString("host", "0.0.0.0");
        final int port = config.getInt("port", 8192);

        if (config.hasBoolean("quiet"))
            debug = !config.getBoolean("quiet");
        else
            debug = config.getBoolean("debug", true);

        if (!debug)
            logger.info("QUIET mode enabled!");

        final boolean disablev1 = config.getBoolean("disable-v1-protocol", false);
        if (disablev1) {
            logger.info("------------------------------------------------------------------------------");
            logger.info("Votifier protocol v1 parsing has been disabled. Most voting websites do not");
            logger.info("currently support the modern Votifier protocol in NuVotifier.");
            logger.info("------------------------------------------------------------------------------");
        }

        this.bootstrap = new VotifierServerBootstrap(host, port, this, disablev1);
        this.bootstrap.start(err -> {});

        return true;
    }

    public void halt() {
        if (bootstrap != null) {
            bootstrap.shutdown();
            bootstrap = null;
        }
    }

    public boolean reload() {
        try {
            halt();
        } catch (Exception ex) {
            logger.error("On halt, an exception was thrown. This may be fine!", ex);
        }

        try {
            loadAndBind();
            logger.info("Reload was successful.");
            return true;
        } catch (Exception ex) {
            try {
                halt();
                logger.error("On reload, there was a problem with the configuration. Votifier currently does nothing!", ex);
            } catch (Exception ex2) {
                logger.error("On reload, there was a problem loading, and we could not re-halt the server. Votifier is in an unstable state!", ex);
                logger.error("(halt exception)", ex2);
            }
            return false;
        }
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
    public void onVoteReceived(Vote vote, VotifierSession.ProtocolVersion protocolVersion, String remoteAddress) {
        if (debug) {
            if (protocolVersion == VotifierSession.ProtocolVersion.ONE) {
                logger.info("Got a protocol v1 vote record from " + remoteAddress + " -> " + vote);
            } else {
                logger.info("Got a protocol v2 vote record from " + remoteAddress + " -> " + vote);
            }
        }

        ZeusMain.getInstance().getEventManager().broadcast(new VotifierEvent(vote)); //TODO add and use a scheduler from Zeus
    }

    @Override
    public void onError(Throwable throwable, boolean voteAlreadyCompleted, String remoteAddress) {
        if (debug) {
            if (voteAlreadyCompleted) {
                logger.error("Vote processed, however an exception " +
                        "occurred with a vote from " + remoteAddress, throwable);
            } else {
                logger.error("Unable to process vote from " + remoteAddress, throwable);
            }
        } else if (!voteAlreadyCompleted) {
            logger.error("Unable to process vote from " + remoteAddress);
        }
    }
}
