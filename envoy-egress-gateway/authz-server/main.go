package main

import (
	"flag"
	"log"
	"net"
	"net/http"

	authv3 "github.com/envoyproxy/go-control-plane/envoy/service/auth/v3"
	"google.golang.org/grpc"
)

func main() {
	configPath := flag.String("config", "config.yaml", "path to the authz server YAML config file")
	flag.Parse()

	log.Printf("starting ext_authz server with config file %s", *configPath)
	config, err := LoadConfig(*configPath)
	if err != nil {
		log.Fatalf("failed to load config: %v", err)
	}
	log.Printf(
		"loaded config: listen_address=%s timeout=%s token_uri=%s client_id=%s shared_secret_present=%t",
		config.Server.ListenAddress,
		config.Server.Timeout,
		config.IdentityBroker.TokenURI,
		config.IdentityBroker.TokenExchangeClientID,
		config.Server.SharedSecret != "",
	)

	listener, err := net.Listen("tcp", config.Server.ListenAddress)
	if err != nil {
		log.Fatalf("failed to listen on %s: %v", config.Server.ListenAddress, err)
	}

	httpClient := &http.Client{Timeout: config.Server.Timeout}
	exchanger := NewIdentityBrokerTokenExchanger(httpClient, config.IdentityBroker)
	log.Printf("created identity broker HTTP client with timeout=%s", config.Server.Timeout)

	grpcServer := grpc.NewServer()
	authv3.RegisterAuthorizationServer(grpcServer, NewAuthServer(config, exchanger))

	log.Printf("ext_authz gRPC server listening on %s", config.Server.ListenAddress)
	if err := grpcServer.Serve(listener); err != nil {
		log.Fatalf("failed to serve: %v", err)
	}
}
