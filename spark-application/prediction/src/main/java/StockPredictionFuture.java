import Representation.StockDataSetIterator;
import Utils.RestoreModel;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.IOException;

public class StockPredictionFuture {
    private static StockDataSetIterator iterator;
    private static int exampleLength = 45;

    public static void main(String[] args) throws IOException {

        System.out.println("Creating dataSet iterator...");
        iterator = new StockDataSetIterator(958398, exampleLength);
        INDArray actual = iterator.getActual();

        System.out.println("Restore Modeling...");
        File locationSaved = new File("data/StockPriceLSTM_ALL.zip");
        MultiLayerNetwork net = RestoreModel.restore(locationSaved);

        System.out.println("Evaluating...");
        INDArray max = Nd4j.create(iterator.getMaxArray());
        INDArray min = Nd4j.create(iterator.getMinArray());

        predictAllCategories(net, actual, max, min);

        System.out.println("Done...");
    }

    /**
     * Predict all the features (open, close, low, high prices and volume) of a stock one-day ahead
     */
    private static void predictAllCategories(MultiLayerNetwork net, INDArray actual, INDArray max, INDArray min) {
        INDArray[] predicts = new INDArray[15];
        INDArray predict = null;
        for (int i = 0; i < 15; i++) {
            if (predict != null) {
                actual.slice(0);
                actual.addi(predict.getRow(exampleLength - 1));
            }
            predict = net.rnnTimeStep(actual);
            predicts[i] = predict.getRow(exampleLength - 1).mul(max.sub(min)).add(min);
        }

        System.out.println("Printing predicted and actual values...");

        for (INDArray indArray : predicts) System.out.println(indArray);
    }
}
