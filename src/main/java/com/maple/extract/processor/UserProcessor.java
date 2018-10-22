package com.maple.extract.processor;

import com.maple.extract.model.Authetication;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.Base64;

public class UserProcessor implements Processor {

    private String restApiName;

    public UserProcessor(String restApiName) {
        this.restApiName = restApiName;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        if ("Authentication".equalsIgnoreCase(restApiName)) {
            String baseString = (String) exchange.getContext().resolvePropertyPlaceholders("{{api.client-id}}") + ":" +
                    (String) exchange.getContext().resolvePropertyPlaceholders("{{api.secret-key}}");
            String base64String = Base64.getEncoder().encodeToString(baseString.getBytes());
            exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
            exchange.getIn().setHeader("content-type", exchange.getContext().resolvePropertyPlaceholders("{{api.content-type}}"));
            exchange.getIn().setHeader("authorization", "Basic " + base64String);
            exchange.getIn().setBody(exchange.getContext().resolvePropertyPlaceholders("{{api.auth-api-body}}"));
        } else if ("GetUsers".equalsIgnoreCase(restApiName)) {
            exchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
            exchange.getIn().setHeader("cache-control", exchange.getContext().resolvePropertyPlaceholders("{{api.cache-control}}"));
            Authetication auth = exchange.getIn().getBody(Authetication.class);
            exchange.getIn().setHeader("Authorization", "Bearer " + auth.getAccess_token());
        }

    }
}
