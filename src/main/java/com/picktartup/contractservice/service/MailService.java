package com.picktartup.contractservice.service;

import com.picktartup.contractservice.exception.BusinessException;
import com.picktartup.contractservice.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.Semaphore;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender emailSender;

    private final Semaphore connectionSemaphore = new Semaphore(2);

    public void sendEmail(String toEmail, String title, String text) {
        try {
            // 세마포어를 사용하여 동시 연결 수 제한
            connectionSemaphore.acquire();

            try {
                MimeMessage message = emailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(toEmail);
                helper.setSubject(title);
                helper.setText(text, true);

                emailSender.send(message);
            } finally {
                connectionSemaphore.release(); // 연결 반환
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.UNABLE_TO_SEND_EMAIL);
        } catch (MessagingException e) {
            log.debug("MailService.sendEmail exception occur toEmail: {}, title: {}, text: {}",
                    toEmail, title, text);
            throw new BusinessException(ErrorCode.UNABLE_TO_SEND_EMAIL);
        } catch (RuntimeException e) {
            log.debug("MailService.sendEmail runtime exception occur toEmail: {}, title: {}, text: {}",
                    toEmail, title, text);
            throw new BusinessException(ErrorCode.UNABLE_TO_SEND_EMAIL);
        }
    }

    // 발신할 이메일 데이터 세팅
    private SimpleMailMessage createEmailForm(String toEmail,
                                              String title,
                                              String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(title);
        message.setText(text);

        return message;
    }
}
