import org.apache.spark.sql.api.java.UDF1;

public class Utils {
    public static final UDF1<String, Float> convertPrice = new UDF1<String, Float>() {
        public Float call(final String value) throws Exception {
            if (value == null || value.isEmpty()) return 0F;
            return Float.parseFloat(value.replace(",", ""));
        }
    };

    public static final UDF1<String, Float> convertPercent = new UDF1<String, Float>() {
        public Float call(final String value) throws Exception {
            return Float.parseFloat(value.replace("%", ""));
        }
    };

    public static final UDF1<String, Double> convertNumber = new UDF1<String, Double>() {
        public Double call(final String value) throws Exception {
            if (value == null || value.isEmpty()) return 0D;
            char lastSymbol = value.charAt(value.length() - 1);
            String number = "";
            if (lastSymbol == 'K' || lastSymbol == 'M' || lastSymbol == 'B' || lastSymbol == 'T') {
                number = value.substring(0, value.length() - 1).replace(",", "");
            } else
                number = value.replace(",", "");
            if (number.isEmpty()) return 0D;
            double result = 0D;
            switch (lastSymbol) {
                case 'K':
                    result = Double.parseDouble(number) * 1000;
                    break;
                case 'M':
                    result = Double.parseDouble(number) * 1000000;
                    break;
                case 'B':
                    result = Double.parseDouble(number) * 1000000000;
                    break;
                case 'T':
                    result = Double.parseDouble(number) * 10e12;
                    break;
                default:
                    result = Double.parseDouble(number);
            }
            return result;
        }
    };
}
