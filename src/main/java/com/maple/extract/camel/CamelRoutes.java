package com.maple.extract.camel;

import com.maple.extract.model.Authetication;
import com.maple.extract.model.User;
import com.maple.extract.model.UserResponse;
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
        from("{{common.trigger-route}}")
                .routeId("common.trigger-route")
                .choice()
                    .when(simple("{{common.enable-api-route}}"))
                        .to("{{api.trigger-route}}")
                    .endChoice()
                    .when(simple("{{common.enable-analytics-route}}"))
                        .to("{{analytics.trigger-route}}")
                    .endChoice()
                .end()
                .to("{{extraction.trigger-route}}");
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
                .end();
    }

    private void getUsersTrigger() {
        from("{{api.trigger-route}}")
                .routeId("api.trigger-route")

                // Creating base64 string
                .setProperty("client-id", simple("{{api.client-id}}"))
                .setProperty("secret-key", simple("{{api.secret-key}}"))
                .process(ex -> {
                    String baseString = (String) ex.getProperty("client-id") + ":" + (String) ex.getProperty("secret-key");
                    String base64String = Base64.getEncoder().encodeToString(baseString.getBytes());
                    ex.setProperty("base64String", base64String);
                })

                // Api call to get authetication token
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("content-type", simple("{{api.content-type}}"))
                .setHeader("authorization", simple("Basic ${property.base64String}"))
                .setBody(simple("{{api.auth-api-body}}"))
                .to("{{api.access-token-uri}}")
                .convertBodyTo(String.class)
                .unmarshal().json(JsonLibrary.Jackson, Authetication.class)

                // Running a loop untill property.isLast == true
                .setProperty("access_token", constant("${body.access_token}"))
                .setBody(simple(null))
                .setProperty("page", constant(0))
                .loopDoWhile(simple("${property.page} == 0 || ${property.isLast} != true"))
                    .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                    .setHeader("cache-control", simple("{{api.cache-control}}"))
                    .setHeader("Authorization", simple("Bearer ${property.access_token}"))

                    // Api call to get User List
                    .recipientList(simple("http4://localhost:8082/users?page=${property.page}&size="+MAX_RECORDS))
                    .unmarshal().json(JsonLibrary.Jackson, UserResponse.class)
                    .setProperty("isLast", simple("${body.last}"))
                    .setBody(simple("${body.content}"))
                    .split(body()).streaming()
                        .process(ex->{
                            User user = ex.getIn().getBody(User.class);
                                                Map<String, Object> dbUser = new HashMap<>();
                                                dbUser.put("id", user.getId());
                                                dbUser.put("creationTime", user.getCreationTime());
                                                dbUser.put("lastUpdateTime", user.getLastUpdated());
                                                ex.getIn().setBody(dbUser);
                        })
                        .to("{{api.insert-user-uri}}")
                    .end()
                    .process(ex -> {
                        Integer o = (Integer) ex.getProperty("page");
                        ex.setProperty("page", o+1);
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
