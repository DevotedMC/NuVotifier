package com.devotedmc.votifier.zeus;

import com.devotedmc.votifier.zeus.events.VotifierEvent;
import com.devotedmc.votifier.zeus.listeners.KiraPublishVoteListener;
import com.github.maxopoly.zeus.ZeusMain;
import com.github.maxopoly.zeus.model.yaml.ConfigSection;
import com.github.maxopoly.zeus.plugin.ZeusLoad;
import com.github.maxopoly.zeus.plugin.ZeusPlugin;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierServerBootstrap;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.platform.scheduler.ScheduledExecutorServiceVotifierScheduler;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.util.IOUtil;
import com.vexsoftware.votifier.util.KeyCreator;
import com.vexsoftware.votifier.util.TokenUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private ExecutorService eventExecutor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("Votifier Event Thread #%d").build());

    @Override
    public boolean onEnable() {
        instance = this;

        scheduler = new ScheduledExecutorServiceVotifierScheduler(Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("Votifier Scheduler Thread #%d").build()));
        pluginLogger = new Log4jLogger(logger);

        registerPluginlistener(new KiraPublishVoteListener());

        try {
            loadAndBind();
            return true;
        } catch (Exception ex) {
            try {
                halt();
                logger.error("On enable, there was a problem with the configuration. Votifier currently does nothing!", ex);
            } catch (Exception ex2) {
                logger.error("On enable, there was a problem loading, and we could not re-halt the server. Votifier is in an unstable state!", ex);
                logger.error("(halt exception)", ex2);
            }
            return false;
        }
    }

    @Override
    public void onDisable() {
        halt();
    }

    public void loadAndBind() {
        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdir()) {
                throw new RuntimeException("Unable to create the plugin data folder " + getDataFolder());
            }
        }

        // Handle configuration.
        File config = new File(getDataFolder() , "config.yml");
        File rsaDirectory = new File(getDataFolder() , "rsa");
        ConfigSection configuration;

        if (!config.exists()) {
            try {
                // First time run - do some initialization.
                logger.info("Configuring Votifier for the first time...");

                // Initialize the configuration file.
                if (!config.createNewFile()) {
                    throw new IOException("Unable to create the config file at " + config);
                }

                String cfgStr = new String(IOUtil.readAllBytes(getClass().getClassLoader().getResourceAsStream("zeusConfig.yml")), StandardCharsets.UTF_8);
                String token = TokenUtil.newToken();
                cfgStr = cfgStr.replace("%default_token%", token);
                Files.copy(new ByteArrayInputStream(cfgStr.getBytes(StandardCharsets.UTF_8)), config.toPath(), StandardCopyOption.REPLACE_EXISTING);

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

        // Load the configuration.
        getConfig().reloadConfig();
        configuration = getConfig().getConfig();

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
        ConfigSection tokenSection = configuration.getConfigSection("tokens");

        if (tokenSection != null) {
            for (String s : tokenSection.dump().keySet()) {
                tokens.put(s, KeyCreator.createKeyFrom(tokenSection.getString(s)));
                logger.info("Loaded token for website: " + s);
            }
        } else {
            String token = TokenUtil.newToken();
            configuration.createConfigSection("tokens").putString("default", token);
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
        final String host = configuration.getString("host", "0.0.0.0");
        final int port = configuration.getInt("port", 8192);

        if (configuration.hasBoolean("quiet"))
            debug = !configuration.getBoolean("quiet");
        else
            debug = configuration.getBoolean("debug", true);

        if (!debug)
            logger.info("QUIET mode enabled!");

        final boolean disablev1 = configuration.getBoolean("disable-v1-protocol", false);
        if (disablev1) {
            logger.info("------------------------------------------------------------------------------");
            logger.info("Votifier protocol v1 parsing has been disabled. Most voting websites do not");
            logger.info("currently support the modern Votifier protocol in NuVotifier.");
            logger.info("------------------------------------------------------------------------------");
        }

        this.bootstrap = new VotifierServerBootstrap(host, port, this, disablev1);
        this.bootstrap.start(err -> {});
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

        eventExecutor.submit(() -> ZeusMain.getInstance().getEventManager().broadcast(new VotifierEvent(vote)));
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
