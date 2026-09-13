# To-Do Tracker

A single-file, mobile-friendly to-do list that lives entirely in your browser and
syncs across devices (phone, home laptop, office laptop — no installs) by storing
its data in a private GitHub Gist.

**Live page:** hosted from this repo via GitHub Pages — see "Deploying" below for
the exact URL once Pages is enabled.

## ⚠️ Security note (read this)

This is a **quick personal tool, not something to share or use for sensitive data.**

- You'll paste a GitHub Personal Access Token (PAT) with `gist` scope into the page
  once per device. It is saved in that browser's **local storage**, tied to this
  page's origin.
- The token is **not** baked into `index.html` — it never gets committed to git or
  shown in "View Source" — but it **is** sitting in plain text in your browser's
  storage, and anyone with access to that browser/device (or who opens DevTools →
  Application → Local Storage) can read it and use it to read/write your gists.
- Treat it like a password: only enter it on devices you personally control, and
  only ever grant it the `gist` scope (nothing else) so a leak is limited to your
  gists.
- The to-do data itself is stored in a **private** gist (`todo-tracker-data.json`),
  but private gists are only "unlisted," not encrypted — don't put anything truly
  sensitive in your task titles.
- You can revoke the token at any time from GitHub (Settings → Developer settings
  → Personal access tokens) and generate a new one if you ever suspect it leaked.

## How it works

- On first load (per device), the page asks for a GitHub PAT and saves it locally.
- It then looks through your gists for one containing a file named
  `todo-tracker-data.json`. If it doesn't find one, it creates a new **private**
  gist and seeds it with your starting tasks.
- Because every device uses the *same* GitHub account/token, they all discover the
  *same* gist automatically — there's nothing to copy between devices besides the
  token itself.
- Every change (add, check off, delete) immediately `PATCH`es the gist. Every page
  load/refresh re-fetches the gist so you always see the latest state.
- Checking a task off moves it to **Done** (it isn't deleted) — use the ✕ button to
  actually delete a task.
- Open tasks with a due date in the past are highlighted in red; tasks due today
  are highlighted amber.

## Generating a Personal Access Token (classic, `gist` scope)

1. Go to **github.com → your profile photo (top right) → Settings**.
2. Scroll to the bottom of the left sidebar → **Developer settings**.
3. **Personal access tokens → Tokens (classic)** → **Generate new token →
   Generate new token (classic)**.
   - Fine-grained tokens don't currently support the Gist API, so use a
     **classic** token for this tool.
4. Give it a note like `todo-tracker` so you remember what it's for.
5. Set an expiration (30/60/90 days, or a custom date/"No expiration" if you'd
   rather not re-generate it periodically — shorter is safer).
6. Under **Select scopes**, check only **`gist`** (nothing else needed).
7. Click **Generate token** at the bottom.
8. Copy the token (starts with `ghp_…`) — GitHub only shows it once. Paste it into
   the to-do tracker page when it asks, on each device you use.

If you ever lose the token or want to rotate it, generate a new one the same way
and paste it into the tracker's **⚙ Settings** panel on each device (or delete the
old token from GitHub's token list to revoke it).

## Deploying / GitHub Pages

This repo is set up to be served with GitHub Pages directly from this branch:

1. In the GitHub repo, go to **Settings → Pages**.
2. Under **Build and deployment → Source**, choose **Deploy from a branch**.
3. Under **Branch**, pick this branch and folder **/ (root)**, then **Save**.
4. GitHub will publish the page at `https://<username>.github.io/<repo>/`
   (usually live within a minute or two).

## Local development

It's a single static file — just open `index.html` in a browser, or serve the
folder with any static file server. No build step, no dependencies.
