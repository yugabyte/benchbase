package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/*
Description :- Returns a random date which will lie between given number of days
Start Date :- 01-01-2023
Params :
1.int: numberofDays (values[0]) :- Number of days
2 int offset (optional)

Eg:-
numberofDays:- 10
Return type : (String):- 03-01-2023
*/
public class RandomDate implements BaseUtil {
    private final int numberofDays;
    LocalDate startDate =  LocalDate.of(2023, 1, 1);

    private final Random rd = new Random((int) System.nanoTime());


    public RandomDate(List<Object> values) {
        if (values.isEmpty()) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }

        this.numberofDays = ((Number) values.get(0)).intValue();
        if(values.size() == 2){
            int offSet = ((Number) values.get(1)).intValue();
           this.startDate = this.startDate.plusDays(offSet);
        }
        if (numberofDays <0)
            throw new RuntimeException("Please enter positive number of days");

    }

    public RandomDate(List<Object> values, int workerId, int totalWorkers) {

        if (values.isEmpty()) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }

        this.numberofDays = values.get(0) instanceof String? Integer.parseInt((String) values.get(0)):  ((Number) values.get(0)).intValue();
        if(values.size() == 2){
            int offSet = values.get(1) instanceof String? Integer.parseInt((String) values.get(1)):  ((Number) values.get(1)).intValue();
            this.startDate = this.startDate.plusDays(offSet);
        }

        if (numberofDays <0)
            throw new RuntimeException("Please enter positive number of days");

    }

    @Override
    public Object run() {
        int days = rd.nextInt(numberofDays);
        return  startDate.plusDays(days);
    }
}
