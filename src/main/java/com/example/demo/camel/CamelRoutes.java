package com.example.demo.camel;

import com.example.demo.model.User;
import com.example.demo.model.UserResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CamelRoutes extends RouteBuilder {

    @Autowired
    ObjectMapper mapper;

    @Override
    public void configure() {
        analyticsTrigger();
        getUsersTrigger();
    }

    private void analyticsTrigger() {
        from("{{analytics.trigger-route}}")
            .routeId("analytics.trigger-route")
            .pollEnrich("{{analytics.read-file-uri}}")
            .split(body().tokenize("\n")).streaming()
                .filter().simple("${headers.CamelSplitIndex}  > 0")
                    .log("Line: ${body}")
                    .process(ex -> {
                        String body = ex.getIn().getBody(String.class);
                        String[] values = body.split(",");

                        Map<String, Object> user = new HashMap<>();
                        user.put("id", values[0]);
                        user.put("used_directory_search_last_access_timestamp", values[12]);

                        ex.getIn().setBody(user);
                    })
                    .to("{{analytics.insert-db-uri}}")
                .end()
            ;
    }

    private void getUsersTrigger() {
        from("{{api.trigger-route}}")
                .routeId("api.trigger-route")
                .setHeader("CamelHttpMethod", constant("GET"))
                .to("{{api.api-endpoint}}")
                .unmarshal().json(JsonLibrary.Jackson, UserResponse.class)
                .setBody(simple("${body.content}"))
                .split(body()).streaming()
                .process(ex -> {
                    User user = ex.getIn().getBody(User.class);
                    System.out.println(user);
                    Map<String, Object> dbUser = new HashMap<>();
                    dbUser.put("id", user.getId());
                    dbUser.put("creationTime", user.getCreationTime());
                    dbUser.put("lastUpdateTime", user.getLastUpdated());
                    ex.getIn().setBody(dbUser);
                })
                .to("{{api.insert-user-uri}}")
                .end();
    }
}
