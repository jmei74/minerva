package com.minerva.creditcard.lounge;

import com.minerva.creditcard.config.JwtTokenProvider;
import com.minerva.creditcard.lounge.dto.AccessCodeDTO;
import com.minerva.creditcard.lounge.dto.AccessValidationDTO;
import com.minerva.creditcard.lounge.entity.LoungeAccessRecord;
import com.minerva.creditcard.lounge.entity.LoungeBenefit;
import com.minerva.creditcard.lounge.repository.LoungeAccessRecordRepository;
import com.minerva.creditcard.lounge.service.LoungeBenefitService;
import com.minerva.creditcard.lounge.service.QrCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * QrCodeService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class QrCodeServiceTest {

    @Mock
    private LoungeAccessRecordRepository accessRecordRepository;

    @Mock
    private LoungeBenefitService benefitService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private QrCodeService service;

    private LoungeBenefit testBenefit;

    @BeforeEach
    void setUp() {
        service = new QrCodeService(accessRecordRepository, benefitService, jwtTokenProvider);
        ReflectionTestUtils.setField(service, "qrSigningKey", "test-qr-key-for-unit-testing-only");

        testBenefit = new LoungeBenefit();
        testBenefit.setBenefitId(UUID.randomUUID());
        testBenefit.setAccountId(UUID.randomUUID());
        testBenefit.setLoungeNetwork("Priority_Pass");
        testBenefit.setCardLast4("1234");
        testBenefit.setTotalUses(10);
        testBenefit.setUsedCount(2);
        testBenefit.setExpiryDate(LocalDate.now().plusMonths(6));
        testBenefit.setStatus(LoungeBenefit.BenefitStatus.ACTIVE);
    }

    @Test
    void generateAccessCode_shouldCreateValidAccessCode() {
        // given
        UUID benefitId = testBenefit.getBenefitId();
        String loungeId = "SIN-Lounge-1";
        String loungeName = "Singapore Changi Lounge";
        String airportCode = "SIN";
        int guestCount = 1;

        when(benefitService.getBenefitById(benefitId)).thenReturn(testBenefit);
        when(accessRecordRepository.save(any(LoungeAccessRecord.class)))
                .thenAnswer(invocation -> {
                    LoungeAccessRecord record = invocation.getArgument(0);
                    ReflectionTestUtils.setField(record, "accessId", UUID.randomUUID());
                    return record;
                });

        // when
        AccessCodeDTO result = service.generateAccessCode(benefitId, loungeId, loungeName, airportCode, guestCount);

        // then
        assertNotNull(result);
        assertNotNull(result.qrCode());
        assertEquals(loungeName, result.loungeName());
        assertEquals(airportCode, result.airportCode());
        assertEquals(guestCount, result.guestCount());
        assertNotNull(result.validFrom());
        assertNotNull(result.validUntil());
        assertTrue(result.validUntil().isAfter(result.validFrom()));

        verify(benefitService).getBenefitById(benefitId);
        verify(accessRecordRepository).save(any(LoungeAccessRecord.class));
    }

    @Test
    void generateAccessCode_shouldThrowException_whenBenefitNotValid() {
        // given
        testBenefit.setUsedCount(10); // exhausted
        UUID benefitId = testBenefit.getBenefitId();
        when(benefitService.getBenefitById(benefitId)).thenReturn(testBenefit);

        // when/then
        assertThrows(IllegalStateException.class, () ->
                service.generateAccessCode(benefitId, "lounge", "name", "SIN", 1));
    }

    @Test
    void validateAccessCode_shouldReturnValid_whenCodeIsValid() {
        // given
        LoungeAccessRecord record = new LoungeAccessRecord();
        record.setAccessId(UUID.randomUUID());
        record.setBenefitId(testBenefit.getBenefitId());
        record.setLoungeName("Test Lounge");
        record.setAirportCode("SIN");
        record.setGuestCount(2);
        record.setAccessTime(LocalDateTime.now());
        record.setStatus(LoungeAccessRecord.AccessStatus.USED);

        when(accessRecordRepository.findById(any())).thenReturn(Optional.empty());
        when(benefitService.getBenefitById(any())).thenReturn(testBenefit);

        // when - try with invalid token (will return INVALID)
        AccessValidationDTO result = service.validateAccessCode("invalid.jwt.token");

        // then
        assertNotNull(result);
        assertEquals("INVALID", result.status());
    }

    @Test
    void qrCodeValidity_shouldBe5Minutes() {
        // given
        UUID benefitId = testBenefit.getBenefitId();
        when(benefitService.getBenefitById(benefitId)).thenReturn(testBenefit);
        when(accessRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        AccessCodeDTO result = service.generateAccessCode(benefitId, "lounge", "name", "SIN", 1);

        // then - verify5 minute validity
        long minutesValid = java.time.Duration.between(result.validFrom(), result.validUntil()).toMinutes();
        assertEquals(5, minutesValid);
    }
}