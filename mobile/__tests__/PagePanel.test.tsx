import React from 'react';
import { Animated, TextInput, VirtualizedList } from 'react-native';
import ReactTestRenderer, { act } from 'react-test-renderer';
import { PagePanel } from '../src/reader/PagePanel';
import { DEFAULT_OPTIONS } from '../src/reader/options';
import PdfEngine from '../src/specs/NativeKitapPdf';

jest.mock(
  'react-native-safe-area-context',
  () => require('react-native-safe-area-context/jest/mock').default,
);
jest.mock('../src/specs/KitapThumbnailNativeComponent', () => 'KitapThumbnail');
jest.mock('../src/specs/NativeKitapPdf', () => ({
  __esModule: true,
  default: { listBookmarks: jest.fn() },
}));
let tree: ReactTestRenderer.ReactTestRenderer;
let finishClose: ((result: { finished: boolean }) => void) | undefined;
const select = jest.fn(),
  close = jest.fn();
beforeEach(() => {
  jest.clearAllMocks();
  finishClose = undefined;
  jest.mocked(PdfEngine.listBookmarks).mockResolvedValue('[]');
  jest.spyOn(Animated, 'timing').mockImplementation((value, config) => ({
    start: callback => {
      if (config.toValue === 0) finishClose = callback;
      else {
        if (
          value instanceof Animated.Value &&
          typeof config.toValue === 'number'
        )
          value.setValue(config.toValue);
        callback?.({ finished: true });
      }
    },
    stop: jest.fn(),
    reset: jest.fn(),
  }));
});
afterEach(async () => {
  if (tree) await act(async () => tree.unmount());
  jest.restoreAllMocks();
});
async function render(count = 80, current = 44, returnToReading?: () => void) {
  await act(async () => {
    tree = ReactTestRenderer.create(
      <PagePanel
        bookId="a"
        count={count}
        current={current}
        options={DEFAULT_OPTIONS}
        close={close}
        select={select}
        returnToReading={returnToReading}
      />,
    );
  });
}
function button(label: string) {
  return tree.root.findAll(
    node =>
      node.props.accessibilityLabel === label &&
      typeof node.props.onPress === 'function',
  )[0];
}

test('million-page overview renders a bounded window and scrolling never selects a reading position', async () => {
  await render(1_000_000, 6000);
  const list = tree.root.findByType(
    VirtualizedList as unknown as React.ElementType,
  );
  expect(
    tree.root.findAll(node => (node.type as unknown) === 'KitapThumbnail')
      .length,
  ).toBeLessThanOrEqual(18);
  expect(list.props.initialScrollIndex).toBe(2000);
  await act(async () =>
    list.props.onViewableItemsChanged({ viewableItems: [{ item: 2001 }] }),
  );
  expect(select).not.toHaveBeenCalled();
  await act(async () => button('Paneli kapat').props.onPress());
  await act(async () => finishClose?.({ finished: true }));
  expect(close).toHaveBeenCalledTimes(1);
  expect(select).not.toHaveBeenCalled();
});
test('invalid page cannot navigate; a valid choice commits only after closing and duplicate presses are ignored', async () => {
  await render();
  const input = tree.root.findByType(TextInput);
  await act(async () => input.props.onChangeText('0'));
  await act(async () => button('Git').props.onPress());
  expect(select).not.toHaveBeenCalled();
  expect(finishClose).toBeUndefined();
  await act(async () => input.props.onChangeText('60'));
  await act(async () => {
    button('Git').props.onPress();
    button('Git').props.onPress();
  });
  expect(select).not.toHaveBeenCalled();
  await act(async () => finishClose?.({ finished: true }));
  expect(select).toHaveBeenCalledTimes(1);
  expect(select).toHaveBeenCalledWith(59);
  expect(close).toHaveBeenCalledTimes(1);
});
test('overview exposes three page cells per row and selection remains available before thumbnails load', async () => {
  await render(1000, 44);
  const cells = tree.root.findAll(
    node =>
      node.props.accessibilityLabel === 'Sayfa 45' &&
      typeof node.props.onPress === 'function',
  );
  expect(cells.length).toBeGreaterThan(0);
  await act(async () => cells[0].props.onPress());
  await act(async () => finishClose?.({ finished: true }));
  expect(select).toHaveBeenCalledWith(44);
});

test('overview omits helper controls while preserving return from notes and highlights', async () => {
  const resume = jest.fn();
  await render(80, 44, resume);
  for (const label of [
    'Yardımcı kontroller',
    'Zoom sıfırla',
    'Büyüt',
    'Küçült',
  ])
    expect(button(label)).toBeUndefined();
  await act(async () => button('Okumaya dön').props.onPress());
  expect(resume).toHaveBeenCalledTimes(1);
});
