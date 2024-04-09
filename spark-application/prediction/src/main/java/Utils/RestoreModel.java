package Utils;

import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;

import java.io.File;
import java.io.IOException;

public class RestoreModel {
    public static MultiLayerNetwork restore(File filePath) throws IOException {
        System.out.println("Restoring model...");
        MultiLayerNetwork net = ModelSerializer.restoreMultiLayerNetwork(filePath);

        //print the score with every 1 iteration
        net.setListeners(new ScoreIterationListener(1));

        //Print the  number of parameters in the network (and for each layer)
        Layer[] layers = net.getLayers();
        for (int i = 0; i < layers.length; i++) {
            int nParams = layers[i].numParams();
            System.out.println("Number of parameters in layer " + i + ": " + nParams);
        }
        return net;
    }
}
