package com.capsule.insurance.insurer.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.insurer.dto.FixedTermsPdfSummaryResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FixedTermsPdfSummaryService {

    private static final String DISCLAIMER = "이 요약은 resources의 고정 PDF를 읽어 생성한 참고용 안내이며, 해석 차이가 있을 수 있으므로 최종 판단은 원문 약관을 확인해야 합니다.";

    private final ResourceLoader resourceLoader;
    private final String fixedPdfPath;
    private final int maxChars;

    public FixedTermsPdfSummaryService(
            ResourceLoader resourceLoader,
            @Value("${insurer.terms-pdf.path:classpath:terms/fixed-terms.pdf}") String fixedPdfPath,
            @Value("${insurer.terms-pdf.max-chars:30000}") int maxChars
    ) {
        this.resourceLoader = resourceLoader;
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
        // 외부 모델 호출은 안전한 AI 연동 PoC 경계 밖이다. 현재 약관 요약은 PDF 원문만 사용한다.
        return fallback;
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
