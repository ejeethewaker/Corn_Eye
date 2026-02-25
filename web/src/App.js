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

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route
          path="/dashboard"
          element={isAuthenticated() ? <Dashboard /> : <Navigate to="/" />}
        />
        <Route
          path="/users"
          element={isAuthenticated() ? <Users /> : <Navigate to="/" />}
        />
        <Route
          path="/notifications"
          element={isAuthenticated() ? <Notifications /> : <Navigate to="/" />}
        />
        <Route
          path="/profile"
          element={isAuthenticated() ? <Profile /> : <Navigate to="/" />}
        />
        <Route
          path="/user/:id"
          element={isAuthenticated() ? <UserProfile /> : <Navigate to="/" />}
        />
      </Routes>
    </Router>
  );
}

export default App;
