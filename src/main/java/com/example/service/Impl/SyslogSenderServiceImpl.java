package com.example.service.Impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.sender.TcpSyslogMessageSender;
import com.example.service.SyslogSenderService;

@Service
public class SyslogSenderServiceImpl implements SyslogSenderService{

    private static final Logger logger = LoggerFactory.getLogger(SyslogSenderServiceImpl.class);

    private final TcpSyslogMessageSender messageSender;

    public SyslogSenderServiceImpl(@Value("${spring.application.name:default-app}") String appName,
            @Value("${syslog.server.hostname}") String syslogServerHostname,
            @Value("${syslog.server.port}") int syslogServerPort) {
 
        messageSender = new TcpSyslogMessageSender();

        messageSender.setDefaultAppName(appName);
        messageSender.setSyslogServerHostname(syslogServerHostname);
        messageSender.setSyslogServerPort(syslogServerPort);
        messageSender.setDefaultFacility(Facility.USER);
        messageSender.setDefaultSeverity(Severity.INFORMATIONAL);
        messageSender.setMessageFormat(MessageFormat.RFC_5424);
        messageSender.setSsl(false);
    }

    public void sendTestSyslogMessage() {
        try {
            messageSender.sendMessage("This is a test message sent from Spring Boot!");
            logger.info("Test syslog message sent.");
        } catch (Exception e) {
            logger.error("Failed to send syslog message: {}", e.getMessage(), e);
        }
    }

    public void sendSyslogMessage(String message, Severity severity) {
        try {
            messageSender.setDefaultSeverity(severity);
            messageSender.sendMessage(message);
            logger.info("Syslog message sent: {}", message);
        } catch (Exception e) {
            logger.error("Failed to send syslog message: {}", e.getMessage(), e);
        }
    }
}
