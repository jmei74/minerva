package com.minerva.creditcard.lounge;

import com.minerva.creditcard.lounge.dto.BenefitDashboardDTO;
import com.minerva.creditcard.lounge.dto.BenefitOverviewDTO;
import com.minerva.creditcard.lounge.entity.LoungeBenefit;
import com.minerva.creditcard.lounge.repository.LoungeBenefitRepository;
import com.minerva.creditcard.lounge.repository.LoungeAccessRecordRepository;
import com.minerva.creditcard.lounge.service.LoungeBenefitService;
import com.minerva.creditcard.lounge.service.QrCodeService;
import com.minerva.creditcard.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * LoungeBenefitService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class LoungeBenefitServiceTest {

    @Mock
    private LoungeBenefitRepository benefitRepository;

    @Mock
    private LoungeAccessRecordRepository accessRecordRepository;

    @Mock
    private QrCodeService qrCodeService;

    private LoungeBenefitService service;

    private UUID accountId;
    private LoungeBenefit testBenefit;

    @BeforeEach
    void setUp() {
        service = new LoungeBenefitService(benefitRepository, accessRecordRepository, qrCodeService);

        accountId = UUID.randomUUID();

        testBenefit = new LoungeBenefit();
        testBenefit.setBenefitId(UUID.randomUUID());
        testBenefit.setAccountId(accountId);
        testBenefit.setLoungeNetwork("Priority_Pass");
        testBenefit.setCardLast4("1234");
        testBenefit.setTotalUses(10);
        testBenefit.setUsedCount(2);
        testBenefit.setExpiryDate(LocalDate.now().plusMonths(6));
        testBenefit.setStatus(LoungeBenefit.BenefitStatus.ACTIVE);
    }

    @Test
    void getBenefitDashboard_shouldReturnCorrectDashboard() {
        // given
        when(benefitRepository.findActiveByAccountId(accountId))
                .thenReturn(List.of(testBenefit));
        when(benefitRepository.findExpiringSoon(eq(accountId), any(java.time.LocalDate.class)))
                .thenReturn(List.of());

        // when
        BenefitDashboardDTO dashboard = service.getBenefitDashboard(accountId);

        // then
        assertNotNull(dashboard);
        assertEquals(accountId, dashboard.accountId());
        assertEquals(1, dashboard.totalLounges());
        assertEquals(8, dashboard.totalRemainingVisits()); // 10 - 2 = 8
        assertEquals(1, dashboard.benefits().size());
    }

    @Test
    void getBenefitById_shouldReturnBenefit_whenExists() {
        // given
        UUID benefitId = testBenefit.getBenefitId();
        when(benefitRepository.findById(benefitId))
                .thenReturn(Optional.of(testBenefit));

        // when
        LoungeBenefit result = service.getBenefitById(benefitId);

        // then
        assertNotNull(result);
        assertEquals(benefitId, result.getBenefitId());
        assertEquals("Priority_Pass", result.getLoungeNetwork());
    }

    @Test
    void getBenefitById_shouldThrowException_whenNotFound() {
        // given
        UUID benefitId = UUID.randomUUID();
        when(benefitRepository.findById(benefitId))
                .thenReturn(Optional.empty());

        // when/then
        assertThrows(ResourceNotFoundException.class, () -> service.getBenefitById(benefitId));
    }

    @Test
    void useBenefit_shouldIncrementUsedCount() {
        // given
        UUID benefitId = testBenefit.getBenefitId();
        when(benefitRepository.findById(benefitId))
                .thenReturn(Optional.of(testBenefit));

        // when
        service.useBenefit(benefitId);

        // then
        assertEquals(3, testBenefit.getUsedCount()); // 2 -> 3
        verify(benefitRepository).save(testBenefit);
    }

    @Test
    void useBenefit_shouldThrowException_whenBenefitNotValid() {
        // given
        testBenefit.setUsedCount(10); // exhausted
        UUID benefitId = testBenefit.getBenefitId();
        when(benefitRepository.findById(benefitId))
                .thenReturn(Optional.of(testBenefit));

        // when/then
        assertThrows(IllegalStateException.class, () -> service.useBenefit(benefitId));
    }

    @Test
    void benefitOverviewDTO_shouldCalculateRemainingUses() {
        // given
        testBenefit.setTotalUses(10);
        testBenefit.setUsedCount(3);

        // when
        BenefitOverviewDTO dto = BenefitOverviewDTO.fromEntity(testBenefit);

        // then
        assertEquals(7, dto.remainingUses());
        assertEquals(3, dto.usedCount());
        assertEquals(10, dto.totalUses());
    }

    @Test
    void benefitOverviewDTO_shouldDetectExpiringSoon() {
        // given - expiry in 5 days
        testBenefit.setExpiryDate(LocalDate.now().plusDays(5));

        // when
        BenefitOverviewDTO dto = BenefitOverviewDTO.fromEntity(testBenefit);

        // then
        assertTrue(dto.isExpiringSoon());
    }

    @Test
    void loungeBenefit_isValid_shouldReturnTrue_whenActiveAndNotExpired() {
        // given
        testBenefit.setStatus(LoungeBenefit.BenefitStatus.ACTIVE);
        testBenefit.setExpiryDate(LocalDate.now().plusMonths(1));
        testBenefit.setUsedCount(5);
        testBenefit.setTotalUses(10);

        // when/then
        assertTrue(testBenefit.isValid());
    }

    @Test
    void loungeBenefit_isValid_shouldReturnFalse_whenExpired() {
        // given
        testBenefit.setExpiryDate(LocalDate.now().minusDays(1));

        // when/then
        assertFalse(testBenefit.isValid());
    }

    @Test
    void loungeBenefit_isValid_shouldReturnFalse_whenExhausted() {
        // given
        testBenefit.setUsedCount(10);
        testBenefit.setTotalUses(10);

        // when/then
        assertFalse(testBenefit.isValid());
    }
}