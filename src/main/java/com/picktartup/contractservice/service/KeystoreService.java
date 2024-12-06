package com.picktartup.contractservice.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picktartup.contractservice.exception.BusinessException;
import com.picktartup.contractservice.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.exception.CipherException;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeystoreService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private final AmazonS3 amazonS3Client;
    private final ObjectMapper objectMapper;

    public WalletFile getWalletFile(String keystoreFileName) {
        log.debug("S3에서 키스토어 파일 조회 {}", keystoreFileName);

        try {
            // S3에서 직접 객체 읽기
            S3Object s3Object = amazonS3Client.getObject(bucket, "wallets/" + keystoreFileName);
            return objectMapper.readValue(s3Object.getObjectContent(), WalletFile.class);
        } catch (AmazonS3Exception e) {
            log.error("S3에서 Keystore 파일을 찾을 수 없음: {}", keystoreFileName);
            throw new BusinessException(ErrorCode.KEYSTORE_FILE_NOT_FOUND);
        } catch (IOException e) {
            log.error("Keystore 파일 읽기 실패: {}", keystoreFileName, e);
            throw new BusinessException(ErrorCode.KEYSTORE_READ_FAILED);
        }
    }


    public String decryptPrivateKey(WalletFile walletFile, String password) {
        try {
            Credentials credentials = Credentials.create(Wallet.decrypt(password, walletFile));
            return credentials.getEcKeyPair().getPrivateKey().toString(16);
        } catch (CipherException e) {
            log.error("Private key 복호화 실패 - 원인: {}", e.getMessage());

            // 비밀번호 오류와 기타 오류 구분
            if (e.getMessage().contains("Invalid password")) {
                throw new BusinessException(
                        ErrorCode.INVALID_PASSWORD,
                        "입력한 비밀번호가 올바르지 않습니다."
                );
            }

            throw new BusinessException(
                    ErrorCode.PRIVATE_KEY_DECRYPT_FAILED,
                    "Private key 복호화 중 오류가 발생했습니다.",
                    e
            );
        }
    }



    private WalletFile readWalletFile(File keystoreFile, String fileName) {
        try {
            return objectMapper.readValue(keystoreFile, WalletFile.class);
        } catch (IOException e) {
            log.error("Keystore 파일 읽기 실패: {}", fileName, e);
            throw new BusinessException(
                    ErrorCode.KEYSTORE_READ_FAILED,
                    String.format("Keystore 파일 읽기에 실패했습니다: %s", fileName),
                    e
            );
        } catch (Exception e) {
            log.error("잘못된 Keystore 파일 형식: {}", fileName, e);
            throw new BusinessException(
                    ErrorCode.INVALID_KEYSTORE_FORMAT,
                    String.format("잘못된 Keystore 파일 형식입니다: %s", fileName),
                    e
            );
        }
    }

}

