package cy.app.bt.hfpclient.cyphon;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HfpClientMainActivity extends Activity implements View.OnClickListener{

    private static final String TAG = "CYHfpClientMainActivity";
    private static final boolean MULTI_DEVICE_ENABLED = true;
    private static String EXTRA_INDICATOR_INFO = "cy.app.bt.hfpclient.cyphon.EXTRA_INDICATOR_INFO";
    private static String EXTRA_INFO_STRING = "cy.app.bt.hfpclient.cyphon.EXTRA_INFO_STRING";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int DEVICE_INDEX_0 = 0;
    private static final int DEVICE_INDEX_1 = 1;
    private static final int NETWORK_UNAVAILABLE = 0;
    private static final int NETWORK_AVAILABLE = 1;
    private static final int NO_ROAMING = 0;
    private static final int ACTIVE_ROAMING = 1;
    private static final int VR_STOPPED = 0;
    private static final int VR_STARTED = 1;
    private static final int WBS_NONE = 0;
    private static final int WBS_YES = 1;
    private static final int INBAND_STATE_OFF = 0;
    private static final int INBAND_STATE_ON = 1;

    /*Alert variables*/
    private AudioManager ag;
    private PowerManager pm;
    private KeyguardManager keyguardMgr;
    private Vibrator vibrator;
    private PowerManager.WakeLock wl;

    /*View variables*/
    private TextView displayState;
    private TextView displayNumber;
    private TextView displayState2;
    private TextView displayNumber2;
    private TextView numberEntryText;
    private EditText numberEntry;
    private Button textButton; /*To display handsfree, private mode strings*/
    private Button redial;
    private Button textButton2; /*to display answer, dial strings*/
    private Button endCall;
    private ImageView batteryStatus;
    private ImageView signalStatus;
    private ListView callListView;
    private Button callControl;
    private Button enhancedCallControl;
    private Button vrControl; /*used as Send key press button in HSP connection mode*/
    private Button swapUiButton; /*used to swap Ui between connected devices*/
    private TextView operatorName;
    private TextView subscriberNumber;
    private TextView wbsStatus;
    private TextView inBandStatus;

    /*Object instance variables*/
    private BluetoothAdapter mBluetoothAdapter;
    private SharedPreferences pref;
    private NotificationManager mNotificationManager;
    private BluetoothHeadsetClient mBluetoothHeadsetClient;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothDevice mCurrentCallingDevice;
    private String dialedNumber;
    private String incomingCallNumber;
    private String waitingCallNumber;
    private String mDeviceName;
    private String mDeviceAddress;
    private int mPeerFeatures;
    private int mChldFeatures;
    private boolean hasAudioFocused;
    private int currentSignalStrength;
    private int currentBatteryCharge;
    private boolean isVibrating;
    private boolean isRoaming;
    private ArrayAdapter callItems;
    private SharedPreferences.Editor editor;
    private final String answerStr = "Answer";
    private final String dialStr = "Dial";
    private final String rejectStr = "Reject";
    private final String endCallStr = "End Call";
    private final String vrOnStr = "VR On";
    private final String vrOffStr = "VR Off";
    private final String wbsOnStr = "WBS On";
    private final String wbsOffStr = "WBS Off";
    private final String privacyMode = "Privacy Mode";
    private final String handsfreeMode = "Handsfree Mode";
    private final String holdCall = "Hold call";
    private final String unholdCall = "Unhold call";
    private final String multiCallControl = "Call control options";
    private final String inBandOffStr = "InBand Off";
    private final String inBandOnStr = "InBand On";

    /*Volume dialog*/
    private Dialog mVolumeDialog;


    private final String sendKeyPress = "Send key press";
    private int mVrState = 0;
    private int callState = -1;

    /*GUI message codes*/
    public static final int GUI_UPDATE_DEVICE_STATUS = 1;
    public static final int GUI_UPDATE_CALL_STATUS = 2;
    public static final int GUI_UPDATE_DEVICE_INDICATORS = 3;
    public static final int GUI_UPDATE_INCOMING_CALL_NUMBER = 4;
    public static final int GUI_UPDATE_AUDIO_STATE = 5;
    public static final int GUI_UPDATE_VENDOR_AT_RSP = 6;
    public static final int GUI_UPDATE_CLCC_AT_RSP = 7;
    public static final int GUI_UPDATE_VOLUME = 8;
    public static final int GUI_UPDATE_OPERATOR = 9;
    public static final int GUI_UPDATE_SUBSCRIBER = 10;
    public static final int GUI_UPDATE_VR_STATE = 11;
    public static final int GUI_UPDATE_PHONEBOOK_AT_RSP = 12;
    public static final int GUI_UPDATE_WBS_STATE = 13;
    public static final int GUI_UPDATE_RING = 14;
    public static final int GUI_UPDATE_IN_BAND_STATUS = 15;
    public static final int GUI_UPDATE_AG_BUSY = 16;

    private boolean isHSPConnection;

    public BroadcastReceiver br = new MyBroadcastReceiver();
    private Context mContext;
    private HashMap<BluetoothDevice, DeviceInfo> mDeviceMap = new HashMap<>();

    private class DeviceInfo {
        public String mDeviceName;
        public String mDeviceAddress;
        public int mPeerFeatures;
        public int mChldFeatures;
        public boolean hasAudioFocused;
        public int currentSignalStrength;
        public int currentBatteryCharge;
        public boolean isRoaming;
        public int mWbsState;
        public String dialedNumber;
        public String incomingCallNumber;
        public String waitingCallNumber;
        public int callState;
        public String mOperatorName;
        public String mSubscriberNum;

        public DeviceInfo(BluetoothDevice device) {
            if(null != device) {
                mDeviceName = device.getName();
                mDeviceAddress = device.getAddress();
                currentSignalStrength = 1;
                currentBatteryCharge = 1;
                isRoaming = false;
                mWbsState = WBS_NONE;
                dialedNumber = "";
                incomingCallNumber = "";
                waitingCallNumber = "";
                callState = -1;
            }
        }

        public void updateDeviceInfo(Bundle b) {
            if(b.containsKey(BluetoothHeadsetClient.EXTRA_AG_FEATURE_3WAY_CALLING)) {
                if(b.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_3WAY_CALLING)) {
                    mPeerFeatures |= CyHfpClientDeviceConstants.PEER_FEAT_3WAY;
                } else {
                    mPeerFeatures &= ~(CyHfpClientDeviceConstants.PEER_FEAT_3WAY);
                }
            }
            if(b.containsKey(BluetoothHeadsetClient.EXTRA_AG_FEATURE_REJECT_CALL)) {
                if(b.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_REJECT_CALL)) {
                    mPeerFeatures |= CyHfpClientDeviceConstants.PEER_FEAT_REJECT;
                } else {
                    mPeerFeatures &= ~(CyHfpClientDeviceConstants.PEER_FEAT_REJECT);
                }
            }
            if(b.containsKey(BluetoothHeadsetClient.EXTRA_AG_FEATURE_ECC)) {
                if(b.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_ECC)) {
                    mPeerFeatures |= CyHfpClientDeviceConstants.PEER_FEAT_ECC;
                } else {
                    mPeerFeatures &= ~(CyHfpClientDeviceConstants.PEER_FEAT_ECC);
                }
            }
            if(b.containsKey(BluetoothHeadsetClient.EXTRA_AG_FEATURE_ACCEPT_HELD_OR_WAITING_CALL)) {
                if(b.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_ACCEPT_HELD_OR_WAITING_CALL)) {
                    mChldFeatures |= CyHfpClientDeviceConstants.CHLD_FEAT_HOLD_ACC;
                } else {
                    mChldFeatures &= ~(CyHfpClientDeviceConstants.CHLD_FEAT_HOLD_ACC);
                }
            }
            if(b.containsKey(BluetoothHeadsetClient.EXTRA_AG_FEATURE_RELEASE_HELD_OR_WAITING_CALL)) {
                if(b.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_RELEASE_HELD_OR_WAITING_CALL)) {
                    mChldFeatures |= CyHfpClientDeviceConstants.CHLD_FEAT_REL;
                } else {
                    mChldFeatures &= ~(CyHfpClientDeviceConstants.CHLD_FEAT_REL);
                }
            }
            if(b.containsKey(BluetoothHeadsetClient.EXTRA_AG_FEATURE_RELEASE_AND_ACCEPT)) {
                if(b.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_RELEASE_AND_ACCEPT)) {
                    mChldFeatures |= CyHfpClientDeviceConstants.CHLD_FEAT_REL_ACC;
                } else {
                    mChldFeatures &= ~(CyHfpClientDeviceConstants.CHLD_FEAT_REL_ACC);
                }
            }
            if(b.containsKey(BluetoothHeadsetClient.EXTRA_AG_FEATURE_MERGE)) {
                if(b.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_MERGE)) {
                    mChldFeatures |= CyHfpClientDeviceConstants.CHLD_FEAT_MERGE;
                } else {
                    mChldFeatures &= ~(CyHfpClientDeviceConstants.CHLD_FEAT_MERGE);
                }
            }
            if(b.containsKey(BluetoothHeadsetClient.EXTRA_AG_FEATURE_MERGE_AND_DETACH)) {
                if(b.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_MERGE_AND_DETACH)) {
                    mChldFeatures |= CyHfpClientDeviceConstants.CHLD_FEAT_MERGE_DETACH;
                } else {
                    mChldFeatures &= ~(CyHfpClientDeviceConstants.CHLD_FEAT_MERGE_DETACH);
                }
            }
            if(b.containsKey(BluetoothHeadsetClient.EXTRA_AUDIO_WBS)) {
                if(b.getBoolean(BluetoothHeadsetClient.EXTRA_AUDIO_WBS)) {
                    mWbsState = WBS_YES;
                } else {
                    mWbsState = WBS_NONE;
                }
            }
        }

        public void resetCallStateVariable() {
            incomingCallNumber = null;
            waitingCallNumber = null;
            dialedNumber = null;
        }
    }

    private synchronized DeviceInfo getDeviceInfo(BluetoothDevice device) {
        if(null == device) {
            Log.e(TAG, "getDeviceInfo: Device is null!!!");
            return null;
        }
        DeviceInfo di = mDeviceMap.get(device);
        if(null != di) {
            Log.d(TAG, "getDeviceInfo: found device info for device " + device);
            return di;
        }
        /*Allocate a new DI*/
        Log.d(TAG, "getDeviceInfo: creating a new device info for " + device);
        di = new DeviceInfo(device);
        mDeviceMap.put(device, di);
        return di;
    }

    private void clearDeviceInfo(BluetoothDevice device) {
        if(mDeviceMap.containsKey(device)) {
            mDeviceMap.remove(device);
        }
        /*If active device in GUI is disconnected we need to update the GUI*/
        if(!mDeviceMap.isEmpty()) {
            if(mBluetoothDevice.equals(device)) {
                for(Map.Entry<BluetoothDevice, DeviceInfo> entry :
                        mDeviceMap.entrySet()) {
                    if(null != entry.getValue()) {
                        swapUi(entry.getKey());
                        break;
                    }
                }
            }
            mCurrentCallingDevice = mBluetoothDevice;
        } else {
            Log.d(TAG, "clearDeviceInfo: Notification cancelled..");
            /*Cancel Notification*/
            mNotificationManager.cancel(CyHfpClientDeviceConstants.HF_NOTIFICATION_ID);
            releaseResources();
            finish();
        }
    }

    private BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.i(TAG, "onServiceConnected()");
            if (BluetoothProfile.HEADSET_CLIENT == profile) {
                if (null == mBluetoothHeadsetClient) {
                    mBluetoothHeadsetClient = (BluetoothHeadsetClient) proxy;
                }
            /*Event handler does not exist in Android-O*/
                /*Set call volume to maximum*/
                ag.setStreamVolume(AudioManager.STREAM_VOICE_CALL, ag.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), AudioManager.FLAG_SHOW_UI);
                ag.setStreamVolume(6, ag.getStreamMaxVolume(6), AudioManager.FLAG_SHOW_UI);
                updateUi();
            } else {
                Log.d(TAG, "onServiceConnected: Wrong profile");
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Log.i(TAG, "onServiceDisconnected()");
            if (BluetoothProfile.HEADSET_CLIENT == profile) {
                mBluetoothHeadsetClient = null;
                Toast.makeText(getApplicationContext(), "HF app closes as HF service is disconnected ",
                        Toast.LENGTH_LONG).show();
                if (null != mNotificationManager) {
                    mNotificationManager.cancel(CyHfpClientDeviceConstants.HF_NOTIFICATION_ID);
                }
                finish();
            } else {
                Log.d(TAG, "onServiceDisconnected: Not HFP Client");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hfp_client_main);
        Log.d(TAG, "onCreate()");

        /*Get Bluetooth adapter*/
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (null == mBluetoothAdapter) {
            Log.d(TAG, "Device does not support bluetooth");
            finish();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            bluetoothSetupPostProcess();
        }

        mContext = this;
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadsetClient.ACTION_AG_EVENT);
        filter.addAction(BluetoothHeadsetClient.ACTION_CALL_CHANGED);
        filter.addAction(BluetoothHeadsetClient.ACTION_AUDIO_STATE_CHANGED);
        this.registerReceiver(br, filter);


        //c.getLatestInboxMessages(device, 2);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if (RESULT_OK == resultCode) {
                    Log.d(TAG, "Bluetooth turned on properly");
                    bluetoothSetupPostProcess();
                } else {
                    Log.d(TAG, "Bluetooth did not turn on properly");
                    finish();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void bluetoothSetupPostProcess() {

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        ag = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        pm = (PowerManager)getSystemService(POWER_SERVICE);
        keyguardMgr = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        currentSignalStrength = 1;
        currentBatteryCharge = 1;
        isVibrating = false;
        displayState = (TextView)findViewById(R.id.titletext);
        displayNumber = (TextView)findViewById(R.id.displaynumber);
        displayState2 = (TextView)findViewById(R.id.titletext2);
        displayNumber2 = (TextView)findViewById(R.id.displaynumber2);
        numberEntryText = (TextView)findViewById(R.id.enternumber);
        numberEntry = (EditText)findViewById(R.id.edittext);
        textButton = (Button)findViewById(R.id.disconnect_button);
        redial = (Button)findViewById(R.id.redial_button);
        textButton2 = (Button)findViewById(R.id.dial_button);
        endCall = (Button)findViewById(R.id.endcall_button);
        batteryStatus = (ImageView)findViewById(R.id.battery);
        signalStatus = (ImageView)findViewById(R.id.signal);
        callListView = (ListView)findViewById(R.id.call_list);
        callControl = (Button)findViewById(R.id.call_control);
        enhancedCallControl = (Button)findViewById(R.id.enhanced_call_control);
        vrControl = (Button)findViewById(R.id.vr_button);
        swapUiButton = (Button)findViewById(R.id.swap_ui_button);
        operatorName = (TextView)findViewById(R.id.operator);
        subscriberNumber = (TextView)findViewById(R.id.subscriberInfo);
        wbsStatus = (TextView)findViewById(R.id.wbsStatus);
        inBandStatus = (TextView)findViewById(R.id.inbandStatus);

        textButton.setOnClickListener(this);
        redial.setOnClickListener(this);
        textButton2.setOnClickListener(this);
        endCall.setOnClickListener(this);
        callControl.setOnClickListener(this);
        enhancedCallControl.setOnClickListener(this);
        vrControl.setOnClickListener(this);
        swapUiButton.setOnClickListener(this);

        numberEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                /*Do nothing or -TODO*/
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<BluetoothHeadsetClientCall> callInfo = mBluetoothHeadsetClient.getCurrentCalls(mBluetoothDevice);
                /*Check if there is any active call - Multi-device Support -TODO*/
                if(callInfo.isEmpty() || (BluetoothHeadsetClientCall.CALL_STATE_ACTIVE != callInfo.get(0).getState())) {
                    Log.d(TAG, "No Active call");
                    return;
                }
                /*There is atleast one active call*/
                char keyChar = 'x';
                try {
                    keyChar = s.charAt(start);
                    String allowedChars = "0123456789*#";
                    if(allowedChars.indexOf(keyChar) != -1) {
                        Log.d(TAG, "DTMF key code char = " + keyChar);
                        mBluetoothHeadsetClient.sendDTMF(mBluetoothDevice, (byte)keyChar);
                    } else {
                        Log.e(TAG, "Invalid character input");
                    }
                } catch (Exception e) {
                    /*Log if required - TODO*/
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                /*Do nothing or -TODO*/
            }
        });

        /*Set focus on some static item to avoid auto focus on editor*/
        signalStatus.setFocusableInTouchMode(true);
        signalStatus.requestFocus();

        callItems = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                new ArrayList<String>());

        callItems.clear();
        callListView.setAdapter(callItems);

        mVolumeDialog = null;

        /*Request for Proxy Object - HEADSET_CLIENT*/
        mBluetoothAdapter.getProfileProxy(this,mProfileListener,
                BluetoothProfile.HEADSET_CLIENT);

        /*Register WBS_STATE_CHANGED broadcast receiver - TODO*/

    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();

        if((null == mBluetoothAdapter) || (!mBluetoothAdapter.isEnabled())) {
            Toast.makeText(this,
                    "Please enable BT and Pair/Connect an Ag and launch the App",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        if(null != mBluetoothHeadsetClient) {
            updateUi();
        } else {
            /*Check how to updateUi if it comes here or is it reqd? -TODO*/
            mBluetoothAdapter.getProfileProxy(this,mProfileListener,
                    BluetoothProfile.HEADSET_CLIENT);
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        /*TODO -send BIA*/
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        /*TODO - send BIA*/
        super.onResume();
    }

    public void onBackPressed() {
        this.moveTaskToBack(true);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();

        if(isVibrating && (null != vibrator)) {
            vibrator.cancel();
            isVibrating = false;
        }
        /*Release all resources*/
        releaseResources();
        Log.d(TAG, "finish()");
        finish();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop()");
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.hfdevice_atcmd_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.atcmd_menu:
                showHfpClientDialog(mBluetoothDevice, CyHfpClientDeviceConstants.HF_DEVICE_AT_CMD_ENTRY_DIALOG_ID, null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void dialNumber(BluetoothDevice device) {
        dialedNumber = numberEntry.getText().toString();
        if (0 != dialedNumber.length()) {
            if (null == mBluetoothHeadsetClient.dial(device, dialedNumber)) { /*number dialed*/
                Log.e(TAG, "onClick: dialing failed as device got disconnected");
                        /*Dialog shows that device is disconnected*/
                showHfpClientDialog(mCurrentCallingDevice, CyHfpClientDeviceConstants.HF_DEVICE_NOTCONNECTED_DIALOG_ID, null);
            }
            Log.d(TAG, "onClick: dialed number is: " + dialedNumber);
            showNotification(R.drawable.stat_sys_audio_state_off);
            if(mBluetoothDevice.equals(device)) {
                displayState.setText("Dialing..");
            } else {
                if(mDeviceMap.containsKey(device)) {
                    displayState2.setText("Dialing..");
                }
            }
        } else {
            Log.e(TAG, "onClick: dialing failed as number is empty");
        }
    }

    /**
     * This function handles all the button press events
     */
    public void onClick(View v) {
        Log.d(TAG, "onClick()");
        Button b = (Button)v;
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if(null != imm) {
            imm.hideSoftInputFromWindow(numberEntry.getWindowToken(), 0);
        }

        if(b == textButton2) {
            String optionString = textButton2.getText().toString();
            if(optionString.equals(dialStr)) {
                Log.d(TAG, "onClick: on clicked dial button");
                if(mDeviceMap.keySet().size() > 1) {
                    showHfpClientDialog(mCurrentCallingDevice, CyHfpClientDeviceConstants.HF_DEVICE_DEVICE_SELECTION_DIALOG_ID, null);
                } else {
                    dialNumber(mCurrentCallingDevice);
                }
            } else {
                Log.d(TAG, "onClick: on clicked answer button");
                if(!mBluetoothHeadsetClient.acceptCall(mCurrentCallingDevice, 0)) { /*answer call*/ /*TODO -0=CALL_ACCEPT_NONE*/
                    Log.e(TAG, "onClick: answering call failed as device got disconnected");
                    showHfpClientDialog(mCurrentCallingDevice, CyHfpClientDeviceConstants.HF_DEVICE_NOTCONNECTED_DIALOG_ID, null);
                }
            }
        } else if(b == redial) {
            Log.d(TAG, "onClick: on clicked redial button..");
            if(null == mBluetoothHeadsetClient.dial(mCurrentCallingDevice, null)) { /*redial*/
                Log.e(TAG, "onClick: redialing failed as device got disconnected");
                /*Dialog shows that device is disconnected*/
                showHfpClientDialog(mCurrentCallingDevice, CyHfpClientDeviceConstants.HF_DEVICE_NOTCONNECTED_DIALOG_ID, null);
            }
            if(mBluetoothDevice.equals(mCurrentCallingDevice)) {
                displayState.setText("Dialing..");
            } else {
                if(mDeviceMap.containsKey(mCurrentCallingDevice)) {
                    if(null != mDeviceMap.get(mCurrentCallingDevice)) {
                        displayState2.setText("Dialing..");
                    }
                }
            }
            showNotification(R.drawable.stat_sys_audio_state_off);
        } else if(b == textButton) {
            String check = textButton.getText().toString();
            if(check.equals(handsfreeMode)) {
                Log.d(TAG, "onClick: on clicked Handsfree Mode Button");
                if(mBluetoothHeadsetClient.connectAudio(mBluetoothDevice)) {
                    /*Nothing to do*/
                }
            } else {
                Log.d(TAG, "onClick: on clicked Private Mode Button");
                if(mBluetoothHeadsetClient.disconnectAudio(mBluetoothDevice)) {
                    /*Nothing to do*/
                }
            }
        } else if(b == endCall) { /*end call*/
            Log.d(TAG, "onClick: on clicked endcall button ");
            BluetoothHeadsetClientCall callInfo = getClientCall(mCurrentCallingDevice);
            if(null != callInfo) {
                /*If there is a single call and the call is in held state send hold cmd*/
                if ((getNumHeldCall(callInfo.getDevice()) >= 1) && (0 == getNumActiveCall(callInfo.getDevice()))) {
                    mBluetoothHeadsetClient.rejectCall(callInfo.getDevice());
                } else {
                    if((BluetoothHeadsetClientCall.CALL_STATE_DIALING == callInfo.getState()) ||
                        (BluetoothHeadsetClientCall.CALL_STATE_ALERTING == callInfo.getState()) ||
                            (BluetoothHeadsetClientCall.CALL_STATE_ACTIVE == callInfo.getState())) {
                        if(!mBluetoothHeadsetClient.terminateCall(callInfo.getDevice(), callInfo)) {
                            Log.e(TAG, "onClick: terminate call failed as device got disconnected..");
                            /*Dialog shows that device is disconnected*/
                            showHfpClientDialog(mCurrentCallingDevice, CyHfpClientDeviceConstants.HF_DEVICE_NOTCONNECTED_DIALOG_ID, null);
                        }
                    } else {
                        if(!mBluetoothHeadsetClient.rejectCall(callInfo.getDevice())) {
                            Log.e(TAG, "onClick: hanging up failed as device got disconnected..");
                            /*Dialog shows that device is disconnected*/
                            showHfpClientDialog(mCurrentCallingDevice, CyHfpClientDeviceConstants.HF_DEVICE_NOTCONNECTED_DIALOG_ID, null);
                        }
                    }
                }
            } else {
                Log.e(TAG, "onClick: No call info found");
            }
            Log.d(TAG, "onClick: Hanging up the call..");
            if(mBluetoothDevice.equals(mCurrentCallingDevice)) {
                displayState.setText("Hanging up");
            } else {
                if(mDeviceMap.containsKey(mCurrentCallingDevice)) {
                    if(null != mDeviceMap.get(mCurrentCallingDevice)) {
                        displayState2.setText("Hanging up");
                    }
                }
            }
        } else if(b == callControl) { /*call control*/
            Log.d(TAG, "onClick: on clicked callControl button ");
            BluetoothHeadsetClientCall callInfo = getClientCall(mCurrentCallingDevice);
            if(null != callInfo) {
                if(1 < (getNumActiveCall(callInfo.getDevice()) + getNumHeldCall(callInfo.getDevice()))) {
                    showHfpClientDialog(mCurrentCallingDevice, CyHfpClientDeviceConstants.HF_DEVICE_MULTI_CALL_CONTROL_DIALOG_ID, null);
                } else if((getNumHeldCall(callInfo.getDevice()) >= 1) || (getNumActiveCall(callInfo.getDevice()) >= 1)) {
                    String check = callControl.getText().toString();
                    if(check.equals(holdCall)) {
                        Log.d(TAG, "onClick: on clicked hold button");
                        mBluetoothHeadsetClient.acceptCall(mBluetoothDevice, BluetoothHeadsetClient.CALL_ACCEPT_HOLD); /*hold the call*/
                        callControl.setText(unholdCall);
                    } else if(check.equals(unholdCall)) {
                        Log.d(TAG, "onClick: on clicked Unhold button");
                        mBluetoothHeadsetClient.acceptCall(mBluetoothDevice, BluetoothHeadsetClient.CALL_ACCEPT_HOLD); /*Unhold the call*/
                        callControl.setText(holdCall);
                    }
                }
            } else {
                Log.e(TAG, "onClick: No call info found");
            }
        } else if(b == enhancedCallControl) {
            showHfpClientDialog(mCurrentCallingDevice, CyHfpClientDeviceConstants.HF_DEVICE_ENHANCED_CALL_CONTROL_DIALOG_ID, null);
        } else if(b == vrControl) {
            String check = vrControl.getText().toString();
            Log.d(TAG, "onClick: on clicked vrControl button " + check);
            if(check.equals(vrOnStr)) {
                Log.d(TAG, "onClick: on clicked VR ON button");
                mBluetoothHeadsetClient.startVoiceRecognition(mBluetoothDevice);
            } else if(check.equals(vrOffStr)) {
                Log.d(TAG, "onClick: on clicked VR OFF button");
                mBluetoothHeadsetClient.stopVoiceRecognition(mBluetoothDevice);
            } else if(check.equals(sendKeyPress)) {
                Log.d(TAG, "onClick: on clicked sendKeyPress button");
                /*TODO - implement key press event for HSP*/
            }
        } else if(b == swapUiButton) {
            for(Map.Entry<BluetoothDevice, DeviceInfo> entry : mDeviceMap.entrySet()) {
                if((null != entry.getValue()) && (!entry.getKey().equals(mBluetoothDevice))) {
                    swapUi(entry.getKey());
                    return;
                }
            }
            Toast.makeText(this, "No device to swap", Toast.LENGTH_LONG).show();
        }
    }

    private void updateUi() {
        Log.d(TAG, "updateUi()");
        if(0 == mBluetoothHeadsetClient.getConnectedDevices().size()) {
            /*No connected device available - exit app*/
            Toast.makeText(this,
                    "Please enable BT and Pair/Connect an Ag and launch the App",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        } else if(mBluetoothHeadsetClient.getConnectionState(mBluetoothDevice) !=
                BluetoothHeadsetClient.STATE_CONNECTING) {
            Log.d(TAG, "updateUi: conn st = " + mBluetoothHeadsetClient.getConnectionState(mBluetoothDevice));
            mBluetoothDevice = mBluetoothHeadsetClient.getConnectedDevices().get(DEVICE_INDEX_0);
            mCurrentCallingDevice = mBluetoothDevice;
            mDeviceName = mBluetoothDevice.getName();
            mDeviceAddress = mBluetoothDevice.getAddress();
            /*Create Device Info*/
            DeviceInfo di = getDeviceInfo(mBluetoothDevice);
            if (null == di) {
                Log.e(TAG, "Cannnot allocate DI ");
            } else {
                di.updateDeviceInfo(mBluetoothHeadsetClient.getCurrentAgEvents(mBluetoothDevice));
            }
            displayState.setText("Connected to " + mDeviceName);
            if(mBluetoothHeadsetClient.getConnectedDevices().size() > 1) {
                BluetoothDevice device2 = mBluetoothHeadsetClient.getConnectedDevices().get(DEVICE_INDEX_1);
                /*Create Device Info*/
                DeviceInfo di_device2 = getDeviceInfo(device2);
                if (null == di_device2) {
                    Log.e(TAG, "Cannnot allocate DI for 2nd device");
                } else {
                    di_device2.updateDeviceInfo(mBluetoothHeadsetClient.getCurrentAgEvents(device2));
                }
                displayState2.setText("Connected to " + device2.getName());
            } else {
                displayState2.setText("Not connected");
            }

            Log.d(TAG, "updateUi: device already connected..");
            Log.d(TAG, "Handler.handleMessage: connected to a device named " +
            mBluetoothDevice.getName() + " address: " +
            mDeviceAddress + " mBluetoothDevice.getAddress(): " +
            mBluetoothDevice.getAddress());

            Bundle b = new Bundle();
            b.putParcelable(BluetoothDevice.EXTRA_DEVICE, mBluetoothDevice);
            b.putParcelable(EXTRA_INDICATOR_INFO, mBluetoothHeadsetClient.getCurrentAgEvents(mBluetoothDevice));
            updateIndicators(b);
            Log.d(TAG, "updateUi: updated indicators..");
            /*Edit the shared preference as it is connected*/
            Log.d(TAG, "updateUi: connected to a device named " + mBluetoothDevice.getName());
            editor = pref.edit();
            editor.putBoolean(CyHfpClientDeviceConstants.HF_DEVICE_CONNECTED, true);
            editor.putString(CyHfpClientDeviceConstants.HF_DEVICE_NAME, mDeviceName);
            editor.putString(CyHfpClientDeviceConstants.HF_DEVICE_ADDRESS, mDeviceAddress);
            editor.apply();

            Message msg = Message.obtain();
            isHSPConnection = isHSPConnection();

            if(!isHSPConnection) {
                msg.what = GUI_UPDATE_CALL_STATUS;
                Bundle b1 = new Bundle();
                b1.putParcelable(BluetoothDevice.EXTRA_DEVICE, mBluetoothDevice);
                b1.putParcelable(BluetoothHeadsetClient.EXTRA_CALL, getClientCall(mBluetoothDevice));
                msg.obj = b1;
                viewUpdateHandler.sendMessage(msg);

                Log.d(TAG, "updateUi: call state is: " + getCallSetupState(mBluetoothDevice)); /*Multi-device - TODO*/

                /*Check if it is reqd to send AT+CLCC here or already the result is available in getCurrentCalls()- TODO*/

                /*Operator name - Check if operator name is to be fetched from peer explicitly now - TODO*/
                String operatorName = mBluetoothHeadsetClient.getCurrentAgEvents(mBluetoothDevice).getString(BluetoothHeadsetClient.EXTRA_OPERATOR_NAME, "N/A");

                /*Subscriber info - Check if subscriber info is to be fetched from the peer explicitly now - TODO*/
                String subscriberInfo = mBluetoothHeadsetClient.getCurrentAgEvents(mBluetoothDevice).getString(BluetoothHeadsetClient.EXTRA_SUBSCRIBER_INFO, "N/A");

                updateViewVrState(mVrState);

                Message msg1 = Message.obtain();
                msg1.what = GUI_UPDATE_WBS_STATE;
                msg1.arg1 = pref.getInt(CyHfpClientDeviceConstants.HF_DEVICE_WBS_STATE, 0);
                viewUpdateHandler.sendMessage(msg1);

                Message msg2 = Message.obtain();
                msg2.what = GUI_UPDATE_IN_BAND_STATUS;
                msg2.arg1 = 0; /*INBAND_STATE_OFF - implementation TODO*/
                viewUpdateHandler.sendMessage(msg2);
            } else {
                vrControl.setText(sendKeyPress);
                vrControl.setVisibility(View.VISIBLE);
            }

            updateViewAudioState(mBluetoothDevice, mBluetoothHeadsetClient.getAudioState(mBluetoothDevice));
        }
    }

    /**
     * This function is used to swap currently showing device info with second device
     * on Ui
     */
    private void swapUi(BluetoothDevice device) {
        Log.d(TAG, "swapUi()");
        if (0 == mBluetoothHeadsetClient.getConnectedDevices().size()) {
            /*No connected device available - exit app*/
            Toast.makeText(this,
                    "Please enable BT and Pair/Connect an Ag and launch the App",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        } else {
            if (mBluetoothHeadsetClient.getConnectionState(device) !=
                    BluetoothHeadsetClient.STATE_CONNECTING) {
                displayState2.setText(displayState.getText());
                displayNumber2.setText(displayNumber.getText());
                mBluetoothDevice = device;
                mCurrentCallingDevice = mBluetoothDevice;
                mDeviceName = mBluetoothDevice.getName();
                mDeviceAddress = mBluetoothDevice.getAddress();
                displayState.setText("Connected to " + mDeviceName);

                Log.d(TAG, "swapUi: device already connected..");
                Log.d(TAG, "Handler.handleMessage: connected to a device named " +
                        mBluetoothDevice.getName() + " address: " +
                        mDeviceAddress + " mBluetoothDevice.getAddress(): " +
                        mBluetoothDevice.getAddress());
                Bundle b = new Bundle();
                b.putParcelable(BluetoothDevice.EXTRA_DEVICE, mBluetoothDevice);
                b.putParcelable(EXTRA_INDICATOR_INFO, mBluetoothHeadsetClient.getCurrentAgEvents(mBluetoothDevice));
                updateIndicators(b);
                Log.d(TAG, "updateUi: updated indicators..");
                /*Edit the shared preference as it is connected*/
                Log.d(TAG, "swapUi: connected to a device named " + mBluetoothDevice.getName());
                editor = pref.edit();
                editor.putBoolean(CyHfpClientDeviceConstants.HF_DEVICE_CONNECTED, true);
                editor.putString(CyHfpClientDeviceConstants.HF_DEVICE_NAME, mDeviceName);
                editor.putString(CyHfpClientDeviceConstants.HF_DEVICE_ADDRESS, mDeviceAddress);
                editor.apply();

                Message msg = Message.obtain();
                isHSPConnection = isHSPConnection();

                if (!isHSPConnection) {
                    msg.what = GUI_UPDATE_CALL_STATUS;
                    Bundle b1 = new Bundle();
                    b1.putParcelable(BluetoothDevice.EXTRA_DEVICE, mBluetoothDevice);
                    b1.putParcelable(BluetoothHeadsetClient.EXTRA_CALL, getClientCall(mBluetoothDevice));
                    msg.obj = b1;
                    viewUpdateHandler.sendMessage(msg);

                    Log.d(TAG, "updateUi: call state is: " + getCallSetupState(mBluetoothDevice)); /*Multi-device - TODO*/

                    /*Check if it is reqd to send AT+CLCC here or already the result is available in getCurrentCalls()- TODO*/

                    /*Operator name - Check if operator name is to be fetched from peer explicitly now - TODO*/
                    String operatorName = mBluetoothHeadsetClient.getCurrentAgEvents(mBluetoothDevice).getString(BluetoothHeadsetClient.EXTRA_OPERATOR_NAME, "N/A");

                    /*Subscriber info - Check if subscriber info is to be fetched from the peer explicitly now - TODO*/
                    String subscriberInfo = mBluetoothHeadsetClient.getCurrentAgEvents(mBluetoothDevice).getString(BluetoothHeadsetClient.EXTRA_SUBSCRIBER_INFO, "N/A");

                    updateViewVrState(mVrState);

                    Message msg1 = Message.obtain();
                    msg1.what = GUI_UPDATE_WBS_STATE;
                    if (mDeviceMap.containsKey(device)) {
                        msg1.arg1 = mDeviceMap.get(device).mWbsState;
                    } else {
                        msg1.arg1 = WBS_NONE;
                    }
                    viewUpdateHandler.sendMessage(msg1);

                    Message msg2 = Message.obtain();
                    msg2.what = GUI_UPDATE_IN_BAND_STATUS;
                    msg2.arg1 = 0; /*INBAND_STATE_OFF - implementation TODO*/
                    viewUpdateHandler.sendMessage(msg2);
                } else {
                    vrControl.setText(sendKeyPress);
                    vrControl.setVisibility(View.VISIBLE);
                }

                updateViewAudioState(device, mBluetoothHeadsetClient.getAudioState(device));
            }
        }
    }

    /**
     *
     * This function is used to update the indicators
     */
    private void updateIndicators(Bundle bundle) {
        BluetoothDevice device = bundle.getParcelable(BluetoothDevice.EXTRA_DEVICE);
        Bundle b = bundle.getParcelable(EXTRA_INDICATOR_INFO);
        if(null != b) {
            int service = b.getInt(BluetoothHeadsetClient.EXTRA_NETWORK_STATUS, NETWORK_UNAVAILABLE);
            int roam = b.getInt(BluetoothHeadsetClient.EXTRA_NETWORK_ROAMING, NO_ROAMING);
            int signalStrength = b.getInt(BluetoothHeadsetClient.EXTRA_NETWORK_SIGNAL_STRENGTH, 0);
            int batteryCharge = b.getInt(BluetoothHeadsetClient.EXTRA_BATTERY_LEVEL, 0);

            Log.d(TAG, "Service = " + service + ", Roam = " + roam + ", Signal = " + signalStrength + ", Batt chgv = " + batteryCharge);

            if (mDeviceMap.containsKey(device)) {
                if (NETWORK_UNAVAILABLE != service) {
                    if (ACTIVE_ROAMING == roam) {
                        mDeviceMap.get(device).isRoaming = true;
                    } else {
                        mDeviceMap.get(device).isRoaming = false;
                    }

                    mDeviceMap.get(device).currentSignalStrength = signalStrength;
                    if (!mDeviceMap.get(device).isRoaming) {
                        // The image offset param indicates image index
                        // referred from stat_signal.xml
                        // "0" offset sets the non-roaming images(1-5 index)
                        if (mBluetoothDevice.equals(device)) {
                            updateSignalStatus(mDeviceMap.get(device).currentSignalStrength, 0);
                        }
                    } else {
                        // The image offset param indicates image index
                        // referred from stat_signal.xml
                        // "5" offset sets the roaming images(6-10 index)
                        if (mBluetoothDevice.equals(device)) {
                            updateSignalStatus(mDeviceMap.get(device).currentSignalStrength, 5);
                        }
                    }
                } else {
                    Log.d(TAG, "updateIndicators: no active service..");
                    // "-1" offset sets the no signal image index (0 index) and signal as zero as no service
                    if (mBluetoothDevice.equals(device)) {
                        updateSignalStatus(0, -1);
                    }
                }

                if (mDeviceMap.get(device).currentBatteryCharge != batteryCharge) {
                    mDeviceMap.get(device).currentBatteryCharge = batteryCharge;
                    if(mBluetoothDevice.equals(device)) {
                        updateBatteryStatus(mDeviceMap.get(device).currentBatteryCharge);
                    }
                }
            }
        } else {
            Log.d(TAG, "updateIndicators: no active service..");
            // "-1" offset sets the no signal image index (0 index) and signal as zero as no service
            updateSignalStatus(0, -1);
        }

        if(currentBatteryCharge != batteryCharge) {
            currentBatteryCharge = batteryCharge;
            updateBatteryStatus(currentBatteryCharge);
        }
    }

    /**
     *
     * This function is used to update the battery status
     */
    private void updateBatteryStatus(int status) {
        Log.d(TAG, "updateBatteryStatus: Updating battery status");
        switch(status) {
            case 0:
                batteryStatus.setImageLevel(0);
                break;
            case 1:
                batteryStatus.setImageLevel(1);
                break;
            case 2:
                batteryStatus.setImageLevel(2);
                break;
            case 3:
                batteryStatus.setImageLevel(3);
                break;
            case 4:
                batteryStatus.setImageLevel(4);
                break;
            case 5:
                batteryStatus.setImageLevel(5);
                break;
            default:
                batteryStatus.setImageLevel(0);
                break;
        }
    }

    /**
     *
     * This function is used to update the signal status
     */
    private void updateSignalStatus(int status, int imageOffset) {
        Log.d(TAG, "updateSignalStatus: Updating signal status");
        switch(status) {
            case 0:
                signalStatus.setImageLevel(1 + imageOffset);
                break;
            case 1:
                signalStatus.setImageLevel(2 + imageOffset);
                break;
            case 2:
                signalStatus.setImageLevel(3 + imageOffset);
                break;
            case 3:
                signalStatus.setImageLevel(4 + imageOffset);
                break;
            case 4:
                signalStatus.setImageLevel(4 + imageOffset); /*is it correct? -TODO*/
                break;
            case 5:
                signalStatus.setImageLevel(5 + imageOffset);
                break;
            default:
                signalStatus.setImageLevel(1+imageOffset);
                break;
        }
    }

    /**
     *
     * This function updates the view with VR state
     */
    private void updateViewVrState(int vrState) {
        switch(vrState) {
            case VR_STOPPED:
                vrControl.setText(vrOffStr);
                break;
            case VR_STARTED:
                vrControl.setText(vrOnStr);
                break;
            default:
                break;
        }

        mVrState = vrState;
        if((isCallSetupInProgress(getClientCall(mBluetoothDevice))) || (0 != (getNumActiveCall(mBluetoothDevice)+getNumHeldCall(mBluetoothDevice)))) {
            vrControl.setVisibility(View.INVISIBLE);
        } else {
            vrControl.setVisibility(View.VISIBLE);
        }
    }

    private void updateViewOnCallWaiting(BluetoothDevice device, String number) {
        long[] timings = {200, 500};
        vibrator.vibrate(VibrationEffect.createWaveform(timings, 0));
        isVibrating = true;
        if((null != device) && (mDeviceMap.containsKey(device))) {
            mDeviceMap.get(device).waitingCallNumber = number;

            /*Now we need to show an AlertDialog with three options - Reject, Accept waiting release active, Accept waiting hold active*/
            showHfpClientDialog(device, CyHfpClientDeviceConstants.HF_DEVICE_CALL_WAITING_DIALOG_ID,
                    mDeviceMap.get(device).waitingCallNumber);
        }
    }

    private void updateViewOnIncoming() {
        long[] timings = {200, 500};
        vibrator.vibrate(VibrationEffect.createWaveform(timings, 0));
        isVibrating = true;

        textButton2.setText(answerStr);
        textButton2.setVisibility(View.VISIBLE);

        redial.setVisibility(View.INVISIBLE);

        numberEntry.setVisibility(View.INVISIBLE);
        numberEntryText.setVisibility(View.INVISIBLE);

        endCall.setText(rejectStr);
        endCall.setVisibility(View.VISIBLE);

        callControl.setText("");
        callControl.setVisibility(View.INVISIBLE);

        textButton.setText("");
        textButton.setVisibility(View.INVISIBLE);

    }

    private void updateViewOnPhoneHook(BluetoothDevice device) {
        BluetoothDevice otherDevice = null;
        boolean isOtherDeviceOnCall = true;
        for(Map.Entry<BluetoothDevice, DeviceInfo> entry : mDeviceMap.entrySet()) {
            if((null != entry.getValue()) && (!entry.getKey().equals(device))) {
                otherDevice = entry.getKey();
                break;
            }
        }
        int numActive = getNumActiveCall(otherDevice);
        int numHeld = getNumHeldCall(otherDevice);
        int callSetup = getCallSetupState(otherDevice);
        if((0 == numActive) && (0 == numHeld) && ((Integer.MAX_VALUE == callSetup) ||
                (BluetoothHeadsetClientCall.CALL_STATE_TERMINATED == callSetup))) {
            isOtherDeviceOnCall = false;
        }
        if(mBluetoothDevice.equals(device)) {
            textButton2.setText(dialStr);
            textButton2.setVisibility(View.VISIBLE);

            redial.setVisibility(View.VISIBLE);

            numberEntry.setVisibility(View.VISIBLE);
            numberEntryText.setVisibility(View.VISIBLE);

            //endCall.setText("");
            //endCall.setVisibility(View.INVISIBLE);

            //callControl.setText("");
            //callControl.setVisibility(View.INVISIBLE);

            //enhancedCallControl.setText("");
            //enhancedCallControl.setVisibility(View.INVISIBLE);

            displayState.setText("Connected to " + mDeviceName);
            displayNumber.setText("");
        } else {
            if(mDeviceMap.containsKey(device)) {
                textButton2.setText(dialStr);
                textButton2.setVisibility(View.VISIBLE);

                redial.setVisibility(View.VISIBLE);

                numberEntry.setVisibility(View.VISIBLE);
                numberEntryText.setVisibility(View.VISIBLE);

                displayState2.setText("Connected to " + mDeviceMap.get(device).mDeviceName);
                displayNumber2.setText("");
            }
        }

        if(!isOtherDeviceOnCall) {
            endCall.setText("");
            endCall.setVisibility(View.INVISIBLE);

            callControl.setText("");
            callControl.setVisibility(View.INVISIBLE);

            enhancedCallControl.setText("");
            enhancedCallControl.setVisibility(View.INVISIBLE);
        }

        if(mBluetoothDevice.equals(device)) {
            updateViewAudioState(device, mBluetoothHeadsetClient.getAudioState(device));
        }
        if(mDeviceMap.containsKey(device)) {
            mDeviceMap.get(device).resetCallStateVariable();
        }
    }

    private void updateViewAudioState(BluetoothDevice device, int audioState) {
        int combinedAudioState = audioState;
        for(Map.Entry<BluetoothDevice, DeviceInfo> entry : mDeviceMap.entrySet()) {
            if((null != entry.getValue()) && (!entry.getKey().equals(device))) {
                if((mBluetoothHeadsetClient.getAudioState(entry.getKey()) != audioState) &&
                        (BluetoothHeadsetClient.STATE_AUDIO_DISCONNECTED == audioState)) {
                    combinedAudioState = mBluetoothHeadsetClient.getAudioState(entry.getKey());
                }
            }
        }
        switch(combinedAudioState) {
            case BluetoothHeadsetClient.STATE_AUDIO_CONNECTED:
                textButton.setText(privacyMode);
                showNotification(R.drawable.stat_sys_audio_state_on);
                break;
            case BluetoothHeadsetClient.STATE_AUDIO_DISCONNECTED:
                textButton.setText(handsfreeMode);
                showNotification(R.drawable.stat_sys_audio_state_off);
                break;
            default:
                break;
        }

        BluetoothHeadsetClientCall call = getClientCall(device);
        if(null != call) {
            if (BluetoothHeadsetClientCall.CALL_STATE_TERMINATED == call.getState()) {
                boolean nonTerminatedCallFound = false;
                for(Map.Entry<BluetoothDevice, DeviceInfo> entry : mDeviceMap.entrySet()) {
                    if((null != entry.getValue()) && (!entry.getKey().equals(device))) {
                        BluetoothHeadsetClientCall otherDeviceCall = getClientCall(entry.getKey());
                        if((null != otherDeviceCall) &&
                                (BluetoothHeadsetClientCall.CALL_STATE_TERMINATED != otherDeviceCall.getState())) {
                            nonTerminatedCallFound = true;
                        }
                    }
                }
                if(!nonTerminatedCallFound) {
                    textButton.setVisibility(View.VISIBLE);
                }
            }
        } else {
            Log.d(TAG, "updateViewAudioState: getClientCall returned null");
        }
    }

    private void updateViewWbsState(int wbsState) {
        switch (wbsState) {
            case WBS_NONE:
                wbsStatus.setText(wbsOffStr);
                break;
            case WBS_YES:
                wbsStatus.setText(wbsOnStr);
                break;
            default:
                break;
        }

        wbsStatus.setVisibility(View.VISIBLE);

        pref = PreferenceManager.getDefaultSharedPreferences(HfpClientMainActivity.this);
        editor = pref.edit().putInt(CyHfpClientDeviceConstants.HF_DEVICE_WBS_STATE, wbsState);
        editor.apply();
    }

    private void updateViewInBandState(int inBandRingStatus) {
        switch(inBandRingStatus) {
            case INBAND_STATE_OFF:
                inBandStatus.setText(inBandOffStr);
                break;
            case INBAND_STATE_ON:
                inBandStatus.setText(inBandOnStr);
                break;
            default:
                break;
        }

        inBandStatus.setVisibility(View.VISIBLE);
        String toastString = "";
        if(0 == inBandRingStatus) {
            toastString = "InBand: " + "Disabled";
        } else {
            toastString = "InBand: " + "Enabled";
        }

        Toast.makeText(this, toastString, Toast.LENGTH_LONG).show();
    }

    private void updateViewOnActiveCall(BluetoothHeadsetClientCall callInfo) {
        int numActive;
        int numHeld;
        int callSetup;
        if (null != callInfo) {
            numActive = getNumActiveCall(callInfo.getDevice());
            numHeld = getNumHeldCall(callInfo.getDevice());
            callSetup = callInfo.getState();
            BluetoothDevice device = callInfo.getDevice();

            if (mBluetoothDevice.equals(device)) {
                displayState.setText("Call Active ");
            } else {
                if (mDeviceMap.containsKey(device)) {
                    displayState2.setText("Call Active");
                }
            }

            /*Case when other device has incoming call*/
            boolean otherDeviceHasIncoming = false;
            for (Map.Entry<BluetoothDevice, DeviceInfo> entry : mDeviceMap.entrySet()) {
                if ((null != entry.getValue()) && (!entry.getKey().equals(device))) {
                    if (BluetoothHeadsetClientCall.CALL_STATE_INCOMING == entry.getValue().callState) {
                        otherDeviceHasIncoming = true;
                        break;
                    }
                }
            }
            if (!otherDeviceHasIncoming) {

                textButton2.setText(dialStr);
                textButton2.setVisibility(View.VISIBLE);

                redial.setVisibility(View.VISIBLE);

                numberEntry.setVisibility(View.VISIBLE);
                numberEntryText.setVisibility(View.VISIBLE);

                endCall.setText(endCallStr);
                endCall.setVisibility(View.VISIBLE);

                callControl.setVisibility(View.VISIBLE);

        /*When there is only one active call*/
                if ((0 != numActive) && (0 == numHeld)) {
                    callControl.setText(holdCall);
                }

        /*When there is only held call*/
                if ((0 == numActive) && (0 != numHeld)) {
                    callControl.setText(unholdCall);
                    if (mBluetoothDevice.equals(device)) {
                        displayState.setText("Held call");
                    } else {
                        if (mDeviceMap.containsKey(device)) {
                            displayState2.setText("Held call");
                        }
                    }
                }

        /*When there is more than one call*/
                if ((0 != numActive) && (0 != numHeld)) {
                    callControl.setText(multiCallControl);
                }
            } else {
                Log.d(TAG, "updateViewOnActiveCall: No active call or other device has incoming");
            }
        } else {
            Log.d(TAG, "updateViewOnActiveCall: No active call");
        }
    }

    /**
     *
     * This function updates the view when call is in progress
     */
    private void updateViewOnCallProgress(BluetoothDevice device, BluetoothHeadsetClientCall callInfo) {
        int numActive;
        int numHeld;
        int callSetup;
        String number;
        if(null != callInfo) {
            numActive = getNumActiveCall(callInfo.getDevice());
            numHeld = getNumHeldCall(callInfo.getDevice());
            callSetup = callInfo.getState();
            number = callInfo.getNumber();
        } else {
            numActive = 0;
            numHeld = 0;
            callSetup = BluetoothHeadsetClientCall.CALL_STATE_TERMINATED;
            number = "";
        }
        String callStatus = "";

        if(!pm.isInteractive()) {
            Log.d(TAG, "updateViewOnCallProgress: Wake screen up as interactive state is " + pm.isInteractive());

            /*Instead of deprecated wakelocks use the following for API level 26 and 27*/
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            setTurnScreenOn(true); /*This should turn on the screen*/
            setShowWhenLocked(true); /*This should put the activity on top of lock screen and bypass keyguard temporarily*/
        }

        /*We do not need KeyguardLock to unlock automatically anymore
        setShowWhenLocked(true) above will take care of it- API level>=26*/

        switch(callSetup) {
            case BluetoothHeadsetClientCall.CALL_STATE_DIALING:
                callStatus = "Dialing..";
                if(mBluetoothDevice.equals(device)) {
                    displayNumber.setText(dialedNumber);
                    displayState.setText(callStatus);
                } else {
                    if((null != device) && (mDeviceMap.containsKey(device))) {
                        displayNumber2.setText(dialedNumber);
                        displayState2.setText(callStatus);
                    }
                }
                endCall.setText(endCallStr);
                endCall.setVisibility(View.VISIBLE);
                break;
            case BluetoothHeadsetClientCall.CALL_STATE_WAITING:
                if((null != device) && (mDeviceMap.containsKey(device))) {
                    if (BluetoothHeadsetClientCall.CALL_STATE_WAITING == mDeviceMap.get(device).callState) {
                        return; /*Already in waiting state*/
                    }
                }
                Log.d(TAG, "CALL_STATE_WAITING");
                updateViewOnCallWaiting(device, number);
                break;
            case BluetoothHeadsetClientCall.CALL_STATE_ALERTING:
                callStatus = "Alerting..";
                if(mBluetoothDevice.equals(device)) {
                    displayState.setText(callStatus);
                } else {
                    if((null != device) && (mDeviceMap.containsKey(device))) {
                        displayState2.setText(callStatus);
                    }
                }
                endCall.setText(endCallStr);
                endCall.setVisibility(View.VISIBLE);
                break;
            case BluetoothHeadsetClientCall.CALL_STATE_INCOMING:
                if(null == number) {
                    updateViewOnIncoming();
                } else {
                    if(mBluetoothDevice.equals(device)) {
                        displayNumber.setText(number);
                        mDeviceMap.get(device).incomingCallNumber = number;
                    } else {
                        if((null != device) && (mDeviceMap.containsKey(device))) {
                            displayNumber2.setText(number);
                            mDeviceMap.get(device).incomingCallNumber = number;
                        }
                    }
                    incomingCallNumber = number;
                    updateViewOnIncoming();
                }
                callStatus = "Incoming";
                if(mBluetoothDevice.equals(device)) {
                    displayState.setText(callStatus);
                } else {
                    if((null != device) && (mDeviceMap.containsKey(device))) {
                        displayState2.setText(callStatus);
                    }
                }
                break;
            default:
                break;
        }
    }

    private void resetCallStateVariable() {
        incomingCallNumber = null;
        waitingCallNumber = null;
        dialedNumber = null;
    }

    /**
     *
     * This function is called when the activity gets destroyed
     */
    private void releaseResources() {
        if(null != mBluetoothHeadsetClient) {
            mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET_CLIENT, mBluetoothHeadsetClient);
            mBluetoothHeadsetClient = null;
            Log.d(TAG, "onStop: un-registered proxy");
        }
    }

    private boolean isPhoneOnHook(BluetoothHeadsetClientCall callInfo) {
        if(null == callInfo) {
            if(0 != mBluetoothHeadsetClient.getConnectedDevices().size()) {
                return true;
            } else {
                Toast.makeText(this,
                        "Please enable BT and Pair/Connect an Ag and launch the App",
                        Toast.LENGTH_LONG).show();
                finish();
                return false;
            }
        } else {
            return ((0 == getNumActiveCall(callInfo.getDevice())) &&
                    (0 == getNumHeldCall(callInfo.getDevice())) &&
                    (BluetoothHeadsetClientCall.CALL_STATE_TERMINATED == callInfo.getState()));
        }
    }

    private boolean isCallSetupInProgress(BluetoothHeadsetClientCall callInfo) {
        return((null != callInfo) && (BluetoothHeadsetClientCall.CALL_STATE_TERMINATED != callInfo.getState()) &&
                ((BluetoothHeadsetClientCall.CALL_STATE_DIALING == callInfo.getState()) ||
                        (BluetoothHeadsetClientCall.CALL_STATE_ALERTING == callInfo.getState()) ||
                        (BluetoothHeadsetClientCall.CALL_STATE_INCOMING == callInfo.getState()) ||
                        (BluetoothHeadsetClientCall.CALL_STATE_WAITING == callInfo.getState()))); /*Is it equivalent to IDLE? -TODO*/
    }

    private boolean isHSPConnection() {
        if((null != mBluetoothHeadsetClient) && (null != mBluetoothDevice)) {
            Bundle AgFeatures = mBluetoothHeadsetClient.getCurrentAgFeatures(mBluetoothDevice);
            boolean feature3WayCalling = AgFeatures.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_3WAY_CALLING, false);
            boolean featureRejectCall = AgFeatures.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_REJECT_CALL, false);
            boolean featureEcc = AgFeatures.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_ECC, false);
            boolean featureChldHoldAcc = AgFeatures.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_ACCEPT_HELD_OR_WAITING_CALL, false);
            boolean featureChldRel = AgFeatures.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_RELEASE_HELD_OR_WAITING_CALL, false);
            boolean featureChldRelAcc = AgFeatures.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_RELEASE_AND_ACCEPT, false);
            boolean featureChldMerge = AgFeatures.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_MERGE, false);
            boolean featureChldMergeDetach = AgFeatures.getBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_MERGE_AND_DETACH, false);

            Log.d(TAG, "3WAY = " + feature3WayCalling +
            " Reject call = " + featureRejectCall +
            " Ecc = " + featureEcc +
            " ChldHoldAcc = " + featureChldHoldAcc +
            " ChldRel = " + featureChldRel +
            " ChldRelAcc = " + featureChldRelAcc +
            " ChldMerge = " + featureChldMerge +
            " ChldMergeDetach = " + featureChldMergeDetach);

            return (!(feature3WayCalling || featureRejectCall || featureEcc || featureChldHoldAcc ||
                    featureChldRel || featureChldRelAcc || featureChldMerge || featureChldMergeDetach));
        } else {
            Log.d(TAG, "Cannot get Ag features : Either there is no device connected or headsetclient class in not yet initialized");
            return false;
        }
    }

    private int getCallSetupState(BluetoothDevice device) {
        List<BluetoothHeadsetClientCall> calls = mBluetoothHeadsetClient.getCurrentCalls(device);
        if(null != calls) {
            Log.d(TAG, "getCallSetupState: got calls.");
            for (BluetoothHeadsetClientCall call : calls) {
                Log.d(TAG, "getCallSetupState: call's device = " + call.getDevice() + " expected device = " + device);
                if (device.getAddress().equals(call.getDevice().getAddress())) {
                    return call.getState();
                }
            }
        } else {
            Log.d(TAG, "getCallSetupState: No calls found.");
        }
        Log.d(TAG, "getCallSetupState: Either no calls or no matching call");
        return Integer.MAX_VALUE;
    }

    /**
     * This function returns current call
     */
    private BluetoothHeadsetClientCall getClientCall(BluetoothDevice device) {
        if((null !=mBluetoothHeadsetClient) && (null != device)) {
            List<BluetoothHeadsetClientCall> calls = mBluetoothHeadsetClient.getCurrentCalls(device);
            if(null != calls) {
                Log.d(TAG, "getClientCalls: got calls");

                for(BluetoothHeadsetClientCall call:calls) {
                    Log.d(TAG, "getClientCall: call's device = " + call.getDevice() + " expected device = " + device);
                    if (device.getAddress().equals(call.getDevice().getAddress())) {
                        return call;
                    }
                }
                Log.d(TAG, "No matching device found in the calls. Returning null");
                return null;
            } else {
                Log.d(TAG, "getClientCalls: No calls");
                return null;
            }
        } else {
            Log.e(TAG, "getClientCall: HeadsetClient or BluetoothDevice is null");
            return null;
        }
    }

    /**
     * This function returns the number of active calls
     */
    private int getNumActiveCall(BluetoothDevice device) {
        int numActiveCalls = 0;
        if(null !=mBluetoothHeadsetClient) {
            List<BluetoothHeadsetClientCall> calls = mBluetoothHeadsetClient.getCurrentCalls(device);
            if(null != calls) {
                for (BluetoothHeadsetClientCall call : calls) {
                    if (BluetoothHeadsetClientCall.CALL_STATE_ACTIVE == call.getState()) {
                        numActiveCalls++;
                    }
                }
            }
        } else {
            Log.e(TAG, "getNumActiveCall: HeadsetClient or BluetoothDevice is null");
        }
        return numActiveCalls;
    }

    /**
     * This function returns the number of held calls
     */
    private int getNumHeldCall(BluetoothDevice device) {
        int numHeldCalls = 0;
        if(null !=mBluetoothHeadsetClient) {
            List<BluetoothHeadsetClientCall> calls = mBluetoothHeadsetClient.getCurrentCalls(device);
            if(null != calls) {
                for (BluetoothHeadsetClientCall call : calls) {
                    if (BluetoothHeadsetClientCall.CALL_STATE_HELD == call.getState()) {
                        numHeldCalls++;
                    }
                }
            }
        } else {
            Log.e(TAG, "getNumHeldCall: HeadsetClient or BluetoothDevice is null");
        }
        return numHeldCalls;
    }

    /**
     * This function is called to send notification to NotificationManager
     */
    private void showNotification(int icon) {
        Log.d(TAG, "showNotification: notification sent.." + mDeviceAddress);

        Intent notificationIntent = new Intent(this, HfpClientMainActivity.class);
        notificationIntent.putExtra(CyHfpClientDeviceConstants.HF_DEVICE_NAME, mDeviceName);
        notificationIntent.putExtra(CyHfpClientDeviceConstants.HF_DEVICE_ADDRESS, mDeviceAddress);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification.Builder notification = new Notification.Builder(this,
                String.valueOf(CyHfpClientDeviceConstants.HF_NOTIFICATION_ID))
                .setSmallIcon(icon)
                .setContentTitle("Connected to " + mDeviceName)
                .setContentText("Device address " + mDeviceAddress)
                .setContentIntent(contentIntent)
                .setAutoCancel(false);

        /*Create channel and set the importance*/
        CharSequence name = "HF Notification";
        String description = "Used to send HF notifications";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(
                String.valueOf(CyHfpClientDeviceConstants.HF_NOTIFICATION_ID),
                name, importance);
        channel.setDescription(description);
        /*Register the channel with the system*/
        mNotificationManager.createNotificationChannel(channel);

        /*Show the notification*/
        mNotificationManager.notify(CyHfpClientDeviceConstants.HF_NOTIFICATION_ID, notification.build());
    }

    /**
     * Handler handles all the GUI events
     */
    private static class hfpClientActivityMsgHandler extends Handler {
        private final WeakReference<HfpClientMainActivity> mActivity;

        public hfpClientActivityMsgHandler(HfpClientMainActivity activity) {
            mActivity = new WeakReference<HfpClientMainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            HfpClientMainActivity activity = mActivity.get();
            if(activity != null) {
                Log.d(TAG, "handleMessage()");
                SharedPreferences.Editor editor;
                BluetoothDevice device;
                Bundle b;
                switch(msg.what) {
                    case GUI_UPDATE_DEVICE_STATUS:
                    {
                        Log.d(TAG, "Handler.handleMessage: updating device status");
                        if((null != msg.obj) && (null != activity.mBluetoothDevice)) {
                            switch(msg.arg1) {
                                case BluetoothHeadsetClient.STATE_CONNECTING: /*update device state to connecting*/
                                    if(activity.mBluetoothDevice.equals(msg.obj)) {
                                        activity.displayState.setText("Connecting to " + activity.mDeviceName);
                                    } else {
                                        activity.displayState2.setText("Connecting to " + ((BluetoothDevice)msg.obj).getName());
                                        Toast.makeText(activity.getBaseContext(),
                                                "Connecting to " + ((BluetoothDevice)msg.obj).getName(), Toast.LENGTH_LONG).show();
                                    }
                                    break;
                                case BluetoothHeadsetClient.STATE_CONNECTED:
                                    if(activity.mBluetoothDevice.equals(msg.obj)) {
                                        /*Edit the shared preference as it is connected*/
                                        Log.d(TAG, "Handler.handleMessage: connected to a device named " +
                                                activity.mBluetoothDevice.getName() + " address: " + activity.mDeviceAddress +
                                                " mBluetoothDevice.getAddress(): " + activity.mBluetoothDevice.getAddress());
                                        editor = activity.pref.edit();
                                        editor.putBoolean(CyHfpClientDeviceConstants.HF_DEVICE_CONNECTED, true);
                                        editor.putString(CyHfpClientDeviceConstants.HF_DEVICE_ADDRESS, activity.mDeviceAddress);
                                        editor.putString(CyHfpClientDeviceConstants.HF_DEVICE_NAME, activity.mDeviceName);
                                        editor.apply();
                                        activity.showNotification(R.drawable.stat_sys_device_connected);
                                        activity.displayState.setText("Connected to " + activity.mDeviceName);
                                    } else {
                                        /*TODO - Do we need preference entry?*/
                                        Log.d(TAG, "Handler.handleMessage: connected to another device named " +
                                                activity.mBluetoothDevice.getName() + " address: " + activity.mDeviceAddress +
                                                " mBluetoothDevice.getAddress(): " + activity.mBluetoothDevice.getAddress());
                                        activity.showNotification(R.drawable.stat_sys_device_connected);
                                        activity.displayState2.setText("Connected to " + ((BluetoothDevice)msg.obj).getName());
                                    }
                                    Toast.makeText(activity.getBaseContext(), "Device connected.", Toast.LENGTH_LONG).show();
                                    break;
                                case BluetoothHeadsetClient.STATE_DISCONNECTED: /*update device state to disconnected*/
                                    if(activity.mBluetoothDevice.equals(msg.obj)) {
                                        Log.d(TAG, "Handler.handleMessage: Device disconnected..");
                                        /*Edit the shared preference as the device is disconnected*/
                                        editor = activity.pref.edit();
                                        editor.putBoolean(CyHfpClientDeviceConstants.HF_DEVICE_CONNECTED, false);
                                        editor.putString(CyHfpClientDeviceConstants.HF_DEVICE_NAME, null);
                                        editor.putString(CyHfpClientDeviceConstants.HF_DEVICE_ADDRESS, null);
                                        editor.apply();
                                        activity.displayState.setText("Not connected");
                                    } else {
                                        Log.d(TAG, "Handler.handleMessage: Device disconnected.." + ((BluetoothDevice)msg.obj).getName());
                                        /*TODO - if preferences added for 2nd device, clear here*/
                                        activity.displayState2.setText("Not connected");
                                    }
                                    activity.clearDeviceInfo((BluetoothDevice)msg.obj);
                                    break;
                            }
                        } else {
                            Log.e(TAG, "Handler.handleMessage: Mismatched device..");
                        }
                    }
                    break;
                    case GUI_UPDATE_CALL_STATUS:
                        Bundle info = (Bundle)msg.obj;
                        activity.updateViewWithCallStatus(info);
                        break;
                    case GUI_UPDATE_AG_BUSY:
                        Toast.makeText(activity.getBaseContext(), "AG BUSY received", Toast.LENGTH_LONG).show();
                        break;
                    case GUI_UPDATE_DEVICE_INDICATORS: /*Update status of Battery, Signal etc.*/
                        activity.updateIndicators((Bundle)msg.obj);
                        break;
                    case GUI_UPDATE_AUDIO_STATE:
                        activity.updateViewAudioState((BluetoothDevice)msg.obj, msg.arg1);
                        break;
                    case GUI_UPDATE_VENDOR_AT_RSP:
                        Log.d(TAG, "Handler.handleMessage: showing vendor at command response");
                        int status = msg.arg1;
                        String toastMsg;
                        toastMsg = "AT vendor rsp status" + status;
                        if(null != msg.obj) {
                            toastMsg += "rsp = " + msg.obj.toString();
                        }
                        Toast.makeText(activity.getBaseContext(),
                                toastMsg.subSequence(0, toastMsg.length()), Toast.LENGTH_LONG).show();
                        break;
                    case GUI_UPDATE_WBS_STATE:
                        activity.updateViewWbsState(msg.arg1);
                        break;
                    case GUI_UPDATE_IN_BAND_STATUS:
                        activity.updateViewInBandState(msg.arg1);
                        break;
                    case GUI_UPDATE_OPERATOR:
                        b = (Bundle)msg.obj;
                        device = b.getParcelable(BluetoothDevice.EXTRA_DEVICE);
                        String opName = (String)b.getString(BluetoothHeadsetClient.EXTRA_OPERATOR_NAME);
                        if(activity.mBluetoothDevice.equals(device)) {
                            if (null != opName) {
                                Log.d(TAG, " Operator Name = " + opName);
                                activity.operatorName.setText(opName);
                            }
                        }
                        if(activity.mDeviceMap.containsKey(device)) {
                            activity.mDeviceMap.get(device).mOperatorName = opName;
                        }
                        break;
                    case GUI_UPDATE_SUBSCRIBER:
                        b = (Bundle)msg.obj;
                        device = b.getParcelable(BluetoothDevice.EXTRA_DEVICE);
                        String subscriberNum = (String)b.getString(BluetoothHeadsetClient.EXTRA_SUBSCRIBER_INFO);
                        if(activity.mBluetoothDevice.equals(device)) {
                            if (null != subscriberNum) {
                                Log.d(TAG, "Subscriber name = " + subscriberNum);
                                activity.subscriberNumber.setText(subscriberNum);
                            }
                        }
                        if(activity.mDeviceMap.containsKey(device)) {
                            activity.mDeviceMap.get(device).mSubscriberNum = subscriberNum;
                        }
                        break;
                    default:
                        break;
                }
            } else {
                Log.e(TAG, "Handler.handleMessages: Activity is null!");
            }
        }
    }
    private final hfpClientActivityMsgHandler viewUpdateHandler = new hfpClientActivityMsgHandler(this);

    /**
     * This function updates view related to call status
     */
    private void updateViewWithCallStatus(Bundle info) {
        int numActive;
        int numHeld;
        int callSetup;
        BluetoothDevice device;
        device = info.getParcelable(BluetoothDevice.EXTRA_DEVICE);
        BluetoothHeadsetClientCall callInfo = info.getParcelable(BluetoothHeadsetClient.EXTRA_CALL);
        if(null != callInfo) {
            numActive = getNumActiveCall(callInfo.getDevice());
            numHeld = getNumHeldCall(callInfo.getDevice());
            callSetup = callInfo.getState();
        } else {
            numActive = 0;
            numHeld = 0;
            callSetup = BluetoothHeadsetClientCall.CALL_STATE_TERMINATED;
        }

        switch(callSetup) {
            case BluetoothHeadsetClientCall.CALL_STATE_ACTIVE:
            case BluetoothHeadsetClientCall.CALL_STATE_DIALING:
            case BluetoothHeadsetClientCall.CALL_STATE_ALERTING:
            case BluetoothHeadsetClientCall.CALL_STATE_INCOMING:
                if(null != device) {
                    mCurrentCallingDevice = device;
                }
                break;

            case BluetoothHeadsetClientCall.CALL_STATE_HELD:
            case BluetoothHeadsetClientCall.CALL_STATE_HELD_BY_RESPONSE_AND_HOLD:
                break;

            case BluetoothHeadsetClientCall.CALL_STATE_WAITING:
                for(Map.Entry<BluetoothDevice, DeviceInfo> entry : mDeviceMap.entrySet()) {
                    if(null != entry.getValue()) {
                        BluetoothHeadsetClientCall call  = getClientCall(entry.getKey());
                        if((null != call) && (BluetoothHeadsetClientCall.CALL_STATE_ACTIVE == call.getState())) {
                            mCurrentCallingDevice = entry.getKey();
                        }
                    }
                }
                break;

            case BluetoothHeadsetClientCall.CALL_STATE_TERMINATED:
                for(Map.Entry<BluetoothDevice, DeviceInfo> entry : mDeviceMap.entrySet()) {
                    if(null != entry.getValue()) {
                        BluetoothHeadsetClientCall call  = getClientCall(entry.getKey());
                        if((null != call) && (BluetoothHeadsetClientCall.CALL_STATE_ACTIVE == call.getState())) {
                            mCurrentCallingDevice = entry.getKey();
                        }
                    }
                }
                break;
        }

        Log.d(TAG, "CyPhon " + "numActive " + numActive + " callSetupState " + callSetup + " numHeld " + numHeld);
        updateViewVrState(mVrState);

        /*When a call setup is in progress show the status*/
        if(isCallSetupInProgress(callInfo)) {
            updateViewOnCallProgress(device, callInfo);
            if((null != device) && (mDeviceMap.containsKey(device))) {
                mDeviceMap.get(device).callState = callSetup;
            }
            return;
        }

        if((mDeviceMap.containsKey(device))) {
            mDeviceMap.get(device).callState = callSetup;
        }
        if(null != callOptionsAlert) {
            Log.d(TAG, "isShowing = " + callOptionsAlert.isShowing());
        }
        if(null != callOptionsAlert) {
            callOptionsAlert.dismiss();
            callOptionsAlert = null;
            Log.d(TAG, "callOptionsAlert cancel try");
        }

        /*Cancel if alerting*/
        if(isVibrating && (null != vibrator)) {
            vibrator.cancel();
            isVibrating = false;
        }

        /*When there is no active calls*/
        if(isPhoneOnHook(callInfo)) {
            updateViewOnPhoneHook(device);
            return;
        }

        /*When there is active call*/
        updateViewOnActiveCall(callInfo);

        /*Update the audio state*/
        if (null != callInfo) {
            updateViewAudioState(device, mBluetoothHeadsetClient.getAudioState(callInfo.getDevice()));
        } else {
            updateViewAudioState(device, BluetoothHeadsetClient.STATE_AUDIO_DISCONNECTED);
        }

    }

    public AlertDialog callOptionsAlert = null;
    private AlertDialog.Builder callOptionsBuilder = null;

    private void showHfpClientDialog(BluetoothDevice device, int dialogID, String extraStringParam) {
        DialogFragment newHfpClientDialogFragment = HfpClientDialogFragments.newInstance(device, dialogID,
                extraStringParam);
        newHfpClientDialogFragment.show(getFragmentManager(), "dialog");
    }

    public static class HfpClientDialogFragments extends DialogFragment {

        public static HfpClientDialogFragments newInstance(BluetoothDevice device, int dialogID, String extraStringParam) {
            HfpClientDialogFragments dialogFrag = new HfpClientDialogFragments();
            Bundle args = new Bundle();
            args.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
            args.putInt("dialogID", dialogID);
            if(null != extraStringParam) {
                args.putString("extraStringParam", extraStringParam);
            }
            dialogFrag.setArguments(args);
            return dialogFrag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int dialogID = getArguments().getInt("dialogID");
            BluetoothDevice device = getArguments().getParcelable(BluetoothDevice.EXTRA_DEVICE);
            String extraStringParam = getArguments().getString("extraStringParam", "N/A");
            AlertDialog.Builder builder = null;
            Log.d(TAG, "onCreateDialog()");

            switch(dialogID) {
                case CyHfpClientDeviceConstants.HF_DEVICE_NOTCONNECTED_DIALOG_ID:
                {
                    Log.e(TAG, "onCreateDialog: Device Not Connected....");
                    builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Not Connected...");
                    builder.setMessage("Press OK to continue");
                    builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //dialog.cancel(); /*Do we really need this? - TODO*/
                            Message msg = Message.obtain();
                            msg.what = CyHfpClientDeviceConstants.HF_DEVICE_NOTCONNECTED_DIALOG_ID;
                            msg.arg1 = which;
                            msg.obj = device;
                            ((HfpClientMainActivity)getActivity())
                                    .handleDialogFragments(msg);

                        }
                    });
                }
                break;

                case CyHfpClientDeviceConstants.HF_DEVICE_SERVICE_NOT_ENABLED:
                {
                    Log.e(TAG, "onCreateDialog: Service not enabled");
                    builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Service Not Enabled..");
                    builder.setMessage("Press Ok to exit");
                    builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Message msg = Message.obtain();
                            msg.what = CyHfpClientDeviceConstants.HF_DEVICE_SERVICE_NOT_ENABLED;
                            msg.arg1 = which;
                            msg.obj = device;
                            ((HfpClientMainActivity)getActivity())
                                    .handleDialogFragments(msg);
                        }
                    });
                }
                break;

                case CyHfpClientDeviceConstants.HF_DEVICE_AT_CMD_ENTRY_DIALOG_ID:
                {
                    LayoutInflater factory = LayoutInflater.from(getActivity());
                    View textEntryView = factory.inflate(R.layout.hfdevice_atcmd_dialog, null);
                    builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("AT Command Input");
                    Log.d(TAG, "onCreateDialog: AT Command input..");
                    builder.setView(textEntryView);
                    Spinner spinner = (Spinner)textEntryView.findViewById(R.id.spinner);
                    final EditText cmdInputEditText = (EditText)textEntryView.findViewById(R.id.atcommand_input);
                    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                            R.array.vendor_cmds, android.R.layout.simple_spinner_item);
                    Log.d(TAG, "onCreateDialog: Spinner object = " + adapter);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(adapter);

                    class MyOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
                        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                            String s = parent.getItemAtPosition(pos).toString();
                            Log.d(TAG, "MyOnItemSelectedListener.onItemSelected: selected item is " + s);
                            cmdInputEditText.setText(s.subSequence(0, s.length()), TextView.BufferType.EDITABLE);
                        }

                        public void onNothingSelected(AdapterView parent) {
                            /*Nothing to do here or ? - TODO*/
                        }
                    }

                    spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Message msg = Message.obtain();
                            msg.what = CyHfpClientDeviceConstants.HF_DEVICE_AT_CMD_ENTRY_DIALOG_ID;
                            msg.arg1 = which;
                            msg.obj = cmdInputEditText.getText().toString(); /*TODO - Multi-Device*/
                            ((HfpClientMainActivity)getActivity())
                                    .handleDialogFragments(msg);
                        }
                    });

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Message msg = Message.obtain();
                            msg.what = CyHfpClientDeviceConstants.HF_DEVICE_AT_CMD_ENTRY_DIALOG_ID;
                            msg.arg1 = which; /*TODO - Multi-Device*/
                            ((HfpClientMainActivity)getActivity())
                                    .handleDialogFragments(msg);
                        }
                    });
                }
                break;

                case CyHfpClientDeviceConstants.HF_DEVICE_CALL_WAITING_DIALOG_ID:
                {
                    final CharSequence[] items = {"Reject waiting call",
                                                    "Accept waiting release active",
                                                    "Accept waiting hold active"};
                    builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Call waiting " + extraStringParam);
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "clicked " + which);
                            /*wait for call status change and handle error*/
                            Message msg = Message.obtain();
                            msg.what = CyHfpClientDeviceConstants.HF_DEVICE_CALL_WAITING_DIALOG_ID;
                            msg.arg1 = which;
                            Bundle b = new Bundle();
                            b.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
                            b.putString(EXTRA_INFO_STRING, extraStringParam);
                            msg.obj = b;
                            ((HfpClientMainActivity)getActivity())
                                    .handleDialogFragments(msg);
                        }
                    });
                }
                break;

                case CyHfpClientDeviceConstants.HF_DEVICE_MULTI_CALL_CONTROL_DIALOG_ID:
                {
                    final CharSequence[] items = {"End all held calls",
                                                    "End all active calls",
                                                    "Swap calls",
                                                    "Join calls"};
                    builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Call Control");
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "clicked " + which);
                            Message msg = Message.obtain();
                            msg.what = CyHfpClientDeviceConstants.HF_DEVICE_MULTI_CALL_CONTROL_DIALOG_ID;
                            msg.arg1 = which;
                            msg.obj = device;
                            ((HfpClientMainActivity)getActivity())
                                    .handleDialogFragments(msg);
                        }
                    });
                }
                break;

                case CyHfpClientDeviceConstants.HF_DEVICE_VOLUME_CHANGE_FAILED_DIALOG_ID:
                {
                    Log.e(TAG, "onCreateDialog: set volume failed....");
                    builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Set Volume failed...");
                    builder.setMessage("Operation allowed only when audio is connected");

                    builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Message msg = Message.obtain();
                            msg.what = CyHfpClientDeviceConstants.HF_DEVICE_VOLUME_CHANGE_FAILED_DIALOG_ID;
                            msg.arg1 = which;
                            msg.obj = device;
                            ((HfpClientMainActivity)getActivity())
                                    .handleDialogFragments(msg);
                        }
                    });
                }
                break;

                case CyHfpClientDeviceConstants.HF_DEVICE_ENHANCED_CALL_CONTROL_DIALOG_ID:
                {
                    LayoutInflater factory = LayoutInflater.from(getActivity());
                    View textEntryView = factory.inflate(R.layout.hfdevice_enhanced_call_dialog, null);
                    builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Enhanced Call Control");
                    builder.setView(textEntryView);
                    final EditText callIndex = (EditText)textEntryView.findViewById(R.id.getCallIndex);
                    builder.setPositiveButton("terminateCall", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "Enhanced Call Control: Release Specific Call Index");
                            int index = -1;
                            if(!callIndex.getText().toString().equals("")) {
                                index = Integer.parseInt(callIndex.getText().toString());
                            } else {
                                index = Integer.MAX_VALUE;
                            }
                            Message msg = Message.obtain();
                            msg.what = CyHfpClientDeviceConstants.HF_DEVICE_ENHANCED_CALL_CONTROL_DIALOG_ID;
                            msg.arg1 = which;
                            msg.arg2 = index;
                            msg.obj = device;
                            ((HfpClientMainActivity)getActivity())
                                    .handleDialogFragments(msg);
                        }
                    });

                    builder.setNegativeButton("separateCall", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "Enhanced Call Control: Private Consultation Mode");
                            int index = -1;
                            if(!callIndex.getText().toString().equals("")) {
                                index = Integer.parseInt(callIndex.getText().toString());
                            } else {
                                index = Integer.MAX_VALUE;
                            }
                            Message msg = Message.obtain();
                            msg.what = CyHfpClientDeviceConstants.HF_DEVICE_ENHANCED_CALL_CONTROL_DIALOG_ID;
                            msg.arg1 = which;
                            msg.arg2 = index;
                            msg.obj = device;
                            ((HfpClientMainActivity)getActivity())
                                    .handleDialogFragments(msg);
                        }
                    });
                }
                break;

                case CyHfpClientDeviceConstants.HF_DEVICE_DEVICE_SELECTION_DIALOG_ID:
                {
                    final CharSequence[] items = {"Device 1",
                            "Device 2"};
                    builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Select Device");
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "clicked " + which);
                            /*wait for call status change and handle error*/
                            Message msg = Message.obtain();
                            msg.what = CyHfpClientDeviceConstants.HF_DEVICE_DEVICE_SELECTION_DIALOG_ID;
                            msg.arg1 = which;
                            msg.obj = device;
                            ((HfpClientMainActivity)getActivity())
                                    .handleDialogFragments(msg);
                        }
                    });
                }
                break;
            }
            if(null != builder) {
                return builder.create();
            } else {
                return null;
            }
        }
    }

    public void handleDialogFragments(Message msg) {
        BluetoothDevice device;
        switch(msg.what) {
            case CyHfpClientDeviceConstants.HF_DEVICE_NOTCONNECTED_DIALOG_ID:
            {
                if(DialogInterface.BUTTON_NEGATIVE == msg.arg1) {
                    Log.d(TAG, "handleDialogFragments: Device not connected: Negative button click");
                    Message message = Message.obtain();
                    message.what = GUI_UPDATE_DEVICE_STATUS;
                    message.arg1 = BluetoothHeadsetClient.STATE_DISCONNECTED;
                    device = (BluetoothDevice) msg.obj;
                    message.obj = device;
                    viewUpdateHandler.sendMessage(message);
                } else {
                    Log.d(TAG, "handleDialogFragments: Device not connected: positive button click");
                }
            }
            break;

            case CyHfpClientDeviceConstants.HF_DEVICE_SERVICE_NOT_ENABLED:
            {
                if(DialogInterface.BUTTON_NEGATIVE == msg.arg1) {
                    Log.d(TAG, "handleDialogFragments: Service not enabled: Negative button click");
                    Message message = Message.obtain();
                    message.what = GUI_UPDATE_DEVICE_STATUS;
                    message.arg1 = BluetoothHeadsetClient.STATE_DISCONNECTED;
                    device = (BluetoothDevice) msg.obj;
                    message.obj = device;
                    viewUpdateHandler.sendMessage(message);
                } else {
                    Log.d(TAG, "handleDialogFragments: Service not enabled: Positive button click");
                }
            }
            break;

            case CyHfpClientDeviceConstants.HF_DEVICE_AT_CMD_ENTRY_DIALOG_ID:
            {
                if(DialogInterface.BUTTON_POSITIVE == msg.arg1) {
                    Log.d(TAG, "handleDialogFragments: AT cmd entry: Positive button click");
                    String cmd = (String)msg.obj;
                    cmd = cmd.trim();
                    if(cmd.contains("+VGM=")) {
                        String volumeStr = cmd.substring(cmd.indexOf("=")+1, cmd.length());
                        int volume = 0;
                        if(null != volumeStr) {
                            volume = Integer.parseInt(volumeStr);
                        }
                        Log.d(TAG, "Set volume Mic = " + volume);
                        /*No API to send VGM till now - TODO*/
                    } else if(cmd.contains("+VGS=")) {
                        String volumeStr = cmd.substring(cmd.indexOf("=")+1, cmd.length());
                        int volume = 0;
                        if(null != volumeStr) {
                            volume = Integer.parseInt(volumeStr);
                        }
                        Log.d(TAG, "Set volume Speaker = " + volume);
                        /*No API to send VGS till now - TODO*/
                    } else {
                        if(!mBluetoothHeadsetClient.sendVendorCmd(mBluetoothDevice, cmd)) { /*TODO - Multi-Device*/
                            Log.e(TAG, "handleDialogFragments: sendVendorCmd() failed");
                        }
                        Log.d(TAG, "handleDialogFragments: Entered command = " + cmd);
                    }
                } else {
                    Log.d(TAG, "handleDialogFragments: AT cmd entry: Negative button click");
                }
            }
            break;

            case CyHfpClientDeviceConstants.HF_DEVICE_CALL_WAITING_DIALOG_ID:
            {
                Log.d(TAG, "handleDialogFragments: call waiting: clicked " + msg.arg1);
                Bundle b = (Bundle)msg.obj;
                device = b.getParcelable(BluetoothDevice.EXTRA_DEVICE);
                String extraStringParam = b.getString(EXTRA_INFO_STRING);
                if(0 == msg.arg1) { /*Reject waiting call*/
                    mBluetoothHeadsetClient.rejectCall(device); /*AT+CHLD=0*/
                } else if(1 == msg.arg1) { /*Accept waiting release active*/
                    mBluetoothHeadsetClient.acceptCall(device, BluetoothHeadsetClient.CALL_ACCEPT_TERMINATE);
                    if(mBluetoothDevice.equals(device)) {
                        displayNumber.setText(extraStringParam);
                    } else {
                        if(mDeviceMap.containsKey(device)) {
                            displayNumber2.setText(extraStringParam);
                        }
                    }
                } else if(2 == msg.arg1) { /*Accept waiting hold active*/
                    mBluetoothHeadsetClient.acceptCall(device, BluetoothHeadsetClient.CALL_ACCEPT_HOLD);
                    if(mBluetoothDevice.equals(device)) {
                        displayNumber.setText(extraStringParam);
                    } else {
                        if(mDeviceMap.containsKey(device)) {
                            displayNumber2.setText(extraStringParam);
                        }
                    }
                } else {
                    Log.e(TAG, "handleDialogFragments: call waiting: unknown click");
                }
            }
            break;

            case CyHfpClientDeviceConstants.HF_DEVICE_MULTI_CALL_CONTROL_DIALOG_ID:
            {
                Log.d(TAG, "handleDialogFragments: multi call control: clicked " + msg.arg1);
                device = (BluetoothDevice)msg.obj;
                if(0 == msg.arg1) { /*End all held calls*/
                    mBluetoothHeadsetClient.rejectCall(device); /*AT+CHLD=0*/
                } else if(1 == msg.arg1) { /*AT+CHLD=1*/
                    mBluetoothHeadsetClient.acceptCall(device, BluetoothHeadsetClient.CALL_ACCEPT_TERMINATE);
                } else if(2 == msg.arg1) { /*AT+CHLD=2*/
                    mBluetoothHeadsetClient.acceptCall(device, BluetoothHeadsetClient.CALL_ACCEPT_HOLD);
                } else if(3 == msg.arg1) { /*AT+CHLD=3*/
                    mBluetoothHeadsetClient.acceptCall(device, BluetoothHeadsetClient.CALL_ACCEPT_NONE);
                } else {
                    Log.e(TAG, "handleDialogFragments: multi call control: unknown click");
                }
            }
            break;

            case CyHfpClientDeviceConstants.HF_DEVICE_VOLUME_CHANGE_FAILED_DIALOG_ID:
            {
                if(DialogInterface.BUTTON_NEGATIVE == msg.arg1) {
                    Log.d(TAG, "handleDialogFragments: Volume change failed: Negative button click");
                    /*Nothing to do*/
                } else {
                    Log.d(TAG, "handleDialogFragments: Volume change failed: Positive button click");
                }
            }
            break;

            case CyHfpClientDeviceConstants.HF_DEVICE_ENHANCED_CALL_CONTROL_DIALOG_ID:
            {
                int index = msg.arg2;
                device = (BluetoothDevice)msg.obj;
                if(DialogInterface.BUTTON_POSITIVE == msg.arg1) {
                    Log.d(TAG, "handleDialogFragments: Enhanced call control: Release Call index = " + index);
                    if(Integer.MAX_VALUE != index) {
                        /*No API avilable for AT+CHLD=1<index> - TODO*/
                    } else {
                        Toast.makeText(this, "Please Enter the Index of the call", Toast.LENGTH_LONG).show();
                    }
                } else if(DialogInterface.BUTTON_NEGATIVE == msg.arg1) {
                    Log.d(TAG, "handleDialogFragments: Enhanced call control: Private Consultation Mode: index = " + index);
                    if(Integer.MAX_VALUE != index) {
                        mBluetoothHeadsetClient.enterPrivateMode(device, index); /*AT+CHLD=2<index>*/
                    } else {
                        Toast.makeText(this, "Please Enter the Index of the call", Toast.LENGTH_LONG).show();
                    }
                }
            }
            break;

            case CyHfpClientDeviceConstants.HF_DEVICE_DEVICE_SELECTION_DIALOG_ID:
            {
                Log.d(TAG, "handleDialogFragments: device selection: clicked " + msg.arg1);
                device = (BluetoothDevice)msg.obj;
                if(0 == msg.arg1) { /*Device 1*/
                    dialNumber(mBluetoothDevice); /*place call to device 1*/
                } else if(1 == msg.arg1) { /*Device 2*/
                    for(Map.Entry<BluetoothDevice, DeviceInfo> entry : mDeviceMap.entrySet()) {
                        if((null != entry.getValue()) && (!mBluetoothDevice.equals(entry.getKey()))) {
                            mCurrentCallingDevice = entry.getKey();
                            dialNumber(mCurrentCallingDevice);
                            break;
                        }
                    }
                } else {
                    Log.e(TAG, "handleDialogFragments: device selection: unknown click");
                }
            }
            break;
        }
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {
        private static final String TAG = "MyMediaPlayer:MyBroadcastReceiver";
        private int reason;
        private String action;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (null != intent) {
                BluetoothDevice device;
                StringBuilder sb = new StringBuilder();
                action = intent.getAction();
                sb.append("Action: " + action + "\n");
                String log = sb.toString();
                Log.d(TAG, log);
                if (action.equals(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED)) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1);
                    int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                    if (BluetoothProfile.STATE_CONNECTED == newState) {
                        Bundle b = new Bundle();
                        b.putBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_3WAY_CALLING,
                                intent.getBooleanExtra(BluetoothHeadsetClient.EXTRA_AG_FEATURE_3WAY_CALLING, false));
                        b.putBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_REJECT_CALL,
                                intent.getBooleanExtra(BluetoothHeadsetClient.EXTRA_AG_FEATURE_REJECT_CALL, false));
                        b.putBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_ECC,
                                intent.getBooleanExtra(BluetoothHeadsetClient.EXTRA_AG_FEATURE_ECC, false));
                        b.putBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_ACCEPT_HELD_OR_WAITING_CALL,
                                intent.getBooleanExtra(BluetoothHeadsetClient.EXTRA_AG_FEATURE_ACCEPT_HELD_OR_WAITING_CALL, false));
                        b.putBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_RELEASE_HELD_OR_WAITING_CALL,
                                intent.getBooleanExtra(BluetoothHeadsetClient.EXTRA_AG_FEATURE_RELEASE_HELD_OR_WAITING_CALL, false));
                        b.putBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_RELEASE_AND_ACCEPT,
                                intent.getBooleanExtra(BluetoothHeadsetClient.EXTRA_AG_FEATURE_RELEASE_AND_ACCEPT, false));
                        b.putBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_MERGE,
                                intent.getBooleanExtra(BluetoothHeadsetClient.EXTRA_AG_FEATURE_MERGE, false));
                        b.putBoolean(BluetoothHeadsetClient.EXTRA_AG_FEATURE_MERGE_AND_DETACH,
                                intent.getBooleanExtra(BluetoothHeadsetClient.EXTRA_AG_FEATURE_MERGE_AND_DETACH, false));

                        DeviceInfo di = getDeviceInfo(device);
                        if (null == di) {
                            Log.e(TAG, "Cannnot allocate DI ");
                        } else {
                            di.updateDeviceInfo(b);
                        }
                    }

                    Log.d(TAG, action + ": newState = " + newState + " Device = " + device);
                    Message msg = Message.obtain();
                    msg.what = GUI_UPDATE_DEVICE_STATUS;
                    msg.arg1 = newState;
                    msg.arg2 = prevState;
                    msg.obj = device;
                    viewUpdateHandler.sendMessage(msg);
                }
                if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    reason = intent.getIntExtra(BluetoothDevice.EXTRA_REASON, 0xff);
                    Log.d(TAG, "Device = " + device + " Disconnection reason = " + reason);
                    Toast.makeText(mContext, String.valueOf(reason), Toast.LENGTH_SHORT).show();
                } else if (action.equals(BluetoothHeadsetClient.ACTION_CALL_CHANGED)) {
                    BluetoothHeadsetClientCall call = intent.getParcelableExtra(BluetoothHeadsetClient.EXTRA_CALL);
                    Log.d(TAG, "ACTION_CALL_CHANGED: Device = " + call.getDevice() +
                            " State = " + call.getState() + " Number = " + call.getNumber());

                    Message msg = Message.obtain();
                    msg.what = GUI_UPDATE_CALL_STATUS;
                    Bundle b = new Bundle();
                    b.putParcelable(BluetoothDevice.EXTRA_DEVICE, call.getDevice());
                    b.putParcelable(BluetoothHeadsetClient.EXTRA_CALL, call);
                    msg.obj = b;
                    viewUpdateHandler.sendMessage(msg);
                } else if (action.equals(BluetoothHeadsetClient.ACTION_AG_EVENT)) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(intent.hasExtra(BluetoothHeadsetClient.EXTRA_OPERATOR_NAME)) {
                        Message msg1 = Message.obtain();
                        msg1.what = GUI_UPDATE_OPERATOR;
                        Bundle b = new Bundle();
                        b.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
                        b.putString(BluetoothHeadsetClient.EXTRA_OPERATOR_NAME,
                                intent.getStringExtra(BluetoothHeadsetClient.EXTRA_OPERATOR_NAME));
                        msg1.obj = b;
                        viewUpdateHandler.sendMessage(msg1);
                    }
                    if(intent.hasExtra(BluetoothHeadsetClient.EXTRA_SUBSCRIBER_INFO)) {
                        Message msg2 = Message.obtain();
                        msg2.what = GUI_UPDATE_SUBSCRIBER;
                        Bundle b = new Bundle();
                        b.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
                        b.putString(BluetoothHeadsetClient.EXTRA_SUBSCRIBER_INFO,
                                intent.getStringExtra(BluetoothHeadsetClient.EXTRA_SUBSCRIBER_INFO));
                        msg2.obj = b;
                        viewUpdateHandler.sendMessage(msg2);
                    }
                    if((intent.hasExtra(BluetoothHeadsetClient.EXTRA_BATTERY_LEVEL)) ||
                    (intent.hasExtra(BluetoothHeadsetClient.EXTRA_NETWORK_SIGNAL_STRENGTH)) ||
                            (intent.hasExtra(BluetoothHeadsetClient.EXTRA_NETWORK_ROAMING)) ||
                            (intent.hasExtra(BluetoothHeadsetClient.EXTRA_NETWORK_STATUS))) {
                        Message msg3 = Message.obtain();
                        msg3.what = GUI_UPDATE_DEVICE_INDICATORS;
                        Bundle b = new Bundle();
                        b.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
                        b.putParcelable(EXTRA_INDICATOR_INFO, mBluetoothHeadsetClient.getCurrentAgEvents(device));
                        msg3.obj = b;
                        viewUpdateHandler.sendMessage(msg3);
                    }
                } else if(action.equals(BluetoothHeadsetClient.ACTION_AUDIO_STATE_CHANGED)) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                    int prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1);
                    Message msg1 = Message.obtain();
                    msg1.what = GUI_UPDATE_AUDIO_STATE;
                    msg1.arg1 = newState;
                    msg1.obj = device;
                    viewUpdateHandler.sendMessage(msg1);
                    if(BluetoothHeadsetClient.STATE_AUDIO_CONNECTED == newState) {
                        if(intent.hasExtra(BluetoothHeadsetClient.EXTRA_AUDIO_WBS)) {
                            Log.d(TAG, "wbs state = " + intent.getBooleanExtra(BluetoothHeadsetClient.EXTRA_AUDIO_WBS, false));
                            //ag.setBluetoothScoOn(true);
                            //ag.stopBluetoothSco();
                            //ag.setMode(AudioManager.MODE_NORMAL);
                            //ag.setParameters("hfp_volume=0");
                            //ag.setStreamVolume(AudioManager.STREAM_MUSIC, ag.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_SHOW_UI);
                            //ag.setStreamVolume(AudioManager.STREAM_VOICE_CALL, ag.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), AudioManager.FLAG_SHOW_UI);
                            //ag.setStreamVolume(6, ag.getStreamMaxVolume(6), AudioManager.FLAG_SHOW_UI);
                            checkandRequestAudioFocus();
                            //ag.requestAudioFocus(null, 6, AudioManager.AUDIOFOCUS_GAIN);
                            //ag.setMode(AudioManager.MODE_IN_COMMUNICATION);
                            //ag.setBluetoothScoOn(true);
                            //ag.setSpeakerphoneOn(true);
                            Log.d(TAG, "is SCO on = " + ag.isBluetoothScoOn());
                            Log.d(TAG, "is Music active = " + ag.isMusicActive());
                            Log.d(TAG, "is speaker phone on = " + ag.isSpeakerphoneOn());
                            Log.d(TAG, "current mode = " + ag.getMode());
                            Message msg2 = Message.obtain();
                            msg2.what = GUI_UPDATE_WBS_STATE;
                            msg2.arg1 = intent.getBooleanExtra(BluetoothHeadsetClient.EXTRA_AUDIO_WBS, false) ? WBS_YES : WBS_NONE;
                            msg2.obj = device;
                            viewUpdateHandler.sendMessage(msg2);
                        } else {
                            Log.d(TAG, "No WBS found ");
                        }
                    }
                }
            }
        }
    }

    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG, "onAudioFocusChange " + focusChange);
            if(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT == focusChange) {
                hasAudioFocused = false;

                if(isInCall(mBluetoothDevice)) {
                    checkandRequestAudioFocus();
                }
            } else if(AudioManager.AUDIOFOCUS_GAIN == focusChange) {
                   hasAudioFocused = true;
               } else if(AudioManager.AUDIOFOCUS_LOSS == focusChange) {
                   hasAudioFocused = false;
                   if(isInCall(mBluetoothDevice)) {
                       checkandRequestAudioFocus();
                   }
               }
            }
        };

    private synchronized void checkandRequestAudioFocus() {
        Log.d(TAG, "checkandRequestAudioFocus hasAudioFocus = " + hasAudioFocused);
        if(!hasAudioFocused){
            int result = ag.requestAudioFocus(afChangeListener, AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            if(AudioManager.AUDIOFOCUS_REQUEST_GRANTED == result) {
                hasAudioFocused = true;
                Log.d(TAG, "checkandRequestAudioFocus: AudioManager.AUDIOFOCUS_REQUEST_GRANTED");
            } else {
                Log.e(TAG, "checkandRequestAudioFocus: AUDIOFOCUS rejected result = " + result);
            }
        } else {
            Log.d(TAG, "checkandRequestAudioFocus: Has audio focus already");
        }
    }

    private boolean isInCall(BluetoothDevice device) {
        int numActive = getNumActiveCall(device);
        int numHeld = getNumHeldCall(device);
        int callSetupState = getCallSetupState(device);
        boolean isInCall = false;

        if((0 != (numActive+numHeld)) ||
                ((BluetoothHeadsetClientCall.CALL_STATE_TERMINATED != callSetupState)) &&
                        ((BluetoothHeadsetClientCall.CALL_STATE_WAITING == callSetupState) ||
                                (BluetoothHeadsetClientCall.CALL_STATE_INCOMING == callSetupState) ||
                                (BluetoothHeadsetClientCall.CALL_STATE_ALERTING == callSetupState) ||
                                (BluetoothHeadsetClientCall.CALL_STATE_DIALING == callSetupState))) {
            isInCall = true;
        }
        Log.d(TAG, "isInCall = " + isInCall);
        return isInCall;
    }
}
