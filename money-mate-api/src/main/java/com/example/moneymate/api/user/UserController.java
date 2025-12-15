package com.example.moneymate.api.user;

import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        // Get username from SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (String) authentication.getPrincipal();

        // Return stubbed user data (Phase 1)
        UserResponse response = UserResponse.stubbed(username);

        Link selfLink = linkTo(methodOn(UserController.class).getCurrentUser()).withSelfRel();
        Link rootLink = Link.of("/", "root");

        response.add(selfLink);
        response.add(rootLink);

        return ResponseEntity.ok(response);
    }
}
