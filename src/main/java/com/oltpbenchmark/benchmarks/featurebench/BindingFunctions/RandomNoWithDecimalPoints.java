package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.util.List;
import java.util.Random;

public class RandomNoWithDecimalPoints implements BaseUtil {

    protected int lowerBound;
    protected int upperBound;
    protected int decimalPoints;

    public RandomNoWithDecimalPoints(int lowerBound, int upperBound, int decimalPoints, List<Object> values) {
        this.lowerBound = (int) values.get(0);
        this.upperBound = (int) values.get(1);
        this.decimalPoints = (int) values.get(2);
        if (lowerBound < 0 || upperBound <= lowerBound || decimalPoints < 0) {
            throw new IllegalArgumentException("error");
        }
    }

    @Override
    public Object run() {
        Random rnd = new Random();
        double randomNo = lowerBound + (upperBound - lowerBound) * rnd.nextDouble();
        double DecimalPointNumber = (double) Math.round(randomNo * (Math.pow(10, decimalPoints))) / (Math.pow(10, decimalPoints));
        return DecimalPointNumber;
    }
}
