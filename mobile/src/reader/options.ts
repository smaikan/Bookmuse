export type ReaderOptions = {
  theme: number;
  paper: string;
  ink: string;
  contrast: number;
  left: number;
  top: number;
  right: number;
  bottom: number;
  fitWidth: boolean;
  brightness: number;
  orientation: 'auto' | 'portrait' | 'landscape';
  screenOn: boolean;
  fullscreen: boolean;
  counterAlways: boolean;
  reduceMotion: boolean;
  leftHand: boolean;
  doublePage: boolean;
  coverSingle: boolean;
};
export const DEFAULT_OPTIONS: ReaderOptions = {
  theme: 0,
  paper: '#F3EBDD',
  ink: '#313B32',
  contrast: 1,
  left: 0,
  top: 0,
  right: 0,
  bottom: 0,
  fitWidth: false,
  brightness: -1,
  orientation: 'auto',
  screenOn: false,
  fullscreen: true,
  counterAlways: false,
  reduceMotion: false,
  leftHand: false,
  doublePage: false,
  coverSingle: true,
};
export function validateOptions(value: ReaderOptions): string | null {
  if (
    !/^#[0-9a-f]{6}$/i.test(value.paper) ||
    !/^#[0-9a-f]{6}$/i.test(value.ink)
  ) {
    return 'Renkleri #RRGGBB biçiminde yaz. Örneğin #F3EBDD.';
  }
  for (const field of ['left', 'top', 'right', 'bottom'] as const) {
    if (
      !Number.isFinite(value[field]) ||
      value[field] < 0 ||
      value[field] > 0.3
    ) {
      return 'Kırpma her kenarda %0–30 olmalı.';
    }
  }
  if (
    value.contrast < 0.6 ||
    value.contrast > 1.8 ||
    !Number.isFinite(value.contrast)
  ) {
    return 'Kontrast ayarı geçersiz.';
  }
  return null;
}
export function isDark(options: ReaderOptions): boolean {
  if (options.theme === 3) {
    return true;
  }
  if (options.theme !== 4) {
    return false;
  }
  const red = Number.parseInt(options.paper.slice(1, 3), 16);
  const green = Number.parseInt(options.paper.slice(3, 5), 16);
  const blue = Number.parseInt(options.paper.slice(5, 7), 16);
  return red * 0.2126 + green * 0.7152 + blue * 0.0722 < 128;
}

export function readerPaper(options: ReaderOptions): string {
  return options.theme === 1
    ? '#FFF5E3'
    : options.theme === 2
    ? '#F4E7CE'
    : options.theme === 3
    ? '#1E2124'
    : options.theme === 4
    ? options.paper
    : '#FFFFFF';
}
