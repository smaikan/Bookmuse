import React from 'react';
import {
  Alert,
  Animated,
  AppState,
  BackHandler,
  Dimensions,
} from 'react-native';
import ReactTestRenderer, { act } from 'react-test-renderer';
import { LibraryScreen } from '../src/reader/LibraryScreen';
import { ReaderScreen } from '../src/reader/ReaderScreen';
import PdfEngine from '../src/specs/NativeKitapPdf';
import { parsePage } from '../src/reader/types';
import { DEFAULT_OPTIONS } from '../src/reader/options';

jest.mock(
  'react-native-safe-area-context',
  () => require('react-native-safe-area-context/jest/mock').default,
);
jest.mock('@react-navigation/native', () => ({
  useFocusEffect: (callback: () => void) =>
    require('react').useEffect(callback, [callback]),
  useIsFocused: () => true,
  usePreventRemove: (_enabled: boolean, callback: unknown) => {
    mockPreventRemove = callback;
  },
}));
jest.mock(
  '../src/specs/KitapGestureHostNativeComponent',
  () => 'KitapGestureHost',
);
jest.mock('../src/specs/KitapPdfViewNativeComponent', () => 'KitapPdfView');
jest.mock('../src/specs/KitapSliderNativeComponent', () => 'KitapSlider');
jest.mock('../src/specs/KitapThumbnailNativeComponent', () => 'KitapThumbnail');
jest.mock('../src/specs/NativeKitapPdf', () => ({
  __esModule: true,
  default: {
    importSharedPdf: jest.fn(),
    listBooks: jest.fn(),
    pickBook: jest.fn(),
    cancelImport: jest.fn(),
    openBook: jest.fn(),
    removeBook: jest.fn(),
    closeSession: jest.fn(),
    saveReaderOptions: jest.fn(),
    setWindowOptions: jest.fn(),
    queryBooks: jest.fn(),
    queryLibrary: jest.fn(),
    getLibraryPreferences: jest.fn(),
    getReaderDefaults: jest.fn(),
    saveReaderDefaults: jest.fn(),
    flushReadingPosition: jest.fn(),
    saveLibraryPreferences: jest.fn(),
    setFavorite: jest.fn(),
    mostRecentBook: jest.fn(),
    getLibraryMode: jest.fn(),
    setLibraryMode: jest.fn(),
    renameBook: jest.fn(),
    listBookmarks: jest.fn(),
    isBookmarked: jest.fn(),
    setBookmark: jest.fn(),
    saveSelection: jest.fn(),
    listAnnotations: jest.fn(),
    changeAnnotation: jest.fn(),
    getAnnotation: jest.fn(),
    newOperationId: jest.fn(),
    copyText: jest.fn(),
    openNoteEditor: jest.fn(),
    writeNoteDraft: jest.fn(),
    saveNote: jest.fn(),
    endNoteEditor: jest.fn(),
    listNotes: jest.fn(),
    changeNoteDeleted: jest.fn(),
    deleteAnnotation: jest.fn(),
    selectionNote: jest.fn(),
  },
}));

