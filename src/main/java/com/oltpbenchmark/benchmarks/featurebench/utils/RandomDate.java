package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.util.List;
import java.util.Random;


public class RandomDate implements BaseUtil {
    protected Random rd;
    protected int yearlowerBound;
    protected int yearupperBound;


    public RandomDate(List<Object> values) {
        if (values.size() != 2) {
            throw new RuntimeException("Incorrect number of parameters for util function");
        }
        Random rnd = new Random();
        this.yearlowerBound = (int) values.get(0);
        this.yearupperBound = (int) values.get(1);
        if(yearlowerBound > yearupperBound || yearlowerBound == 0 && yearupperBound == 0 || yearlowerBound < 0)
            throw new RuntimeException("Please enter correct bounds for max and min year");

    }

    @Override
    public Object run() {
        if (this.rd == null) {
            this.rd = new Random();
        }
        int year = rd.nextInt(yearupperBound - yearlowerBound) + yearlowerBound;
        int month = rd.nextInt(12 - 1) + 1;
        int day = rd.nextInt(30 - 1) + 1;
        String date = day + "-" + month + "-" + year;
        return date;
    }
}
