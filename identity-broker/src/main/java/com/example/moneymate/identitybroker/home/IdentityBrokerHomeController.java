package com.example.moneymate.identitybroker.home;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.util.stream.Collectors;

@RestController
public class IdentityBrokerHomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home(Authentication authentication) {
        String username = authentication.getName();
        String authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .sorted()
            .collect(Collectors.joining(", "));
        String principalType = authentication.getPrincipal().getClass().getSimpleName();
        String principalSummary = describePrincipal(authentication.getPrincipal());

        return """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Identity broker</title>
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; margin: 2rem; color: #0f172a; }
                h1 { margin-bottom: 1rem; }
                dl { display: grid; grid-template-columns: 220px 1fr; gap: .5rem 1rem; max-width: 900px; }
                dt { font-weight: 600; color: #334155; }
                dd { margin: 0; }
                .card { padding: 1rem 1.25rem; border: 1px solid #cbd5e1; border-radius: 12px; background: #f8fafc; }
              </style>
            </head>
            <body>
              <h1>Identity broker</h1>
              <p>Login successful.</p>
              <div class="card">
                <dl>
                  <dt>Username</dt>
                  <dd>%s</dd>
                  <dt>Authorities</dt>
                  <dd>%s</dd>
                  <dt>Authentication Type</dt>
                  <dd>%s</dd>
                  <dt>Principal Details</dt>
                  <dd>%s</dd>
                </dl>
              </div>
            </body>
            </html>
            """.formatted(
            HtmlUtils.htmlEscape(username),
            HtmlUtils.htmlEscape(authorities),
            HtmlUtils.htmlEscape(principalType),
            HtmlUtils.htmlEscape(principalSummary)
        );
    }

    private static String describePrincipal(Object principal) {
        if (principal instanceof OidcUser oidcUser) {
            return "OIDC subject=" + oidcUser.getSubject();
        }
        if (principal instanceof UserDetails userDetails) {
            return "UserDetails username=" + userDetails.getUsername();
        }
        return String.valueOf(principal);
    }
}
