import { describe, expect, it } from 'vitest';
import { formatMoney, groupSeatsByRow, toggleSeat, tomorrow } from './utils.js';

describe('utils', () => {
  it('groups seats by row letter in order', () => {
    const rows = groupSeatsByRow([{ seatNumber: 'A1' }, { seatNumber: 'A2' }, { seatNumber: 'B1' }]);
    expect(rows.map(([row, seats]) => [row, seats.length])).toEqual([['A', 2], ['B', 1]]);
  });

  it('formats money in rupees', () => {
    expect(formatMoney(1250)).toBe('₹1,250.00');
    expect(formatMoney('abc')).toBe('');
  });

  it('computes tomorrow across a month end', () => {
    expect(tomorrow(new Date(2026, 0, 31))).toBe('2026-02-01');
  });

  it('toggles seats and respects the limit', () => {
    expect(toggleSeat(['A1'], 'A2')).toEqual(['A1', 'A2']);
    expect(toggleSeat(['A1', 'A2'], 'A1')).toEqual(['A2']);
    expect(toggleSeat(['A1'], 'A2', 1)).toEqual(['A1']);
  });
});
