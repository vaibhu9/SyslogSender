package com.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloudbees.syslog.Severity;
import com.example.service.SyslogSenderService;

@RestController
public class SyslogSenderController {

    private final SyslogSenderService syslogSenderService;

    public SyslogSenderController(SyslogSenderService syslogSenderService) {
        this.syslogSenderService = syslogSenderService;
    }

    @GetMapping("/send-syslog")
    public String sendSyslogMessage() {
        syslogSenderService.sendTestSyslogMessage();
        return "Syslog message sent!";
    }

    @PostMapping("/send-syslog")
    public String sendSyslogMessage(@RequestParam("message") String message, @RequestParam("severity") Severity severity) {
        syslogSenderService.sendSyslogMessage(message, severity);
        return "Syslog message sent with severity: " + severity;
    }
    // curl -X POST "http://localhost:8081/send-syslog?message=TestMessage&severity=INFORMATIONAL"

}
