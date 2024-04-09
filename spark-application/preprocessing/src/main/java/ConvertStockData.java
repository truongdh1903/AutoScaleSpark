import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataTypes;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;

import static org.apache.spark.sql.functions.from_unixtime;

public class ConvertStockData {
    public static void convert(SparkSession spark) {
        String[] stockPath = {
                "hdfs://hadoop-master:9000/stock/2021/*.json",
                "hdfs://hadoop-master:9000/stock/2020/*.json",
                "hdfs://hadoop-master:9000/stock/2019/*.json",
        };
        Dataset<Row> stocks = spark.read().json(stockPath);

        spark.udf().register("convertPrice", Utils.convertPrice, DataTypes.FloatType);
        spark.udf().register("convertPercent", Utils.convertPercent, DataTypes.FloatType);

        Dataset<Row> newDs = stocks
                .withColumn("company_id", functions.col("company_id").cast("Integer"))
                .withColumn("volume", functions.col("volume").cast("Integer"))
                .withColumn("high", functions.callUDF("convertPrice", stocks.col("high")))
                .withColumn("low", functions.callUDF("convertPrice", stocks.col("low")))
                .withColumn("open", functions.callUDF("convertPrice", stocks.col("open")))
                .withColumn("close", functions.callUDF("convertPrice", stocks.col("close")))
                .withColumn("change", functions.callUDF("convertPercent", stocks.col("change")))
                .withColumn("timestamp", functions.col("date").cast("Long"))
                .withColumn("date", from_unixtime(stocks.col("date")));

        JavaEsSpark.saveJsonToEs(newDs.toJSON().toJavaRDD(), "stock/stock");
    }
}
