package cy.app.bt.hfpclient.cyphon;

/**
 * Created by user on 9/4/18.
 */

public class CyHfpClientDeviceConstants {
    public static final String HF_DEVICE_NAME = "cy.app.bt.hfpclient.cyphon.devicename";
    public static final String HF_DEVICE_ADDRESS = "cy.app.bt.hfpclient.cyphon.deviceaddress";
    public static final String HF_DEVICE_CONNECTED = "cy.app.bt.hfpclient.cyphon.connected";
    public static final String HF_DEVICE_STATUS = "cy.app.bt.hfpclient.cyphon.status";
    public static final String HF_DEVICE_STATE_CHANGE_TYPE = "cy.app.bt.hfpclient.cyphon.statechangetype";
    public static final String HF_DEVICE_WBS_STATE = "cy.app.bt.hfpclient.cyphon.wbs_state";

    public static final int HF_DEVICE_NOTCONNECTED_DIALOG_ID = 1;
    public static final int HF_DEVICE_SERVICE_NOT_ENABLED = 2;
    public static final int HF_DEVICE_AT_CMD_ENTRY_DIALOG_ID = 3;
    public static final int HF_DEVICE_CALL_WAITING_DIALOG_ID = 4;
    public static final int HF_DEVICE_MULTI_CALL_CONTROL_DIALOG_ID = 5;
    public static final int HF_DEVICE_VOLUME_CHANGE_FAILED_DIALOG_ID = 6;
    public static final int HF_DEVICE_ENHANCED_CALL_CONTROL_DIALOG_ID = 7;
    public static final int HF_DEVICE_DEVICE_SELECTION_DIALOG_ID = 8;
    public static final int HF_NOTIFICATION_ID = 1000001; /*Identifier for the notification*/

    public final static int PEER_FEAT_3WAY     = 0x00000001;
    // Echo cancellation and/or noise reduction
    public final static int PEER_FEAT_ECNR     = 0x00000002;
    // Voice recognition
    public final static int PEER_FEAT_VREC     = 0x00000004;
    // In-band ring tone
    public final static int PEER_FEAT_INBAND   = 0x00000008;
    // Attach a phone number to a voice tag
    public final static int PEER_FEAT_VTAG     = 0x00000010;
    // Ability to reject incoming call
    public final static int PEER_FEAT_REJECT   = 0x00000020;
    // Enhanced Call Status
    public final static int PEER_FEAT_ECS      = 0x00000040;
    // Enhanced Call Control
    public final static int PEER_FEAT_ECC      = 0x00000080;
    // Extended error codes
    public final static int PEER_FEAT_EXTERR   = 0x00000100;
    // Codec Negotiation
    public final static int PEER_FEAT_CODEC    = 0x00000200;

    // AG's 3WC features masks
    // match up with masks in bt_hf_client.h
    // 0  Release waiting call or held calls
    public final static int CHLD_FEAT_REL           = 0x00000001;
    // 1  Release active calls and accept other (waiting or held) cal
    public final static int CHLD_FEAT_REL_ACC       = 0x00000002;
    // 1x Release specified active call only
    public final static int CHLD_FEAT_REL_X         = 0x00000004;
    // 2  Active calls on hold and accept other (waiting or held) call
    public final static int CHLD_FEAT_HOLD_ACC      = 0x00000008;
    // 2x Request private mode with specified call (put the rest on hold)
    public final static int CHLD_FEAT_PRIV_X        = 0x00000010;
    // 3  Add held call to multiparty */
    public final static int CHLD_FEAT_MERGE         = 0x00000020;
    // 4  Connect two calls and leave (disconnect from) multiparty */
    public final static int CHLD_FEAT_MERGE_DETACH  = 0x00000040;
}
