<%@ page pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="ui" tagdir="/WEB-INF/tags" %>
<c:set var="signingIn" value="${authAction eq 'signin'}" />
<c:set var="registering" value="${authAction eq 'register'}" />
<c:set var="recovering" value="${authAction eq 'recovery'}" />
<div class="home-width auth-scene">
    <aside class="auth-story" aria-label="About Expense Tracker" data-motion="auth-story">
        <div class="auth-story-intro">
            <p class="auth-story-eyebrow"><ui:public-icon name="shield"/> Your private workspace</p>
            <p class="auth-story-heading">
                <c:choose>
                    <c:when test="${signingIn}">Welcome back. <em>Pick up where you left off.</em></c:when>
                    <c:when test="${registering}">Every expense, <em>in one place.</em></c:when>
                    <c:otherwise>Locked out? <em>Let’s get you back in.</em></c:otherwise>
                </c:choose>
            </p>
        </div>
        <ul class="auth-perks">
            <li><span class="auth-perk-icon"><ui:public-icon name="scan"/></span>
                <div><strong>Scan receipts</strong><span>Upload a photo and review the merchant, date, and amount.</span></div></li>
            <li><span class="auth-perk-icon"><ui:public-icon name="pie"/></span>
                <div><strong>Set monthly budgets</strong><span>See what’s left in each category before you overspend.</span></div></li>
            <li><span class="auth-perk-icon"><ui:public-icon name="file"/></span>
                <div><strong>Export anytime</strong><span>Download a PDF summary or a CSV of your transactions.</span></div></li>
        </ul>
        <div class="auth-budget" aria-hidden="true">
            <div class="auth-budget-top"><strong>Monthly budget</strong><span>On track</span></div>
            <div class="auth-budget-amount">₹8,420 <small>of ₹12,000</small></div>
            <div class="auth-budget-bar"><span></span></div>
            <div class="auth-budget-foot"><span>70% used</span><span>₹3,580 left</span></div>
        </div>
    </aside>
    <div class="auth-card-column">
        <a class="home-brand auth-brand" href="${base}/"><img src="${assets}/app-logo.png" alt="" width="40" height="40">Expense Tracker</a>
        <section class="credential-card" aria-labelledby="credentials-heading" data-motion="auth-card">
            <header class="credential-heading">
                <h1 id="credentials-heading"><c:out value="${signingIn ? 'Log in' : title}"/></h1>
                <c:choose>
                    <c:when test="${signingIn}"><p>Log in to your Expense Tracker workspace.</p></c:when>
                    <c:when test="${registering}"><p>Start tracking your expenses and budgets.</p></c:when>
                    <c:when test="${not recovering}"><p>Enter your email to request a reset link.</p></c:when>
                </c:choose>
            </header>
            <c:if test="${not empty notice}">
                <div class="notice ${notice.type}" role="${notice.type eq 'error' ? 'alert' : 'status'}"><c:out value="${notice.text}"/></div>
            </c:if>
            <c:choose>
                <c:when test="${not cloud and not signingIn and not registering}">
                    <p class="muted">Cloud accounts are unavailable. Configure Supabase to continue.</p>
                </c:when>
                <c:when test="${recovering and not canReset}">
                    <p class="muted">Request a new reset email and open it in the same browser.</p>
                    <a class="home-button credential-submit" href="${base}/forgot">Request a reset link</a>
                </c:when>
                <c:otherwise>
                    <form method="post" action="${base}/${authAction}" class="credential-form">
                        <ui:csrf/>
                        <c:if test="${not recovering}">
                            <div class="credential-group">
                                <label for="account-email">Email address</label>
                                <div class="credential-field">
                                    <ui:public-icon name="mail"/>
                                    <input id="account-email" type="email" name="email" value="<c:out value='${registrationEmail}'/>"
                                           placeholder="Enter your email" autocomplete="email" required maxlength="254">
                                </div>
                            </div>
                        </c:if>
                        <c:if test="${authAction ne 'forgot'}">
                            <div class="credential-group">
                                <label for="account-password">${recovering ? 'New password' : 'Password'}</label>
                                <div class="credential-field">
                                    <ui:public-icon name="lock"/>
                                    <input id="account-password" type="password" name="password"
                                           placeholder="${signingIn ? 'Enter your password' : 'Create a password'}"
                                           autocomplete="${signingIn ? 'current-password' : 'new-password'}"
                                           required minlength="${signingIn ? '1' : '6'}" maxlength="1024">
                                    <button class="credential-toggle" type="button" data-password-toggle="account-password"
                                            aria-controls="account-password" aria-label="Show password" aria-pressed="false"><ui:public-icon name="eye-off"/></button>
                                </div>
                            </div>
                        </c:if>
                        <c:if test="${recovering}">
                            <div class="credential-group">
                                <label for="confirm-password">Confirm password</label>
                                <div class="credential-field"><ui:public-icon name="lock"/>
                                    <input id="confirm-password" type="password" name="confirmPassword" placeholder="Repeat your new password"
                                           autocomplete="new-password" required minlength="6" maxlength="1024">
                                </div>
                            </div>
                        </c:if>
                        <c:if test="${signingIn}"><a class="credential-forgot" href="${base}/forgot">Forgot password?</a></c:if>
                        <button type="submit" class="home-button credential-submit">
                            ${authAction eq 'forgot' ? 'Send reset email' : recovering ? 'Update password' : signingIn ? 'Log in' : 'Create account'}
                        </button>
                    </form>
                </c:otherwise>
            </c:choose>
            <c:if test="${cloud and (signingIn or registering)}">
                <div class="credential-divider" aria-hidden="true">or</div>
                <a class="home-button home-button-outline credential-google" href="${base}/auth/google"><img src="${assets}/icons/google.svg" alt="" width="18" height="18"> Continue with Google</a>
            </c:if>
            <div class="credential-links">
                <c:choose>
                    <c:when test="${signingIn}"><span>New here?</span> <a href="${base}/register">Create an account</a></c:when>
                    <c:when test="${registering}"><span>Already have an account?</span> <a href="${base}/signin">Log in</a></c:when>
                    <c:otherwise><a href="${base}/signin">Back to log in</a></c:otherwise>
                </c:choose>
            </div>
            <c:if test="${not cloud and (signingIn or registering)}"><p class="credential-status">Cloud service not configured</p></c:if>
        </section>
    </div>
</div>
