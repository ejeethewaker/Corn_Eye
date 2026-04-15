// Farmers List
// Lists all registered farmers with search and account management options.
import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { database } from './firebase';
import { ref, get, onValue } from 'firebase/database';
import './Users.css';
import './Dashboard.css';

const avatarColors = ['#2196f3', '#4caf50', '#e91e63', '#9c27b0', '#ff9800', '#00bcd4', '#795548'];

function getInitials(name) {
  return name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2);
}

function Users() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [users, setUsers] = useState([]);
  const [adminName, setAdminName] = useState('');
  const [adminInitials, setAdminInitials] = useState('');

  useEffect(() => {
    // One-time admin profile load
    const storedEmail = localStorage.getItem('adminEmail') || sessionStorage.getItem('adminEmail') || '';
    get(ref(database, 'admins')).then((snap) => {
      if (snap.exists()) {
        const matched = Object.values(snap.val()).find((a) => a.email === storedEmail);
        if (matched) {
          const name = matched.fullName || 'Admin';
          setAdminName(name);
          setAdminInitials(name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2));
        }
      }
    }).catch((err) => console.error('Failed to load admin:', err));

    // Real-time farmer list listener
    const unsubFarmers = onValue(ref(database, 'farmers'), (snapshot) => {
      if (snapshot.exists()) {
        const farmersData = snapshot.val();
        const farmersList = Object.keys(farmersData).map((key, index) => {
          const farmer = farmersData[key];
          const name = farmer.fullname || 'Unknown';
          const initials = getInitials(name);
          const rawStatus = farmer.status || 'active';
          const status = rawStatus.charAt(0).toUpperCase() + rawStatus.slice(1).toLowerCase();
          const rawPhoto = farmer.profile_photo_url || null;
          return {
            id: key,
            name: name,
            email: farmer.email_address || '',
            status: status,
            avatar: initials,
            avatarBg: avatarColors[index % avatarColors.length],
            photoUrl: rawPhoto ? `data:image/jpeg;base64,${rawPhoto}` : null,
          };
        });
        setUsers(farmersList);
      } else {
        setUsers([]);
      }
    });

    return () => {
      unsubFarmers();
    };
  }, []);

  const filteredUsers = users.filter(
    (user) =>
      user.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      user.email.toLowerCase().includes(searchQuery.toLowerCase())
  );

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

          <Link to="/profile" className="sidebar-user-card sidebar-user-clickable">
            <div className="user-avatar">{adminInitials || 'A'}</div>
            <div className="user-info">
              <span className="user-name">{adminName || 'Admin'}</span><span className="user-role">Administrator</span>
            </div>
          </Link>

          <nav className="sidebar-nav">
            <button className="nav-item" onClick={() => navigate('/dashboard')}>
              <span className="nav-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M3 9.5L12 3l9 6.5V20a1 1 0 01-1 1H4a1 1 0 01-1-1V9.5z"/>
                  <path d="M9 21V12h6v9"/>
                </svg>
              </span>
              <span>Dashboard</span>
            </button>
            <button className="nav-item active">
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
        <h1 className="um-title">User Management</h1>

        <div className="um-search-bar">
          <span className="um-search-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#999" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="11" cy="11" r="8"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
          </span>
          <input
            type="text"
            className="um-search-input"
            placeholder="Search users..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {/* Table Header */}
        <div className="um-table-header">
          <span className="um-col-name">NAME</span>
          <span className="um-col-email">EMAIL</span>
          <span className="um-col-status">STATUS</span>
          <span className="um-col-action"></span>
        </div>

        {/* User Rows */}
        <div className="um-user-list">
          {filteredUsers.map((user) => (
            <Link key={user.id} to={`/user/${user.id}`} className="um-user-row" style={{cursor: 'pointer', textDecoration: 'none', color: 'inherit'}}>
              <div className="um-col-name">
                <div
                  className="um-user-avatar"
                  style={{ backgroundColor: user.photoUrl ? 'transparent' : user.avatarBg, overflow: 'hidden', padding: 0 }}
                >
                  {user.photoUrl
                    ? <img src={user.photoUrl} alt={user.name} style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%' }} />
                    : user.avatar}
                </div>
                <span className="um-user-name">{user.name}</span>
              </div>
              <span className="um-col-email">{user.email}</span>
              <span className="um-col-status">
                <span
                  className={`um-status-dot ${
                    user.status === 'Active' ? 'um-dot-active' : 'um-dot-inactive'
                  }`}
                ></span>
                <span
                  className={
                    user.status === 'Active'
                      ? 'um-status-active'
                      : 'um-status-inactive'
                  }
                >
                  {user.status}
                </span>
              </span>
              <span className="um-col-action">
                <button className="um-delete-btn" title="Delete user">
                  🗑️
                </button>
              </span>
            </Link>
          ))}
        </div>
      </main>
    </div>
  );
}

export default Users;
