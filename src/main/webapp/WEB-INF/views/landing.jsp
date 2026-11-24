<%@ page pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<section class="home-width home-hero" aria-labelledby="hero-heading">
    <div class="home-hero-copy">
        <h1 id="hero-heading">Make sense of <em>your spending.</em></h1>
        <p>Scan receipts, set monthly budgets, and see where your money goes in one private workspace.</p>
        <c:choose>
            <c:when test="${signedIn}">
                <div class="home-actions">
                    <a class="home-button hero-dashboard" href="${base}/dashboard">Open your dashboard <ui:public-icon name="arrow-right"/></a>
                </div>
            </c:when>
            <c:otherwise>
                <form class="hero-signup" action="${base}/register/start" method="post">
                    <ui:csrf/>
                    <label class="sr-only" for="hero-email">Email address</label>
                    <input id="hero-email" name="email" type="email" autocomplete="email" maxlength="254" placeholder="Your email address" required>
                    <button class="home-button" type="submit">Create account</button>
                </form>
            </c:otherwise>
        </c:choose>
        <a class="hero-workflow-link" href="#how-it-works">See how it works</a>
    </div>

    <figure class="hero-product">
        <jsp:include page="/WEB-INF/views/hero-preview.jsp" />
        <div class="hero-chip hero-chip-budget" aria-hidden="true" data-chip-budget>
            <div><strong>Monthly budget</strong><span data-chip-used>70% used</span></div>
            <div class="hero-chip-bar"><i data-chip-bar></i></div>
            <small data-chip-left>₹3,580 left this month</small>
        </div>
        <div class="hero-chip hero-chip-scan" aria-hidden="true" data-chip-scan>
            <span class="hero-chip-icon"><ui:public-icon name="scan"/></span>
            <div><strong>Receipt scanned</strong><small>Market Studio · ₹300.00</small></div>
        </div>
        <figcaption class="sr-only">Illustration of the Expense Tracker dashboard and the steps for attaching a receipt, reviewing its details, and adding an expense.</figcaption>
    </figure>
</section>

<ul class="home-width home-highlights" aria-label="Product highlights">
    <li><ui:public-icon name="scan"/> Receipt scanning</li>
    <li><ui:public-icon name="pie"/> Monthly budgets</li>
    <li><ui:public-icon name="file"/> PDF and CSV exports</li>
</ul>

<section class="home-section home-width" id="how-it-works" aria-labelledby="workflow-heading">
    <header class="home-section-heading" data-reveal>
        <h2 id="workflow-heading">From receipt to report</h2>
        <p>See how an expense moves through your workspace.</p>
    </header>
    <div class="home-workflow" data-workflow data-reveal>
        <div class="workflow-tabs" role="tablist" aria-label="Expense workflow" hidden>
            <button type="button" id="capture-tab" role="tab" aria-controls="capture-panel" aria-selected="true"><ui:public-icon name="scan"/> Capture</button>
            <button type="button" id="organize-tab" role="tab" aria-controls="organize-panel" aria-selected="false" tabindex="-1"><ui:public-icon name="card"/> Organize</button>
            <button type="button" id="review-tab" role="tab" aria-controls="review-panel" aria-selected="false" tabindex="-1"><ui:public-icon name="chart"/> Review</button>
        </div>
        <article class="workflow-panel workflow-capture" id="capture-panel">
            <div class="workflow-copy">
                <h3>Scan a receipt.</h3>
                <p>Upload a JPG or PNG. Expense Tracker reads the merchant, date, and amount for you to review before saving.</p>
            </div>
            <div class="workflow-visual" aria-hidden="true">
                <div class="sample-receipt">
                    <strong>Corner Café</strong><span>12 November</span>
                    <div><span>Flat white</span><span>₹120</span></div>
                    <div><span>Banana bread</span><span>₹160</span></div>
                    <div><strong>Total</strong><strong>₹280</strong></div>
                </div>
                <div class="sample-scan"><ui:public-icon name="scan"/><span>Suggested amount<strong>₹280.00</strong></span></div>
            </div>
        </article>
        <article class="workflow-panel workflow-organize" id="organize-panel">
            <div class="workflow-copy">
                <h3>Give it a category.</h3>
                <p>Confirm the receipt details, choose a category and payment method, and add a note so you can find it later.</p>
            </div>
            <div class="workflow-visual" aria-hidden="true">
                <div class="sample-details">
                    <strong>Expense details</strong>
                    <dl><dt>Merchant</dt><dd>Corner Café</dd><dt>Amount</dt><dd>₹280.00</dd>
                        <dt>Category</dt><dd><span>Food &amp; Dining</span></dd><dt>Payment</dt><dd>UPI</dd></dl>
                </div>
            </div>
        </article>
        <article class="workflow-panel workflow-review" id="review-panel">
            <div class="workflow-copy">
                <h3>See what adds up.</h3>
                <p>Check how much you’ve spent by category, compare it with your monthly limits, and export the period you need.</p>
            </div>
            <div class="workflow-visual" aria-hidden="true">
                <div class="sample-details sample-breakdown">
                    <strong>Spending by category</strong>
                    <div><span>Food &amp; Dining</span><strong>₹3,240</strong><progress value="3240" max="8420"></progress></div>
                    <div><span>Shopping</span><strong>₹2,180</strong><progress value="2180" max="8420"></progress></div>
                    <div><span>Travel</span><strong>₹1,480</strong><progress value="1480" max="8420"></progress></div>
                </div>
            </div>
        </article>
    </div>
