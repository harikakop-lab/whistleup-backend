package com.whistleup.backend.service;

import com.whistleup.backend.resource.SmsRequest;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private final SmsCountryClient smsCountryClient;

    public SmsService(SmsCountryClient smsCountryClient) {
        this.smsCountryClient = smsCountryClient;
    }

    public void sendOtp(String mobile, String otp) {

        SmsRequest request = new SmsRequest();
        request.setText("User Admin login OTP is " + otp + " - SMSCOU");
        request.setNumber(mobile);
        request.setSenderId("SMSCOU");
        request.setDrNotifyUrl("https://www.domainname.com/notifyurl");
        request.setDrNotifyHttpMethod("POST");
        request.setTool("API");

        smsCountryClient.sendSms(request);
    }
}
