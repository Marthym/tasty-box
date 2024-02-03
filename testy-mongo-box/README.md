## testy-mongo-box

This project is used to test MongoDB repositories. It provides extensions to use an embedded Mongo database:

* [WithEmbeddedMongo](https://marthym.github.io/testy-box/fr/ght1pc9kc/testy/mongo/WithEmbeddedMongo.html) initializes the embbeded Mongo database.
* [WithMongoData](https://marthym.github.io/testy-box/fr/ght1pc9kc/testy/mongo/WithMongoData.html) inserts test data into the database.

### WithEmbeddedMongo

This extension starts an embedded MongoDB.

```java
@RegisterExtension
static final WithEmbeddedMongo wMongo = WithEmbeddedMongo
        .builder()
        .setDatabaseName("my_database")
        .build();
```

With this extension, `MongoClient`, `ReactiveMongoDatabaseFactory` and `ReactiveMongoTemplate` can be injected as parameters.

```java
@BeforeEach
void setUp(MongoClient mongoClient, 
           ReactiveMongoDatabaseFactory factory,
           ReactiveMongoTemplate mongoTemplate) {
    // (...)
}
```

### WithMongoData

This extension resets the content of the collections before each test method. The data of a collection can be defined by implementing [MongoDataSet](https://marthym.github.io/testy-box/fr/ght1pc9kc/testy/mongo/MongoDataSet.html).

```java
public class MyElementDataSet implements MongoDataSet<MyElement> {

    @Override
    public List<MyElement> documents() {
        // List the objects to be inserted in the collection
    }
}
```

Each data set can be associated with a specific collection with the extension.

```java
private static final WithEmbeddedMongo wMongo = WithEmbeddedMongo
        .builder()
        .build();
private static final WithMongoData wMongoData = WithMongoData
        .builder(wMongo)
        .addDataset("my_element_collection", new MyElementDataSet())
        .build();

@RegisterExtension
static final ChainedExtension wExtensions = ChainedExtension
        .outer(wMongo)
        .append(wMongoData)
        .register();
```

#### Performances enhancement with dbTracker
If you have lot of tests and you don't want the all database was reset on each test. You can use the db Tracker.

```java
    @Test
    void should_have_read_data(WithMongoData.Tracker tracker) {
        tracker.skipNextSampleLoad();

        // You read only test code
    }
```

The next test will not drop and reload the DataSets.

#### Using custom Object Mapper
Optionally, a specific mapper can be used to convert objects to Mongo Documents by including the extension [WithObjectMapper](https://marthym.github.io/testy-box/fr/ght1pc9kc/testy/core/extensions/WithObjectMapper.html).

```java
private static final WithEmbeddedMongo wMongo = WithEmbeddedMongo
        .builder()
        .build();
private static final WithObjectMapper wObjectMapper = WithObjectMapper
        .builder()
        .addModule(new JavaTimeModule())
        .build();
private static final WithMongoData wMongoData = WithMongoData
        .builder(wMongo)
        .withObjectMapper(wObjectMapper)
        .addDataset("my_element_collection", new MyElementDataSet())
        .build();

@RegisterExtension
static final ChainedExtension wExtensions = ChainedExtension
        .outer(wMongo)
        .append(wObjectMapper)
        .append(wMongoData)
        .register();
```
