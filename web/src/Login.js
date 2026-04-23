// Login
// Admin login form with Firebase Authentication + OTP-based forgot password.
import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { database, functions } from './firebase';
import { ref, get } from 'firebase/database';
import { httpsCallable } from 'firebase/functions';
import './Login.css';

// ── OTP Forgot Password steps ─────────────────────────────────────────────────
// step 0 → login form
// step 1 → enter email to receive OTP
// step 2 → enter 6-digit OTP
// step 3 → enter new password

function Login() {
  const navigate = useNavigate();

  // ── Login state ──────────────────────────────────────────────────────────────
  const [email, setEmail] = useState(localStorage.getItem('adminEmail') || '');
  const [password, setPassword] = useState(localStorage.getItem('adminPassword') || '');
  const [rememberMe, setRememberMe] = useState(localStorage.getItem('adminRememberMe') === 'true');
  const [showPassword, setShowPassword] = useState(false);

  // ── Shared state ─────────────────────────────────────────────────────────────
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState({ show: false, message: '', type: '' });

  // ── Forgot-password state ────────────────────────────────────────────────────
  const [step, setStep] = useState(0);           // 0=login, 1=email, 2=otp, 3=newPwd
  const [fpEmail, setFpEmail] = useState('');
  const [otpDigits, setOtpDigits] = useState(['', '', '', '', '', '']);
  const [newPwd, setNewPwd] = useState('');
  const [confirmPwd, setConfirmPwd] = useState('');
  const [showNewPwd, setShowNewPwd] = useState(false);
  const [countdown, setCountdown] = useState(0); // seconds until resend allowed
  const otpRefs = useRef([]);
  const countdownRef = useRef(null);

  // ── Toast helper ─────────────────────────────────────────────────────────────
  const showToast = (message, type = 'success') => {
    setToast({ show: true, message, type });
    setTimeout(() => setToast({ show: false, message: '', type: '' }), 3000);
  };

  // ── Cleanup countdown on unmount ─────────────────────────────────────────────
  useEffect(() => () => clearInterval(countdownRef.current), []);

  // ── Start 60s resend countdown ───────────────────────────────────────────────
  const startCountdown = () => {
    setCountdown(60);
    clearInterval(countdownRef.current);
    countdownRef.current = setInterval(() => {
      setCountdown((c) => {
        if (c <= 1) { clearInterval(countdownRef.current); return 0; }
        return c - 1;
      });
    }, 1000);
  };

  // ── Login submit ─────────────────────────────────────────────────────────────
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const snapshot = await get(ref(database, 'admins'));
      if (snapshot.exists()) {
        const admins = snapshot.val();
        const match = Object.values(admins).find(
          (a) => a.email === email && a.password === password
        );
        if (match) {
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

  // ── Step 1: send OTP ─────────────────────────────────────────────────────────
  const handleSendOtp = async (e) => {
    e.preventDefault();
    setError('');
    if (!fpEmail.trim()) { setError('Please enter your email.'); return; }
    setLoading(true);
    try {
      const sendOtp = httpsCallable(functions, 'sendOtp');
      await sendOtp({ email: fpEmail.trim().toLowerCase() });
      showToast('OTP sent! Check your inbox.', 'success');
      setOtpDigits(['', '', '', '', '', '']);
      startCountdown();
      setStep(2);
      setTimeout(() => otpRefs.current[0]?.focus(), 200);
    } catch (err) {
      const msg = err?.message || 'Failed to send OTP.';
      setError(msg.replace('Firebase: ', '').replace(/ \(.*\)$/, ''));
    } finally {
      setLoading(false);
    }
  };

  // ── OTP input handling ───────────────────────────────────────────────────────
  const handleOtpChange = (index, value) => {
    if (!/^\d?$/.test(value)) return;
    const digits = [...otpDigits];
    digits[index] = value;
    setOtpDigits(digits);
    if (value && index < 5) otpRefs.current[index + 1]?.focus();
  };

  const handleOtpKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otpDigits[index] && index > 0) {
      otpRefs.current[index - 1]?.focus();
    }
  };

  const handleOtpPaste = (e) => {
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (pasted.length === 6) {
      setOtpDigits(pasted.split(''));
      otpRefs.current[5]?.focus();
      e.preventDefault();
    }
  };

  // ── Step 2: verify OTP → advance to step 3 ──────────────────────────────────
  const handleVerifyOtp = (e) => {
    e.preventDefault();
    setError('');
    if (otpDigits.join('').length < 6) { setError('Enter all 6 digits.'); return; }
    setStep(3);
  };

  // ── Step 3: reset password ───────────────────────────────────────────────────
  const handleResetPassword = async (e) => {
    e.preventDefault();
    setError('');
    if (newPwd.length < 6) { setError('Password must be at least 6 characters.'); return; }
    if (newPwd !== confirmPwd) { setError('Passwords do not match.'); return; }
    setLoading(true);
    try {
      const verifyOtpAndReset = httpsCallable(functions, 'verifyOtpAndReset');
      await verifyOtpAndReset({
        email: fpEmail.trim().toLowerCase(),
        otp: otpDigits.join(''),
        newPassword: newPwd,
      });
      showToast('Password reset successfully!', 'success');
      clearInterval(countdownRef.current);
      setStep(0);
      setFpEmail('');
      setOtpDigits(['', '', '', '', '', '']);
      setNewPwd('');
      setConfirmPwd('');
    } catch (err) {
      const msg = err?.message || 'Failed to reset password.';
      setError(msg.replace('Firebase: ', '').replace(/ \(.*\)$/, ''));
      // OTP wrong or expired — go back to OTP entry
      if (err?.code === 'functions/unauthenticated' || err?.code === 'functions/deadline-exceeded') {
        setStep(2);
        setOtpDigits(['', '', '', '', '', '']);
        setTimeout(() => otpRefs.current[0]?.focus(), 200);
      }
    } finally {
      setLoading(false);
    }
  };

  // ── Resend OTP ───────────────────────────────────────────────────────────────
  const handleResend = async () => {
    if (countdown > 0) return;
    setError('');
    setLoading(true);
    try {
      const sendOtp = httpsCallable(functions, 'sendOtp');
      await sendOtp({ email: fpEmail.trim().toLowerCase() });
      showToast('New OTP sent!', 'success');
      setOtpDigits(['', '', '', '', '', '']);
      startCountdown();
      setTimeout(() => otpRefs.current[0]?.focus(), 200);
    } catch (err) {
      const msg = err?.message || 'Failed to resend OTP.';
      setError(msg.replace('Firebase: ', '').replace(/ \(.*\)$/, ''));
    } finally {
      setLoading(false);
    }
  };

  // ── Reset entire forgot-password flow ────────────────────────────────────────
  const resetForgotFlow = () => {
    clearInterval(countdownRef.current);
    setStep(0);
    setFpEmail('');
    setOtpDigits(['', '', '', '', '', '']);
    setNewPwd('');
    setConfirmPwd('');
    setError('');
    setCountdown(0);
  };

  // ── Eye icon SVGs ─────────────────────────────────────────────────────────────
  const EyeOff = () => (
    <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
      <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
      <line x1="1" y1="1" x2="23" y2="23"/>
    </svg>
  );
  const EyeOn = () => (
    <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
      <circle cx="12" cy="12" r="3"/>
    </svg>
  );

  return (
    <>
      {toast.show && (
        <div className={`toast-notification toast-${toast.type}`}>
          <div className="toast-icon">{toast.type === 'success' ? '✓' : '✕'}</div>
          <span className="toast-message">{toast.message}</span>
        </div>
      )}

      <div className="login-container">
        {/* ── Sidebar ── */}
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

        {/* ── STEP 0: Login ── */}
        {step === 0 && (
          <div className="login-main">
            <div className="login-form-wrapper">
              <h1 className="login-heading">Welcome Back</h1>
              <p className="login-subheading">Sign in to your admin account</p>

              <form onSubmit={handleSubmit}>
                {error && <p className="login-error">{error}</p>}

                <div className="form-group">
                  <label className="form-label" htmlFor="login-email">Email Address</label>
                  <input
                    id="login-email"
                    type="email"
                    className="form-input"
                    placeholder="admin@corneye.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="login-password">Password</label>
                  <div className="password-wrapper">
                    <input
                      id="login-password"
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
                      {showPassword ? <EyeOff /> : <EyeOn />}
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
                    <span> Remember me</span>
                  </label>
                </div>

                <button type="submit" className="login-button" disabled={loading}>
                  {loading ? 'Signing In...' : 'Sign In'}
                </button>

                <p className="forgot-link-row">
                  <button
                    type="button"
                    className="forgot-link"
                    onClick={() => { setError(''); setStep(1); }}
                  >
                    Forgot password?
                  </button>
                </p>
              </form>
            </div>
          </div>
        )}

        {/* ── STEP 1: Enter email ── */}
        {step === 1 && (
          <div className="login-main">
            <div className="login-form-wrapper">
              <button type="button" className="fp-back-btn" onClick={resetForgotFlow}>
                ← Back to login
              </button>
              <h1 className="login-heading">Forgot Password</h1>
              <p className="login-subheading">
                Enter your admin email and we'll send you a 6-digit OTP.
              </p>

              <form onSubmit={handleSendOtp}>
                {error && <p className="login-error">{error}</p>}

                <div className="form-group">
                  <label className="form-label" htmlFor="fp-email">Email Address</label>
                  <input
                    id="fp-email"
                    type="email"
                    className="form-input"
                    placeholder="admin@corneye.com"
                    value={fpEmail}
                    onChange={(e) => setFpEmail(e.target.value)}
                    required
                    autoFocus
                  />
                </div>

                <button type="submit" className="login-button" disabled={loading}>
                  {loading ? 'Sending OTP...' : 'Send OTP'}
                </button>
              </form>
            </div>
          </div>
        )}

        {/* ── STEP 2: Enter OTP ── */}
        {step === 2 && (
          <div className="login-main">
            <div className="login-form-wrapper">
              <button type="button" className="fp-back-btn" onClick={() => { setError(''); setStep(1); }}>
                ← Change email
              </button>
              <h1 className="login-heading">Enter OTP</h1>
              <p className="login-subheading">
                A 6-digit code was sent to <strong>{fpEmail}</strong>. It expires in 10 minutes.
              </p>

              <form onSubmit={handleVerifyOtp}>
                {error && <p className="login-error">{error}</p>}

                <div className="otp-box-row" onPaste={handleOtpPaste}>
                  {otpDigits.map((digit, i) => (
                    <input
                      key={i}
                      ref={(el) => (otpRefs.current[i] = el)}
                      type="text"
                      inputMode="numeric"
                      maxLength={1}
                      className="otp-box"
                      value={digit}
                      onChange={(e) => handleOtpChange(i, e.target.value)}
                      onKeyDown={(e) => handleOtpKeyDown(i, e)}
                    />
                  ))}
                </div>

                <div className="otp-resend-row">
                  {countdown > 0 ? (
                    <span className="otp-countdown">Resend OTP in {countdown}s</span>
                  ) : (
                    <button
                      type="button"
                      className="forgot-link"
                      onClick={handleResend}
                      disabled={loading}
                    >
                      Resend OTP
                    </button>
                  )}
                </div>

                <button
                  type="submit"
                  className="login-button"
                  disabled={loading || otpDigits.join('').length < 6}
                >
                  {loading ? 'Verifying...' : 'Verify OTP'}
                </button>
              </form>
            </div>
          </div>
        )}

        {/* ── STEP 3: New password ── */}
        {step === 3 && (
          <div className="login-main">
            <div className="login-form-wrapper">
              <button type="button" className="fp-back-btn" onClick={() => { setError(''); setStep(2); }}>
                ← Back
              </button>
              <h1 className="login-heading">New Password</h1>
              <p className="login-subheading">Create a new password for your admin account.</p>

              <form onSubmit={handleResetPassword}>
                {error && <p className="login-error">{error}</p>}

                <div className="form-group">
                  <label className="form-label" htmlFor="new-pwd">New Password</label>
                  <div className="password-wrapper">
                    <input
                      id="new-pwd"
                      type={showNewPwd ? 'text' : 'password'}
                      className="form-input"
                      placeholder="Min. 6 characters"
                      value={newPwd}
                      onChange={(e) => setNewPwd(e.target.value)}
                      required
                      autoFocus
                    />
                    <button
                      type="button"
                      className="password-toggle"
                      onClick={() => setShowNewPwd((v) => !v)}
                      aria-label={showNewPwd ? 'Hide password' : 'Show password'}
                    >
                      {showNewPwd ? <EyeOff /> : <EyeOn />}
                    </button>
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="confirm-pwd">Confirm Password</label>
                  <input
                    id="confirm-pwd"
                    type={showNewPwd ? 'text' : 'password'}
                    className="form-input"
                    placeholder="Re-enter new password"
                    value={confirmPwd}
                    onChange={(e) => setConfirmPwd(e.target.value)}
                    required
                  />
                </div>

                <button type="submit" className="login-button" disabled={loading}>
                  {loading ? 'Resetting...' : 'Reset Password'}
                </button>
              </form>
            </div>
          </div>
        )}
      </div>
    </>
  );
}

export default Login;

