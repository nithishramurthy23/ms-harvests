# Deploying with HTTPS

This puts the whole stack on one Linux server with a real domain and HTTPS:

```
Internet ──443──► Caddy ──► nginx ──► backend ×2 ──► MySQL
                (HTTPS)                               │
                                              daily backup ──► ./backups
```

- **Caddy** gets a free Let's Encrypt certificate for your domain and renews it automatically. It redirects HTTP to HTTPS and adds security headers (`deploy/Caddyfile`).
- Only ports 80 and 443 are open to the internet. MySQL, the backends and nginx are reachable only inside Docker's network.
- A **backup** container writes a compressed `mysqldump` to `./backups` once a day and keeps 7 days of them.

All of this is in `deploy/docker-compose.prod.yml`, which is layered on top of the normal `docker-compose.yml`.

---

## What you need

1. **A server.** Any Linux VM with at least **2 GB RAM** (4 GB is more comfortable, because the server builds the images itself) and a public IP. For example: a DigitalOcean droplet, AWS Lightsail, Hetzner, or Oracle Cloud's Always Free tier. Pick Ubuntu 24.04.
2. **A domain name** you can add DNS records to. A free subdomain from [DuckDNS](https://www.duckdns.org) works too.
3. **A Gmail app password** for sending emails, and **new secrets**. Don't reuse any password or JWT secret that was ever committed or shared.

---

## Steps

### 1. Point the domain at the server

At your DNS provider, add an **A record**: name `@` (or a subdomain such as `shop`), value = the server's public IP. Check that it resolves before going on:

```bash
dig +short shop.yourdomain.com   # should print the server's IP
```

### 2. Open ports 80 and 443

In your cloud provider's firewall or security group, allow inbound TCP **22, 80, 443** (and UDP 443 for HTTP/3). On the server itself:

```bash
sudo ufw allow OpenSSH && sudo ufw allow 80,443/tcp && sudo ufw allow 443/udp && sudo ufw enable
```

### 3. Install Docker

The production file uses `!reset`, which needs Docker Compose 2.24 or newer. The script below installs the latest version.

```bash
ssh ubuntu@YOUR_SERVER_IP
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && exit   # log out and back in so the group applies
```

On a 2 GB server, add swap so the image builds don't run out of memory:

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### 4. Get the code onto the server

If the repository is public:

```bash
git clone https://github.com/nithishramurthy23/ms-harvests.git && cd ms-harvests
```

If it is private, give the server a read-only **deploy key**:

```bash
ssh-keygen -t ed25519 -f ~/.ssh/ms-harvests -N ""
cat ~/.ssh/ms-harvests.pub
# GitHub → repo → Settings → Deploy keys → Add deploy key → paste it (leave "write access" off)
GIT_SSH_COMMAND="ssh -i ~/.ssh/ms-harvests" git clone git@github.com:nithishramurthy23/ms-harvests.git && cd ms-harvests
```

### 5. Create `.env` with fresh secrets

```bash
cp .env.example .env
nano .env
```

Fill in at least:

| Setting | Value |
|---|---|
| `DOMAIN` | `shop.yourdomain.com` (no `https://`) |
| `DB_PASSWORD` | a long random password: `openssl rand -base64 24` |
| `JWT_SECRET` | a new secret: `openssl rand -base64 64 \| tr -d '\n'` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | the Gmail address and a **new** app password |
| `GOOGLE_CLIENT_ID` | optional, for Sign in with Google. Add `https://` + your domain to the client's **Authorized JavaScript origins** first ([google-sign-in.md](google-sign-in.md)) |

`DB_URL`, `DB_USERNAME` and `CORS_ALLOWED_ORIGINS` are set by the compose files, so you can leave them alone. Then lock the file down: `chmod 600 .env`.

### 6. Start everything

```bash
docker compose -f docker-compose.yml -f deploy/docker-compose.prod.yml up -d --build
```

The first build takes a few minutes. Watch it come up:

```bash
docker compose -f docker-compose.yml -f deploy/docker-compose.prod.yml ps
docker compose -f docker-compose.yml -f deploy/docker-compose.prod.yml logs -f caddy backend
```

When Caddy logs `certificate obtained successfully`, open **https://shop.yourdomain.com**.

> Tip: save typing with `alias dc='docker compose -f docker-compose.yml -f deploy/docker-compose.prod.yml'`.

### 7. Check it

```bash
curl -I https://shop.yourdomain.com                 # 200, with a strict-transport-security header
curl -I http://shop.yourdomain.com                  # 308 redirect to https
```

Then sign up on the site, check that the OTP email arrives, and place a test order.

---

## Day-to-day

**Deploy a new version**

```bash
cd ms-harvests && git pull
docker compose -f docker-compose.yml -f deploy/docker-compose.prod.yml up -d --build
```

Compose rebuilds and restarts only what changed. The backends shut down gracefully, so requests in flight are allowed to finish.

**Backups**

```bash
ls -lh backups/                                   # one file per day
# restore one (this overwrites the current data):
gunzip -c backups/farm_fresh-20261012-0300.sql.gz | \
  docker compose -f docker-compose.yml -f deploy/docker-compose.prod.yml exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" farm_fresh'
```

The backups live on the same server, so they don't protect you if the server itself is lost. Copy them off the server regularly, for example with a nightly `rclone copy backups/ remote:farm-fresh-backups` to Google Drive or S3, or turn on your provider's disk snapshots.

**More backend copies**: `BACKEND_REPLICAS=3` in `.env`, then run the `up -d` command again.

---

## Troubleshooting

- **Caddy can't get a certificate.** Usually the DNS record doesn't point at this server yet, or port 80/443 is blocked. Check `dig +short` and the firewall, then run `docker compose ... restart caddy`. Let's Encrypt limits repeated failures, so fix the cause before retrying many times.
- **Build killed / out of memory.** Add the swap from step 3.
- **Emails don't arrive.** Check `docker compose ... logs backend | grep -i mail`. Gmail needs an *app password* (2-step verification on), not your normal password.
