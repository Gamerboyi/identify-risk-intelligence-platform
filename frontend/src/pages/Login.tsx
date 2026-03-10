import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../api/client';
import { ShieldAlert, Lock, User, Loader2, Mail, UserPlus, LogIn } from 'lucide-react';

export default function Login() {
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const response = await apiClient.post('/auth/login', { username, password });
      const { token, username: returnedUsername } = response.data;

      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify({ username: returnedUsername, roles: ['ROLE_USER'] }));

      navigate('/dashboard');
    } catch (err: any) {
      setError(err.response?.data?.error || err.response?.data?.message || 'Invalid username or password. Account may be locked.');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      await apiClient.post('/auth/register', { username, email, password });
      setSuccess('Account created successfully! You can now sign in.');
      setIsRegister(false);
      setEmail('');
      setPassword('');
    } catch (err: any) {
      setError(err.response?.data?.error || err.response?.data?.message || 'Registration failed. Username or email may already exist.');
    } finally {
      setLoading(false);
    }
  };

  const toggleMode = () => {
    setIsRegister(!isRegister);
    setError('');
    setSuccess('');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950 px-4 relative overflow-hidden">
      {/* Background Cyber Effect */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-blue-900/20 via-slate-950 to-slate-950"></div>

      <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-xl shadow-2xl z-10 p-8">
        <div className="flex flex-col items-center mb-8">
          <div className="w-16 h-16 bg-blue-500/10 text-blue-500 rounded-full flex items-center justify-center mb-4 border border-blue-500/20">
            <ShieldAlert size={32} />
          </div>
          <h2 className="text-2xl font-bold text-white tracking-tight">
            {isRegister ? 'Create Account' : 'Security SOC Login'}
          </h2>
          <p className="text-slate-400 text-sm mt-1">IRIP — Intelligent Risk Intelligence Platform</p>
        </div>

        {error && (
          <div className="bg-red-500/10 border border-red-500/50 text-red-500 text-sm p-3 rounded-lg mb-6 flex items-start">
            <ShieldAlert size={16} className="mt-0.5 mr-2 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {success && (
          <div className="bg-emerald-500/10 border border-emerald-500/50 text-emerald-400 text-sm p-3 rounded-lg mb-6 flex items-start">
            <UserPlus size={16} className="mt-0.5 mr-2 shrink-0" />
            <span>{success}</span>
          </div>
        )}

        <form onSubmit={isRegister ? handleRegister : handleLogin} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-slate-300">Username</label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <User size={18} className="text-slate-500" />
              </div>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                className="block w-full pl-10 pr-3 py-2 border border-slate-700 rounded-lg bg-slate-800/50 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
                placeholder="admin"
              />
            </div>
          </div>

          {isRegister && (
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-slate-300">Email</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Mail size={18} className="text-slate-500" />
                </div>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="block w-full pl-10 pr-3 py-2 border border-slate-700 rounded-lg bg-slate-800/50 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
                  placeholder="user@example.com"
                />
              </div>
            </div>
          )}

          <div className="space-y-1.5">
            <label className="text-sm font-medium text-slate-300">Password</label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Lock size={18} className="text-slate-500" />
              </div>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="block w-full pl-10 pr-3 py-2 border border-slate-700 rounded-lg bg-slate-800/50 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
                placeholder="••••••••"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full flex justify-center items-center py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 focus:ring-offset-slate-900 disabled:opacity-50 disabled:cursor-not-allowed transition-all mt-6"
          >
            {loading ? (
              <Loader2 className="animate-spin" size={20} />
            ) : isRegister ? (
              <><UserPlus size={18} className="mr-2" /> Create Account</>
            ) : (
              <><LogIn size={18} className="mr-2" /> Authenticate</>
            )}
          </button>
        </form>

        {/* Toggle Login / Register */}
        <div className="mt-6 text-center">
          <button
            onClick={toggleMode}
            className="text-sm text-slate-400 hover:text-blue-400 transition-colors"
          >
            {isRegister ? (
              <>Already have an account? <span className="text-blue-500 font-medium">Sign In</span></>
            ) : (
              <>Don't have an account? <span className="text-blue-500 font-medium">Register</span></>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
