import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import './Layout.css';

interface LayoutProps {
  children: React.ReactNode;
}

const navItems = [
  { path: '/account', label: '账户概览', icon: '📊' },
  { path: '/transactions', label: '交易记录', icon: '💳' },
  { path: '/credit-limit', label: '额度查看', icon: '💰' },
  { path: '/bills', label: '账单展示', icon: '📋' },
];

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const location = useLocation();

  return (
    <div className="layout">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="sidebar-header">
          <h1 className="sidebar-logo">💳 信用卡系统</h1>
        </div>
        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className={`nav-item ${location.pathname === item.path ? 'active' : ''}`}
            >
              <span className="nav-icon">{item.icon}</span>
              <span className="nav-label">{item.label}</span>
            </Link>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="user-info">
            <div className="user-avatar">👤</div>
            <div className="user-details">
              <div className="user-name">演示用户</div>
              <div className="user-role">信用卡持卡人</div>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="main-content">
        <header className="main-header">
          <div className="header-title">
            <h2>{navItems.find((item) => item.path === location.pathname)?.label}</h2>
          </div>
          <div className="header-actions">
            <button className="btn btn-secondary btn-sm">
              <span>🔔</span>
              <span className="notification-badge">3</span>
            </button>
            <button className="btn btn-secondary btn-sm">帮助</button>
          </div>
        </header>
        <div className="content-area">
          {children}
        </div>
      </main>

      {/* Mobile Menu Toggle */}
      <button className="mobile-menu-toggle" aria-label="菜单">
        <span></span>
        <span></span>
        <span></span>
      </button>
    </div>
  );
};

export default Layout;
