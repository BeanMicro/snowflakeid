package io.beandev.snowflakeid;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.SelectClasspathResources;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import static java.lang.System.out;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource(".")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
@ConfigurationParameter(key = "mr", value = "bean")
@ConfigurationParameter(key = "cucumber.filter.tags", value = "not @service")
public class RunCucumberTest {
    @Before
    public void before(Scenario scenario) {
        out.println("before each Integration Test Scenario for scenario " + scenario.getName());
//        if (System.getProperty("TEST") == null)
        out.println("Mr Bean is " + System.getProperty("bean"));
        System.setProperty("TEST", "INTEGRATION");
    }
}
