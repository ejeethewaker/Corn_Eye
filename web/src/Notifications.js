// Notifications
// Displays and manages broadcast notifications sent to farmers.
import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { database } from './firebase';
import { ref, get, update, remove, onValue } from 'firebase/database';
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

function formatTimestamp(ts) {
  if (!ts) return '—';
  const d = new Date(ts);
  return d.toLocaleString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: 'numeric', minute: '2-digit', hour12: true,
  });
}

function Notifications() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('all');
  const [notifications, setNotifications] = useState([]);
  const [adminName, setAdminName] = useState('');
  const [adminInitials, setAdminInitials] = useState('');
  const [selectedNotif, setSelectedNotif] = useState(null);
  const [farmerName, setFarmerName] = useState('');
  const [scanImage, setScanImage] = useState(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);

  const markAsRead = (notifId) => {
    setNotifications((prev) =>
      prev.map((n) => n.id === notifId ? { ...n, read: true } : n)
    );
    update(ref(database, `notifications/${notifId}`), { is_read: true });
  };

  const deleteNotif = (notifId) => {
    remove(ref(database, `notifications/${notifId}`));
    setConfirmDeleteId(null);
    setSelectedNotif(null);
  };

  const openNotifDetail = (notif) => {
    if (!notif.read) markAsRead(notif.id);
    setSelectedNotif(notif);
    setFarmerName('');
    setScanImage(null);
    if (notif.farmerId) {
      get(ref(database, `farmers/${notif.farmerId}/fullname`)).then((snap) => {
        if (snap.exists()) setFarmerName(snap.val());
      }).catch(() => {});
    }
    if (notif.analysisId) {
      get(ref(database, `analysis_results/${notif.analysisId}/image_url`)).then((snap) => {
        if (snap.exists()) setScanImage(snap.val());
      }).catch(() => {});
    }
  };

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

    // Real-time notifications listener
    const unsubNotifs = onValue(ref(database, 'notifications'), (snapshot) => {
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
            farmerId: n.farmer_id || '',
            analysisId: n.analysis_id || '',
            analysisLabel: n.analysis_label || '',
            confidence: n.confidence_score || null,
          };
        });
        const sorted = [...notifList].sort((a, b) => b.timestamp - a.timestamp);
        setNotifications(sorted);
      } else {
        setNotifications([]);
      }
    });

    return () => {
      unsubNotifs();
    };
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
            <button className="nav-item" onClick={() => navigate('/users')}>
              <span className="nav-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="7" r="4"/>
                  <path d="M5.5 21a6.5 6.5 0 0113 0"/>
                </svg>
              </span>
              <span>Users</span>
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
          {filteredNotifications.map((notif) => {
            let typeClass = '';
            if (notif.type === 'new_farmer') typeClass = 'notif-new-farmer';
            else if (notif.type === 'scan_disease') typeClass = 'notif-disease';
            else if (notif.type === 'scan_healthy') typeClass = 'notif-healthy';

            let notifIcon;
            if (notif.type === 'new_farmer') {
              notifIcon = (
                <svg width="38" height="38" viewBox="0 0 24 24" fill="none" stroke="#4caf50" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="8" r="4" />
                  <path d="M4 20c0-4 4-7 8-7s8 3 8 7" />
                </svg>
              );
            } else if (notif.type === 'scan_disease') {
              notifIcon = (
                <svg width="38" height="38" viewBox="0 0 24 24" fill="none" stroke="#e65100" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                  <line x1="12" y1="9" x2="12" y2="13" />
                  <line x1="12" y1="17" x2="12.01" y2="17" />
                </svg>
              );
            } else if (notif.type === 'scan_healthy') {
              notifIcon = (
                <svg width="38" height="38" viewBox="0 0 24 24" fill="none" stroke="#2e7d32" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 22V12" />
                  <path d="M12 12C12 12 7 10 5 5c3 0 6 2 7 7z" />
                  <path d="M12 12C12 12 17 10 19 5c-3 0-6 2-7 7z" />
                  <path d="M12 12C12 12 9 7 9 3c2 1 4 4 3 9z" />
                  <path d="M12 12C12 12 15 7 15 3c-2 1-4 4-3 9z" />
                </svg>
              );
            } else {
              notifIcon = (
                <svg width="38" height="38" viewBox="0 0 24 24" fill="none" stroke="#9e9e9e" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="11" cy="11" r="8" />
                  <line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
              );
            }

            const NotifWrapper = 'button';

            return (
              <NotifWrapper
                key={notif.id}
                type="button"
                className={`notif-card ${notif.read ? '' : 'notif-unread'} ${typeClass}`}
                onClick={() => openNotifDetail(notif)}
                style={{ cursor: 'pointer' }}
              >
                <div className="notif-avatar">{notifIcon}</div>
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
              </NotifWrapper>
            );
          })}
        </div>

        {/* Notification Detail Modal */}
        {selectedNotif && (() => {
          let headerClass = 'notif-modal-farmer';
          if (selectedNotif.type === 'scan_disease') headerClass = 'notif-modal-disease';
          else if (selectedNotif.type === 'scan_healthy') headerClass = 'notif-modal-healthy';
          const typeLabels = { new_farmer: 'New Registration', scan_disease: 'Disease Scan', scan_healthy: 'Healthy Scan' };
          const typeLabel = typeLabels[selectedNotif.type] || 'Notification';
          return (
          // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
          <dialog className="notif-modal-overlay" open
            onClick={() => setSelectedNotif(null)} onKeyDown={(e) => e.key === 'Escape' && setSelectedNotif(null)}>
            {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions */}
            <div className="notif-modal" onClick={(e) => e.stopPropagation()}>
              <button className="notif-modal-close" onClick={() => setSelectedNotif(null)}>&times;</button>

              <div className={`notif-modal-header ${headerClass}`}>
                <h2>{selectedNotif.title}</h2>
                <span className="notif-modal-type">{typeLabel}</span>
              </div>

              <div className="notif-modal-body">
                <p className="notif-modal-message">{selectedNotif.description}</p>

                {scanImage && (
                  <div className="notif-modal-image-wrapper">
                    <img
                      src={`data:image/jpeg;base64,${scanImage}`}
                      alt="Scanned leaf"
                      className="notif-modal-image"
                    />
                  </div>
                )}

                <div className="notif-modal-details">
                  <div className="notif-detail-row">
                    <span className="notif-detail-label">Date & Time</span>
                    <span className="notif-detail-value">{formatTimestamp(selectedNotif.timestamp)}</span>
                  </div>

                  {farmerName && (
                    <div className="notif-detail-row">
                      <span className="notif-detail-label">Farmer</span>
                      <span className="notif-detail-value">{farmerName}</span>
                    </div>
                  )}

                  {selectedNotif.farmerId && (
                    <div className="notif-detail-row">
                      <span className="notif-detail-label">Farmer ID</span>
                      <span className="notif-detail-value notif-detail-mono">{selectedNotif.farmerId}</span>
                    </div>
                  )}

                  {selectedNotif.analysisLabel && (
                    <div className="notif-detail-row">
                      <span className="notif-detail-label">Diagnosis</span>
                      <span className={`notif-detail-value notif-detail-tag ${selectedNotif.analysisLabel === 'Healthy' ? 'notif-tag-healthy' : 'notif-tag-disease'}`}>
                        {selectedNotif.analysisLabel}
                      </span>
                    </div>
                  )}

                  {selectedNotif.confidence != null && (
                    <div className="notif-detail-row">
                      <span className="notif-detail-label">Confidence</span>
                      <span className="notif-detail-value">{(selectedNotif.confidence * 100).toFixed(1)}%</span>
                    </div>
                  )}

                  {selectedNotif.analysisId && (
                    <div className="notif-detail-row">
                      <span className="notif-detail-label">Analysis ID</span>
                      <span className="notif-detail-value notif-detail-mono">{selectedNotif.analysisId}</span>
                    </div>
                  )}

                  <div className="notif-detail-row">
                    <span className="notif-detail-label">Status</span>
                    <span className="notif-detail-value">{selectedNotif.read ? 'Read' : 'Unread'}</span>
                  </div>
                </div>

                <button className="notif-delete-btn" onClick={() => setConfirmDeleteId(selectedNotif.id)}>
                  Delete Notification
                </button>
              </div>
            </div>
          </dialog>
          );
        })()}

        {/* Delete Confirmation Modal */}
        {confirmDeleteId && (
          // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
          <dialog className="notif-modal-overlay confirm-overlay" open
            onClick={() => setConfirmDeleteId(null)} onKeyDown={(e) => e.key === 'Escape' && setConfirmDeleteId(null)}>
            {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions */}
            <div className="confirm-modal" onClick={(e) => e.stopPropagation()}>
              <div className="confirm-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d32f2f" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M3 6h18"/><path d="M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
                  <path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/>
                  <line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/>
                </svg>
              </div>
              <h3 className="confirm-title">Delete Notification</h3>
              <p className="confirm-message">Are you sure you want to delete this notification? This action cannot be undone.</p>
              <div className="confirm-actions">
                <button className="confirm-cancel" onClick={() => setConfirmDeleteId(null)}>Cancel</button>
                <button className="confirm-delete" onClick={() => deleteNotif(confirmDeleteId)}>Delete</button>
              </div>
            </div>
          </dialog>
        )}
      </main>
    </div>
  );
}

export default Notifications;
