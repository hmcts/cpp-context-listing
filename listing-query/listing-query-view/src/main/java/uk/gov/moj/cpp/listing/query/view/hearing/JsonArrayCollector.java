package uk.gov.moj.cpp.listing.query.view.hearing;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class JsonArrayCollector implements Collector<JsonObject, JsonArrayBuilder, JsonArray> {

    @Override
    public Supplier<JsonArrayBuilder> supplier() {
        return Json::createArrayBuilder;
    }

    @Override
    public BiConsumer<JsonArrayBuilder, JsonObject> accumulator() {
        return (x, y) -> x.add(y);
    }

    @Override
    public BinaryOperator<JsonArrayBuilder> combiner() {
        return (x, y) -> {
            x.add(y);
            return x;
        };
    }

    @Override
    public Function<JsonArrayBuilder, JsonArray> finisher() {
        return JsonArrayBuilder::build;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }

    public static JsonArrayCollector toArrayNode() {
        return new JsonArrayCollector();
    }
}
