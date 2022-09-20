package com.oltpbenchmark;


import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DisabledListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;

public class YAMLTest {
    public static void main(String[] args) throws ConfigurationException {
        YAMLConfiguration co = buildConfiguration("/Users/shaharukshaikh/code/benchbase/config/yugabyte/sample_featurebench_config.yaml");
        String driverFromYaml = co.getString("/parameters/driver");

        // converting the yaml to the xmlConfig which is required by benchmark
        XMLConfiguration conf = new XMLConfiguration(co);
        conf.setListDelimiterHandler(new DisabledListDelimiterHandler());
        conf.setExpressionEngine(new XPathExpressionEngine());
        String driverFromConvertedXML = conf.getString("/parameters/driver");

        System.out.printf("Driver from yaml: %s\tDriver from xml: %s", driverFromYaml, driverFromConvertedXML);
    }
    public static YAMLConfiguration buildConfiguration(String filename) throws ConfigurationException {
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<YAMLConfiguration> builder = new FileBasedConfigurationBuilder<>(YAMLConfiguration.class)
            .configure(params.hierarchical()
                .setFileName(filename)
                .setListDelimiterHandler(new DisabledListDelimiterHandler())
                .setExpressionEngine(new XPathExpressionEngine()));
        return builder.getConfiguration();
    }
}
