package fr.ght1pc9kc.testy.mongo;


import fr.ght1pc9kc.testy.core.extensions.ChainedExtension;
import fr.ght1pc9kc.testy.core.extensions.WithObjectMapper;
import fr.ght1pc9kc.testy.mongo.sample.ClazzDataSet;
import fr.ght1pc9kc.testy.mongo.sample.DocumentDataSet;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WithMongoDataTest {

    private static final String COLLECTION_0 = "firstCollection";
    private static final String COLLECTION_1 = "secondCollection";
    private static final String COLLECTION_2 = "dummyCollection";

    private static final WithEmbeddedMongo WITH_EMBEDDED_MONGO = WithEmbeddedMongo.builder().build();
    private static final WithObjectMapper WITH_OBJECT_MAPPER = WithObjectMapper.builder().build();
    private static final WithMongoData WITH_MONGO_DATA = WithMongoData.builder(WITH_EMBEDDED_MONGO)
            .withObjectMapper(WITH_OBJECT_MAPPER)
            .addDataset(COLLECTION_0, new DocumentDataSet())
            .addDataset(COLLECTION_1, new DocumentDataSet())
            .addDataset(COLLECTION_2, new ClazzDataSet())
            .build();

    @RegisterExtension
    @SuppressWarnings("unused")
    static final ChainedExtension chain = ChainedExtension.outer(WITH_EMBEDDED_MONGO)
            .append(WITH_OBJECT_MAPPER)
            .append(WITH_MONGO_DATA)
            .register();

    private ReactiveMongoTemplate mongoTemplate;

    @BeforeEach
    void setUp(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Order(1)
    @ParameterizedTest
    @ValueSource(strings = {
            COLLECTION_0,
            COLLECTION_1,
    })
    void should_have_inserted_data(String collectionName) {
        final List<Document> actual = mongoTemplate.findAll(Document.class, collectionName)
                .collectList()
                .block();

        assertThat(actual).containsExactly(
                DocumentDataSet.DOCUMENT_0,
                DocumentDataSet.DOCUMENT_1,
                DocumentDataSet.DOCUMENT_WITH_MONGO_ID);
    }

    @Test
    @Order(2)
    void should_have_inserted_clazz_data(WithMongoData.Tracker tracker) {
        tracker.skipNextSampleLoad();

        final List<Document> actual = mongoTemplate.findAll(Document.class, COLLECTION_2)
                .collectList()
                .block();

        assertThat(actual).containsExactly(
                new Document(Map.of("_id", "Luke", "bar", "Skywalker")),
                new Document(Map.of("_id", "Obiwan", "bar", "Kenobi")));

        mongoTemplate.insert(new Document(Map.of("_id", "Darth", "name", "Vader")), COLLECTION_2)
                .block();
    }

    @Test
    @Order(3)
    void should_skip_reset_data() {
        final List<Document> actual = mongoTemplate.findAll(Document.class, COLLECTION_2)
                .collectList()
                .block();

        assertThat(actual).containsExactly(
                new Document(Map.of("_id", "Luke", "bar", "Skywalker")),
                new Document(Map.of("_id", "Obiwan", "bar", "Kenobi")),
                new Document(Map.of("_id", "Darth", "name", "Vader")));
    }
}