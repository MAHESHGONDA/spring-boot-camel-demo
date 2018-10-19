package com.maple.extract.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;

import java.util.ArrayList;

public class BatchSizePredicate implements Predicate {

    public int size;

    public BatchSizePredicate(int size) {
        this.size = size;
    }

    @Override
    public boolean matches(Exchange exchange) {
        if (exchange != null) {
            ArrayList list = exchange.getIn().getBody(ArrayList.class);
            if (list != null && list.size() == size) {
                return true;
            }
        }
        return false;
    }

}