package com.capsule.insurance.policy.change;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.policy.domain.PolicySnapshot;
import com.capsule.insurance.policy.infra.JdbcPolicyRepository;
import com.capsule.insurance.quote.domain.QuoteSnapshot;
import com.capsule.insurance.quote.application.port.QuoteRepository;
import com.capsule.insurance.premiumcollection.dto.CreatePremiumReceivableRequest;
import com.capsule.insurance.premiumcollection.domain.PremiumCollectionSnapshot;
import com.capsule.insurance.premiumcollection.infra.JdbcPremiumCollectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Simulation: changes take effect at the next calendar month, Asia/Seoul. */
@Service
public class CoverageChangeService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private final JdbcTemplate db;
    private final JdbcPolicyRepository policies;
    private final QuoteRepository quotes;
    private final JdbcPremiumCollectionRepository premiums;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;
    private final Clock clock;

    @Autowired
    public CoverageChangeService(JdbcTemplate db, JdbcPolicyRepository policies, QuoteRepository quotes,
            JdbcPremiumCollectionRepository premiums, ObjectMapper mapper, PlatformTransactionManager manager) {
        this(db, policies, quotes, premiums, mapper, manager, Clock.systemUTC());
    }
    public CoverageChangeService(JdbcTemplate db, JdbcPolicyRepository policies, QuoteRepository quotes,
            JdbcPremiumCollectionRepository premiums, ObjectMapper mapper, PlatformTransactionManager manager, Clock clock) {
        this.db=db; this.policies=policies; this.quotes=quotes; this.premiums=premiums;
        this.mapper=mapper; this.tx=new TransactionTemplate(manager); this.clock=clock;
    }

    public ChangeView schedule(Long userId, Long policyId, String key, ChangeRequest request) {
        if (key == null || key.isBlank() || key.length() > 150) fail(ErrorCode.INVALID_INPUT, "변경 요청 키가 필요합니다.");
        return tx.execute(status -> {
            lockOwned(userId, policyId);
            List<ChangeView> repeated = db.query("""
                    SELECT c.*, v.snapshot_json::TEXT AS snapshot_json FROM ins_policy_change c
                    JOIN ins_policy_version v USING(policy_version_id) WHERE c.idempotency_key = ?
                    """, (rs,n) -> view(rs.getLong("policy_change_id"), rs.getLong("policy_id"),
                    rs.getLong("quote_id"), rs.getTimestamp("effective_at").toInstant(), rs.getString("snapshot_json")), key);
            if (!repeated.isEmpty()) {
                ChangeView existing = repeated.getFirst();
                if (!existing.policyId().equals(policyId) || !existing.quoteId().equals(request.quoteId())
                        || !Boolean.TRUE.equals(request.agreed()) || !existing.quote().termsHash().equals(request.documentHash())) {
                    fail(ErrorCode.IDEMPOTENCY_CONFLICT, "같은 키에 다른 변경 내용이 전달되었습니다.");
                }
                return existing;
            }
            var policy = policies.findOwned(policyId,userId).orElseThrow();
            if (!"ACTIVE".equals(policy.status())) fail(ErrorCode.INVALID_STATE_TRANSITION, "정상 유지 중인 계약만 변경할 수 있습니다.");
            Instant now = Instant.now(clock);
            Instant effective = nextMonth().atStartOfDay(ZONE).toInstant();
            if (Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM ins_policy_version WHERE policy_id=? AND valid_from>?)",
                    Boolean.class,policyId,Timestamp.from(now)))) fail(ErrorCode.BUSINESS_RULE_VIOLATION, "이미 예약된 변경이 있습니다.");
            if (Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM ins_premium_receivable WHERE policy_id=? AND billing_cycle>=?)",
                    Boolean.class,policyId,nextMonth()))) fail(ErrorCode.BUSINESS_RULE_VIOLATION, "다음 달 보험료가 이미 청구되어 변경할 수 없습니다.");
            if (db.queryForList("SELECT quote_id FROM ins_quote WHERE quote_id=? AND user_id=? FOR UPDATE",Long.class,request.quoteId(),userId).isEmpty())
                fail(ErrorCode.RESOURCE_NOT_FOUND,"변경 견적을 찾을 수 없습니다.");
            var quote = quotes.findOwned(request.quoteId(),userId).orElseThrow(() ->
                    new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,"변경 견적을 찾을 수 없습니다."));
            if (!"ISSUED".equals(quote.status()) || !quote.expiresAt().isAfter(now)) fail(ErrorCode.INVALID_STATE_TRANSITION,"사용했거나 만료된 견적입니다.");
            var current = policy.policyVersion();
            if (!quote.productVersionId().equals(current.productVersionId())) fail(ErrorCode.BUSINESS_RULE_VIOLATION,"동일 상품 안에서 보장을 변경해야 합니다.");
            if (!Boolean.TRUE.equals(request.agreed()) || !quote.termsDocumentHash().equals(request.documentHash())) fail(ErrorCode.BUSINESS_RULE_VIOLATION,"변경 약관 동의가 필요합니다.");
            Set<Long> oldIds = new HashSet<>(current.coverages().stream().map(c -> c.productCoverageId()).toList());
            Set<Long> newIds = new HashSet<>(quote.snapshot().coverages().stream().map(c -> c.productCoverageId()).toList());
            if (oldIds.equals(newIds)) fail(ErrorCode.BUSINESS_RULE_VIOLATION,"현재와 다른 보장을 선택해야 합니다.");
            PolicySnapshot fresh = policies.snapshotForQuote(quote.snapshot());
            PolicySnapshot snapshot = new PolicySnapshot(quote.snapshot(), fresh.claimRules().stream().map(rule ->
                current.snapshot().claimRules().stream().filter(old -> old.productCoverageId().equals(rule.productCoverageId()))
                        .findFirst().orElse(rule)).toList());
            db.update("UPDATE ins_policy_version SET valid_to=? WHERE policy_version_id=?",Timestamp.from(effective),current.policyVersionId());
            Long versionId = db.queryForObject("""
                    INSERT INTO ins_policy_version(policy_id,version,product_version_id,terms_document_id,valid_from,snapshot_json)
                    VALUES(?,?,?,?,?,CAST(? AS JSONB)) RETURNING policy_version_id
                    """,Long.class,policyId,current.version()+1,quote.productVersionId(),quote.snapshot().termsDocumentId(),Timestamp.from(effective),json(snapshot));
            for (var coverage: quote.snapshot().coverages()) {
                Instant start = current.coverages().stream().filter(c -> c.productCoverageId().equals(coverage.productCoverageId()))
                        .map(c -> c.coverageStartAt()).findFirst().orElse(effective.plusSeconds(coverage.waitingPeriodDays()*86400L));
                db.update("""
                        INSERT INTO ins_policy_coverage(policy_version_id,product_coverage_id,coverage_code_snapshot,
                            insured_amount,currency_code,coverage_start_at) VALUES(?,?,?,?,?,?)
                        """,versionId,coverage.productCoverageId(),coverage.coverageCode(),coverage.insuredAmount(),coverage.currencyCode(),Timestamp.from(start));
            }
            db.update("UPDATE ins_quote SET status='USED',used_at=? WHERE quote_id=?",Timestamp.from(now),quote.quoteId());
            Long changeId = db.queryForObject("""
                    INSERT INTO ins_policy_change(policy_id,quote_id,policy_version_id,effective_at,idempotency_key,consent_hash)
                    VALUES(?,?,?,?,?,?) RETURNING policy_change_id
                    """,Long.class,policyId,quote.quoteId(),versionId,Timestamp.from(effective),key,request.documentHash());
            db.update("""
                    INSERT INTO ops_outbox_event(event_id,aggregate_type,aggregate_id,event_type,payload_json)
                    VALUES(?,'POLICY',?,'POLICY_COVERAGE_CHANGE_SCHEDULED',CAST(? AS JSONB))
                    ""","POLICY-CHANGE-"+changeId,policyId.toString(),json(Map.of("policyId",policyId,"changeId",changeId,"effectiveAt",effective.toString())));
            return new ChangeView(changeId,policyId,quote.quoteId(),effective,"SCHEDULED",quote.snapshot());
        });
    }

    public List<ChangeView> history(Long userId, Long policyId) {
        policies.findOwned(policyId,userId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,"보험계약을 찾을 수 없습니다."));
        return db.query("""
                SELECT c.*,v.snapshot_json::TEXT AS snapshot_json FROM ins_policy_change c
                JOIN ins_policy_version v USING(policy_version_id) WHERE c.policy_id=? ORDER BY c.effective_at DESC
                """, (rs,n) -> view(rs.getLong("policy_change_id"),policyId,rs.getLong("quote_id"),
                rs.getTimestamp("effective_at").toInstant(),rs.getString("snapshot_json")),policyId);
    }

    /** Uses the version effective on the billing date; caller cannot supply an amount. */
    public PremiumCollectionSnapshot createNextPremium(Long userId,Long policyId) {
        return tx.execute(status -> {
            lockOwned(userId,policyId);
            LocalDate cycle=nextMonth();
            List<Long> existing=db.queryForList("SELECT premium_receivable_id FROM ins_premium_receivable WHERE policy_id=? AND billing_cycle=?",Long.class,policyId,cycle);
            if(!existing.isEmpty()) return premiums.find(existing.getFirst());
            var policy=policies.findOwned(policyId,userId).orElseThrow();
            if(!"ACTIVE".equals(policy.status())) fail(ErrorCode.INVALID_STATE_TRANSITION,"정상 유지 중인 계약만 다음 달 보험료를 생성할 수 있습니다.");
            BigDecimal amount=db.queryForObject("""
                    SELECT (snapshot_json->'quote'->>'monthlyPremium')::NUMERIC FROM ins_policy_version
                    WHERE policy_id=? AND valid_from<=? AND (valid_to IS NULL OR ?<valid_to)
                    """,BigDecimal.class,policyId,Timestamp.from(cycle.atStartOfDay(ZONE).toInstant()),Timestamp.from(cycle.atStartOfDay(ZONE).toInstant()));
            Long versionId=db.queryForObject("""
                    SELECT policy_version_id FROM ins_policy_version
                    WHERE policy_id=? AND valid_from<=? AND (valid_to IS NULL OR ?<valid_to)
                    """,Long.class,policyId,Timestamp.from(cycle.atStartOfDay(ZONE).toInstant()),Timestamp.from(cycle.atStartOfDay(ZONE).toInstant()));
            PremiumCollectionSnapshot due=premiums.createDueWithAutomaticInstruction(
                    new CreatePremiumReceivableRequest(policyId,cycle,cycle,cycle.plusDays(14),amount),"COL-"+UUID.randomUUID());
            db.update("UPDATE ins_premium_receivable SET policy_version_id=? WHERE premium_receivable_id=?",versionId,due.premiumReceivableId());
            return due;
        });
    }
    private void lockOwned(Long userId,Long policyId) {
        if(db.queryForList("SELECT policy_id FROM ins_policy WHERE policy_id=? AND policyholder_user_id=? FOR UPDATE",Long.class,policyId,userId).isEmpty())
            fail(ErrorCode.RESOURCE_NOT_FOUND,"보험계약을 찾을 수 없습니다.");
    }
    private LocalDate nextMonth(){return LocalDate.now(clock.withZone(ZONE)).withDayOfMonth(1).plusMonths(1);}
    private ChangeView view(Long id,Long policyId,Long quoteId,Instant at,String raw){
        try{return new ChangeView(id,policyId,quoteId,at,at.isAfter(Instant.now(clock))?"SCHEDULED":"EFFECTIVE",mapper.readValue(raw,PolicySnapshot.class).quote());}
        catch(Exception ex){throw new IllegalStateException("변경 내역을 읽지 못했습니다.",ex);}
    }
    private String json(Object value){try{return mapper.writeValueAsString(value);}catch(Exception ex){throw new IllegalStateException(ex);}}
    private static void fail(ErrorCode code,String message){throw new BusinessException(code,message);}
    public record ChangeRequest(@jakarta.validation.constraints.NotNull Long quoteId,
            @jakarta.validation.constraints.NotBlank String documentHash,@jakarta.validation.constraints.NotNull Boolean agreed){}
    public record ChangeView(Long policyChangeId,Long policyId,Long quoteId,Instant effectiveAt,String status,QuoteSnapshot quote){}
}
