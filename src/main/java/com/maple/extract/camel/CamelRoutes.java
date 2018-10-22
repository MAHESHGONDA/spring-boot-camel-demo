package com.maple.extract.camel;

import com.maple.extract.model.Authetication;
import com.maple.extract.model.User;
import com.maple.extract.model.UserResponse;
import com.maple.extract.processor.ExceptionHandlingProcessor;
import com.maple.extract.processor.UserProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CamelRoutes extends RouteBuilder {

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
                .onException(Exception.class)
                    .handled(true)
                    .maximumRedeliveries(3)
                    .redeliveryDelay(0)
                .end()
                .choice()
                .when(simple("{{common.enable-api-route}}")) //TODO attempt to run it 3 times
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

                // Exception Handling
                .onException(Exception.class)
                .handled(true)
                .process(new ExceptionHandlingProcessor())
                .end()

                // Api call to get Authentication token
                .process(new UserProcessor("Authentication"))
                .to("{{api.access-token-uri}}")
                .convertBodyTo(String.class)
                .unmarshal().json(JsonLibrary.Jackson, Authetication.class)

                // Running a loop untill property.isLast == true
                .process(new UserProcessor("GetUsers"))
                .loopDoWhile(simple("${property.CamelLoopIndex} == 0 || ${property.isLast} != true"))

                // Api call to get User List
                .setBody(simple(null))
                .toD("{{api.api-endpoint}}?page=${property.CamelLoopIndex}&size={{api.batch-size}}")
                .unmarshal().json(JsonLibrary.Jackson, UserResponse.class)
                .setProperty("isLast", simple("${body.last}"))
                .setBody(simple("${body.content}"))
                .setProperty("dataCount", simple("${body.size()}"))
                .split(body()).streaming()
                .process(ex -> {
                    User user = ex.getIn().getBody(User.class);
                    Map<String, Object> dbUser = new HashMap<>();
                    dbUser.put("id", user.getId());
                    dbUser.put("creationTime", user.getCreatedDate());
                    dbUser.put("lastUpdateTime", user.getLastUpdated());
                    ex.getIn().setBody(dbUser);
                })
                .aggregate(constant(true),
                        new DataAggregationStrategy())
                .completionSize(simple("${property.dataCount}"))
                .transform(simple("${body}"))
                .end()

                // Inserting data to h2 db
                .to("{{api.insert-user-uri}}")
                .end();
    }

    private void userExtractionTrigger() {
        CsvDataFormat csv = new CsvDataFormat();
        csv.setDelimiter("|"); //TODO Need to ask: what shall we show for empty fields, 1) show null 2) skip it from writing to csv 3) leave empty delemeters?
        csv.setHeader(Arrays.asList("id", "creation_time", "last_update_time", "program_enrollment_timestamp", "program_last_accessed_timestamp", "program_accessed_count", "used_virtual_care_flag", "used_virtual_care_count",
                "used_virtual_care_last_access_timestamp", "used_directory_search_flag", "used_directory_search_count", "used_directory_search_last_access_timestamp"));

        from("{{extraction.trigger-route}}")
                .routeId("extraction.trigger-route")
                .to("{{extraction.read-db-uri}}")
                .marshal(csv)
                .to("{{extraction.create-file-uri}}");
    }
}
