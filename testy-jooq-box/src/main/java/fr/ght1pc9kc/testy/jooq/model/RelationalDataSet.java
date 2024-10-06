package fr.ght1pc9kc.testy.jooq.model;

import org.jooq.TableRecord;

import java.util.List;

public interface RelationalDataSet<T extends TableRecord<T>> {
    List<T> records();
}
