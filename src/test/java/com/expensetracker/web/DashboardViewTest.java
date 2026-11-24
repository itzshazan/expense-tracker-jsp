package com.expensetracker.web;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.PaymentMethod;
import com.expensetracker.model.ReceiptStatus;
import com.expensetracker.service.BudgetService;
import com.expensetracker.settings.Formats;
import com.expensetracker.settings.UserSettings;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardViewTest {
    private static final UUID USER = UUID.randomUUID();
    private static final Category FOOD = new Category(UUID.randomUUID(), "Food");
    private static final Category TRAVEL = new Category(UUID.randomUUID(), "Travel");
    private static final List<Category> CATEGORIES = List.of(FOOD, TRAVEL);
    private static final Formats FORMATS = new Formats(UserSettings.defaults());
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    // November so far: 500. October: 250 by the 10th, 1,250 in total.
    private static final List<Expense> EXPENSES = List.of(
            expense(FOOD, "Corner Café", "300", LocalDate.of(2026, 11, 3), ReceiptStatus.NO_RECEIPT),
            expense(TRAVEL, "Metro card", "200", LocalDate.of(2026, 11, 9), ReceiptStatus.AVAILABLE),
            expense(FOOD, "Market", "250", LocalDate.of(2026, 10, 5), ReceiptStatus.NO_RECEIPT),
            expense(TRAVEL, "Train", "1000", LocalDate.of(2026, 10, 20), ReceiptStatus.NO_RECEIPT));

    private static Expense expense(Category category, String merchant, String amount, LocalDate date, ReceiptStatus receipt) {
        return new Expense(UUID.randomUUID(), USER, category.id(), merchant, new BigDecimal(amount), date,
                PaymentMethod.UPI, null, receipt == ReceiptStatus.AVAILABLE ? "receipt.png" : null, receipt,
                Instant.now(), Instant.now());
    }

    private static Map<String, Object> build(List<Budget> budgets) {
        var statuses = BudgetService.evaluate(budgets, EXPENSES, CATEGORIES, YearMonth.from(TODAY), 80);
        return DashboardView.build(EXPENSES, CATEGORIES, statuses, TODAY, FORMATS);
    }

    private static String money(String amount) {
        return FORMATS.money(new BigDecimal(amount));
    }

    @Test
    @SuppressWarnings("unchecked")
    void comparesThisMonthWithTheSamePointLastMonth() {
        Map<String, Object> view = build(List.of());

        assertEquals(money("500"), view.get("spent"));
        Map<String, Object> change = (Map<String, Object>) view.get("change");
        assertEquals("up", change.get("direction"));
        assertTrue(((String) change.get("label")).startsWith("↑ 100%"), change.get("label").toString());
        assertEquals(money("50"), view.get("dailyAverage"));
        assertEquals(money("1500"), view.get("projected"));
        assertNull(view.get("overall"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void overallBudgetShowsWhatIsLeftPerDayAndTheProjection() {
        Map<String, Object> overall = (Map<String, Object>) build(
                List.of(new Budget(UUID.randomUUID(), null, new BigDecimal("1200")))).get("overall");

        assertEquals(money("700"), overall.get("left"));
        assertEquals(21, overall.get("daysLeft"));
        assertEquals(money("33"), overall.get("perDay"));
        assertEquals(true, overall.get("projectedOver"));
        assertEquals(money("300"), overall.get("projectedGap"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void budgetsNeedingAttentionComeFirst() {
        List<Map<String, Object>> rows = (List<Map<String, Object>>) build(List.of(
                new Budget(UUID.randomUUID(), null, new BigDecimal("5000")),
                new Budget(UUID.randomUUID(), FOOD.id(), new BigDecimal("200")))).get("budgets");

        assertEquals("Food", rows.get(0).get("label"));
        assertEquals("exceeded", rows.get(0).get("level"));
        assertEquals("Over by " + money("100"), rows.get(0).get("remaining"));
        assertEquals(money("4500") + " left", rows.get(1).get("remaining"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void weeksThatHaveNotStartedAreUpcomingAndLastMonthIsAlongside() {
        List<Map<String, Object>> weeks = (List<Map<String, Object>>) build(List.of()).get("trend");

        assertEquals(5, weeks.size());
        assertEquals("1–7", weeks.get(0).get("days"));
        assertEquals("300", weeks.get(0).get("value"));
        assertEquals("250", weeks.get(0).get("previousValue"));
        assertFalse((Boolean) weeks.get(1).get("upcoming"));
        assertTrue((Boolean) weeks.get(2).get("upcoming"));
        assertEquals("1000", weeks.get(2).get("previousValue"));
        assertEquals("1000", weeks.get(0).get("max"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void categoriesShowShareAndChange() {
        List<Map<String, Object>> categories = (List<Map<String, Object>>) build(List.of()).get("categories");

        assertEquals("Food", categories.get(0).get("label"));
        assertEquals(60L, categories.get(0).get("share"));
        assertEquals("up", categories.get(0).get("direction"));
        assertEquals("+" + money("50") + " vs. last month", categories.get(0).get("change"));
        // Travel's October spending came after the 10th, so it does not count yet.
        assertEquals("new", categories.get(1).get("direction"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void largestExpenseAndMissingReceiptsCoverThisMonthOnly() {
        Map<String, Object> view = build(List.of());

        Map<String, Object> largest = (Map<String, Object>) view.get("largest");
        assertEquals("Corner Café", largest.get("merchant"));
        assertEquals(money("300"), largest.get("amount"));
        assertEquals(1L, view.get("missingReceipts"));
        assertEquals("2026-11-01", view.get("monthStart"));
        assertEquals("2026-11-30", view.get("monthEnd"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handlesAMonthWithNoHistoryAndALongerMonthThanTheLast() {
        LocalDate march31 = LocalDate.of(2026, 3, 31);
        List<Expense> onlyMarch = List.of(expense(FOOD, "Shop", "90", march31, ReceiptStatus.NO_RECEIPT));
        Map<String, Object> view = DashboardView.build(onlyMarch, CATEGORIES, List.of(), march31, FORMATS);

        Map<String, Object> change = (Map<String, Object>) view.get("change");
        assertEquals("none", change.get("direction"));
        assertEquals("Nothing by this point last month", change.get("label"));
        assertFalse((Boolean) view.get("hasLastMonth"));
    }
}
