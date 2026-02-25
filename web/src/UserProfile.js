import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import './UserProfile.css';
import './Dashboard.css';

const usersData = {
  1: {
    id: 1,
    name: 'Juan Dela Cruz',
    email: 'juan@email.com',
    status: 'Active',
    avatar: '👨‍🌾',
    avatarBg: '#2196f3',
    location: 'Nueva Ecija, Philippines',
    joined: 'August 15, 2024',
    farmArea: '2.5 hectares',
    totalScans: 47,
    scansChange: '↑ 5 this week',
    diseasesFound: 12,
    infectionRate: '25% infection rate',
  },
  2: {
    id: 2,
    name: 'Maria Santos',
    email: 'maria@email.com',
    status: 'Active',
    avatar: '👩‍🌾',
    avatarBg: '#4caf50',
    location: 'Pampanga, Philippines',
    joined: 'September 3, 2024',
    farmArea: '1.8 hectares',
    totalScans: 32,
    scansChange: '↑ 3 this week',
    diseasesFound: 8,
    infectionRate: '20% infection rate',
  },
  3: {
    id: 3,
    name: 'Pedro Reyes',
    email: 'pedro@email.com',
    status: 'Inactive',
    avatar: '👨‍🌾',
    avatarBg: '#e91e63',
    location: 'Tarlac, Philippines',
    joined: 'July 20, 2024',
    farmArea: '3.0 hectares',
    totalScans: 15,
    scansChange: '↑ 0 this week',
    diseasesFound: 5,
    infectionRate: '33% infection rate',
  },
  4: {
    id: 4,
    name: 'Ana Garcia',
    email: 'ana@email.com',
    status: 'Active',
    avatar: '👩‍🌾',
    avatarBg: '#9c27b0',
    location: 'Isabela, Philippines',
    joined: 'October 10, 2024',
    farmArea: '2.0 hectares',
    totalScans: 28,
    scansChange: '↑ 4 this week',
    diseasesFound: 6,
    infectionRate: '21% infection rate',
  },
};

function UserProfile() {
  const navigate = useNavigate();
  const { id } = useParams();
  const user = usersData[id];

  const [isActive, setIsActive] = useState(user ? user.status === 'Active' : true);

  const handleLogout = () => {
    localStorage.removeItem('adminLoggedIn');
    localStorage.removeItem('adminEmail');
    sessionStorage.removeItem('adminLoggedIn');
    sessionStorage.removeItem('adminEmail');
    navigate('/');
  };

  const handleDeactivate = () => {
    setIsActive(false);
  };

  const handleToggle = () => {
    setIsActive(!isActive);
  };

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
            <div className="up-avatar" style={{ backgroundColor: user.avatarBg }}>
              {user.avatar}
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
            <h2 className="up-stat-number blue">{user.totalScans}</h2>
            <p className="up-stat-label">Total Scans</p>
            <span className="up-stat-change green">{user.scansChange}</span>
          </div>
          <div className="up-stat-card up-stat-red">
            <h2 className="up-stat-number red">{user.diseasesFound}</h2>
            <p className="up-stat-label">Diseases Found</p>
            <span className="up-stat-change red">{user.infectionRate}</span>
          </div>
        </div>
      </main>
    </div>
  );
}

export default UserProfile;
