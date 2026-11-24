package com.expensetracker.web;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.ReceiptStatus;
import com.expensetracker.service.ReportService;
import com.expensetracker.settings.Formats;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

final class ViewData {
    private ViewData() {
    }

    static List<Map<String, String>> categories(List<Category> categories, boolean useNameForValue) {
        return categories.stream().map(category -> Map.of("value",
                useNameForValue ? category.name() : category.id().toString(), "label", category.name())).toList();
    }

    static List<Map<String, String>> payments() {
        return options(PaymentMethod.values(), PaymentMethod::displayName);
    }

    static <E extends Enum<E>> List<Map<String, String>> options(E[] items, Function<E, String> label) {
        return Arrays.stream(items).map(item -> Map.of("value", item.name(), "label", label.apply(item))).toList();
    }

    static Map<UUID, String> names(List<Category> categories) {
        return categories.stream().collect(Collectors.toMap(Category::id, Category::name));
    }

    static List<Map<String, Object>> expenses(List<Expense> expenses, List<Category> categories, Formats formats) {
        Map<UUID, String> names = names(categories);
        return expenses.stream().map(expense -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", expense.id().toString());
            row.put("merchant", expense.merchant());
            row.put("description", expense.description());
            row.put("category", names.getOrDefault(expense.categoryId(), "Uncategorized"));
            row.put("amount", formats.money(expense.amount()));
            row.put("date", formats.date(expense.expenseDate()));
            row.put("payment", expense.paymentMethod().displayName());
            row.put("receipt", expense.receiptPath() != null && !expense.receiptPath().isBlank());
            row.put("receiptStatus", expense.receiptStatus().displayName());
            row.put("failed", expense.receiptStatus() == ReceiptStatus.UPLOAD_FAILED);
            return row;
        }).toList();
    }

    static List<Map<String, String>> budgets(List<Budget> budgets, List<Category> categories, Formats formats) {
        Map<UUID, String> names = names(categories);
        return budgets.stream().map(budget -> Map.of("id", budget.id().toString(),
                "label", budget.categoryId() == null ? "Overall" : names.getOrDefault(budget.categoryId(), "Uncategorized"),
                "amount", formats.money(budget.monthlyLimit()))).toList();
    }

    static List<Map<String, Object>> categories(ReportService.DetailedReport report, Formats formats) {
        return report.categoryBreakdowns().stream().map(row ->
                breakdown(row.categoryName(), row.count(), row.totalAmount(), row.percentage(), formats)).toList();
    }

    static List<Map<String, Object>> payments(ReportService.DetailedReport report, Formats formats) {
        return report.paymentBreakdowns().stream().map(row ->
                breakdown(row.paymentMethod().displayName(), row.count(), row.totalAmount(), row.percentage(), formats)).toList();
    }

    private static Map<String, Object> breakdown(String label, int count, BigDecimal total, double share, Formats formats) {
        return Map.of("label", label, "count", count, "total", formats.money(total), "share", Math.round(share));
    }
}
