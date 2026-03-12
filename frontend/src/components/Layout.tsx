
import { Outlet, Navigate, useNavigate, useLocation } from 'react-router-dom';
import { LayoutDashboard, Shield, Activity, LogOut, ShieldAlert } from 'lucide-react';
import clsx from 'clsx';

export default function Layout() {
  const token = localStorage.getItem('token');
  const userStr = localStorage.getItem('user');
  
  let user = null;
  try {
    if (userStr && userStr !== 'undefined') {
      user = JSON.parse(userStr);
    }
  } catch (e) {
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    window.location.href = '/login';
  }

  const navigate = useNavigate();
  const location = useLocation();

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  const navItems = [
    { name: 'Command Center', path: '/dashboard', icon: LayoutDashboard },
    { name: 'Firewalls', path: '/firewall', icon: Shield },
    { name: 'Intrusion Detection', path: '/ids', icon: Activity },
    { name: 'Identity & Access', path: '/users', icon: Shield },
  ];

  return (
    <div className="min-h-screen bg-slate-950 text-slate-300 flex">
      {/* Sidebar */}
      <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col hidden md:flex">
        <div className="h-16 flex items-center px-6 border-b border-slate-800 shrink-0">
          <ShieldAlert className="text-blue-500 mr-2" size={24} />
          <span className="text-white font-bold tracking-wider">IRIP SOC</span>
        </div>
        
        <nav className="flex-1 px-4 py-6 space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname.startsWith(item.path);
            return (
              <a
                key={item.name}
                href={item.path}
                onClick={(e) => { e.preventDefault(); navigate(item.path); }}
                className={clsx(
                  'flex items-center px-3 py-2.5 text-sm font-medium rounded-md transition-colors',
                  isActive 
                    ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20' 
                    : 'text-slate-400 hover:bg-slate-800/50 hover:text-white'
                )}
              >
                <Icon className={clsx('mr-3 shrink-0 h-5 w-5', isActive ? 'text-blue-400' : 'text-slate-500')} />
                {item.name}
              </a>
            );
          })}
        </nav>

        <div className="p-4 border-t border-slate-800 shrink-0">
          <div className="flex items-center px-3 py-2">
            <div className="bg-slate-800 rounded-full h-8 w-8 flex items-center justify-center border border-slate-700 text-white font-bold shrink-0">
              {user?.username?.[0]?.toUpperCase() || 'U'}
            </div>
            <div className="ml-3 truncate">
              <p className="text-sm font-medium text-white truncate">{user?.username || 'Analyst'}</p>
              <p className="text-xs text-slate-500 truncate">{user?.roles?.[0] || 'ROLE_USER'}</p>
            </div>
          </div>
          <button 
            onClick={handleLogout}
            className="mt-2 w-full flex items-center px-3 py-2 text-sm font-medium rounded-md text-slate-400 hover:bg-slate-800/50 hover:text-white transition-colors"
          >
            <LogOut className="mr-3 shrink-0 h-5 w-5 text-slate-500" />
            Sign Out
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col overflow-hidden">
        <header className="h-16 bg-slate-900 border-b border-slate-800 flex items-center justify-between px-6 shrink-0 md:hidden">
          <div className="flex items-center">
             <ShieldAlert className="text-blue-500 mr-2" size={24} />
             <span className="text-white font-bold">IRIP</span>
          </div>
          <button onClick={handleLogout} className="text-slate-400">
            <LogOut size={20} />
          </button>
        </header>
        <div className="flex-1 overflow-auto bg-slate-950 p-6">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
