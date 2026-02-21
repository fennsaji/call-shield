# GitHub Secrets Setup

All secrets are added in one place:
**GitHub → Repository → Settings → Secrets and variables → Actions → New repository secret**

The `ci-pr.yml` workflow (PR checks) requires **no secrets** — it uses stub values for compilation. Only the two deployment workflows need secrets.

---

## Quick reference

| Secret name | Backend deploy | Android deploy | CI/PR |
|---|:---:|:---:|:---:|
| `SUPABASE_PROJECT_REF` | ✅ | ✅ | — |
| `SUPABASE_ACCESS_TOKEN` | ✅ | — | — |
| `SUPABASE_DB_PASSWORD` | ✅ | — | — |
| `SUPABASE_ANON_KEY` | ✅ | ✅ | — |
| `SUPABASE_SERVICE_ROLE_KEY` | ✅ | — | — |
| `GOOGLE_SERVICE_ACCOUNT_KEY` | ✅ | — | — |
| `ANDROID_KEYSTORE_BASE64` | — | ✅ | — |
| `ANDROID_KEYSTORE_PASSWORD` | — | ✅ | — |
| `ANDROID_KEY_ALIAS` | — | ✅ | — |
| `ANDROID_KEY_PASSWORD` | — | ✅ | — |
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | — | ✅ | — |
| `GITHUB_TOKEN` | — | auto | — |

---

## Supabase secrets (shared by both deployment workflows)

### `SUPABASE_PROJECT_REF`
The short alphanumeric ID of your Supabase project.

1. Open [supabase.com](https://supabase.com) → your project
2. Go to **Settings → General**
3. Copy **Reference ID** (looks like `abcdefghijklmnop`)

---

### `SUPABASE_ACCESS_TOKEN`
A personal access token used by the Supabase CLI to authenticate on your behalf.

1. Go to [supabase.com/dashboard/account/tokens](https://supabase.com/dashboard/account/tokens)
2. Click **Generate new token**
3. Give it a name (e.g. `callshield-ci`) and copy the token immediately — it is only shown once

---

### `SUPABASE_DB_PASSWORD`
The PostgreSQL superuser password for your project database.

1. Open your Supabase project → **Settings → Database**
2. Under **Connection info**, click **Reset database password** if you have never saved it
3. Copy the password shown

---

### `SUPABASE_ANON_KEY`
The public anonymous API key — safe to embed in the Android app but still kept out of source control via this secret.

1. Open your Supabase project → **Settings → API**
2. Under **Project API keys**, copy the `anon` `public` key

---

### `SUPABASE_SERVICE_ROLE_KEY`
The service role key — grants full database access, bypasses RLS. Only used by Edge Functions server-side.

1. Open your Supabase project → **Settings → API**
2. Under **Project API keys**, click **Reveal** next to `service_role` and copy it

> Keep this key strictly confidential. Never put it in the Android app.

---

### `GOOGLE_SERVICE_ACCOUNT_KEY`
A Google service account JSON used by the `verify-subscription` Edge Function to call the Google Play Developer API and verify in-app purchases.

1. Open [Google Play Console](https://play.google.com/console) → **Setup → API access**
2. Link your Google Cloud project (or create one)
3. Under **Service accounts**, click **Create new service account**
4. In Google Cloud IAM, grant the service account the **Financial data viewer** role on your Play Console project
5. Back in Play Console, grant the service account **View financial data** permissions
6. In [Google Cloud Console](https://console.cloud.google.com) → IAM → Service Accounts → your account → **Keys → Add key → Create new key → JSON**
7. Download the JSON file
8. Paste the entire JSON content as the secret value (no base64 encoding needed)

---

## Android signing secrets

### Creating the upload keystore (one-time setup)

Run this once on your local machine and **keep the `.jks` file backed up securely**:

```bash
keytool -genkey -v \
  -keystore upload-keystore.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias callshield-upload \
  -dname "CN=CallShield, O=Fenn, C=IN"
```

You will be prompted to set a **store password** and a **key password**. Note both down — they become the `ANDROID_KEYSTORE_PASSWORD` and `ANDROID_KEY_PASSWORD` secrets.

---

### `ANDROID_KEYSTORE_BASE64`
The upload keystore file, base64-encoded so it can be stored as a text secret.

```bash
base64 -i upload-keystore.jks | pbcopy   # macOS — copies to clipboard
# or
base64 -i upload-keystore.jks            # print to terminal, then copy
```

Paste the output (a long single-line or multi-line base64 string) as the secret value.

---

### `ANDROID_KEYSTORE_PASSWORD`
The **store password** you set when running `keytool -genkey` above.

---

### `ANDROID_KEY_ALIAS`
The alias you passed to `keytool` with `-alias`. If you used the command above, this is `callshield-upload`.

---

### `ANDROID_KEY_PASSWORD`
The **key password** you set when running `keytool -genkey`. Often the same as the store password.

---

### `PLAY_STORE_SERVICE_ACCOUNT_JSON`
A Google service account JSON that gives GitHub Actions permission to upload builds to the Play Store.

1. Open [Google Play Console](https://play.google.com/console) → **Setup → API access**
2. Link or create a Google Cloud project
3. Click **Create new service account**
4. In Google Cloud IAM, grant it no extra roles (Play Console manages permissions separately)
5. Back in Play Console, grant the service account **Release manager** (or at minimum **Release to internal testing**) access under **Users and permissions**
6. In [Google Cloud Console](https://console.cloud.google.com) → IAM → Service Accounts → your account → **Keys → Add key → Create new key → JSON**
7. Download the JSON file
8. Paste the **entire JSON content** as the secret value

> This is a different service account from `GOOGLE_SERVICE_ACCOUNT_KEY` above — one is for the Play Developer API (purchase verification), the other is for uploading APKs/AABs.

---

### `GITHUB_TOKEN`
**Automatically provided by GitHub Actions** — no setup required. It is used to create GitHub Releases. Do not add this manually.

---

## GitHub Actions environment (backend deploy only)

The backend deploy workflow uses `environment: production`. Create this environment in GitHub so you can add environment-level protection rules (e.g. require a manual approval before deploying to production):

1. GitHub → Repository → **Settings → Environments → New environment**
2. Name it `production`
3. Optionally add **Required reviewers** and **Deployment branches** (restrict to `main`)

All secrets above work as **repository secrets** — you do not need to duplicate them as environment secrets unless you want environment-specific values.

---

## Local development

For local development, copy `android/local.properties.example` to `android/local.properties` and `supabase/.env.example` to `supabase/.env`, then fill in the values. Both files are gitignored and never committed.
