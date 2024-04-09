package Representation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class StockDataSetIterator implements DataSetIterator {
    /**
     * category and its index
     */
    private final Map<PriceCategory, Integer> featureMapIndex = ImmutableMap.of(PriceCategory.OPEN, 0, PriceCategory.CLOSE, 1,
            PriceCategory.LOW, 2, PriceCategory.HIGH, 3, PriceCategory.VOLUME, 4);

    private final int VECTOR_SIZE = 5; // number of features for a stock data
    private int miniBatchSize; // mini-batch size
    private int exampleLength = 45; // default 22, say, 22 working days per month
    private int predictLength = 1; // default 1, say, one day ahead prediction

    /**
     * minimal values of each feature in stock dataset
     */
    private double[] minArray = new double[VECTOR_SIZE];
    /**
     * maximal values of each feature in stock dataset
     */
    private double[] maxArray = new double[VECTOR_SIZE];

    /**
     * feature to be selected as a training target
     */
    private PriceCategory category;

    /**
     * mini-batch offset
     */
    private LinkedList<Integer> exampleStartOffsets = new LinkedList<>();

    /**
     * stock dataset for training
     */
    private List<StockData> train;

    /**
     * adjusted stock dataset for testing
     */
    private List<Pair<INDArray, INDArray>> test;

    private INDArray actual;

    public StockDataSetIterator(int miniBatchSize, int exampleLength, double splitRatio, PriceCategory category) {
        List<StockData> stockDataList = readStockDataFromFile(958398, 1000);
        this.miniBatchSize = miniBatchSize;
        this.exampleLength = exampleLength;
        this.category = category;
        int split = (int) Math.round(stockDataList.size() * splitRatio);
        train = stockDataList.subList(0, split);
        test = generateTestDataSet(stockDataList.subList(split, stockDataList.size()));
        initializeOffsets();
    }

    public StockDataSetIterator(int comId, int exampleLength) {
        List<StockData> stockDataList = readStockDataFromFile(comId, exampleLength);
        this.exampleLength = exampleLength;
        actual = Nd4j.create(new int[]{exampleLength, VECTOR_SIZE}, 'f');
        for (int j = 0; j < exampleLength; j++) {
            StockData stock = stockDataList.get(j);
            actual.putScalar(new int[]{j, 0}, (stock.getOpen() - minArray[0]) / (maxArray[0] - minArray[0]));
            actual.putScalar(new int[]{j, 1}, (stock.getClose() - minArray[1]) / (maxArray[1] - minArray[1]));
            actual.putScalar(new int[]{j, 2}, (stock.getLow() - minArray[2]) / (maxArray[2] - minArray[2]));
            actual.putScalar(new int[]{j, 3}, (stock.getHigh() - minArray[3]) / (maxArray[3] - minArray[3]));
            actual.putScalar(new int[]{j, 4}, (stock.getVolume() - minArray[4]) / (maxArray[4] - minArray[4]));
        }
    }

    /**
     * initialize the mini-batch offsets
     */
    private void initializeOffsets() {
        exampleStartOffsets.clear();
        int window = exampleLength + predictLength;
        for (int i = 0; i < train.size() - window; i++) {
            exampleStartOffsets.add(i);
        }
    }

    public List<Pair<INDArray, INDArray>> getTestDataSet() {
        return test;
    }

    public INDArray getActual() {
        return actual;
    }

    public double[] getMaxArray() {
        return maxArray;
    }

    public double[] getMinArray() {
        return minArray;
    }

    public double getMaxNum(PriceCategory category) {
        return maxArray[featureMapIndex.get(category)];
    }

    public double getMinNum(PriceCategory category) {
        return minArray[featureMapIndex.get(category)];
    }

    public DataSet next(int num) {
        if (exampleStartOffsets.size() == 0) throw new NoSuchElementException();
        int actualMiniBatchSize = Math.min(num, exampleStartOffsets.size());
        INDArray input = Nd4j.create(new int[]{actualMiniBatchSize, VECTOR_SIZE, exampleLength}, 'f');
        INDArray label;

        if (category.equals(PriceCategory.ALL))
            label = Nd4j.create(new int[]{actualMiniBatchSize, VECTOR_SIZE, exampleLength}, 'f');
        else
            label = Nd4j.create(new int[]{actualMiniBatchSize, predictLength, exampleLength}, 'f');

        for (int index = 0; index < actualMiniBatchSize; index++) {
            int startIdx = exampleStartOffsets.removeFirst();
            int endIdx = startIdx + exampleLength;
            StockData curData = train.get(startIdx);
            StockData nextData;
            for (int i = startIdx; i < endIdx; i++) {
                int c = i - startIdx;
                input.putScalar(new int[]{index, 0, c}, (curData.getOpen() - minArray[0]) / (maxArray[0] - minArray[0]));
                input.putScalar(new int[]{index, 1, c}, (curData.getClose() - minArray[1]) / (maxArray[1] - minArray[1]));
                input.putScalar(new int[]{index, 2, c}, (curData.getLow() - minArray[2]) / (maxArray[2] - minArray[2]));
                input.putScalar(new int[]{index, 3, c}, (curData.getHigh() - minArray[3]) / (maxArray[3] - minArray[3]));
                input.putScalar(new int[]{index, 4, c}, (curData.getVolume() - minArray[4]) / (maxArray[4] - minArray[4]));
                nextData = train.get(i + 1);
                if (category.equals(PriceCategory.ALL)) {
                    label.putScalar(new int[]{index, 0, c}, (nextData.getOpen() - minArray[1]) / (maxArray[1] - minArray[1]));
                    label.putScalar(new int[]{index, 1, c}, (nextData.getClose() - minArray[1]) / (maxArray[1] - minArray[1]));
                    label.putScalar(new int[]{index, 2, c}, (nextData.getLow() - minArray[2]) / (maxArray[2] - minArray[2]));
                    label.putScalar(new int[]{index, 3, c}, (nextData.getHigh() - minArray[3]) / (maxArray[3] - minArray[3]));
                    label.putScalar(new int[]{index, 4, c}, (nextData.getVolume() - minArray[4]) / (maxArray[4] - minArray[4]));
                } else {
                    label.putScalar(new int[]{index, 0, c}, feedLabel(nextData));
                }
                curData = nextData;
            }
            if (exampleStartOffsets.size() == 0) break;
        }
        return new DataSet(input, label);
    }

    private double feedLabel(StockData data) {
        double value;
        switch (category) {
            case OPEN:
                value = (data.getOpen() - minArray[0]) / (maxArray[0] - minArray[0]);
                break;
            case CLOSE:
                value = (data.getClose() - minArray[1]) / (maxArray[1] - minArray[1]);
                break;
            case LOW:
                value = (data.getLow() - minArray[2]) / (maxArray[2] - minArray[2]);
                break;
            case HIGH:
                value = (data.getHigh() - minArray[3]) / (maxArray[3] - minArray[3]);
                break;
            case VOLUME:
                value = (data.getVolume() - minArray[4]) / (maxArray[4] - minArray[4]);
                break;
            default:
                throw new NoSuchElementException();
        }
        return value;
    }

    public int totalExamples() {
        return train.size() - exampleLength - predictLength;
    }

    public int inputColumns() {
        return VECTOR_SIZE;
    }

    @Override
    public int totalOutcomes() {
        if (this.category.equals(PriceCategory.ALL)) return VECTOR_SIZE;
        else return predictLength;
    }

    public boolean resetSupported() {
        return false;
    }

    public boolean asyncSupported() {
        return false;
    }

    public void reset() {
        initializeOffsets();
    }

    public int batch() {
        return miniBatchSize;
    }

    public int cursor() {
        return totalExamples() - exampleStartOffsets.size();
    }

    public int numExamples() {
        return totalExamples();
    }

    public DataSetPreProcessor getPreProcessor() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void setPreProcessor(DataSetPreProcessor dataSetPreProcessor) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public List<String> getLabels() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public boolean hasNext() {
        return exampleStartOffsets.size() > 0;
    }

    public DataSet next() {
        return next(miniBatchSize);
    }

    private List<Pair<INDArray, INDArray>> generateTestDataSet(List<StockData> stockDataList) {
        int window = exampleLength + predictLength;
        List<Pair<INDArray, INDArray>> test = new ArrayList<>();
        for (int i = 0; i < stockDataList.size() - window; i++) {
            INDArray input = Nd4j.create(new int[]{exampleLength, VECTOR_SIZE}, 'f');
            for (int j = i; j < i + exampleLength; j++) {
                StockData stock = stockDataList.get(j);
                input.putScalar(new int[]{j - i, 0}, (stock.getOpen() - minArray[0]) / (maxArray[0] - minArray[0]));
                input.putScalar(new int[]{j - i, 1}, (stock.getClose() - minArray[1]) / (maxArray[1] - minArray[1]));
                input.putScalar(new int[]{j - i, 2}, (stock.getLow() - minArray[2]) / (maxArray[2] - minArray[2]));
                input.putScalar(new int[]{j - i, 3}, (stock.getHigh() - minArray[3]) / (maxArray[3] - minArray[3]));
                input.putScalar(new int[]{j - i, 4}, (stock.getVolume() - minArray[4]) / (maxArray[4] - minArray[4]));
            }
            StockData stock = stockDataList.get(i + exampleLength);
            INDArray label;
            if (category.equals(PriceCategory.ALL)) {
                label = Nd4j.create(new int[]{VECTOR_SIZE}, 'f'); // ordering is set as 'f', faster construct
                label.putScalar(new int[]{0}, stock.getOpen());
                label.putScalar(new int[]{1}, stock.getClose());
                label.putScalar(new int[]{2}, stock.getLow());
                label.putScalar(new int[]{3}, stock.getHigh());
                label.putScalar(new int[]{4}, stock.getVolume());
            } else {
                label = Nd4j.create(new int[]{1}, 'f');
                switch (category) {
                    case OPEN:
                        label.putScalar(new int[]{0}, stock.getOpen());
                        break;
                    case CLOSE:
                        label.putScalar(new int[]{0}, stock.getClose());
                        break;
                    case LOW:
                        label.putScalar(new int[]{0}, stock.getLow());
                        break;
                    case HIGH:
                        label.putScalar(new int[]{0}, stock.getHigh());
                        break;
                    case VOLUME:
                        label.putScalar(new int[]{0}, stock.getVolume());
                        break;
                    default:
                        throw new NoSuchElementException();
                }
            }
            test.add(new Pair<>(input, label));
        }
        return test;
    }

    @SuppressWarnings("resource")
    private List<StockData> readStockDataFromFile(int comId, int size) {
        List<StockData> stockDataList = new ArrayList<>();
        try {
            for (int i = 0; i < maxArray.length; i++) { // initialize max and min arrays
                maxArray[i] = Double.MIN_VALUE;
                minArray[i] = Double.MAX_VALUE;
            }

            ObjectMapper mapper = new ObjectMapper();
            RestHighLevelClient client = new RestHighLevelClient(
                    RestClient.builder(new HttpHost("192.168.56.1", 9200, "http")));

            SearchRequest searchRequest = new SearchRequest("stock");
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.matchQuery("company_id", comId));
            searchSourceBuilder.size(size);
            searchSourceBuilder.sort("timestamp", SortOrder.DESC);
            searchRequest.source(searchSourceBuilder);
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHit[] searchHits = response.getHits().getHits();

            stockDataList =
                    Arrays.stream(searchHits)
                            .map(hit -> {
                                try {
                                    String _temp = hit.getSourceAsString();
                                    StockData data = mapper.readValue(_temp, StockData.class);

                                    if (data.getOpen() > maxArray[0]) maxArray[0] = data.getOpen();
                                    if (data.getOpen() < minArray[0]) minArray[0] = data.getOpen();

                                    if (data.getClose() > maxArray[1]) maxArray[1] = data.getClose();
                                    if (data.getClose() < minArray[1]) minArray[1] = data.getClose();

                                    if (data.getLow() > maxArray[2]) maxArray[2] = data.getLow();
                                    if (data.getLow() < minArray[2]) minArray[2] = data.getLow();

                                    if (data.getHigh() > maxArray[3]) maxArray[3] = data.getHigh();
                                    if (data.getHigh() < minArray[3]) minArray[3] = data.getHigh();

                                    if (data.getVolume() > maxArray[4]) maxArray[4] = data.getVolume();
                                    if (data.getVolume() < minArray[4]) minArray[4] = data.getVolume();

                                    return data;
                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                                return null;
                            })
                            .collect(Collectors.toList());
            client.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return stockDataList;
    }
}
