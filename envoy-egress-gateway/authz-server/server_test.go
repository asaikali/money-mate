package main

import (
	"context"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
	"time"

	authv3 "github.com/envoyproxy/go-control-plane/envoy/service/auth/v3"
	typev3 "github.com/envoyproxy/go-control-plane/envoy/type/v3"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/metadata"
)

type fakeExchanger struct {
	token string
	err   error
}

func (f fakeExchanger) ExchangeForOBPToken(ctx context.Context, subjectToken string) (string, error) {
	if f.err != nil {
		return "", f.err
	}
	return f.token, nil
}

func TestLoadConfig(t *testing.T) {
	tempDir := t.TempDir()
	configPath := filepath.Join(tempDir, "config.yaml")
	configYAML := []byte(`
server:
  listen_address: 127.0.0.1:10005
  shared_secret: test-secret
  timeout: 3s
identity_broker:
  token_uri: http://localhost:9000/oauth2/token
  token_exchange_client_id: demo-client
  token_exchange_client_secret: demo-secret
`)
	if err := os.WriteFile(configPath, configYAML, 0o600); err != nil {
		t.Fatalf("write config: %v", err)
	}

	config, err := LoadConfig(configPath)
	if err != nil {
		t.Fatalf("LoadConfig returned error: %v", err)
	}

	if config.Server.ListenAddress != "127.0.0.1:10005" {
		t.Fatalf("unexpected listen address: %s", config.Server.ListenAddress)
	}
	if config.Server.Timeout != 3*time.Second {
		t.Fatalf("unexpected timeout: %s", config.Server.Timeout)
	}
	if config.IdentityBroker.TokenExchangeClientID != "demo-client" {
		t.Fatalf("unexpected client id: %s", config.IdentityBroker.TokenExchangeClientID)
	}
}

func TestExchangeForOBPToken(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		if request.URL.Path != "/oauth2/token" {
			t.Fatalf("unexpected path: %s", request.URL.Path)
		}
		if got := request.Header.Get("Authorization"); got == "" {
			t.Fatalf("expected basic auth header")
		}
		if err := request.ParseForm(); err != nil {
			t.Fatalf("parse form: %v", err)
		}
		if got := request.Form.Get("subject_token"); got != "subject-token" {
			t.Fatalf("unexpected subject_token: %s", got)
		}
		writer.Header().Set("Content-Type", "application/json")
		_, _ = writer.Write([]byte(`{"access_token":"obp-token"}`))
	}))
	defer server.Close()

	exchanger := NewIdentityBrokerTokenExchanger(&http.Client{Timeout: time.Second}, IdentityBrokerConfig{
		TokenURI:                  server.URL + "/oauth2/token",
		TokenExchangeClientID:     "client-id",
		TokenExchangeClientSecret: "client-secret",
	})

	token, err := exchanger.ExchangeForOBPToken(context.Background(), "subject-token")
	if err != nil {
		t.Fatalf("ExchangeForOBPToken returned error: %v", err)
	}
	if token != "obp-token" {
		t.Fatalf("unexpected token: %s", token)
	}
}

func TestCheckRewritesAuthorizationHeader(t *testing.T) {
	config := Config{
		Server: ServerConfig{
			SharedSecret: "test-secret",
		},
	}
	server := NewAuthServer(config, fakeExchanger{token: "obp-token"})
	ctx := metadata.NewIncomingContext(context.Background(), metadata.Pairs("x-authz-secret", "test-secret"))

	response, err := server.Check(ctx, &authv3.CheckRequest{
		Attributes: &authv3.AttributeContext{
			Request: &authv3.AttributeContext_Request{
				Http: &authv3.AttributeContext_HttpRequest{
					Headers: map[string]string{
						"authorization": "Bearer subject-token",
					},
				},
			},
		},
	})
	if err != nil {
		t.Fatalf("Check returned error: %v", err)
	}
	if response.GetStatus().GetCode() != int32(codes.OK) {
		t.Fatalf("expected OK status, got %d", response.GetStatus().GetCode())
	}
	headers := response.GetOkResponse().GetHeaders()
	if len(headers) != 1 {
		t.Fatalf("expected one header override, got %d", len(headers))
	}
	if got := headers[0].GetHeader(); got.GetKey() != "authorization" || got.GetValue() != "DirectLogin token=obp-token" {
		t.Fatalf("unexpected header rewrite: %#v", got)
	}
}

func TestCheckRejectsMissingSecret(t *testing.T) {
	config := Config{
		Server: ServerConfig{
			SharedSecret: "test-secret",
		},
	}
	server := NewAuthServer(config, fakeExchanger{token: "obp-token"})

	response, err := server.Check(context.Background(), &authv3.CheckRequest{
		Attributes: &authv3.AttributeContext{
			Request: &authv3.AttributeContext_Request{
				Http: &authv3.AttributeContext_HttpRequest{
					Headers: map[string]string{
						"authorization": "Bearer subject-token",
					},
				},
			},
		},
	})
	if err != nil {
		t.Fatalf("Check returned error: %v", err)
	}
	if response.GetStatus().GetCode() != int32(codes.PermissionDenied) {
		t.Fatalf("expected permission denied, got %d", response.GetStatus().GetCode())
	}
}

func TestDenyResponseIncludesForbiddenStatus(t *testing.T) {
	response := denyResponse("denied\n")
	if response.GetDeniedResponse().GetStatus().GetCode() != typev3.StatusCode_Forbidden {
		t.Fatalf("expected forbidden status")
	}
}
