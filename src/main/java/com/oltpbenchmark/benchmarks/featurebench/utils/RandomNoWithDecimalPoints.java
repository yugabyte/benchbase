package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.util.List;
import java.util.Random;

public class RandomNoWithDecimalPoints implements BaseUtil {

    protected int lowerBound;
    protected int upperBound;
    protected int decimalPoints;

    public RandomNoWithDecimalPoints(List<Object> values) {
        if (values.size() != 3) {
            throw new RuntimeException("Incorrect number of parameters for util function");
        }
        this.lowerBound = (int) values.get(0);
        this.upperBound = (int) values.get(1);
        this.decimalPoints = (int) values.get(2);
        if (lowerBound < 0 || upperBound < lowerBound || decimalPoints < 0) {
            throw new RuntimeException("Incorrect parameters for random no with decimal points");
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
