import { useEffect, useState } from 'react';
import { Shield, Plus, Trash2, Power, PowerOff, ShieldAlert, Loader2, X } from 'lucide-react';
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
  
  // Modal State
  const [showModal, setShowModal] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  
  // Form State
  const [ruleName, setRuleName] = useState('');
  const [ruleType, setRuleType] = useState('BLOCK_IP');
  const [sourceIp, setSourceIp] = useState('');
  const [protocol, setProtocol] = useState('TCP');
  const [priority, setPriority] = useState('100');

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

  const handleCreateRule = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsSubmitting(true);

    try {
      const payload = {
        ruleName,
        ruleType,
        sourceIp,
        protocol,
        priority: parseInt(priority, 10),
        active: true,
        destinationPort: 0
      };

      const response = await apiClient.post('/firewall/rules', payload);
      setRules(prev => [...prev, response.data]);
      
      // Reset & Close
      setShowModal(false);
      setRuleName('');
      setSourceIp('');
    } catch (err: any) {
      setError(err.response?.data?.message || err.response?.data?.error || 'Failed to create rule. Make sure you are an Admin.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 relative">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight flex items-center">
            <Shield className="text-blue-500 mr-3" /> Firewall Rules
          </h1>
          <p className="text-slate-400 text-sm mt-1">Manage global network access blocks and threat rules</p>
        </div>
        <button 
          onClick={() => setShowModal(true)}
          className="flex items-center px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium transition-colors shadow-lg shadow-blue-500/20 text-opacity-90"
        >
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
            <p>No external firewall rules currently configured.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="bg-slate-950/50 text-xs uppercase font-semibold text-slate-500 border-b border-slate-800">
                <tr>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4">Rule Name</th>
                  <th className="px-6 py-4">Source IP</th>
                  <th className="px-6 py-4">Protocol</th>
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

      {/* CREATE RULE MODAL */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="bg-slate-900 border border-slate-700 rounded-xl shadow-2xl w-full max-w-md overflow-hidden relative">
            <div className="flex justify-between items-center p-4 border-b border-slate-800 bg-slate-800/50">
              <h2 className="text-lg font-bold text-white flex items-center">
                <ShieldAlert size={18} className="mr-2 text-blue-500" /> Construct Firewall Rule
              </h2>
              <button onClick={() => setShowModal(false)} className="text-slate-400 hover:text-white transition-colors">
                <X size={20} />
              </button>
            </div>
            
            <form onSubmit={handleCreateRule} className="p-6 space-y-4">
              {error && (
                <div className="bg-red-500/10 border border-red-500/50 text-red-500 text-sm p-3 rounded-lg flex items-start">
                  <ShieldAlert size={16} className="mt-0.5 mr-2 shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1">Rule Name</label>
                <input 
                  type="text" required value={ruleName} onChange={e => setRuleName(e.target.value)} placeholder="e.g. Block Russian Botnet"
                  className="w-full bg-slate-950 border border-slate-700 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-1">Rule Type</label>
                  <select value={ruleType} onChange={e => setRuleType(e.target.value)} className="w-full bg-slate-950 border border-slate-700 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500">
                    <option value="BLOCK_IP">BLOCK IP</option>
                    <option value="ALLOW_IP">ALLOW IP</option>
                    <option value="RATE_LIMIT">RATE LIMIT</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-1">Protocol</label>
                  <select value={protocol} onChange={e => setProtocol(e.target.value)} className="w-full bg-slate-950 border border-slate-700 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500">
                    <option value="TCP">TCP</option>
                    <option value="UDP">UDP</option>
                    <option value="ICMP">ICMP</option>
                    <option value="ANY">ANY</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1">Target IPv4 Address</label>
                <input 
                  type="text" required={ruleType.includes('IP')} value={sourceIp} onChange={e => setSourceIp(e.target.value)} placeholder="192.168.1.50"
                  className="w-full bg-slate-950 border border-slate-700 text-white font-mono rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1">Execution Priority (1-1000)</label>
                <input 
                  type="number" required min="1" max="1000" value={priority} onChange={e => setPriority(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="pt-4 flex justify-end space-x-3 border-t border-slate-800 mt-6">
                <button type="button" onClick={() => setShowModal(false)} className="px-4 py-2 text-sm font-medium text-slate-300 hover:text-white transition-colors">
                  Cancel
                </button>
                <button type="submit" disabled={isSubmitting} className="flex items-center px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium transition-colors disabled:opacity-50">
                  {isSubmitting ? <Loader2 className="animate-spin mr-2" size={16} /> : <ShieldAlert size={16} className="mr-2" />}
                  Deploy Rule
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
