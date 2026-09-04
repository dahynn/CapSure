import React, { useCallback, useEffect, useMemo, useState } from 'react';
import PremiumDelinquencyPanel from './PremiumDelinquencyPanel';
import { useNavigate } from 'react-router-dom';
import {
  Activity,
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  CircleDotDashed,
  Clock3,
  DatabaseZap,
  FileWarning,
  Loader2,
  LockKeyhole,
  Play,
  RefreshCw,
  Radio,
  RotateCcw,
  ServerCog,
  ShieldAlert,
  TimerReset,
  Wrench,
  Workflow,
  WifiOff,
  X,
} from 'lucide-react';
import {
  getOperationsDashboard,
  getPremiumCollectionTimeline,
  replayDeadLetter,
  runPaymentReconciliation,
} from './api/operations.api';

const integer = new Intl.NumberFormat('ko-KR');

const STATUS_STYLE = {
  HEALTHY: {
    label: '정상',
    description: '즉시 확인할 운영 이슈가 없습니다.',
    className: 'border-emerald-400/25 bg-emerald-400/10 text-emerald-200',
    icon: CheckCircle2,
  },
  ATTENTION: {
    label: '확인 필요',
    description: '처리 대기 또는 실행 중인 원장이 있습니다.',
    className: 'border-amber-300/25 bg-amber-300/10 text-amber-100',
    icon: AlertTriangle,
  },
  CRITICAL: {
    label: '조치 필요',
    description: '실패 원장 또는 미처리 DLQ가 발견되었습니다.',
    className: 'border-rose-400/25 bg-rose-400/10 text-rose-100',
    icon: ShieldAlert,
  },
};

const JOB_LABELS = {
  PAYMENT_RECONCILIATION: '결제 대사',
  CATALOG_IMPORT: '상품 배치',
};

const RESULT_LABELS = {
  MATCHED: '일치',
  CORRECTED: '보정 완료',
  STILL_UNKNOWN: '미확정',
  FAILED: '실패',
};

const STATUS_COLORS = {
  COMPLETED: 'bg-emerald-400/10 text-emerald-200',
  RUNNING: 'bg-sky-400/10 text-sky-200',
  FAILED: 'bg-rose-400/10 text-rose-200',
  STOPPED: 'bg-amber-300/10 text-amber-100',
  PENDING: 'bg-amber-300/10 text-amber-100',
  REPLAYED: 'bg-emerald-400/10 text-emerald-200',
  SUCCEEDED: 'bg-emerald-400/10 text-emerald-200',
  CORRECTED: 'bg-emerald-400/10 text-emerald-200',
  MATCHED: 'bg-sky-400/10 text-sky-200',
  STILL_UNKNOWN: 'bg-amber-300/10 text-amber-100',
  TIMEOUT: 'bg-amber-300/10 text-amber-100',
  CIRCUIT_OPEN: 'bg-rose-400/10 text-rose-100',
  REQUESTED: 'bg-sky-400/10 text-sky-200',
  REJECTED: 'bg-rose-400/10 text-rose-100',
  DUE: 'bg-amber-300/10 text-amber-100',
  GRACE: 'bg-rose-400/10 text-rose-100',
  SETTLED: 'bg-emerald-400/10 text-emerald-200',
  OVERPAID: 'bg-violet-400/10 text-violet-200',
  LAPSED: 'bg-rose-400/10 text-rose-100',
  SCHEDULED: 'bg-sky-400/10 text-sky-200',
  SUBMITTED: 'bg-amber-300/10 text-amber-100',
  CANCEL_REQUESTED: 'bg-amber-300/10 text-amber-100',
  CANCELED: 'bg-slate-800 text-slate-300',
  CAPTURED: 'bg-emerald-400/10 text-emerald-200',
  AUTO_REFUND_ELIGIBLE: 'bg-violet-400/10 text-violet-200',
  MANUAL_REVIEW: 'bg-rose-400/10 text-rose-100',
  REFUNDED: 'bg-emerald-400/10 text-emerald-200',
};

const formatCount = (value) => integer.format(Number(value || 0));

const formatDateTime = (value) => {
  if (!value) return '-';
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(new Date(value));
};

const calculateDuration = (startedAt, finishedAt) => {
  if (!startedAt) return '-';
  const end = finishedAt ? new Date(finishedAt) : new Date();
  const milliseconds = Math.max(0, end.getTime() - new Date(startedAt).getTime());
  if (milliseconds < 1000) return `${milliseconds}ms`;
  return `${(milliseconds / 1000).toFixed(milliseconds < 10000 ? 1 : 0)}초`;
};

const formatDurationMs = (value) => {
  const milliseconds = Number(value || 0);
  if (milliseconds < 1000) return `${integer.format(milliseconds)}ms`;
  if (milliseconds < 60000) return `${(milliseconds / 1000).toFixed(1)}초`;
  return `${Math.floor(milliseconds / 60000)}분 ${Math.round((milliseconds % 60000) / 1000)}초`;
};

const RECOVERY_LABELS = {
  DLQ_REPLAY: 'DLQ 재처리',
  PAYMENT_RECONCILIATION: '결제 대사',
};

