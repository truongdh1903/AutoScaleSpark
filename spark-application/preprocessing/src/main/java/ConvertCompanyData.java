import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataTypes;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;

public class ConvertCompanyData {
    public static void convert(SparkSession spark) {
        String companyPath = "hdfs://hadoop-master:9000/company/vietnam.json";
        Dataset<Row> companies = spark.read().json(companyPath);

        spark.udf().register("convertPrice", Utils.convertPrice, DataTypes.FloatType);
        spark.udf().register("convertPercent", Utils.convertPercent, DataTypes.FloatType);
        spark.udf().register("convertNumber", Utils.convertNumber, DataTypes.DoubleType);

        Dataset<Row> newDs = companies
                .withColumn("company_id", functions.col("company_id").cast("Integer"))
                .withColumn("volume", functions.callUDF("convertNumber", companies.col("volume")))
                .withColumn("volume_3m", functions.callUDF("convertNumber", companies.col("volume_3m")))
                .withColumn("market_cap", functions.callUDF("convertNumber", companies.col("market_cap")))
                .withColumn("revenue", functions.callUDF("convertNumber", companies.col("revenue")))
                .withColumn("p_e_ratio", functions.callUDF("convertPrice", companies.col("p_e_ratio")))
                .withColumn("beta", functions.callUDF("convertPrice", companies.col("beta")))
                .withColumn("change_per_year", functions.callUDF("convertPrice", companies.col("change_per_year")))
                .withColumn("shares_outstanding", functions.callUDF("convertNumber", companies.col("shares_outstanding")))
                .withColumn("eps", functions.callUDF("convertPrice", companies.col("eps")));

        JavaEsSpark.saveJsonToEs(newDs.toJSON().toJavaRDD(), "company/company");

//        JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext());
//        JavaPairRDD<String, Map<String, Object>> esRDD = JavaEsSpark.esRDD(jsc, "company/company");
    }
}
