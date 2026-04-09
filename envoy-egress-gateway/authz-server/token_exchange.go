package main

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/url"
	"strings"
)

const (
	tokenExchangeGrantType = "urn:ietf:params:oauth:grant-type:token-exchange"
	accessTokenType        = "urn:ietf:params:oauth:token-type:access_token"
)

type TokenExchanger interface {
	ExchangeForOBPToken(ctx context.Context, subjectToken string) (string, error)
}

type IdentityBrokerTokenExchanger struct {
	client *http.Client
	config IdentityBrokerConfig
}

func NewIdentityBrokerTokenExchanger(client *http.Client, config IdentityBrokerConfig) *IdentityBrokerTokenExchanger {
	return &IdentityBrokerTokenExchanger{
		client: client,
		config: config,
	}
}

func (e *IdentityBrokerTokenExchanger) ExchangeForOBPToken(ctx context.Context, subjectToken string) (string, error) {
	log.Printf(
		"TOKEN_EXCHANGE: preparing request token_uri=%s client_id=%s subject_token_preview=%s",
		e.config.TokenURI,
		e.config.TokenExchangeClientID,
		previewToken(subjectToken),
	)
	form := url.Values{}
	// Ask identity-broker to swap the caller's JWT for a downstream access token.
	// In this demo the downstream token is the OBP DirectLogin token we need to
	// send to the sandbox.
	form.Set("grant_type", tokenExchangeGrantType)
	form.Set("subject_token", subjectToken)
	form.Set("subject_token_type", accessTokenType)
	form.Set("requested_token_type", accessTokenType)

	request, err := http.NewRequestWithContext(ctx, http.MethodPost, e.config.TokenURI, strings.NewReader(form.Encode()))
	if err != nil {
		return "", fmt.Errorf("build token exchange request: %w", err)
	}

	request.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	// ext_authz authenticates to identity-broker as the confidential token-exchange client.
	request.SetBasicAuth(e.config.TokenExchangeClientID, e.config.TokenExchangeClientSecret)
	log.Printf("TOKEN_EXCHANGE: calling identity broker token endpoint %s", e.config.TokenURI)

	response, err := e.client.Do(request)
	if err != nil {
		return "", fmt.Errorf("call identity broker token endpoint: %w", err)
	}
	defer response.Body.Close()

	body, err := io.ReadAll(response.Body)
	if err != nil {
		return "", fmt.Errorf("read identity broker response: %w", err)
	}
	log.Printf("TOKEN_EXCHANGE: received status=%d body_preview=%s", response.StatusCode, previewBody(body))

	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return "", fmt.Errorf("identity broker token exchange failed with status %d body=%s", response.StatusCode, previewBody(body))
	}

	var payload struct {
		AccessToken string `json:"access_token"`
	}
	if err := json.Unmarshal(body, &payload); err != nil {
		return "", fmt.Errorf("decode identity broker response: %w", err)
	}
	if payload.AccessToken == "" {
		return "", fmt.Errorf("identity broker response did not include access_token")
	}
	// The broker returns the OBP DirectLogin token in access_token so the caller
	// can stay OAuth-shaped while Envoy remains responsible for the final header rewrite.
	log.Printf("TOKEN_EXCHANGE: parsed access_token preview=%s", previewToken(payload.AccessToken))

	return payload.AccessToken, nil
}

func previewBody(body []byte) string {
	text := strings.TrimSpace(string(body))
	if text == "" {
		return "<empty>"
	}
	if len(text) <= 160 {
		return text
	}
	return text[:160] + "..."
}
