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
              <input
                type="password"
                className="form-input"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
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
