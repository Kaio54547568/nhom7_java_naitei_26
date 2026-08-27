package com.nhom7.coworkingspace.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * A lightweight browser client used to demonstrate the authenticated STOMP chat flow.
 */
@Controller
public class ChatDemoWebController {

    @GetMapping("/chat")
    public String chatDemoPage() {
        return "chat/demo";
    }
}
