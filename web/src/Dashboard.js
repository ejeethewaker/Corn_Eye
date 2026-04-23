// Admin Dashboard
// Main admin screen showing scan statistics, disease analytics, and recent activity.
import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { database } from './firebase';
import { ref, get, onValue } from 'firebase/database';
import './Dashboard.css';

function Dashboard() {
  const navigate = useNavigate();
  const [adminName, setAdminName] = useState('');
  const [adminInitials, setAdminInitials] = useState('');
  const [allFarmers, setAllFarmers] = useState([]);
  const [allScans, setAllScans] = useState([]);
  const [dateFilter, setDateFilter] = useState('all');
  const [customFrom, setCustomFrom] = useState('');
  const [customTo, setCustomTo] = useState('');

  useEffect(() => {
    // One-time admin profile load — show cached value instantly, refresh in background
    const storedEmail = localStorage.getItem('adminEmail') || sessionStorage.getItem('adminEmail') || '';
    const cachedName = localStorage.getItem('adminCachedName');
    const cachedInitials = localStorage.getItem('adminCachedInitials');
    if (cachedName) {
      setAdminName(cachedName);
      setAdminInitials(cachedInitials || 'A');
    }
    get(ref(database, 'admins')).then((snap) => {
      if (snap.exists()) {
        const matched = Object.values(snap.val()).find((a) => a.email === storedEmail);
        if (matched) {
          const name = matched.fullName || 'Admin';
          const initials = name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2);
          setAdminName(name);
          setAdminInitials(initials);
          localStorage.setItem('adminCachedName', name);
          localStorage.setItem('adminCachedInitials', initials);
        }
      }
    }).catch((err) => console.error('Failed to load admin:', err));

    // Real-time listeners
    const unsubFarmers = onValue(ref(database, 'farmers'), (snap) => {
      if (snap.exists()) {
        setAllFarmers(Object.values(snap.val()));
      } else {
        setAllFarmers([]);
      }
    });

    const unsubScans = onValue(ref(database, 'analysis_results'), (snap) => {
      if (snap.exists()) {
        setAllScans(Object.values(snap.val()));
      } else {
        setAllScans([]);
      }
    });

    return () => {
      unsubFarmers();
      unsubScans();
    };
  }, []);

  // Compute date range from filter preset
  const getDateRange = () => {
    const now = new Date();
    const endOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59, 999).getTime();
    let from = 0;
    let to = endOfDay;

    switch (dateFilter) {
      case 'today': {
        from = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
        break;
      }
      case '7days': {
        from = endOfDay - 7 * 86400000;
        break;
      }
      case '30days': {
        from = endOfDay - 30 * 86400000;
        break;
      }
      case 'month': {
        from = new Date(now.getFullYear(), now.getMonth(), 1).getTime();
        break;
      }
      case 'year': {
        from = new Date(now.getFullYear(), 0, 1).getTime();
        break;
      }
      case 'custom': {
        if (customFrom) from = new Date(customFrom).getTime();
        if (customTo) to = new Date(customTo + 'T23:59:59').getTime();
        break;
      }
      default:
        break;
    }
    return { from, to };
  };

  const { from: rangeFrom, to: rangeTo } = getDateRange();

  // Filter data by date range
  const filteredFarmers = dateFilter === 'all'
    ? allFarmers
    : allFarmers.filter(f => {
        const ts = f.createdAt || 0;
        return ts >= rangeFrom && ts <= rangeTo;
      });

  const filteredScans = dateFilter === 'all'
    ? allScans
    : allScans.filter(r => {
        const ts = r.time_scanned || r.timestamp || r.scanned_at || 0;
        return ts >= rangeFrom && ts <= rangeTo;
      });

  // Compute stats from filtered data
  const totalUsers = filteredFarmers.length;
  const totalScans = filteredScans.length;

  let diseasesDetected = 0;
  let healthyScans = 0;
  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  let diseasesToday = 0;

  filteredScans.forEach((r) => {
    const ts = r.time_scanned || r.timestamp || r.scanned_at || 0;
    const label = (r.analysis_label || '').toLowerCase();
    const isDiseased = label && label !== 'healthy';
    if (isDiseased) {
      diseasesDetected++;
      if (ts >= startOfToday) diseasesToday++;
    } else if (label === 'healthy') {
      healthyScans++;
    }
  });

  // Comparison stats (always based on all data)
  const startOfThisMonth = new Date(now.getFullYear(), now.getMonth(), 1).getTime();
  const startOfLastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1).getTime();
  const startOfThisWeek = startOfToday - 6 * 86400000;
  const startOfPrevWeek = startOfThisWeek - 7 * 86400000;

  const thisMonthUsers = allFarmers.filter(f => (f.createdAt || 0) >= startOfThisMonth).length;
  const lastMonthUsers = allFarmers.filter(f => (f.createdAt || 0) >= startOfLastMonth && (f.createdAt || 0) < startOfThisMonth).length;
  let usersMonthPct = 0;
  if (lastMonthUsers > 0) usersMonthPct = Math.round(((thisMonthUsers - lastMonthUsers) / lastMonthUsers) * 100);
  else if (thisMonthUsers > 0) usersMonthPct = 100;

  let thisWeekScans = 0;
  let prevWeekScans = 0;
  allScans.forEach((r) => {
    const ts = r.time_scanned || r.timestamp || r.scanned_at || 0;
    if (ts >= startOfThisWeek) thisWeekScans++;
    else if (ts >= startOfPrevWeek) prevWeekScans++;
  });
  let scansWeekPct = 0;
  if (prevWeekScans > 0) scansWeekPct = Math.round(((thisWeekScans - prevWeekScans) / prevWeekScans) * 100);
  else if (thisWeekScans > 0) scansWeekPct = 100;

  const handleLogout = () => {
    localStorage.removeItem('adminLoggedIn');
    localStorage.removeItem('adminEmail');
    localStorage.removeItem('adminCachedName');
    localStorage.removeItem('adminCachedInitials');
    sessionStorage.removeItem('adminLoggedIn');
    sessionStorage.removeItem('adminEmail');
    navigate('/');
  };

  return (
    <div className="dashboard-container">
      {/* Sidebar */}}
      <aside className="dashboard-sidebar">
        <div className="sidebar-top">
          <div className="sidebar-brand">
            <img
              src={process.env.PUBLIC_URL + '/dashboard-logo.png'}
              alt="CornEye Logo"
              className="sidebar-brand-logo"
            />
          </div>

          <Link to="/profile" className="sidebar-user-card sidebar-user-clickable">
            <div className="user-avatar">{adminInitials || 'A'}</div>
            <div className="user-info">
              <span className="user-name">{adminName || 'Admin'}</span><span className="user-role">Administrator</span>
            </div>
          </Link>

          <nav className="sidebar-nav">
            <button className="nav-item active">
              <span className="nav-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M3 9.5L12 3l9 6.5V20a1 1 0 01-1 1H4a1 1 0 01-1-1V9.5z"/>
                  <path d="M9 21V12h6v9"/>
                </svg>
              </span>
              <span>Dashboard</span>
            </button>
            <button className="nav-item" onClick={() => navigate('/users')}>
              <span className="nav-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="7" r="4"/>
                  <path d="M5.5 21a6.5 6.5 0 0113 0"/>
                </svg>
              </span>
              <span>Users</span>
            </button>
            <button className="nav-item" onClick={() => navigate('/notifications')}>
              <span className="nav-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M18 8a6 6 0 10-12 0c0 7-3 9-3 9h18s-3-2-3-9"/>
                  <path d="M13.73 21a2 2 0 01-3.46 0"/>
                  <path d="M6 4a1 1 0 011-1"/>
                  <path d="M18 4a1 1 0 00-1-1"/>
                </svg>
              </span>
              <span>Notifications</span>
            </button>
          </nav>
        </div>

        <div className="sidebar-bottom">
          <button className="nav-item logout-btn" onClick={handleLogout}>
            <span className="nav-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
            </span>
            <span>Logout</span>
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="dashboard-main">
        <div className="dashboard-header">
          <h1 className="dashboard-title">Dashboard Overview</h1>
          <div className="date-filter">
            <div className="date-filter-presets">
              {[
                { value: 'all', label: 'All Time' },
                { value: 'today', label: 'Today' },
                { value: '7days', label: '7 Days' },
                { value: '30days', label: '30 Days' },
                { value: 'month', label: 'This Month' },
                { value: 'year', label: 'This Year' },
                { value: 'custom', label: 'Custom' },
              ].map((opt) => (
                <button
                  key={opt.value}
                  className={`date-filter-btn ${dateFilter === opt.value ? 'date-filter-btn-active' : ''}`}
                  onClick={() => setDateFilter(opt.value)}
                >
                  {opt.label}
                </button>
              ))}
            </div>
            {dateFilter === 'custom' && (
              <div className="date-filter-custom">
                <label>
                  From{' '}
                  <input type="date" value={customFrom} onChange={(e) => setCustomFrom(e.target.value)} />
                </label>
                <label>
                  To{' '}
                  <input type="date" value={customTo} onChange={(e) => setCustomTo(e.target.value)} />
                </label>
              </div>
            )}
          </div>
        </div>

        <div className="stats-grid">
          <div className="stat-card stat-blue" style={{ cursor: 'pointer' }} onClick={() => navigate('/users')}>
            <h2 className="stat-number blue">{totalUsers}</h2>
            <p className="stat-label">Total Users</p>
            {usersMonthPct !== null && (
              <p className={`stat-change ${usersMonthPct >= 0 ? 'green' : 'red'}`}>
                {usersMonthPct >= 0 ? '↑' : '↓'} {Math.abs(usersMonthPct)}% this month
              </p>
            )}
          </div>

          <div className="stat-card stat-orange" style={{ cursor: 'pointer' }} onClick={() => navigate('/scans')}>
            <h2 className="stat-number orange">{totalScans}</h2>
            <p className="stat-label">Total Scans</p>
            {scansWeekPct !== null && (
              <p className={`stat-change ${scansWeekPct >= 0 ? 'green' : 'red'}`}>
                {scansWeekPct >= 0 ? '↑' : '↓'} {Math.abs(scansWeekPct)}% this week
              </p>
            )}
          </div>

          <div className="stat-card stat-pink" style={{ cursor: 'pointer' }} onClick={() => navigate('/scans?filter=disease')}>
            <h2 className="stat-number red">{diseasesDetected}</h2>
            <p className="stat-label">Diseases Detected</p>
            <p className={`stat-change ${diseasesToday > 0 ? 'orange' : 'green'}`}>
              {diseasesToday > 0
                ? `↑ ${diseasesToday} new today`
                : '✓ None today'}
            </p>
          </div>

          <div className="stat-card stat-teal" style={{ cursor: 'pointer' }} onClick={() => navigate('/scans?filter=healthy')}>
            <h2 className="stat-number teal">{healthyScans}</h2>
            <p className="stat-label">Healthy Scans</p>
            <p className="stat-change green">
              {healthyScans > 0
                ? `✓ ${Math.round((healthyScans / (totalScans || 1)) * 100)}% of total scans`
                : '✓ No scans yet'}
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}

export default Dashboard;
