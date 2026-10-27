import React, { useEffect, useRef, useState } from 'react';
import { api } from './api.js';
import { BookingSummary, ErrorBox, QrImage, SeatPicker, ShowPicker } from './components.jsx';
import { formatMoney, tomorrow } from './utils.js';

export default function BookTab({ customerId, onNeedSignIn }) {
  const [cities, setCities] = useState([]);
  const [cityId, setCityId] = useState('');
  const [date, setDate] = useState(tomorrow());
  const [language, setLanguage] = useState('');
  const [genre, setGenre] = useState('');
  const [allMovies, setAllMovies] = useState([]);
  const [movies, setMovies] = useState([]);
  const [movie, setMovie] = useState(null);
  const [show, setShow] = useState(null);
  const [selected, setSelected] = useState([]);
  const [quote, setQuote] = useState(null);
  const [booking, setBooking] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [seatRefresh, setSeatRefresh] = useState(0);
  const attemptKey = useRef(null);

  useEffect(() => {
    api.cities().then((list) => {
      setCities(list);
      if (list.length > 0) setCityId(String(list[0].id));
    }).catch(setError);
  }, []);

  // all movies of the city (to build the filter lists), then the filtered list
  useEffect(() => {
    if (!cityId) return;
    api.moviesInCity(cityId).then(setAllMovies).catch(setError);
  }, [cityId]);

  useEffect(() => {
    if (!cityId) return;
    setMovie(null);
    setShow(null);
    api.moviesInCity(cityId, language, genre).then(setMovies).catch(setError);
  }, [cityId, language, genre]);

  useEffect(() => {
    setQuote(null);
    attemptKey.current = null;
    if (!show || selected.length === 0) return;
    api.quote(show.showId, selected).then(setQuote).catch(setError);
  }, [show, selected]);

  const languages = [...new Set(allMovies.map((m) => m.language))].sort();
  const genres = [...new Set(allMovies.map((m) => m.genre))].sort();

  async function confirmBooking() {
    if (!customerId) {
      setError({ message: 'Please sign in with a customer account before booking.' });
      onNeedSignIn();
      return;
    }
    setBusy(true);
    setError(null);
    // The same key is reused if the user clicks again, so a slow network can never create two bookings.
    attemptKey.current = attemptKey.current || crypto.randomUUID();
    try {
      const result = await api.book(Number(customerId), show.showId, selected, attemptKey.current);
      setBooking(result);
      setSelected([]);
      setShow(null);
    } catch (e) {
      setError(e);
      // reload the seat map so seats taken by someone else show up as booked
      setSeatRefresh((n) => n + 1);
      setSelected([]);
    } finally {
      setBusy(false);
    }
  }

  async function payNow() {
    setBusy(true);
    setError(null);
    try {
      setBooking(await api.pay(booking.bookingId));
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }

  if (booking) {
    const waiting = booking.status === 'PENDING_PAYMENT';
    return (
      <section>
        <h2>{waiting ? 'Complete your payment' : 'Booking confirmed'}</h2>
        <BookingSummary booking={booking} />
        <ErrorBox error={error} />
        {waiting ? (
          <div className="card">
            <p>
              Your seats are held until {new Date(booking.holdExpiresAt).toLocaleTimeString()}. If payment is not
              completed by then, the seats are released and nothing is charged.
            </p>
            <button className="primary" disabled={busy} onClick={payNow}>
              {busy ? 'Paying…' : 'Pay now (demo payment gateway)'}
            </button>
          </div>
        ) : (
          <div className="card">
            <p>Show this QR code at the theatre entrance.</p>
            <QrImage bookingId={booking.bookingId} />
          </div>
        )}
        <button onClick={() => setBooking(null)}>Book more tickets</button>
      </section>
    );
  }

  return (
    <section>
      <h2>Find a movie</h2>
      <div className="row filters">
        <label>
          City
          <select value={cityId} onChange={(e) => setCityId(e.target.value)}>
            {cities.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </label>
        <label>
          Date
          <input type="date" value={date} onChange={(e) => { setDate(e.target.value); setShow(null); }} />
        </label>
        <label>
          Language
          <select value={language} onChange={(e) => setLanguage(e.target.value)}>
            <option value="">All languages</option>
            {languages.map((l) => (<option key={l}>{l}</option>))}
          </select>
        </label>
        <label>
          Genre
          <select value={genre} onChange={(e) => setGenre(e.target.value)}>
            <option value="">All genres</option>
            {genres.map((g) => (<option key={g}>{g}</option>))}
          </select>
        </label>
      </div>
      <ErrorBox error={error} />

      <div className="row">
        {movies.map((m) => (
          <button key={m.id} className={movie?.id === m.id ? 'card pick selected' : 'card pick'} onClick={() => { setMovie(m); setShow(null); setSelected([]); }}>
            <strong>{m.title}</strong>
            <small>{m.language} · {m.genre} · {m.durationMinutes} min</small>
          </button>
        ))}
        {movies.length === 0 && <p className="muted">No movies match your filters in this city.</p>}
      </div>

      {movie && (
        <>
          <h3>Theatres and show times for {movie.title}</h3>
          <ShowPicker movieId={movie.id} cityId={cityId} date={date} selectedShowId={show?.showId}
            onPick={(s) => { setShow(s); setSelected([]); }} />
        </>
      )}

      {show && (
        <>
          <h3>Choose your seats</h3>
          <p className="muted">Offers: the third seat you pick gets 50% off, and afternoon shows get 20% off, where the theatre offers them.</p>
          <SeatPicker key={`${show.showId}-${seatRefresh}`} showId={show.showId} selected={selected} onChange={setSelected} />
          {quote && (
            <div className="card">
              <p>Seats: {selected.join(', ')}</p>
              <p>Price {formatMoney(quote.gross)}, discount {formatMoney(quote.discount)}</p>
              <p><strong>You pay {formatMoney(quote.net)}</strong></p>
              <button className="primary" disabled={busy} onClick={confirmBooking}>
                {busy ? 'Booking…' : 'Confirm booking'}
              </button>
            </div>
          )}
        </>
      )}
    </section>
  );
}
