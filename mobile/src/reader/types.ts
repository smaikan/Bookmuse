export type Book = {
  id: string;
  title: string;
  pageCount: number;
  pageIndex: number;
  sizeBytes: number;
  favorite?: boolean;
};
export type OpenBook = Book & {
  sessionId: string;
  options?: import('./options').ReaderOptions;
};
export type Routes = {
  Library: undefined;
  Reader: {
    book: OpenBook;
    annotation?: { id: string; pageIndex: number };
    notePage?: number;
  };
};

export function parsePage(value: string, pageCount: number): number | null {
  const trimmed = value.trim();
  if (!/^[0-9]+$/.test(trimmed)) {
    return null;
  }
  const number = Number(trimmed);
  return Number.isSafeInteger(number) && number >= 1 && number <= pageCount
    ? number - 1
    : null;
}

export function errorMessage(error: unknown): string {
  return error instanceof Error
    ? error.message
    : 'İşlem tamamlanamadı. Tekrar deneyin.';
}
