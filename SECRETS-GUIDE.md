# Secrets & Environment Configuration Guide

This project uses multiple approaches for managing secrets and configuration based on their use case.

## Directory Structure

```
money-mate/
├── .secrets/                          # Git-ignored secrets directory
│   ├── README.md                      # Documentation for .secrets/
│   └── .env                          # Environment variables for scripts
│
├── .env.example                       # Template showing required variables
├── http-client.env.json               # Public HTTP client config (git tracked)
├── http-client.private.env.json       # Private HTTP client secrets (git ignored)
│
├── obp-api/                          # HTTP client request files (organized)
│   ├── 00-auth.http                  # Authentication
│   ├── 01-banks.http                 # Bank operations
│   └── ...                           # Other organized .http files
└── sandbox/                          # Local sandbox setup (docker-compose, scripts)
    └── setup-sandbox.sh              # Script that uses .secrets/.env
```

## When to Use Each Approach

### 1. `.secrets/.env` - For Shell Scripts & Applications
**Use for:**
- Environment variables loaded by shell scripts
- Application configuration (Spring Boot, Node.js, etc.)
- CLI tools and automation scripts
- Database connection strings

**Example:**
```bash
# .secrets/.env
OBP_USERNAME=admin@example.com
OBP_PASSWORD=secret123
DATABASE_URL=postgres://user:pass@localhost/db
```

**Usage:**
```bash
# Scripts automatically use .secrets/.env
sandbox/setup-sandbox.sh

# Or specify a different file
sandbox/setup-sandbox.sh .secrets/.env.prod
```

### 2. `http-client.private.env.json` - For HTTP Client (IntelliJ/JetBrains IDEs)
**Use for:**
- Testing APIs with IntelliJ IDEA HTTP Client
- Development/testing of REST endpoints
- Quick API exploration

**Why separate?**
- HTTP Client requires files in project root for IDE integration
- Uses JSON format (not shell syntax)
- Supports multiple environments (dev, staging, prod)

**Example:**
```json
{
  "dev": {
    "consumer_key": "abc123",
    "admin_password": "secret"
  }
}
```

**Usage:**
1. Open any `.http` file in IntelliJ
2. Select environment from dropdown (dev/public-sandbox)
3. Run requests - variables auto-filled from env files

## Setup Instructions

### Initial Setup

1. **Copy the template:**
   ```bash
   cp .env.example .secrets/.env
   ```

2. **Fill in your credentials:**
   ```bash
   nano .secrets/.env  # or use your preferred editor
   ```

3. **For HTTP Client (Optional):**
   - `http-client.private.env.json` already exists with example values
   - Update with your actual credentials if using IntelliJ HTTP Client

### Security Best Practices

✅ **DO:**
- Store all credentials in `.secrets/` or `http-client.private.env.json`
- Use `.env.example` as documentation for required variables
- Rotate credentials regularly
- Use different credentials for dev/staging/production

❌ **DON'T:**
- Commit `.secrets/` directory to git
- Commit `http-client.private.env.json` to git
- Share credentials via email/Slack
- Hard-code credentials in application code

## File Locations Quick Reference

| File Type | Location | Git Tracked | Purpose |
|-----------|----------|-------------|---------|
| Shell script secrets | `.secrets/.env` | ❌ No | Used by `sandbox/setup-sandbox.sh` |
| HTTP Client secrets | `http-client.private.env.json` | ❌ No | Used by `.http` files in IDE |
| HTTP Client public config | `http-client.env.json` | ✅ Yes | Non-sensitive HTTP Client config |
| Template/Documentation | `.env.example` | ✅ Yes | Shows what variables are needed |
| Secrets documentation | `.secrets/README.md` | ✅ Yes | Explains `.secrets/` directory |

## Sharing Secrets with Team

**Never commit secrets to git!** Instead:

1. **Password Manager** (Recommended)
   - Store in 1Password, LastPass, etc.
   - Share vault with team members

2. **Secrets Management Service**
   - AWS Secrets Manager
   - HashiCorp Vault
   - Google Secret Manager

3. **Secure File Transfer**
   - Encrypted email attachment
   - Secure file sharing service
   - In-person transfer

## Troubleshooting

### "Env file not found" error
```bash
# Solution: Make sure .secrets/.env exists
cp .env.example .secrets/.env
nano .secrets/.env  # Fill in values
```

### HTTP Client variables not working
```bash
# Solution: Check file is in project root
ls -la http-client.private.env.json

# Make sure you selected an environment in the IDE dropdown
```

### Want to use different env file
```bash
# Pass custom path to script
sandbox/setup-sandbox.sh .secrets/.env.staging
```