let mockPreventRemove: any;
const book = {
  id: 'a',
  title: 'İstanbul',
  pageCount: 80,
  pageIndex: 44,
  sizeBytes: 1024,
  favorite: false,
};
const mark = {
  id: 'mark-a',
  pageIndex: 44,
  quote: 'İstanbul kitap okuma',
  kind: 'highlight',
  color: '#FFEB3B',
  lineCount: 2,
  provenance: 'pdf-text',
  createdAt: 1,
  revision: 1,
  deleted: false,
};
const selected = {
  id: 'selection-a',
  pageIndex: 44,
  quote: 'İstanbul kitap okuma',
  lineCount: 2,
  region: false,
};
function text(tree: ReactTestRenderer.ReactTestRenderer) {
  return tree.root
    .findAll(n => n.props.children !== undefined)
    .map(n =>
      [n.props.children]
        .flat(Infinity)
        .filter(v => typeof v === 'string' || typeof v === 'number')
        .join(''),
    )
    .join(' ');
}
function button(tree: ReactTestRenderer.ReactTestRenderer, label: string) {
  const exact = tree.root.findAll(
    n =>
      n.props.accessibilityLabel === label &&
      typeof n.props.onPress === 'function',
  )[0];
  if (exact) return exact;
  let node: ReactTestRenderer.ReactTestInstance | null =
    tree.root.findAll(n => n.props.children === label)[0] ?? null;
  while (node && typeof node.props.onPress !== 'function') node = node.parent;
  return (node || undefined)!;
}
function pdf(tree: ReactTestRenderer.ReactTestRenderer) {
  return tree.root.findAll(
    n =>
      n.props.sessionId === 'session-a' &&
      n.props.trackReading === true &&
      typeof n.props.onDiagnostic === 'function',
  )[0];
}
async function event(
  tree: ReactTestRenderer.ReactTestRenderer,
  status: string,
  pageIndex = 44,
  extra: Record<string, unknown> = {},
) {
  await act(async () =>
    pdf(tree).props.onDiagnostic({
      nativeEvent: {
        status,
        pageIndex,
        sessionId: 'session-a',
        zoom: 1,
        direction: 0,
        message: '',
        renderMs: 5,
        bitmapBytes: 400,
        ...extra,
      },
    }),
  );
}
async function reader(extra: Record<string, unknown> = {}) {
  const navigation = {
    setOptions: jest.fn(),
    dispatch: jest.fn(),
    popToTop: jest.fn(),
    goBack: jest.fn(),
  };
  let tree!: ReactTestRenderer.ReactTestRenderer;
  await act(async () => {
    tree = ReactTestRenderer.create(
      <ReaderScreen
        {...({
          route: {
            params: {
              book: {
                ...book,
                sessionId: 'session-a',
                options: DEFAULT_OPTIONS,
              },
              ...extra,
            },
          },
          navigation,
        } as any)}
      />,
    );
  });
  return { tree, navigation };
}
async function library() {
  const navigation = { navigate: jest.fn() };
  let tree!: ReactTestRenderer.ReactTestRenderer;
  await act(async () => {
    tree = ReactTestRenderer.create(
      <LibraryScreen {...({ navigation } as any)} />,
    );
  });
  return { tree, navigation };
}
async function tools(tree: ReactTestRenderer.ReactTestRenderer) {
  await event(tree, 'toggleTools');
}
async function menu(tree: ReactTestRenderer.ReactTestRenderer) {
  await act(async () =>
    button(tree, 'İstanbul, kitap menüsü').props.onPress({
      nativeEvent: { pageX: 200, pageY: 200 },
    }),
  );
}
beforeEach(() => {
  Object.defineProperty(AppState, 'currentState', {
    value: 'active',
    configurable: true,
  });
  jest.clearAllMocks();
  Dimensions.set({
    window: { width: 800, height: 1200, scale: 1, fontScale: 1 },
    screen: { width: 800, height: 1200, scale: 1, fontScale: 1 },
  });
  jest.spyOn(Animated, 'timing').mockImplementation((value, config) => ({
    start: callback => {
      if (value instanceof Animated.Value)
        value.setValue(config.toValue as number);
      callback?.({ finished: true });
    },
    stop: () => {},
    reset: () => {},
  }));
  jest.mocked(PdfEngine.importSharedPdf).mockResolvedValue('null');
  jest
    .mocked(PdfEngine.getLibraryPreferences)
    .mockResolvedValue('{"sort":"recent","favoritesOnly":false}');
  jest.mocked(PdfEngine.queryLibrary).mockResolvedValue(JSON.stringify([book]));
  jest.mocked(PdfEngine.mostRecentBook).mockResolvedValue('null');
  jest.mocked(PdfEngine.saveLibraryPreferences).mockResolvedValue();
  jest.mocked(PdfEngine.setFavorite).mockResolvedValue(true);
  jest.mocked(PdfEngine.openBook).mockResolvedValue(
    JSON.stringify({
      ...book,
      sessionId: 'session-a',
      options: DEFAULT_OPTIONS,
    }),
  );
  jest.mocked(PdfEngine.closeSession).mockResolvedValue();
  jest.mocked(PdfEngine.flushReadingPosition).mockResolvedValue();
  jest.mocked(PdfEngine.setWindowOptions).mockResolvedValue();
  jest
    .mocked(PdfEngine.getReaderDefaults)
    .mockResolvedValue(JSON.stringify(DEFAULT_OPTIONS));
  jest
    .mocked(PdfEngine.saveReaderDefaults)
    .mockImplementation(async value => value);
  jest.mocked(PdfEngine.listBookmarks).mockResolvedValue('[]');
  jest.mocked(PdfEngine.isBookmarked).mockResolvedValue(false);
  jest.mocked(PdfEngine.setBookmark).mockResolvedValue(true);
  jest
    .mocked(PdfEngine.listAnnotations)
    .mockResolvedValue(JSON.stringify([mark]));
  jest
    .mocked(PdfEngine.getAnnotation)
    .mockResolvedValue(JSON.stringify({ ...mark, noteCount: 0 }));
  jest.mocked(PdfEngine.listNotes).mockResolvedValue('[]');
  jest.mocked(PdfEngine.newOperationId).mockResolvedValue('operation-a');
  jest.mocked(PdfEngine.saveSelection).mockResolvedValue(JSON.stringify(mark));
  jest.mocked(PdfEngine.copyText).mockResolvedValue();
  jest.mocked(PdfEngine.renameBook).mockResolvedValue();
});
afterEach(() => jest.restoreAllMocks());

