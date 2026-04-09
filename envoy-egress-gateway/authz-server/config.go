package main

import (
	"fmt"
	"os"
	"time"

	"gopkg.in/yaml.v3"
)

type Config struct {
	Server         ServerConfig         `yaml:"server"`
	IdentityBroker IdentityBrokerConfig `yaml:"identity_broker"`
}

type ServerConfig struct {
	ListenAddress string        `yaml:"listen_address"`
	SharedSecret  string        `yaml:"shared_secret"`
	Timeout       time.Duration `yaml:"timeout"`
}

type IdentityBrokerConfig struct {
	TokenURI                  string `yaml:"token_uri"`
	TokenExchangeClientID     string `yaml:"token_exchange_client_id"`
	TokenExchangeClientSecret string `yaml:"token_exchange_client_secret"`
}

func LoadConfig(path string) (Config, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return Config{}, fmt.Errorf("read config: %w", err)
	}

	var cfg Config
	if err := yaml.Unmarshal(data, &cfg); err != nil {
		return Config{}, fmt.Errorf("parse config: %w", err)
	}

	if cfg.Server.ListenAddress == "" {
		cfg.Server.ListenAddress = "127.0.0.1:10003"
	}
	if cfg.Server.Timeout == 0 {
		cfg.Server.Timeout = 2 * time.Second
	}

	if cfg.Server.SharedSecret == "" {
		return Config{}, fmt.Errorf("server.shared_secret is required")
	}
	if cfg.IdentityBroker.TokenURI == "" {
		return Config{}, fmt.Errorf("identity_broker.token_uri is required")
	}
	if cfg.IdentityBroker.TokenExchangeClientID == "" {
		return Config{}, fmt.Errorf("identity_broker.token_exchange_client_id is required")
	}
	if cfg.IdentityBroker.TokenExchangeClientSecret == "" {
		return Config{}, fmt.Errorf("identity_broker.token_exchange_client_secret is required")
	}

	return cfg, nil
}
