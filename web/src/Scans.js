// Scans List
// Shows all scan results with optional filter (all / disease / healthy).
import React, { useState, useEffect } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { database } from './firebase';
import { ref, get, onValue } from 'firebase/database';
import './Scans.css';
import './Dashboard.css';
import './UserProfile.css';
import './Notifications.css';

function isDiseaseLabel(label) {
  return label && label.toLowerCase() !== 'healthy' && label.toLowerCase() !== 'invalid';
}

function badgeClass(label) {
  const l = (label || '').toLowerCase();
  if (l === 'healthy') return 'scan-badge scan-badge-healthy';
  if (l === 'common rust') return 'scan-badge scan-badge-rust';
  if (l === 'gray leaf spot') return 'scan-badge scan-badge-gls';
  if (l === 'northern leaf blight') return 'scan-badge scan-badge-nlb';
  return 'scan-badge scan-badge-disease';
}

function formatDate(ts) {
  if (!ts) return '—';
  return new Date(ts).toLocaleString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric',
    hour: 'numeric', minute: '2-digit', hour12: true,
  });
}

function Scans() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const filterParam = searchParams.get('filter') || 'all'; // all | disease | healthy

  const [allScans, setAllScans] = useState([]);
  const [farmerMap, setFarmerMap] = useState({});
  const [searchQuery, setSearchQuery] = useState('');
  const [adminName, setAdminName] = useState('');
  const [adminInitials, setAdminInitials] = useState('');
  const [selectedScan, setSelectedScan] = useState(null);

  useEffect(() => {
    // Admin profile — show cached value instantly, refresh in background
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
    }).catch(() => {});

    // Farmer map for names
    get(ref(database, 'farmers')).then((snap) => {
      if (snap.exists()) {
        const map = {};
        Object.entries(snap.val()).forEach(([id, f]) => {
          map[id] = f.fullname || f.name || 'Unknown Farmer';
        });
        setFarmerMap(map);
      }
    }).catch(() => {});

    // Real-time scans
    const unsub = onValue(ref(database, 'analysis_results'), (snap) => {
      if (snap.exists()) {
        const list = Object.entries(snap.val()).map(([id, r]) => ({ id, ...r }));
        // Sort newest first
        list.sort((a, b) => (b.time_scanned || 0) - (a.time_scanned || 0));
        setAllScans(list);
      } else {
        setAllScans([]);
      }
    });

    return () => unsub();
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('adminLoggedIn');
    localStorage.removeItem('adminEmail');
    localStorage.removeItem('adminCachedName');
    localStorage.removeItem('adminCachedInitials');
    sessionStorage.removeItem('adminLoggedIn');
    sessionStorage.removeItem('adminEmail');
    navigate('/');
  };
  const filtered = allScans.filter((s) => {
    const label = (s.analysis_label || '').toLowerCase();
    if (filterParam === 'disease' && !isDiseaseLabel(label)) return false;
    if (filterParam === 'healthy' && label !== 'healthy') return false;
    return true;
  });

  // Apply search
  const displayed = filtered.filter((s) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    const farmerName = (farmerMap[s.farmer_id] || '').toLowerCase();
    const label = (s.analysis_label || '').toLowerCase();
    return farmerName.includes(q) || label.includes(q);
  });

  const pageTitle =
    filterParam === 'disease' ? 'Diseases Detected' :
    filterParam === 'healthy' ? 'Healthy Scans' :
    'All Scans';

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
              <span className="user-name">{adminName || 'Admin'}</span>
              <span className="user-role">Administrator</span>
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
        <button className="up-back-link" onClick={() => navigate('/dashboard')}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{verticalAlign: 'middle', marginRight: '6px'}}>
            <line x1="19" y1="12" x2="5" y2="12"/>
            <polyline points="12 19 5 12 12 5"/>
          </svg>
          Back to Dashboard
        </button>
        <h1 className="scans-title">{pageTitle}</h1>

        {/* Filter tabs */}
        <div className="scans-filter-tabs">
          {[
            { value: 'all',     label: 'All Scans' },
            { value: 'disease', label: 'Diseases Only' },
            { value: 'healthy', label: 'Healthy Only' },
          ].map((tab) => (
            <button
              key={tab.value}
              className={`scans-filter-tab ${filterParam === tab.value ? 'scans-filter-tab-active' : ''}`}
              onClick={() => navigate(`/scans${tab.value === 'all' ? '' : `?filter=${tab.value}`}`)}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Search */}
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
            placeholder="Search by farmer or diagnosis..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {/* Count */}
        <p className="scans-count">{displayed.length} result{displayed.length !== 1 ? 's' : ''}</p>

        {/* Table header */}
        <div className="scans-table-header">
          <span className="scans-col-farmer">FARMER</span>
          <span className="scans-col-diagnosis">DIAGNOSIS</span>
          <span className="scans-col-confidence">CONFIDENCE</span>
          <span className="scans-col-date">DATE & TIME</span>
        </div>

        {/* Rows */}
        <div className="scans-list">
          {displayed.length === 0 ? (
            <p className="scans-empty">No scan records found.</p>
          ) : (
            displayed.map((scan) => (
              <div
                key={scan.id}
                className="scans-row"
                onClick={() => setSelectedScan(scan)}
                style={{ cursor: 'pointer' }}
              >
                <span className="scans-col-farmer">
                  {farmerMap[scan.farmer_id] || scan.farmer_id || '—'}
                </span>
                <span className="scans-col-diagnosis">
                  <span className={badgeClass(scan.analysis_label)}>
                    {scan.analysis_label || '—'}
                  </span>
                </span>
                <span className="scans-col-confidence">
                  {scan.confidence_score != null
                    ? `${(scan.confidence_score * 100).toFixed(1)}%`
                    : '—'}
                </span>
                <span className="scans-col-date">
                  {formatDate(scan.time_scanned || scan.timestamp)}
                </span>
              </div>
            ))
          )}
        </div>

        {/* Scan Detail Modal */}
        {selectedScan && (
          // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
          <dialog className="notif-modal-overlay" open
            onClick={() => setSelectedScan(null)} onKeyDown={(e) => e.key === 'Escape' && setSelectedScan(null)}>
            {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions */}
            <div className="notif-modal" onClick={(e) => e.stopPropagation()}>
              <button className="notif-modal-close" onClick={() => setSelectedScan(null)}>&times;</button>

              <div className={`notif-modal-header ${
                (selectedScan.analysis_label || '').toLowerCase() === 'healthy'
                  ? 'notif-modal-healthy'
                  : 'notif-modal-disease'
              }`}>
                <h2>Scan Result</h2>
                <span className="notif-modal-type">
                  {(selectedScan.analysis_label || '').toLowerCase() === 'healthy' ? 'Healthy Scan' : 'Disease Scan'}
                </span>
              </div>

              <div className="notif-modal-body">
                {selectedScan.image_url && (
                  <div className="notif-modal-image-wrapper">
                    <img
                      src={`data:image/jpeg;base64,${selectedScan.image_url}`}
                      alt="Scanned leaf"
                      className="notif-modal-image"
                    />
                  </div>
                )}

                <div className="notif-modal-details">
                  <div className="notif-detail-row">
                    <span className="notif-detail-label">Date &amp; Time</span>
                    <span className="notif-detail-value">{formatDate(selectedScan.time_scanned || selectedScan.timestamp)}</span>
                  </div>
                  <div className="notif-detail-row">
                    <span className="notif-detail-label">Farmer</span>
                    <span className="notif-detail-value">{farmerMap[selectedScan.farmer_id] || '—'}</span>
                  </div>
                  <div className="notif-detail-row">
                    <span className="notif-detail-label">Farmer ID</span>
                    <span className="notif-detail-value notif-detail-mono">{selectedScan.farmer_id || '—'}</span>
                  </div>
                  <div className="notif-detail-row">
                    <span className="notif-detail-label">Diagnosis</span>
                    <span className={`notif-detail-value notif-detail-tag ${
                      (selectedScan.analysis_label || '').toLowerCase() === 'healthy'
                        ? 'notif-tag-healthy' : 'notif-tag-disease'
                    }`}>
                      {selectedScan.analysis_label || '—'}
                    </span>
                  </div>
                  {selectedScan.confidence_score != null && (
                    <div className="notif-detail-row">
                      <span className="notif-detail-label">Confidence</span>
                      <span className="notif-detail-value">{(selectedScan.confidence_score * 100).toFixed(1)}%</span>
                    </div>
                  )}
                  {selectedScan.analysis_id && (
                    <div className="notif-detail-row">
                      <span className="notif-detail-label">Analysis ID</span>
                      <span className="notif-detail-value notif-detail-mono">{selectedScan.analysis_id}</span>
                    </div>
                  )}
                </div>

                <button
                  className="notif-delete-btn"
                  style={{ marginTop: '16px' }}
                  onClick={() => { setSelectedScan(null); navigate(`/user/${selectedScan.farmer_id}`); }}
                >
                  View Farmer Profile
                </button>
              </div>
            </div>
          </dialog>
        )}
      </main>
    </div>
  );
}

export default Scans;