test('cover menu preserves the three work actions and appends rename and remove', async () => {
  const { tree } = await library();
  expect(text(tree)).toContain('(45/80)');
  expect(button(tree, 'Liste')).toBeUndefined();
  expect(button(tree, 'İstanbul, kitap menüsü')).toBeDefined();
  await menu(tree);
  expect(button(tree, 'Notlar')).toBeDefined();
  expect(button(tree, 'Altı çizilenler')).toBeDefined();
  expect(button(tree, 'Favoriye ekle')).toBeDefined();
  expect(button(tree, 'Adı düzenle')).toBeDefined();
  expect(button(tree, 'Kaldır')).toBeDefined();
  const labels = tree.root
    .findAll(
      n =>
        (n.type as unknown) === 'View' &&
        n.props.accessibilityRole === 'button',
    )
    .map(n => n.props.accessibilityLabel);
  expect(
    labels.filter(label =>
      [
        'Notlar',
        'Altı çizilenler',
        'Favoriye ekle',
        'Adı düzenle',
        'Kaldır',
      ].includes(label),
    ),
  ).toEqual([
    'Notlar',
    'Altı çizilenler',
    'Favoriye ekle',
    'Adı düzenle',
    'Kaldır',
  ]);
  await act(async () => button(tree, 'Favoriye ekle').props.onPress());
  expect(PdfEngine.setFavorite).toHaveBeenCalledWith('a', true);
  await act(async () => tree.unmount());
});
test('restored position from native is used when opening a book', async () => {
  const { tree, navigation } = await library();
  await act(async () =>
    button(tree, 'İstanbul, 45 / 80. Okumaya devam et').props.onPress(),
  );
  expect(navigation.navigate).toHaveBeenCalledWith('Reader', {
    book: expect.objectContaining({ pageIndex: 44 }),
  });
  await act(async () => tree.unmount());
});
test('favorites and sort are saved before the filtered query', async () => {
  const { tree } = await library();
  await act(async () => button(tree, 'Favoriler').props.onPress());
  expect(PdfEngine.saveLibraryPreferences).toHaveBeenCalledWith('recent', true);
  expect(PdfEngine.queryLibrary).toHaveBeenLastCalledWith(
    '',
    'recent',
    true,
    0,
    40,
  );
  await act(async () => button(tree, 'Sıralama seçenekleri').props.onPress());
  await act(async () => button(tree, 'Kitap adı A–Z').props.onPress());
  expect(PdfEngine.queryLibrary).toHaveBeenLastCalledWith(
    '',
    'title',
    true,
    0,
    40,
  );
  await act(async () => tree.unmount());
});
test('missing PDF still allows reading saved quotes from its card', async () => {
  jest
    .mocked(PdfEngine.openBook)
    .mockRejectedValueOnce(new Error('Yerel kopya kayıp'));
  const { tree } = await library();
  await menu(tree);
  await act(async () => button(tree, 'Altı çizilenler').props.onPress());
  expect(text(tree)).toContain(mark.quote);
  await act(async () => button(tree, 'Konuma git · Sayfa 45').props.onPress());
  expect(text(tree)).toContain('Yerel kopya kayıp');
  await act(async () => tree.unmount());
});
test('rename from the cover menu prefills the title and preserves stable book id', async () => {
  const { tree } = await library();
  await menu(tree);
  await act(async () => button(tree, 'Adı düzenle').props.onPress());
  const input = tree.root.findAll(
    n =>
      n.props.accessibilityLabel === 'Yeni kitap adı' &&
      typeof n.props.onChangeText === 'function',
  )[0];
  expect(input.props.value).toBe('İstanbul');
  await act(async () => input.props.onChangeText('Yeni ad'));
  await act(async () => button(tree, 'Adı kaydet').props.onPress());
  expect(PdfEngine.renameBook).toHaveBeenCalledWith('a', 'Yeni ad');
  await act(async () => tree.unmount());
});
test('library read error can be retried with pull to refresh', async () => {
  jest
    .mocked(PdfEngine.queryLibrary)
    .mockRejectedValueOnce(new Error('Kitaplık okunamadı'));
  const { tree } = await library();
  expect(text(tree)).toContain('Kitaplık okunamadı');
  await act(async () =>
    tree.root
      .findAll(n => typeof n.props.onRefresh === 'function')[0]
      .props.onRefresh(),
  );
  expect(text(tree)).toContain('İstanbul');
  await act(async () => tree.unmount());
});
test('reader starts without visible controls and always requests uncropped fullscreen', async () => {
  const { tree, navigation } = await reader();
  expect(button(tree, 'Sayfaya not ekle')).toBeUndefined();
  expect(navigation.setOptions).toHaveBeenCalledWith(
    expect.objectContaining({ headerShown: false }),
  );
  expect(JSON.parse(pdf(tree).props.readerOptions)).toEqual(
    expect.objectContaining({
      fullscreen: true,
      fitWidth: false,
      left: 0,
      top: 0,
      right: 0,
      bottom: 0,
    }),
  );
  await act(async () => tree.unmount());
});
test('tap reveals only the small work overlay without changing pdf props', async () => {
  const { tree } = await reader();
  await event(tree, 'ready');
  const style = pdf(tree).props.style;
  await tools(tree);
  for (const label of [
    'Sayfaya not ekle',
    'Kitaba not ekle',
    'Metin seç / Altını çiz',
    'Sayfalar',
  ])
    expect(button(tree, label)).toBeDefined();
  for (const label of [
    'Okuma ayarları',
    'Büyüt',
    'Notlar',
    'Altı çizilenler',
    'Orijinal',
  ])
    expect(button(tree, label)).toBeUndefined();
  expect(pdf(tree).props.style).toEqual(style);
  await tools(tree);
  expect(button(tree, 'Sayfaya not ekle')).toBeUndefined();
  await act(async () => tree.unmount());
});
test('starting a reading gesture hides the overlay', async () => {
  const { tree } = await reader();
  await event(tree, 'ready');
  await tools(tree);
  await event(tree, 'interaction');
  expect(button(tree, 'Sayfaya not ekle')).toBeUndefined();
  await act(async () => tree.unmount());
});
test('native navigation ignores stale session events and commits only displayed page', async () => {
  const { tree } = await reader();
  await event(tree, 'ready');
  await event(tree, 'navigate', 45, { sessionId: 'old-session', direction: 1 });
  expect(pdf(tree).props.pageIndex).toBe(44);
  await event(tree, 'navigate', 45, { direction: 1 });
  expect(pdf(tree).props.pageIndex).toBe(45);
  await tools(tree);
  await act(async () => button(tree, 'Sayfalar').props.onPress());
  expect(text(tree)).toContain('45 / 80');
  await event(tree, 'ready', 45);
  expect(text(tree)).toContain('46 / 80');
  await act(async () => tree.unmount());
});
test('page failure offers retry without committing an unshown page', async () => {
  const { tree } = await reader();
  await event(tree, 'ready');
  await event(tree, 'navigate', 45, { direction: 1 });
  await event(tree, 'error', 45, { message: 'Sayfa açılamadı' });
  expect(text(tree)).toContain('Sayfa açılamadı');
  expect(button(tree, 'Sayfayı tekrar aç')).toBeDefined();
  await act(async () => tree.unmount());
});
test('leaving waits for position flush and does not discard a failed commit', async () => {
  const { tree, navigation } = await reader();
  let finish!: () => void;
  jest.mocked(PdfEngine.flushReadingPosition).mockReturnValueOnce(
    new Promise(resolve => {
      finish = resolve;
    }),
  );
  await act(async () =>
    mockPreventRemove({ data: { action: { type: 'GO_BACK' } } }),
  );
  expect(navigation.dispatch).not.toHaveBeenCalled();
  await act(async () => finish());
  expect(PdfEngine.flushReadingPosition).toHaveBeenCalledWith('session-a');
  expect(navigation.dispatch).toHaveBeenCalledWith({ type: 'GO_BACK' });
  await act(async () => tree.unmount());
});
test('failed selection commit keeps quote and retries the same operation id', async () => {
  const { tree } = await reader();
  await event(tree, 'ready');
  await event(tree, 'selection', 44, { message: JSON.stringify(selected) });
  jest
    .mocked(PdfEngine.saveSelection)
    .mockRejectedValueOnce(new Error('disk full'));
  await act(async () => button(tree, 'Sarıyla işaretle').props.onPress());
  expect(text(tree)).toContain(selected.quote);
  await act(async () => button(tree, 'Sarıyla işaretle').props.onPress());
  expect(PdfEngine.newOperationId).toHaveBeenCalledTimes(1);
  expect(jest.mocked(PdfEngine.saveSelection).mock.calls[0]).toEqual(
    jest.mocked(PdfEngine.saveSelection).mock.calls[1],
  );
  await act(async () => tree.unmount());
});
test('duplicate mark presses share one write and stale quotes do not activate selection', async () => {
  const { tree } = await reader();
  await event(tree, 'ready');
  await event(tree, 'selection', 44, {
    sessionId: 'old-session',
    message: JSON.stringify(selected),
  });
  expect(button(tree, 'Sarıyla işaretle')).toBeUndefined();
  await event(tree, 'selection', 44, { message: JSON.stringify(selected) });
  let finish!: (value: string) => void;
  jest.mocked(PdfEngine.saveSelection).mockReturnValueOnce(
    new Promise(resolve => {
      finish = resolve;
    }),
  );
  await act(async () => {
    button(tree, 'Sarıyla işaretle').props.onPress();
    button(tree, 'Sarıyla işaretle').props.onPress();
  });
  expect(PdfEngine.saveSelection).toHaveBeenCalledTimes(1);
  await act(async () => finish(JSON.stringify(mark)));
  await act(async () => tree.unmount());
});
test('page bookmark refers to displayed physical page', async () => {
  const { tree } = await reader();
  await event(tree, 'ready');
  await tools(tree);
  await act(async () => button(tree, 'Sayfalar').props.onPress());
  await act(async () => button(tree, 'Yer imi ekle').props.onPress());
  expect(PdfEngine.setBookmark).toHaveBeenCalledWith('a', 44, true);
  await act(async () => tree.unmount());
});
test('global reader settings are saved without targeting an individual book', async () => {
  const { tree } = await library();
  await act(async () => button(tree, 'Ayarlar').props.onPress());
  expect(text(tree)).toContain('Okuma Görünümü');
  expect(text(tree)).toContain('Ekran');
  for (const label of [
    'Kitapları yönet',
    'Yedekleme ve depolama',
    'Krem',
    'Okuma görünümü ve ekran',
  ])
    expect(button(tree, label)).toBeUndefined();
  await act(async () => button(tree, 'Gece').props.onPress());
  await act(async () => button(tree, 'Ayarları kaydet').props.onPress());
  expect(PdfEngine.saveReaderDefaults).toHaveBeenCalledWith(
    expect.stringContaining('"theme":3'),
  );
  expect(PdfEngine.saveReaderOptions).not.toHaveBeenCalled();
  await act(async () => tree.unmount());
});
test('back closes active selection before navigating', async () => {
  let back: (() => boolean) | undefined;
  const listener = jest
    .spyOn(BackHandler, 'addEventListener')
    .mockImplementation((_event, callback) => {
      back = () => callback({} as never) ?? false;
      return { remove: jest.fn() };
    });
  const { tree } = await reader();
  await event(tree, 'ready');
  await event(tree, 'selection', 44, { message: JSON.stringify(selected) });
  await act(async () => expect(back?.()).toBe(true));
  expect(pdf(tree).props.selectionCommand % 10).toBe(3);
  await act(async () => tree.unmount());
  listener.mockRestore();
});
test('back resets zoom on both panes after removal of the overview helpers', async () => {
  let back: (() => boolean) | undefined;
  jest
    .spyOn(BackHandler, 'addEventListener')
    .mockImplementation((_event, callback) => {
      back = () => callback({} as never) ?? false;
      return { remove: jest.fn() };
    });
  Dimensions.set({
    window: { width: 1200, height: 800, scale: 1, fontScale: 1 },
    screen: { width: 1200, height: 800, scale: 1, fontScale: 1 },
  });
  const { tree } = await reader({
    book: {
      ...book,
      sessionId: 'session-a',
      options: { ...DEFAULT_OPTIONS, doublePage: true },
    },
  });
  await event(tree, 'ready');
  await event(tree, 'zoom', 44, { zoom: 2 });
  await act(async () => expect(back?.()).toBe(true));
  const panes = tree.root.findAll(
    node => (node.type as unknown) === 'KitapPdfView',
  );
  expect(panes).toHaveLength(2);
  expect(panes[0].props.zoomCommand % 10).toBe(3);
  expect(panes[0].props.zoomCommand).toBe(panes[1].props.zoomCommand);
  await act(async () => tree.unmount());
});
test('page parsing rejects fractions, partial input, zero and overflow', () => {
  expect(parsePage(' 45 ', 80)).toBe(44);
  for (const value of [
    '0',
    '81',
    '1e2',
    '42x',
    '1.5',
    '',
    '999999999999999999999',
  ])
    expect(parsePage(value, 80)).toBeNull();
});

