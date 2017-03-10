package bbejeck.chapter_5;



import static bbejeck.clients.producer.MockDataProducer.STOCK_TICKER_STREAM_TOPIC;
import static bbejeck.clients.producer.MockDataProducer.STOCK_TICKER_TABLE_TOPIC;

import bbejeck.clients.producer.MockDataProducer;
import bbejeck.model.StockTickerData;
import bbejeck.util.serde.StreamsSerdes;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;

import java.util.Properties;

public class KStreamVsKTableExample {

    public static void main(String[] args) throws Exception {

        StreamsConfig streamsConfig = new StreamsConfig(getProperties());

        KStreamBuilder kStreamBuilder = new KStreamBuilder();


        KTable<String, StockTickerData> stockTickerTable = kStreamBuilder.table(STOCK_TICKER_TABLE_TOPIC, "ticker-store");
        KStream<String, StockTickerData> stockTickerStream = kStreamBuilder.stream(STOCK_TICKER_STREAM_TOPIC);

        stockTickerTable.toStream().print( "Stocks-KTable");
        stockTickerStream.print( "Stocks-KStream");

        int numberCompanies = 3;
        int iterations = 3;

        MockDataProducer.produceStockTickerData(numberCompanies, iterations);

        KafkaStreams kafkaStreams = new KafkaStreams(kStreamBuilder, streamsConfig);
        System.out.println("KTable vs KStream output started");
        kafkaStreams.start();
        Thread.sleep(15000);
        System.out.println("Shutting down the Kafka Streams Application now");
        kafkaStreams.close();
        MockDataProducer.shutdown();

    }

    private static Properties getProperties() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "KStreamVSKTable_app");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "KStreamVSKTable_group");
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "KStreamVSKTable_client");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "30000");
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, "15000");
        //props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG,"0");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, "1");
        props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, "10000");
        props.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, StreamsSerdes.StockTickerSerde().getClass().getName());
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 1);
        props.put(StreamsConfig.TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class);
        return props;

    }
}
