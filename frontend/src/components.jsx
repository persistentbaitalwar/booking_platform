import React, { useEffect, useState } from 'react';
import { api } from './api.js';
import { formatMoney, groupSeatsByRow, toggleSeat } from './utils.js';

export function ErrorBox({ error }) {
  if (!error) return null;
  return (
    <div className="error" role="alert">
      <strong>{error.message}</strong>
      {error.details && error.details.length > 0 && (
        <ul>
          {error.details.map((d) => (
            <li key={d}>{d}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

/** Lists the theatres that run a movie in a city on a date and lets the user pick a show. */
export function ShowPicker({ movieId, cityId, date, onPick, selectedShowId }) {
  const [theatres, setTheatres] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!movieId || !cityId || !date) return;
    setLoading(true);
    setError(null);
    api
      .theatres(movieId, cityId, date)
      .then(setTheatres)
      .catch(setError)
      .finally(() => setLoading(false));
  }, [movieId, cityId, date]);

  if (loading) return <p>Loading theatres…</p>;
  return (
    <div>
      <ErrorBox error={error} />
      {!error && theatres.length === 0 && <p className="muted">No theatres are running this movie on the selected date.</p>}
      {theatres.map((t) => (
        <div className="card" key={t.theatreId}>
          <h4>{t.theatreName}</h4>
          <p className="muted">{t.address}</p>
          {t.offers.length > 0 && (
            <p>
              {t.offers.map((o) => (
                <span className="badge" key={o.code}>
                  {o.description}
                </span>
              ))}
            </p>
          )}
          <div className="row">
            {t.shows.map((s) => (
              <button
                key={s.showId}
                className={s.showId === selectedShowId ? 'chip selected' : 'chip'}
                disabled={s.availableSeats === 0}
                onClick={() => onPick(s, t)}
                title={`${s.screenName}, ${s.availableSeats} seats left`}
              >
                {s.startTime.slice(0, 5)} · {formatMoney(s.basePrice)}
                <small>{s.availableSeats === 0 ? 'Sold out' : `${s.availableSeats} left`}</small>
              </button>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

/** Seat map. `selected` keeps the order in which seats were chosen. */
export function SeatPicker({ showId, selected, onChange, limit = 10 }) {
  const [seats, setSeats] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    setError(null);
    api.seats(showId).then(setSeats).catch(setError);
  }, [showId]);

  return (
    <div>
      <ErrorBox error={error} />
      <div className="screen">Screen this way</div>
      {groupSeatsByRow(seats).map(([row, rowSeats]) => (
        <div className="seat-row" key={row}>
          <span className="row-label">{row}</span>
          {rowSeats.map((seat) => {
            const isSelected = selected.includes(seat.seatNumber);
            const available = seat.status === 'AVAILABLE';
            let className = 'seat';
            if (isSelected) className += ' selected';
            else if (!available) className += seat.status === 'BOOKED' ? ' booked' : ' blocked';
            return (
              <button
                key={seat.seatId}
                className={className}
                disabled={!available && !isSelected}
                onClick={() => onChange(toggleSeat(selected, seat.seatNumber, limit))}
                title={`${seat.seatNumber} · ${formatMoney(seat.price)} · ${seat.status.toLowerCase()}`}
                aria-pressed={isSelected}
              >
                {seat.seatNumber.slice(1)}
              </button>
            );
          })}
        </div>
      ))}
      <p className="legend">
        <span className="seat">Free</span> <span className="seat selected">Selected</span>{' '}
        <span className="seat booked">Booked</span> <span className="seat blocked">Not for sale</span>
      </p>
    </div>
  );
}

/** QR code of a confirmed booking (loaded with the sign-in token, so it is fetched instead of linked). */
export function QrImage({ bookingId, small }) {
  const [url, setUrl] = useState(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let objectUrl = null;
    let active = true;
    api
      .qrImageUrl(bookingId)
      .then((u) => {
        objectUrl = u;
        if (active) setUrl(u);
      })
      .catch(() => active && setFailed(true));
    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [bookingId]);

  if (failed) return <p className="muted">The QR code could not be loaded.</p>;
  if (!url) return <p className="muted">Loading QR code…</p>;
  return <img className={small ? 'qr small' : 'qr'} src={url} alt={`QR code for booking ${bookingId}`} />;
}

export function BookingSummary({ booking }) {
  return (
    <div className="card">
      <h4>
        {booking.movieTitle}{' '}
        <span className={`badge ${booking.status === 'CANCELLED' ? 'danger' : booking.status === 'CONFIRMED' ? 'ok' : ''}`}>
          {booking.status === 'PENDING_PAYMENT' ? 'WAITING FOR PAYMENT' : booking.status}
        </span>
      </h4>
      <p>
        {booking.theatreName} · {booking.screenName} · {booking.showDate} at {booking.startTime.slice(0, 5)}
      </p>
      <p>Seats: {booking.seatNumbers.join(', ')}</p>
      <p>
        Price {formatMoney(booking.grossAmount)}, discount {formatMoney(booking.discountAmount)},{' '}
        <strong>total {formatMoney(booking.netAmount)}</strong>
      </p>
      {booking.appliedOffers.length > 0 && <p className="muted">Offers applied: {booking.appliedOffers.join(', ')}</p>}
    </div>
  );
}
