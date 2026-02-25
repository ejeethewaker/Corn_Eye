import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Notifications.css';
import './Dashboard.css';

const sampleNotifications = [
  {
    id: 1,
    title: 'New user registered',
    description: 'Maria Santos joined the platform as a farmer',
    time: '2 minutes ago',
    read: false,
  },
  {
    id: 2,
    title: 'New user registered',
    description: 'Pedro Rodriguez joined the platform as a farmer',
    time: '1 hour ago',
    read: false,
  },
  {
    id: 3,
    title: 'New user registered',
    description: 'Ana Garcia joined the platform as a farmer',
    time: '3 hours ago',
    read: false,
  },
  {
    id: 4,
    title: 'New user registered',
    description: 'Carlos Mendoza joined the platform as a farmer',
    time: 'Yesterday',
    read: true,
  },
  {
    id: 5,
    title: 'New user registered',
    description: 'Luis Fernandez joined the platform as a farmer',
    time: '2 days ago',
    read: true,
  },
];

function Notifications() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('all');

  const allCount = sampleNotifications.length;
  const unreadCount = sampleNotifications.filter((n) => !n.read).length;

  const filteredNotifications =
    activeTab === 'unread'
      ? sampleNotifications.filter((n) => !n.read)
      : sampleNotifications;

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
            <button className="nav-item" onClick={() => navigate('/users')}>
              <span className="nav-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="7" r="4"/>
                  <path d="M5.5 21a6.5 6.5 0 0113 0"/>
                </svg>
              </span>
              Users
            </button>
            <button className="nav-item active">
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
        <h1 className="notif-title">Notifications</h1>

        <div className="notif-tabs">
          <button
            className={`notif-tab ${activeTab === 'all' ? 'notif-tab-active' : ''}`}
            onClick={() => setActiveTab('all')}
          >
            All ({allCount})
          </button>
          <button
            className={`notif-tab ${activeTab === 'unread' ? 'notif-tab-active' : ''}`}
            onClick={() => setActiveTab('unread')}
          >
            Unread ({unreadCount})
          </button>
        </div>

        <div className="notif-list">
          {filteredNotifications.map((notif) => (
            <div key={notif.id} className={`notif-card ${!notif.read ? 'notif-unread' : ''}`}>
              <div className="notif-avatar">
                <svg width="40" height="40" viewBox="0 0 24 24" fill="#9e9e9e" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="12" cy="8" r="4" />
                  <path d="M4 20c0-4 4-7 8-7s8 3 8 7" />
                </svg>
              </div>
              <div className="notif-content">
                <span className="notif-card-title">{notif.title}</span>
                <span className="notif-card-desc">{notif.description}</span>
                <span className="notif-card-time">{notif.time}</span>
              </div>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}

export default Notifications;
