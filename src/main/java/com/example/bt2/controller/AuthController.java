package com.example.bt2.controller;

import com.example.bt2.service.SpotifyAuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@Controller
public class AuthController {

    @Autowired
    private SpotifyAuthService authService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/spotify-login")
    public void login(HttpServletResponse response) throws IOException {
        String url = authService.buildAuthorizationUri();
        response.sendRedirect(url);
    }

    @GetMapping("/callback")
    @ResponseBody
    public String callback(@RequestParam String code) {
        String accessToken = authService.exchangeCodeForToken(code);
        return "Access Token: " + accessToken;
    }
}
