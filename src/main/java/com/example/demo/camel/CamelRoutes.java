package com.example.demo.camel;

import com.example.demo.model.User;
import com.example.demo.model.UserResponse;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CamelRoutes extends RouteBuilder {

    private static final int MAX_RECORDS = 900;
    @Override
    public void configure() {
        analyticsTrigger();
        getUsersTrigger();
        userExtractionTrigger();
        commonRoute();
    }

    private void commonRoute() {
        from("{{common.trigger-route}}").routeId("common.trigger-route")
                 .to("{{api.trigger-route}}")
               // .to("{{analytics.trigger-route}}")
               // .to("{{extraction.trigger-route}}")
                .end();
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
                .log("Lin: ${headers}")

                .process(exchange -> exchange.getIn().setBody("{\"grant_type\": \"client_credentials\", \"scope\": \"idp.link.okta_pcid_report\"}")) //TODO use a request class
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("content-type", constant("application/x-www-form-urlencoded"))
                .setHeader("authorization", constant("Basic ODBhZjJkMzItNmFiOC00ZmZjLWE0MjgtZWNlNjdiNzkzOTE2OmJhNzVkMWYyLThiYjYtNDFmMC1iMmUzLTI4ODRjNWZkMzhhYQ==")) //TODO Get Base 64
                .setHeader("accept", constant("*/*"))
                .setHeader("accept-encoding", constant("gzip, deflate"))
                .setHeader("content-length", constant("53"))

               // .setBody(constant("grant_type: client_credentials, scope: idp.link.okta_pcid_report"))
              //  .to("{{api.access-token-uri}}")
                .log("value:::::${body}")
                .setBody(constant(""))
               // .unmarshal().json(JsonLibrary.Jackson, Object.class)
             //   .setBody(simple("${body.access_token}"))
                .setHeader("page", constant(0))
                .loopDoWhile(simple("${header.page} == 0 || ${header.isLast} != true"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("cache-control", constant("no-cache"))
              //  .setHeader("Authorization", simple("${body.access_token}"))
                .recipientList(simple("http4://localhost:8082/users?page=${header.page}&size="+MAX_RECORDS))
                .unmarshal().json(JsonLibrary.Jackson, UserResponse.class)
                .setHeader("isLast", simple("${body.last}"))
                .setBody(simple("${body.content}"))
                .split(body()).streaming()
                .process(ex->{
                    User user = ex.getIn().getBody(User.class);
                                        System.out.println(user);
                                        Map<String, Object> dbUser = new HashMap<>();
                                        dbUser.put("id", user.getId());
                                        dbUser.put("creationTime", user.getCreationTime());
                                        dbUser.put("lastUpdateTime", user.getLastUpdated());
                                        ex.getIn().setBody(dbUser);
                })
                .to("{{api.insert-user-uri}}")
                .end()
                .process(ex -> {
                    Integer o = (Integer) ex.getIn().getHeader("page");
                    ex.getIn().setHeader("page", o+1);
                    ex.getIn().setBody(null);
                })
                .end();
    }

    private void userExtractionTrigger() {
        CsvDataFormat csv = new CsvDataFormat();
        csv.setDelimiter(",");
        //csv.setHeader(Arrays.asList("ID", "creationTime", "lastUpdateTime","used_directory_search_last_access_timestamp"));
        from("{{extraction.trigger-route}}")
                .routeId("extraction.trigger-route")
                .to("{{extraction.read-db-uri}}")
                .log("Line: ${body}")
                .split(body()).streaming()
                .marshal(csv)
                .to("{{extraction.create-file-uri}}")
                .end();

    }
}
