package com.capsule.insurance.insurer.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.insurer.dto.FixedTermsPdfSummaryResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class FixedTermsPdfSummaryService {

    private static final String DISCLAIMER = "이 요약은 resources의 고정 PDF를 읽어 생성한 참고용 안내이며, 해석 차이가 있을 수 있으므로 최종 판단은 원문 약관을 확인해야 합니다.";

    private final ResourceLoader resourceLoader;
    private final ChatClient chatClient;
    private final String openAiApiKey;
    private final String fixedPdfPath;
    private final int maxChars;

    public FixedTermsPdfSummaryService(
            ResourceLoader resourceLoader,
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            @Value("${spring.ai.openai.api-key:}") String openAiApiKey,
            @Value("${insurer.terms-pdf.path:classpath:terms/fixed-terms.pdf}") String fixedPdfPath,
            @Value("${insurer.terms-pdf.max-chars:30000}") int maxChars
    ) {
        this.resourceLoader = resourceLoader;
        ChatClient.Builder chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
        this.chatClient = chatClientBuilder == null ? null : chatClientBuilder.build();
        this.openAiApiKey = openAiApiKey;
        this.fixedPdfPath = fixedPdfPath;
        this.maxChars = maxChars;
    }

    public FixedTermsPdfSummaryResponse summarizeFixedPdf() {
        ExtractedPdf extractedPdf = extractPdfText();
        PdfAiSummary fallback = buildFallbackSummary(extractedPdf.text());
        PdfAiSummary aiSummary = generateAiSummary(extractedPdf.text(), fallback);

        return new FixedTermsPdfSummaryResponse(
                fixedPdfPath,
                extractedPdf.pageCount(),
                valueOrDefault(aiSummary.headline(), fallback.headline()),
                valueOrDefault(aiSummary.coverageScope(), fallback.coverageScope()),
                valueOrDefault(aiSummary.coverageAmount(), fallback.coverageAmount()),
                valueOrDefault(aiSummary.exclusions(), fallback.exclusions()),
                valueOrDefault(aiSummary.keyLimitations(), fallback.keyLimitations()),
                valueOrDefault(aiSummary.specialNotes(), fallback.specialNotes()),
                limitText(extractedPdf.text(), 700),
                DISCLAIMER
        );
    }

    private ExtractedPdf extractPdfText() {
        Resource resource = resourceLoader.getResource(fixedPdfPath);
        if (!resource.exists()) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "고정 약관 PDF를 찾을 수 없습니다: " + fixedPdfPath
            );
        }

        try (InputStream inputStream = resource.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String text = normalizeText(pdfTextStripper.getText(document));

            if (!StringUtils.hasText(text)) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "PDF에서 추출된 텍스트가 없습니다.");
            }

            return new ExtractedPdf(document.getNumberOfPages(), limitText(text, maxChars));
        } catch (IOException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "PDF를 읽는 중 오류가 발생했습니다."
            );
        }
    }

    private PdfAiSummary generateAiSummary(String pdfText, PdfAiSummary fallback) {
        if (chatClient == null || !StringUtils.hasText(openAiApiKey)) {
            return fallback;
        }

        try {
            PdfAiSummary result = chatClient.prompt()
                    .system("""
                            너는 보험 약관 요약 도우미다.
                            전달된 PDF 텍스트만 근거로 요약한다.
                            없는 내용을 추정하지 않는다.
                            특히 보장하지 않는 경우, 면책, 주요 제한 조건은 문서에 실제로 보일 때만 적는다.
                            찾지 못한 정보는 '문서에서 명시적으로 찾지 못했습니다.'로 작성한다.
                            각 필드는 짧고 발표용으로 읽기 좋게 한국어로 정리한다.
                            """)
                    .user("""
                            아래는 고정 약관 PDF에서 추출한 텍스트다.
                            다음 항목으로만 요약해줘.

                            1. headline: 문서 전체를 한 줄로 요약
                            2. coverageScope: 보장 범위
                            3. coverageAmount: 보장 금액 또는 지급 기준
                            4. exclusions: 면책 사항 또는 보장하지 않는 경우
                            5. keyLimitations: 주요 제한 조건
                            6. specialNotes: 기타 중요한 사항

                            [PDF TEXT]
                            %s
                            """.formatted(pdfText))
                    .call()
                    .entity(PdfAiSummary.class);

            return result == null ? fallback : result;
        } catch (Exception exception) {
            log.warn("Fixed PDF summary generation failed", exception);
            return fallback;
        }
    }

    private PdfAiSummary buildFallbackSummary(String pdfText) {
        String preview = limitText(pdfText, 500);
        return new PdfAiSummary(
                "고정 약관 PDF 요약",
                preview,
                "문서에서 보장 금액을 명확히 추출하지 못했습니다.",
                "문서에서 명시적으로 찾지 못했습니다.",
                "문서에서 명시적으로 찾지 못했습니다.",
                "발표 시에는 원문 약관 문구와 함께 확인하는 것이 안전합니다."
        );
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace('\u0000', ' ')
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String limitText(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private record ExtractedPdf(int pageCount, String text) {
    }

    public record PdfAiSummary(
            String headline,
            String coverageScope,
            String coverageAmount,
            String exclusions,
            String keyLimitations,
            String specialNotes
    ) {
    }
}
