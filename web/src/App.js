// App Router
// Sets up top-level routes and authentication guards for the admin dashboard.
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import './App.css';
import Login from './Login';
import Dashboard from './Dashboard';
import Users from './Users';
import Notifications from './Notifications';
import Profile from './Profile';
import UserProfile from './UserProfile';

function isAuthenticated() {
  return (
    localStorage.getItem('adminLoggedIn') === 'true' ||
    sessionStorage.getItem('adminLoggedIn') === 'true'
  );
}

function ProtectedRoute({ children }) {
  return isAuthenticated() ? children : <Navigate to="/" />;
}

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route
          path="/dashboard"
          element={<ProtectedRoute><Dashboard /></ProtectedRoute>}
        />
        <Route
          path="/users"
          element={<ProtectedRoute><Users /></ProtectedRoute>}
        />
        <Route
          path="/notifications"
          element={<ProtectedRoute><Notifications /></ProtectedRoute>}
        />
        <Route
          path="/profile"
          element={<ProtectedRoute><Profile /></ProtectedRoute>}
        />
        <Route
          path="/user/:id"
          element={<ProtectedRoute><UserProfile /></ProtectedRoute>}
        />
      </Routes>
    </Router>
  );
}

export default App;
