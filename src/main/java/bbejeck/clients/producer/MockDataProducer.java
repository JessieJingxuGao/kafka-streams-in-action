package bbejeck.clients.producer;

import bbejeck.model.PublicTradedCompany;
import bbejeck.model.Purchase;
import bbejeck.model.StockTickerData;
import bbejeck.util.datagen.DataGenerator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static bbejeck.util.datagen.DataGenerator.NUM_ITERATIONS;

/**
 * Class will produce 100 Purchase records per iteration
 * for a total of 1,000 records in one minute then it will shutdown.
 */
public class MockDataProducer {

    private static Producer<String, String> producer;
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private static ExecutorService executorService = Executors.newFixedThreadPool(1);
    private static Callback callback;
    private static final String TRANSACTIONS_TOPIC = "transactions";
    private static final String STOCK_TOPIC = "stock-ticker";
    public static final String STOCK_TICKER_TABLE_TOPIC = "stock-ticker-table";
    public static final String STOCK_TICKER_STREAM_TOPIC = "stock-ticker-stream";
    private static final String YELLING_APP_TOPIC = "src-topic";
    private static final int YELLING_APP_ITERATIONS = 5;




    public static void producePurchaseData() {
        producePurchaseData(DataGenerator.DEFAULT_NUM_PURCHASES, DataGenerator.NUM_ITERATIONS, DataGenerator.NUMBER_UNIQUE_CUSTOMERS);
    }

    public static void producePurchaseData(int numberPurchases, int numberIterations, int numberCustomers){
        Runnable generateTask = () -> {
            init();
            int counter = 0;
            while (counter++ < numberIterations) {
                List<Purchase> purchases = DataGenerator.generatePurchases(numberPurchases, numberCustomers);
                List<String> jsonValues = convertToJson(purchases);
                for (String value : jsonValues) {
                    ProducerRecord<String, String> record = new ProducerRecord<>(TRANSACTIONS_TOPIC, null, value);
                    producer.send(record, callback);
                }
                System.out.println("Record batch sent");
                try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
            }
            System.out.println("Done generating purchase data");

        };
        executorService.submit(generateTask);
    }

    public static void produceStockTickerData() {
          produceStockTickerData(DataGenerator.NUMBER_TRADED_COMPANIES, NUM_ITERATIONS);
    }

    public static void produceStockTickerData(int numberCompanies, int numberIterations) {
        Runnable generateTask = () -> {
            init();
            int counter = 0;
            List<PublicTradedCompany> publicTradedCompanyList = DataGenerator.stockTicker(numberCompanies);

            while (counter++ < numberIterations) {
                for (PublicTradedCompany company : publicTradedCompanyList) {
                    String value = convertToJson(new StockTickerData(company.getPrice(),company.getSymbol()));

                    ProducerRecord<String, String> record = new ProducerRecord<>(STOCK_TICKER_TABLE_TOPIC, company.getSymbol(), value);
                    producer.send(record, callback);

                    record = new ProducerRecord<>(STOCK_TICKER_STREAM_TOPIC, company.getSymbol(), value);
                    producer.send(record, callback);
                    
                    company.updateStockPrice();
                }
                System.out.println("Stock updates sent");
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
            }
            //System.out.println("Done generating StockTickerData Data");

        };
        executorService.submit(generateTask);
    }
    

    private static List<StockTickerData> getTickerData(List<PublicTradedCompany> companies){
        List<StockTickerData> tickerData = new ArrayList<>();
        for (PublicTradedCompany company : companies) {
              tickerData.add(new StockTickerData(company.getPrice(), company.getSymbol()));
        }
        return tickerData;
    }

    public static void produceRandomTextData() {
        Runnable generateTask = () -> {
            init();
            int counter = 0;
            while (counter++ < YELLING_APP_ITERATIONS) {
                List<String> textValues = DataGenerator.generateRandomText();

                for (String value : textValues) {
                    ProducerRecord<String, String> record = new ProducerRecord<>(YELLING_APP_TOPIC, null, value);
                    producer.send(record, callback);
                }
                System.out.println("Text batch sent");
                try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
            }
            System.out.println("Done generating text data");

        };
        executorService.submit(generateTask);
    }

    public static void shutdown() {
        System.out.println("Shutting down data generation");

        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        if (producer != null) {
            producer.close();
            producer = null;
        }

    }

    private static void init() {
        System.out.println("Initializing the producer");
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("acks", "1");
        properties.put("retries", "3");

        producer = new KafkaProducer<>(properties);

        callback = (metadata, exception) -> {
            if (exception != null) {
                exception.printStackTrace();
            }
        };
        System.out.println("Producer initialized");
    }


    private static <T> List<String> convertToJson(List<T> generatedDataItems) {
        List<String> jsonList = new ArrayList<>();
        for (T generatedData : generatedDataItems) {
            jsonList.add(convertToJson(generatedData));
        }
        return jsonList;
    }
    
    private static <T> String convertToJson(T generatedDataItem){
        return gson.toJson(generatedDataItem);
    }
}
