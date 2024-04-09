import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;

public class SparkAppMain {
    public static void main(String[] args) {
        SparkSession spark = SparkSession
                .builder()
                .config("es.index.auto.create", true)
                .config("es.nodes", "172.17.0.1")
                .getOrCreate();
        JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());

//        ConvertCompanyData.convert(spark);
        ConvertStockData.convert(spark);
        sc.close();
    }
}
