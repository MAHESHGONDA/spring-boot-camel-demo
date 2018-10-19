package com.example.demo.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.processor.aggregate.AggregationStrategy;

import java.util.ArrayList;
import java.util.List;

public class DataAggregationStrategy implements AggregationStrategy {

    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        Message newIn = newExchange.getIn();
        List user = newIn.getBody(List.class);
        List list = null;
        if (null == oldExchange) {
            list = new ArrayList();
            System.out.println(user);
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
