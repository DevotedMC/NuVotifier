package com.devotedmc.votifier.zeus;

import com.vexsoftware.votifier.platform.LoggingAdapter;
import org.apache.logging.log4j.Logger;

public class Log4jLogger implements LoggingAdapter {

    private Logger logger;

    public Log4jLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void error(String s) {
        logger.error(s);
    }

    @Override
    public void error(String s, Object... o) {
        logger.error(s, o);
    }

    @Override
    public void warn(String s) {
        logger.warn(s);
    }

    @Override
    public void warn(String s, Object... o) {
        logger.warn(s, o);
    }

    @Override
    public void info(String s) {
        logger.info(s);
    }

    @Override
    public void info(String s, Object... o) {
        logger.info(s, o);
    }
}
