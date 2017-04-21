# The desing of secure channel

### Authentication

The PC-application (PC) obtains Smart Card (SC) public key during the initial configuration. Further on, SC proves its identity to PC by decrypting
with its private key. PC is a point of trust, i.e. we trust the binary we run.  

### Session keys

As PC is a point of trust, session keys can be forced to specific values by PC app. During the secure channel initialization, the PC
app sends two AES keys (one for encryption, second for MAC) and session token to the SC. Whole message is encrypted by SC public-key. The integrity of this message remains unprotected. 
Those two symmetric keys are further used for communication troughout whole session. 

### Confidentiality

Once the card obtains the symmetric keys for encryption, all messages are protected by encryption.  

### Integrity protecion

All messages are protected with MAC code, using the symmetric session MAC key. Moreover, headers of all APDU commands are protected as well, sent in
the body of the message.

### Freshness

### Exchanged messages

### Exact specification

TBA

### Random notes

### Extra features



