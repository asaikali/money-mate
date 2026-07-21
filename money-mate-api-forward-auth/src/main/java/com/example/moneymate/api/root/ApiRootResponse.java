package com.example.moneymate.api.root;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.RepresentationModel;

public class ApiRootResponse extends RepresentationModel<ApiRootResponse> {

  @JsonProperty("api_usage")
  private final String apiUsage =
      "This API uses HAL-FORMS. The current representation advertises related resources" +
          " in _links and available state transitions in _templates. Additional conventions" +
          " are documented by _links.profile.";

  public String getApiUsage() {
    return apiUsage;
  }
}
