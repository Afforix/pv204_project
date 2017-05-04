
package applets;

// IMPORTED PACKAGES
import javacard.framework.*;
import javacard.security.AESKey;
import javacard.security.KeyBuilder;
import javacard.security.RandomData;
import javacardx.crypto.Cipher;


/**
 * @author saros
 */
public class KeePassApplet extends javacard.framework.Applet {

    // Error codes
    final static short SW_BAD_PIN = (short) 0x6900;
    final static short SW_NOT_AUTHENTICATED = (short) 0x6714;

    // Main instruction class
    final static byte CLA_KEEPASSAPLET = (byte) 0x80;

    // Instructions
    final static byte INS_VERIFYPIN = (byte) 0x00;
    final static byte INS_SENDPASSWD = (byte) 0x01;
    final static byte INS_SETPIN = (byte) 0x02;
    final static byte INS_SETPASSWD = (byte) 0x03;

    // Variables
    private OwnerPIN clientPIN = null;
    private byte password[] = null;
    private short passwordLength = 0;
    private AESKey m_aesKey = null; // used to encrypt stored password
    private RandomData m_secureRandom = null; // used to generate the key to store password
    private Cipher m_encryptCipher = null; // for encrypting and decrypting key
    private Cipher m_decryptCipher = null;
    private byte m_ramArray[] = null;

    /**
     * Consturctor, whole setup happens here
     */
    protected KeePassApplet() {
        password = new byte[128]; //not set initially.
        m_ramArray = JCSystem.makeTransientByteArray((short) 128, JCSystem.CLEAR_ON_DESELECT);
        //init key
        m_aesKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
        
        // create objects for cbc ciphering
        m_encryptCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
        m_decryptCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);

        // initialize random object
        m_secureRandom = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
        
        // create random AES key, used to encrypt/decrypt the password, no-one except the card knows the key
        // the only purpose is to keep the password encrypted on the card
        m_secureRandom.generateData(m_ramArray, (short) 0, (short) 16);
        m_aesKey.setKey(m_ramArray, (short) 0);
        
        //init IV
        m_secureRandom.generateData(m_ramArray, (short) 0, (short) 16);
        m_encryptCipher.init(m_aesKey, Cipher.MODE_ENCRYPT, m_ramArray, (short) 0, (short) 16);
        m_decryptCipher.init(m_aesKey, Cipher.MODE_DECRYPT, m_ramArray, (short) 0, (short) 16);
        
        // Initialize PIN to '0000'
        clientPIN = new OwnerPIN((byte) 3, (byte) 4);
        byte[] initial_pin = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        clientPIN.update(initial_pin, (short) 0, (byte) 4);

        // register this instance
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
       
        // encrypt the incoming buffer straight away and store the result. 
        // notice that we always encrypt 128 bytes. As javacard does not support padding
        // it is the reasonable way how to handle it. As we encrypt "something" and we keep the length
        // of the password saved (length is unencrypted), we always know how much to send back.
        m_encryptCipher.doFinal(apduBuff, ISO7816.OFFSET_CDATA, (short) 128, password, (short) 0);
    }

    /**
     * Sends password to the client app. Allowed only when PIN is verified.
     */
    void sendPasswd(APDU apdu) {
        if(!clientPIN.isValidated()){
            ISOException.throwIt(SW_NOT_AUTHENTICATED);
        }
        byte[] apduBuff = apdu.getBuffer();
        
        //decrypt the password to the outgoing buffer
        m_decryptCipher.doFinal(password, (short) 0, (short) 128, apduBuff, ISO7816.OFFSET_CDATA);
        
        //buffer is filled with decrypted 128bytes, but only "passwordLength" bytes is sent.
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, passwordLength);
        
    }
}
