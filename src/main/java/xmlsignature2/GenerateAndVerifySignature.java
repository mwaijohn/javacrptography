package xmlsignature2;

import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class GenerateAndVerifySignature {

    static {
        org.apache.xml.security.Init.init();
    }

    public static void main(String[] args) throws Exception {GenerateAndVerifySignature generateAndVerifySignature = new GenerateAndVerifySignature();
       String signedMessage =  generateAndVerifySignature.generateXMLDigitalSignature(TestString.sampleXML);

       System.out.println(signedMessage);
       KeyStore keyStore = getKeyStore();
       PublicKey publicKey = keyStore.getCertificate("1").getPublicKey();

       System.out.println(isXmlDigitalSignatureValid(signedMessage,publicKey));

       validateAuthenticity(signedMessage);


        String withoutSignature = removeXMLNode("Signature",signedMessage);
        String newMessage = generateAndVerifySignature.generateXMLDigitalSignature(withoutSignature);

        System.out.println(newMessage);

        System.out.println(validateAuthenticity(withoutSignature));
    }


    public  String generateXMLDigitalSignature(String xmlToSign)
            throws UnrecoverableEntryException, CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException, ParserConfigurationException, SAXException, TransformerException {
        //Get the XML Document object
        Document doc = convertStringToDocument(xmlToSign);
        //Create XML Signature Factory
        XMLSignatureFactory xmlSigFactory = XMLSignatureFactory.getInstance("DOM");

        KeyStore keyStore = getKeyStore();

        PrivateKey privateKey = (PrivateKey) keyStore.getKey("1", "123456".toCharArray());
        DOMSignContext domSignCtx = new DOMSignContext(privateKey, doc.getDocumentElement());
        Reference ref = null;
        SignedInfo signedInfo = null;
        try {
            ref = xmlSigFactory.newReference("", xmlSigFactory.newDigestMethod(DigestMethod.SHA1, null),
                    Collections.singletonList(xmlSigFactory.newTransform(Transform.ENVELOPED,
                            (TransformParameterSpec) null)), null, null);
            signedInfo = xmlSigFactory.newSignedInfo(
                    xmlSigFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
                            (C14NMethodParameterSpec) null),
                    xmlSigFactory.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                    Collections.singletonList(ref));
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException ex) {
            ex.printStackTrace();
        }
        //Pass the Public Key File Path
        KeyInfo keyInfo = getKeyInfo(keyStore,xmlSigFactory);

        //Create a new XML Signature
        XMLSignature xmlSignature = xmlSigFactory.newXMLSignature(signedInfo, keyInfo);
        try {
            //Sign the document
            xmlSignature.sign(domSignCtx);
        } catch (MarshalException ex) {
            ex.printStackTrace();
        } catch (XMLSignatureException ex) {
            ex.printStackTrace();
        }
        //Store the digitally signed document inta a location
        //storeSignedDoc(doc, destnSignedXmlFilePath);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter stringWriter = new StringWriter();
        StreamResult streamResult = new StreamResult(stringWriter);
        transformer.transform(new DOMSource(doc),streamResult);

        return new String(stringWriter.toString().getBytes(StandardCharsets.US_ASCII));
    }

    private Document getXmlDocument(String originalXmlFilePath) throws ParserConfigurationException, IOException, SAXException {
//        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//        dbf.setNamespaceAware(true);
//        Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(originalXmlFilePath)));
//
        File file = new File(originalXmlFilePath);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        return doc;
    }

    public static KeyStore getKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
        final String KEY_STORE = "keystore.p12";
        final String KEY_STORE_TYPE = "PKCS12";
        final String KEY_STORE_PASSWORD = "123456";

        FileInputStream fileInputStream = new FileInputStream("E:\\PROJECTS\\java\\demo\\cryptograpghy\\keystore.p12");

        KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
        keyStore.load(fileInputStream, KEY_STORE_PASSWORD.toCharArray());

        PrivateKey privateKey = (PrivateKey) keyStore.getKey("1", "123456".toCharArray());
        PublicKey publicKey = keyStore.getCertificate("1").getPublicKey();

        return keyStore;
    }

    public KeyInfo getKeyInfo(KeyStore keyStore,XMLSignatureFactory xmlSignatureFactory) throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException {
        KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry("1",new KeyStore.PasswordProtection("123456".toCharArray()));
        X509Certificate x509Certificate = (X509Certificate) keyEntry.getCertificate();
        KeyInfoFactory keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();

        List<Object> x509Content = new ArrayList<>();
        x509Content.add(x509Certificate);
        X509Data data = keyInfoFactory.newX509Data(x509Content);
        KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(data));
        return keyInfo;
    }

    public static boolean isXmlDigitalSignatureValid(String signedXmlString, PublicKey publicKey) throws Exception {
        boolean validFlag = false;
        Document doc = convertStringToDocument(signedXmlString);
        NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

        if (nl.getLength() == 0) {
            throw new Exception("No XML Digital Signature Found, document is discarded");
        }
        DOMValidateContext valContext = new DOMValidateContext(publicKey, nl.item(0));
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        XMLSignature signature = fac.unmarshalXMLSignature(valContext);
        validFlag = signature.validate(valContext);
        return validFlag;
    }

    private static String convertDocumentToString(Document doc) {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();
            // below code to remove XML declaration
            // transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String output = writer.getBuffer().toString();
            return output;
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Document convertStringToDocument(String xmlStr) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setIgnoringComments(true);
        DocumentBuilder builder;
        try
        {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse( new InputSource( new StringReader( xmlStr ) ) );
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return null;
    }

    //message authentisity validate
    public static String validateAuthenticity(String signedMessage) throws UnrecoverableEntryException, CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException, ParserConfigurationException, TransformerException, SAXException, InvalidKeySpecException, InvalidKeyException, InvalidCanonicalizerException, CanonicalizationException {
        // canonicalize the document to a byte array and convert it to string
        Canonicalizer canon = Canonicalizer.getInstance(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        byte canonXmlBytes[] = canon.canonicalize(signedMessage.getBytes());
        String canonXmlString = new String(canonXmlBytes);

        // get instance of the message digest based on the SHA-256 hashing algorithm
        MessageDigest digest = MessageDigest.getInstance("SHA1");

        // call the digest method passing the byte stream on the text, this directly updates the message
        // being digested and perform the hashing
        byte[] hash = digest.digest(canonXmlString.getBytes(StandardCharsets.UTF_8));

        // encode the endresult byte hash
//        byte[] encodedBytes = Base64.encodeBase64(hash);

//        return new String(encodedBytes);

        return Base64.getEncoder().encodeToString(hash);
    }

    public static String removeXMLNode(String nodeName, String requestPayload) {
        // requestPayload = requestPayload.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>","");
        try {
            InputSource is = new InputSource(new StringReader(requestPayload));

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(is);

            //Node node = doc.getElementsByTagName(nodeName).item(0);

            Element element = (Element) doc.getElementsByTagName(nodeName).item(0);
            element.getParentNode().removeChild(element);
            doc.normalize();
            //doc.removeChild(node);
            // write the content to the xml file
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            DOMSource src = new DOMSource(doc);


            StringWriter writer = new StringWriter();
            StreamResult streamResult = new StreamResult(writer);

            //StreamResult res = new StreamResult(new File(file));
            transformer.transform(src, streamResult);
            String stringResult = new String(writer.toString().getBytes(StandardCharsets.UTF_8));

//            System.err.println(stringResult);
            return stringResult;
//            transformer.transform(src, res);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
