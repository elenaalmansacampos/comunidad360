package com.comunidad360.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBankAccountRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String bankName,
        @NotBlank @Size(min = 15, max = 34) String iban,
        @Size(max = 3) String currency
) {
}
