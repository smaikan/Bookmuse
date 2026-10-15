import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  openSample(): Promise<string>;
  close(): Promise<void>;
  runLifecycleProbe(): Promise<string>;
  listBooks(offset: number, limit: number): Promise<string>;
  pickBook(relinkId: string): Promise<string>;
  cancelImport(): Promise<boolean>;
  importSharedPdf(): Promise<string>;
  openBook(bookId: string): Promise<string>;
  removeBook(bookId: string): Promise<void>;
  closeSession(sessionId: string): Promise<void>;
  saveReaderOptions(bookId: string, options: string): Promise<string>;
  setWindowOptions(options: string): Promise<void>;
  queryBooks(
    query: string,
    sort: string,
    offset: number,
    limit: number,
  ): Promise<string>;
  mostRecentBook(): Promise<string>;
  queryLibrary(
    query: string,
    sort: string,
    favoritesOnly: boolean,
    offset: number,
    limit: number,
  ): Promise<string>;
  setFavorite(bookId: string, enabled: boolean): Promise<boolean>;
  getLibraryPreferences(): Promise<string>;
  getReaderDefaults(): Promise<string>;
  saveReaderDefaults(options: string): Promise<string>;
  flushReadingPosition(sessionId: string): Promise<void>;
  saveLibraryPreferences(sort: string, favoritesOnly: boolean): Promise<void>;
  renameBook(bookId: string, title: string): Promise<void>;
  getLibraryMode(): Promise<string>;
  setLibraryMode(mode: string): Promise<void>;
  isBookmarked(bookId: string, page: number): Promise<boolean>;
  setBookmark(bookId: string, page: number, enabled: boolean): Promise<boolean>;
  listBookmarks(bookId: string, offset: number, limit: number): Promise<string>;
  saveSelection(
    sessionId: string,
    selectionId: string,
    kind: string,
    color: string,
    operationId: string,
    manualText: string,
  ): Promise<string>;
  listAnnotations(
    bookId: string,
    query: string,
    kind: string,
    color: string,
    sort: string,
    offset: number,
    limit: number,
  ): Promise<string>;
  changeAnnotation(
    bookId: string,
    id: string,
    revision: number,
    kind: string,
    color: string,
    deleted: boolean,
  ): Promise<string>;
  copyText(text: string): Promise<void>;
  newOperationId(): Promise<string>;
  getAnnotation(bookId: string, id: string): Promise<string>;
  openNoteEditor(
    bookId: string,
    id: string,
    scope: string,
    page: number,
    annotationId: string,
  ): Promise<string>;
  writeNoteDraft(
    bookId: string,
    id: string,
    editorToken: string,
    revision: number,
    title: string,
    body: string,
  ): Promise<string>;
  saveNote(
    bookId: string,
    id: string,
    editorToken: string,
    revision: number,
    title: string,
    body: string,
  ): Promise<string>;
  endNoteEditor(
    bookId: string,
    id: string,
    editorToken: string,
    revision: number,
    title: string,
    body: string,
  ): Promise<void>;
  listNotes(
    bookId: string,
    query: string,
    scope: string,
    sort: string,
    offset: number,
    limit: number,
  ): Promise<string>;
  changeNoteDeleted(
    bookId: string,
    id: string,
    revision: number,
    draftOnly: boolean,
    deleted: boolean,
  ): Promise<string>;
  deleteAnnotation(
    bookId: string,
    id: string,
    revision: number,
    deleteNotes: boolean,
  ): Promise<string>;
  selectionNote(
    sessionId: string,
    selectionId: string,
    operationId: string,
    color: string,
    manualText: string,
  ): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('KitapPdf');
