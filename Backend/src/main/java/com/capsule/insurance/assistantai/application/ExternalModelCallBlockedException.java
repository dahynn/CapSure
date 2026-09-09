package com.capsule.insurance.assistantai.application;

public class ExternalModelCallBlockedException extends RuntimeException {
    public ExternalModelCallBlockedException() {
        super("외부 모델 호출은 기본 정책상 차단됩니다.");
    }
}
