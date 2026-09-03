import React, { useCallback, useEffect, useMemo, useState } from 'react';
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
  RefreshCw,
  ServerCog,
  ShieldAlert,
  TimerReset,
  Workflow,
} from 'lucide-react';
import { getOperationsDashboard } from './api/operations.api';

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
  CORRECTED: 'bg-emerald-400/10 text-emerald-200',
  MATCHED: 'bg-sky-400/10 text-sky-200',
  STILL_UNKNOWN: 'bg-amber-300/10 text-amber-100',
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
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [forbidden, setForbidden] = useState(false);

  const loadDashboard = useCallback(async (silent = false) => {
    if (silent) setRefreshing(true);
    else setLoading(true);
    setError('');
    setForbidden(false);
    try {
      setDashboard(await getOperationsDashboard(8));
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

        <section className="flex items-start gap-3 rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
          <ServerCog className="mt-0.5 h-4 w-4 shrink-0 text-slate-500" />
          <p className="text-xs leading-5 text-slate-500">
            고객 화면과 분리된 관리자 조회 전용 화면입니다. 실제 운영에서는 알림·재처리 승인·권한
            감사가 추가됩니다.
          </p>
        </section>
      </main>
    </div>
  );
};

export default CancerInsuranceOperationsPage;
