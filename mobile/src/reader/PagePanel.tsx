import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Animated,
  BackHandler,
  FlatList,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  useWindowDimensions,
  View,
  VirtualizedList,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import PdfEngine from '../specs/NativeKitapPdf';
import Thumbnail from '../specs/KitapThumbnailNativeComponent';
import { Action } from './controls';
import { errorMessage, parsePage } from './types';
import { isDark, readerPaper } from './options';
import type { ReaderOptions } from './options';

/** Rows are generated on demand; opening a thousand-page document allocates no page array. */
export function PagePanel({
  bookId,
  count,
  current,
  options,
  close,
  closeStart,
  select,
  counter,
  bookmarked,
  bookmarkBusy,
  toggleBookmark,
  returnToReading,
  reduced = false,
}: {
  bookId: string;
  count: number;
  current: number;
  options: ReaderOptions;
  close: () => void;
  closeStart?: () => void;
  select: (page: number) => void;
  counter?: string;
  bookmarked?: boolean;
  bookmarkBusy?: boolean;
  toggleBookmark?: () => void;
  returnToReading?: () => void;
  reduced?: boolean;
}) {
  const window = useWindowDimensions(),
    insets = useSafeAreaInsets();
  const dark = isDark(options),
    ink = dark ? '#dadbd7' : '#354436';
  const cellWidth = (window.width - insets.left - insets.right - 32 - 24) / 3;
  const imageHeight = Math.min(
    240,
    Math.max(72, Math.min(cellWidth * 1.4, window.height * 0.22)),
  );
  const rowHeight = imageHeight + Math.max(48, 28 * window.fontScale) + 20;
  const [tab, setTab] = useState('pages');
  const [visible, setVisible] = useState<Set<number>>(
    new Set([Math.floor(current / 3)]),
  );
  const [number, setNumber] = useState('');
  const [bookmarks, setBookmarks] = useState<number[]>([]);
  const [more, setMore] = useState(false),
    [error, setError] = useState('');
  const alive = useRef(true),
    fetching = useRef(false),
    offset = useRef(0),
    closing = useRef(false);
  const selectedAfterClose = useRef<number | null>(null);
  const reloadRequested = useRef(false);
  const progress = useRef(new Animated.Value(reduced ? 1 : 0)).current;
  useEffect(() => {
    const animation = Animated.timing(progress, {
      toValue: 1,
      duration: reduced ? 0 : 200,
      useNativeDriver: true,
    });
    animation.start();
    return () => animation.stop();
  }, [progress, reduced]);
  const dismiss = useCallback(() => {
    if (closing.current) return;
    closing.current = true;
    closeStart?.();
    Animated.timing(progress, {
      toValue: 0,
      duration: reduced ? 0 : 160,
      useNativeDriver: true,
    }).start(({ finished }) => {
      if (finished) {
        if (selectedAfterClose.current !== null)
          select(selectedAfterClose.current);
        close();
      }
    });
  }, [close, closeStart, progress, reduced, select]);
  useEffect(() => {
    const back = BackHandler.addEventListener('hardwareBackPress', () => {
      dismiss();
      return true;
    });
    return () => back.remove();
  }, [dismiss]);
  const choose = (page: number) => {
    if (closing.current) return;
    selectedAfterClose.current = page;
    dismiss();
  };
  const load: (reset: boolean) => Promise<void> = useCallback(
    async (reset: boolean): Promise<void> => {
      if (fetching.current) {
        if (reset) reloadRequested.current = true;
        return;
      }
      fetching.current = true;
      try {
        const batch: number[] = JSON.parse(
          await PdfEngine.listBookmarks(bookId, reset ? 0 : offset.current, 40),
        );
        if (alive.current) {
          offset.current = (reset ? 0 : offset.current) + batch.length;
          setBookmarks(old => (reset ? batch : [...old, ...batch]));
          setMore(batch.length === 40);
        }
      } catch (issue) {
        if (alive.current) setError(errorMessage(issue));
      } finally {
        fetching.current = false;
        if (alive.current && reloadRequested.current) {
          reloadRequested.current = false;
          load(true);
        }
      }
    },
    [bookId],
  );
  useEffect(() => {
    alive.current = true;
    load(true);
    return () => {
      alive.current = false;
    };
  }, [load, bookmarked]);
  const onVisible = useRef(
    ({ viewableItems }: { viewableItems: Array<{ item: number }> }) =>
      setVisible(new Set(viewableItems.map(item => item.item))),
  ).current;
  return (
    <Animated.View
      testID="page-overview"
      accessibilityViewIsModal
      style={[
        styles.screen,
        {
          backgroundColor: readerPaper(options),
          paddingTop: insets.top + 8,
          paddingBottom: insets.bottom + 8,
          paddingLeft: insets.left + 16,
          paddingRight: insets.right + 16,
          opacity: progress,
          transform: [
            {
              scale: progress.interpolate({
                inputRange: [0, 1],
                outputRange: reduced ? [1, 1] : [0.94, 1],
              }),
            },
          ],
        },
      ]}
    >
      <View
        style={[
          styles.header,
          options.leftHand && { flexDirection: 'row-reverse' },
        ]}
      >
        <View style={styles.headerCopy}>
          <Text
            accessibilityRole="header"
            numberOfLines={1}
            style={[styles.title, { color: ink }]}
          >
            Sayfalar
          </Text>
          <Text style={[styles.counter, { color: ink }]}>
            {counter ?? `${current + 1} / ${count}`}
          </Text>
        </View>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Paneli kapat"
          onPress={dismiss}
          style={styles.close}
        >
          <Text
            maxFontSizeMultiplier={1.5}
            style={{ fontSize: 24, color: ink }}
          >
            ×
          </Text>
        </Pressable>
      </View>
      <ScrollView
        style={{
          maxHeight: Math.max(96, window.height * 0.32),
          flexGrow: 0,
          flexShrink: 1,
        }}
        contentContainerStyle={styles.toolbar}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.row}>
          <TextInput
            accessibilityLabel="Sayfa numarası"
            placeholder={`1–${count}`}
            placeholderTextColor={dark ? '#aab0ae' : '#667064'}
            value={number}
            maxLength={10}
            keyboardType="number-pad"
            selectTextOnFocus
            onChangeText={setNumber}
            onSubmitEditing={() => {
              const page = parsePage(number, count);
              if (page === null) setError(`1–${count} arasında bir sayfa gir.`);
              else choose(page);
            }}
            style={[styles.input, { color: ink }]}
          />
          <Action
            label="Git"
            onPress={() => {
              const page = parsePage(number, count);
              if (page === null) setError(`1–${count} arasında bir sayfa gir.`);
              else choose(page);
            }}
          />
        </View>
        <View style={styles.row}>
          <Action
            label="Tüm sayfalar"
            selected={tab === 'pages'}
            onPress={() => setTab('pages')}
          />
          <Action
            label="Yer imleri"
            selected={tab === 'bookmarks'}
            onPress={() => setTab('bookmarks')}
          />
          {toggleBookmark && (
            <Action
              label={bookmarked ? 'Yer imini kaldır' : 'Yer imi ekle'}
              disabled={bookmarkBusy}
              onPress={toggleBookmark}
            />
          )}
        </View>
        {returnToReading && (
          <Action label="Okumaya dön" onPress={returnToReading} />
        )}
      </ScrollView>
      {!!error && (
        <Text accessibilityLiveRegion="polite" style={{ color: ink }}>
          {error}
        </Text>
      )}
      {tab === 'pages' ? (
        <VirtualizedList
          key={`${Math.round(cellWidth)}-${window.fontScale}-${Math.round(
            imageHeight,
          )}`}
          style={styles.fill}
          data={{ count }}
          getItem={(_, index) => index}
          getItemCount={() => Math.ceil(count / 3)}
          keyExtractor={row => String(row)}
          initialNumToRender={3}
          maxToRenderPerBatch={3}
          windowSize={3}
          initialScrollIndex={Math.floor(current / 3)}
          getItemLayout={(_, index) => ({
            length: rowHeight,
            offset: rowHeight * index,
            index,
          })}
          onViewableItemsChanged={onVisible}
          viewabilityConfig={{ itemVisiblePercentThreshold: 10 }}
          keyboardShouldPersistTaps="handled"
          renderItem={({ item: row }) => (
            <View style={[styles.gridRow, { height: rowHeight }]}>
              {Array.from(
                { length: Math.min(3, count - row * 3) },
                (_, column) => row * 3 + column,
              ).map(page => (
                <Pressable
                  key={page}
                  accessibilityRole="button"
                  accessibilityLabel={`Sayfa ${page + 1}`}
                  accessibilityState={{ selected: page === current }}
                  onPress={() => choose(page)}
                  style={[
                    styles.cell,
                    { width: cellWidth },
                    page === current && styles.current,
                  ]}
                >
                  <Thumbnail
                    accessible={false}
                    importantForAccessibility="no-hide-descendants"
                    style={{ width: cellWidth - 8, height: imageHeight }}
                    bookId={bookId}
                    pageIndex={page}
                    enabled={visible.has(row)}
                    dark={dark}
                  />
                  <Text style={[styles.page, { color: ink }]}>{page + 1}</Text>
                </Pressable>
              ))}
            </View>
          )}
        />
      ) : (
        <FlatList
          style={styles.fill}
          data={bookmarks}
          keyExtractor={page => String(page)}
          initialNumToRender={12}
          maxToRenderPerBatch={12}
          windowSize={3}
          renderItem={({ item: page }) => (
            <View style={styles.bookmark}>
              <Action
                label={`Sayfa ${page + 1}`}
                onPress={() => choose(page)}
              />
            </View>
          )}
          ListEmptyComponent={
            <Text style={{ color: ink }}>Yer imi bulunmuyor.</Text>
          }
          ListFooterComponent={
            more ? (
              <Action label="Daha fazla yer imi" onPress={() => load(false)} />
            ) : undefined
          }
        />
      )}
    </Animated.View>
  );
}
const styles = StyleSheet.create({
  screen: {
    position: 'absolute',
    top: 0,
    right: 0,
    bottom: 0,
    left: 0,
    gap: 8,
  },
  fill: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  headerCopy: { flex: 1, minWidth: 0 },
  toolbar: { gap: 8, paddingBottom: 2 },
  close: {
    minWidth: 48,
    minHeight: 48,
    alignItems: 'center',
    justifyContent: 'center',
  },
  row: { flexDirection: 'row', alignItems: 'center', flexWrap: 'wrap', gap: 8 },
  title: { fontSize: 22, fontWeight: '600', flexGrow: 1 },
  counter: { fontSize: 16 },
  input: {
    borderWidth: 1,
    borderColor: '#879186',
    borderRadius: 8,
    minHeight: 48,
    minWidth: 72,
    flex: 1,
    paddingHorizontal: 12,
    fontSize: 16,
  },
  gridRow: { flexDirection: 'row', gap: 12, alignItems: 'flex-start' },
  cell: {
    overflow: 'hidden',
    alignItems: 'center',
    borderRadius: 8,
    padding: 2,
    borderWidth: 2,
    borderColor: 'transparent',
  },
  current: { borderColor: '#6d8a50', backgroundColor: '#9dae7733' },
  page: { fontSize: 16, minHeight: 44, paddingTop: 8 },
  bookmark: { paddingVertical: 6 },
});