</section>

<section class="home-feature-band" id="features" aria-labelledby="features-heading">
    <div class="home-width home-section">
        <header class="home-section-heading" data-reveal>
            <h2 id="features-heading">Stay on top of your spending</h2>
            <p>Set limits, find past purchases, and take your records with you.</p>
        </header>
        <div class="home-feature-grid" data-reveal>
            <article class="home-feature home-feature-budget">
                <span class="preview-icon"><ui:public-icon name="pie"/></span>
                <h3>Budgets you can check at a glance</h3>
                <p>Set an overall or category limit. See what’s left and get a warning as you approach your threshold.</p>
                <div class="budget-illustration" aria-hidden="true">
                    <div class="budget-illustration-heading"><strong>Monthly budget</strong><span>On track</span></div>
                    <div class="budget-illustration-amount">₹8,420 <small>of ₹12,000</small></div>
                    <progress value="8420" max="12000"></progress>
                    <div class="budget-illustration-footer"><span>70% used</span><strong>₹3,580 left</strong></div>
                </div>
            </article>
            <article class="home-feature home-feature-search">
                <span class="preview-icon"><ui:public-icon name="search"/></span>
                <h3>Find any expense</h3>
                <p>Search by merchant or note, then narrow your results by date, category, payment, or receipt status.</p>
                <div class="example-filters" aria-hidden="true"><span>This month</span><span>Food &amp; Dining</span><span>Has a receipt</span></div>
            </article>
            <article class="home-feature home-feature-export">
                <span class="preview-icon"><ui:public-icon name="file"/></span>
                <h3>Export what you need</h3>
                <p>Save a PDF summary or download a CSV of transactions for your spreadsheet.</p>
                <div class="example-filters" aria-hidden="true"><span>PDF summary</span><span>CSV transactions</span></div>
            </article>
        </div>
    </div>
</section>

<section class="home-width home-privacy" id="security" aria-labelledby="privacy-heading" data-reveal>
    <div>
        <span class="preview-icon"><ui:public-icon name="shield"/></span>
        <h2 id="privacy-heading">Your records stay private.</h2>
    </div>
    <div class="privacy-details">
        <div><h3>Account-based access</h3><p>Supabase Auth and row-level security scope expenses and budgets to your account.</p></div>
        <div><h3>Private receipt storage</h3><p>Open receipts from your signed-in workspace alongside the expense they belong to.</p></div>
    </div>
</section>

<section class="home-width home-section home-faq" id="faq" aria-labelledby="faq-heading">
    <header class="home-section-heading" data-reveal><h2 id="faq-heading">Frequently asked questions</h2></header>
    <div class="home-questions" data-reveal>
        <details>
            <summary>Can I add an expense without a receipt?</summary>
            <p>Yes. Enter the merchant, date, amount, category, and payment method yourself. Attaching a receipt is optional.</p>
        </details>
        <details>
            <summary>What happens when I scan a receipt?</summary>
            <p>The scanner suggests details from a JPG or PNG. You can review and correct the suggestions before saving the expense.</p>
        </details>
        <details>
            <summary>Can I set separate budgets for each category?</summary>
            <p>Yes. Set individual limits for categories such as food or travel, or add an overall monthly limit for all spending.</p>
        </details>
        <details>
            <summary>How can I download my expenses?</summary>
            <p>Open Reports, select a date range, and choose CSV or PDF. You can also export your full expense history from Settings.</p>
        </details>
    </div>
</section>

<section class="home-width home-final" aria-labelledby="final-heading" data-reveal>
    <div class="home-final-copy">
        <h2 id="final-heading">Make room for <em>better money habits.</em></h2>
        <p>Log your first expense, set a monthly limit, and see where your money goes.</p>
        <div class="home-actions">
            <c:choose>
                <c:when test="${signedIn}">
                    <a class="home-button" href="${base}/dashboard">Open your dashboard <ui:public-icon name="arrow-right"/></a>
                    <a class="home-button home-button-outline" href="${base}/expense/new">Add an expense</a>
                </c:when>
                <c:otherwise>
                    <a class="home-button" href="${base}/register">Create an account <ui:public-icon name="arrow-right"/></a>
                    <a class="home-button home-button-outline" href="${base}/signin">Log in</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
    <div class="home-final-visual" aria-hidden="true">
        <div class="final-card final-card-budget" data-float>
            <div class="final-card-top"><strong>Monthly budget</strong><span>On track</span></div>
            <div class="final-card-amount">₹8,420 <small>of ₹12,000</small></div>
            <div class="final-card-bar"><i></i></div>
            <div class="final-card-foot"><span>70% used</span><strong>₹3,580 left</strong></div>
        </div>
        <div class="final-card final-card-receipt" data-float>
            <span class="final-card-icon"><ui:public-icon name="receipt"/></span>
            <div><strong>Corner Café</strong><small>Food &amp; Dining · UPI</small></div>
            <b>₹280</b>
        </div>
        <div class="final-card final-card-toast" data-float>
            <span class="final-card-icon"><ui:public-icon name="scan"/></span>
            <div><strong>Receipt scanned</strong><small>Details ready to review</small></div>
        </div>
    </div>
</section>