test('removal waits for explicit confirmation and targets only the chosen book', async () => {
  const alert = jest.spyOn(Alert, 'alert').mockImplementation(() => {});
  jest.mocked(PdfEngine.removeBook).mockResolvedValue();
  const { tree } = await library();
  await menu(tree);
  await act(async () => button(tree, 'Kaldır').props.onPress());
  expect(PdfEngine.removeBook).not.toHaveBeenCalled();
  const choices = alert.mock.calls[0][2]!;
  expect(choices[0].style).toBe('cancel');
  expect(choices[1].style).toBe('destructive');
  await act(async () => choices[0].onPress?.());
  expect(PdfEngine.removeBook).not.toHaveBeenCalled();
  await menu(tree);
  await act(async () => button(tree, 'Kaldır').props.onPress());
  await act(async () => alert.mock.calls[1][2]![1].onPress?.());
  expect(PdfEngine.removeBook).toHaveBeenCalledTimes(1);
  expect(PdfEngine.removeBook).toHaveBeenCalledWith('a');
  await act(async () => tree.unmount());
});

test('failed rename stays in its editor with the existing title available for retry', async () => {
  jest
    .mocked(PdfEngine.renameBook)
    .mockRejectedValueOnce(new Error('disk full'));
  const { tree } = await library();
  await menu(tree);
  await act(async () => button(tree, 'Adı düzenle').props.onPress());
  await act(async () => button(tree, 'Adı kaydet').props.onPress());
  expect(text(tree)).toContain('disk full');
  expect(button(tree, 'Adı kaydet')).toBeDefined();
  await act(async () => button(tree, 'Adı kaydet').props.onPress());
  expect(PdfEngine.renameBook).toHaveBeenLastCalledWith('a', 'İstanbul');
  await act(async () => tree.unmount());
});
