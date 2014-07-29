/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.rs.security.oauth2.jwe;

import java.nio.ByteBuffer;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.apache.cxf.rs.security.oauth2.jws.JwsCompactReaderWriterTest;
import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.HmacUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.KeyProperties;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JweCompactReaderWriterTest extends Assert {
    private static final byte[] CONTENT_ENCRYPTION_KEY = {
        (byte)177, (byte)161, (byte)244, (byte)128, 84, (byte)143, (byte)225,
        115, 63, (byte)180, 3, (byte)255, 107, (byte)154, (byte)212, (byte)246,
        (byte)138, 7, 110, 91, 112, 46, 34, 105, 47, 
        (byte)130, (byte)203, 46, 122, (byte)234, 64, (byte)252};
    
    private static final byte[] CONTENT_ENCRYPTION_KEY_A3 = {
        4, (byte)211, 31, (byte)197, 84, (byte)157, (byte)252, (byte)254, 11, 100, 
        (byte)157, (byte)250, 63, (byte)170, 106, (byte)206, 107, 124, (byte)212, 
        45, 111, 107, 9, (byte)219, (byte)200, (byte)177, 0, (byte)240, (byte)143, 
        (byte)156, 44, (byte)207};
    private static final byte[] INIT_VECTOR_A3 = {
        3, 22, 60, 12, 43, 67, 104, 105, 108, 108, 105, 99, 111, 116, 104, 101};
    private static final String KEY_ENCRYPTION_KEY_A3 = "GawgguFyGrWKav7AX4VKUg";
    private static final String JWE_OUTPUT_A3 = 
        "eyJhbGciOiJBMTI4S1ciLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0" 
        + ".6KB707dM9YTIgHtLvtgWQ8mKwboJW3of9locizkDTHzBC2IlrT1oOQ" 
        + ".AxY8DCtDaGlsbGljb3RoZQ" 
        + ".KDlTtXchhZTGufMYmOYGS4HffxPSUrfmqCHXaI9wOGY" 
        + ".U0m_YmjN04DJvceFICbCVQ";
    private static final String RSA_MODULUS_ENCODED = "oahUIoWw0K0usKNuOR6H4wkf4oBUXHTxRvgb48E-BVvxkeDNjbC4he8rUW"
           + "cJoZmds2h7M70imEVhRU5djINXtqllXI4DFqcI1DgjT9LewND8MW2Krf3S"
           + "psk_ZkoFnilakGygTwpZ3uesH-PFABNIUYpOiN15dsQRkgr0vEhxN92i2a"
           + "sbOenSZeyaxziK72UwxrrKoExv6kc5twXTq4h-QChLOln0_mtUZwfsRaMS"
           + "tPs6mS6XrgxnxbWhojf663tuEQueGC-FCMfra36C9knDFGzKsNa7LZK2dj"
           + "YgyD3JR_MB_4NUJW_TqOQtwHYbxevoJArm-L5StowjzGy-_bq6Gw";
    private static final String RSA_PUBLIC_EXPONENT_ENCODED = "AQAB";
    private static final String RSA_PRIVATE_EXPONENT_ENCODED = 
        "kLdtIj6GbDks_ApCSTYQtelcNttlKiOyPzMrXHeI-yk1F7-kpDxY4-WY5N"
        + "WV5KntaEeXS1j82E375xxhWMHXyvjYecPT9fpwR_M9gV8n9Hrh2anTpTD9"
        + "3Dt62ypW3yDsJzBnTnrYu1iwWRgBKrEYY46qAZIrA2xAwnm2X7uGR1hghk"
        + "qDp0Vqj3kbSCz1XyfCs6_LehBwtxHIyh8Ripy40p24moOAbgxVw3rxT_vl"
        + "t3UVe4WO3JkJOzlpUf-KTVI2Ptgm-dARxTEtE-id-4OJr0h-K-VFs3VSnd"
        + "VTIznSxfyrj8ILL6MG_Uv8YAu7VILSB3lOW085-4qE3DzgrTjgyQ";
    
    private static final byte[] INIT_VECTOR = {(byte)227, (byte)197, 117, (byte)252, 2, (byte)219, 
        (byte)233, 68, (byte)180, (byte)225, 77, (byte)219};
     
    @BeforeClass
    public static void registerBouncyCastleIfNeeded() throws Exception {
        try {
            // Java 8 apparently has it
            Cipher.getInstance(Algorithm.AES_GCM_ALGO_JAVA);
        } catch (Throwable t) {
            // Oracle Java 7
            Security.addProvider(new BouncyCastleProvider());    
        }
    }
    @AfterClass
    public static void unregisterBouncyCastleIfNeeded() throws Exception {
        Security.removeProvider(BouncyCastleProvider.class.getName());    
    }
    
    @Test
    public void testEncryptDecryptA128CBCHS256() throws Exception {
        final String specPlainText = "Live long and prosper.";
        byte[] macKey = new byte[16];
        System.arraycopy(CONTENT_ENCRYPTION_KEY_A3, 0, macKey, 0, 16);
        byte[] encKey = new byte[16];
        System.arraycopy(CONTENT_ENCRYPTION_KEY_A3, 16, encKey, 0, 16);
        SecretKey secretEncKey = 
            CryptoUtils.createSecretKeySpec(encKey, Algorithm.A128CBC_HS256.getJavaAlgoName());
        KeyProperties keyProps = new KeyProperties(Algorithm.AES_CBC_ALGO_JAVA);
        keyProps.setAlgoSpec(new IvParameterSpec(INIT_VECTOR_A3));
        byte[] cipher = CryptoUtils.encryptBytes(specPlainText.getBytes("UTF-8"), secretEncKey, keyProps);
        
        JweHeaders headers = new JweHeaders();
        headers.setAlgorithm(Algorithm.A128KW.getJwtName());
        headers.setContentEncryptionAlgorithm(Algorithm.A128CBC_HS256.getJwtName());
        
        AesWrapKeyEncryption keyEncryption = new AesWrapKeyEncryption(Base64UrlUtility.decode(KEY_ENCRYPTION_KEY_A3), 
                                                                      Algorithm.A128KW.getJwtName());
        byte[] encryptedCek = keyEncryption.getEncryptedContentEncryptionKey(headers, CONTENT_ENCRYPTION_KEY_A3);
        
        byte[] aad = headers.toCipherAdditionalAuthData();
        ByteBuffer buf = ByteBuffer.allocate(8);
        byte[] al = buf.putInt(0).putInt(aad.length * 8).array();
        
        Mac mac = HmacUtils.getInitializedMac(macKey, Algorithm.HMAC_SHA_256_JAVA, null);
        mac.update(aad);
        mac.update(INIT_VECTOR_A3);
        mac.update(cipher);
        mac.update(al);
        byte[] sig = mac.doFinal();
        assertEquals(32, sig.length);
        byte[] authTag = new byte[16];
        System.arraycopy(sig, 0, authTag, 0, 16);
        
        JweCompactProducer p = new JweCompactProducer(headers,
                                                      encryptedCek,
                                                      INIT_VECTOR_A3,
                                                      cipher,
                                                      authTag);
        assertEquals(JWE_OUTPUT_A3, p.getJweContent());
    }
    
    @Test
    public void testEncryptDecryptSpecExample() throws Exception {
        final String specPlainText = "The true sign of intelligence is not knowledge but imagination.";
        String jweContent = encryptContent(specPlainText, true);
        
        decrypt(jweContent, specPlainText, true);
    }
    
    @Test
    public void testDirectKeyEncryptDecrypt() throws Exception {
        final String specPlainText = "The true sign of intelligence is not knowledge but imagination.";
        SecretKey key = createSecretKey(true);
        String jweContent = encryptContentDirect(key, specPlainText);
        
        decryptDirect(key, jweContent, specPlainText);
    }
    
    @Test
    public void testEncryptDecryptJwsToken() throws Exception {
        String jweContent = encryptContent(JwsCompactReaderWriterTest.ENCODED_TOKEN_SIGNED_BY_MAC, false);
        decrypt(jweContent, JwsCompactReaderWriterTest.ENCODED_TOKEN_SIGNED_BY_MAC, false);
    }
    
    private String encryptContent(String content, boolean createIfException) throws Exception {
        RSAPublicKey publicKey = CryptoUtils.getRSAPublicKey(RSA_MODULUS_ENCODED, RSA_PUBLIC_EXPONENT_ENCODED);
        SecretKey key = createSecretKey(createIfException);
        String jwtKeyName = null;
        if (key == null) {
            // the encryptor will generate it
            jwtKeyName = Algorithm.A128GCM.getJwtName();
        } else {
            jwtKeyName = Algorithm.toJwtName(key.getAlgorithm(), key.getEncoded().length * 8);
        }
        RSAJweEncryption encryptor = new RSAJweEncryption(publicKey, 
                                                          Algorithm.RSA_OAEP.getJwtName(),
                                                        key, 
                                                        jwtKeyName, 
                                                        INIT_VECTOR);
        return encryptor.encrypt(content.getBytes("UTF-8"), null);
    }
    private String encryptContentDirect(SecretKey key, String content) throws Exception {
        DirectKeyJweEncryption encryptor = new DirectKeyJweEncryption(key, INIT_VECTOR);
        return encryptor.encrypt(content.getBytes("UTF-8"), null);
    }
    private void decrypt(String jweContent, String plainContent, boolean unwrap) throws Exception {
        RSAPrivateKey privateKey = CryptoUtils.getRSAPrivateKey(RSA_MODULUS_ENCODED, RSA_PRIVATE_EXPONENT_ENCODED);
        RSAJweDecryption decryptor = new RSAJweDecryption(privateKey, unwrap);
        String decryptedText = decryptor.decrypt(jweContent).getContentText();
        assertEquals(decryptedText, plainContent);
    }
    private void decryptDirect(SecretKey key, String jweContent, String plainContent) throws Exception {
        DirectKeyJweDecryption decryptor = new DirectKeyJweDecryption(key);
        String decryptedText = decryptor.decrypt(jweContent).getContentText();
        assertEquals(decryptedText, plainContent);
    }
    private SecretKey createSecretKey(boolean createIfException) throws Exception {
        SecretKey key = null;
        if (Cipher.getMaxAllowedKeyLength("AES") > 128) { 
            key = CryptoUtils.createSecretKeySpec(CONTENT_ENCRYPTION_KEY, "AES");
        } else if (createIfException) {
            key = CryptoUtils.createSecretKeySpec(CryptoUtils.generateSecureRandomBytes(128 / 8), "AES");
        }
        return key;
    }
}

