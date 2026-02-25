import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Users.css';
import './Dashboard.css';

const sampleUsers = [
  {
    id: 1,
    name: 'Juan Dela Cruz',
    email: 'juan@email.com',
    status: 'Active',
    avatar: '👨‍🌾',
    avatarBg: '#2196f3',
  },
  {
    id: 2,
    name: 'Maria Santos',
    email: 'maria@email.com',
    status: 'Active',
    avatar: '👩‍🌾',
    avatarBg: '#4caf50',
  },
  {
    id: 3,
    name: 'Pedro Reyes',
    email: 'pedro@email.com',
    status: 'Inactive',
    avatar: '👨‍🌾',
    avatarBg: '#e91e63',
  },
  {
    id: 4,
    name: 'Ana Garcia',
    email: 'ana@email.com',
    status: 'Active',
    avatar: '👩‍🌾',
    avatarBg: '#9c27b0',
  },
];

function Users() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');

  const filteredUsers = sampleUsers.filter(
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

          <div className="sidebar-user-card sidebar-user-clickable" onClick={() => navigate('/profile')}>
            <div className="user-avatar">AD</div>
            <div className="user-info">
              <span className="user-name">Admin User</span>
              <span className="user-role">Administrator</span>
            </div>
          </div>

          <nav className="sidebar-nav">
            <button className="nav-item" onClick={() => navigate('/dashboard')}>
              <span className="nav-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M3 9.5L12 3l9 6.5V20a1 1 0 01-1 1H4a1 1 0 01-1-1V9.5z"/>
                  <path d="M9 21V12h6v9"/>
                </svg>
              </span>
              Dashboard
            </button>
            <button className="nav-item active">
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
            <div key={user.id} className="um-user-row" onClick={() => navigate(`/user/${user.id}`)} style={{cursor: 'pointer'}}>
              <div className="um-col-name">
                <div
                  className="um-user-avatar"
                  style={{ backgroundColor: user.avatarBg }}
                >
                  {user.avatar}
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
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}

export default Users;
