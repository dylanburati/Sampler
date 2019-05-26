package libre.sampler.utils;

import java.text.DecimalFormat;

public class MyDecimalFormat {
    private DecimalFormat[] formats;
    public MyDecimalFormat(int maxDecimalPlaces, int maxSignificantDigits) {
        StringBuilder builder = new StringBuilder("0.");
        for(int i = 0; i < Math.min(maxDecimalPlaces, maxSignificantDigits); i++) {
            builder.append("#");
        }

        formats = new DecimalFormat[maxSignificantDigits + 1];
        formats[0] = new DecimalFormat(builder.toString());
        for(int i = 1; i < maxSignificantDigits + 1; i++) {
            int possibleDecimalPlaces = maxSignificantDigits - i;
            if(possibleDecimalPlaces >= maxDecimalPlaces) {
                formats[i] = formats[0];
            } else if(possibleDecimalPlaces > 0) {
                formats[i] = new DecimalFormat(builder.substring(0, 2 + possibleDecimalPlaces));
            } else {
                formats[i] = new DecimalFormat("0");
            }
        }
    }

    public String format(double number) {
        int index = 0;
        double numCopy = Math.abs(number);
        while(index < formats.length - 1 && numCopy >= 1) {
            numCopy /= 10;
            index++;
        }
        return formats[index].format(number);
    }
}
