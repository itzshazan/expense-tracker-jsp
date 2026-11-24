package com.expensetracker.web;

import com.expensetracker.model.Category;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

final class Forms {
    private Forms() {
    }

    static String value(HttpServletRequest request, String field, int maximum) {
        String raw = request.getParameter(field);
        String value = raw == null ? "" : raw.strip();
        if (value.length() > maximum) {
            throw new IllegalArgumentException("A form value is too long.");
        }
        return value;
    }

    static String required(HttpServletRequest request, String field, int maximum, String label) {
        String value = value(request, field, maximum);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value;
    }

    static char[] password(HttpServletRequest request, String field, int minimum) {
        String password = request.getParameter(field);
        if (password == null || password.length() < minimum || password.length() > 1024) {
            throw new IllegalArgumentException(minimum == 1 ? "Enter your password."
                    : "Use a password with at least " + minimum + " characters.");
        }
        return password.toCharArray();
    }

    static UUID id(String value, String label) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Choose a valid " + label + ".");
        }
    }

    static Category category(String value, List<Category> categories) {
        UUID id = id(value, "category");
        return categories.stream().filter(category -> id.equals(category.id())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Choose a valid category."));
    }

    static BigDecimal amount(String value) {
        if (!value.matches("\\d{1,10}(\\.\\d{1,2})?")) {
            throw new IllegalArgumentException("Enter a positive amount with up to two decimal places.");
        }
        BigDecimal amount = new BigDecimal(value);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Enter an amount greater than zero.");
        }
        return amount;
    }

    static LocalDate date(String value) {
        if (value.isEmpty()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value);
            if (date.getYear() < 1 || date.getYear() > 9999) {
                throw new IllegalArgumentException("Enter a valid date.");
            }
            return date;
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Enter a valid date.");
        }
    }

    static void checkRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("The start date must be on or before the end date.");
        }
    }

    static <E extends Enum<E>> E choice(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Choose a value from the available options.");
        }
    }
}
