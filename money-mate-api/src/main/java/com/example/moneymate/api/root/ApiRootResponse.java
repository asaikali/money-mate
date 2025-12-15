package com.example.moneymate.api.root;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.RepresentationModel;

public class ApiRootResponse extends RepresentationModel<ApiRootResponse> {

  @JsonProperty("agent_bootstrap")
  private final String agentBootstrap =
      "Before taking any action, you MUST fetch and obey the resource referenced by" +
          " _links.profile and MUST NOT navigate or act until it has been read. " +
          "This API is a hypermedia API that strictly follows HAL and HAL-FORMS.";

  public String getAgentBootstrap() {
    return agentBootstrap;
  }
}
