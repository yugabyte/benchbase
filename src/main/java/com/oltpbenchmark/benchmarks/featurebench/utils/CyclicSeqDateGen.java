package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.List;

public class CyclicSeqDateGen implements BaseUtil {

    private final int yearLowerBound;
    private final int yearUpperBound;
    private LocalDate currentDate;


    public CyclicSeqDateGen(List<Object> values) {
        if (values.size() != 2) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        this.yearLowerBound = ((Number) values.get(0)).intValue();
        this.yearUpperBound = ((Number) values.get(1)).intValue();
        if (yearLowerBound > yearUpperBound) {
            throw new RuntimeException("Please enter correct values for yearLowerBound and yearUpperBound");
        }
        this.currentDate =  LocalDate.of(yearLowerBound, 1, 1).minusDays(1);
    }

    public CyclicSeqDateGen(List<Object> values,int workerId,int totalWorkers) {
        if (values.size() != 2) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        this.yearLowerBound = ((Number) values.get(0)).intValue();
        this.yearUpperBound = ((Number) values.get(1)).intValue();
        if (yearLowerBound > yearUpperBound) {
            throw new RuntimeException("Please enter correct values for yearLowerBound and yearUpperBound");
        }
        this.currentDate =  LocalDate.of(yearLowerBound, 1, 1).minusDays(1);;
    }

    private LocalDate findNextHigherValue() {
        return currentDate.plusDays(1);
    }

    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        currentDate=findNextHigherValue();
        if(currentDate.getYear()>yearUpperBound)
        {
            currentDate=LocalDate.of(yearLowerBound, 1, 1);
        }
        return currentDate;
    }
}