const MetricCard = ({ icon: Icon, label, value, caption, tone = 'blue' }) => {
  const tones = {
    blue: 'bg-[#82D8FC]/10 text-[#82D8FC]',
    violet: 'bg-[#F2BEF7]/10 text-[#F2BEF7]',
    amber: 'bg-amber-300/10 text-amber-200',
    rose: 'bg-rose-400/10 text-rose-200',
  };
  return (
    <article className="rounded-2xl border border-slate-800 bg-[#09111F] p-4">
      <div className={`flex h-9 w-9 items-center justify-center rounded-xl ${tones[tone]}`}>
        <Icon className="h-4.5 w-4.5" />
      </div>
      <p className="mt-4 text-xs font-bold text-slate-500">{label}</p>
      <p className="mt-1 text-2xl font-black tracking-tight text-white">{formatCount(value)}</p>
      <p className="mt-1 text-[11px] leading-4 text-slate-600">{caption}</p>
    </article>
  );
};

const EmptyState = ({ children }) => (
  <div className="rounded-2xl border border-dashed border-slate-800 px-5 py-8 text-center text-sm text-slate-500">
    {children}
  </div>
);

const CancerInsuranceOperationsPage = () => {
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState(null);
  const [premiumTimeline, setPremiumTimeline] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [forbidden, setForbidden] = useState(false);
  const [actionDialog, setActionDialog] = useState(null);
  const [actionReason, setActionReason] = useState('');
  const [actionPending, setActionPending] = useState(false);
  const [actionError, setActionError] = useState('');
  const [actionNotice, setActionNotice] = useState('');

  const loadDashboard = useCallback(async (silent = false) => {
    if (silent) setRefreshing(true);
    else setLoading(true);
    setError('');
    setForbidden(false);
    try {
      const [dashboardData, timelineData] = await Promise.all([
        getOperationsDashboard(8),
        getPremiumCollectionTimeline(8),
      ]);
      setDashboard(dashboardData);
      setPremiumTimeline(timelineData);
    } catch (requestError) {
      if (requestError.response?.status === 403) {
        setForbidden(true);
      }
      setError(requestError.message || '운영 지표를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  const openActionDialog = (type, eventId = null) => {
    setActionDialog({ type, eventId });
    setActionReason('');
    setActionError('');
  };

  const closeActionDialog = () => {
    if (actionPending) return;
    setActionDialog(null);
    setActionReason('');
    setActionError('');
  };

  const submitRecoveryAction = async (event) => {
    event.preventDefault();
    const reason = actionReason.trim();
    if (!reason) {
      setActionError('운영 조치 사유를 입력해주세요.');
      return;
    }

    setActionPending(true);
    setActionError('');
    setActionNotice('');
    try {
      const result =
        actionDialog.type === 'DLQ_REPLAY'
          ? await replayDeadLetter(actionDialog.eventId, reason)
          : await runPaymentReconciliation(reason);
      const recovery = result.recovery;
      const label = RECOVERY_LABELS[recovery.actionType] || '운영 조치';
      setActionNotice(
        `${label} 완료 · 조치 ${formatDurationMs(recovery.actionDurationMs)} · 복구 ${formatDurationMs(recovery.recoveryTimeMs)}`
      );
      setActionDialog(null);
      setActionReason('');
      await loadDashboard(true);
    } catch (requestError) {
      setActionError(
        requestError.payload?.message || requestError.message || '운영 조치를 완료하지 못했습니다.'
      );
    } finally {
      setActionPending(false);
    }
  };

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'auto' });
    loadDashboard();
    const intervalId = window.setInterval(() => loadDashboard(true), 30000);
    return () => window.clearInterval(intervalId);
  }, [loadDashboard]);

  const metrics = useMemo(() => {
    if (!dashboard) return null;
    const outboxBacklog =
      dashboard.outbox.pendingCount +
      dashboard.outbox.processingCount +
      dashboard.outbox.failedCount;
    const processed = Number(dashboard.reconciliation.processedCount || 0);
    const resolved = Number(dashboard.reconciliation.resolvedCount || 0);
    return {
      outboxBacklog,
      resolutionRate: processed === 0 ? 0 : Math.round((resolved / processed) * 1000) / 10,
    };
  }, [dashboard]);

  if (loading) {
    return (
      <div className="flex min-h-[620px] flex-col items-center justify-center gap-4 px-6 text-center">
        <Loader2 className="h-9 w-9 animate-spin text-[#82D8FC]" />
        <div>
          <p className="font-black text-white">금융 원장을 집계하고 있습니다</p>
          <p className="mt-1 text-sm text-slate-500">Outbox·DLQ·배치·결제 대사를 확인합니다.</p>
        </div>
      </div>
    );
  }

  if (!dashboard) {
    return (
      <div className="flex min-h-[620px] flex-col items-center justify-center px-8 text-center">
        {forbidden ? (
          <LockKeyhole className="h-11 w-11 text-amber-200" />
        ) : (
          <ServerCog className="h-11 w-11 text-rose-300" />
        )}
        <h1 className="mt-5 text-xl font-black text-white">
          {forbidden ? '운영자 권한이 필요합니다' : '운영 지표를 불러오지 못했습니다'}
        </h1>
        <p className="mt-2 max-w-sm text-sm leading-6 text-slate-400">
          {forbidden
            ? '고객 채널과 운영 채널을 분리했습니다. 운영자 계정으로 다시 로그인해주세요.'
            : error}
        </p>
        <div className="mt-7 flex gap-3">
          <button
            type="button"
            onClick={() => navigate(forbidden ? '/login' : '/cancer-insurance')}
            className="rounded-2xl border border-slate-700 px-5 py-3 text-sm font-black text-white"
          >
            {forbidden ? '다시 로그인' : '상품으로 돌아가기'}
          </button>
          {!forbidden && (
            <button
              type="button"
              onClick={() => loadDashboard()}
              className="rounded-2xl bg-[#82D8FC] px-5 py-3 text-sm font-black text-[#020715]"
            >
              다시 시도
            </button>
          )}
        </div>
      </div>
    );
  }

  const status = STATUS_STYLE[dashboard.overallStatus] || STATUS_STYLE.ATTENTION;
  const StatusIcon = status.icon;

  return (
    <div className="min-h-full bg-[#020715] pb-36">
      <header className="flex items-center justify-between px-5 py-5">
        <div className="flex items-center">
          <button
            type="button"
            onClick={() => navigate('/cancer-insurance')}
            className="-ml-2 rounded-full p-2 text-white transition-colors hover:bg-slate-800"
            aria-label="암보험 상품으로 돌아가기"
          >
            <ArrowLeft className="h-6 w-6" />
          </button>
          <div className="ml-2">
            <p className="text-xs font-bold text-[#82D8FC]">BACK OFFICE · READ MODEL</p>
            <h1 className="mt-0.5 text-xl font-black text-white">보험 운영 콘솔</h1>
          </div>
        </div>
        <button
          type="button"
          onClick={() => loadDashboard(true)}
          disabled={refreshing}
          className="rounded-2xl border border-slate-800 bg-[#09111F] p-3 text-slate-300 disabled:opacity-50"
          aria-label="운영 지표 새로고침"
        >
          <RefreshCw className={`h-4.5 w-4.5 ${refreshing ? 'animate-spin' : ''}`} />
        </button>
      </header>

      <main className="space-y-6 px-6">
        {actionNotice && (
          <div className="flex items-start justify-between gap-3 rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-xs text-emerald-100">
            <span className="flex items-start gap-2 leading-5">
              <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />
              {actionNotice}
            </span>
            <button
              type="button"
              onClick={() => setActionNotice('')}
              className="rounded-full p-1 text-emerald-200/70 hover:bg-black/10"
              aria-label="운영 조치 완료 알림 닫기"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          </div>
        )}

        <section className={`rounded-[28px] border p-5 ${status.className}`}>
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-start gap-3">
              <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-black/15">
                <StatusIcon className="h-6 w-6" />
              </span>
              <div>
                <p className="text-xs font-bold opacity-70">통합 운영 상태</p>
                <h2 className="mt-1 text-2xl font-black">{status.label}</h2>
                <p className="mt-1 text-xs leading-5 opacity-80">{status.description}</p>
              </div>
            </div>
            <span className="shrink-0 rounded-full bg-black/15 px-3 py-1 text-[10px] font-black">
              30초 자동 갱신
            </span>
          </div>
          <div className="border-current/10 mt-5 border-t pt-4 text-[11px] opacity-70">
            기준 시각 {formatDateTime(dashboard.refreshedAt)}
          </div>
        </section>

        <section className="rounded-[26px] border border-[#82D8FC]/20 bg-[#82D8FC]/5 p-5">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-xs font-bold text-[#82D8FC]">OPERATIONS ACTION</p>
              <h2 className="mt-1 text-lg font-black text-white">관리자 수동 조치</h2>
              <p className="mt-2 text-xs leading-5 text-slate-400">
                실행자와 사유, 감지 시각부터 복구까지 걸린 시간을 원장에 남깁니다.
              </p>
            </div>
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-[#82D8FC]/10 text-[#82D8FC]">
              <Wrench className="h-5 w-5" />
            </span>
          </div>
          <button
            type="button"
            onClick={() => openActionDialog('PAYMENT_RECONCILIATION')}
            disabled={dashboard.reconciliation.dueOrderCount === 0}
            className="mt-5 flex w-full items-center justify-between rounded-2xl bg-[#82D8FC] px-4 py-3.5 text-left text-sm font-black text-[#020715] transition-opacity disabled:cursor-not-allowed disabled:opacity-40"
          >
            <span className="flex items-center gap-2">
              <Play className="h-4 w-4 fill-current" />
              결제 대사 수동 실행
            </span>
            <span className="text-xs">
              대상 {formatCount(dashboard.reconciliation.dueOrderCount)}건
            </span>
          </button>
          {dashboard.reconciliation.dueOrderCount === 0 && (
            <p className="mt-2 text-center text-[10px] text-slate-600">
              현재 실행 가능한 미확정 결제가 없습니다.
            </p>
          )}
        </section>

        <section>
          <div className="mb-4 flex items-end justify-between">
            <div>
              <p className="text-xs font-bold text-[#82D8FC]">CONTROL TOTAL</p>
              <h2 className="mt-1 text-lg font-black text-white">현재 처리해야 할 원장</h2>
            </div>
            <span className="text-[11px] text-slate-600">DB 실시간 집계</span>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <MetricCard
              icon={Workflow}
              label="Outbox 미발행"
              value={metrics.outboxBacklog}
              caption={`발행 완료 ${formatCount(dashboard.outbox.publishedCount)}건`}
            />
            <MetricCard
              icon={FileWarning}
              label="미처리 DLQ"
              value={dashboard.outbox.pendingDeadLetterCount}
              caption="재처리 판단이 필요한 이벤트"
              tone={dashboard.outbox.pendingDeadLetterCount > 0 ? 'rose' : 'violet'}
            />
            <MetricCard
              icon={CircleDotDashed}
              label="결제 미확정"
              value={dashboard.reconciliation.waitingOrderCount}
              caption={`지금 실행 가능 ${formatCount(dashboard.reconciliation.dueOrderCount)}건`}
              tone="amber"
            />
            <MetricCard
              icon={DatabaseZap}
              label="감사원장 투영"
              value={dashboard.outbox.projectedAuditCount}
              caption="변경 불가 금융 이벤트 이력"
              tone="violet"
            />
          </div>
        </section>

        <section className="rounded-[26px] border border-[#82D8FC]/20 bg-[#09111F] p-5">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs font-bold text-[#82D8FC]">FINANCIAL INTERFACE</p>
              <h2 className="mt-1 text-lg font-black text-white">결제 전문 · 기관 상태</h2>
              <p className="mt-2 text-xs leading-5 text-slate-400">
                채널 요청과 외부기관 응답을 주문번호·상관관계 ID 기준으로 확인합니다.
              </p>
            </div>
            <span
              className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl ${dashboard.paymentInterfaceCircuit.open ? 'bg-rose-400/10 text-rose-200' : 'bg-[#82D8FC]/10 text-[#82D8FC]'}`}
            >
              {dashboard.paymentInterfaceCircuit.open ? (
                <WifiOff className="h-5 w-5" />
              ) : (
                <Radio className="h-5 w-5" />
              )}
            </span>
          </div>
          <div className="mt-5 grid grid-cols-4 gap-2 text-center">
            {[
              ['전문', dashboard.paymentInterface.totalMessageCount, 'text-white'],
              ['성공', dashboard.paymentInterface.succeededResponseCount, 'text-emerald-200'],
              ['timeout', dashboard.paymentInterface.timeoutResponseCount, 'text-amber-200'],
              ['차단', dashboard.paymentInterface.circuitOpenResponseCount, 'text-rose-200'],
            ].map(([label, value, className]) => (
              <div key={label} className="rounded-2xl bg-slate-950/55 px-2 py-3">
                <p className={`text-lg font-black ${className}`}>{formatCount(value)}</p>
                <p className="mt-1 text-[10px] font-bold text-slate-600">{label}</p>
              </div>
            ))}
          </div>
          <div className="mt-4 flex items-center justify-between rounded-2xl border border-slate-800 px-4 py-3 text-xs">
            <span className="text-slate-400">현재 circuit</span>
            <strong className={dashboard.paymentInterfaceCircuit.open ? 'text-rose-200' : 'text-emerald-200'}>
              {dashboard.paymentInterfaceCircuit.open
                ? `OPEN · ${formatDateTime(dashboard.paymentInterfaceCircuit.openUntil)}까지`
                : `CLOSED · timeout ${dashboard.paymentInterfaceCircuit.consecutiveTimeouts}/${dashboard.paymentInterfaceCircuit.failureThreshold}`}
            </strong>
          </div>
          <p className="mt-3 text-[10px] text-slate-600">
            마지막 전문 {formatDateTime(dashboard.paymentInterface.latestMessageAt)}
          </p>
        </section>

        <section>
          <div className="mb-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-bold text-[#82D8FC]">MESSAGE TIMELINE</p>
              <h2 className="mt-1 text-lg font-black text-white">최근 결제 전문</h2>
            </div>
            <span className="text-xs text-slate-600">
              최근 {dashboard.recentPaymentInterfaceMessages.length}건
            </span>
          </div>
          <div className="space-y-3">
            {dashboard.recentPaymentInterfaceMessages.length === 0 ? (
              <EmptyState>아직 외부기관에 기록된 결제 전문이 없습니다.</EmptyState>
            ) : (
              dashboard.recentPaymentInterfaceMessages.map((message) => (
                <article
                  key={message.financialMessageId}
                  className="rounded-2xl border border-slate-800 bg-[#09111F] p-4"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-black text-white">{message.businessKey}</p>
                      <p className="mt-1 truncate text-[11px] text-slate-600">
                        {message.direction === 'OUTBOUND_REQUEST' ? '기관 요청' : '기관 응답'} · {message.messageType}
                      </p>
                    </div>
                    <span
                      className={`shrink-0 rounded-full px-2.5 py-1 text-[10px] font-black ${STATUS_COLORS[message.status] || 'bg-slate-800 text-slate-300'}`}
                    >
                      {message.status}
                    </span>
                  </div>
                  <div className="mt-3 rounded-xl bg-slate-950/55 px-3 py-2 text-[10px] text-slate-500">
                    <p className="truncate">corr · {message.correlationId}</p>
                    {message.idempotencyKey && <p className="mt-1 truncate">idem · {message.idempotencyKey}</p>}
                  </div>
                  <div className="mt-3 flex items-center justify-between text-[10px] text-slate-600">
                    <span>{message.interfaceName}</span>
                    <span>{formatDateTime(message.occurredAt)}</span>
                  </div>
                  {message.errorCode && (
                    <p className="mt-3 rounded-xl bg-rose-400/10 px-3 py-2 text-[11px] text-rose-200">
                      {message.errorCode}
                    </p>
                  )}
                </article>
              ))
            )}
          </div>
        </section>

        <section className="rounded-[26px] border border-slate-800 bg-[#09111F] p-5">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs font-bold text-[#F2BEF7]">PAYMENT RECONCILIATION</p>
              <h2 className="mt-1 text-lg font-black text-white">결제 대사 누적 성과</h2>
            </div>
            <span className="rounded-full bg-[#F2BEF7]/10 px-3 py-1 text-xs font-black text-[#F2BEF7]">
              해결률 {metrics.resolutionRate}%
            </span>
          </div>
          <div className="mt-5 grid grid-cols-4 gap-2 text-center">
            {[
              ['처리', dashboard.reconciliation.processedCount, 'text-white'],
              ['해결', dashboard.reconciliation.resolvedCount, 'text-emerald-200'],
              ['미확정', dashboard.reconciliation.stillUnknownCount, 'text-amber-200'],
              ['실패', dashboard.reconciliation.failedCount, 'text-rose-200'],
            ].map(([label, value, className]) => (
              <div key={label} className="rounded-2xl bg-slate-950/55 px-2 py-3">
                <p className={`text-lg font-black ${className}`}>{formatCount(value)}</p>
                <p className="mt-1 text-[10px] font-bold text-slate-600">{label}</p>
              </div>
            ))}
          </div>
          <div className="mt-4 flex items-center justify-between rounded-2xl border border-slate-800 px-4 py-3 text-xs">
            <span className="flex items-center gap-2 text-slate-400">
              <LockKeyhole className="h-3.5 w-3.5" /> 작업자 선점 중
            </span>
            <strong className="text-white">
              {formatCount(dashboard.reconciliation.lockedOrderCount)}건
            </strong>
          </div>
        </section>

        <section className="rounded-[26px] border border-slate-800 bg-[#09111F] p-5">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs font-bold text-emerald-300">RECOVERY TIME</p>
              <h2 className="mt-1 text-lg font-black text-white">장애 복구 훈련 성과</h2>
            </div>
            <span className="rounded-full bg-emerald-400/10 px-3 py-1 text-xs font-black text-emerald-200">
              평균 {formatDurationMs(dashboard.recovery.averageRecoveryTimeMs)}
            </span>
          </div>
          <div className="mt-5 grid grid-cols-3 gap-2 text-center">
            {[
              ['전체 조치', dashboard.recovery.totalActionCount, 'text-white'],
              ['성공', dashboard.recovery.succeededActionCount, 'text-emerald-200'],
              ['실패', dashboard.recovery.failedActionCount, 'text-rose-200'],
            ].map(([label, value, className]) => (
              <div key={label} className="rounded-2xl bg-slate-950/55 px-2 py-3">
                <p className={`text-lg font-black ${className}`}>{formatCount(value)}</p>
                <p className="mt-1 text-[10px] font-bold text-slate-600">{label}</p>
              </div>
            ))}
          </div>
          <div className="mt-4 flex items-center justify-between rounded-2xl border border-slate-800 px-4 py-3 text-xs">
            <span className="flex items-center gap-2 text-slate-400">
              <TimerReset className="h-3.5 w-3.5" /> 최근 복구 시간
            </span>
            <strong className="text-white">
              {formatDurationMs(dashboard.recovery.latestRecoveryTimeMs)}
            </strong>
          </div>
        </section>

        <section>
          <div className="mb-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-bold text-emerald-300">RECOVERY AUDIT</p>
              <h2 className="mt-1 text-lg font-black text-white">최근 관리자 조치</h2>
            </div>
            <span className="text-xs text-slate-600">
              최근 {dashboard.recentRecoveryActions.length}건
            </span>
          </div>
          <div className="space-y-3">
            {dashboard.recentRecoveryActions.length === 0 ? (
              <EmptyState>아직 기록된 수동 복구 조치가 없습니다.</EmptyState>
            ) : (
              dashboard.recentRecoveryActions.map((action) => (
                <article
                  key={action.recoveryActionId}
                  className="rounded-2xl border border-slate-800 bg-[#09111F] p-4"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-sm font-black text-white">
                        {RECOVERY_LABELS[action.actionType] || action.actionType}
                      </p>
                      <p className="mt-1 truncate text-[11px] text-slate-600">
                        {action.actorName} · {action.targetId || '실행 준비'}
                      </p>
                    </div>
                    <span
                      className={`shrink-0 rounded-full px-2.5 py-1 text-[10px] font-black ${STATUS_COLORS[action.status] || 'bg-slate-800 text-slate-300'}`}
                    >
                      {action.status}
                    </span>
                  </div>
                  <p className="mt-3 rounded-xl bg-slate-950/55 px-3 py-2 text-xs leading-5 text-slate-300">
                    {action.reason}
                  </p>
                  <div className="mt-3 grid grid-cols-2 gap-2 text-[10px]">
                    <div className="rounded-xl border border-slate-800 px-3 py-2 text-slate-500">
                      조치 시간
                      <strong className="mt-1 block text-xs text-white">
                        {formatDurationMs(action.actionDurationMs)}
                      </strong>
                    </div>
                    <div className="rounded-xl border border-slate-800 px-3 py-2 text-slate-500">
                      전체 복구 시간
                      <strong className="mt-1 block text-xs text-emerald-200">
                        {formatDurationMs(action.recoveryTimeMs)}
                      </strong>
                    </div>
                  </div>
                  <p className="mt-3 text-[10px] text-slate-600">
                    감지 {formatDateTime(action.detectedAt)} · 종료{' '}
                    {formatDateTime(action.completedAt)}
                  </p>
                  {action.errorReason && (
                    <p className="mt-3 rounded-xl bg-rose-400/10 px-3 py-2 text-[11px] leading-5 text-rose-200">
                      {action.errorReason}
                    </p>
                  )}
                </article>
              ))
            )}
          </div>
        </section>

        <section>
          <div className="mb-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-bold text-[#82D8FC]">BATCH EXECUTION</p>
              <h2 className="mt-1 text-lg font-black text-white">최근 배치 실행</h2>
            </div>
            <span className="text-xs text-slate-600">최근 {dashboard.recentJobs.length}건</span>
          </div>
          <div className="space-y-3">
            {dashboard.recentJobs.length === 0 ? (
              <EmptyState>아직 실행된 배치가 없습니다.</EmptyState>
            ) : (
              dashboard.recentJobs.map((job) => {
                const isPaymentJob = job.jobName === 'PAYMENT_RECONCILIATION';
                return (
                  <article
                    key={job.jobExecutionId}
                    className="rounded-2xl border border-slate-800 bg-[#09111F] p-4"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <p className="truncate text-sm font-black text-white">
                          {JOB_LABELS[job.jobName] || job.jobName}
                        </p>
                        <p className="mt-1 truncate text-[11px] text-slate-600">
                          {job.instanceKey} · #{job.executionNo}
                        </p>
                      </div>
                      <span
                        className={`shrink-0 rounded-full px-2.5 py-1 text-[10px] font-black ${STATUS_COLORS[job.status] || 'bg-slate-800 text-slate-300'}`}
                      >
                        {job.status}
                      </span>
                    </div>
                    <div className="mt-4 grid grid-cols-3 gap-2 rounded-xl bg-slate-950/50 px-3 py-3 text-center">
                      <div>
                        <p className="text-sm font-black text-white">
                          {formatCount(isPaymentJob ? job.processedCount : job.inputCount)}
                        </p>
                        <p className="text-[9px] text-slate-600">입력/처리</p>
                      </div>
                      <div>
                        <p className="text-sm font-black text-emerald-200">
                          {formatCount(isPaymentJob ? job.resolvedCount : job.acceptedCount)}
                        </p>
                        <p className="text-[9px] text-slate-600">정상</p>
                      </div>
                      <div>
                        <p className="text-sm font-black text-rose-200">
                          {formatCount(isPaymentJob ? job.failedCount : job.quarantinedCount)}
                        </p>
                        <p className="text-[9px] text-slate-600">실패/격리</p>
                      </div>
                    </div>
                    <div className="mt-3 flex items-center justify-between text-[10px] text-slate-600">
                      <span className="flex items-center gap-1.5">
                        <Clock3 className="h-3 w-3" />
                        {formatDateTime(job.startedAt)}
                      </span>
                      <span className="flex items-center gap-1.5">
                        <TimerReset className="h-3 w-3" />
                        {calculateDuration(job.startedAt, job.finishedAt)}
                      </span>
                    </div>
                    {job.errorReason && (
                      <p className="mt-3 rounded-xl bg-rose-400/10 px-3 py-2 text-[11px] leading-5 text-rose-200">
                        {job.errorReason}
                      </p>
                    )}
                  </article>
                );
              })
            )}
          </div>
        </section>

        <section>
          <div className="mb-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-bold text-rose-300">DEAD LETTER QUEUE</p>
              <h2 className="mt-1 text-lg font-black text-white">이벤트 장애함</h2>
            </div>
            <FileWarning className="h-5 w-5 text-slate-700" />
          </div>
          <div className="space-y-3">
            {dashboard.deadLetters.length === 0 ? (
              <EmptyState>미처리 장애 이벤트가 없습니다.</EmptyState>
            ) : (
              dashboard.deadLetters.map((item) => (
                <article
                  key={item.deadLetterId}
                  className="rounded-2xl border border-rose-400/15 bg-rose-400/5 p-4"
                >
                  <div className="flex items-center justify-between gap-3">
                    <p className="min-w-0 truncate text-xs font-black text-white">{item.eventId}</p>
                    <span
                      className={`shrink-0 rounded-full px-2.5 py-1 text-[10px] font-black ${STATUS_COLORS[item.replayStatus] || 'bg-slate-800 text-slate-300'}`}
                    >
                      {item.replayStatus}
                    </span>
                  </div>
                  <p className="mt-3 text-xs leading-5 text-rose-100/80">{item.errorReason}</p>
                  <p className="mt-2 text-[10px] text-slate-600">
                    유입 {formatDateTime(item.createdAt)}
                  </p>
                  {item.replayStatus === 'PENDING' && (
                    <button
                      type="button"
                      onClick={() => openActionDialog('DLQ_REPLAY', item.eventId)}
                      className="mt-4 flex w-full items-center justify-center gap-2 rounded-xl border border-rose-300/20 bg-rose-300/10 px-3 py-2.5 text-xs font-black text-rose-100"
                    >
                      <RotateCcw className="h-3.5 w-3.5" />
                      사유를 남기고 재처리
                    </button>
                  )}
                </article>
              ))
            )}
          </div>
        </section>

        <section>
          <div className="mb-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-bold text-[#F2BEF7]">RECONCILIATION LOG</p>
              <h2 className="mt-1 text-lg font-black text-white">최근 결제 대사 판정</h2>
            </div>
            <Activity className="h-5 w-5 text-slate-700" />
          </div>
          <div className="overflow-hidden rounded-2xl border border-slate-800 bg-[#09111F]">
            {dashboard.recentReconciliations.length === 0 ? (
              <EmptyState>아직 결제 대사 판정이 없습니다.</EmptyState>
            ) : (
              dashboard.recentReconciliations.map((item, index) => (
                <article
                  key={item.reconciliationId}
                  className={`p-4 ${index > 0 ? 'border-t border-slate-800' : ''}`}
                >
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-xs font-black text-white">주문 #{item.targetId}</p>
                      <p className="mt-1 text-[10px] text-slate-600">
                        {item.provider} · {formatDateTime(item.executedAt)}
                      </p>
                    </div>
                    <span
                      className={`rounded-full px-2.5 py-1 text-[10px] font-black ${STATUS_COLORS[item.result] || 'bg-rose-400/10 text-rose-200'}`}
                    >
                      {RESULT_LABELS[item.result] || item.result}
                    </span>
                  </div>
                  <div className="mt-3 flex items-center gap-2 text-[11px]">
                    <span className="rounded-lg bg-slate-950/60 px-2 py-1 text-slate-400">
                      내부 {item.localStatus}
                    </span>
                    <span className="text-slate-700">→</span>
                    <span className="rounded-lg bg-slate-950/60 px-2 py-1 text-slate-300">
                      PG {item.providerStatus}
                    </span>
                  </div>
                </article>
              ))
            )}
          </div>
        </section>

        <section>
          <div className="mb-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-bold text-amber-200">PREMIUM COLLECTION</p>
              <h2 className="mt-1 text-lg font-black text-white">보험료 수납·환급 타임라인</h2>
            </div>
            <TimerReset className="h-5 w-5 text-slate-700" />
          </div>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <MetricCard icon={Clock3} label="납부 대기" value={premiumTimeline?.dueCount} caption="보험료 채권" tone="amber" />
            <MetricCard icon={AlertTriangle} label="유예 중" value={premiumTimeline?.graceCount} caption="독촉·유예기간" tone="rose" />
            <MetricCard icon={CircleDotDashed} label="초과 수납" value={premiumTimeline?.overpaidCount} caption="대사 대상" tone="violet" />
            <MetricCard icon={RotateCcw} label="환급 대기" value={premiumTimeline?.refundPendingCount} caption="자동·수동 검토" tone="blue" />
          </div>
          <div className="mt-3 overflow-hidden rounded-2xl border border-slate-800 bg-[#09111F]">
            {!premiumTimeline || premiumTimeline.items.length === 0 ? (
              <EmptyState>아직 보험료 수납 원장이 없습니다.</EmptyState>
            ) : premiumTimeline.items.map((item, index) => (
              <article key={item.premiumReceivableId} className={`p-4 ${index ? 'border-t border-slate-800' : ''}`}>
                <div className="flex items-center justify-between gap-3">
                  <div><p className="text-xs font-black text-white">증권 #{item.policyId} · {item.billingCycle}</p><p className="mt-1 text-[10px] text-slate-500">청구 {Number(item.amountDue).toLocaleString()}원 · 수납 {Number(item.amountSettled).toLocaleString()}원</p></div>
                  <span className={`rounded-full px-2.5 py-1 text-[10px] font-black ${STATUS_COLORS[item.receivableStatus] || 'bg-slate-800 text-slate-300'}`}>{item.receivableStatus}</span>
                </div>
                <p className="mt-3 text-[11px] text-slate-400">자동출금 {item.instructionStatus || '-'} · 환급 {item.refundStatus || '없음'}</p>
                <p className="mt-2 text-[11px] text-amber-100">계약 {item.policyStatus} · 납부기일 {item.dueDate} · 유예 종료 {item.effectiveGraceEndsOn || '독촉 완료 전'}</p>
                <p className="mt-1 text-[11px] text-slate-400">독촉 {item.noticeStatus === 'SIMULATED_DELIVERED' ? '모의 완료' : item.noticeStatus === 'FAILED' ? '실패 · 재시도 필요' : '없음'} · 변경 사유 {({ OVERDUE_PREMIUM: '보험료 미납', GRACE_EXPIRED_UNPAID: '유예 종료 후 미납', ARREARS_SETTLED: '연체분 완납' })[item.changeReason] || '-'}</p>
                {item.lapsedAt && <p className="mt-1 text-[11px] text-rose-300">실효 시점 {formatDateTime(item.lapsedAt)}</p>}
                {item.lateReviewCount > 0 && <p className="mt-1 text-[11px] text-rose-300">실효 후 수납 검토 {item.lateReviewCount}건 · 자동 부활 안 함</p>}
              </article>
            ))}
          </div>
          <p className="mt-3 text-xs text-slate-400">실효 계약 {formatCount(premiumTimeline?.lapsedPolicyCount)}건 · 실효 후 수납 검토 {formatCount(premiumTimeline?.lateSettlementReviewCount)}건</p>
        </section>

        <PremiumDelinquencyPanel onUpdated={() => loadDashboard(true)} />

        <section className="flex items-start gap-3 rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
          <ServerCog className="mt-0.5 h-4 w-4 shrink-0 text-slate-500" />
          <p className="text-xs leading-5 text-slate-500">
            고객 화면과 분리된 관리자 운영 화면입니다. 조치 유형에 따라 실행자·사유·처리 결과와
            실행 이력을 별도 원장에 기록합니다.
          </p>
        </section>
      </main>

      {actionDialog && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/70 px-4 pb-5 backdrop-blur-sm">
          <form
            onSubmit={submitRecoveryAction}
            className="w-full max-w-md rounded-[28px] border border-slate-700 bg-[#09111F] p-5 shadow-2xl"
          >
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-xs font-bold text-[#82D8FC]">AUDITED ACTION</p>
                <h2 className="mt-1 text-xl font-black text-white">
                  {RECOVERY_LABELS[actionDialog.type]}
                </h2>
                {actionDialog.eventId && (
                  <p className="mt-1 max-w-[280px] truncate text-xs text-slate-500">
                    {actionDialog.eventId}
                  </p>
                )}
              </div>
              <button
                type="button"
                onClick={closeActionDialog}
                disabled={actionPending}
                className="rounded-full p-2 text-slate-400 hover:bg-slate-800"
                aria-label="운영 조치 창 닫기"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <label
              className="mt-5 block text-xs font-bold text-slate-300"
              htmlFor="recovery-reason"
            >
              조치 사유
            </label>
            <textarea
              id="recovery-reason"
              value={actionReason}
              onChange={(event) => setActionReason(event.target.value)}
              maxLength={500}
              rows={4}
              autoFocus
              disabled={actionPending}
              placeholder="장애 원인 확인 내용과 수동 조치가 필요한 이유를 입력해주세요."
              className="mt-2 w-full resize-none rounded-2xl border border-slate-700 bg-slate-950/70 px-4 py-3 text-sm leading-6 text-white outline-none placeholder:text-slate-700 focus:border-[#82D8FC] disabled:opacity-60"
            />
            <div className="mt-2 flex items-start justify-between gap-3 text-[10px]">
              <p className={actionError ? 'text-rose-300' : 'text-slate-600'}>
                {actionError ||
                  (actionDialog.type === 'DLQ_REPLAY'
                    ? '재투입 후 해당 이벤트 발행까지 확인하며 관리자와 복구 시간을 기록합니다.'
                    : '로그인한 관리자와 결제 대사 완료 시간이 감사 원장에 기록됩니다.')}
              </p>
              <span className="shrink-0 text-slate-700">{actionReason.length}/500</span>
            </div>

            <button
              type="submit"
              disabled={actionPending || !actionReason.trim()}
              className="mt-5 flex w-full items-center justify-center gap-2 rounded-2xl bg-[#82D8FC] px-4 py-3.5 text-sm font-black text-[#020715] disabled:opacity-40"
            >
              {actionPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : actionDialog.type === 'DLQ_REPLAY' ? (
                <RotateCcw className="h-4 w-4" />
              ) : (
                <Play className="h-4 w-4 fill-current" />
              )}
              {actionPending ? '처리하고 있습니다' : '사유를 남기고 실행'}
            </button>
          </form>
        </div>
      )}
    </div>
  );
};

export default CancerInsuranceOperationsPage;
