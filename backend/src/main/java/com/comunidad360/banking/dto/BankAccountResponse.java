package com.comunidad360.banking.dto;

import com.comunidad360.banking.BankAccount;
import com.comunidad360.banking.BankAccountSource;
import com.comunidad360.banking.BankAccountStatus;

import java.util.UUID;

public record BankAccountResponse(
        UUID id,
        String name,
        String bankName,
        String ibanLast4,
        String ibanMasked,
        String currency,
        BankAccountStatus status,
        BankAccountSource source,
        long version
) {
    public static BankAccountResponse from(BankAccount account) {
        return new BankAccountResponse(
                account.getId(),
                account.getName(),
                account.getBankName(),
                account.getIbanLast4(),
                account.getIbanMasked(),
                account.getCurrency(),
                account.getStatus(),
                account.getSource(),
                account.getVersion()
        );
    }
}
