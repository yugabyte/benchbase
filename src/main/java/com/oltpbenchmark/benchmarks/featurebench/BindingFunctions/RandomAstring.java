package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;

public class RandomAstring extends Random implements BaseUtil {

    private int minimum_length;
    private int maximum_length;

    public RandomAstring(List<Object> values) {
        super((int) System.currentTimeMillis());
        this.minimum_length = (int) values.get(0);
        this.maximum_length = (int) values.get(1);
    }

    /**
     * @returns a random alphabetic string with length in range [minimum_length,
     *          maximum_length].
     */
    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        return randomString(minimum_length, maximum_length, 'a', 26);
    }
      private String randomString(int minimum_length, int maximum_length, char base, int numCharacters) {
        int length = number(minimum_length, maximum_length);
        byte baseByte = (byte) base;
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; ++i) {
            bytes[i] = (byte) (baseByte + number(0, numCharacters - 1));
        }
        return new String(bytes);
    }
    public int number(int minimum, int maximum) {

        int range_size = maximum - minimum + 1;
        int value = this.nextInt(range_size);
        value += minimum;

        return value;
    }

}
