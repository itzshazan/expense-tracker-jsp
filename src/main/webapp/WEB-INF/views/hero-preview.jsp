<%@ page pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<%-- Illustration only. hero-preview.js plays it as a looping product demo. --%>
<div class="preview-screen" aria-hidden="true" data-preview>
    <div class="preview-sidebar">
        <span class="preview-brand"><img src="${assets}/app-logo.png" alt="" width="32" height="32"> Expense Tracker</span>
        <ul>
            <li><ui:public-icon name="chart"/> Dashboard</li>
            <li><ui:public-icon name="receipt"/> Expenses</li>
            <li><ui:public-icon name="file"/> Reports</li>
            <li><ui:public-icon name="shield"/> Settings</li>
        </ul>
    </div>
    <div class="preview-workspace" data-preview-workspace>
        <div class="preview-dashboard">
            <div class="preview-dashboard-header"><strong>Dashboard</strong><span class="preview-add" data-preview-add>+ Add expense</span></div>
            <div class="preview-stats">
                <div><span>Spent this month</span><strong data-count="8420" data-next="8720" data-money>₹8,420</strong></div>
                <div><span>Transactions</span><strong data-count="24" data-next="25">24</strong></div>
                <div><span>Budget remaining</span><strong data-count="3580" data-next="3280" data-money>₹3,580</strong></div>
            </div>
            <div class="preview-insights">
                <div class="preview-panel">
                    <strong>Spending trend</strong>
                    <div class="preview-bars">
                        <span><i></i>Week 1</span><span><i></i>Week 2</span><span><i></i>Week 3</span><span><i data-preview-week></i>Week 4</span>
                    </div>
                </div>
                <div class="preview-panel preview-categories">
                    <strong>Top categories</strong>
                    <div><span>Food &amp; Dining</span><span>₹3,240</span><progress value="3240" max="8720"></progress></div>
                    <div><span>Shopping</span><span data-count="2180" data-next="2480" data-money>₹2,180</span><progress value="2180" max="8720" data-preview-shopping></progress></div>
                    <div><span>Travel</span><span>₹1,480</span><progress value="1480" max="8720"></progress></div>
                </div>
            </div>
            <div class="preview-panel preview-transactions">
                <strong>Recent expenses</strong>
                <div class="preview-row-head"><span>Merchant</span><span>Category</span><span>Amount</span></div>
                <div class="preview-rows">
                    <div class="preview-row preview-row-new" data-preview-new><b>Market Studio</b><span>Shopping</span><b>₹300</b></div>
                    <div class="preview-row"><b>Corner Café</b><span>Food &amp; Dining</span><b>₹280</b></div>
                    <div class="preview-row"><b>Metro card</b><span>Travel</span><b>₹450</b></div>
                    <div class="preview-row"><b>Weekend market</b><span>Shopping</span><b>₹620</b></div>
                </div>
            </div>
            <div class="preview-panel preview-budget">
                <strong>Overall budget</strong><span data-preview-used>70% of ₹12,000</span>
                <progress value="8420" max="12000" data-preview-budget></progress>
            </div>
        </div>

        <div class="preview-backdrop" data-preview-backdrop></div>
        <div class="preview-editor" data-preview-editor>
            <div class="preview-editor-title"><strong>Add expense</strong><span>×</span></div>
            <div class="preview-editor-body">
                <div class="preview-receipt" data-preview-receipt>
                    <ui:public-icon name="receipt"/>
                    <strong>Market Studio</strong><span>12 November</span>
                    <div><span>Notebook</span><strong>₹180</strong></div>
                    <div><span>Pen set</span><strong>₹120</strong></div>
                    <div><strong>Total</strong><strong>₹300</strong></div>
                    <i class="preview-scanline" data-preview-scanline></i>
                </div>
                <div class="preview-editor-details">
                    <span class="preview-scan" data-preview-scan><ui:public-icon name="scan"/> Receipt scanned</span>
                    <dl>
                        <dt>Merchant</dt><dd data-type="Market Studio"></dd>
                        <dt>Amount</dt><dd data-type="₹300.00"></dd>
                        <dt>Category</dt><dd data-type="Shopping"></dd>
                        <dt>Payment</dt><dd data-type="UPI"></dd>
                    </dl>
                    <span class="preview-save" data-preview-save>Save expense</span>
                </div>
            </div>
        </div>

        <span class="preview-saved" data-preview-toast><ui:public-icon name="receipt"/> Expense added · ₹300</span>
        <span class="preview-pointer" data-preview-pointer><i></i></span>
    </div>
</div>
