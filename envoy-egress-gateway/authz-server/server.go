package main

import (
	"context"
	"log"
	"net/http"
	"strings"

	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	authv3 "github.com/envoyproxy/go-control-plane/envoy/service/auth/v3"
	typev3 "github.com/envoyproxy/go-control-plane/envoy/type/v3"
	"google.golang.org/genproto/googleapis/rpc/status"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/metadata"
)

type AuthServer struct {
	config    Config
	exchanger TokenExchanger
	authv3.UnimplementedAuthorizationServer
}

func NewAuthServer(config Config, exchanger TokenExchanger) *AuthServer {
	return &AuthServer{
		config:    config,
		exchanger: exchanger,
	}
}

func (s *AuthServer) Check(ctx context.Context, req *authv3.CheckRequest) (*authv3.CheckResponse, error) {
	httpRequest := req.GetAttributes().GetRequest().GetHttp()
	method := httpRequest.GetMethod()
	path := httpRequest.GetPath()
	host := httpRequest.GetHost()
	headers := httpRequest.GetHeaders()
	log.Printf("CHECK: received ext_authz request method=%s host=%s path=%s header_count=%d", method, host, path, len(headers))

	if !s.verifySecret(ctx) {
		log.Println("DENIED: invalid or missing x-authz-secret metadata")
		return denyResponse("unauthorized authz client\n"), nil
	}
	log.Println("CHECK: x-authz-secret metadata verified")

	authHeader, ok := headers["authorization"]
	if !ok {
		log.Println("DENIED: no authorization header on incoming request")
		return denyResponse("missing authorization header\n"), nil
	}
	log.Printf("CHECK: incoming authorization header prefix=%q value_preview=%s", authorizationScheme(authHeader), previewToken(authHeader))

	// Envoy forwards the caller's bearer token to ext_authz unchanged.
	// We treat that incoming JWT as the subject token for RFC 8693 token exchange.
	subjectToken, ok := extractBearerToken(authHeader)
	if !ok {
		log.Printf("DENIED: authorization header is not bearer format: %q", authHeader)
		return denyResponse("authorization header must be bearer format\n"), nil
	}
	log.Printf("CHECK: extracted bearer subject token preview=%s", previewToken(subjectToken))

	// Exchange the caller token with identity-broker before the request leaves our
	// boundary. The broker returns an OBP DirectLogin token wrapped as an access token.
	obpToken, err := s.exchanger.ExchangeForOBPToken(ctx, subjectToken)
	if err != nil {
		log.Printf("DENIED: token exchange failed: %v", err)
		return denyResponse("token exchange failed\n"), nil
	}
	log.Printf("CHECK: token exchange succeeded obp_token_preview=%s", previewToken(obpToken))

	// Envoy applies this header mutation to the original request before forwarding it
	// upstream, so OBP sees DirectLogin auth instead of the original bearer token.
	updated := "DirectLogin token=" + obpToken
	log.Printf("ALLOWED: rewriting authorization header to DirectLogin token for upstream host=%s path=%s", host, path)

	return &authv3.CheckResponse{
		Status: &status.Status{Code: int32(codes.OK)},
		HttpResponse: &authv3.CheckResponse_OkResponse{
			OkResponse: &authv3.OkHttpResponse{
				Headers: []*corev3.HeaderValueOption{
					{
						Header: &corev3.HeaderValue{
							Key:   "authorization",
							Value: updated,
						},
					},
				},
			},
		},
	}, nil
}

func (s *AuthServer) verifySecret(ctx context.Context) bool {
	md, ok := metadata.FromIncomingContext(ctx)
	if !ok {
		return false
	}
	values := md.Get("x-authz-secret")
	return len(values) == 1 && values[0] == s.config.Server.SharedSecret
}

func extractBearerToken(header string) (string, bool) {
	if !strings.HasPrefix(strings.ToLower(header), "bearer ") {
		return "", false
	}
	token := strings.TrimSpace(header[len("Bearer "):])
	return token, token != ""
}

func authorizationScheme(header string) string {
	parts := strings.Fields(header)
	if len(parts) == 0 {
		return ""
	}
	return parts[0]
}

func previewToken(token string) string {
	token = strings.TrimSpace(token)
	if token == "" {
		return "<empty>"
	}
	if strings.HasPrefix(strings.ToLower(token), strings.ToLower(http.CanonicalHeaderKey("Bearer "))) {
		token = strings.TrimSpace(token[len("Bearer "):])
	}
	if len(token) <= 12 {
		return token
	}
	return token[:8] + "..." + token[len(token)-4:]
}

func denyResponse(body string) *authv3.CheckResponse {
	return &authv3.CheckResponse{
		Status: &status.Status{Code: int32(codes.PermissionDenied)},
		HttpResponse: &authv3.CheckResponse_DeniedResponse{
			DeniedResponse: &authv3.DeniedHttpResponse{
				Status: &typev3.HttpStatus{
					Code: typev3.StatusCode_Forbidden,
				},
				Body: body,
			},
		},
	}
}
