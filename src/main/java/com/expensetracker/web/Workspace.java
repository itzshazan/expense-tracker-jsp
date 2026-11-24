package com.expensetracker.web;

import com.expensetracker.config.AppConfig;
import com.expensetracker.model.Category;
import com.expensetracker.model.Expense;
import com.expensetracker.model.User;
import com.expensetracker.repository.*;
import com.expensetracker.service.*;
import com.expensetracker.settings.SettingsStore;
import com.expensetracker.settings.UserSettings;
import com.expensetracker.supabase.SupabaseClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class Workspace {
    final User user;
    final AuthService auth;
    final ExpenseService expenses;
    final ExpenseRepository expenseRepository;
    final CategoryRepository categories;
    final BudgetRepository budgets;
    final StorageService storage;
    final SettingsStore settingsStore;

    private Workspace(User user, AuthService auth, ExpenseRepository expenses, CategoryRepository categories,
                      BudgetRepository budgets, StorageService storage) {
        this.user = user;
        this.auth = auth;
        this.expenses = new SupabaseExpenseService(expenses, user.id());
        this.expenseRepository = expenses;
        this.categories = categories;
        this.budgets = budgets;
        this.storage = storage;
        this.settingsStore = new SettingsStore(user.id());
    }

    static Workspace cloud(AppConfig config, SupabaseClient client, SupabaseAuthService auth, User user) {
        return new Workspace(user, auth, new SupabaseExpenseRepository(client, auth::getAccessToken, user.id()),
                new SupabaseCategoryRepository(client, auth::getAccessToken),
                new SupabaseBudgetRepository(client, auth::getAccessToken, user.id()),
                new SupabaseStorageService(client, auth::getAccessToken, config.storageBucket(), user.id(),
                        config.maxReceiptSizeBytes()));
    }

    List<Expense> allExpenses() {
        return expenses.getExpenses(user.id());
    }

    Optional<Expense> findExpense(UUID id) {
        return expenseRepository.findById(id).filter(expense -> user.id().equals(expense.userId()));
    }

    List<Category> allCategories() {
        return categories.findAll();
    }

    UserSettings settings() {
        return settingsStore.get();
    }

    void saveSettings(UserSettings settings) {
        settingsStore.save(settings);
    }
}
