import { Navigate, NavLink, Route, Routes } from 'react-router-dom'
import AccountsPage from './pages/AccountsPage'
import TransferPage from './pages/TransferPage'
import TransactionsPage from './pages/TransactionsPage'
import './App.css'

function App() {
  return (
    <div className="app">
      <nav className="app-nav">
        <NavLink to="/accounts" className={({ isActive }) => (isActive ? 'active' : undefined)}>
          Számlák
        </NavLink>
        <NavLink to="/transfer" className={({ isActive }) => (isActive ? 'active' : undefined)}>
          Utalás
        </NavLink>
        <NavLink to="/transactions" className={({ isActive }) => (isActive ? 'active' : undefined)}>
          Tranzakciók
        </NavLink>
      </nav>
      <main className="app-main">
        <Routes>
          <Route path="/" element={<Navigate to="/accounts" replace />} />
          <Route path="/accounts" element={<AccountsPage />} />
          <Route path="/transfer" element={<TransferPage />} />
          <Route path="/transactions" element={<TransactionsPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
