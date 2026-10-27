/** Groups seats such as A1, A2, B1 into rows keyed by their letter, keeping the order of the input. */
export function groupSeatsByRow(seats) {
  const rows = new Map();
  seats.forEach((seat) => {
    const row = seat.seatNumber.charAt(0);
    if (!rows.has(row)) {
      rows.set(row, []);
    }
    rows.get(row).push(seat);
  });
  return Array.from(rows.entries());
}

/** Formats an amount for display, for example 1250 becomes "₹1,250.00". */
export function formatMoney(amount) {
  const value = Number(amount);
  if (Number.isNaN(value)) {
    return '';
  }
  return `₹${value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

/** Returns tomorrow as yyyy-mm-dd in the local time zone. */
export function tomorrow(now = new Date()) {
  const d = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${month}-${day}`;
}

/** Removes a seat from the selection if present, otherwise appends it (selection order matters for offers). */
export function toggleSeat(selected, seatNumber, limit = 10) {
  if (selected.includes(seatNumber)) {
    return selected.filter((s) => s !== seatNumber);
  }
  return selected.length >= limit ? selected : [...selected, seatNumber];
}
