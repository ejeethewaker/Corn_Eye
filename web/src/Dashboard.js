// Admin Dashboard
// Main admin screen showing scan statistics, disease analytics, and recent activity.
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { database } from './firebase';
import { ref, get } from 'firebase/database';
import './Dashboard.css';

function Dashboard() {
  const navigate = useNavigate();
  const [adminName, setAdminName] = useState('');
  const [adminInitials, setAdminInitials] = useState('');
  const [totalUsers, setTotalUsers] = useState(0);
  const [totalScans, setTotalScans] = useState(0);
  const [diseasesDetected, setDiseasesDetected] = useState(0);
  const [healthyScans, setHealthyScans] = useState(0);
  const [usersMonthPct, setUsersMonthPct] = useState(null);
  const [scansWeekPct, setScansWeekPct] = useState(null);
  const [diseasesToday, setDiseasesToday] = useState(0);

  useEffect(() => {
    const loadData = async () => {
      try {
        // Load admin profile
        const storedEmail = localStorage.getItem('adminEmail') || sessionStorage.getItem('adminEmail') || '';
        const adminsRef = ref(database, 'admins');
        const adminsSnap = await get(adminsRef);
        if (adminsSnap.exists()) {
          const admins = adminsSnap.val();
          const matched = Object.values(admins).find((a) => a.email === storedEmail);
          if (matched) {
            const name = matched.fullName || 'Admin';
            setAdminName(name);
            setAdminInitials(
              name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)
            );
          }
        }

        // Time boundaries
        const now = new Date();
        const startOfToday    = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
        const startOfThisMonth = new Date(now.getFullYear(), now.getMonth(), 1).getTime();
        const startOfLastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1).getTime();
        const startOfThisWeek  = startOfToday - 6 * 86400000;   // last 7 days
        const startOfPrevWeek  = startOfThisWeek - 7 * 86400000;

        // Count total farmers + this-month vs last-month
        const farmersRef = ref(database, 'farmers');
        const farmersSnap = await get(farmersRef);
        if (farmersSnap.exists()) {
          const farmers = Object.values(farmersSnap.val());
          setTotalUsers(farmers.length);
          const thisMonth = farmers.filter(f => (f.createdAt || 0) >= startOfThisMonth).length;
          const lastMonth = farmers.filter(f => (f.createdAt || 0) >= startOfLastMonth && (f.createdAt || 0) < startOfThisMonth).length;
          if (lastMonth > 0) {
            setUsersMonthPct(Math.round(((thisMonth - lastMonth) / lastMonth) * 100));
          } else if (thisMonth > 0) {
            setUsersMonthPct(100);
          } else {
            setUsersMonthPct(0);
          }
        }

        // Count scans + this-week vs prev-week + diseases today
        const scansRef = ref(database, 'analysis_results');
        const scansSnap = await get(scansRef);
        if (scansSnap.exists()) {
          const results = Object.values(scansSnap.val());
          setTotalScans(results.length);

          let diseased = 0;
          let diseasedToday = 0;
          let healthy = 0;
          let thisWeekScans = 0;
          let prevWeekScans = 0;

          results.forEach((r) => {
            const ts = r.time_scanned || r.timestamp || r.scanned_at || 0;
            const label = (r.analysis_label || '').toLowerCase();
            const isDiseased = label && label !== 'healthy';

            if (isDiseased) {
              diseased++;
              if (ts >= startOfToday) diseasedToday++;
            } else if (label === 'healthy') {
              healthy++;
            }
            if (ts >= startOfThisWeek) thisWeekScans++;
            else if (ts >= startOfPrevWeek) prevWeekScans++;
          });

          setDiseasesDetected(diseased);
          setHealthyScans(healthy);
          setDiseasesToday(diseasedToday);

          if (prevWeekScans > 0) {
            setScansWeekPct(Math.round(((thisWeekScans - prevWeekScans) / prevWeekScans) * 100));
          } else if (thisWeekScans > 0) {
            setScansWeekPct(100);
          } else {
            setScansWeekPct(0);
          }
        }
      } catch (err) {
        console.error('Failed to load dashboard data:', err);
      }
    };
    loadData();
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('adminLoggedIn');
    localStorage.removeItem('adminEmail');
    sessionStorage.removeItem('adminLoggedIn');
    sessionStorage.removeItem('adminEmail');
    navigate('/');
  };

  return (
    <div className="dashboard-container">
      {/* Sidebar */}
      <aside className="dashboard-sidebar">
        <div className="sidebar-top">
          <div className="sidebar-brand">
            <img
              src={process.env.PUBLIC_URL + '/dashboard-logo.png'}
              alt="CornEye Logo"
              className="sidebar-brand-logo"
            />
          </div>

          <div className="sidebar-user-card sidebar-user-clickable" onClick={() => navigate('/profile')}>
            <div className="user-avatar">{adminInitials || '?'}</div>
            <div className="user-info">
              <span className="user-name">{adminName || 'Admin'}</span>
              <span className="user-role">Administrator</span>
            </div>
          </div>

          <nav className="sidebar-nav">
            <button className="nav-item active">
              <span className="nav-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M3 9.5L12 3l9 6.5V20a1 1 0 01-1 1H4a1 1 0 01-1-1V9.5z"/>
                  <path d="M9 21V12h6v9"/>
                </svg>
              </span>
              Dashboard
            </button>
            <button className="nav-item" onClick={() => navigate('/users')}>
              <span className="nav-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="7" r="4"/>
                  <path d="M5.5 21a6.5 6.5 0 0113 0"/>
                </svg>
              </span>
              Users
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
              Notifications
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
            Logout
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="dashboard-main">
        <h1 className="dashboard-title">Dashboard Overview</h1>

        <div className="stats-grid">
          <div className="stat-card stat-blue">
            <h2 className="stat-number blue">{totalUsers}</h2>
            <p className="stat-label">Total Users</p>
            {usersMonthPct !== null && (
              <p className={`stat-change ${usersMonthPct >= 0 ? 'green' : 'red'}`}>
                {usersMonthPct >= 0 ? '↑' : '↓'} {Math.abs(usersMonthPct)}% this month
              </p>
            )}
          </div>

          <div className="stat-card stat-orange">
            <h2 className="stat-number orange">{totalScans}</h2>
            <p className="stat-label">Total Scans</p>
            {scansWeekPct !== null && (
              <p className={`stat-change ${scansWeekPct >= 0 ? 'green' : 'red'}`}>
                {scansWeekPct >= 0 ? '↑' : '↓'} {Math.abs(scansWeekPct)}% this week
              </p>
            )}
          </div>

          <div className="stat-card stat-pink">
            <h2 className="stat-number red">{diseasesDetected}</h2>
            <p className="stat-label">Diseases Detected</p>
            <p className={`stat-change ${diseasesToday > 0 ? 'orange' : 'green'}`}>
              {diseasesToday > 0
                ? `↑ ${diseasesToday} new today`
                : '✓ None today'}
            </p>
          </div>

          <div className="stat-card stat-teal">
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
