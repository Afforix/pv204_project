/*
AID: 0113158A9A
 */
package applets;

// IMPORTED PACKAGES
import javacard.framework.*;


/**
 * @author saros
 */
public class KeePassApplet extends javacard.framework.Applet {

    // ERROR CODES
    final static short SW_BAD_PIN = (short) 0x6900;
    final static short SW_NOT_AUTHENTICATED = (short) 0x6714;

    // MAIN INSTRUCTION CLASS
    final static byte CLA_KEEPASSAPLET = (byte) 0x80;

    // INSTRUCTIONS
    final static byte INS_VERIFYPIN = (byte) 0x00;
    final static byte INS_SENDPASSWD = (byte) 0x01;
    final static byte INS_SETPIN = (byte) 0x02;
    final static byte INS_SETPASSWD = (byte) 0x03;

    private OwnerPIN clientPIN = null;
    private byte password[] = null;
    private short passwordLength = 0;

    /**
     * Consturctor, whole setup happens here
     */
    protected KeePassApplet() {
        password = new byte[128];

        // INIT GLOBAL PIN
        clientPIN = new OwnerPIN((byte) 3, (byte) 4);
        byte[] initial_pin = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        clientPIN.update(initial_pin, (short) 0, (byte) 4);

        register();
    }

    /**
     * Installs this applet.
     *
     * @param _bArray the array containing installation parameters
     * @param _bOffset the starting offset in bArray
     * @param _bLength the length in bytes of the parameter data in bArray
     */
    public static void install(byte[] _bArray, short _bOffset, byte _bLength) {
        // APPLET INSTANCE CREATION
        new KeePassApplet();
    }

    public boolean select() {
        clientPIN.reset();
        return clientPIN.getTriesRemaining() != 0;
    }

    /**
     * Deselect method called by the system in the deselection process.
     */
    public void deselect() {
        // so far nothing
    }

    /**
     * Processes an incoming APDU.
     *
     * @see APDU
     * @param _apdu the incoming APDU
     */
    public void process(APDU apdu) throws ISOException {
        // get the APDU buffer
        byte[] apduBuff = apdu.getBuffer();

        // ignore the applet select command dispached to the process
        if (selectingApplet()) {
            return;
        }

        // APDU instruction parser
        if (apduBuff[ISO7816.OFFSET_CLA] == CLA_KEEPASSAPLET) {
            switch (apduBuff[ISO7816.OFFSET_INS]) {
                case INS_VERIFYPIN:
                    verifyPIN(apdu);
                    break;
                case INS_SETPIN:
                    setPIN(apdu);
                    break;
                case INS_SETPASSWD:
                    setPasswd(apdu);
                    break;
                case INS_SENDPASSWD:
                    sendPasswd(apdu);
                    break;
                default:
                    ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
                    break;
            }
        } else {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }
    }
    
    /**
     * Verifies pin on the card. 
     * @param apdu - to get the pin from.
     */
    void verifyPIN(APDU apdu) {
        byte[] apdubuf = apdu.getBuffer();
        short dataLen = apdu.setIncomingAndReceive();

        // VERIFY PIN
        if (clientPIN.check(apdubuf, ISO7816.OFFSET_CDATA, (byte) dataLen) == false) {
            ISOException.throwIt(SW_BAD_PIN);
        }
    }

    /**
     * Sets pin on the card. This is allowed only when no pin is set or the pin
     * is verified
     *
     * @param apdu to get the pin from
     */
    void setPIN(APDU apdu) {
        
        if(!clientPIN.isValidated()){
            ISOException.throwIt(SW_NOT_AUTHENTICATED);
        }
        
        byte[] apduBuff = apdu.getBuffer();
        short data_length = apdu.setIncomingAndReceive();
        // SET NEW PIN
        clientPIN.update(apduBuff, ISO7816.OFFSET_CDATA, (byte) data_length);
        
    }

    /**
     * Sets the password for the card, i.e. 128 bytes. Allowed only when pin is
     * verified. Rewrites the old password (if any).
     *
     * @param apdu to get the password from
     */
    void setPasswd(APDU apdu) {
        if(!clientPIN.isValidated()){
            ISOException.throwIt(SW_NOT_AUTHENTICATED);
        }
        byte[] apduBuff = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();
        
        passwordLength = dataLength;
        Util.arrayCopyNonAtomic(apduBuff, ISO7816.OFFSET_CDATA, password, (byte) 0, dataLength);
            
    }

    /**
     * Sends password to the client app. Allowed only when PIN is verified.
     */
    void sendPasswd(APDU apdu) {
        if(!clientPIN.isValidated()){
            ISOException.throwIt(SW_NOT_AUTHENTICATED);
        }
        byte[] apduBuff = apdu.getBuffer();
        Util.arrayCopyNonAtomic(password, (short) 0, apduBuff, ISO7816.OFFSET_CDATA, passwordLength);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, passwordLength);
        
    }
}
