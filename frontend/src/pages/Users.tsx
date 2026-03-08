import { useState, useEffect } from 'react';
import { Users, ShieldCheck, ShieldOff, LockOpen, Loader2, Search } from 'lucide-react';
import clsx from 'clsx';
import apiClient from '../api/client';

interface UserRecord {
  id: string;
  username: string;
  email: string;
  accountLocked: boolean;
  failedAttemptCount: number;
  roles: string[];
  createdAt: string;
}

export default function UsersPage() {
  const [users, setUsers] = useState<UserRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [unlocking, setUnlocking] = useState<string | null>(null);

  const fetchUsers = async () => {
    try {
      const response = await apiClient.get('/admin/users');
      setUsers(response.data);
    } catch (err) {
      console.error('Failed to fetch users', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleUnlock = async (id: string) => {
    setUnlocking(id);
    try {
      await apiClient.put(`/admin/users/${id}/unlock`);
      setUsers(prev => prev.map(u => u.id === id ? { ...u, accountLocked: false, failedAttemptCount: 0 } : u));
    } catch (err) {
      console.error('Failed to unlock user', err);
    } finally {
      setUnlocking(null);
    }
  };

  const filtered = users.filter(u =>
    u.username.toLowerCase().includes(search.toLowerCase()) ||
    u.email.toLowerCase().includes(search.toLowerCase())
  );

  const lockedCount = users.filter(u => u.accountLocked).length;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight flex items-center">
            <Users className="text-violet-500 mr-3" /> Identity & Access
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            {users.length} total users · <span className={clsx(lockedCount > 0 ? 'text-rose-400' : 'text-emerald-400')}>{lockedCount} locked</span>
          </p>
        </div>
      </div>

      {/* Search */}
      <div className="relative max-w-md">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          <Search size={18} className="text-slate-500" />
        </div>
        <input
          type="text"
          placeholder="Search by username or email..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="block w-full pl-10 pr-3 py-2 border border-slate-700 rounded-lg bg-slate-900 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent text-sm"
        />
      </div>

      {/* Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl shadow-sm overflow-hidden">
        {loading ? (
          <div className="flex justify-center items-center p-12">
            <Loader2 className="animate-spin text-violet-500" size={32} />
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center p-16 text-slate-500">
            <Users size={48} className="mb-4 opacity-20" />
            <p>No users found.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="bg-slate-950/50 text-xs uppercase font-semibold text-slate-500 border-b border-slate-800">
                <tr>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4">Username</th>
                  <th className="px-6 py-4">Email</th>
                  <th className="px-6 py-4">Roles</th>
                  <th className="px-6 py-4">Failed Attempts</th>
                  <th className="px-6 py-4">Created</th>
                  <th className="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/50">
                {filtered.map((user) => (
                  <tr key={user.id} className={clsx("hover:bg-slate-800/30 transition-colors", user.accountLocked && "bg-rose-500/5")}>
                    <td className="px-6 py-4">
                      {user.accountLocked ? (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-rose-500/10 text-rose-500 border border-rose-500/20">
                          <ShieldOff size={12} className="mr-1" /> Locked
                        </span>
                      ) : (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-emerald-500/10 text-emerald-500 border border-emerald-500/20">
                          <ShieldCheck size={12} className="mr-1" /> Active
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 font-medium text-white">{user.username}</td>
                    <td className="px-6 py-4 text-slate-400">{user.email}</td>
                    <td className="px-6 py-4">
                      <div className="flex flex-wrap gap-1">
                        {user.roles.map(role => (
                          <span key={role} className="text-xs px-2 py-0.5 rounded bg-violet-500/10 text-violet-400 border border-violet-500/20">
                            {role.replace('ROLE_', '')}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className={clsx("font-mono", user.failedAttemptCount > 3 ? "text-rose-400" : "text-slate-400")}>
                        {user.failedAttemptCount}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-slate-500 text-xs">
                      {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—'}
                    </td>
                    <td className="px-6 py-4 text-right">
                      {user.accountLocked && (
                        <button
                          onClick={() => handleUnlock(user.id)}
                          disabled={unlocking === user.id}
                          className="inline-flex items-center px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-medium rounded-md transition-colors disabled:opacity-50"
                        >
                          {unlocking === user.id ? <Loader2 size={14} className="animate-spin mr-1" /> : <LockOpen size={14} className="mr-1" />}
                          Unlock
                        </button>
                      )}
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
