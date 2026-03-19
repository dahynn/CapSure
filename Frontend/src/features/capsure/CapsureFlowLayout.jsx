import React from 'react';
import { Outlet } from 'react-router-dom';
import { CapsureProvider } from './context/CapsureContext';

const CapsureFlowLayout = () => {
    return (
        <CapsureProvider>
            <div className="flex min-h-screen flex-col items-center bg-[#020715]">
                <div className="animate-in slide-in-from-bottom-8 fade-in relative flex min-h-screen w-full max-w-[560px] flex-col bg-[#020715] duration-500">
                    <Outlet />
                </div>
            </div>
        </CapsureProvider>
    );
};

export default CapsureFlowLayout;
