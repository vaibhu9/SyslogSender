package com.example.service;

import com.cloudbees.syslog.Severity;

public interface SyslogSenderService {

    public void sendTestSyslogMessage();

    public void sendSyslogMessage(String message, Severity severity);
}