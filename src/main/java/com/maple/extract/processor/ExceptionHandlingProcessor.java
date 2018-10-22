package com.maple.extract.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;


public class ExceptionHandlingProcessor implements Processor {

    Logger log = Logger.getLogger(ExceptionHandlingProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        log.error("Exception in Route: api.trigger-route, during API call : " + ExceptionUtils.getStackTrace(cause));
        exchange.getContext().getShutdownStrategy().setLogInflightExchangesOnTimeout(false);
        exchange.getContext().getShutdownStrategy().setTimeout(30);
        exchange.getContext().stop();
    }
}

