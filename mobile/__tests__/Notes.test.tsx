import React from 'react';
import { AppState, Keyboard } from 'react-native';
import type { AppStateStatus } from 'react-native';
import Renderer, { act } from 'react-test-renderer';
import { NoteEditor } from '../src/reader/NoteEditor';
import type { NoteDraft } from '../src/reader/NoteEditor';
import { NoteList } from '../src/reader/Notes';
import PdfEngine from '../src/specs/NativeKitapPdf';

jest.mock(
  'react-native-safe-area-context',
  () => require('react-native-safe-area-context/jest/mock').default,
);
jest.mock('../src/specs/NativeKitapPdf', () => ({
  __esModule: true,
  default: {
    writeNoteDraft: jest.fn(),
    saveNote: jest.fn(),
    endNoteEditor: jest.fn(),
    openNoteEditor: jest.fn(),
    listNotes: jest.fn(),
    changeNoteDeleted: jest.fn(),
  },
}));
jest.setTimeout(15000);
const initial: NoteDraft = {
  id: '575c50ab-b6ee-4888-8d5b-4f1a3c3aa93f',
  scope: 'page',
  pageIndex: 41,
  annotationId: '',
  quote: '',
  title: '',
  body: '',
  editorToken: 'editor-token',
  contentRevision: 0,
  baseRevision: 0,
  dirty: false,
  recovered: false,
};
function button(tree: Renderer.ReactTestRenderer, label: string) {
  return tree.root.findAll(
    node =>
      node.props.accessibilityLabel === label &&
      typeof node.props.onPress === 'function',
  )[0];
}
function input(tree: Renderer.ReactTestRenderer, label = 'Not metni') {
  return tree.root.findAll(
    node =>
      node.props.accessibilityLabel === label &&
      typeof node.props.onChangeText === 'function',
  )[0];
}
function text(tree: Renderer.ReactTestRenderer) {
  return tree.root
    .findAll(node => typeof node.props.children === 'string')
    .map(node => node.props.children)
    .join(' ');
}
async function editor(value: Partial<NoteDraft> = {}) {
  const closed = jest.fn();
  let tree!: Renderer.ReactTestRenderer;
  await act(async () => {
    tree = Renderer.create(
      <NoteEditor
        bookId="a"
        initial={{ ...initial, ...value }}
        closed={closed}
      />,
    );
  });
  return { tree, closed };
}
beforeEach(() => {
  jest.useFakeTimers();
  jest
    .mocked(PdfEngine.writeNoteDraft)
    .mockImplementation(async (_book, _id, _token, revision) =>
      JSON.stringify({ contentRevision: revision }),
    );
  jest.mocked(PdfEngine.saveNote).mockResolvedValue('{}');
  jest.mocked(PdfEngine.endNoteEditor).mockResolvedValue();
  jest
    .mocked(PdfEngine.openNoteEditor)
    .mockResolvedValue(JSON.stringify(initial));
  jest.mocked(PdfEngine.listNotes).mockResolvedValue('[]');
});
afterEach(() => {
  jest.clearAllMocks();
  jest.restoreAllMocks();
  jest.useRealTimers();
});

test('typing debounces one latest draft and waits for its commit acknowledgement', async () => {
  const { tree } = await editor();
  await act(async () => {
    input(tree).props.onChangeText('İstanbul');
    input(tree).props.onChangeText('İstanbul son metin');
  });
  expect(PdfEngine.writeNoteDraft).not.toHaveBeenCalled();
  await act(async () => jest.advanceTimersByTime(650));
  expect(PdfEngine.writeNoteDraft).toHaveBeenCalledTimes(1);
  expect(PdfEngine.writeNoteDraft).toHaveBeenCalledWith(
    'a',
    initial.id,
    initial.editorToken,
    2,
    '',
    'İstanbul son metin',
  );
  expect(text(tree)).toContain('Taslak kaydedildi.');
  await act(async () => tree.unmount());
});

test('late failure for revision one cannot erase revision two saved status', async () => {
  let rejectOld!: (error: Error) => void;
  let resolveNew!: (value: string) => void;
  jest
    .mocked(PdfEngine.writeNoteDraft)
    .mockReturnValueOnce(
      new Promise((_, reject) => {
        rejectOld = reject;
      }),
    )
    .mockReturnValueOnce(
      new Promise(resolve => {
        resolveNew = resolve;
      }),
    );
  const { tree } = await editor();
  await act(async () => {
    input(tree).props.onChangeText('Bir');
    jest.advanceTimersByTime(650);
  });
  await act(async () => {
    input(tree).props.onChangeText('İki');
    jest.advanceTimersByTime(650);
  });
  await act(async () => resolveNew('{"contentRevision":2}'));
  await act(async () => rejectOld(new Error('Eski yazı hatası')));
  expect(input(tree).props.value).toBe('İki');
  expect(text(tree)).toContain('Taslak kaydedildi.');
  expect(text(tree)).not.toContain('Eski yazı hatası');
  await act(async () => tree.unmount());
});

