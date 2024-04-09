import Representation.PriceCategory;
import Representation.StockDataSetIterator;
import Utils.PlotUtil;
import org.deeplearning4j.eval.RegressionEvaluation;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

public class StockPricePrediction {
    private static int exampleLength = 45; // time series length, assume 22 working days per month
    private static StockDataSetIterator iterator;

    public static void main(String[] args) throws IOException {
        int batchSize = 128; // mini-batch size
        double splitRatio = 0.8; // 80% for training, 20% for testing
        int epochs = 300; // training epochs

        System.out.println("Creating dataSet iterator...");
        PriceCategory category = PriceCategory.ALL; // CLOSE: predict close price
        iterator = new StockDataSetIterator(batchSize, exampleLength, splitRatio, category);
        System.out.println("Loading test dataset...");
        List<Pair<INDArray, INDArray>> test = iterator.getTestDataSet();

        System.out.println("Building LSTM networks...");
        MultiLayerNetwork net = RecurrentNets.createAndBuildLstmNetworks(iterator.inputColumns(), iterator.totalOutcomes());

        //Initialize the user interface backend
        //UIServer uiServer = UIServer.getInstance();

        //Configure where the network information (gradients, activations, score vs. time etc) is to be stored
        //Then add the StatsListener to collect this information from the network, as it trains
        //StatsStorage statsStorage = new InMemoryStatsStorage();             //Alternative: new FileStatsStorage(File) - see UIStorageExample

        //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
        //uiServer.attach(statsStorage);

        //net.setListeners(new StatsListener(statsStorage, listenerFrequency));

        System.out.println("Training LSTM network...");
        for (int i = 0; i < epochs; i++) {
            while (iterator.hasNext()) net.fit(iterator.next()); // fit model using mini-batch data
            iterator.reset(); // reset iterator
            net.rnnClearPreviousState(); // clear previous state
        }

        //Print the  number of parameters in the network (and for each layer)
        Layer[] layers_before_saving = net.getLayers();
        int totalNumParams_before_saving = 0;
        for (int i = 0; i < layers_before_saving.length; i++) {
            int nParams = layers_before_saving[i].numParams();
            System.out.println("Number of parameters in layer " + i + ": " + nParams);
            totalNumParams_before_saving += nParams;
        }
        System.out.println("Total number of network parameters: " + totalNumParams_before_saving);

        System.out.println("Saving model...");
        File locationToSave = new File("data/StockPriceLSTM_".concat(String.valueOf(category)).concat(".zip"));
        ModelSerializer.writeModel(net, locationToSave, true);

        System.out.println("Evaluating...");
        INDArray max = Nd4j.create(iterator.getMaxArray());
        INDArray min = Nd4j.create(iterator.getMinArray());
        predictAllCategories(net, test, max, min);
        System.out.println("Done...");
    }

    /**
     * Predict all the features (open, close, low, high prices and volume) of a stock one-day ahead
     */
    /**
     * Predict all the features (open, close, low, high prices and volume) of a stock one-day ahead
     */
    private static void predictAllCategories(MultiLayerNetwork net, List<Pair<INDArray, INDArray>> testData, INDArray max, INDArray min) {
        INDArray[] predicts = new INDArray[testData.size()];
        INDArray[] actuals = new INDArray[testData.size()];
        INDArray predict = null;
        for (int i = 0; i < testData.size(); i++) {
            INDArray temp = testData.get(i).getKey();
            if (predict != null) {
                temp.add(predict);
                temp.slice(0);
            }
            predict = net.rnnTimeStep(temp);
            predicts[i] = predict.getRow(exampleLength - 1).mul(max.sub(min)).add(min);
            actuals[i] = testData.get(i).getValue();
        }

        System.out.println("Plottig...");

        RegressionEvaluation eval = net.evaluateRegression(iterator);
        System.out.println(eval.stats());

        for (int n = 0; n < 5; n++) {
            double[] pred = new double[predicts.length];
            double[] actu = new double[actuals.length];
            for (int i = 0; i < predicts.length; i++) {
                pred[i] = predicts[i].getDouble(n);
                actu[i] = actuals[i].getDouble(n);
            }
            String name;
            switch (n) {
                case 0:
                    name = "OPEN Price";
                    break;
                case 1:
                    name = "CLOSE Price";
                    break;
                case 2:
                    name = "LOW Price";
                    break;
                case 3:
                    name = "HIGH Price";
                    break;
                case 4:
                    name = "CHANGE Amount";
                    break;
                default:
                    throw new NoSuchElementException();
            }
            PlotUtil.plot(pred, actu, name);
        }
    }
}
