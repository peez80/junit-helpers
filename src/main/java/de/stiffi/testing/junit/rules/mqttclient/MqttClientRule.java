package de.stiffi.testing.junit.rules.mqttclient;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Assert;
import org.junit.rules.ExternalResource;

import java.util.*;

public class MqttClientRule extends ExternalResource implements MqttCallback {

    private final String brokerhost;
    private final boolean ssl;
    private int brokerPort;
    private final String username;
    private final String password;
    private String truststorePath;
    private String truststorePass;
    private boolean doPrintOnMessageReceived = true;
    private List<String> existingSubscriptions = new ArrayList<>();

    private List<MqttClient> mqttClients = new ArrayList<>();

    private int maxInflightWindow = 10;
    private int clientInstanceCount = 1;

    /**
     * if null, a generated clientId will be used
     */
    private String predefinedClientId = null;

    private String SHARED_SUBSCRIPTION_PREFIX = "$share:SH" + DigestUtils.md5Hex("" + new Random().nextInt()) + ":";

    /**
     * topic - list(messages)
     */
    private List<ReceivedMessage> receivedMessages = new LinkedList<>();
    private MqttMessageHandler messageHandler;
    private boolean doCollectInternally = true;


    public MqttClientRule(String brokerhost, boolean ssl, int brokerPort, String username, String password, String truststorePath, String truststorePass) {
        this.brokerhost = brokerhost;
        this.ssl = ssl;
        this.brokerPort = brokerPort;
        this.username = username;
        this.password = password;
        this.truststorePath = truststorePath;
        this.truststorePass = truststorePass;
    }

    public MqttClientRule withMaxInflight(int maxInflight) {
        maxInflightWindow = maxInflight;
        return this;
    }

    public MqttClientRule withMqttClientInstances(int clientInstanceCount) {
        this.clientInstanceCount = clientInstanceCount;
        return this;
    }

    public MqttClientRule withBrokerPort(int port) {
        this.brokerPort = port;
        return this;
    }

    public MqttClientRule withClientId(String clientId) {
        this.predefinedClientId = clientId;
        return this;
    }

    public MqttClientRule doPrintOnMessageReceived(boolean doPrint) {
        this.doPrintOnMessageReceived = doPrint;
        return this;
    }

    public MqttClientRule withMessageHandler(MqttMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        return this;
    }

    public MqttClientRule withInternalMessageCollection(boolean doCollectInternally) {
        this.doCollectInternally = doCollectInternally;
        return this;
    }

    @Override
    protected void before() throws Throwable {
        connect();
    }

    public void connect() throws MqttException {
        for (int i = 0; i < clientInstanceCount; i++) {
            String clientId = generateClientId(i);
            MqttClient client = connect(clientId);
            mqttClients.add(client);
        }
    }

    private String generateClientId(int counter) {
        return predefinedClientId == null ?
                "MqttClientRuleTesting_"+ counter + "_" + System.currentTimeMillis()
                : predefinedClientId + "_" + counter;
    }

    private MqttClient connect(String clientId) throws MqttException {
        String serverUri = (ssl ? "ssl://" : "tcp://") + brokerhost + ":" + brokerPort;

        MqttClient mqttClient = new MqttClient(serverUri, clientId, new MemoryPersistence());
        mqttClient.setCallback(this);

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setMaxInflight(maxInflightWindow);
        connOpts.setCleanSession(true);
        if (username != null) {
            connOpts.setUserName(username);
        }
        if (password != null) {
            connOpts.setPassword(password.toCharArray());
        }
        connOpts.setAutomaticReconnect(false);

        if (ssl && !StringUtils.isBlank(truststorePath)) {
            Properties sslClientProperties = new Properties();
            sslClientProperties.setProperty(SSLSocketFactoryFactory.SSLPROTOCOL, "TLSv1.2");
            sslClientProperties.setProperty(SSLSocketFactoryFactory.JSSEPROVIDER, "SunJSSE");
            sslClientProperties.setProperty(SSLSocketFactoryFactory.TRUSTSTORETYPE, "JKS");
            sslClientProperties.setProperty(SSLSocketFactoryFactory.TRUSTSTORE, truststorePath);
            sslClientProperties.setProperty(SSLSocketFactoryFactory.TRUSTSTOREPWD, truststorePass);

            connOpts.setSSLProperties(sslClientProperties);
        }


        System.out.println("MQTT Connect " + brokerhost + ":" + brokerPort + ", clientId: " + clientId + "....");
        mqttClient.connect(connOpts);

        return mqttClient;
    }

    @Override
    protected void after() {
        disconnect();
    }

