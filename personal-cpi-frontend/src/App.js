import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8080';

// Auth Context for managing authentication state
const AuthContext = React.createContext();

const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [user, setUser] = useState(null);

  const login = (token) => {
    localStorage.setItem('token', token);
    setToken(token);
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ token, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

// API utility functions
const api = {
  signup: async (userData) => {
    const response = await fetch(`${API_BASE_URL}/auth/signup`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(userData)
    });
    return response;
  },

  login: async (credentials) => {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials)
    });
    return response;
  },

  verify: async (verificationData) => {
    const response = await fetch(`${API_BASE_URL}/auth/verify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(verificationData)
    });
    return response;
  },

  resend: async (email) => {
    const response = await fetch(`${API_BASE_URL}/auth/resend?email=${email}`, {
      method: 'POST'
    });
    return response;
  }
};

// Login Component
const LoginForm = ({ onSwitchToSignup }) => {
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = React.useContext(AuthContext);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await api.login(formData);
      
      if (response.ok) {
        const data = await response.json();
        login(data.token);
        alert('Login successful!');
      } else {
        const errorText = await response.text();
        setError(errorText || 'Login failed');
      }
    } catch (err) {
      setError('Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  return (
    <div>
      <h2>Login to Personal CPI Tracker</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Email:</label>
          <input
            type="email"
            name="email"
            value={formData.email}
            onChange={handleChange}
            required
          />
        </div>
        
        <div>
          <label>Password:</label>
          <input
            type="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
            required
          />
        </div>

        {error && <div style={{color: 'red'}}>{error}</div>}
        
        <button type="submit" disabled={loading}>
          {loading ? 'Logging in...' : 'Login'}
        </button>
      </form>
      
      <p>
        Don't have an account?{' '}
        <button onClick={onSwitchToSignup}>Sign up</button>
      </p>
    </div>
  );
};

// Signup Component
const SignupForm = ({ onSwitchToLogin, onSignupSuccess }) => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await api.signup(formData);
      
      if (response.ok) {
        const data = await response.json();
        onSignupSuccess(formData.email);
      } else {
        const errorData = await response.json();
        if (response.status === 409) {
          setError(errorData.message + ' Would you like to resend verification?');
        } else {
          setError(errorData.message || 'Signup failed');
        }
      }
    } catch (err) {
      setError('Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleResend = async () => {
    try {
      const response = await api.resend(formData.email);
      if (response.ok) {
        alert('Verification code sent! Check your email.');
        onSignupSuccess(formData.email);
      } else {
        const errorText = await response.text();
        setError(errorText);
      }
    } catch (err) {
      setError('Failed to resend verification code');
    }
  };

  return (
    <div>
      <h2>Sign Up for Personal CPI Tracker</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Username:</label>
          <input
            type="text"
            name="username"
            value={formData.username}
            onChange={handleChange}
            required
          />
        </div>

        <div>
          <label>Email:</label>
          <input
            type="email"
            name="email"
            value={formData.email}
            onChange={handleChange}
            required
          />
        </div>
        
        <div>
          <label>Password:</label>
          <input
            type="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
            required
          />
        </div>

        {error && (
          <div style={{color: 'red'}}>
            {error}
            {error.includes('resend verification') && (
              <button type="button" onClick={handleResend}>
                Resend Verification Code
              </button>
            )}
          </div>
        )}
        
        <button type="submit" disabled={loading}>
          {loading ? 'Signing up...' : 'Sign Up'}
        </button>
      </form>
      
      <p>
        Already have an account?{' '}
        <button onClick={onSwitchToLogin}>Login</button>
      </p>
    </div>
  );
};

// Verification Component
const VerificationForm = ({ email, onVerificationSuccess, onBackToSignup }) => {
  const [verificationCode, setVerificationCode] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await api.verify({
        email: email,
        verificationCode: verificationCode
      });
      
      if (response.ok) {
        alert('Email verified successfully! You can now login.');
        onVerificationSuccess();
      } else {
        const errorText = await response.text();
        setError(errorText || 'Verification failed');
      }
    } catch (err) {
      setError('Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    try {
      const response = await api.resend(email);
      if (response.ok) {
        alert('New verification code sent! Check your email.');
      } else {
        const errorText = await response.text();
        setError(errorText);
      }
    } catch (err) {
      setError('Failed to resend verification code');
    }
  };

  return (
    <div>
      <h2>Verify Your Email</h2>
      <p>We sent a verification code to: <strong>{email}</strong></p>
      
      <form onSubmit={handleSubmit}>
        <div>
          <label>Verification Code:</label>
          <input
            type="text"
            value={verificationCode}
            onChange={(e) => setVerificationCode(e.target.value)}
            required
            placeholder="Enter 6-digit code"
          />
        </div>

        {error && <div style={{color: 'red'}}>{error}</div>}
        
        <button type="submit" disabled={loading}>
          {loading ? 'Verifying...' : 'Verify Email'}
        </button>
      </form>
      
      <p>
        Didn't receive the code?{' '}
        <button onClick={handleResend}>Resend Code</button>
      </p>
      
      <p>
        <button onClick={onBackToSignup}>Back to Signup</button>
      </p>
    </div>
  );
};

// Dashboard Component (shown after login)
const Dashboard = () => {
  const { logout, token } = React.useContext(AuthContext);
  
  const testAuth = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/receipts/test-auth`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      const text = await response.text();
      alert(`Auth test result: ${text}`);
    } catch (err) {
      alert('Auth test failed: ' + err.message);
    }
  };

  return (
    <div>
      <h1>Personal CPI Tracker Dashboard</h1>
      <p>Welcome! Your authentication is working.</p>
      
      <div>
        <button onClick={testAuth}>Test Authentication</button>
        <button onClick={logout}>Logout</button>
      </div>
      
      <div>
        <h3>Coming Soon:</h3>
        <ul>
          <li>Receipt Upload</li>
          <li>Spending Dashboard</li>
          <li>CPI Analysis</li>
          <li>Monthly Reports</li>
        </ul>
      </div>
    </div>
  );
};

// Main App Component
const App = () => {
  const [currentView, setCurrentView] = useState('login');
  const [verificationEmail, setVerificationEmail] = useState('');
  const { token } = React.useContext(AuthContext);

  // If user is logged in, show dashboard
  if (token) {
    return <Dashboard />;
  }

  // Handle view switching
  const switchToLogin = () => setCurrentView('login');
  const switchToSignup = () => setCurrentView('signup');
  const switchToVerification = (email) => {
    setVerificationEmail(email);
    setCurrentView('verification');
  };

  return (
    <div style={{ padding: '20px', maxWidth: '400px', margin: '0 auto' }}>
      {currentView === 'login' && (
        <LoginForm onSwitchToSignup={switchToSignup} />
      )}
      
      {currentView === 'signup' && (
        <SignupForm 
          onSwitchToLogin={switchToLogin} 
          onSignupSuccess={switchToVerification}
        />
      )}
      
      {currentView === 'verification' && (
        <VerificationForm 
          email={verificationEmail}
          onVerificationSuccess={switchToLogin}
          onBackToSignup={switchToSignup}
        />
      )}
    </div>
  );
};

// Root component with Auth Provider
const AppWithAuth = () => {
  return (
    <AuthProvider>
      <App />
    </AuthProvider>
  );
};

export default AppWithAuth;
