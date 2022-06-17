import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;

public class CryptoGraphy {
    public static void main(String[] args) {
        System.out.println("hghhgh");
        try {
            PrivateKey privateKey = loadPrivateKey();
            PublicKey publicKey = loadPublicKey();

            String message = "Mwai";
            //create a digest from the message
            byte[] messageDigest = generateMessageHash(message);
            //generate signature from the digest
            byte[] signature = encryptHash(privateKey,messageDigest);

            //verify message by taking signature
            Boolean status = verifySignature(publicKey,signature,message);
            System.out.println(status.toString());

        }catch (Exception exception){
            System.err.println(exception.getMessage());
        }
    }

    public static PrivateKey loadPrivateKey() throws KeyStoreException, IOException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new FileInputStream("E:\\PROJECTS\\java\\demo\\cryptograpghy\\src\\main\\java\\sender_keystore.p12"), "changeit".toCharArray());
        PrivateKey privateKey =
                (PrivateKey) keyStore.getKey("senderKeyPair", "changeit".toCharArray());
        return privateKey;
    }

    public static PublicKey loadPublicKey() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new FileInputStream("E:\\PROJECTS\\java\\demo\\cryptograpghy\\src\\main\\java\\receiver_keystore.p12"), "changeit".toCharArray());
        Certificate certificate = keyStore.getCertificate("receiverKeyPair");
        PublicKey publicKey = certificate.getPublicKey();
        return publicKey;
    }

    //generating the message digest/hash
    /*
    * generating the digest from the message
    * */
    public static byte[] generateMessageHash(String message) throws NoSuchAlgorithmException {
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] messageHash = md.digest(messageBytes);
        return messageHash;
    }

    //generating message signature from the hash
    /*
    * @messageHash hash generated from payload
    * We are obtaining a signature */
    public static byte[] encryptHash(PrivateKey privateKey,byte[] messageHash) throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] digitalSignature = cipher.doFinal(messageHash);
        return digitalSignature;
    }

    /*
    * verifying the signature
    * param @publickey public key from pesalink
    * @encryptedMessageHash/signature
    * @message the xm payload without signature */
    public static Boolean verifySignature(PublicKey publicKey,byte[] encryptedMessageHash,String message) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        //digest in the message
        byte[] decryptedMessageHash = cipher.doFinal(encryptedMessageHash);


        MessageDigest md = MessageDigest.getInstance("SHA-256");
        //the xml payload
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] newMessageHash = md.digest(messageBytes);
        boolean isCorrect = Arrays.equals(decryptedMessageHash, newMessageHash);
        return isCorrect;
    }
}
