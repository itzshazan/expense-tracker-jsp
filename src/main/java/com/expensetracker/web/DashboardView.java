package com.expensetracker.web;

import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ReceiptStatus;
import com.expensetracker.service.BudgetService;
import com.expensetracker.settings.Formats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The dashboard's figures. Everything answers "am I on track this month?", so this month is
 * compared with the same point last month (day 1 up to today's day number) rather than with all
 * of last month, which would make every month look cheap until its last week.
 */
final class DashboardView {
    private static final int TOP_CATEGORIES = 5;

    private DashboardView() {
    }

    static Map<String, Object> build(List<Expense> expenses, List<Category> categories,
                                     List<BudgetService.Status> budgets, LocalDate today, Formats formats) {
        YearMonth month = YearMonth.from(today);
        YearMonth previous = month.minusMonths(1);
        int day = today.getDayOfMonth();
        int daysInMonth = month.lengthOfMonth();
        int previousCutoff = Math.min(day, previous.lengthOfMonth());
        Map<UUID, String> names = ViewData.names(categories);

        List<Expense> current = expenses.stream().filter(e -> YearMonth.from(e.expenseDate()).equals(month)).toList();
        List<Expense> last = expenses.stream().filter(e -> YearMonth.from(e.expenseDate()).equals(previous)).toList();
        List<Expense> lastToDate = last.stream().filter(e -> e.expenseDate().getDayOfMonth() <= previousCutoff).toList();

        BigDecimal spent = sum(current);
        BigDecimal spentLastToDate = sum(lastToDate);
        BigDecimal dailyAverage = spent.divide(BigDecimal.valueOf(day), 2, RoundingMode.HALF_UP);
        BigDecimal projected = dailyAverage.multiply(BigDecimal.valueOf(daysInMonth)).setScale(0, RoundingMode.HALF_UP);

        Map<String, Object> view = new HashMap<>();
        view.put("monthLabel", month.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                + " " + month.getYear());
        view.put("dayOfMonth", day);
        view.put("daysInMonth", daysInMonth);
        view.put("hasMonthSpending", !current.isEmpty());
        view.put("spent", formats.money(spent));
        view.put("change", change(spent, spentLastToDate, formats));
        view.put("dailyAverage", formats.money(dailyAverage));
        view.put("projected", formats.money(projected));
        view.put("lastMonthDailyAverage", formats.money(sum(last)
                .divide(BigDecimal.valueOf(previous.lengthOfMonth()), 2, RoundingMode.HALF_UP)));
        view.put("hasLastMonth", !last.isEmpty());

        BudgetService.Status overall = budgets.stream().filter(b -> b.categoryId() == null).findFirst().orElse(null);
        view.put("overall", overall == null ? null : overall(overall, projected, daysInMonth - day + 1, formats));
        view.put("budgets", budgetRows(budgets, formats));

        Expense largest = current.stream().max(Comparator.comparing(Expense::amount)).orElse(null);
        if (largest != null) {
            view.put("largest", Map.of(
                    "amount", formats.money(largest.amount()),
                    "merchant", largest.merchant(),
                    "date", formats.date(largest.expenseDate()),
                    "category", names.getOrDefault(largest.categoryId(), "Uncategorized")));
        }

        view.put("trend", trend(current, last, month, previous, day, formats));
        view.put("categories", categoryRows(current, lastToDate, spent, names, formats));

        long missing = current.stream().filter(e -> e.receiptStatus() == ReceiptStatus.NO_RECEIPT).count();
        view.put("missingReceipts", missing);
        view.put("monthStart", month.atDay(1).toString());
        view.put("monthEnd", month.atEndOfMonth().toString());
        return view;
    }

    /** "↑ 12%" style comparison of this month with the same point last month. */
    static Map<String, Object> change(BigDecimal now, BigDecimal before, Formats formats) {
        Map<String, Object> change = new HashMap<>();
        if (before.signum() == 0) {
            change.put("direction", "none");
            change.put("label", now.signum() == 0 ? "No spending yet" : "Nothing by this point last month");
            return change;
        }
        long percent = now.subtract(before).multiply(BigDecimal.valueOf(100))
                .divide(before, 0, RoundingMode.HALF_UP).longValue();
        change.put("direction", percent > 0 ? "up" : percent < 0 ? "down" : "same");
        change.put("label", (percent > 0 ? "↑ " + percent + "%" : percent < 0 ? "↓ " + Math.abs(percent) + "%" : "Same")
                + " vs. " + formats.money(before) + " by this point last month");
        return change;
    }

    private static Map<String, Object> overall(BudgetService.Status status, BigDecimal projected, int daysLeft,
                                               Formats formats) {
        BigDecimal left = status.limit().subtract(status.spent());
        Map<String, Object> overall = new HashMap<>();
        overall.put("limit", formats.money(status.limit()));
        overall.put("level", level(status));
        overall.put("over", left.signum() < 0);
        overall.put("left", formats.money(left.abs()));
        overall.put("daysLeft", daysLeft);
        overall.put("perDay", left.signum() > 0
                ? formats.money(left.divide(BigDecimal.valueOf(daysLeft), 0, RoundingMode.DOWN)) : null);
        BigDecimal projectedGap = projected.subtract(status.limit());
        overall.put("projectedOver", projectedGap.signum() > 0);
        overall.put("projectedGap", formats.money(projectedGap.abs()));
        return overall;
    }

    /** Budgets that need attention first, then the fullest. */
    private static List<Map<String, Object>> budgetRows(List<BudgetService.Status> budgets, Formats formats) {
        return budgets.stream()
                .sorted(Comparator.comparing((BudgetService.Status b) -> -b.level().ordinal())
                        .thenComparing(b -> -b.percent()))
                .map(budget -> {
                    BigDecimal left = budget.limit().subtract(budget.spent());
                    Map<String, Object> row = new HashMap<>();
                    row.put("label", budget.name());
                    row.put("level", level(budget));
                    row.put("message", switch (budget.level()) {
                        case OK -> "On track";
                        case WARNING -> "Near limit";
                        case EXCEEDED -> "Over limit";
                    });
                    row.put("spent", formats.money(budget.spent()));
                    row.put("limit", formats.money(budget.limit()));
                    row.put("percent", Math.round(budget.percent()));
                    row.put("progress", Math.min(100, Math.round(budget.percent())));
                    row.put("remaining", left.signum() < 0
                            ? "Over by " + formats.money(left.abs()) : formats.money(left) + " left");
                    return row;
                }).toList();
    }

    /**
     * Weekly totals (days 1–7, 8–14, …) for this month next to the same days last month.
     * Weeks that have not started yet are marked upcoming instead of showing zero.
     */
    static List<Map<String, Object>> trend(List<Expense> current, List<Expense> last, YearMonth month,
                                           YearMonth previous, int today, Formats formats) {
        int weeks = (month.lengthOfMonth() + 6) / 7;
        BigDecimal[] now = new BigDecimal[weeks];
        BigDecimal[] before = new BigDecimal[weeks];
        java.util.Arrays.fill(now, BigDecimal.ZERO);
        java.util.Arrays.fill(before, BigDecimal.ZERO);
        for (Expense expense : current) {
            int week = (expense.expenseDate().getDayOfMonth() - 1) / 7;
            now[week] = now[week].add(expense.amount());
        }
        for (Expense expense : last) {
            int week = (expense.expenseDate().getDayOfMonth() - 1) / 7;
            if (week < weeks) {
                before[week] = before[week].add(expense.amount());
            }
        }
        BigDecimal maximum = BigDecimal.ONE;
        for (int week = 0; week < weeks; week++) {
            maximum = maximum.max(now[week]).max(before[week]);
        }

        List<Map<String, Object>> rows = new ArrayList<>(weeks);
        for (int week = 0; week < weeks; week++) {
            int firstDay = week * 7 + 1;
            int lastDay = Math.min(firstDay + 6, month.lengthOfMonth());
            boolean upcoming = firstDay > today && now[week].signum() == 0;
            Map<String, Object> row = new HashMap<>();
            row.put("days", firstDay == lastDay ? Integer.toString(firstDay) : firstDay + "–" + lastDay);
            row.put("amount", formats.money(now[week]));
            row.put("value", now[week].toPlainString());
            row.put("previousAmount", formats.money(before[week]));
            row.put("previousValue", before[week].toPlainString());
            row.put("max", maximum.toPlainString());
            row.put("upcoming", upcoming);
            rows.add(row);
        }
        return rows;
    }

    /** This month's biggest categories, their share, and how they compare with the same point last month. */
    private static List<Map<String, Object>> categoryRows(List<Expense> current, List<Expense> lastToDate,
                                                          BigDecimal spent, Map<UUID, String> names,
                                                          Formats formats) {
        Map<UUID, BigDecimal> now = totals(current);
        Map<UUID, BigDecimal> before = totals(lastToDate);
        return now.entrySet().stream()
                .sorted(Map.Entry.<UUID, BigDecimal>comparingByValue().reversed())
                .limit(TOP_CATEGORIES)
                .map(entry -> {
                    BigDecimal amount = entry.getValue();
                    BigDecimal previous = before.getOrDefault(entry.getKey(), BigDecimal.ZERO);
                    BigDecimal difference = amount.subtract(previous);
                    long share = amount.multiply(BigDecimal.valueOf(100)).divide(spent, 0, RoundingMode.HALF_UP).longValue();
                    Map<String, Object> row = new HashMap<>();
                    row.put("label", names.getOrDefault(entry.getKey(), "Uncategorized"));
                    row.put("total", formats.money(amount));
                    row.put("share", share);
                    if (previous.signum() == 0) {
                        row.put("direction", "new");
                        row.put("change", "New this month");
                    } else if (difference.signum() == 0) {
                        row.put("direction", "same");
                        row.put("change", "Same as last month");
                    } else {
                        row.put("direction", difference.signum() > 0 ? "up" : "down");
                        row.put("change", (difference.signum() > 0 ? "+" : "−") + formats.money(difference.abs())
                                + " vs. last month");
                    }
                    return row;
                }).toList();
    }

    private static Map<UUID, BigDecimal> totals(List<Expense> expenses) {
        Map<UUID, BigDecimal> totals = new HashMap<>();
        for (Expense expense : expenses) {
            totals.merge(expense.categoryId(), expense.amount(), BigDecimal::add);
        }
        return totals;
    }

    private static BigDecimal sum(List<Expense> expenses) {
        return expenses.stream().map(Expense::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String level(BudgetService.Status status) {
        return status.level().name().toLowerCase(java.util.Locale.ROOT);
    }
}
