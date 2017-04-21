# The desing of secure channel

## Usage scenario

##### What are the sensitive objects?

* Private key on the smart card
* PIN
* Session keys
* Key for password Manager stored on the smart card

##### What are these sensitive objects used for and what is the data flow of these objects?

* Private key stays on the smart card, used to decrypt session keys
* Session keys are used for secure channel communication, they stay on PC and SC, go trough non secure channel once, encrypted with public key


##### What are the capabilites of the attacker

Absolutely no idea...

##### What are the points where an attacker can observe the system?

* The attacker can obtain any message he wishes during the communication between SC and PC
* The attacker can obtain the binary of PC app

##### Which parts of the system muset be trusted to obtain required functionality?

* The PC app
* Assumption that keys cannot be retreived from SC (no forward secrecy included)

### Authentication

The PC-application (PC) obtains Smart Card (SC) public key during the initial configuration. Further on, SC proves its identity to PC by decrypting
with its private key. PC is a point of trust, i.e. we trust the binary we run.  

### Session keys

As PC is a point of trust, session keys can be forced to specific values by PC app. During the secure channel initialization, the PC
app sends two AES keys (one for encryption, second for HMAC) and session token to the SC. Whole message is encrypted by SC public-key. The integrity of this message remains unprotected. 
Those two symmetric keys are further used for communication troughout whole session. 

### Confidentiality

Once the card obtains the symmetric keys for encryption, all messages are protected by encryption.  

### Integrity protecion

All messages are protected with HMAC code, using the symmetric session MAC key. Moreover, headers of all APDU commands are protected as well, sent in
the body of the message.

### Freshness

### Exchanged messages

Each message should have its unique header.

1. PC -> SC: AES_enc key, AES_mac key, session_token
2. SC -> PC: OK acknowledgment
3. PC -> SC: PIN Sent
4. SC -> PC: Key sent / PIN NOK
5. PC -> SC: OK, terminating / resending PIN 

### Exact specification

TBA

### Random notes

* Secure channel is closed on any error or unexpected data receival
* Encryption is done in CBC mode

### Extra features

* Provide forward secrecy
* Add dummy traffic, messages of fixed length

