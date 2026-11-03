package com.capsule.insurance.policy.change;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.policy.infra.JdbcPolicyRepository;
import com.capsule.insurance.quote.application.port.QuoteRepository;
import com.capsule.insurance.quote.domain.QuoteSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Human-reviewed educational endorsement flow; not an insurer underwriting engine. */
@Service
public class CoverageChangeReviewService {
    private static final ZoneId ZONE=ZoneId.of("Asia/Seoul");
    private final JdbcTemplate db;
    private final JdbcPolicyRepository policies;
    private final QuoteRepository quotes;
    private final CoverageChangeService changes;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;
    private final Clock clock;

    @Autowired
    public CoverageChangeReviewService(JdbcTemplate db,JdbcPolicyRepository policies,QuoteRepository quotes,
            CoverageChangeService changes,ObjectMapper mapper,PlatformTransactionManager manager){
        this(db,policies,quotes,changes,mapper,manager,Clock.systemUTC());
    }
    public CoverageChangeReviewService(JdbcTemplate db,JdbcPolicyRepository policies,QuoteRepository quotes,
            CoverageChangeService changes,ObjectMapper mapper,PlatformTransactionManager manager,Clock clock){
        this.db=db;this.policies=policies;this.quotes=quotes;this.changes=changes;this.mapper=mapper;
        this.tx=new TransactionTemplate(manager);this.clock=clock;
    }
    public RequestView submit(Long userId,Long policyId,String key,CoverageChangeService.ChangeRequest request){
        if(key==null||key.isBlank()||key.length()>150)fail(ErrorCode.INVALID_INPUT,"변경 신청 키가 필요합니다.");
        return tx.execute(status->{
            lockPolicy(policyId,userId);
            Instant now=Instant.now(clock);
            db.update("""
                    UPDATE ins_policy_change_request SET review_status='EXPIRED',decided_at=?,decision_reason='적용 기한 만료'
                    WHERE policy_id=? AND review_status='PENDING_REVIEW' AND effective_at<=?
                    """,Timestamp.from(now),policyId,Timestamp.from(now));
            var repeated=db.query("SELECT * FROM ins_policy_change_request WHERE idempotency_key=?",this::read,key);
            if(!repeated.isEmpty()){
                Row row=repeated.getFirst();
                if(!row.policyId().equals(policyId)||!row.userId().equals(userId)||!row.quoteId().equals(request.quoteId())
                        ||!Boolean.TRUE.equals(request.agreed())||!row.hash().equals(request.documentHash()))
                    fail(ErrorCode.IDEMPOTENCY_CONFLICT,"같은 신청 키에 다른 변경 내용이 전달되었습니다.");
                return view(row);
            }
            var policy=policies.findOwned(policyId,userId).orElseThrow();
            if(!"ACTIVE".equals(policy.status()))fail(ErrorCode.INVALID_STATE_TRANSITION,"정상 유지 중인 계약만 보장 변경을 신청할 수 있습니다.");
            if(Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM ins_policy_change_request WHERE policy_id=? AND review_status='PENDING_REVIEW')",
                    Boolean.class,policyId)))fail(ErrorCode.BUSINESS_RULE_VIOLATION,"심사 중인 변경 신청이 있습니다.");
            if(Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM ins_policy_version WHERE policy_id=? AND valid_from>?)",
                    Boolean.class,policyId,Timestamp.from(now))))fail(ErrorCode.BUSINESS_RULE_VIOLATION,"이미 승인된 변경 예약이 있습니다.");
            LocalDate nextMonth=LocalDate.now(clock.withZone(ZONE)).withDayOfMonth(1).plusMonths(1);
            if(Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM ins_premium_receivable WHERE policy_id=? AND billing_cycle>=?)",
                    Boolean.class,policyId,nextMonth)))fail(ErrorCode.BUSINESS_RULE_VIOLATION,"다음 달 청구가 이미 생성됐습니다.");
            if(db.queryForList("SELECT quote_id FROM ins_quote WHERE quote_id=? AND user_id=? FOR UPDATE",Long.class,request.quoteId(),userId).isEmpty())
                fail(ErrorCode.RESOURCE_NOT_FOUND,"변경 견적을 찾을 수 없습니다.");
            var quote=quotes.findOwned(request.quoteId(),userId).orElseThrow();
            if(!"ISSUED".equals(quote.status())||!quote.expiresAt().isAfter(now))fail(ErrorCode.INVALID_STATE_TRANSITION,"사용했거나 만료된 견적입니다.");
            if(!quote.productVersionId().equals(policy.policyVersion().productVersionId()))fail(ErrorCode.BUSINESS_RULE_VIOLATION,"동일 상품 안에서 보장을 변경해야 합니다.");
            if(!Boolean.TRUE.equals(request.agreed())||!quote.termsDocumentHash().equals(request.documentHash()))fail(ErrorCode.BUSINESS_RULE_VIOLATION,"변경 내용과 약관 동의가 필요합니다.");
            Set<Long> before=new HashSet<>(policy.policyVersion().coverages().stream().map(c->c.productCoverageId()).toList());
            Set<Long> after=new HashSet<>(quote.snapshot().coverages().stream().map(c->c.productCoverageId()).toList());
            if(before.equals(after))fail(ErrorCode.BUSINESS_RULE_VIOLATION,"현재와 다른 보장을 선택해야 합니다.");
            Instant effective=nextMonth.atStartOfDay(ZONE).toInstant();
            Long id=db.queryForObject("""
                    INSERT INTO ins_policy_change_request(policy_id,requested_by,quote_id,idempotency_key,consent_hash,
                        quote_snapshot_json,effective_at) VALUES(?,?,?,?,?,CAST(? AS JSONB),?) RETURNING change_request_id
                    """,Long.class,policyId,userId,quote.quoteId(),key,request.documentHash(),json(quote.snapshot()),Timestamp.from(effective));
            db.update("UPDATE ins_quote SET status='USED',used_at=? WHERE quote_id=?",Timestamp.from(now),quote.quoteId());
            event(id,policyId,"REQUESTED");
            return view(find(id,false));
        });
    }
    public RequestView decide(Long actorId,Long requestId,Decision decision,String reason){
        if(decision==null||reason==null||reason.isBlank()||reason.trim().length()>500)fail(ErrorCode.INVALID_INPUT,"심사 판단과 500자 이내 사유가 필요합니다.");
        return tx.execute(status->{
            assertOperator(actorId);
            Row initial=find(requestId,false);
            lockPolicy(initial.policyId(),initial.userId());
            Row row=find(requestId,true);
            if(actorId.equals(row.userId()))fail(ErrorCode.FORBIDDEN,"본인의 변경 신청은 심사할 수 없습니다.");
            String resulting=decision==Decision.APPROVE?"APPROVED":"REJECTED";
            if(!"PENDING_REVIEW".equals(row.status())){
                if(resulting.equals(row.status())&&actorId.equals(row.reviewer())&&reason.trim().equals(row.reason()))return view(row);
                fail(ErrorCode.INVALID_STATE_TRANSITION,"심사 결과가 이미 확정된 신청입니다.");
            }
            Instant now=Instant.now(clock);
            if(!row.effectiveAt().isAfter(now)){
                db.update("UPDATE ins_policy_change_request SET review_status='EXPIRED',decided_at=?,decision_reason='적용 기한 만료' WHERE change_request_id=?",
                        Timestamp.from(now),requestId);event(requestId,row.policyId(),"EXPIRED");return view(find(requestId,false));
            }
            db.update("""
                    UPDATE ins_policy_change_request SET review_status=?,reviewed_by=?,decision_reason=?,decided_at=?
                    WHERE change_request_id=?
                    """,resulting,actorId,reason.trim(),Timestamp.from(now),requestId);
            if(decision==Decision.APPROVE){
                var scheduled=changes.schedule(row.userId(),row.policyId(),"review-approved:"+requestId,
                        new CoverageChangeService.ChangeRequest(row.quoteId(),row.hash(),true));
                db.update("UPDATE ins_policy_change_request SET scheduled_change_id=? WHERE change_request_id=?",scheduled.policyChangeId(),requestId);
            }
            event(requestId,row.policyId(),resulting);
            return view(find(requestId,false));
        });
    }
    public List<RequestView> history(Long userId,Long policyId){
        policies.findOwned(policyId,userId).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,"보험계약을 찾을 수 없습니다."));
        List<RequestView> rows=new ArrayList<>(db.query("SELECT * FROM ins_policy_change_request WHERE policy_id=? ORDER BY change_request_id DESC",this::read,policyId).stream().map(this::view).toList());
        Set<Long> quotes=new HashSet<>(rows.stream().map(RequestView::quoteId).toList());
        for(var legacy:changes.history(userId,policyId))if(!quotes.contains(legacy.quoteId()))
            rows.add(new RequestView(null,legacy.policyId(),legacy.quoteId(),legacy.effectiveAt(),"LEGACY",legacy.status(),legacy.quote(),null,null,null));
        return rows;
    }
    public List<RequestView> pending(Long actorId){
        assertOperator(actorId);
        return db.query("SELECT * FROM ins_policy_change_request WHERE review_status='PENDING_REVIEW' AND effective_at>? ORDER BY change_request_id LIMIT 50",
                this::read,Timestamp.from(Instant.now(clock))).stream().map(this::view).toList();
    }
    private void assertOperator(Long id){
        if(id==null||!Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM usr_user WHERE user_id=? AND access_role='ROLE_ADMIN' AND user_status='ACTIVE')",Boolean.class,id)))
            fail(ErrorCode.FORBIDDEN,"운영자 권한이 필요합니다.");
    }
    private void lockPolicy(Long id,Long owner){
        if(db.queryForList("SELECT policy_id FROM ins_policy WHERE policy_id=? AND policyholder_user_id=? FOR UPDATE",Long.class,id,owner).isEmpty())
            fail(ErrorCode.RESOURCE_NOT_FOUND,"보험계약을 찾을 수 없습니다.");
    }
    private Row find(Long id,boolean lock){return db.query("SELECT * FROM ins_policy_change_request WHERE change_request_id=?"+(lock?" FOR UPDATE":""),this::read,id).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,"변경 신청을 찾을 수 없습니다."));}
    private Row read(ResultSet rs,int n)throws SQLException{
        try{return new Row(rs.getLong("change_request_id"),rs.getLong("policy_id"),rs.getLong("requested_by"),rs.getLong("quote_id"),rs.getString("consent_hash"),
                mapper.readValue(rs.getString("quote_snapshot_json"),QuoteSnapshot.class),rs.getTimestamp("effective_at").toInstant(),rs.getString("review_status"),
                rs.getObject("reviewed_by",Long.class),rs.getString("decision_reason"),rs.getTimestamp("requested_at").toInstant(),rs.getTimestamp("decided_at")==null?null:rs.getTimestamp("decided_at").toInstant());}
        catch(java.io.IOException ex){throw new IllegalStateException("변경 견적을 읽지 못했습니다.",ex);}
    }
    private RequestView view(Row r){
        String state=r.status();
        if("APPROVED".equals(state))state=r.effectiveAt().isAfter(Instant.now(clock))?"SCHEDULED":"EFFECTIVE";
        else if("PENDING_REVIEW".equals(state)&&!r.effectiveAt().isAfter(Instant.now(clock)))state="EXPIRED";
        return new RequestView(r.id(),r.policyId(),r.quoteId(),r.effectiveAt(),r.status(),state,r.quote(),r.reason(),r.requestedAt(),r.decidedAt());
    }
    private void event(Long requestId,Long policyId,String type){
        db.update("INSERT INTO ops_outbox_event(event_id,aggregate_type,aggregate_id,event_type,payload_json) VALUES(?,'POLICY',?,?,CAST(? AS JSONB))",
                "POLICY-CHANGE-REVIEW-"+requestId+"-"+type,policyId.toString(),"POLICY_COVERAGE_CHANGE_"+type,json(Map.of("policyId",policyId,"changeRequestId",requestId,"decision",type)));
    }
    private String json(Object value){try{return mapper.writeValueAsString(value);}catch(Exception ex){throw new IllegalStateException(ex);}}
    private static void fail(ErrorCode c,String message){throw new BusinessException(c,message);}
    public enum Decision{APPROVE,REJECT}
    public record RequestView(Long changeRequestId,Long policyId,Long quoteId,Instant effectiveAt,String reviewStatus,String status,QuoteSnapshot quote,String decisionReason,Instant requestedAt,Instant decidedAt){}
    private record Row(Long id,Long policyId,Long userId,Long quoteId,String hash,QuoteSnapshot quote,Instant effectiveAt,String status,Long reviewer,String reason,Instant requestedAt,Instant decidedAt){}
}
