package de.stiffi.testing.junit.rules;

import de.stiffi.testing.junit.helpers.SocketHelper;
import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.util.*;

public class MoquetteMqttServerRule extends ExternalResource {

    private int port = -1;

    private Server mqttBroker;

    private List<InterceptConnectMessage> connectMessages = new ArrayList<>();
    private List<InterceptPublishMessage> publishMessages = new ArrayList<>();
    private List<InterceptSubscribeMessage> subscribeMessages = new ArrayList<>();


    private MoquetteMqttServerRule() {
        port = SocketHelper.findFreePort();
    }

    public static MoquetteMqttServerRule create() {
        MoquetteMqttServerRule me = new MoquetteMqttServerRule();
        return me;
    }


    @Override
    protected void before() throws Throwable {
        startServer();
    }


    @Override
    protected void after() {
        stopServer();
    }

    public void startServer() throws IOException {
        mqttBroker = new Server();
        Properties configProperties = new Properties();
        configProperties.setProperty("host", "0.0.0.0");
        configProperties.setProperty("port", String.valueOf(port));
        configProperties.setProperty("allow_anonymous", "true");
        MemoryConfig memoryConfig= new MemoryConfig(configProperties);

        mqttBroker.startServer(memoryConfig, Arrays.asList(new MyInterceptHandler()));
        System.out.println("Started Moquette MQTT Server on Port " + port);
    }

    public void stopServer() {
        mqttBroker.stopServer();
        System.out.println("Stopped Moquette MQTT Server");
        mqttBroker = null;
    }

    public int getPort() {
        return port;
    }

    public List<InterceptConnectMessage> getConnectMessages() {
        return connectMessages;
    }

    public List<InterceptPublishMessage> getPublishMessages() {
        return publishMessages;
    }

    public List<InterceptSubscribeMessage> getSubscribeMessages() {
        return subscribeMessages;
    }

    public void clearMessages() {
        connectMessages.clear();
        subscribeMessages.clear();
        publishMessages.clear();
    }

    public class MyInterceptHandler extends AbstractInterceptHandler {


        @Override
        public String getID() {
            return "InterceptHandler";
        }

        @Override
        public void onConnect(InterceptConnectMessage msg) {
            connectMessages.add(msg);
            System.out.println("CONNECT clientID: " + msg.getClientID());
        }

        @Override
        public void onDisconnect(InterceptDisconnectMessage msg) {
            System.out.println("DISCONNECT " + msg.getClientID());
        }

        @Override
        public void onConnectionLost(InterceptConnectionLostMessage msg) {
            System.out.println("CONNECTION LOST " + msg.getClientID());
        }

        @Override
        public void onPublish(InterceptPublishMessage msg) {
            publishMessages.add(msg);
            String payload = Base64.getEncoder().encodeToString(msg.getPayload().array());
            System.out.println("PUBLISH:  clientID: " + msg.getClientID() + "topic: " + msg.getTopicName() + ", payload: " + payload);
        }

        @Override
        public void onSubscribe(InterceptSubscribeMessage msg) {
            subscribeMessages.add(msg);
            System.out.println("SUBSCRIBE clientID: " + msg.getClientID() + ", topic: " + msg.getTopicFilter() );
        }

        @Override
        public void onUnsubscribe(InterceptUnsubscribeMessage msg) {
            logInterceptedMessage(msg);
        }

        @Override
        public void onMessageAcknowledged(InterceptAcknowledgedMessage msg) {
            logInterceptedMessage(msg);
        }

        private void logInterceptedMessage(Object msg) {
            System.out.println("MQTT Server event: " + ToStringBuilder.reflectionToString(msg));
        }
    }
}
