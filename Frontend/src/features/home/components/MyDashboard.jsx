import React, { useState, useEffect } from 'react';
import { ShieldCheck, AlertCircle, Loader2 } from 'lucide-react';
import { getMyDataStatus, agreeMyData } from '../api/home.api.js';

import CoverageAnalysis from './dashboard/CoverageAnalysis';
import SubscriptionSummary from './dashboard/SubscriptionSummary';
import MyInsurances from './dashboard/MyInsurances';

const MyDashboard = () => {
  const [isLoading, setIsLoading] = useState(false);

  // TODO: Fetch dashboard data when API is ready

  if (isLoading) {
    return (
      <div className="flex min-h-[150px] items-center justify-center rounded-3xl border border-slate-100 bg-white p-6 shadow-sm">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
      </div>
    );
  }

  return (
    <div className="rounded-3xl border border-slate-100 bg-white p-6 shadow-sm">
      <h2 className="mb-4 px-1 text-xl font-bold text-slate-800">마이 대시보드</h2>

      <div className="animate-in fade-in mt-2 grid grid-cols-1 gap-4 duration-500 md:grid-cols-2">
        <div className="md:col-span-2">
          <CoverageAnalysis />
        </div>
        <SubscriptionSummary />
        <MyInsurances />
      </div>
    </div>
  );
};

export default MyDashboard;
