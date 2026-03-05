// Farmer Profile
// Detailed view of an individual farmer's scan history and account info.
import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { database } from './firebase';
import { ref, get, update } from 'firebase/database';
import './UserProfile.css';
import './Dashboard.css';

const avatarColors = ['#2196f3', '#4caf50', '#e91e63', '#9c27b0', '#ff9800', '#00bcd4'];

function UserProfile() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [user, setUser] = useState(null);
  const [isActive, setIsActive] = useState(true);
  const [totalScans, setTotalScans] = useState(0);
  const [diseasesFound, setDiseasesFound] = useState(0);
  const [loading, setLoading] = useState(true);
  const [adminName, setAdminName] = useState('Admin User');
  const [adminInitials, setAdminInitials] = useState('AD');

  useEffect(() => {
    // Load admin info for sidebar
    const adminEmail = localStorage.getItem('adminEmail') || sessionStorage.getItem('adminEmail');
    if (adminEmail) {
      const adminsRef = ref(database, 'admins');
      get(adminsRef).then(snapshot => {
        if (snapshot.exists()) {
          const admins = snapshot.val();
          const matched = Object.values(admins).find(a => a.email === adminEmail);
          if (matched) {
            const name = matched.fullname || matched.name || 'Admin User';
            setAdminName(name);
            const parts = name.split(' ');
            setAdminInitials(parts.length >= 2 ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase() : name.substring(0, 2).toUpperCase());
          }
        }
      });
    }

    // Load farmer data
    if (id) {
      const farmerRef = ref(database, `farmers/${id}`);
      get(farmerRef).then(snapshot => {
        if (snapshot.exists()) {
          const data = snapshot.val();
          const fullname = data.fullname || data.name || 'Unknown';
          const createdAt = data.createdAt ? new Date(data.createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' }) : 'N/A';
          const initials = fullname.split(' ').map(w => w[0]).join('').toUpperCase().substring(0, 2);
          setUser({
            id: id,
            name: fullname,
            email: data.email_address || data.email || '',
            status: data.status || 'active',
            initials: initials,
            avatarBg: avatarColors[fullname.codePointAt(0) % avatarColors.length],
            location: data.farm_location || 'Not set',
            joined: createdAt,
            farmArea: data.farm_area ? `${data.farm_area} hectares` : 'Not set',
            photoUrl: data.profile_photo_url ? `data:image/jpeg;base64,${data.profile_photo_url}` : null,
          });
          setIsActive((data.status || 'active') === 'active');
        }
        setLoading(false);
      }).catch(() => setLoading(false));

      // Load scan stats
      const resultsRef = ref(database, 'analysis_results');
      get(resultsRef).then(snapshot => {
        if (snapshot.exists()) {
          let scans = 0;
          let diseases = 0;
          const results = snapshot.val();
          Object.values(results).forEach(r => {
            if (r.farmer_id === id) {
              scans++;
              const label = r.analysis_label || '';
              if (label && label.toLowerCase() !== 'healthy') diseases++;
            }
          });
          setTotalScans(scans);
          setDiseasesFound(diseases);
        }
      });
    }
  }, [id]);

  const handleLogout = () => {
    localStorage.removeItem('adminLoggedIn');
    localStorage.removeItem('adminEmail');
    sessionStorage.removeItem('adminLoggedIn');
    sessionStorage.removeItem('adminEmail');
    navigate('/');
  };

  const handleDeactivate = () => {
    setIsActive(false);
    if (id) {
      update(ref(database, `farmers/${id}`), { status: 'inactive' });
    }
  };

  const handleToggle = () => {
    const newStatus = !isActive;
    setIsActive(newStatus);
    if (id) {
      update(ref(database, `farmers/${id}`), { status: newStatus ? 'active' : 'inactive' });
    }
  };

  if (loading) {
    return (
      <div className="dashboard-container">
        <main className="dashboard-main">
          <h1>Loading...</h1>
        </main>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="dashboard-container">
        <main className="dashboard-main">
          <h1>User not found</h1>
          <button className="up-back-link" onClick={() => navigate('/users')}>
            ← Back to Users
          </button>
        </main>
      </div>
    );
  }

  const infectionRate = totalScans > 0 ? Math.round((diseasesFound / totalScans) * 100) : 0;

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
            <div className="user-avatar">{adminInitials}</div>
            <div className="user-info">
              <span className="user-name">{adminName}</span>
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
            <button className="nav-item active" onClick={() => navigate('/users')}>
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
        <button className="up-back-link" onClick={() => navigate('/users')}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{verticalAlign: 'middle', marginRight: '6px'}}>
            <line x1="19" y1="12" x2="5" y2="12"/>
            <polyline points="12 19 5 12 12 5"/>
          </svg>
          Back to Users
        </button>
        <h1 className="up-title">User Profile</h1>

        {/* Profile Card */}
        <div className="up-profile-card">
          <div className="up-avatar-section">
            <div
              className="up-avatar"
              style={{ backgroundColor: user.photoUrl ? 'transparent' : user.avatarBg, overflow: 'hidden', padding: 0 }}
            >
              {user.photoUrl
                ? <img src={user.photoUrl} alt={user.name} style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%' }} />
                : user.initials}
            </div>
          </div>
          <div className="up-info-section">
            <div className="up-name-row">
              <h2 className="up-name">{user.name}</h2>
              <span className={`up-status-badge ${isActive ? 'up-badge-active' : 'up-badge-inactive'}`}>
                {isActive ? 'Active' : 'Inactive'}
              </span>
            </div>
            <div className="up-details">
              <span className="up-detail-item">📧 {user.email}</span>
              <span className="up-detail-item">📍 {user.location}</span>
              <span className="up-detail-item">📅 Joined: {user.joined}</span>
              <span className="up-detail-item">🌾 Farm Area: {user.farmArea}</span>
            </div>
          </div>
        </div>

        {/* Account Control */}
        <div className="up-account-control">
          <div className="up-control-left">
            <h3 className="up-control-title">Account Control</h3>
            <div className="up-control-status">
              <span className="up-control-label">Account Status:</span>
              <button
                className={`up-toggle ${isActive ? 'up-toggle-on' : 'up-toggle-off'}`}
                onClick={handleToggle}
              >
                <span className="up-toggle-knob"></span>
              </button>
              <span className={`up-control-value ${isActive ? 'up-value-active' : 'up-value-inactive'}`}>
                {isActive ? 'Activated' : 'Deactivated'}
              </span>
            </div>
          </div>
          <button className="up-deactivate-btn" onClick={handleDeactivate}>
            Deactivate
          </button>
        </div>

        {/* Statistics */}
        <h3 className="up-stats-title">Statistics</h3>
        <div className="up-stats-grid">
          <div className="up-stat-card up-stat-blue">
            <h2 className="up-stat-number blue">{totalScans}</h2>
            <p className="up-stat-label">Total Scans</p>
          </div>
          <div className="up-stat-card up-stat-red">
            <h2 className="up-stat-number red">{diseasesFound}</h2>
            <p className="up-stat-label">Diseases Found</p>
            <span className="up-stat-change red">{infectionRate}% infection rate</span>
          </div>
        </div>
      </main>
    </div>
  );
}

export default UserProfile;
