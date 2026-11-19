package com.comunidad360.finance;

import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.property.Unit;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AllocationService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal ONE_CENT = new BigDecimal("1");

    public List<Allocation> allocate(BigDecimal totalAmount, List<Unit> units,
                                     AllocationMethod method, BigDecimal tolerance) {
        if (totalAmount == null || totalAmount.setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.unprocessable(
                    "ASSESSMENT_AMOUNT_INVALID", "errors.assessment.amountInvalid");
        }
        if (units.isEmpty()) {
            throw BusinessException.unprocessable(
                    "NO_ACTIVE_UNITS", "errors.assessment.noActiveUnits");
        }
        if (method == AllocationMethod.CUSTOM) {
            throw BusinessException.unprocessable(
                    "ALLOCATION_METHOD_UNSUPPORTED", "errors.assessment.methodUnsupported");
        }
        if (method == AllocationMethod.COEFFICIENT) {
            BigDecimal coefficientSum = units.stream()
                    .map(Unit::getCoefficient)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (coefficientSum.subtract(ONE_HUNDRED).abs().compareTo(tolerance) > 0) {
                throw BusinessException.unprocessable(
                        "COEFFICIENT_SUM_INVALID", "errors.assessment.coefficientSum");
            }
        }

        BigDecimal total = totalAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCents = total.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY);
        List<Candidate> candidates = new ArrayList<>();
        for (Unit unit : units) {
            if (method == AllocationMethod.COEFFICIENT
                    && unit.getCoefficient().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal rawCents = method == AllocationMethod.COEFFICIENT
                    ? totalCents.multiply(unit.getCoefficient()).divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP)
                    : totalCents.divide(BigDecimal.valueOf(units.size()), 10, RoundingMode.HALF_UP);
            BigDecimal floor = rawCents.setScale(0, RoundingMode.FLOOR);
            candidates.add(new Candidate(unit, floor, rawCents.subtract(floor)));
        }
        if (candidates.isEmpty()) {
            throw BusinessException.unprocessable(
                    "NO_ACTIVE_UNITS", "errors.assessment.noActiveUnits");
        }

        BigDecimal assigned = candidates.stream()
                .map(Candidate::cents)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int remainingCents = totalCents.subtract(assigned).intValueExact();

        List<Candidate> ordered = new ArrayList<>(candidates);
        ordered.sort(Comparator
                .comparing(Candidate::remainder, Comparator.reverseOrder())
                .thenComparing(candidate -> candidate.unit().getCode()));

        List<Allocation> result = new ArrayList<>();
        for (int index = 0; index < ordered.size(); index++) {
            Candidate candidate = ordered.get(index);
            BigDecimal cents = candidate.cents();
            if (index < remainingCents) {
                cents = cents.add(ONE_CENT);
            }
            BigDecimal amount = cents.movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY);
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                result.add(new Allocation(
                        candidate.unit().getId(),
                        candidate.unit().getCode(),
                        candidate.unit().getCoefficient(),
                        amount
                ));
            }
        }
        return result;
    }

    private record Candidate(Unit unit, BigDecimal cents, BigDecimal remainder) {
    }

    public record Allocation(UUID unitId, String unitCode, BigDecimal coefficientSnapshot, BigDecimal amount) {
    }
}
