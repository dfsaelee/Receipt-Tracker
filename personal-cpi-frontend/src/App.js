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
  },

  // Analytics API functions
  getCurrentMonthSpending: async (token) => {
    const response = await fetch(`${API_BASE_URL}/api/receipts/summary/current-month`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    return response;
  },

  getMonthlySpending: async (token) => {
    const response = await fetch(`${API_BASE_URL}/api/receipts/summary/monthly`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    return response;
  },

  getSpendingByPeriod: async (token, startDate, endDate) => {
    const response = await fetch(`${API_BASE_URL}/api/receipts/summary/period?startDate=${startDate}&endDate=${endDate}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    return response;
  },

  getTotalSpending: async (token) => {
    const response = await fetch(`${API_BASE_URL}/api/receipts/total`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    return response;
  },

  getAllReceipts: async (token) => {
    const response = await fetch(`${API_BASE_URL}/api/receipts/all`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    return response;
  },

  getReceiptsByCategory: async (token, categoryId) => {
    const response = await fetch(`${API_BASE_URL}/api/receipts/category/${categoryId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    return response;
  },

  getReceiptsByDateRange: async (token, startDate, endDate) => {
    const response = await fetch(`${API_BASE_URL}/api/receipts/date-range?startDate=${startDate}&endDate=${endDate}`, {
      headers: { 'Authorization': `Bearer ${token}` }
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

// Analytics Components
const MonthlySpendingChart = ({ monthlyData }) => {
  if (!monthlyData || monthlyData.length === 0) {
    return <div>No monthly spending data available</div>;
  }

  const maxAmount = Math.max(...monthlyData.map(item => parseFloat(item.totalAmount)));
  
  return (
    <div style={{ margin: '20px 0' }}>
      <h3>Monthly Spending Trend</h3>
      <div style={{ display: 'flex', alignItems: 'end', height: '200px', gap: '10px' }}>
        {monthlyData.map((item, index) => {
          const height = (parseFloat(item.totalAmount) / maxAmount) * 150;
          return (
            <div key={index} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <div
                style={{
                  width: '40px',
                  height: `${height}px`,
                  backgroundColor: '#4CAF50',
                  marginBottom: '5px',
                  borderRadius: '4px 4px 0 0'
                }}
                title={`${item.monthName} ${item.year}: $${item.totalAmount}`}
              />
              <div style={{ fontSize: '12px', textAlign: 'center' }}>
                {item.monthName.substring(0, 3)}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

const CategorySpendingChart = ({ categoryData }) => {
  if (!categoryData || categoryData.length === 0) {
    return <div>No category spending data available</div>;
  }

  const totalAmount = categoryData.reduce((sum, item) => sum + parseFloat(item.totalAmount), 0);
  
  return (
    <div style={{ margin: '20px 0' }}>
      <h3>Spending by Category</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        {categoryData.map((item, index) => {
          const percentage = (parseFloat(item.totalAmount) / totalAmount) * 100;
          return (
            <div key={index} style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <div style={{ minWidth: '120px', fontSize: '14px' }}>{item.categoryName}</div>
              <div style={{ flex: 1, height: '20px', backgroundColor: '#f0f0f0', borderRadius: '10px', overflow: 'hidden' }}>
                <div
                  style={{
                    width: `${percentage}%`,
                    height: '100%',
                    backgroundColor: `hsl(${index * 60}, 70%, 50%)`,
                    transition: 'width 0.3s ease'
                  }}
                />
              </div>
              <div style={{ minWidth: '80px', fontSize: '14px', textAlign: 'right' }}>
                ${parseFloat(item.totalAmount).toFixed(2)} ({percentage.toFixed(1)}%)
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

const DateRangePicker = ({ onDateRangeChange }) => {
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (startDate && endDate) {
      onDateRangeChange(startDate, endDate);
    }
  };

  return (
    <div style={{ margin: '20px 0', padding: '15px', border: '1px solid #ddd', borderRadius: '8px' }}>
      <h3>Custom Date Range</h3>
      <form onSubmit={handleSubmit} style={{ display: 'flex', gap: '10px', alignItems: 'end' }}>
        <div>
          <label>Start Date:</label>
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            required
          />
        </div>
        <div>
          <label>End Date:</label>
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            required
          />
        </div>
        <button type="submit">Apply Filter</button>
      </form>
    </div>
  );
};

const CategoryFilter = ({ categories, selectedCategory, onCategoryChange }) => {
  return (
    <div style={{ margin: '20px 0' }}>
      <h3>Filter by Category</h3>
      <select 
        value={selectedCategory || ''} 
        onChange={(e) => onCategoryChange(e.target.value)}
        style={{ padding: '8px', fontSize: '14px', minWidth: '200px' }}
      >
        <option value="">All Categories</option>
        {categories.map(category => (
          <option key={category.categoryId} value={category.categoryId}>
            {category.categoryName}
          </option>
        ))}
      </select>
    </div>
  );
};

const AnalyticsDashboard = () => {
  const { token } = React.useContext(AuthContext);
  const [monthlyData, setMonthlyData] = useState([]);
  const [currentMonthData, setCurrentMonthData] = useState([]);
  const [totalSpending, setTotalSpending] = useState(0);
  const [allReceipts, setAllReceipts] = useState([]);
  const [filteredReceipts, setFilteredReceipts] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('');
  const [customPeriodData, setCustomPeriodData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Extract unique categories from receipts
  const categories = React.useMemo(() => {
    const categoryMap = new Map();
    allReceipts.forEach(receipt => {
      if (receipt.categoryId && receipt.categoryName) {
        categoryMap.set(receipt.categoryId, {
          categoryId: receipt.categoryId,
          categoryName: receipt.categoryName
        });
      }
    });
    return Array.from(categoryMap.values());
  }, [allReceipts]);

  // Load initial data
  React.useEffect(() => {
    loadAnalyticsData();
  }, []);

  // Filter receipts when category changes
  React.useEffect(() => {
    if (selectedCategory) {
      const filtered = allReceipts.filter(receipt => receipt.categoryId == selectedCategory);
      setFilteredReceipts(filtered);
    } else {
      setFilteredReceipts(allReceipts);
    }
  }, [selectedCategory, allReceipts]);

  const loadAnalyticsData = async () => {
    setLoading(true);
    setError('');
    
    try {
      const [monthlyResponse, currentMonthResponse, totalResponse, receiptsResponse] = await Promise.all([
        api.getMonthlySpending(token),
        api.getCurrentMonthSpending(token),
        api.getTotalSpending(token),
        api.getAllReceipts(token)
      ]);

      if (monthlyResponse.ok) {
        const monthlyData = await monthlyResponse.json();
        setMonthlyData(monthlyData);
      }

      if (currentMonthResponse.ok) {
        const currentMonthData = await currentMonthResponse.json();
        setCurrentMonthData(currentMonthData);
      }

      if (totalResponse.ok) {
        const totalData = await totalResponse.json();
        setTotalSpending(totalData.totalSpending || 0);
      }

      if (receiptsResponse.ok) {
        const receiptsData = await receiptsResponse.json();
        setAllReceipts(receiptsData);
        setFilteredReceipts(receiptsData);
      }
    } catch (err) {
      setError('Failed to load analytics data: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDateRangeChange = async (startDate, endDate) => {
    setLoading(true);
    setError('');
    
    try {
      const response = await api.getSpendingByPeriod(token, startDate, endDate);
      if (response.ok) {
        const data = await response.json();
        setCustomPeriodData(data);
      } else {
        setError('Failed to load period data');
      }
    } catch (err) {
      setError('Failed to load period data: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCategoryChange = (categoryId) => {
    setSelectedCategory(categoryId);
  };

  if (loading) {
    return <div>Loading analytics...</div>;
  }

  return (
    <div style={{ padding: '20px' }}>
      <h2>Analytics Dashboard</h2>
      
      {error && <div style={{ color: 'red', marginBottom: '20px' }}>{error}</div>}
      
      {/* Total Spending Summary */}
      <div style={{ margin: '20px 0', padding: '15px', backgroundColor: '#f5f5f5', borderRadius: '8px' }}>
        <h3>Total Spending: ${parseFloat(totalSpending).toFixed(2)}</h3>
        <p>Total Receipts: {allReceipts.length}</p>
      </div>

      {/* Current Month Spending */}
      {currentMonthData.length > 0 && (
        <div style={{ margin: '20px 0' }}>
          <h3>Current Month Spending by Category</h3>
          <CategorySpendingChart categoryData={currentMonthData} />
        </div>
      )}

      {/* Monthly Trend Chart */}
      <MonthlySpendingChart monthlyData={monthlyData} />

      {/* Category Filter */}
      <CategoryFilter 
        categories={categories}
        selectedCategory={selectedCategory}
        onCategoryChange={handleCategoryChange}
      />

      {/* Custom Date Range */}
      <DateRangePicker onDateRangeChange={handleDateRangeChange} />

      {/* Custom Period Results */}
      {customPeriodData.length > 0 && (
        <div style={{ margin: '20px 0' }}>
          <h3>Custom Period Spending by Category</h3>
          <CategorySpendingChart categoryData={customPeriodData} />
        </div>
      )}

      {/* Filtered Receipts Table */}
      <div style={{ margin: '20px 0' }}>
        <h3>Receipts {selectedCategory ? `(Filtered by Category)` : ''}</h3>
        <div style={{ maxHeight: '300px', overflowY: 'auto', border: '1px solid #ddd' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead style={{ backgroundColor: '#f5f5f5', position: 'sticky', top: 0 }}>
              <tr>
                <th style={{ padding: '8px', border: '1px solid #ddd' }}>Store</th>
                <th style={{ padding: '8px', border: '1px solid #ddd' }}>Date</th>
                <th style={{ padding: '8px', border: '1px solid #ddd' }}>Category</th>
                <th style={{ padding: '8px', border: '1px solid #ddd' }}>Amount</th>
              </tr>
            </thead>
            <tbody>
              {filteredReceipts.map(receipt => (
                <tr key={receipt.receiptId}>
                  <td style={{ padding: '8px', border: '1px solid #ddd' }}>{receipt.storeName}</td>
                  <td style={{ padding: '8px', border: '1px solid #ddd' }}>
                    {new Date(receipt.purchaseDate).toLocaleDateString()}
                  </td>
                  <td style={{ padding: '8px', border: '1px solid #ddd' }}>{receipt.categoryName}</td>
                  <td style={{ padding: '8px', border: '1px solid #ddd' }}>
                    ${parseFloat(receipt.amount).toFixed(2)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

// Dashboard Component (shown after login)
const Dashboard = () => {
  const { logout, token } = React.useContext(AuthContext);
  const [currentView, setCurrentView] = useState('dashboard');
  
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
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h1>Personal CPI Tracker Dashboard</h1>
        <div>
          <button 
            onClick={() => setCurrentView('dashboard')}
            style={{ marginRight: '10px', padding: '8px 16px' }}
          >
            Dashboard
          </button>
          <button 
            onClick={() => setCurrentView('analytics')}
            style={{ marginRight: '10px', padding: '8px 16px' }}
          >
            Analytics
          </button>
          <button onClick={testAuth} style={{ marginRight: '10px', padding: '8px 16px' }}>
            Test Auth
          </button>
          <button onClick={logout} style={{ padding: '8px 16px' }}>
            Logout
          </button>
        </div>
      </div>
      
      {currentView === 'dashboard' && (
        <div>
          <p>Welcome! Your authentication is working.</p>
          
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
      )}
      
      {currentView === 'analytics' && <AnalyticsDashboard />}
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
