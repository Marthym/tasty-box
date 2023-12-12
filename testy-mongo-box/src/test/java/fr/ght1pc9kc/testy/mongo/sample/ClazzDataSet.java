package fr.ght1pc9kc.testy.mongo.sample;

import fr.ght1pc9kc.testy.dummy.Dummy;
import fr.ght1pc9kc.testy.mongo.MongoDataSet;

import java.util.List;

public class ClazzDataSet implements MongoDataSet<Dummy> {
    @Override
    public List<Dummy> documents() {
        return List.of(
                new Dummy("Luke", "Skywalker"),
                new Dummy("Obiwan", "Kenobi"));
    }

    @Override
    public String identifier() {
        return "foo";
    }
}
