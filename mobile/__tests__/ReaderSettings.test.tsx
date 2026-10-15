import React from 'react';
import { StyleSheet, View } from 'react-native';
import ReactTestRenderer, { act } from 'react-test-renderer';
import { ReaderSettings } from '../src/reader/ReaderSettings';
import { ReaderToneContext } from '../src/reader/controls';
import { DEFAULT_OPTIONS } from '../src/reader/options';

jest.mock(
  'react-native-safe-area-context',
  () => require('react-native-safe-area-context/jest/mock').default,
);
jest.mock('../src/specs/KitapSliderNativeComponent', () => 'KitapSlider');
let tree: ReactTestRenderer.ReactTestRenderer;
const save = jest.fn(),
  close = jest.fn();
function button(label: string) {
  return tree.root.findAll(
    n =>
      n.props.accessibilityLabel === label &&
      typeof n.props.onPress === 'function',
  )[0];
}
async function render(theme = 0, paper = '#F3EBDD') {
  await act(async () => {
    tree = ReactTestRenderer.create(
      <ReaderToneContext.Provider value={true}>
        <ReaderSettings
          options={{ ...DEFAULT_OPTIONS, theme, paper }}
          save={save}
          close={close}
        />
      </ReaderToneContext.Provider>,
    );
  });
}
beforeEach(() => {
  jest.clearAllMocks();
  save.mockReset();
  save.mockResolvedValue(undefined);
});
afterEach(async () => {
  if (tree) await act(async () => tree.unmount());
});

test.each([
  ['Orijinal', 0],
  ['Sepya', 2],
  ['Gece', 3],
  ['Özel', 4],
])(
  '%s keeps its persisted native theme id and settings stay light even under a dark reader context',
  async (label, theme) => {
    await render();
    expect(button('Krem')).toBeUndefined();
    await act(async () => button(label).props.onPress());
    expect(
      tree.root
        .findAllByType(View)
        .some(
          n => StyleSheet.flatten(n.props.style)?.backgroundColor === '#F5F3ED',
        ),
    ).toBe(true);
    const style = StyleSheet.flatten(button('Ayarları kaydet').props.style);
    expect(style.backgroundColor).toBe('#dce3d7');
    await act(async () => button('Ayarları kaydet').props.onPress());
    expect(save).toHaveBeenCalledWith(expect.objectContaining({ theme }));
  },
);

test('custom dark paper does not recolor the settings surface or controls', async () => {
  await render(4, '#000000');
  expect(
    StyleSheet.flatten(button('Ayarları kaydet').props.style).backgroundColor,
  ).toBe('#dce3d7');
  const paper = tree.root.findAll(
    n =>
      n.props.accessibilityLabel === 'Arka plan rengi' &&
      typeof n.props.onChangeText === 'function',
  )[0];
  expect(paper.props.value).toBe('#000000');
  expect(StyleSheet.flatten(paper.props.style).color).toBe('#273c2d');
});

test('failed save stays open for retry; duplicate presses and closing while saving do not discard changes', async () => {
  let reject!: (error: Error) => void;
  save.mockReturnValueOnce(
    new Promise((_, fail) => {
      reject = fail;
    }),
  );
  await render(3);
  await act(async () => {
    button('Ayarları kaydet').props.onPress();
    button('Ayarları kaydet').props.onPress();
    button('Vazgeç').props.onPress();
  });
  expect(save).toHaveBeenCalledTimes(1);
  expect(close).not.toHaveBeenCalled();
  await act(async () => reject(new Error('disk full')));
  expect(
    tree.root.findAll(n => n.props.children === 'disk full').length,
  ).toBeGreaterThan(0);
  await act(async () => button('Ayarları kaydet').props.onPress());
  expect(save).toHaveBeenCalledTimes(2);
  expect(save).toHaveBeenLastCalledWith(expect.objectContaining({ theme: 3 }));
});
