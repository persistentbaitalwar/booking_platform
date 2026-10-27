import React, { useCallback, useEffect, useState } from 'react';
import { api } from './api.js';
import { BookingSummary, ErrorBox, QrImage, SeatPicker, ShowPicker } from './components.jsx';
import { tomorrow } from './utils.js';

function ReschedulePanel({ booking, onDone }) {
  const [date, setDate] = useState(tomorrow());
  const [show, setShow] = useState(null);
  const [selected, setSelected] = useState([]);
  const [error, setError] = useState(null);

  async function submit() {
    try {
      await api.reschedule(booking.bookingId, show.showId, selected);
      onDone();
    } catch (e) {
      setError(e);
    }
  }

  return (
    <div className="card">
      <h4>Reschedule to another show of {booking.movieTitle}</h4>
      <label>
        New date
        <input type="date" value={date} onChange={(e) => { setDate(e.target.value); setShow(null); }} />
      </label>
      <ShowPicker movieId={booking.movieId} cityId={booking.cityId} date={date} selectedShowId={show?.showId}
        onPick={(s) => { setShow(s); setSelected([]); }} />
      {show && (
        <>
          <p>Pick exactly {booking.seatNumbers.length} seat(s).</p>
          <SeatPicker showId={show.showId} selected={selected} onChange={setSelected} limit={booking.seatNumbers.length} />
        </>
      )}
      <ErrorBox error={error} />
      <button className="primary" disabled={!show || selected.length !== booking.seatNumbers.length} onClick={submit}>
        Confirm new show
      </button>
    </div>
  );
}

export default function MyBookingsTab({ customerId }) {
  const [bookings, setBookings] = useState([]);
  const [error, setError] = useState(null);
  const [rescheduling, setRescheduling] = useState(null);
  const [notifications, setNotifications] = useState([]);

  const load = useCallback(() => {
    if (!customerId) return;
    api.myBookings(customerId).then(setBookings).catch(setError);
    api.notifications(customerId).then(setNotifications).catch(() => setNotifications([]));
  }, [customerId]);

  useEffect(load, [load]);

  async function cancel(id) {
    if (!window.confirm('Cancel this booking? The amount will be refunded.')) return;
    setError(null);
    try {
      await api.cancel(id);
      load();
    } catch (e) {
      setError(e);
    }
  }

  async function pay(id) {
    setError(null);
    try {
      await api.pay(id);
      load();
    } catch (e) {
      setError(e);
    }
  }

  if (!customerId) return <p>Please sign in with a customer account to see your bookings.</p>;
  return (
    <section>
      <h2>My bookings</h2>
      <ErrorBox error={error} />
      {notifications.length > 0 && (
        <div className="card">
          <h4>Latest messages</h4>
          <ul>
            {notifications.slice(0, 5).map((n) => (
              <li key={n.id}>{n.message}</li>
            ))}
          </ul>
          <button onClick={load}>Refresh messages</button>
        </div>
      )}
      {bookings.length === 0 && <p className="muted">You have no bookings yet.</p>}
      {bookings.map((b) => (
        <div key={b.bookingId}>
          <BookingSummary booking={b} />
          {b.status === 'PENDING_PAYMENT' && (
            <div className="row">
              <button className="primary" onClick={() => pay(b.bookingId)}>Pay now (demo payment gateway)</button>
              <button onClick={() => cancel(b.bookingId)}>Release seats</button>
            </div>
          )}
          {b.status === 'CONFIRMED' && (
            <div className="row">
              <QrImage bookingId={b.bookingId} small />
              <button onClick={() => cancel(b.bookingId)}>Cancel booking</button>
              <button onClick={() => setRescheduling(rescheduling === b.bookingId ? null : b.bookingId)}>Reschedule</button>
            </div>
          )}
          {rescheduling === b.bookingId && (
            <ReschedulePanel booking={b} onDone={() => { setRescheduling(null); load(); }} />
          )}
        </div>
      ))}
    </section>
  );
}
