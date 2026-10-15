export function spreadPages(
  active: number,
  count: number,
  coverSingle: boolean,
): number[] {
  if (coverSingle && active === 0) {
    return [0];
  }
  const first = coverSingle
    ? 1 + Math.floor((active - 1) / 2) * 2
    : Math.floor(active / 2) * 2;
  return first + 1 < count ? [first, first + 1] : [first];
}
export function adjacentSpread(
  active: number,
  count: number,
  coverSingle: boolean,
  direction: number,
): number {
  const pages = spreadPages(active, count, coverSingle);
  return direction > 0
    ? Math.min(count - 1, pages[pages.length - 1] + 1)
    : Math.max(0, pages[0] - 2);
}
