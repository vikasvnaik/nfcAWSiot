package com.vikas.nfc.Controller;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.vikas.nfc.MainActivity;
import com.vikas.nfc.R;
import com.vikas.nfc.utils.Constants;
import com.vikas.nfc.utils.ToastMessage;

import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.UUID;

public class AWSConnection {

    static final String LOG_TAG = AWSConnection.class.getCanonicalName();



    // IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
    //a3ia6ywt1k1290.iot.us-west-2.amazonaws.com
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a36zqdn1wgny04.iot.us-west-2.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    //us-west-2:a5152ac4-1f33-44ec-a398-b27f9e310d2e
    private static final String COGNITO_POOL_ID = "us-west-2:68ec09f4-5b7b-43ea-98d3-1c1030207ce0";
    // Name of the AWS IoT policy to attach to a newly created certificate
    private static final String AWS_IOT_POLICY_NAME = "IoTpolicy";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.DEFAULT_REGION;
    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";
    private static final int MINIMO_LEVEL_WATER = 5;
    //Status with dragonboard
    private boolean mConnectionDragonboard = false;

    private Context mContext;
    private MainActivity mActivity;
    private AWSIotClient mIotAndroidClient;
    private AWSIotMqttManager mqttManager;
    private String mClientId;
    private String mKeystorePath;
    private String mKeystoreName;
    private String mKeystorePassword;
    private Boolean mConnected;
    private int mGlassPosition;
    private int mWaterLevel;
    private int mCoffeeLevel;
    private String mCoffeeMachineStatus;
    private int mShortCoffeeStatus;
    private int mLongCoffeeStatus;

    private KeyStore mClientKeyStore = null;
    private String mCertificateId;

    private CognitoCachingCredentialsProvider mCredentialsProvider;


    private static AWSConnection managerInstance;

    private AWSConnection(Context context) {
        mActivity = (MainActivity) context;
        mContext = context;
    }


    public static AWSConnection getInstance(Context context) {
        if (managerInstance == null) {
            managerInstance = new AWSConnection(context);
        }
        return managerInstance;
    }


    private void cognitoAuthenticate() {

        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        mClientId = UUID.randomUUID().toString();

        // Initialize the AWS Cognito credentials provider
        mCredentialsProvider = new CognitoCachingCredentialsProvider(
                mContext, // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(mClientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);

        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(mCredentialsProvider);
        mIotAndroidClient.setRegion(region);

        mKeystorePath = mContext.getFilesDir().getPath();
        mKeystoreName = KEYSTORE_NAME;
        mKeystorePassword = KEYSTORE_PASSWORD;
        mCertificateId = CERTIFICATE_ID;

        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(mKeystorePath, mKeystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(mCertificateId, mKeystorePath,
                        mKeystoreName, mKeystorePassword)) {
                    Log.i(LOG_TAG, "Certificate " + mCertificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    mClientKeyStore = AWSIotKeystoreHelper.getIotKeystore(mCertificateId,
                            mKeystorePath, mKeystoreName, mKeystorePassword);
                } else {
                    Log.i(LOG_TAG, "Key/cert " + mCertificateId + " not found in keystore.");
                }
            } else {
                Log.i(LOG_TAG, "Keystore " + mKeystorePath + "/" + mKeystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        if (mClientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create a new private key and certificate. This call
                        // creates both on the server and returns them to the
                        // device.
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                        createKeysAndCertificateResult =
                                mIotAndroidClient.createKeysAndCertificate(
                                        createKeysAndCertificateRequest);
                        Log.i(LOG_TAG,
                                "Cert ID: " +
                                        createKeysAndCertificateResult.getCertificateId() +
                                        " created.");

                        // store in keystore for use in MQTT client
                        // saved as alias "default" so a new certificate isn't
                        // generated each run of this application
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(mCertificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                mKeystorePath, mKeystoreName, mKeystorePassword);

                        // load keystore from file into memory to pass on
                        // connection
                        mClientKeyStore = AWSIotKeystoreHelper.getIotKeystore(mCertificateId,
                                mKeystorePath, mKeystoreName, mKeystorePassword);

                        // Attach a policy to the newly created certificate.
                        // This flow assumes the policy was already created in
                        // AWS IoT and we are now just attaching it to the
                        // certificate.
                        AttachPrincipalPolicyRequest policyAttachRequest =
                                new AttachPrincipalPolicyRequest();
                        policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                .getCertificateArn());
                        mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);
                        getConnection();

                    } catch (Exception e) {
                        Log.e(LOG_TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                    }
                }
            }).start();
        }
    }


    public void subscribeAllTopics() {
        Log.d(LOG_TAG, "topic = " + Constants.ALL_TOPICS);

        try {
            mqttManager.subscribeToTopic(Constants.ALL_TOPICS, AWSIotMqttQos.QOS0, new AWSIotMqttNewMessageCallback() {
                @Override
                public void onMessageArrived(final String topic, final byte[] data) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String message = new String(data, "UTF-8");
                                Log.d(LOG_TAG, "Message arrived:");
                                Log.d(LOG_TAG, "   Topic: " + topic);
                                Log.d(LOG_TAG, " Message: " + message);

                                switch (topic) {

                                }
                            } catch (UnsupportedEncodingException e) {
                                Log.e(LOG_TAG, "Message encoding error.", e);
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error.", e);
        }
    }


    public void getConnection() {

        //cognitoAuthenticate();
        mConnected = false;

        try {
            if (mClientKeyStore != null) {
                mqttManager.connect(mClientKeyStore, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status,
                                                final Throwable throwable) {
                        Log.d(LOG_TAG, "Status = " + String.valueOf(status));
                        if (status == AWSIotMqttClientStatus.Connected) {
                            mConnected = true;
                            subscribeAllTopics();
                        } else {
                            mConnected = false;
                        }
                    }
                });
            } else {
                ToastMessage.setToastMessage(mContext, mActivity.getResources().getString(R.string.try_connect), Toast.LENGTH_LONG);
            }
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
        }
    }


    public void topicPublish(String message, String topic) {
        try {
            mqttManager.publishString(message, topic, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Publish error.", e);
        }
    }


    public void disconnectAWS() {
        try {
            mqttManager.disconnect();
            managerInstance = null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Disconnect error.", e);
        }
    }


    public boolean isConnected() {
        return mConnected;
    }
}
