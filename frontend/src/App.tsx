import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import AccountOverview from './pages/AccountOverview';
import TransactionHistory from './pages/TransactionHistory';
import CreditLimit from './pages/CreditLimit';
import BillDisplay from './pages/BillDisplay';

// Demo account ID - in production, this would come from auth
const DEMO_ACCOUNT_ID = 'demo-account-001';

function App() {
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route path="/" element={<Navigate to="/account" replace />} />
          <Route path="/account" element={<AccountOverview accountId={DEMO_ACCOUNT_ID} />} />
          <Route path="/transactions" element={<TransactionHistory accountId={DEMO_ACCOUNT_ID} />} />
          <Route path="/credit-limit" element={<CreditLimit accountId={DEMO_ACCOUNT_ID} />} />
          <Route path="/bills" element={<BillDisplay accountId={DEMO_ACCOUNT_ID} />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  );
}

export default App;
