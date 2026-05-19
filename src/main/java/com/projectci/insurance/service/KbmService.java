package com.projectci.insurance.service;

import com.projectci.insurance.config.InsuranceProperties;
import com.projectci.insurance.model.InsuranceRequest;
import com.projectci.insurance.model.KbmCalculation;
import com.projectci.insurance.repository.KbmCalculationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KbmService {

    private final InsuranceProperties properties;
    private final KbmCalculationRepository kbmRepository;

    public BigDecimal getManufacturerKbm(String manufacturerId) {
        Optional<KbmCalculation> lastCalc = kbmRepository.findFirstByEntityIdAndEntityTypeOrderByCalculationDateDesc(manufacturerId, "MANUFACTURER");
        return lastCalc.map(KbmCalculation::getNewKbm).orElse(properties.getBaseKbm());
    }

    public List<KbmCalculation> getManufacturerHistory(String manufacturerId) {
        return kbmRepository.findByEntityIdAndEntityTypeAndCalculationDateBetween(
                manufacturerId,
                "MANUFACTURER",
                LocalDateTime.now(),
                LocalDateTime.now().plusYears(1L)
        );
    }

    public BigDecimal getOperatorKbm(String operatorId) {
        Optional<KbmCalculation> lastCalc = kbmRepository.findFirstByEntityIdAndEntityTypeOrderByCalculationDateDesc(operatorId, "OPERATOR");
        return lastCalc.map(KbmCalculation::getNewKbm).orElse(properties.getBaseKbm());
    }

    public List<KbmCalculation> getOperatorHistory(String operatorId) {
        return kbmRepository.findByEntityIdAndEntityTypeAndCalculationDateBetween(
                operatorId,
                "OPERATOR",
                LocalDateTime.now(),
                LocalDateTime.now().plusYears(1L)
        );
    }

    public BigDecimal getDroneKbm(String droneId) {
        Optional<KbmCalculation> lastCalc = kbmRepository.findFirstByEntityIdAndEntityTypeOrderByCalculationDateDesc(droneId, "DRONE");
        return lastCalc.map(KbmCalculation::getNewKbm).orElse(properties.getBaseKbm());
    }

    // заменен сервисом аналитики
    public BigDecimal calculatePolicyCost(InsuranceRequest request) {
        // Заглушка: простая формула стоимости
        BigDecimal baseCost = properties.getBaseCost();
        BigDecimal manufacturerKbm = getManufacturerKbm(request.getManufacturerId());
        BigDecimal operatorKbm = getOperatorKbm(request.getOperatorId());

        // Стоимость = базовая * КБМ_производителя * КБМ_оператора
        return baseCost
                .multiply(manufacturerKbm)
                .multiply(operatorKbm)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public void recalculateKbm(String entityId, String entityType, BigDecimal newKbm, String incidentId, int incidentsCount) {
        log.info("Recalculating KBM for {}: {}", entityType, entityId);

        BigDecimal currentKbm = entityType.equals("MANUFACTURER")
                ? getManufacturerKbm(entityId)
                : getOperatorKbm(entityId);

        // ОФ5: Пересчёт КБМ с учётом инцидентов (перерассчёт подаётся на входе. получен от аналитики)

        // Сохраняем результат
        KbmCalculation calculation = new KbmCalculation();
        calculation.setEntityId(entityId);
        calculation.setEntityType(entityType);
        calculation.setCurrentKbm(currentKbm);
        calculation.setNewKbm(newKbm);
        calculation.setIncidentCount(incidentsCount);
        calculation.setCalculationDate(LocalDateTime.now());
        calculation.setRelatedIncidentId(incidentId);

        KbmCalculation saved = kbmRepository.save(calculation);

        log.info("KBM updated from {} to {}", currentKbm, newKbm);

    }
}
