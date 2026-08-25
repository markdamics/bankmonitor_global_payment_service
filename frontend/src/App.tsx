import { Navigate, NavLink, Route, Routes } from 'react-router-dom'
import AccountsPage from './pages/AccountsPage'
import './App.css'

function App() {
  return (
    <div className="app">
      <nav className="app-nav">
        <NavLink to="/accounts" className={({ isActive }) => (isActive ? 'active' : undefined)}>
          Számlák
        </NavLink>
      </nav>
      <main className="app-main">
        <Routes>
          <Route path="/" element={<Navigate to="/accounts" replace />} />
          <Route path="/accounts" element={<AccountsPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
