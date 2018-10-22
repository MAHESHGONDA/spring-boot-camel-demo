package com.maple.extract.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.processor.aggregate.AggregationStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataAggregationStrategy implements AggregationStrategy {

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        Message newIn = newExchange.getIn();
        Map user = newIn.getBody(Map.class);
        List list = null;
        if (null == oldExchange) {
            list = new ArrayList();
            list.add(user);
            newIn.setBody(list);
            return newExchange;
        } else {
            Message in = oldExchange.getIn();
            list = in.getBody(ArrayList.class);
            list.add(user);
            return oldExchange;
        }
    }
}
