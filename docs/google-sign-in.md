# Sign in with Google

The login and sign-up pages can show a **Sign in with Google** button next to the
email/password and email OTP options. It only appears once you've given the app a
Google **client ID**, so this is a one-time setup in Google Cloud Console.

You need: a Google account, and about 10 minutes.

## 1. Create a Google Cloud project

1. Go to https://console.cloud.google.com/ and sign in.
2. Click the project picker at the top left (it may say "Select a project") → **New project**.
3. Project name: `Farm Fresh`. Click **Create**, wait a few seconds, then make sure `Farm Fresh` is selected in the project picker.

## 2. Set up the consent screen

This is the "MS Fresh Harvest wants to access your Google account" screen users see.

1. Open the menu (☰) → **APIs & Services** → **OAuth consent screen**. (Newer accounts land on a page called **Google Auth Platform**; click **Get started**.)
2. **App information**: App name `MS Fresh Harvest`, User support email: your Gmail. **Next**.
3. **Audience**: choose **External**. **Next**.
4. **Contact information**: your Gmail. **Next**.
5. Tick the agreement box, then **Continue** → **Create**.
6. While the app is in **Testing**, only test users can sign in. Go to **Audience** → **Test users** → **Add users**, add your own Gmail (and anyone else who'll try it), **Save**.
   When you put the site online for real customers, come back to **Audience** and click **Publish app**. The app only asks for name and email, so Google doesn't need to review it.

## 3. Create the OAuth client ID

1. Go to **Clients** (or **APIs & Services** → **Credentials** → **Create credentials** → **OAuth client ID**).
2. Click **Create client**.
3. **Application type**: **Web application**. Name: `Farm Fresh web`.
4. Under **Authorized JavaScript origins**, click **Add URI** for each of these (exactly, no trailing slash):

   | Origin | Used when |
   | --- | --- |
   | `http://localhost:8081` | running with Docker (`docker-compose up`) |
   | `http://localhost` | Google asks for this too when testing on localhost |
   | `http://localhost:5173` | running the frontend with `npm start` (no Docker) |
   | `https://your-domain.com` | production, once deployed ([deployment.md](deployment.md)) |

5. **Authorized redirect URIs**: leave this **empty**. The button uses a Google popup, which hands the result straight back to the page, so there's no redirect.
6. Click **Create**. Copy the **Client ID**. It looks like `123456789012-abc123def456.apps.googleusercontent.com`.
   You can ignore the client secret; this setup doesn't use it.

Changes to origins can take a few minutes (occasionally longer) to take effect at Google.

## 4. Add it to `.env`

Open `.env` in the project folder and add:

```ini
GOOGLE_CLIENT_ID=123456789012-abc123def456.apps.googleusercontent.com
```

Then restart:

```sh
docker-compose up --build
```

(or restart the backend if you run it without Docker). Open http://localhost:8081, click **Login**, and the Google button appears under the other options.

The client ID is only read by the backend. The frontend asks the backend for it (`GET /api/auth/google/config`), so you don't have to set it twice or rebuild the frontend when it changes.

## How it works

```
Browser                         Google                     Our backend
   │  click "Sign in with Google"  │                            │
   │ ─────────────────────────────►│                            │
   │  ◄── ID token (signed JWT) ───│                            │
   │  POST /api/auth/google {credential: <ID token>} ──────────►│
   │                               │  ◄── public keys (cached) ─│ check signature,
   │                               │                            │ issuer, audience,
   │                               │                            │ expiry, email_verified
   │  ◄──────────────── our own JWT + user, same as password login
```

- A first-time Google user gets an account automatically (name and email from Google; phone and address can be added on the profile page).
- If someone already signed up with the same email and a password, Google logs them into **that same account**. Their password keeps working.
- Google users can still set a password later with **Forgot password**.

## Troubleshooting

| What you see | Fix |
| --- | --- |
| No Google button at all | `GOOGLE_CLIENT_ID` is empty or the backend wasn't restarted. Check `curl http://localhost:8081/api/auth/google/config` shows `"enabled":true`. |
| Popup says **origin_mismatch** / "The given origin is not allowed" | The address in your browser bar isn't in **Authorized JavaScript origins**. Add it exactly (scheme + host + port), wait a few minutes. |
| "Access blocked: app has not completed the Google verification process" | You're not a test user. Add your Gmail under **Audience** → **Test users**, or publish the app. |
| "Invalid Google sign-in" after picking an account | The `GOOGLE_CLIENT_ID` in `.env` doesn't match the client you added the origins to. |
