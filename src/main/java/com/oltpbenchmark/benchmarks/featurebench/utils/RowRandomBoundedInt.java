package com.oltpbenchmark.benchmarks.featurebench.utils;

/*
 * Copyright 2020 Trino
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class RowRandomBoundedInt implements BaseUtil {
    private final int lowValue;
    private final int highValue;

    public RowRandomBoundedInt(List<Object> values) {
        if (values.size() != 2) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        this.lowValue = ((Number) values.get(0)).intValue();
        this.highValue = ((Number) values.get(1)).intValue();
        if (lowValue > highValue)
            throw new RuntimeException("Please enter correct value for max and min value");

    }

    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException,
        NoSuchMethodException, InstantiationException, IllegalAccessException {
        return ThreadLocalRandom.current().nextInt(lowValue, highValue + 1);
    }
}

