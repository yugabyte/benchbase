package com.oltpbenchmark.benchmarks.featurebench;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.oltpbenchmark.benchmarks.featurebench.workerhelpers.YamlHelpers.Properties;

import java.io.File;
import java.io.IOException;

public class YamlValidator {

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File file = new File(classLoader.getResource("7.yaml").getFile());
        Properties properties = mapper.readValue(file, Properties.class);
        System.out.println(properties.getProperties().toString());
    }
}