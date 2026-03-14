import React, { useState, useEffect } from "react";
import { ShieldCheck, AlertCircle, Loader2 } from "lucide-react";
import { getMyDataStatus, agreeMyData } from "../api/home.api.js";

import CoverageAnalysis from "./dashboard/CoverageAnalysis";
import SubscriptionSummary from "./dashboard/SubscriptionSummary";
import MyInsurances from "./dashboard/MyInsurances";

const MyDashboard = () => {
    const [isLoading, setIsLoading] = useState(false);

    // TODO: Fetch dashboard data when API is ready

    if (isLoading) {
        return (
            <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-center min-h-[150px]">
                <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
            </div>
        );
    }

    return (
        <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100">
            <h2 className="text-xl font-bold text-slate-800 mb-4 px-1">
                마이 대시보드
            </h2>

            <div className="flex flex-col gap-4 animate-in fade-in duration-500 mt-2">
                <CoverageAnalysis />
                <SubscriptionSummary />
                <MyInsurances />
            </div>
        </div>
    );
};

export default MyDashboard;
