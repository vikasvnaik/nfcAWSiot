package com.vikas.nfc

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings.ACTION_NFC_SETTINGS
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos
import com.amazonaws.regions.Regions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.vikas.nfc.Controller.AWSConnection
import com.vikas.nfc.parser.NdefMessageParser
import com.vikas.nfc.utils.Utils
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*


class MainActivity : Activity() {

    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLastLocation: Location? = null

    private var nfcAdapter: NfcAdapter? = null
    // launch our application when a new Tag or Card will be scanned
    private var pendingIntent: PendingIntent? = null
    // display the data read
    private var text: TextView? = null

    //AWS connection manager object
    private var mAWSConnection: AWSConnection? = null

    val LOG_TAG = PubSubActivity::class.java.canonicalName

    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    private val CUSTOMER_SPECIFIC_ENDPOINT =
        "a36zqdn1wgny04.iot.us-west-2.amazonaws.com"

    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private val COGNITO_POOL_ID = "us-west-2:68ec09f4-5b7b-43ea-98d3-1c1030207ce0"

    // Region of AWS IoT
    private val MY_REGION = Regions.US_WEST_2

    var txtSubscribe: EditText? = null

    var tvLastMessage: TextView? = null
    var tvClientId: TextView? = null
    var tvStatus: TextView? = null
    var nfcData: TextView? = null

    var btnConnect: Button? = null
    var btnSubscribe: Button? = null
    var btnDisconnect: Button? = null

    var mqttManager: AWSIotMqttManager? = null
    var clientId: String? = null

    var credentialsProvider: CognitoCachingCredentialsProvider? = null
    val topic = "aws/things/Testing-device/shadow/update"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*val aswAcivity = Intent(this,PubSubActivity::class.java)
        startActivity(aswAcivity)*/

       /* if (!checkPermissions()) {
            requestPermissions();
        } else {
            getLastLocation();
        }*/
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        nfcData = findViewById<View>(R.id.nfcData) as TextView
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        //startConnectionAWS();
        if (nfcAdapter == null) {
            Toast.makeText(this, "No NFC", Toast.LENGTH_SHORT).show()
            //finish()
            //return
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, this.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )

        //AWS PART

        txtSubscribe = findViewById<View>(R.id.txtSubscribe) as EditText

        tvLastMessage = findViewById<View>(R.id.tvLastMessage) as TextView
        tvClientId = findViewById<View>(R.id.tvClientId) as TextView
        tvStatus = findViewById<View>(R.id.tvStatus) as TextView

        btnConnect = findViewById<View>(R.id.btnConnect) as Button
        btnConnect!!.setOnClickListener(connectClick)
        btnConnect!!.isEnabled = false

        btnSubscribe = findViewById<View>(R.id.btnSubscribe) as Button
        btnSubscribe!!.setOnClickListener(subscribeClick)

        btnDisconnect = findViewById<View>(R.id.btnDisconnect) as Button
        btnDisconnect!!.setOnClickListener(disconnectClick)

        // MQTT client IDs are required to be unique per AWS IoT account.
// This UUID is "practically unique" but does not _guarantee_
// uniqueness.
        clientId = UUID.randomUUID().toString()
        tvClientId!!.text = clientId

        // Initialize the AWS Cognito credentials provider
        // Initialize the AWS Cognito credentials provider
        credentialsProvider = CognitoCachingCredentialsProvider(
            applicationContext,  // context
            COGNITO_POOL_ID,  // Identity Pool ID
            MY_REGION // Region
        )

