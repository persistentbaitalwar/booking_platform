import React, { useEffect, useState } from 'react';
import { api, getSession, setSession } from './api.js';
import BookTab from './BookTab.jsx';
import MyBookingsTab from './MyBookingsTab.jsx';
import PartnerTab from './PartnerTab.jsx';
import { ErrorBox } from './components.jsx';

/** Sign in, create a customer account, or sign up as a theatre partner. */
function AuthPanel({ onDone }) {
  const [mode, setMode] = useState('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      let result;
      if (mode === 'login') result = await api.login(email, password);
      else if (mode === 'customer') result = await api.registerCustomer(name, email, password);
      else result = await api.registerPartner(name, email, password);
      onDone(result);
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="card auth" onSubmit={submit}>
      <div className="row">
        <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>Sign in</button>
        <button type="button" className={mode === 'customer' ? 'active' : ''} onClick={() => setMode('customer')}>Create customer account</button>
        <button type="button" className={mode === 'partner' ? 'active' : ''} onClick={() => setMode('partner')}>Theatre partner sign-up</button>
      </div>
      {mode !== 'login' && (
        <label>Name<input value={name} onChange={(e) => setName(e.target.value)} autoComplete="name" /></label>
      )}
      <label>Email address<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="username" /></label>
      <label>
        Password
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
          autoComplete={mode === 'login' ? 'current-password' : 'new-password'} />
      </label>
      {mode !== 'login' && <p className="muted">Use 8 or more characters with at least one letter and one digit.</p>}
      <button className="primary" type="submit" disabled={busy}>
        {mode === 'login' ? 'Sign in' : 'Create account'}
      </button>
      <ErrorBox error={error} />
      <p className="muted">Local demo accounts: demo@example.com / Demo@12345 (customer), partner@example.com / Partner@12345 (theatre partner).</p>
    </form>
  );
}

export default function App() {
  const [session, setSessionState] = useState(getSession());
  const [tab, setTab] = useState('book');
  const [showAuth, setShowAuth] = useState(false);
  const role = session?.role;

  function signedIn(result) {
    const next = { token: result.token, role: result.role, customerId: result.customerId, name: result.displayName };
    setSession(next);
    setSessionState(next);
    setShowAuth(false);
    setTab(next.role === 'PARTNER' ? 'partner' : 'book');
  }

  function signOut() {
    setSession(null);
    setSessionState(null);
    setTab('book');
  }

  useEffect(() => {
    const expired = () => {
      setSessionState(null);
      setShowAuth(true);
    };
    window.addEventListener('session-expired', expired);
    return () => window.removeEventListener('session-expired', expired);
  }, []);

  const canBook = !role || role === 'CUSTOMER' || role === 'ADMIN';
  const isPartner = role === 'PARTNER' || role === 'ADMIN';

  return (
    <div className="app">
      <header>
        <h1>🎬 Movie Ticket Booking</h1>
        <nav>
          {canBook && <button className={tab === 'book' ? 'active' : ''} onClick={() => setTab('book')}>Book tickets</button>}
          {role === 'CUSTOMER' && <button className={tab === 'mine' ? 'active' : ''} onClick={() => setTab('mine')}>My bookings</button>}
          {isPartner && <button className={tab === 'partner' ? 'active' : ''} onClick={() => setTab('partner')}>Theatre partner</button>}
        </nav>
      </header>
      <div className="customer-bar">
        {session ? (
          <>
            Signed in as {session.name} ({session.role.toLowerCase()}) <button onClick={signOut}>Sign out</button>
          </>
        ) : (
          <button onClick={() => setShowAuth(!showAuth)}>Sign in or create an account</button>
        )}
      </div>
      {showAuth && !session && <AuthPanel onDone={signedIn} />}
      <main>
        {tab === 'book' && canBook && <BookTab customerId={session?.customerId ?? ''} onNeedSignIn={() => setShowAuth(true)} />}
        {tab === 'mine' && role === 'CUSTOMER' && <MyBookingsTab customerId={session.customerId} />}
        {tab === 'partner' && isPartner && <PartnerTab />}
      </main>
    </div>
  );
}
