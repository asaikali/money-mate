# OBP API Testing Collection

HTTP request collection for testing Open Bank Project (OBP) API against both local development instance and public sandbox.

## Quick Start

### 1. Select Your Environment

In IntelliJ IDEA (or JetBrains IDE):
1. Open any `.http` file in this directory
2. Look for the environment dropdown (usually top-right of the editor)
3. Select either:
   - **`local`** - Your local OBP instance (http://localhost:8081)
   - **`public-sandbox`** - OBP public sandbox (https://apisandbox.openbankproject.com)

### 2. Authenticate

**Open `00-auth.http` and run the "DirectLogin - Get Token" request**

This authenticates you and saves the token globally. The token will be available in **all other files** using the `{{token}}` variable.

✅ **You only need to authenticate ONCE per session**

### 3. Use Any Other File

Once authenticated, you can use any other file. The numbered prefixes show the recommended order:

- `00-auth.http` - **Start here** (authenticate first)
- `01-banks.http` - Create and list banks
- `02-users.http` - Create users
- `03-accounts.http` - Discover and create accounts
- `04-transactions.http` - Query transactions
- `05-admin.http` - Grant roles and manage permissions

## How Token Sharing Works

**Q: Do I need to authenticate in each file?**

**A: No!** IntelliJ HTTP Client shares global variables across ALL `.http` files in your project.

When you run authentication in `00-auth.http`:
```javascript
> {%
  client.global.set("token", response.body.token);
%}
```

The `{{token}}` variable becomes available in **every other file** automatically.

**Benefits:**
- ✅ Authenticate once, use everywhere
- ✅ No need to copy/paste tokens
- ✅ Easy to re-authenticate (just re-run 00-auth.http)
- ✅ Each file focuses on its domain

## File Organization

```
obp-api/
├── README.md                       # This file
│
├── http-client.env.json            # Public config (git tracked)
├── http-client.private.env.json   # Secrets (git ignored)
│
├── 00-auth.http                    # Authentication (RUN FIRST)
├── 01-banks.http                   # Bank operations
├── 02-users.http                   # User management
├── 03-accounts.http                # Account operations
├── 04-transactions.http            # Transaction queries
└── 05-admin.http                   # Admin/roles
```

## Typical Workflows

### Complete Setup (Local Instance)

1. **Authenticate**: Run `00-auth.http` → DirectLogin
2. **Grant Admin Roles**: Run `05-admin.http` → Grant CanCreateBank and CanCreateAccount
3. **Create Bank**: Run `01-banks.http` → Create a New Bank
4. **Create Users**: Run `02-users.http` → Create Alice and Bob
5. **Create Accounts**: Run `03-accounts.http` → Create accounts for Alice and Bob

### Explore Existing Data (Public Sandbox)

1. **Authenticate**: Run `00-auth.http` → DirectLogin
2. **Discover Accounts**: Run `03-accounts.http` → List ALL My Accounts
3. **View Transactions**: Run `04-transactions.http` → Get Account Transactions

## Environment Configuration

### Environments

| Environment | Base URL | Version | Purpose |
|------------|----------|---------|---------|
| `local` | http://localhost:8081 | v5.0.0 | Local development instance |
| `public-sandbox` | https://apisandbox.openbankproject.com | v5.1.0 | OBP public sandbox |

### Variables

Global variables automatically available in all files (set by `00-auth.http`):

| Variable | Set By | Description |
|----------|--------|-------------|
| `{{token}}` | 00-auth.http | DirectLogin authentication token |
| `{{admin_user_id}}` | 00-auth.http | Current user's ID |
| `{{bank_id}}` | 01-banks.http | Created bank ID |
| `{{alice_user_id}}` | 02-users.http | Alice's user ID |
| `{{bob_user_id}}` | 02-users.http | Bob's user ID |
| `{{account_bank_id}}` | 03-accounts.http | Auto-saved from first account |
| `{{account_id}}` | 03-accounts.http | Auto-saved account ID |
| `{{view_id}}` | 03-accounts.http | Account view (usually "owner") |

Environment-specific variables (from `http-client.env.json`):

| Variable | Description | Example |
|----------|-------------|---------|
| `{{host}}` | OBP API base URL | http://localhost:8081 |
| `{{api_version}}` | API version | v5.0.0 |
| `{{admin_username}}` | Admin username | admin@example.com |
| `{{admin_password}}` | Admin password (from private file) | admin123 |
| `{{consumer_key}}` | Consumer key (from private file) | abc123... |

## Important Notes

### DirectLogin Endpoint is Unversioned

The `/my/logins/direct` endpoint is **NOT versioned**. It's accessed directly:
```
POST {{host}}/my/logins/direct
```

**NOT:**
```
POST {{host}}/obp/{{api_version}}/my/logins/direct  ❌ WRONG
```

All other endpoints use the versioned format: `{{host}}/obp/{{api_version}}/...`

### Authentication Header Formats

**For initial authentication (getting token):**
```http
Authorization: DirectLogin username={{admin_username}},password={{admin_password}},consumer_key={{consumer_key}}
```

**For authenticated requests (using token):**
```http
Authorization: DirectLogin token={{token}}
```

### Creating Resources Requires Roles

Some operations require specific roles:

| Operation | Required Role | Grant In |
|-----------|---------------|----------|
| Create Bank | `CanCreateBank` | 05-admin.http |
| Create Account | `CanCreateAccount` | 05-admin.http |
| Import Sandbox Data | `CanCreateSandbox` | 05-admin.http |

## Troubleshooting

### "Missing token" error
**Solution:** Run `00-auth.http` first to authenticate and save the token globally.

### "Missing role" error
**Solution:** Run the appropriate request in `05-admin.http` to grant the required role.

### Variables not working
**Solution:**
1. Check that you've selected an environment (local or public-sandbox)
2. Verify `http-client.env.json` and `http-client.private.env.json` are in this directory
3. Make sure you're using the correct variable syntax: `{{variable_name}}`

### 404 on versioned endpoint
**Solution:**
- Local instance uses v5.0.0 (switch to `local` environment)
- Public sandbox uses v5.1.0 (switch to `public-sandbox` environment)

### Token expired
**Solution:** Re-run the "DirectLogin - Get Token" request in `00-auth.http`

## Security

- **`http-client.private.env.json`** contains secrets (passwords, keys) and is git-ignored
- Never commit credentials to version control
- Use different credentials for dev vs production environments
- Rotate credentials regularly

## Tips

- **Run requests in order** - Variables are often set by earlier requests
- **Check response scripts** - Many requests save useful variables for later use
- **Use auto-complete** - Type `{{` to see available variables
- **View global variables** - In IntelliJ: Tools → HTTP Client → Show HTTP Requests History
- **Token stays valid** - No need to re-authenticate between files in same session

## Related Documentation

- [OBP API Documentation](https://apiexplorersandbox.openbankproject.com/)
- [DirectLogin Documentation](https://github.com/OpenBankProject/OBP-API/wiki/Direct-Login)
- [IntelliJ HTTP Client Guide](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html)