test('failed final save preserves text and retry then closes without resurrecting a draft', async () => {
  const { tree, closed } = await editor();
  await act(async () =>
    input(tree).props.onChangeText('Kaybolmaması gereken düşünce'),
  );
  jest.mocked(PdfEngine.saveNote).mockRejectedValueOnce(new Error('Disk dolu'));
  await act(async () => button(tree, 'Notu kaydet').props.onPress());
  expect(closed).not.toHaveBeenCalled();
  expect(input(tree).props.value).toBe('Kaybolmaması gereken düşünce');
  expect(text(tree)).toContain('Disk dolu');
  const first = jest.mocked(PdfEngine.saveNote).mock.calls[0];
  await act(async () => button(tree, 'Notu kaydet').props.onPress());
  expect(jest.mocked(PdfEngine.saveNote).mock.calls[1]).toEqual(first);
  expect(closed).toHaveBeenCalledWith(true);
  await act(async () => tree.unmount());
  expect(PdfEngine.writeNoteDraft).not.toHaveBeenCalled();
});

test('background flushes current text before the debounce and reopen displays recovered draft', async () => {
  let background!: (state: AppStateStatus) => void;
  jest.spyOn(AppState, 'addEventListener').mockImplementation((_, handler) => {
    background = handler;
    return { remove: jest.fn() };
  });
  const { tree } = await editor({
    recovered: true,
    dirty: true,
    body: 'Önceki commit',
    contentRevision: 4,
  });
  expect(text(tree)).toContain('Son kaydedilmiş taslak kurtarıldı.');
  await act(async () => input(tree).props.onChangeText('Son düşünce'));
  await act(async () => background('background'));
  expect(PdfEngine.writeNoteDraft).toHaveBeenCalledWith(
    'a',
    initial.id,
    initial.editorToken,
    5,
    '',
    'Son düşünce',
  );
  await act(async () => tree.unmount());
});

test('back dismisses IME first then closes with the latest draft payload', async () => {
  const callbacks: Record<string, () => void> = {};
  jest.spyOn(Keyboard, 'addListener').mockImplementation((name, handler) => {
    callbacks[name] = handler as () => void;
    return { remove: jest.fn() };
  });
  const dismiss = jest.spyOn(Keyboard, 'dismiss').mockImplementation(() => {});
  const { tree, closed } = await editor();
  await act(async () =>
    input(tree).props.onChangeText('Geri sırasında son tuş'),
  );
  const modal = tree.root.findAll(
    node => typeof node.props.onRequestClose === 'function',
  )[0];
  await act(async () => {
    callbacks.keyboardDidShow();
    modal.props.onRequestClose();
  });
  expect(dismiss).toHaveBeenCalled();
  expect(PdfEngine.endNoteEditor).not.toHaveBeenCalled();
  await act(async () => {
    callbacks.keyboardDidHide();
    modal.props.onRequestClose();
  });
  expect(PdfEngine.endNoteEditor).toHaveBeenCalledWith(
    'a',
    initial.id,
    initial.editorToken,
    1,
    '',
    'Geri sırasında son tuş',
  );
  expect(closed).toHaveBeenCalledWith(false);
  await act(async () => tree.unmount());
});

test('note list replaces its forty-row window and opens only selected full body', async () => {
  const cards = Array.from({ length: 40 }, (_, index) => ({
    id: `note-${index}`,
    scope: 'book',
    pageIndex: -1,
    annotationId: '',
    quote: '',
    title: `Not ${index}`,
    body: 'Kısa önizleme',
    revision: 1,
    updatedAt: 1,
    hasDraft: false,
    draftOnly: false,
    annotationDeleted: false,
  }));
  jest.mocked(PdfEngine.listNotes).mockResolvedValue(JSON.stringify(cards));
  let tree!: Renderer.ReactTestRenderer;
  await act(async () => {
    tree = Renderer.create(<NoteList bookId="a" jump={jest.fn()} />);
  });
  expect(PdfEngine.openNoteEditor).not.toHaveBeenCalled();
  jest
    .mocked(PdfEngine.listNotes)
    .mockResolvedValue(
      JSON.stringify([{ ...cards[0], id: 'note-40', title: 'Sonraki not' }]),
    );
  await act(async () => button(tree, 'Sonraki notlar').props.onPress());
  expect(PdfEngine.listNotes).toHaveBeenLastCalledWith(
    'a',
    '',
    '',
    'date',
    40,
    40,
  );
  expect(text(tree)).not.toContain('Not 39');
  await act(async () => button(tree, 'Notu aç').props.onPress());
  expect(PdfEngine.openNoteEditor).toHaveBeenCalledWith(
    'a',
    'note-40',
    'book',
    -1,
    '',
  );
  await act(async () => tree.unmount());
});
