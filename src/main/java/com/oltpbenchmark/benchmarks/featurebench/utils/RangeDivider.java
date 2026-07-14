package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.util.List;

/*
Description: Divides a total range into equal chunks and assigns each worker a non-overlapping range based on its workerId. Stateless and safe across multiple workloads without needing a reset.

Params:
1. int: total (values[0]) - The total range to divide.
2. int: divisor (values[1]) - Number of chunks to divide into.
3. int: mode (values[2], optional) - 0 returns start_range (default), 1 returns end_range.

Eg:
total: 1000, divisor: 5
Worker 0 gets: start=0, end=200
Worker 1 gets: start=200, end=400 and so on.
*/
public class RangeDivider implements BaseUtil {

    private final int startRange;

    public RangeDivider(List<Object> values, int workerId, int totalWorkers) {
        if (values.size() != 1) {
            throw new RuntimeException("RangeDivider requires 1 parameter: "
                + "[total]");
        }

        int total = values.get(0) instanceof Number ? ((Number) values.get(0)).intValue() : Integer.parseInt(values.get(0).toString());

        if (total <= 0) {
            throw new RuntimeException("Total must be positive");
        }

        int chunkSize = total / totalWorkers;

        this.startRange = workerId * chunkSize;
    }

    @Override
    public Object run() {
        return startRange;
    }
}
