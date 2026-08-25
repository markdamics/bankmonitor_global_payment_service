import { NavLink, Route, Routes } from 'react-router-dom'
import './App.css'
import AccountsPage from './pages/AccountsPage'
import TransferPage from './pages/TransferPage'
import TransactionsPage from './pages/TransactionsPage'

function App() {
  return (
    <div className="app">
      <nav className="app-nav">
        <NavLink to="/accounts">Számlák</NavLink>
        <NavLink to="/transfer">Utalás</NavLink>
        <NavLink to="/transactions">Tranzakciók</NavLink>
      </nav>
      <main className="app-main">
        <Routes>
          <Route path="/" element={<AccountsPage />} />
          <Route path="/accounts" element={<AccountsPage />} />
          <Route path="/transfer" element={<TransferPage />} />
          <Route path="/transactions" element={<TransactionsPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
