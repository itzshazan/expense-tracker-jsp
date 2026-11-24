# Expense Tracker JSP

A browser version of Expense Tracker Desktop using Jakarta Servlets, JSP and JSTL. It reuses the desktop project's Java expense, budget, report, receipt, OCR and Supabase logic. It works with the original Supabase schema and records.

## Run locally

Use **JDK 19 or newer** and open PowerShell in this folder.

```powershell
.\run-web.ps1
```

Open [http://127.0.0.1:8080/](http://127.0.0.1:8080/) and sign in with your Supabase account. See the configuration section below. The launcher builds the WAR with the included Maven wrapper, downloads and verifies a Tomcat 10.1 distribution when needed, and binds the server to local loopback. Press Ctrl+C in the terminal to stop it.

To start from the included WAR, run `.\run-web.ps1 -SkipBuild`.

Use `.\run-web.ps1 -Port 8090` to change the local port, or pass `-TomcatHome` to use a Tomcat 10.1 installation you already have.

For packaging or tests without the launcher:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

The deployable WAR is `target/expense-tracker.war`. For a separate Tomcat installation, deploy that WAR and use its context path, typically `/expense-tracker`. The `mvnw` shell wrapper is available on Linux and macOS.

## Use your Supabase data

If the desktop project already has a working configuration, point the launcher to its existing .env file.

```powershell
.\run-web.ps1 -SkipBuild -EnvFile 'C:\path\to\expense-tracker-desktop\.env'
```

A separate Tomcat installation can use the same file by setting `EXPENSE_ENV_FILE` before startup.

1. Use the existing Supabase project, or create a test project and apply `supabase/schema.sql` in its SQL editor.
2. Copy `.env.example` to `.env` in the folder you run from. Enter `SUPABASE_URL` and the public `SUPABASE_ANON_KEY`. You can also set them as environment variables. Use only the public key.
3. Set `EXPENSE_WEB_URL` to the application's public base URL, including the context path if any. Add its `/app/recovery` URL to the Supabase Auth redirect allowlist. The reset link must open in the same browser that requested it.
4. For OCR, install the matching Tesseract language data when needed and set `TESSDATA_PREFIX` to the directory containing the files. JPG and PNG uploads are supported, with a 10 MB default and a 20 MB web request ceiling.

Use HTTPS for any access beyond loopback. Account confirmation, recovery delivery and data access depend on your Supabase Auth and database configuration.

## Enable Google sign-in

1. In Google Auth Platform, create an OAuth client for a **Web application**. Under **Authorized redirect URIs**, enter the exact callback from the Supabase Google provider page, such as `https://your-project-ref.supabase.co/auth/v1/callback`. Leave JavaScript origins blank for this server redirect flow.
2. In Supabase, open **Authentication → Sign In / Providers → Google**. Enable the provider and enter the Google client ID and client secret. Keep the secret in Supabase.
3. In Supabase **Authentication → URL Configuration**, add the exact application callback to **Redirect URLs**. On the local server on port 8096 this is `http://127.0.0.1:8096/app/auth/google/callback`. For a deployed app, append `/app/auth/google/callback` to its configured `EXPENSE_WEB_URL`, including any context path.
4. Open the app on the same origin as the allowed callback and select **Continue with Google**. Supabase handles the Google credentials and the JSP server exchanges the returned code for a session using PKCE.

## Deploy on Render

The `Dockerfile` builds the WAR and runs it on Tomcat 10.1 with Tesseract for receipt scanning. `render.yaml` describes the Render service.

1. Push this folder to a GitHub repository. `.env` is ignored by git and by Docker, so your keys stay local.
2. In Render, choose **New → Blueprint**, pick the repository, and enter `SUPABASE_URL` and `SUPABASE_ANON_KEY` when asked.
3. When the first deploy finishes, note the service address, such as `https://expense-tracker-xxxx.onrender.com`. The app uses it automatically through Render's `RENDER_EXTERNAL_URL`. With a custom domain, set `EXPENSE_WEB_URL` to that domain instead.
4. In Supabase **Authentication → URL Configuration**, set the Site URL to the service address and add these Redirect URLs:
   - `https://<your-service>.onrender.com/app/recovery`
   - `https://<your-service>.onrender.com/app/auth/google/callback`

Things to know about the free plan:

- The service sleeps after about 15 minutes without visits; the next visit takes up to a minute to wake it.
- Sign-ins are kept in server memory, so a redeploy or restart signs everyone out.
- Display preferences (currency, date style, defaults) are saved on the server's disk, which is reset on each deploy. Expenses, budgets and receipts are in Supabase and are not affected.

## Features

- Email and Google sign-in, registration, password recovery, display name updates, password changes and account deletion.
- Dashboard, monthly overall and category budgets, and budget threshold alerts.
- Expense creation, editing, deletion, sorting, and filters for search, date, category, payment and receipt status.
- Receipt upload, preview, replacement, removal, and reviewable OCR suggestions.
- Date-range spending breakdowns, downloadable CSV and PDF, and a printable report.
- Currency and date display, default expense fields, OCR language and automatic scanning preferences.

Expenses, budgets and receipts use Supabase. Display preferences are stored per user under the application server account in Java Preferences.
