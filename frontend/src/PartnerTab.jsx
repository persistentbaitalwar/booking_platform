import React, { useEffect, useState } from 'react';
import { api } from './api.js';
import { ErrorBox } from './components.jsx';

/** Small generic form: shows the server result or the error message under the button. */
function ActionForm({ title, description, fields, submitLabel, onSubmit }) {
  const [values, setValues] = useState(() => Object.fromEntries(fields.map((f) => [f.name, f.defaultValue ?? ''])));
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  async function submit(e) {
    e.preventDefault();
    setError(null);
    setResult(null);
    try {
      const response = await onSubmit(values);
      setResult(response === null ? { message: 'Done.' } : response);
    } catch (err) {
      setError(err);
    }
  }

  return (
    <form className="card" onSubmit={submit}>
      <h4>{title}</h4>
      {description && <p className="muted">{description}</p>}
      {fields.map((f) => (
        <label key={f.name}>
          {f.label}
          {f.options ? (
            <select value={values[f.name]} onChange={(e) => setValues({ ...values, [f.name]: e.target.value })}>
              <option value="">Select…</option>
              {f.options.map((o) => (<option key={o.value} value={o.value}>{o.label}</option>))}
            </select>
          ) : f.multiline ? (
            <textarea rows={4} value={values[f.name]} placeholder={f.placeholder}
              onChange={(e) => setValues({ ...values, [f.name]: e.target.value })} />
          ) : (
            <input type={f.type || 'text'} value={values[f.name]} placeholder={f.placeholder}
              onChange={(e) => setValues({ ...values, [f.name]: e.target.value })} />
          )}
        </label>
      ))}
      <button className="primary" type="submit">{submitLabel}</button>
      <ErrorBox error={error} />
      {result && <pre className="result">{JSON.stringify(result, null, 2)}</pre>}
    </form>
  );
}

const opt = (list, label) => list.map((x) => ({ value: x.id, label: label(x) }));

export default function PartnerTab() {
  const [cities, setCities] = useState([]);
  const [movies, setMovies] = useState([]);

  useEffect(() => {
    api.cities().then(setCities).catch(() => {});
    api.movies().then(setMovies).catch(() => {});
  }, []);

  return (
    <section>
      <h2>Theatre partner portal</h2>
      <p className="muted">You can only manage the theatres you onboarded here with your own partner account.</p>
      <div className="grid">
        <ActionForm title="1. Onboard a theatre" submitLabel="Onboard theatre"
          fields={[
            { name: 'name', label: 'Theatre name' },
            { name: 'address', label: 'Address' },
            { name: 'cityId', label: 'City', options: opt(cities, (c) => c.name) },
          ]}
          onSubmit={(v) => api.partner.onboardTheatre({ name: v.name, address: v.address, cityId: Number(v.cityId) || null })} />

        <ActionForm title="2. Add a screen" description="Seats are generated from rows and seats per row (A1, A2, …)." submitLabel="Add screen"
          fields={[
            { name: 'theatreId', label: 'Theatre ID', type: 'number' },
            { name: 'name', label: 'Screen name' },
            { name: 'rows', label: 'Rows (1 to 26)', type: 'number', defaultValue: 5 },
            { name: 'seatsPerRow', label: 'Seats per row (1 to 50)', type: 'number', defaultValue: 10 },
          ]}
          onSubmit={(v) => api.partner.addScreen(v.theatreId, { name: v.name, rows: Number(v.rows), seatsPerRow: Number(v.seatsPerRow) })} />

        <ActionForm title="3. Create a show" submitLabel="Create show"
          fields={[
            { name: 'movieId', label: 'Movie', options: opt(movies, (m) => `${m.title} (${m.language})`) },
            { name: 'screenId', label: 'Screen ID', type: 'number' },
            { name: 'showDate', label: 'Date', type: 'date' },
            { name: 'startTime', label: 'Start time', type: 'time' },
            { name: 'basePrice', label: 'Base ticket price', type: 'number', defaultValue: 250 },
          ]}
          onSubmit={(v) => api.partner.createShow({ movieId: Number(v.movieId) || null, screenId: Number(v.screenId) || null,
            showDate: v.showDate || null, startTime: v.startTime || null, basePrice: Number(v.basePrice) })} />

        <ActionForm title="4. Update a show" description="Leave a field empty to keep its current value. Time cannot change once seats are booked."
          submitLabel="Update show"
          fields={[
            { name: 'showId', label: 'Show ID', type: 'number' },
            { name: 'showDate', label: 'New date', type: 'date' },
            { name: 'startTime', label: 'New start time', type: 'time' },
            { name: 'basePrice', label: 'New base price', type: 'number' },
          ]}
          onSubmit={(v) => api.partner.updateShow(v.showId, { showDate: v.showDate || null, startTime: v.startTime || null,
            basePrice: v.basePrice ? Number(v.basePrice) : null })} />

        <ActionForm title="5. Delete a show" description="Customers with bookings for this show are refunded automatically."
          submitLabel="Delete show"
          fields={[{ name: 'showId', label: 'Show ID', type: 'number' }]}
          onSubmit={(v) => api.partner.deleteShow(v.showId)} />

        <ActionForm title="6. Update seat inventory" description="Block a seat, release it, change its price, or add an extra seat."
          submitLabel="Update seat"
          fields={[
            { name: 'showId', label: 'Show ID', type: 'number' },
            { name: 'seatNumber', label: 'Seat number (for example A1)' },
            { name: 'status', label: 'Status', options: [{ value: 'AVAILABLE', label: 'Available' }, { value: 'BLOCKED', label: 'Blocked' }] },
            { name: 'price', label: 'Price (optional for existing seats)', type: 'number' },
          ]}
          onSubmit={(v) => api.partner.updateSeats(v.showId, [{ seatNumber: v.seatNumber.trim().toUpperCase(),
            status: v.status || null, price: v.price ? Number(v.price) : null }])} />

        <ActionForm title="7. Import shows from your own system"
          description="Paste a JSON list of shows exported from your existing scheduling software."
          submitLabel="Import shows"
          fields={[{ name: 'json', label: 'Shows (JSON)', multiline: true,
            placeholder: '[{"movieId":1,"screenId":1,"showDate":"2026-12-01","startTime":"10:00","basePrice":200}]' }]}
          onSubmit={(v) => api.partner.importShows(JSON.parse(v.json))} />

        <ActionForm title="8. Verify a ticket at the entrance" description="Paste the text read from the customer's QR code."
          submitLabel="Verify ticket"
          fields={[{ name: 'payload', label: 'QR code text' }]}
          onSubmit={(v) => api.partner.verifyTicket(v.payload.trim())} />
      </div>
    </section>
  );
}
