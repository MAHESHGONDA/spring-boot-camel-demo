package com.example.demo.camel;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CamelRoutes extends RouteBuilder {
    @Override
    public void configure() {
        analyticsTrigger();
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
}
