// Login
// Admin login form with Firebase Authentication.
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { database } from './firebase';
import { ref, get } from 'firebase/database';
import './Login.css';

function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState(localStorage.getItem('adminEmail') || '');
  const [password, setPassword] = useState(localStorage.getItem('adminPassword') || '');
  const [rememberMe, setRememberMe] = useState(localStorage.getItem('adminRememberMe') === 'true');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState({ show: false, message: '', type: '' });
  const [showPassword, setShowPassword] = useState(false);

  const showToast = (message, type = 'success') => {
    setToast({ show: true, message, type });
    setTimeout(() => setToast({ show: false, message: '', type: '' }), 3000);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const adminsRef = ref(database, 'admins');
      const snapshot = await get(adminsRef);

      if (snapshot.exists()) {
        const admins = snapshot.val();
        const matchedAdmin = Object.values(admins).find(
          (admin) => admin.email === email && admin.password === password
        );

        if (matchedAdmin) {
          if (rememberMe) {
            localStorage.setItem('adminLoggedIn', 'true');
            localStorage.setItem('adminEmail', email);
            localStorage.setItem('adminPassword', password);
            localStorage.setItem('adminRememberMe', 'true');
          } else {
            localStorage.removeItem('adminEmail');
            localStorage.removeItem('adminPassword');
            localStorage.removeItem('adminRememberMe');
            sessionStorage.setItem('adminLoggedIn', 'true');
            sessionStorage.setItem('adminEmail', email);
          }
          showToast('Login successful! Redirecting...', 'success');
          setTimeout(() => navigate('/dashboard'), 1500);
        } else {
          setError('Invalid email or password.');
        }
      } else {
        setError('No admin accounts found.');
      }
    } catch (err) {
      console.error(err);
      setError('Failed to connect to database.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      {toast.show && (
        <div className={`toast-notification toast-${toast.type}`}>
          <div className="toast-icon">
            {toast.type === 'success' ? '✓' : '✕'}
          </div>
          <span className="toast-message">{toast.message}</span>
        </div>
      )}
      <div className="login-container">
      <div className="login-sidebar">
        <div className="sidebar-content">
          <img
            src={process.env.PUBLIC_URL + '/logo.png'}
            alt="CornEye Logo"
            className="sidebar-logo"
          />
          <h2 className="sidebar-title">Admin Portal</h2>
          <p className="sidebar-subtitle">
            Manage your corn disease detection system with ease.
          </p>
        </div>
      </div>

      <div className="login-main">
        <div className="login-form-wrapper">
          <h1 className="login-heading">Welcome Back</h1>
          <p className="login-subheading">Sign in to your admin account</p>

          <form onSubmit={handleSubmit}>
            {error && <p className="login-error">{error}</p>}
            <div className="form-group">
              <label className="form-label">Email Address</label>
              <input
                type="email"
                className="form-input"
                placeholder="admin@corneye.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Password</label>
              <div className="password-wrapper">
                <input
                  type={showPassword ? 'text' : 'password'}
                  className="form-input"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowPassword((v) => !v)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? (
                    <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                      <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                      <line x1="1" y1="1" x2="23" y2="23"/>
                    </svg>
                  ) : (
                    <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                      <circle cx="12" cy="12" r="3"/>
                    </svg>
                  )}
                </button>
              </div>
            </div>

            <div className="form-checkbox-group">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="checkbox-input"
                />
                <span className="checkbox-custom"></span>
                Remember me
              </label>
            </div>

            <button type="submit" className="login-button" disabled={loading}>
              {loading ? 'Signing In...' : 'Sign In'}
            </button>
          </form>
        </div>
      </div>
    </div>
    </>
  );
}

export default Login;
