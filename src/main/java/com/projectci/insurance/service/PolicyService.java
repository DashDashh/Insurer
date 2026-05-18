package com.projectci.insurance.service;

import com.projectci.insurance.config.InsuranceProperties;
import com.projectci.insurance.model.Policy;
import com.projectci.insurance.model.analytics.CalcResponse;
import com.projectci.insurance.repository.PolicyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final InsuranceProperties properties;

    public Policy createPolicy(CalcResponse response, Policy.PolicyType type, Policy.PolicyStatus status, BigDecimal droneKbm) {
        Policy policy = new Policy();
        policy.setPolicyNumber("POL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        policy.setOrderId(response.getOrderId());
        policy.setManufacturerId(response.getManufacturerId());
        policy.setOperatorId(response.getOperatorId());
        policy.setDroneId(response.getDroneId());


        switch (type) {
            case mission:
                policy.setStartDate(LocalDateTime.now());
                policy.setEndDate(LocalDateTime.now().plusDays(properties.getPolicyDurationDays()));
            case annual:
                policy.setStartDate(LocalDateTime.now());
                policy.setEndDate(LocalDateTime.now().plusYears(1L));
        }
        // стоимость от аналитики
        policy.setCost(response.getCalculatedCost());
        policy.setCoverageAmount(response.getCoverageAmount());

        policy.setStatus(status);
        policy.setPolicyType(type);

        policy.setDroneKbm(droneKbm);

        return policyRepository.save(policy);
    }

    public boolean terminatePolicyByOrderId(String orderId) {
        Optional<Policy> policyOpt = policyRepository.findByOrderId(orderId);

        if (policyOpt.isPresent()) {
            Policy policy = policyOpt.get();
            policy.setStatus(Policy.PolicyStatus.terminated);
            policy.setEndDate(LocalDateTime.now());
            policyRepository.save(policy);
            log.info("Policy {} terminated for order {}", policy.getId(), orderId);
            return true;
        }

        log.warn("Policy not found for order {}", orderId);
        return false;
    }

    public Optional<Policy> getActivePolicyForOrder(String orderId) {
        return policyRepository.findByOrderId(orderId)
                .filter(p -> p.getStatus() == Policy.PolicyStatus.active);
    }

    public Optional<Policy> getCalculatedPolicyForOrder(String orderId) {
        return policyRepository.findByOrderId(orderId)
                .filter(p -> p.getStatus() == Policy.PolicyStatus.calculated);
    }

    @Transactional
    public void updatePolicyStatus(String policyNumber, Policy.PolicyStatus newStatus) {
        policyRepository.updatePolicyStatusByPolicyNumber(policyNumber, newStatus);
    }
}
