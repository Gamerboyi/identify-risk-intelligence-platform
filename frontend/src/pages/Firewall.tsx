import { useEffect, useState } from 'react';
import { Shield, Plus, Trash2, Power, PowerOff, ShieldAlert, Loader2 } from 'lucide-react';
import apiClient from '../api/client';

interface FirewallRule {
  id: number;
  ruleName: string;
  ruleType: string;
  sourceIp: string;
  destinationPort: number;
  protocol: string;
  priority: number;
  active: boolean;
}

export default function Firewall() {
  const [rules, setRules] = useState<FirewallRule[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchRules = async () => {
    try {
      const response = await apiClient.get('/firewall/rules');
      setRules(response.data);
    } catch (err) {
      console.error('Failed to fetch rules', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRules();
  }, []);

  const handleToggle = async (id: number) => {
    try {
      await apiClient.put(`/firewall/rules/${id}/toggle`);
      setRules(prev => prev.map(r => r.id === id ? { ...r, active: !r.active } : r));
    } catch (err) {
      console.error('Failed to toggle rule', err);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await apiClient.delete(`/firewall/rules/${id}`);
      setRules(prev => prev.filter(r => r.id !== id));
    } catch (err) {
      console.error('Failed to delete rule', err);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight flex items-center">
            <Shield className="text-blue-500 mr-3" /> Firewall Rules
          </h1>
          <p className="text-slate-400 text-sm mt-1">Manage network access blocks and threat rules</p>
        </div>
        <button className="flex items-center px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium transition-colors shadow-lg shadow-blue-500/20 text-opacity-90">
          <Plus size={18} className="mr-2" /> New Rule
        </button>
      </div>

      <div className="bg-slate-900 border border-slate-800 rounded-xl shadow-sm overflow-hidden">
        {loading ? (
          <div className="flex justify-center items-center p-12">
            <Loader2 className="animate-spin text-blue-500" size={32} />
          </div>
        ) : rules.length === 0 ? (
          <div className="flex flex-col items-center justify-center p-16 text-slate-500">
            <ShieldAlert size={48} className="mb-4 opacity-20" />
            <p>No firewall rules configured.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="bg-slate-950/50 text-xs uppercase font-semibold text-slate-500 border-b border-slate-800">
                <tr>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4">Rule Name</th>
                  <th className="px-6 py-4">Source IP</th>
                  <th className="px-6 py-4">Protocol : Port</th>
                  <th className="px-6 py-4">Priority</th>
                  <th className="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/50">
                {rules.map((rule) => (
                  <tr key={rule.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="px-6 py-4">
                      {rule.active ? (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-emerald-500/10 text-emerald-500 border border-emerald-500/20">
                          Active
                        </span>
                      ) : (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-slate-500/10 text-slate-400 border border-slate-500/20">
                          Disabled
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 font-medium text-white">{rule.ruleName}</td>
                    <td className="px-6 py-4 font-mono text-xs">{rule.sourceIp || 'ANY'}</td>
                    <td className="px-6 py-4">
                      <span className="font-mono text-xs text-blue-400 bg-blue-500/10 px-2 py-0.5 rounded border border-blue-500/20 mr-2">
                        {rule.protocol}
                      </span>
                      {rule.destinationPort || 'ANY'}
                    </td>
                    <td className="px-6 py-4">{rule.priority}</td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex items-center justify-end space-x-3">
                        <button 
                          onClick={() => handleToggle(rule.id)}
                          className={`p-1.5 rounded-md transition-colors ${rule.active ? 'text-rose-400 hover:bg-rose-500/10' : 'text-emerald-400 hover:bg-emerald-500/10'}`}
                        >
                          {rule.active ? <PowerOff size={18} /> : <Power size={18} />}
                        </button>
                        <button 
                          onClick={() => handleDelete(rule.id)}
                          className="p-1.5 text-slate-500 hover:text-rose-500 hover:bg-rose-500/10 rounded-md transition-colors"
                        >
                          <Trash2 size={18} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
