// Admin Profile
// View and update the admin account details and profile photo.
import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { database } from './firebase';
import { ref, get, update } from 'firebase/database';
import './Users.css';
import './Dashboard.css';

function Profile() {
  const navigate = useNavigate();

  const [isEditing, setIsEditing] = useState(false);
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [location, setLocation] = useState('');
  const [farmSize, setFarmSize] = useState('');
  const [profilePhoto, setProfilePhoto] = useState('');
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState({ show: false, message: '', type: '' });
  const [adminKey, setAdminKey] = useState('');
  const fileInputRef = useRef(null);

  const showToast = (message, type = 'success') => {
    setToast({ show: true, message, type });
    setTimeout(() => setToast({ show: false, message: '', type: '' }), 3000);
  };

  // Load admin profile from Firebase using stored email
  useEffect(() => {
    const loadProfile = async () => {
      try {
        const storedEmail = localStorage.getItem('adminEmail') || sessionStorage.getItem('adminEmail') || '';
        const adminsRef = ref(database, 'admins');
        const snapshot = await get(adminsRef);
        if (snapshot.exists()) {
          const admins = snapshot.val();
          const matchedKey = Object.keys(admins).find(
            (key) => admins[key].email === storedEmail
          );
          if (matchedKey) {
            const data = admins[matchedKey];
            setAdminKey(matchedKey);
            setFullName(data.fullName || '');
            setEmail(data.email || '');
            setPhone(data.phone || '');
            setLocation(data.location || '');
            setFarmSize(data.farmSize || '');
            setProfilePhoto(data.profilePhoto || '');
          }
        }
      } catch (err) {
        console.error('Failed to load profile:', err);
      }
    };
    loadProfile();
  }, []);

  const handlePhotoChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      showToast('Please select an image file.', 'error');
      return;
    }

    if (file.size > 2 * 1024 * 1024) {
      showToast('Image must be less than 2MB.', 'error');
      return;
    }

    const reader = new FileReader();
    reader.onload = async (event) => {
      const base64 = event.target.result;
      setProfilePhoto(base64);
      try {
        if (adminKey) {
          const profileRef = ref(database, `admins/${adminKey}`);
          await update(profileRef, { profilePhoto: base64 });
          showToast('Profile photo updated!', 'success');
        }
      } catch (err) {
        console.error('Failed to save photo:', err);
        showToast('Failed to save photo.', 'error');
      }
    };
    reader.readAsDataURL(file);
  };

  const handleLogout = () => {
    localStorage.removeItem('adminLoggedIn');
    localStorage.removeItem('adminEmail');
    sessionStorage.removeItem('adminLoggedIn');
    sessionStorage.removeItem('adminEmail');
    navigate('/');
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      if (adminKey) {
        const profileRef = ref(database, `admins/${adminKey}`);
        await update(profileRef, {
          fullName,
          email,
          phone,
          location,
          farmSize,
        });
        showToast('Profile updated successfully!', 'success');
        setIsEditing(false);
      }
    } catch (err) {
      console.error('Failed to save profile:', err);
      showToast('Failed to save profile. Please try again.', 'error');
    } finally {
      setSaving(false);
    }
  };

  const initials = fullName
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);

  return (
    <div className="dashboard-container">
      {/* Toast Notification */}
      {toast.show && (
        <div className={`toast-notification toast-${toast.type}`}>
          <span className="toast-icon">{toast.type === 'success' ? '✓' : '✕'}</span>
          <span className="toast-message">{toast.message}</span>
        </div>
      )}
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
            <div className="user-avatar">{initials || '?'}</div>
            <div className="user-info">
              <span className="user-name">{fullName || 'Admin'}</span>
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
        {isEditing ? (
          /* ── Edit Mode ── */
          <>
            <button className="back-link" onClick={() => setIsEditing(false)}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{verticalAlign: 'middle', marginRight: '6px'}}>
                <line x1="19" y1="12" x2="5" y2="12"/>
                <polyline points="12 19 5 12 12 5"/>
              </svg>
              Back to Profile
            </button>
            <h1 className="page-title">Edit Profile</h1>

            <form className="profile-form" onSubmit={handleSave}>
              {/* Profile Photo */}
              <div className="profile-photo-card">
                {profilePhoto ? (
                  <img src={profilePhoto} alt="Profile" className="profile-photo-avatar profile-photo-img" />
                ) : (
                  <div className="profile-photo-avatar">{initials}</div>
                )}
                <div className="profile-photo-info">
                  <span className="profile-photo-label">Profile Photo</span>
                  <span className="profile-photo-desc">Update your profile picture</span>
                </div>
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handlePhotoChange}
                  accept="image/*"
                  style={{ display: 'none' }}
                />
                <button type="button" className="change-photo-btn" onClick={() => fileInputRef.current.click()}>
                  📷 Change Photo
                </button>
              </div>

              {/* Full Name */}
              <div className="profile-field">
                <label className="profile-label">
                  Full Name <span className="required">*</span>
                </label>
                <input
                  type="text"
                  className="profile-input"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  required
                />
              </div>

              {/* Email Address */}
              <div className="profile-field">
                <label className="profile-label">
                  Email Address <span className="required">*</span>
                </label>
                <input
                  type="email"
                  className="profile-input"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>

              {/* Phone Number */}
              <div className="profile-field">
                <label className="profile-label">Phone Number</label>
                <input
                  type="tel"
                  className="profile-input"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                />
              </div>

              {/* Location */}
              <div className="profile-field">
                <label className="profile-label">Location</label>
                <input
                  type="text"
                  className="profile-input"
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                />
              </div>

              {/* Farm Size */}
              <div className="profile-field">
                <label className="profile-label">Farm Size</label>
                <input
                  type="text"
                  className="profile-input"
                  value={farmSize}
                  onChange={(e) => setFarmSize(e.target.value)}
                />
              </div>

              {/* Buttons */}
              <div className="profile-actions">
                <button type="submit" className="save-btn" disabled={saving}>
                  {saving ? 'Saving...' : '✔ Save Changes'}
                </button>
                <button
                  type="button"
                  className="cancel-btn"
                  onClick={() => setIsEditing(false)}
                >
                  Cancel
                </button>
              </div>
            </form>
          </>
        ) : (
          /* ── View Mode (Admin Profile) ── */
          <>
            <h1 className="ap-title">Admin Profile</h1>

            {/* Profile Card */}
            <div className="ap-profile-card">
              {profilePhoto ? (
                <img src={profilePhoto} alt="Profile" className="ap-avatar ap-avatar-img" />
              ) : (
                <div className="ap-avatar" style={{ fontSize: '36px', fontWeight: 700 }}>{initials || '👨‍💼'}</div>
              )}
              <div className="ap-card-info">
                <h2 className="ap-card-name">{fullName}</h2>
                <p className="ap-card-email">{email}</p>
                <p className="ap-card-role">Administrator Role</p>
                <p className="ap-card-since">Member since: January 2024</p>
              </div>
              <button className="ap-edit-btn" onClick={() => setIsEditing(true)}>
                ✏️ Edit
              </button>
            </div>

            {/* Account Information */}
            <h2 className="ap-section-title">Account Information</h2>

            <div className="ap-info-field">
              <span className="ap-info-label">Full Name</span>
              <strong className="ap-info-value">{fullName}</strong>
            </div>

            <div className="ap-info-field">
              <span className="ap-info-label">Email Address</span>
              <strong className="ap-info-value">{email}</strong>
            </div>

            <div className="ap-info-row">
              <div className="ap-info-half ap-info-field">
                <span className="ap-info-label">Role</span>
                <strong className="ap-info-value">Administrator</strong>
              </div>
              <div className="ap-info-half ap-info-field">
                <span className="ap-info-label">Account Status</span>
                <span className="ap-status-active">
                  <span className="ap-status-dot" />
                  <strong>Active</strong>
                </span>
              </div>
            </div>
          </>
        )}
      </main>
    </div>
  );
}

export default Profile;
