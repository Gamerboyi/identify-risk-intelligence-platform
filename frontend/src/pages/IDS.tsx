import React, { useState } from 'react';
import { ShieldAlert, Terminal, Activity, CheckCircle, AlertOctagon, Loader2 } from 'lucide-react';
import clsx from 'clsx';
import apiClient from '../api/client';

export default function IDS() {
  const [ipAddress, setIpAddress] = useState('192.168.1.100');
  const [requestUri, setRequestUri] = useState('/api/auth/login');
  const [requestBody, setRequestBody] = useState('{"username": "admin", "password": "\' OR 1=1 --"}');
  
  const [loading, setLoading] = useState(false);
  const [analysis, setAnalysis] = useState<any>(null);

  const handleAnalyze = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      // Create x-www-form-urlencoded params since the Controller uses @RequestParam
      const query = new URLSearchParams();
      query.append('ipAddress', ipAddress);
      query.append('requestUri', requestUri);
      if (requestBody) query.append('requestBody', requestBody);

      // Send via Query String so we completely bypass the JSON vs Form-Data body argument!
      const response = await apiClient.post(`/ids/analyze?${query.toString()}`);
      setAnalysis(response.data);
    } catch (err: any) {
      console.error('Analysis failed', err);
      // Display the actual error payload on the screen instead of failing silently
      setAnalysis({ 
        isThreat: true, 
        threats: ['ERROR: ' + (err.response?.data?.message || err.message)],
        errorData: err.response?.data
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight flex items-center">
            <Activity className="text-emerald-500 mr-3" /> Intrusion Detection System
          </h1>
          <p className="text-slate-400 text-sm mt-1">Real-time threat analysis and pattern matching simulator</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Threat Simulator Form */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl shadow-sm overflow-hidden p-6">
          <div className="flex items-center mb-6 border-b border-slate-800 pb-4">
            <Terminal className="text-slate-400 mr-2" size={20} />
            <h2 className="text-lg font-semibold text-white">Threat Analyzer Simulator</h2>
          </div>
          
          <form onSubmit={handleAnalyze} className="space-y-4">
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-slate-300">Target IP Address</label>
              <input 
                type="text" 
                value={ipAddress}
                onChange={(e) => setIpAddress(e.target.value)}
                required
                className="block w-full px-3 py-2 border border-slate-700 rounded-lg bg-slate-950 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 font-mono text-sm"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-sm font-medium text-slate-300">Request URI</label>
              <input 
                type="text" 
                value={requestUri}
                onChange={(e) => setRequestUri(e.target.value)}
                required
                className="block w-full px-3 py-2 border border-slate-700 rounded-lg bg-slate-950 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 font-mono text-sm"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-sm font-medium text-slate-300">Request Payload (Optional)</label>
              <textarea 
                value={requestBody}
                onChange={(e) => setRequestBody(e.target.value)}
                rows={4}
                className="block w-full px-3 py-2 border border-slate-700 rounded-lg bg-slate-950 text-emerald-400 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 font-mono text-xs resize-none"
              />
            </div>
            
            <button 
              type="submit" 
              disabled={loading}
              className="w-full flex items-center justify-center space-x-2 bg-emerald-600 hover:bg-emerald-700 text-white py-2 rounded-lg font-medium transition-colors mt-6 uppercase text-sm tracking-wider"
            >
              {loading ? <Loader2 size={18} className="animate-spin" /> : <ShieldAlert size={18} />}
              <span>{loading ? 'Analyzing...' : 'Execute Analysis'}</span>
            </button>
          </form>
        </div>

        {/* Results Panel */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl shadow-sm overflow-hidden p-6 flex flex-col">
          <div className="flex items-center mb-6 border-b border-slate-800 pb-4 shrink-0">
            <AlertOctagon className="text-amber-500 mr-2" size={20} />
            <h2 className="text-lg font-semibold text-white">Analysis Results</h2>
          </div>

          <div className="flex-1 bg-slate-950 border border-slate-800 rounded-lg p-4 font-mono text-sm overflow-auto">
            {!analysis && !loading && (
              <div className="h-full flex flex-col items-center justify-center text-slate-600">
                <ShieldAlert size={48} className="mb-4 opacity-20" />
                <p>Run a simulation to view threat results.</p>
              </div>
            )}
            
            {loading && (
              <div className="h-full flex items-center justify-center text-emerald-500">
                <Loader2 size={32} className="animate-spin" />
              </div>
            )}

            {analysis && !loading && (
              <div className="space-y-4">
                <div className={clsx("p-3 rounded border flex items-center", analysis.isThreat ? "bg-rose-500/10 border-rose-500/30 text-rose-400" : "bg-emerald-500/10 border-emerald-500/30 text-emerald-400")}>
                  {analysis.isThreat ? <AlertOctagon size={24} className="mr-3 shrink-0" /> : <CheckCircle size={24} className="mr-3 shrink-0" />}
                  <div>
                    <h3 className="font-bold">{analysis.isThreat ? 'CRITICAL THREAT DETECTED' : 'TRAFFIC IS SAFE'}</h3>
                    <p className="text-xs opacity-90">{analysis.threats?.join(', ') || 'No malicious patterns found.'}</p>
                  </div>
                </div>

                <div>
                  <h4 className="text-slate-500 mb-2 uppercase text-xs font-bold tracking-wider">Raw JSON Dump</h4>
                  <pre className="text-emerald-300 text-xs overflow-x-auto whitespace-pre-wrap">
                    {JSON.stringify(analysis, null, 2)}
                  </pre>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
