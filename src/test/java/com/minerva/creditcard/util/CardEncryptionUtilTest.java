package com.minerva.creditcard.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 卡号加密工具单元测试
 * 架构约束: §11 PCI-DSS - AES-256卡号加密
 */
class CardEncryptionUtilTest {

    private final CardEncryptionUtil util =
            new CardEncryptionUtil("minerva-credit-card-key-32bytes!".getBytes());

    @Test
    @DisplayName("加密后能正确解密")
    void testEncryptDecrypt() {
        String plain = "6288888888881234";
        String encrypted = util.encrypt(plain);

        assertNotEquals(plain, encrypted);
        assertTrue(encrypted.startsWith("GCM:") || encrypted.length() > 40);

        String decrypted = util.decrypt(encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    @DisplayName("掩码：13-19位卡号正确掩码")
    void testMask() {
        assertEquals("**** **** **** 1234", CardEncryptionUtil.mask("6288888888881234"));
        assertEquals("**** **** **** 5678", CardEncryptionUtil.mask("1234567890125678"));
        assertEquals("**** **** **** 9012", CardEncryptionUtil.mask("1234567890129012"));
    }

    @Test
    @DisplayName("掩码：短卡号返回 ****")
    void testMaskShort() {
        assertEquals("****", CardEncryptionUtil.mask("123"));
        assertEquals("****", CardEncryptionUtil.mask(null));
        assertEquals("****", CardEncryptionUtil.mask(""));
    }

    @Test
    @DisplayName("不同明文加密结果不同（随机IV）")
    void testRandomIV() {
        String plain = "6288888888881234";
        String enc1 = util.encrypt(plain);
        String enc2 = util.encrypt(plain);

        // GCM with random IV should produce different ciphertexts
        assertNotEquals(enc1, enc2);

        // But both should decrypt to the same plaintext
        assertEquals(plain, util.decrypt(enc1));
        assertEquals(plain, util.decrypt(enc2));
    }
}
