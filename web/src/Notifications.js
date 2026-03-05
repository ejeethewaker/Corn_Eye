// Notifications
// Displays and manages broadcast notifications sent to farmers.
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { database } from './firebase';
import { ref, get, update } from 'firebase/database';
import './Notifications.css';
import './Dashboard.css';

function getTimeAgo(timestamp) {
  if (!timestamp) return '';
  const diff = Date.now() - timestamp;
  const mins  = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days  = Math.floor(diff / 86400000);
  if (mins < 1)   return 'Just now';
  if (mins < 60)  return `${mins} minute${mins === 1 ? '' : 's'} ago`;
  if (hours < 24) return `${hours} hour${hours === 1 ? '' : 's'} ago`;
  if (days === 1) return 'Yesterday';
  return `${days} days ago`;
}

function Notifications() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('all');
  const [notifications, setNotifications] = useState([]);
  const [adminName, setAdminName] = useState('');
  const [adminInitials, setAdminInitials] = useState('');

  const markAsRead = (notifId) => {
    setNotifications((prev) =>
      prev.map((n) => n.id === notifId ? { ...n, read: true } : n)
    );
    update(ref(database, `notifications/${notifId}`), { is_read: true });
  };

  useEffect(() => {
    const loadData = async () => {
      try {
        // Load admin profile for sidebar
        const storedEmail = localStorage.getItem('adminEmail') || sessionStorage.getItem('adminEmail') || '';
        const adminsRef = ref(database, 'admins');
        const adminsSnap = await get(adminsRef);
        if (adminsSnap.exists()) {
          const admins = adminsSnap.val();
          const matched = Object.values(admins).find((a) => a.email === storedEmail);
          if (matched) {
            const name = matched.fullName || 'Admin';
            setAdminName(name);
            setAdminInitials(name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2));
          }
        }

        // Load notifications
        const notifRef = ref(database, 'notifications');
        const snapshot = await get(notifRef);
        if (snapshot.exists()) {
          const data = snapshot.val();
          const notifList = Object.keys(data).map((key) => {
            const n = data[key];
            return {
              id: key,
              title: n.notif_title || '',
              description: n.notif_message || '',
              type: n.notif_type || 'scan',
              time: getTimeAgo(n.timestamp || n.time_scanned || null),
              timestamp: n.timestamp || n.time_scanned || 0,
              read: n.is_read || false,
            };
          });
          const sorted = [...notifList].sort((a, b) => b.timestamp - a.timestamp);
          setNotifications(sorted);
        }
      } catch (err) {
        console.error('Failed to load notifications:', err);
      }
    };
    loadData();
  }, []);

  const allCount = notifications.length;
  const unreadCount = notifications.filter((n) => !n.read).length;

  const filteredNotifications =
    activeTab === 'unread'
      ? notifications.filter((n) => !n.read)
      : notifications;

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
            <div
              key={notif.id}
              className={`notif-card ${notif.read ? '' : 'notif-unread'} ${notif.type === 'new_farmer' ? 'notif-new-farmer' : notif.type === 'scan_disease' ? 'notif-disease' : notif.type === 'scan_healthy' ? 'notif-healthy' : ''}`}
              onClick={() => !notif.read && markAsRead(notif.id)}
              style={{ cursor: notif.read ? 'default' : 'pointer' }}
            >
              <div className="notif-avatar">
                {notif.type === 'new_farmer' ? (
                  /* User / person icon */
                  <svg width="38" height="38" viewBox="0 0 24 24" fill="none" stroke="#4caf50" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <circle cx="12" cy="8" r="4" />
                    <path d="M4 20c0-4 4-7 8-7s8 3 8 7" />
                  </svg>
                ) : notif.type === 'scan_disease' ? (
                  /* Warning triangle icon */
                  <svg width="38" height="38" viewBox="0 0 24 24" fill="none" stroke="#e65100" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                    <line x1="12" y1="9" x2="12" y2="13" />
                    <line x1="12" y1="17" x2="12.01" y2="17" />
                  </svg>
                ) : notif.type === 'scan_healthy' ? (
                  /* Corn / leaf icon */
                  <svg width="38" height="38" viewBox="0 0 24 24" fill="none" stroke="#2e7d32" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M12 22V12" />
                    <path d="M12 12C12 12 7 10 5 5c3 0 6 2 7 7z" />
                    <path d="M12 12C12 12 17 10 19 5c-3 0-6 2-7 7z" />
                    <path d="M12 12C12 12 9 7 9 3c2 1 4 4 3 9z" />
                    <path d="M12 12C12 12 15 7 15 3c-2 1-4 4-3 9z" />
                  </svg>
                ) : (
                  /* Generic scan icon fallback */
                  <svg width="38" height="38" viewBox="0 0 24 24" fill="none" stroke="#9e9e9e" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <circle cx="11" cy="11" r="8" />
                    <line x1="21" y1="21" x2="16.65" y2="16.65" />
                  </svg>
                )}
              </div>
              <div className="notif-content">
                <span className="notif-card-title">{notif.title}</span>
                <span className="notif-card-desc">{notif.description}</span>
                {notif.time && <span className="notif-card-time">{notif.time}</span>}
              </div>
              <div className="notif-read-indicator">
                {notif.read
                  ? <span className="notif-badge-read">Read</span>
                  : <span className="notif-badge-unread">New</span>}
              </div>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}

export default Notifications;
