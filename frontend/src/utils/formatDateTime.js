// Formats a backend date/datetime string for display.
// - Datetime values (e.g. "2026-07-01 09:00:00" or with a "T" separator) are
//   shown as date + hours:minutes, with seconds dropped.
// - Plain date values (e.g. "2026-07-15") are returned unchanged.
export function formatDateTime(value) {
  if (!value) return value;

  const [datePart, timePart] = String(value).split(/[ T]/);
  if (!timePart) {
    return datePart;
  }

  const [hours, minutes] = timePart.split(':');
  return `${datePart} ${hours}:${minutes}`;
}