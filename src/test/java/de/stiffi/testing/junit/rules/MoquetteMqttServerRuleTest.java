package de.stiffi.testing.junit.rules;

import de.stiffi.testing.junit.rules.mqttclient.MqttClientRule;
import org.junit.Test;

public class MoquetteMqttServerRuleTest {

    @Test
    public void testStartup() throws Throwable {

        // Given
        MoquetteMqttServerRule underTest = MoquetteMqttServerRule.create();
        underTest.before();


        //When
        MqttClientRule mqttClient = new MqttClientRule(
                "localhost",
                false,
                underTest.getPort(),
                null,
                null,
                null,
                null
                );
        mqttClient.connect();


        //Then
        mqttClient.disconnect();
        underTest.after();



    }
}
