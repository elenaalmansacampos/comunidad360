package com.comunidad360.banking;

import com.comunidad360.common.exception.BusinessException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BankCsvParser {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    );

    public ParseResult parse(byte[] content, Mapping mapping) {
        String text = new String(content, StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        if (text.isBlank()) {
            throw BusinessException.unprocessable("BANK_IMPORT_EMPTY", "errors.bank.importEmpty");
        }
        char separator = resolveSeparator(mapping.separator());
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(separator)
                .setIgnoreEmptyLines(true)
                .setIgnoreSurroundingSpaces(true)
                .setTrim(true)
                .build();
        try (CSVParser parser = CSVParser.parse(new StringReader(text), format)) {
            Iterator<CSVRecord> iterator = parser.iterator();
            if (!iterator.hasNext()) {
                throw BusinessException.unprocessable("BANK_IMPORT_EMPTY", "errors.bank.importEmpty");
            }
            CSVRecord headerRecord = iterator.next();
            Map<String, Integer> headers = new HashMap<>();
            for (int index = 0; index < headerRecord.size(); index++) {
                headers.put(normalize(headerRecord.get(index)), index);
            }
            int dateIndex = requiredIndex(headers, mapping.dateColumn());
            int descriptionIndex = requiredIndex(headers, mapping.descriptionColumn());
            int amountIndex = requiredIndex(headers, mapping.amountColumn());
            int balanceIndex = optionalIndex(headers, mapping.balanceColumn());
            int externalIdIndex = optionalIndex(headers, mapping.externalIdColumn());

            List<ParsedRow> rows = new ArrayList<>();
            int total = 0;
            int errors = 0;
            while (iterator.hasNext()) {
                CSVRecord record = iterator.next();
                total++;
                try {
                    String description = value(record, descriptionIndex);
                    if (description.isBlank()) {
                        throw new IllegalArgumentException("Description is required");
                    }
                    LocalDate valueDate = parseDate(value(record, dateIndex), mapping.dateFormat());
                    BigDecimal amount = parseAmount(value(record, amountIndex));
                    BigDecimal balance = balanceIndex < 0 ? null : parseOptionalAmount(value(record, balanceIndex));
                    String externalId = externalIdIndex < 0 ? null : value(record, externalIdIndex);
                    rows.add(new ParsedRow(valueDate, description, amount, balance,
                            externalId == null || externalId.isBlank() ? null : externalId));
                } catch (RuntimeException exception) {
                    errors++;
                }
            }
            return new ParseResult(rows, total, errors);
        } catch (IOException exception) {
            throw new IllegalStateException("CSV parsing failed", exception);
        }
    }

    private char resolveSeparator(String value) {
        if (value == null || value.isBlank()) {
            return ',';
        }
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "comma", "coma" -> ',';
            case "semicolon", "punto y coma", "punt i coma" -> ';';
            case "tab", "tabulador" -> '\t';
            case "pipe" -> '|';
            default -> value.charAt(0);
        };
    }

    private int requiredIndex(Map<String, Integer> headers, String column) {
        int index = optionalIndex(headers, column);
        if (index < 0) {
            throw BusinessException.unprocessable("CSV_MAPPING_INVALID", "errors.bank.mappingInvalid");
        }
        return index;
    }

    private int optionalIndex(Map<String, Integer> headers, String column) {
        if (column == null || column.isBlank()) {
            return -1;
        }
        return headers.getOrDefault(normalize(column), -1);
    }

    private String value(CSVRecord record, int index) {
        if (index < 0 || index >= record.size()) {
            return "";
        }
        String value = record.get(index);
        return value == null ? "" : value.strip();
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.replace("\uFEFF", "").strip().toLowerCase(Locale.ROOT);
    }

    private LocalDate parseDate(String value, String format) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("Date is required");
        }
        if (format != null && !format.isBlank()) {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern(format));
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format.
            }
        }
        throw new IllegalArgumentException("Unsupported date: " + value);
    }

    private BigDecimal parseOptionalAmount(String value) {
        return value.isBlank() ? null : parseAmount(value);
    }

    private BigDecimal parseAmount(String value) {
        String normalized = value.replace("\u00A0", "")
                .replace(" ", "")
                .replaceAll("[^0-9,.'\\-]", "")
                .replace("'", "");
        if (normalized.contains(",") && normalized.contains(".")) {
            if (normalized.lastIndexOf(',') > normalized.lastIndexOf('.')) {
                normalized = normalized.replace(".", "").replace(',', '.');
            } else {
                normalized = normalized.replace(",", "");
            }
        } else if (normalized.contains(",")) {
            normalized = normalized.replace(',', '.');
        }
        return new BigDecimal(normalized);
    }

    public record Mapping(
            String dateColumn,
            String descriptionColumn,
            String amountColumn,
            String balanceColumn,
            String externalIdColumn,
            String separator,
            String dateFormat
    ) {
    }

    public record ParsedRow(
            LocalDate valueDate,
            String description,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String externalId
    ) {
    }

    public record ParseResult(List<ParsedRow> rows, int totalRows, int errorCount) {
    }
}
