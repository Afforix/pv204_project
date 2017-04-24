/*
AID: 0113158A9A
 */
package keepassapplet;

// IMPORTED PACKAGES
import javacard.framework.*;

/**
 * @author saros
 */
public class KeePassApplet extends Applet {

    // MAIN INSTRUCTION CLASS
    final static byte   CLA_KEEPASSAPLET        = ( byte ) 0xB0;

    // INSTRUCTIONS
    final static byte   INS_SETPIN              = ( byte ) 0x20;
    final static byte   INS_SETPASSWD           = ( byte ) 0x21;
    final static byte   INS_SENDPIN             = ( byte ) 0x22;
    final static byte   INS_GETPASSWD           = ( byte ) 0x23;
    
    final static short  ARRAY_LENGTH            = ( short ) 0xff;
    
    private OwnerPIN    client_pin              = null;
    
    private byte        e_dataArray[]           = null;

    /**
     * Installs this applet.
     * @param _bArray the array containing installation parameters
     * @param _bOffset the starting offset in bArray
     * @param _bLength the length in bytes of the parameter data in bArray
     */
    public static void install( byte[] _bArray, short _bOffset, byte _bLength ) {
        // APPLET INSTANCE CREATION
        new KeePassApplet();
    }

    /**
     * Only this class's install method should create the applet object.
     */
    protected KeePassApplet() {
        e_dataArray     = new byte[ ARRAY_LENGTH ];
        Util.arrayFillNonAtomic( e_dataArray, ( short ) 0, ARRAY_LENGTH, ( byte ) 0 );
        
        // INIT GLOBAL PIN
        client_pin      = new OwnerPIN( ( byte ) 3, ( byte ) 4 );
        client_pin.update( e_dataArray, ( byte ) 0, ( byte ) 4 );
        
        register();
    }
    
    /*
    public void select() {
    
    }
    */
    
    /**
     * Deselect method called by the system in the deselection process.
     */
    public void deselect() {
        // RESET THE PIN OF THE CARD
        client_pin.reset();
    }

    /**
     * Processes an incoming APDU.
     * @see APDU
     * @param _apdu the incoming APDU
     */
    public void process( APDU _apdu ) throws ISOException {
        // get the APDU buffer
        byte[] _apduBuff        = _apdu.getBuffer();

        // ignore the applet select command dispached to the process
        if ( selectingApplet() ) {
            return;
        }

        // APDU instruction parser
        if ( _apduBuff[ ISO7816.OFFSET_CLA ] == CLA_KEEPASSAPLET ) {
            switch ( _apduBuff[ ISO7816.OFFSET_CLA ] ) {
                case INS_SETPIN:
                    setPIN( _apdu );
                    break;
                case INS_SETPASSWD:
                    setPasswd( _apdu );
                    break;
                case INS_SENDPIN:
                    sendPIN( _apdu );
                    break;
                case INS_GETPASSWD:
                    getPasswd( _apdu );
                    break;
                default:
                    ISOException.throwIt( ISO7816.SW_INS_NOT_SUPPORTED );
                    break;
            }
        } else {
            ISOException.throwIt( ISO7816.SW_CLA_NOT_SUPPORTED );
        }
    }

    // SET GLOBAL PIN OF THE CARD
    void setPIN( APDU _apdu ) {
        byte[]  _apduBuff       = _apdu.getBuffer();
        short   _data_length    = _apdu.setIncomingAndReceive();
        
        // SET NEW PIN
        client_pin.update( _apduBuff, ISO7816.OFFSET_CDATA, ( byte ) _data_length );
    }

    // SET GLOBAL PASSWORD OF THE CARD
    void setPasswd( APDU _apdu ) {
        
    }

    // SEND PIN TO THE APPLICATION FOR THE CLIENT
    void sendPIN( APDU _apdu ) {

    }

    // GET THE PASSWORD SEND BY THE CLIENT APPLICATION
    void getPasswd( APDU _apdu ) {
        
    }
}
