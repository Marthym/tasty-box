## testy-params-box

`testy-params-box` provides an aggregator to manage String Varargs.

```java
@ParameterizedTest
@CsvSource({
        "1, one, two, three"
})
void should_use_string_aggregator(int index, @AggregateWith(StringVargsAggregator.class) String... aggregated) {
    assertThat(index).isEqualTo(1);
    assertThat(aggregated).containsExactly("one", "two", "three");
}
```