        // MQTT Client
        // MQTT Client
        mqttManager = AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT)

        // The following block uses a Cognito credentials provider for authentication with AWS IoT.
        // The following block uses a Cognito credentials provider for authentication with AWS IoT.
        Thread(Runnable { runOnUiThread { btnConnect!!.isEnabled = true } }).start()
    }


    override fun onResume() {
        super.onResume()

        val nfcAdapterRefCopy = nfcAdapter
        if (nfcAdapterRefCopy != null) {
            if (!nfcAdapterRefCopy.isEnabled())
                showNFCSettings()

            nfcAdapterRefCopy.enableForegroundDispatch(this, pendingIntent, null, null)
        }
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        resolveIntent(intent)
    }

    private fun showNFCSettings() {
        Toast.makeText(this, "You need to enable NFC", Toast.LENGTH_SHORT).show()
        val intent = Intent(ACTION_NFC_SETTINGS)
        startActivity(intent)
    }

    /**
     * Tag data is converted to string to display
     *
     * @return the data dumped from this tag in String format
     */
    private fun dumpTagData(tag: Tag): String {
        val sb = StringBuilder()
        val id = tag.getId()
        //sb.append("ID (hex): ").append(Utils.toHex(id)).append('\n')
        //sb.append("ID (reversed hex): ").append(Utils.toReversedHex(id)).append('\n')
        //sb.append("ID (dec): ").append(Utils.toDec(id)).append('\n')
        sb.append(Utils.toDec(id)).append('\n')
        //sb.append("ID (reversed dec): ").append(Utils.toReversedDec(id)).append('\n')

        val prefix = "android.nfc.tech."
        //sb.append("Technologies: ")
        /*for (tech in tag.getTechList()) {
            sb.append(tech.substring(prefix.length))
            sb.append(", ")
        }*/

        sb.delete(sb.length - 2, sb.length)

        for (tech in tag.getTechList()) {
            if (tech == MifareClassic::class.java.name) {
                sb.append('\n')
                var type = "Unknown"

                try {
                    val mifareTag = MifareClassic.get(tag)

                    when (mifareTag.type) {
                        MifareClassic.TYPE_CLASSIC -> type = "Classic"
                        MifareClassic.TYPE_PLUS -> type = "Plus"
                        MifareClassic.TYPE_PRO -> type = "Pro"
                    }
                    sb.append("Mifare Classic type: ")
                    sb.append(type)
                    sb.append('\n')

                    sb.append("Mifare size: ")
                    sb.append(mifareTag.size.toString() + " bytes")
                    sb.append('\n')

                    sb.append("Mifare sectors: ")
                    sb.append(mifareTag.sectorCount)
                    sb.append('\n')

                    sb.append("Mifare blocks: ")
                    sb.append(mifareTag.blockCount)
                } catch (e: Exception) {
                    sb.append("Mifare classic error: " + e.message)
                }

            }

            if (tech == MifareUltralight::class.java.name) {
                sb.append('\n')
                val mifareUlTag = MifareUltralight.get(tag)
                var type = "Unknown"
                when (mifareUlTag.type) {
                    MifareUltralight.TYPE_ULTRALIGHT -> type = "Ultralight"
                    MifareUltralight.TYPE_ULTRALIGHT_C -> type = "Ultralight C"
                }
                sb.append("Mifare Ultralight type: ")
                sb.append(type)
            }
        }

        return sb.toString()
    }

    private fun resolveIntent(intent: Intent) {
        val action = intent.action

        if (NfcAdapter.ACTION_TAG_DISCOVERED == action
            || NfcAdapter.ACTION_TECH_DISCOVERED == action
            || NfcAdapter.ACTION_NDEF_DISCOVERED == action
        ) {
            val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            Log.i("NFC", "Size:" + rawMsgs);
            if (rawMsgs != null) {
                Log.i("NFC", "Size:" + rawMsgs.size);
                val ndefMessages: Array<NdefMessage> =
                    Array(rawMsgs.size, { i -> rawMsgs[i] as NdefMessage });
                displayNfcMessages(ndefMessages)
            } else {
                val empty = ByteArray(0)
                val id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)
                val tag = intent.getParcelableExtra<Parcelable>(NfcAdapter.EXTRA_TAG) as Tag
                val payload = dumpTagData(tag).toByteArray()
                val record = NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, payload)
                val emptyMsg = NdefMessage(arrayOf(record))
                val emptyNdefMessages: Array<NdefMessage> = arrayOf(emptyMsg);
                displayNfcMessages(emptyNdefMessages)
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun displayNfcMessages(msgs: Array<NdefMessage>?) {
        if (msgs == null || msgs.isEmpty())
            return

        val builder = StringBuilder()
        val records = NdefMessageParser.parse(msgs[0])
        val size = records.size

        for (i in 0 until size) {
            val record = records[i]
            val str = record.str()
            builder.append(str).append("\n")
        }

        //val loc = "Location : " +getLastLocation();

        val data = JSONObject()
        val random =  Random().nextInt(1000);
        data.put("row",random)
        val random1 =  Random().nextInt(1000);
        data.put("pos",random1)
        val rfidData = JSONObject()
        rfidData.put("id",builder.toString())
        data.put("payload",rfidData)

        mqttManager!!.publishString(data.toString(),topic, AWSIotMqttQos.QOS0)
        nfcData?.setText(builder.toString())
    }

    private fun getLastLocation(): String? {
        var location: String?= null;
        mFusedLocationClient?.getLastLocation()
            ?.addOnCompleteListener(this,
                OnCompleteListener<Location?> { task ->
                    if (task.isSuccessful && task.result != null) {
                        mLastLocation = task.result
                        location =  mLastLocation?.latitude.toString()  + mLastLocation?.longitude.toString()
                    } else { //Log.w(TAG, "getLastLocation:exception", task.getException());
                        //showSnackbar(getString(R.string.no_location_detected));
                    }
                })
        return location;
    }

    /**
     * Shows a [Snackbar].
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private fun showSnackbar(
        mainTextStringId: Int, actionStringId: Int,
        listener: View.OnClickListener
    ) {
        Snackbar.make(
            findViewById(android.R.id.content),
            getString(mainTextStringId),
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(getString(actionStringId), listener).show()
    }

    //AWS PART

    var connectClick =
        View.OnClickListener {
            Log.d(PubSubActivity.LOG_TAG, "clientId = $clientId")
            try {
                mqttManager!!.connect(
                    credentialsProvider
                ) { status, throwable ->
                    Log.d(
                        PubSubActivity.LOG_TAG,
                        "Status = $status"
                    )
                    runOnUiThread {
                        if (status == AWSIotMqttClientStatus.Connecting) {
                            tvStatus!!.text = "Connecting..."
                        } else if (status == AWSIotMqttClientStatus.Connected) {
                            tvStatus!!.text = "Connected"
                        } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                            if (throwable != null) {
                                Log.e(
                                    PubSubActivity.LOG_TAG,
                                    "Connection error.",
                                    throwable
                                )
                                throwable.printStackTrace()
                            }
                            tvStatus!!.text = "Reconnecting"
                        } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                            if (throwable != null) {
                                Log.e(
                                    PubSubActivity.LOG_TAG,
                                    "Connection error.",
                                    throwable
                                )
                                throwable.printStackTrace()
                            }
                            tvStatus!!.text = "Disconnected"
                        } else {
                            tvStatus!!.text = "Disconnected"
                        }
                    }
                }
            } catch (e: java.lang.Exception) {
                Log.e(PubSubActivity.LOG_TAG, "Connection error.", e)
                tvStatus!!.text = "Error! " + e.message
            }
        }

    var subscribeClick =
        View.OnClickListener {
            //final String topic = txtSubscribe.getText().toString();
            val topic = "aws/things/Testing-device/shadow/update"
            Log.d(PubSubActivity.LOG_TAG, "topic = $topic")
            //String payload = "the payload";
//Log.d(TAG, "Inside publishToServer payload: " + payload);

            try {
                mqttManager!!.subscribeToTopic(
                    topic, AWSIotMqttQos.QOS0
                ) { topic, data ->
                    runOnUiThread {
                        try {
                            var message: String? = null;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                message = String(data, StandardCharsets.UTF_8)
                            } else {
                                message = String(data, Charset.forName("UTF-8"))
                            }
                            Log.d(
                                PubSubActivity.LOG_TAG,
                                "Message arrived:"
                            )
                            Log.d(
                                PubSubActivity.LOG_TAG,
                                "   Topic: $topic"
                            )
                            Log.d(
                                PubSubActivity.LOG_TAG,
                                " Message: $message"
                            )
                            tvLastMessage!!.text = message
                        } catch (e: UnsupportedEncodingException) {
                            Log.e(
                                PubSubActivity.LOG_TAG,
                                "Message encoding error.",
                                e
                            )
                        }
                    }
                }
            } catch (e: java.lang.Exception) {
                Log.e(PubSubActivity.LOG_TAG, "Subscription error.", e)
            }
        }


    var disconnectClick =
        View.OnClickListener {
            try {
                mqttManager!!.disconnect()
            } catch (e: java.lang.Exception) {
                Log.e(PubSubActivity.LOG_TAG, "Disconnect error.", e)
            }
        }




}