    public void disconnect() {
        System.out.println("MQTT Disconnect...");
        for (MqttClient mqttClient : mqttClients) {
            try {
                if (mqttClient.isConnected()) {
                    mqttClient.disconnect();
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        mqttClients.clear();
        clearReceivedMessages();
        existingSubscriptions.clear();
    }

    public void subscribe(String topic) throws MqttException {
        if (existingSubscriptions.contains(topic)) {
            return;
        }

        existingSubscriptions.add(topic);
        if (clientInstanceCount > 1) {
            topic = SHARED_SUBSCRIPTION_PREFIX + topic;
        }
        for (MqttClient mqttClient : mqttClients) {
            System.out.println("Subscribe " + topic);
            mqttClient.subscribe(topic, 1);
        }
    }

    public void publish(String topic, byte[] payload, int qos) throws MqttException {
        publish(topic, payload, qos, false);
    }

    public void publish(String topic, byte[] payload, int qos, boolean retained) throws MqttException {
        System.out.println("Publishing message to " + topic);
        //Currently we publish on the first client - if this causes trouble - make round robin
        mqttClients.get(0).publish(topic, payload, qos, retained);
    }

    @Override
    public void connectionLost(Throwable cause) {
        throw new IllegalStateException(cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if (messageHandler != null) {
            messageHandler.messageReceived(new ReceivedMessage(topic, message.getPayload(), message.isRetained()));
        }

        if (doCollectInternally) {
            synchronized (receivedMessages) {
                receivedMessages.add(new ReceivedMessage(topic, message.getPayload(), message.isRetained()));
            }
        }

        if (doPrintOnMessageReceived) {
            final String payload;
            if (message.getPayload() == null) {
                payload = null;
            }else if (message.getPayload() != null && StringOrBinaryRecognizer.looksLikeUtf8String(message.getPayload())) {
                payload = new String(message.getPayload(), "UTF-8");
            }else{
                payload = Base64.getEncoder().encodeToString(message.getPayload());
            }

            System.out.println("Received MQTT message on topic "
                    + topic
                    + ", Count: " + getMessages(topic).size()
                    + ", Content: " + payload
            );
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    public List<byte[]> getMessages(String topic) {
        List<byte[]> messages = new ArrayList<>(receivedMessages.size());
        synchronized (receivedMessages) {
            for (ReceivedMessage msg : receivedMessages) {
                if (msg.getTopic().equals(topic)) {
                    messages.add(msg.getPayload());
                }
            }
        }
        return messages;
    }

    public List<ReceivedMessage> getReceivedMessages(String topic) {
        List<ReceivedMessage> messages = new ArrayList<>(receivedMessages.size());
        synchronized (receivedMessages) {
            for (ReceivedMessage msg : receivedMessages) {
                if (msg.getTopic().equals(topic)) {
                    messages.add(msg);
                }
            }
        }
        return messages;
    }

    public List<ReceivedMessage> getMessages() {
        return receivedMessages;
    }

    public void clearReceivedMessages() {
        receivedMessages.clear();
    }

    public void waitForMessage(String topic, long timeoutMs) {
        waitForMessage(topic, timeoutMs, 1);
    }

    public void waitForMessage(String topic, long timeoutMs, int minimalNumberOfMessages) {
        System.out.println("Waiting " + timeoutMs + "ms for >=" + minimalNumberOfMessages + " messages on topic " + topic);
        if (timeoutMs <= 0) {
            return;
        }

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() < start + timeoutMs) {
            try {
                Thread.sleep(50);
                List<byte[]> messages = getMessages(topic);
                if (messages.size() >= minimalNumberOfMessages) {
                    return;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void waitForMessage(String topic) {
        waitForMessage(topic, 10000l);
    }

    /**
     * Assert that at least one or more messages are received
     * @param failedMessage
     * @param topic
     */
    public void assertMessagesReceived(String failedMessage, String topic) {
        assertMessagesReceived(failedMessage, topic, -1);
    }

    public void assertMessagesReceived(String failedMessage, String topic, int expectedMessageCount) {
        assertMessagesReceived(failedMessage, topic, expectedMessageCount, 10000l);
    }

    public void assertMessagesReceived(String failedMessage, String topic, int expectedMessageCount, long waitForMessageTimeout) {
        waitForMessage(topic, waitForMessageTimeout, expectedMessageCount);
        List<byte[]> receivedMessages = getMessages(topic);

        if (expectedMessageCount == -1 && receivedMessages.size() == 0) {
            //-1 means, that an undefined number of messages should be received. At least 1
            Assert.fail(failedMessage +", Topic: " + topic);
        }else if (expectedMessageCount == -1 && receivedMessages.size() > 0) {
            return;
        }else if (receivedMessages.size() != expectedMessageCount) {
            String msg = failedMessage + ", \nExpected : " + expectedMessageCount + " messages on " + topic + "\nActual   : " + receivedMessages.size() + " messages";
            Assert.fail(msg);
        }
    }
}
