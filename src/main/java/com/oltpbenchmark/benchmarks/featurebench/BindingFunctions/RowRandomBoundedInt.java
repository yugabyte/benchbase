package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

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


public class RowRandomBoundedInt
    extends RowRandomInt implements BaseUtil {
    private final int lowValue;
    private final int highValue;

    public RowRandomBoundedInt(List<Object> values) {
        //super(seed,seedsPerRow);
        super((long) values.get(0), (int) values.get(3));
        this.lowValue = (int) values.get(1);
        this.highValue = (int) values.get(2);

    }

    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return nextInt(lowValue, highValue);
    }
}

