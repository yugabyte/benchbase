package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;

public class RandomLong extends Random implements BaseUtil {

    private long minimum;
    private long maximum;

    public RandomLong(List<Object> values) {
        super((int) System.currentTimeMillis());
        this.minimum = (long) values.get(0);
        this.maximum = (long) values.get(1);
    }

    /**
     * Returns a random long value between minimum and maximum (inclusive)
     *
     * @param minimum
     * @param maximum
     * @return
     */
    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        long range_size = (maximum - minimum) + 1;

        // error checking and 2^x checking removed for simplicity.
        long bits, val;
        do {
            bits = (this.nextLong() << 1) >>> 1;
            val = bits % range_size;
        } while (bits - val + range_size < 0L);
        val += minimum;

        return val;
    }
}
