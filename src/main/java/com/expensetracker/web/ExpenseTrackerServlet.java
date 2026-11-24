package com.expensetracker.web;

import com.expensetracker.config.AppConfig;
import com.expensetracker.model.*;
import com.expensetracker.service.*;
import com.expensetracker.settings.*;
import com.expensetracker.supabase.SupabaseClient;
import com.expensetracker.ui.ExpenseDeleteWorkflow;
import com.expensetracker.ui.ReceiptWorkflow;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.sourceforge.tess4j.util.LoadLibs;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@WebServlet(urlPatterns = "/app/*", loadOnStartup = 1)
@MultipartConfig(maxFileSize = ReceiptUpload.MAX_FILE_BYTES,
        maxRequestSize = ReceiptUpload.MAX_FILE_BYTES + 131072)
public final class ExpenseTrackerServlet extends HttpServlet {
    private static final String WORKSPACE = "expenseWorkspace";
    private static final String CSRF = "csrf";
    private static final String REGISTRATION_EMAIL = "registrationEmail";
    private static final String RECOVERY = "recoveryChallenge";
    private static final String RECOVERY_CODE = "recoveryCode";
    private static final String GOOGLE_SIGN_IN = "googleSignIn";
    private static final String GOOGLE_CALLBACK = "/auth/google/callback";
    private static final String FLASH = "flash";
    private final ReportService reportService = new DefaultReportService();
    private final ObjectMapper json = new ObjectMapper();
    private final java.util.concurrent.Semaphore ocrSlots = new java.util.concurrent.Semaphore(2, true);
    private AppConfig config;
    private SupabaseClient client;
    private WebRecovery recovery;
    private String googleCallback;
    private String tessDataDirectory;

    @Override
    public void init() {
        config = AppConfig.fromEnvironment();
        if (config.isSupabaseConfigured()) {
            client = new SupabaseClient(config);
            String publicUrl = System.getProperty("EXPENSE_WEB_URL", System.getenv("EXPENSE_WEB_URL"));
            if (publicUrl == null || publicUrl.isBlank()) {
                // Render sets this to the service's https://….onrender.com address.
                publicUrl = System.getenv("RENDER_EXTERNAL_URL");
            }
            if (publicUrl == null || publicUrl.isBlank()) {
                publicUrl = "http://127.0.0.1:8080" + getServletContext().getContextPath();
            }
            String appUrl = publicBaseUrl(publicUrl.strip()) + "/app";
            recovery = new WebRecovery(client, appUrl + "/recovery");
            googleCallback = appUrl + GOOGLE_CALLBACK;
        }
    }

