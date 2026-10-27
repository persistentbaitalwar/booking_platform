const BASE = '/api/v1';

// The signed-in user lives in sessionStorage: it disappears when the browser tab is closed.
let session = null;
try {
  session = JSON.parse(sessionStorage.getItem('session') || 'null');
} catch (ignored) {
  session = null;
}

export function getSession() {
  return session;
}

export function setSession(next) {
  session = next;
  if (next) sessionStorage.setItem('session', JSON.stringify(next));
  else sessionStorage.removeItem('session');
}

export class ApiError extends Error {
  constructor(payload, status) {
    super(payload?.message || 'Something went wrong. Please try again.');
    this.code = payload?.code;
    this.details = payload?.details || [];
    this.status = status;
  }
}

async function request(path, { method = 'GET', body, headers = {} } = {}) {
  let response;
  try {
    response = await fetch(`${BASE}${path}`, {
      method,
      headers: {
        ...(body ? { 'Content-Type': 'application/json' } : {}),
        ...(session ? { Authorization: `Bearer ${session.token}` } : {}),
        ...headers,
      },
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch (networkError) {
    throw new ApiError({ message: 'The server cannot be reached. Please check your connection and try again.' }, 0);
  }
  if (response.status === 204) {
    return null;
  }
  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;
  if (!response.ok) {
    if (response.status === 401 && session) {
      setSession(null);
      window.dispatchEvent(new Event('session-expired'));
    }
    throw new ApiError(payload, response.status);
  }
  return payload;
}

/** Downloads a protected image (the QR code) and returns a temporary URL for an img tag. */
async function imageUrl(path) {
  const response = await fetch(`${BASE}${path}`, { headers: session ? { Authorization: `Bearer ${session.token}` } : {} });
  if (!response.ok) {
    throw new ApiError(await response.json().catch(() => null), response.status);
  }
  return URL.createObjectURL(await response.blob());
}

export const api = {
  cities: () => request('/cities'),
  movies: () => request('/movies'),
  moviesInCity: (cityId, language, genre) => {
    const params = new URLSearchParams();
    if (language) params.set('language', language);
    if (genre) params.set('genre', genre);
    const query = params.toString();
    return request(`/cities/${cityId}/movies${query ? `?${query}` : ''}`);
  },
  theatres: (movieId, cityId, date) => request(`/movies/${movieId}/theatres?cityId=${cityId}&date=${date}`),
  seats: (showId) => request(`/shows/${showId}/seats`),
  quote: (showId, seats) => request(`/shows/${showId}/quote?seats=${encodeURIComponent(seats.join(','))}`),
  login: (email, password) => request('/auth/login', { method: 'POST', body: { email, password } }),
  registerCustomer: (name, email, password) =>
    request('/auth/register', { method: 'POST', body: { name, email, password } }),
  registerPartner: (name, email, password) =>
    request('/auth/register-partner', { method: 'POST', body: { name, email, password } }),
  pay: (bookingId) => request(`/payments/simulator/${bookingId}/pay`, { method: 'POST' }),
  book: (customerId, showId, seatNumbers, idempotencyKey) =>
    request('/bookings', {
      method: 'POST',
      body: { customerId, showId, seatNumbers },
      headers: { 'Idempotency-Key': idempotencyKey },
    }),
  myBookings: (customerId) => request(`/customers/${customerId}/bookings`),
  cancel: (bookingId) => request(`/bookings/${bookingId}`, { method: 'DELETE' }),
  reschedule: (bookingId, newShowId, seatNumbers) =>
    request(`/bookings/${bookingId}/reschedule`, { method: 'PUT', body: { newShowId, seatNumbers } }),
  notifications: (customerId) => request(`/customers/${customerId}/notifications`),
  qrImageUrl: (bookingId) => imageUrl(`/bookings/${bookingId}/qr`),
  partner: {
    onboardTheatre: (body) => request('/partner/theatres', { method: 'POST', body }),
    addScreen: (theatreId, body) => request(`/partner/theatres/${theatreId}/screens`, { method: 'POST', body }),
    createShow: (body) => request('/partner/shows', { method: 'POST', body }),
    updateShow: (showId, body) => request(`/partner/shows/${showId}`, { method: 'PUT', body }),
    deleteShow: (showId) => request(`/partner/shows/${showId}`, { method: 'DELETE' }),
    updateSeats: (showId, seats) => request(`/partner/shows/${showId}/seats`, { method: 'PUT', body: { seats } }),
    importShows: (shows) => request('/partner/shows/import', { method: 'POST', body: { shows } }),
    verifyTicket: (payload) => request(`/partner/tickets/verify?payload=${encodeURIComponent(payload)}`),
  },
};
