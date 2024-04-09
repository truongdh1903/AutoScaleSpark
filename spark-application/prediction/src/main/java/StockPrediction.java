import Representation.PriceCategory;
import Representation.StockDataSetIterator;
import Utils.PlotUtil;
import Utils.RestoreModel;
import org.deeplearning4j.eval.RegressionEvaluation;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

public class StockPrediction {
    private static StockDataSetIterator iterator;
    private static int exampleLength = 45;

    public static void main(String[] args) throws IOException {
        int batchSize = 128; // mini-batch size
        double splitRatio = 0.5; // 80% for training, 20% for testing

        System.out.println("Creating dataSet iterator...");
        PriceCategory category = PriceCategory.ALL;
        iterator = new StockDataSetIterator(batchSize, exampleLength, splitRatio, category);

        System.out.println("Loading test dataset...");
        List<Pair<INDArray, INDArray>> test = iterator.getTestDataSet();

        System.out.println("Restore Modeling...");
        File locationSaved = new File("data/StockPriceLSTM_ALL.zip");
        MultiLayerNetwork net = RestoreModel.restore(locationSaved);

        System.out.println("Evaluating...");
        INDArray max = Nd4j.create(iterator.getMaxArray());
        INDArray min = Nd4j.create(iterator.getMinArray());
        predictAllCategories(net, test, max, min);

        System.out.println("Done...");
    }

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