    private static String publicBaseUrl(String publicUrl) {
        URI uri = URI.create(publicUrl);
        boolean loopback = "localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                || uri.getFragment() != null || !("https".equals(uri.getScheme())
                || loopback && "http".equals(uri.getScheme()))) {
            throw new IllegalArgumentException("Set EXPENSE_WEB_URL to the public base URL of this app.");
        }
        return publicUrl.replaceFirst("/+$", "");
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Content-Security-Policy", "default-src 'self'; img-src 'self'; "
                + "script-src 'self'; style-src 'self'; object-src 'none'; "
                + "frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
        super.service(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String route = route(request);
        Workspace workspace = workspace(request);
        try {
            switch (route) {
                case "/" -> {
                    // Signed-in users can visit the home page too; it offers them their dashboard.
                    render(request, response, "landing", "Expense Tracker");
                    return;
                }
                case "/signin", "/register", "/forgot" -> {
                    authPage(request, response, route);
                    return;
                }
                case "/recovery" -> {
                    showRecovery(request, response);
                    return;
                }
                case "/auth/google" -> {
                    startGoogleSignIn(request, response);
                    return;
                }
                case GOOGLE_CALLBACK -> {
                    completeGoogleSignIn(request, response);
                    return;
                }
                default -> { }
            }
            if (workspace == null) {
                redirect(request, response, "/signin");
                return;
            }
            synchronized (workspace) {
                switch (route) {
                    case "/dashboard" -> dashboard(request, response, workspace);
                    case "/expenses" -> expenses(request, response, workspace);
                    case "/expense/new" -> expenseForm(request, response, workspace, null, false);
                    case "/expense/edit" -> expenseForm(request, response, workspace, findExpense(request, workspace), false);
                    case "/receipt" -> receipt(request, response, findExpense(request, workspace), workspace);
                    case "/reports" -> reports(request, response, workspace, false);
                    case "/reports/export" -> reports(request, response, workspace, true);
                    case "/settings" -> settings(request, response, workspace);
                    default -> problem(request, response, 404, "This page could not be found.");
                }
            }
        } catch (MissingRecord exception) {
            problem(request, response, 404, exception.getMessage());
        } catch (IllegalArgumentException | ServiceException exception) {
            problem(request, response, 400, exception.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String route = route(request);
        try {
            if (!validCsrf(request)) {
                problem(request, response, 403, "Your form session expired. Reload the page and try again.");
                return;
            }
        } catch (IllegalStateException exception) {
            problem(request, response, 413, "The attached file exceeds the web upload limit.");
            return;
        }
        try {
            switch (route) {
                case "/signin", "/register" -> {
                    authenticate(request, response, "/register".equals(route));
                    return;
                }
                case "/register/start" -> {
                    if (workspace(request) != null) {
                        redirect(request, response, "/dashboard");
                    } else {
                        request.getSession().setAttribute(REGISTRATION_EMAIL, email(request));
                        redirect(request, response, "/register");
                    }
                    return;
                }
                case "/forgot" -> {
                    sendRecovery(request, response);
                    return;
                }
                case "/recovery" -> {
                    changeRecoveredPassword(request, response);
                    return;
                }
                default -> { }
            }
            Workspace workspace = workspace(request);
            if (workspace == null) {
                if ("/receipt/scan".equals(route)) {
                    json(response, 401, Map.of("error", "Your session expired. Reload the page to continue."));
                } else {
                    redirect(request, response, "/signin");
                }
                return;
            }
            synchronized (workspace) {
                switch (route) {
                    case "/logout" -> logout(request, response, workspace);
                    case "/expense/save" -> saveExpense(request, response, workspace);
                    case "/expense/delete" -> deleteExpense(request, response, workspace);
                    case "/receipt/scan" -> scanReceipt(request, response, workspace);
                    case "/budget/save", "/budget/delete", "/settings/save", "/settings/reset",
                         "/account/name", "/account/password", "/account/delete" ->
                            changeSettings(request, response, workspace, route);
                    default -> problem(request, response, 404, "This action could not be found.");
                }
            }
        } catch (MissingRecord exception) {
            problem(request, response, 404, exception.getMessage());
        } catch (IllegalArgumentException | ServiceException exception) {
            if ("/receipt/scan".equals(route)) {
                json(response, 400, Map.of("error", exception.getMessage()));
            } else {
                flash(request, exception.getMessage(), "error");
                String errorPage = switch (route) {
                    case "/signin", "/register", "/forgot", "/recovery" -> route;
                    case "/register/start" -> "/";
                    case "/expense/delete" -> "/expenses";
                    default -> "/settings";
                };
                redirect(request, response, errorPage);
            }
        }
    }

    private void authenticate(HttpServletRequest request, HttpServletResponse response, boolean signup)
            throws IOException {
        requireCloud();
        if (workspace(request) != null) {
            redirect(request, response, "/dashboard");
            return;
        }
        String email = email(request);
        char[] password = Forms.password(request, "password", signup ? 6 : 1);
        SupabaseAuthService auth = new SupabaseAuthService(client);
        try {
            User user = signup ? auth.register(email, password) : auth.login(email, password);
            if (auth.getCurrentUser() == null) {
                flash(request, "Check your email to confirm your account, then sign in.", "success");
                redirect(request, response, "/signin");
                return;
            }
            signIn(request, Workspace.cloud(config, client, auth, user));
            redirect(request, response, "/dashboard");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static String email(HttpServletRequest request) {
        String email = Forms.required(request, "email", 254, "Email");
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        return email;
    }

    private void startGoogleSignIn(HttpServletRequest request, HttpServletResponse response) {
        if (workspace(request) != null) {
            redirect(request, response, "/dashboard");
            return;
        }
        try {
            requireCloud();
            var settings = client.send("/auth/v1/settings", "GET", null, null);
            if (settings.statusCode() != 200) {
                throw new ServiceException("Supabase Auth is unavailable.");
            }
            if (!json.readTree(settings.body()).path("external").path("google").asBoolean()) {
                flash(request, "Google sign-in is not enabled in Supabase yet.", "error");
                redirect(request, response, "/signin");
                return;
            }
            Pkce pkce = Pkce.create(Duration.ofMinutes(10));
            request.getSession().setAttribute(GOOGLE_SIGN_IN, pkce);
            String authorizePath = "/auth/v1/authorize?provider=google&redirect_to="
                    + URLEncoder.encode(googleCallback, StandardCharsets.UTF_8)
                    + "&code_challenge=" + pkce.challenge() + "&code_challenge_method=s256";
            response.setStatus(303);
            response.setHeader("Location", client.endpoint(authorizePath).toASCIIString());
        } catch (ServiceException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            flash(request, "Google sign-in is unavailable right now. Try again or sign in with email.", "error");
            redirect(request, response, "/signin");
        }
    }

    private void completeGoogleSignIn(HttpServletRequest request, HttpServletResponse response) {
        if (workspace(request) != null) {
            redirect(request, response, "/dashboard");
            return;
        }
        HttpSession session = request.getSession(false);
        Pkce pkce = null;
        if (session != null) {
            synchronized (session) {
                pkce = (Pkce) session.getAttribute(GOOGLE_SIGN_IN);
                session.removeAttribute(GOOGLE_SIGN_IN);
            }
        }
        if (pkce == null || pkce.expired()) {
            flash(request, "Start Google sign-in again in this browser.", "error");
        } else if ("access_denied".equals(request.getParameter("error"))) {
            flash(request, "Google sign-in was cancelled.", "warning");
        } else {
            try {
                requireCloud();
                String code = Forms.required(request, "code", 1024, "Authorization code");
                SupabaseAuthService auth = new SupabaseAuthService(client);
                User user = auth.signInWithCode(code, pkce.verifier());
                signIn(request, Workspace.cloud(config, client, auth, user));
                redirect(request, response, "/dashboard");
                return;
            } catch (IllegalArgumentException | ServiceException exception) {
                flash(request, "Google sign-in could not be completed. Start again or sign in with email.", "error");
            }
        }
        redirect(request, response, "/signin");
    }

    private void sendRecovery(HttpServletRequest request, HttpServletResponse response) throws IOException {
        requireCloud();
        request.getSession().setAttribute(RECOVERY, recovery.send(email(request)));
        request.getSession().removeAttribute(RECOVERY_CODE);
        flash(request, "If the account exists, check your email and open the link in this browser.", "success");
        redirect(request, response, "/forgot");
    }

    private void showRecovery(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getParameter("code") != null) {
            HttpSession session = request.getSession();
            if (session.getAttribute(RECOVERY) != null) {
                session.setAttribute(RECOVERY_CODE, Forms.required(request, "code", 1024, "Recovery code"));
            }
            redirect(request, response, "/recovery");
            return;
        }
        request.setAttribute("canReset", request.getSession().getAttribute(RECOVERY_CODE) != null);
        authPage(request, response, "/recovery");
    }

    private void changeRecoveredPassword(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        requireCloud();
        HttpSession session = request.getSession();
        synchronized (session) {
            var challenge = (Pkce) session.getAttribute(RECOVERY);
            String code = (String) session.getAttribute(RECOVERY_CODE);
            if (code == null || challenge == null) {
                throw new ServiceException("Request a new reset email and open it in this browser.");
            }
            char[] password = verifiedNewPassword(request);
            session.removeAttribute(RECOVERY);
            session.removeAttribute(RECOVERY_CODE);
            recovery.reset(challenge, code, password);
        }
        if (workspace(request) != null) {
            session.invalidate();
        }
        flash(request, "Password updated. Sign in with your new password.", "success");
        redirect(request, response, "/signin");
    }

    private static char[] verifiedNewPassword(HttpServletRequest request) {
        char[] password = Forms.password(request, "password", 6);
        if (!Arrays.equals(password, Objects.toString(request.getParameter("confirmPassword"), "").toCharArray())) {
            Arrays.fill(password, '\0');
            throw new IllegalArgumentException("The two passwords do not match.");
        }
        return password;
    }

    private void requireCloud() {
        if (client == null) {
            throw new ServiceException("Cloud accounts are unavailable. Configure Supabase to continue.");
        }
    }

    private void dashboard(HttpServletRequest request, HttpServletResponse response, Workspace workspace)
            throws ServletException, IOException {
        LocalDate today = today(request);
        var entries = workspace.allExpenses();
        var categories = workspace.allCategories();
        var totals = reportService.generateDashboard(entries, today);
        Formats formats = new Formats(workspace.settings());
        var statuses = BudgetService.evaluate(workspace.budgets.findAll(), entries, categories, YearMonth.from(today),
                workspace.settings().alertThreshold());
        request.setAttribute("dash", DashboardView.build(entries, categories, statuses, today, formats));
        request.setAttribute("rows", ViewData.expenses(totals.recentExpenses(), categories, formats));
        render(request, response, "dashboard", "Dashboard");
    }

    /**
     * Today's date where the user is. app.js stores the browser's time zone in the "tz" cookie;
     * without it (or with an unknown zone) the server's own date is used.
     */
    static LocalDate today(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("tz".equals(cookie.getName()) && cookie.getValue().length() <= 80) {
                    try {
                        return LocalDate.now(ZoneId.of(URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8)));
                    } catch (RuntimeException ignored) {
                        // Unknown or malformed zone: fall back to the server's date.
                    }
                }
            }
        }
        return LocalDate.now();
    }

    private void expenses(HttpServletRequest request, HttpServletResponse response, Workspace workspace)
            throws ServletException, IOException {
        var categories = workspace.allCategories();
        var from = Forms.date(Forms.value(request, "from", 10));
        var to = Forms.date(Forms.value(request, "to", 10));
        Forms.checkRange(from, to);
        String query = Forms.value(request, "q", 150).toLowerCase(Locale.ROOT);
        String category = Forms.value(request, "category", 36);
        String payment = Forms.value(request, "payment", 32);
        String receiptStatus = Forms.value(request, "receipt", 32);
        String order = Forms.value(request, "sort", 32);
        Comparator<Expense> comparator = switch (order) {
            case "oldest" -> Comparator.comparing(Expense::expenseDate);
            case "largest" -> Comparator.comparing(Expense::amount).reversed();
            case "smallest" -> Comparator.comparing(Expense::amount);
            default -> Comparator.comparing(Expense::expenseDate).reversed();
        };
        List<Expense> entries = workspace.allExpenses().stream()
                .filter(expense -> query.isEmpty() || (expense.merchant() + " "
                        + Objects.toString(expense.description(), "")).toLowerCase(Locale.ROOT).contains(query))
                .filter(expense -> category.isEmpty() || expense.categoryId().toString().equals(category))
                .filter(expense -> payment.isEmpty() || expense.paymentMethod().name().equals(payment))
                .filter(expense -> receiptStatus.isEmpty() || expense.receiptStatus().name().equals(receiptStatus))
                .filter(expense -> from == null || !expense.expenseDate().isBefore(from))
                .filter(expense -> to == null || !expense.expenseDate().isAfter(to))
                .sorted(comparator.thenComparing(Expense::id)).toList();
        request.setAttribute("categories", ViewData.categories(categories, false));
        request.setAttribute("payments", ViewData.payments());
        request.setAttribute("receiptOptions", ViewData.options(ReceiptStatus.values(), ReceiptStatus::displayName));
        request.setAttribute("rows", ViewData.expenses(entries, categories, new Formats(workspace.settings())));
        render(request, response, "expenses", "Expenses");
    }

    private void expenseForm(HttpServletRequest request, HttpServletResponse response, Workspace workspace,
                             Expense expense, boolean preservePost) throws ServletException, IOException {
        var categories = workspace.allCategories();
        var defaults = workspace.settings();
        String defaultCategory = categories.stream().filter(category -> category.name().equals(defaults.defaultCategory()))
                .map(category -> category.id().toString()).findFirst().orElse("");
        Map<String, String> fields = new HashMap<>();
        fields.put("merchant", expense == null ? "" : expense.merchant());
        fields.put("amount", expense == null ? "" : expense.amount().toPlainString());
        fields.put("date", expense == null ? today(request).toString() : expense.expenseDate().toString());
        fields.put("category", expense == null ? defaultCategory : expense.categoryId().toString());
        fields.put("payment", expense == null ? (defaults.defaultPayment() == null ? "" : defaults.defaultPayment().name())
                : expense.paymentMethod().name());
        fields.put("description", expense == null ? "" : Objects.toString(expense.description(), ""));
        if (preservePost) {
            fields.replaceAll((key, value) -> Objects.toString(request.getParameter(key), ""));
        }
        request.setAttribute("fields", fields);
        request.setAttribute("expenseId", expense == null ? "" : expense.id().toString());
        request.setAttribute("hasReceipt", expense != null && expense.receiptPath() != null && !expense.receiptPath().isBlank());
        request.setAttribute("autoScan", defaults.autoScan());
        request.setAttribute("categories", ViewData.categories(categories, false));
        request.setAttribute("payments", ViewData.payments());
        render(request, response, "expense-form", expense == null ? "Add expense" : "Edit expense");
    }

    private void saveExpense(HttpServletRequest request, HttpServletResponse response, Workspace workspace)
            throws ServletException, IOException {
        String id = Forms.value(request, "id", 36);
        Expense existing = id.isBlank() ? null : findExpense(request, workspace);
        try (ReceiptUpload upload = ReceiptUpload.read(request.getPart("receipt"), config.maxReceiptSizeBytes())) {
            LocalDate date = Forms.date(Forms.required(request, "date", 10, "Expense date"));
            var category = Forms.category(Forms.value(request, "category", 36), workspace.allCategories());
            var payment = Forms.choice(PaymentMethod.class, Forms.value(request, "payment", 32));
            var expense = new Expense(existing == null ? UUID.randomUUID() : existing.id(), workspace.user.id(), category.id(),
                    Forms.required(request, "merchant", 200, "Merchant"),
                    Forms.amount(Forms.value(request, "amount", 32)), date, payment,
                    Forms.value(request, "description", 1500), existing == null ? null : existing.receiptPath(),
                    existing == null ? ReceiptStatus.NO_RECEIPT : existing.receiptStatus(),
                    existing == null ? Instant.now() : existing.createdAt(), Instant.now());
            var saved = existing == null ? workspace.expenses.createExpense(expense) : workspace.expenses.updateExpense(expense);
            var result = ReceiptWorkflow.finish(workspace.expenses, workspace.storage, saved, existing, upload.path(),
                    request.getParameter("removeReceipt") != null);
            flash(request, result.message(), result.warning() ? "warning" : "success");
            redirect(request, response, "/expenses");
        } catch (IllegalArgumentException | ServiceException exception) {
            response.setStatus(400);
            request.setAttribute("notice", Map.of("text", exception.getMessage(), "type", "error"));
            expenseForm(request, response, workspace, existing, true);
        }
    }

    private static void deleteExpense(HttpServletRequest request, HttpServletResponse response, Workspace workspace)
            throws IOException {
        Expense expense = findExpense(request, workspace);
        var result = ExpenseDeleteWorkflow.delete(workspace.expenses, workspace.storage, List.of(expense), expense.id());
        if (!result.deleted()) {
            throw new ServiceException("The expense could not be deleted.", result.error());
        }
        flash(request, result.receiptLeftBehind()
                ? "Expense deleted. Its receipt could not be removed from storage." : "Expense deleted.",
                result.receiptLeftBehind() ? "warning" : "success");
        redirect(request, response, "/expenses");
    }

    private static Expense findExpense(HttpServletRequest request, Workspace workspace) {
        UUID id = Forms.id(Forms.value(request, "id", 36), "expense");
        return workspace.findExpense(id).orElseThrow(() -> new MissingRecord("This expense could not be found."));
    }

    private static void receipt(HttpServletRequest request, HttpServletResponse response, Expense expense,
                                Workspace workspace) throws IOException {
        String path = expense.receiptPath();
        if (path == null || path.isBlank()) {
            throw new MissingRecord("This expense has no receipt.");
        }
        byte[] bytes = workspace.storage.downloadReceipt(path);
        boolean png = bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50
                && bytes[2] == 0x4e && bytes[3] == 0x47;
        boolean jpeg = bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff;
        if (!png && !jpeg) {
            throw new ServiceException("This receipt is not a supported image.");
        }
        response.setContentType(png ? "image/png" : "image/jpeg");
        response.setHeader("Content-Disposition", png ? "inline; filename=\"receipt.png\"" : "inline; filename=\"receipt.jpg\"");
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    private void scanReceipt(HttpServletRequest request, HttpServletResponse response, Workspace workspace)
            throws ServletException, IOException {
        if (!ocrSlots.tryAcquire()) {
            json(response, 429, Map.of("error", "Receipt scanning is busy. Try again in a moment."));
            return;
        }
        try (ReceiptUpload upload = ReceiptUpload.read(request.getPart("receipt"), config.maxReceiptSizeBytes())) {
            if (upload.path() == null) {
                throw new ServiceException("Choose a receipt image to scan.");
            }
            upload.validateDimensions();
            OcrService ocr = new Tess4JOcrService(tessDataDirectory(), config.maxReceiptSizeBytes());
            ocr.useLanguage(workspace.settings().ocrLanguage());
            OcrResult result = ocr.processReceipt(upload.path());
            json(response, 200, Map.of("merchant", Objects.toString(result.merchant(), ""),
                    "amount", Objects.toString(result.amount(), ""), "date", Objects.toString(result.expenseDate(), "")));
        } finally {
            ocrSlots.release();
        }
    }

    private synchronized String tessDataDirectory() {
        if (tessDataDirectory == null) {
            String configured = System.getenv("TESSDATA_PREFIX");
            if (configured != null && !configured.isBlank()) {
                Path directory = Path.of(configured.strip());
                if (!Files.isRegularFile(directory.resolve("eng.traineddata"))
                        && Files.isDirectory(directory.resolve("tessdata"))) {
                    directory = directory.resolve("tessdata");
                }
                tessDataDirectory = directory.toAbsolutePath().toString();
            } else if (Files.isRegularFile(Path.of("tessdata", "eng.traineddata"))) {
                tessDataDirectory = Path.of("tessdata").toAbsolutePath().toString();
            } else {
                tessDataDirectory = LoadLibs.extractTessResources("tessdata").getAbsolutePath();
            }
        }
        return tessDataDirectory;
    }

    private void reports(HttpServletRequest request, HttpServletResponse response, Workspace workspace, boolean export)
            throws ServletException, IOException {
        String range = Forms.value(request, "range", 16);
        LocalDate today = today(request);
        LocalDate from = switch (range) {
            case "all" -> null;
            case "month" -> today.withDayOfMonth(1);
            case "30" -> today.minusDays(29);
            case "year" -> today.withDayOfYear(1);
            default -> request.getParameter("from") == null ? today.withDayOfMonth(1)
                    : Forms.date(Forms.value(request, "from", 10));
        };
        LocalDate to = switch (range) {
            case "all" -> null;
            case "month", "30", "year" -> today;
            default -> request.getParameter("to") == null ? today : Forms.date(Forms.value(request, "to", 10));
        };
        Forms.checkRange(from, to);
        var categories = workspace.allCategories();
        var report = reportService.generateDetailedReport(workspace.allExpenses(), categories, from, to);
        if (export) {
            exportReport(request, response, workspace, categories, report);
            return;
        }
        Formats formats = new Formats(workspace.settings());
        request.setAttribute("from", Objects.toString(from, ""));
        request.setAttribute("to", Objects.toString(to, ""));
        request.setAttribute("total", formats.money(report.totalAmount()));
        request.setAttribute("count", report.count());
        request.setAttribute("average", formats.money(report.averageAmount()));
        request.setAttribute("highest", report.highestExpense() == null ? "—" : formats.money(report.highestExpense().amount()));
        request.setAttribute("categoryBreakdown", ViewData.categories(report, formats));
        request.setAttribute("paymentBreakdown", ViewData.payments(report, formats));
        request.setAttribute("rows", ViewData.expenses(report.expenses(), categories, formats));
        render(request, response, "reports", "Reports");
    }

    private void exportReport(HttpServletRequest request, HttpServletResponse response, Workspace workspace,
                              List<Category> categories, ReportService.DetailedReport report) throws IOException {
        String format = Forms.value(request, "format", 3);
        if (!"pdf".equals(format) && !"csv".equals(format)) {
            throw new IllegalArgumentException("Choose CSV or PDF.");
        }
        var file = Files.createTempFile("expense-report-", "." + format);
        try {
            if ("pdf".equals(format)) {
                reportService.exportToPdf(report, categories, file, workspace.user.email());
            } else {
                reportService.exportToCsv(report, categories, file);
            }
            response.setContentType("pdf".equals(format) ? "application/pdf" : "text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"expenses-" + LocalDate.now() + "." + format + "\"");
            response.setContentLengthLong(Files.size(file));
            Files.copy(file, response.getOutputStream());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private void settings(HttpServletRequest request, HttpServletResponse response, Workspace workspace)
            throws ServletException, IOException {
        var settings = workspace.settings();
        var categories = workspace.allCategories();
        request.setAttribute("currencies", ViewData.options(CurrencyOption.values(), CurrencyOption::toString));
        request.setAttribute("dateStyles", ViewData.options(DateStyle.values(), DateStyle::toString));
        request.setAttribute("payments", ViewData.payments());
        request.setAttribute("categories", ViewData.categories(categories, false));
        request.setAttribute("categoryNames", ViewData.categories(categories, true));
        var languages = Tess4JOcrService.installedLanguages();
        languages.putIfAbsent(settings.ocrLanguage(), settings.ocrLanguage() + " (not installed)");
        request.setAttribute("languages", languages.entrySet().stream()
                .map(entry -> Map.of("value", entry.getKey(), "label", entry.getValue())).toList());
        request.setAttribute("budgets", ViewData.budgets(workspace.budgets.findAll(), categories, new Formats(settings)));
        request.setAttribute("preferences", Map.of("currency", settings.currency().name(), "date", settings.dateStyle().name(),
                "payment", settings.defaultPayment() == null ? "" : settings.defaultPayment().name(),
                "category", Objects.toString(settings.defaultCategory(), ""), "ocr", settings.ocrLanguage(),
                "autoScan", settings.autoScan(), "threshold", settings.alertThreshold()));
        render(request, response, "settings", "Settings");
    }

    private void changeSettings(HttpServletRequest request, HttpServletResponse response, Workspace workspace, String route)
            throws IOException {
        String message = switch (route) {
            case "/budget/save" -> {
                String category = Forms.value(request, "category", 36);
                UUID categoryId = category.isBlank() ? null : Forms.category(category, workspace.allCategories()).id();
                workspace.budgets.save(categoryId, Forms.amount(Forms.value(request, "limit", 32)));
                yield "Monthly budget saved.";
            }
            case "/budget/delete" -> {
                UUID budget = Forms.id(Forms.value(request, "id", 36), "budget");
                if (workspace.budgets.findAll().stream().noneMatch(item -> item.id().equals(budget))) {
                    throw new MissingRecord("This budget could not be found.");
                }
                workspace.budgets.delete(budget);
                yield "Budget removed.";
            }
            case "/settings/save" -> {
                workspace.saveSettings(preferences(request, workspace));
                yield "Preferences saved.";
            }
            case "/settings/reset" -> {
                workspace.saveSettings(UserSettings.defaults());
                yield "Default preferences restored.";
            }
            case "/account/name" -> {
                workspace.auth.updateDisplayName(Forms.required(request, "displayName", 100, "Display name"));
                yield "Display name saved.";
            }
            case "/account/password" -> {
                char[] password = verifiedNewPassword(request);
                try {
                    workspace.auth.changePassword(password);
                } finally {
                    Arrays.fill(password, '\0');
                }
                yield "Password updated.";
            }
            case "/account/delete" -> {
                deleteAccount(request, response, workspace);
                yield null;
            }
            default -> throw new IllegalArgumentException("This action could not be found.");
        };
        if (message != null) {
            flash(request, message, "success");
            redirect(request, response, "/settings");
        }
    }

    private static UserSettings preferences(HttpServletRequest request, Workspace workspace) {
        String payment = Forms.value(request, "payment", 32);
        String category = Forms.value(request, "category", 100);
        if (!category.isEmpty() && workspace.allCategories().stream().noneMatch(item -> item.name().equals(category))) {
            throw new IllegalArgumentException("Choose an available default category.");
        }
        String language = Forms.value(request, "ocr", 32);
        if (!workspace.settings().ocrLanguage().equals(language)
                && !Tess4JOcrService.installedLanguages().containsKey(language)) {
            throw new IllegalArgumentException("Choose an available OCR language.");
        }
        int threshold;
        try {
            threshold = Integer.parseInt(Forms.value(request, "threshold", 3));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a budget alert between 50 and 100.");
        }
        if (threshold < 50 || threshold > 100) {
            throw new IllegalArgumentException("Enter a budget alert between 50 and 100.");
        }
        return new UserSettings(Forms.choice(CurrencyOption.class, Forms.value(request, "currency", 3)),
                Forms.choice(DateStyle.class, Forms.value(request, "dateStyle", 24)),
                payment.isEmpty() ? null : Forms.choice(PaymentMethod.class, payment), category.isEmpty() ? null : category,
                language, request.getParameter("autoScan") != null, threshold);
    }

    private static void deleteAccount(HttpServletRequest request, HttpServletResponse response, Workspace workspace)
            throws IOException {
        if (!"DELETE".equals(Forms.value(request, "confirmation", 10))) {
            throw new IllegalArgumentException("Type DELETE to confirm account deletion.");
        }
        workspace.auth.deleteAccount(false);
        int failedReceipts = 0;
        for (Expense expense : workspace.allExpenses()) {
            if (expense.receiptPath() != null && !expense.receiptPath().isBlank()) {
                try {
                    workspace.storage.deleteReceipt(expense.receiptPath());
                } catch (ServiceException exception) {
                    failedReceipts++;
                }
            }
        }
        workspace.auth.deleteAccount(true);
        workspace.settingsStore.clear();
        request.getSession().invalidate();
        flash(request, failedReceipts == 0 ? "Your account was deleted."
                : "Your account was deleted. " + failedReceipts + " receipt file(s) could not be removed.",
                failedReceipts == 0 ? "success" : "warning");
        redirect(request, response, "/signin");
    }

    private static void logout(HttpServletRequest request, HttpServletResponse response, Workspace workspace)
            throws IOException {
        try {
            workspace.auth.logout();
        } finally {
            request.getSession().invalidate();
        }
        redirect(request, response, "/");
    }

    private static String route(HttpServletRequest request) {
        return Objects.toString(request.getPathInfo(), "/");
    }

    private static Workspace workspace(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (Workspace) session.getAttribute(WORKSPACE);
    }

    private static void signIn(HttpServletRequest request, Workspace workspace) {
        HttpSession session = request.getSession();
        synchronized (session) {
            if (session.getAttribute(WORKSPACE) != null) {
                return;
            }
            request.changeSessionId();
            session.removeAttribute(RECOVERY);
            session.removeAttribute(RECOVERY_CODE);
            session.removeAttribute(GOOGLE_SIGN_IN);
            session.removeAttribute(REGISTRATION_EMAIL);
            session.setAttribute(CSRF, UUID.randomUUID().toString());
            session.setAttribute(WORKSPACE, workspace);
        }
    }

    private static boolean validCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object token = session == null ? null : session.getAttribute(CSRF);
        return token != null && token.equals(request.getParameter("csrf"));
    }

    private static void flash(HttpServletRequest request, String text, String kind) {
        request.getSession().setAttribute(FLASH, Map.of("text", text, "type", kind));
    }

    private void authPage(HttpServletRequest request, HttpServletResponse response, String route)
            throws ServletException, IOException {
        String heading = switch (route) {
            case "/register" -> "Create your account";
            case "/forgot" -> "Reset your password";
            case "/recovery" -> "Choose a new password";
            default -> "Sign in";
        };
        request.setAttribute("authAction", route.substring(1));
        if ("/register".equals(route)) {
            HttpSession session = request.getSession();
            synchronized (session) {
                request.setAttribute("registrationEmail", session.getAttribute(REGISTRATION_EMAIL));
                session.removeAttribute(REGISTRATION_EMAIL);
            }
        }
        render(request, response, "auth", heading);
    }

    private void problem(HttpServletRequest request, HttpServletResponse response, int code, String message)
            throws ServletException, IOException {
        response.setStatus(code);
        request.setAttribute("problemMessage", message);
        render(request, response, "problem", code == 404 ? "Page not found" : "We couldn’t complete that request");
    }

    private void render(HttpServletRequest request, HttpServletResponse response, String screen, String title)
            throws ServletException, IOException {
        Workspace workspace = workspace(request);
        HttpSession session = request.getSession();
        synchronized (session) {
            if (session.getAttribute(CSRF) == null) {
                session.setAttribute(CSRF, UUID.randomUUID().toString());
            }
            request.setAttribute(CSRF, session.getAttribute(CSRF));
            if (request.getAttribute("notice") == null) {
                request.setAttribute("notice", session.getAttribute(FLASH));
            }
            session.removeAttribute(FLASH);
        }
        request.setAttribute("signedIn", workspace != null);
        request.setAttribute("cloud", client != null);
        request.setAttribute("screen", screen);
        request.setAttribute("title", title);
        if (workspace != null) {
            request.setAttribute("email", workspace.user.email());
            request.setAttribute("displayName", workspace.auth.getDisplayName());
        }
        response.setContentType("text/html; charset=UTF-8");
        request.getRequestDispatcher("/WEB-INF/views/layout.jsp").forward(request, response);
    }

    private static void redirect(HttpServletRequest request, HttpServletResponse response, String path) {
        response.setStatus(303);
        response.setHeader("Location", request.getContextPath() + "/app" + path);
    }

    private void json(HttpServletResponse response, int code, Map<String, String> body) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json; charset=UTF-8");
        json.writeValue(response.getOutputStream(), body);
    }

    private static final class MissingRecord extends RuntimeException {
        MissingRecord(String message) {
            super(message);
        }
    }
}
