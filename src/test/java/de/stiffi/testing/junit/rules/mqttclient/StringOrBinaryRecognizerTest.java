package de.stiffi.testing.junit.rules.mqttclient;

import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class StringOrBinaryRecognizerTest {


    @Test
    public void testJson() throws UnsupportedEncodingException {
        //Given
        byte[] json = "{\"vehicles\":[{\"vin\":\"{{vin}}\",\"cdMappingActive\":true,\"carsharingEnabled\":true,\"cdCarsharingCapable\":false,\"cdCarsharingNaoipActive\":false,\"firstFleetInDate\":\"2015-09-09T12:33:47.205Z\",\"fleetInDate\":\"2019-06-25T08:46:44.749Z\",\"fleetOutDate\":\"2019-06-25T08:45:28.434Z\",\"keyUploadState\":\"AUTOMATIC_TRUE\",\"numberPlate\":\"Reply-Concept\",\"shouldBeImported\":false,\"vehicleRetrofitState\":\"KEY_ALLOWED\",\"fleet\":\"TO-Testing\",\"subsidiary\":\"Sixt TO Absicherung\",\"operator\":\"T&A_EG8_EI4\",\"csmSerialNumber\":\"{{csmSnr}}\",\"csmType\":\"CSM3\",\"csmActivationDate\":\"2019-03-29T17:32:40.097Z\",\"csmProvisioningDate\":\"2019-04-23T16:26:19.374Z\",\"csmProvisioningStatus\":\"PENDING\",\"csmProvisioningEnvironment\":\"INT\",\"csmStatus\":\"COUPLED\",\"csmSoftwareVersion\":\"15\",\"csmCreationDate\":\"2014-09-04T20:42:55.000Z\",\"csmSoftwareUpdateState\":\"ACK\",\"csmSoftwareUpdateDate\":\"2019-04-01T10:20:05.451Z\",\"csmIccid\":\"89314404000075748963\",\"csmSimMsisdn\":\"882393727802394\",\"csmSimStatus\":\"ACTIVE\",\"comboxCertificateCN\":\"{{comboxCertificateCN}}\",\"comboxSerialNumber\":\"7486050000500809E94801004019003416\"}]}".getBytes("UTF-8");

        //When
        boolean result = StringOrBinaryRecognizer.looksLikeUtf8String(json);

        //Then
        Assert.assertTrue("Should have recognized as Text.", result);
    }

    @Test
    public void testBinary() throws UnsupportedEncodingException {
        //Given
        byte[] binaryProto = Base64.getDecoder().decode("UgA=");

        //When
        boolean result = StringOrBinaryRecognizer.looksLikeUtf8String(binaryProto);

        //Then
        Assert.assertFalse("Should have been recognized as binary", result);

    }

    @Test
    public void testShortString() throws UnsupportedEncodingException {
        //Given
        byte[] stringdata = "a".getBytes("UTF-8");

        //When
        boolean result = StringOrBinaryRecognizer.looksLikeUtf8String(stringdata);

        //Then
        Assert.assertTrue("Should have been recognized as string", result);
    }
}