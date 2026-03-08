import { useEffect, useState } from 'react';
import { Activity, ShieldBan, ShieldAlert, Cpu, AlertTriangle, Users, Clock } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, AreaChart, Area } from 'recharts';
import apiClient from '../api/client';

interface AuditEvent {
  id: number;
  eventType: string;
  actorId: string;
  eventData: Record<string, any>;
  createdAt: string;
}

interface LoginLog {
  id: number;
  userId: string;
  riskScore: number | null;
  successFlag: boolean;
  ipAddress: string;
  loginTimestamp: string;
}

export default function Dashboard() {
  const [stats, setStats] = useState({
    activeRules: 0,
    totalUsers: 0,
    lockedUsers: 0,
    totalLogins: 0,
    totalAuditEvents: 0,
    avgRiskScore: 0,
  });
  const [auditEvents, setAuditEvents] = useState<AuditEvent[]>([]);
  const [loginLogs, setLoginLogs] = useState<LoginLog[]>([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        // Fetch admin stats
        const statsRes = await apiClient.get('/admin/stats');
        setStats(prev => ({ ...prev, ...statsRes.data }));
      } catch (err) {
        console.error('Failed to load admin stats (may require ADMIN role)', err);
      }

      try {
        // Fetch active firewall rules count
        const rules = await apiClient.get('/firewall/rules/active');
        setStats(prev => ({ ...prev, activeRules: rules.data.length }));
      } catch (err) {
        console.error('Failed to load firewall rules', err);
      }

      try {
        // Fetch recent audit events for the live feed
        const auditRes = await apiClient.get('/admin/audit-events');
        setAuditEvents(auditRes.data.slice(0, 15));
      } catch (err) {
        console.error('Failed to load audit events', err);
      }

      try {
        // Fetch recent login logs for the chart
        const logsRes = await apiClient.get('/admin/login-logs');
        setLoginLogs(logsRes.data);
      } catch (err) {
        console.error('Failed to load login logs', err);
      }
    };

    fetchData();
  }, []);

  // Build chart data from real login logs
  const riskChartData = loginLogs
    .filter(l => l.riskScore !== null)
    .reverse()
    .map((l, i) => ({
      time: l.loginTimestamp ? new Date(l.loginTimestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : `#${i}`,
      score: l.riskScore || 0,
    }));

  const trafficChartData = loginLogs
    .reverse()
    .reduce((acc: Record<string, number>, l) => {
      const hour = l.loginTimestamp ? new Date(l.loginTimestamp).toLocaleTimeString([], { hour: '2-digit' }) : 'N/A';
      acc[hour] = (acc[hour] || 0) + 1;
      return acc;
    }, {});

  const trafficData = Object.entries(trafficChartData).map(([time, count]) => ({ time, count }));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white tracking-tight flex items-center">
          <Activity className="text-blue-500 mr-3" /> Command Center
        </h1>
        <div className="flex items-center space-x-2 text-sm text-slate-400">
          <span className="relative flex h-3 w-3">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-3 w-3 bg-emerald-500"></span>
          </span>
          <span>System Status: Optimal</span>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm hover:border-slate-700 transition-colors">
          <div className="flex items-center space-x-4">
            <div className="p-3 bg-blue-500/10 text-blue-500 rounded-lg">
              <ShieldBan size={24} />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-400">Active Firewalls</p>
              <h3 className="text-2xl font-bold text-white">{stats.activeRules}</h3>
            </div>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm hover:border-slate-700 transition-colors">
          <div className="flex items-center space-x-4">
            <div className="p-3 bg-rose-500/10 text-rose-500 rounded-lg">
              <ShieldAlert size={24} />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-400">Locked Accounts</p>
              <h3 className="text-2xl font-bold text-white">{stats.lockedUsers}</h3>
            </div>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm hover:border-slate-700 transition-colors">
          <div className="flex items-center space-x-4">
            <div className="p-3 bg-amber-500/10 text-amber-500 rounded-lg">
              <AlertTriangle size={24} />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-400">Avg Risk Score</p>
              <h3 className="text-2xl font-bold text-white">{stats.avgRiskScore}</h3>
            </div>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm hover:border-slate-700 transition-colors">
          <div className="flex items-center space-x-4">
            <div className="p-3 bg-emerald-500/10 text-emerald-500 rounded-lg">
              <Cpu size={24} />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-400">Total Login Events</p>
              <h3 className="text-2xl font-bold text-white">{stats.totalLogins.toLocaleString()}</h3>
            </div>
          </div>
        </div>
      </div>

      {/* Charts + Audit Feed Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="col-span-2 bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-sm">
          <h3 className="text-lg font-semibold text-white mb-6 flex items-center">
            <Activity size={18} className="mr-2 text-rose-500" />
            ML Risk Score Timeline
          </h3>
          <div className="h-[300px] w-full">
            {riskChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={riskChartData} margin={{ top: 5, right: 0, left: -20, bottom: 0 }}>
                  <defs>
                    <linearGradient id="colorScore" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#f43f5e" stopOpacity={0.3}/>
                      <stop offset="95%" stopColor="#f43f5e" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
                  <XAxis dataKey="time" stroke="#64748b" tick={{fill: '#64748b'}} axisLine={false} tickLine={false} />
                  <YAxis stroke="#64748b" tick={{fill: '#64748b'}} axisLine={false} tickLine={false} />
                  <Tooltip 
                    contentStyle={{ backgroundColor: '#0f172a', border: '1px solid #1e293b', borderRadius: '8px' }}
                    itemStyle={{ color: '#f8fafc' }}
                  />
                  <Area type="monotone" dataKey="score" stroke="#f43f5e" strokeWidth={2} fillOpacity={1} fill="url(#colorScore)" />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex items-center justify-center text-slate-600">
                <p>Login with ML service active to populate risk data</p>
              </div>
            )}
          </div>
        </div>

        {/* Live Audit Event Feed */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-sm flex flex-col">
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center shrink-0">
            <Clock size={18} className="mr-2 text-amber-500" />
            Live Audit Feed
          </h3>
          <div className="flex-1 overflow-auto space-y-2 pr-1">
            {auditEvents.length === 0 ? (
              <p className="text-slate-600 text-center mt-8">No audit events yet.</p>
            ) : (
              auditEvents.map((event) => (
                <div key={event.id} className="bg-slate-950 border border-slate-800 rounded-lg p-3 text-xs">
                  <div className="flex items-center justify-between mb-1">
                    <span className={`font-bold ${
                      event.eventType?.includes('HIGH_RISK') ? 'text-rose-400' :
                      event.eventType?.includes('LOCKED') ? 'text-amber-400' :
                      event.eventType?.includes('LOGIN_SUCCESS') ? 'text-emerald-400' :
                      'text-blue-400'
                    }`}>
                      {event.eventType}
                    </span>
                    <span className="text-slate-600">
                      {event.createdAt ? new Date(event.createdAt).toLocaleTimeString() : ''}
                    </span>
                  </div>
                  {event.eventData && (
                    <p className="text-slate-500 truncate">
                      {Object.entries(event.eventData).map(([k, v]) => `${k}: ${v}`).join(', ')}
                    </p>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
