import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Alert,
  AppState,
  DeviceEventEmitter,
  FlatList,
  KeyboardAvoidingView,
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  useWindowDimensions,
  View,
} from 'react-native';
import { useFocusEffect, useIsFocused } from '@react-navigation/native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import PdfEngine from '../specs/NativeKitapPdf';
import Thumbnail from '../specs/KitapThumbnailNativeComponent';
import { Action } from './controls';
import { errorMessage } from './types';
import type { Book, OpenBook, Routes } from './types';
import { AnnotationsPanel } from './Annotations';
import type { Annotation } from './Annotations';
import { NotesPanel } from './Notes';
import type { NoteCard } from './Notes';
import { ReaderSettings } from './ReaderSettings';
import type { ReaderOptions } from './options';

const PAGE_SIZE = 40;
export function LibraryScreen({
  navigation,
}: NativeStackScreenProps<Routes, 'Library'>) {
  const insets = useSafeAreaInsets();
  const window = useWindowDimensions();
  const focused = useIsFocused();
  const [books, setBooks] = useState<Book[]>([]);
  const [recent, setRecent] = useState<Book | null>(null);
  const [message, setMessage] = useState('Kitaplık hazırlanıyor…');
  const [busy, setBusy] = useState('');
  const [cancelRequested, setCancelRequested] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const [text, setText] = useState('');
  const [query, setQuery] = useState('');
  const [sort, setSort] = useState('recent');
  const [favoritesOnly, setFavoritesOnly] = useState(false);
  const [preferencesReady, setPreferencesReady] = useState(false);
  const [menuBook, setMenuBook] = useState<Book | null>(null);
  const [sortOpen, setSortOpen] = useState(false);
  const [menuPoint, setMenuPoint] = useState({ x: 16, y: 100 });
  const [visible, setVisible] = useState<Set<string>>(new Set());
  const [renaming, setRenaming] = useState<Book | null>(null);
  const [name, setName] = useState('');
  const [annotationBook, setAnnotationBook] = useState<Book | null>(null);
  const [readerSettings, setReaderSettings] = useState<ReaderOptions | null>(
    null,
  );
  const [notesBook, setNotesBook] = useState<Book | null>(null);
  async function jumpNote(note: NoteCard) {
    if (!notesBook || operation.current) return;
    operation.current = true;
    setBusy('Kitap açılıyor…');
    try {
      const opened: OpenBook = JSON.parse(
        await PdfEngine.openBook(notesBook.id),
      );
      setNotesBook(null);
      navigation.navigate('Reader', {
        book: opened,
        annotation:
          note.annotationId && !note.annotationDeleted
            ? { id: note.annotationId, pageIndex: note.pageIndex }
            : undefined,
        notePage:
          !note.annotationId || note.annotationDeleted
            ? note.pageIndex
            : undefined,
      });
    } catch (issue) {
      setMessage(errorMessage(issue));
      throw issue;
    } finally {
      operation.current = false;
      setBusy('');
    }
  }
  async function jumpAnnotation(mark: Annotation) {
    if (!annotationBook || operation.current) return;
    operation.current = true;
    setBusy('Kitap açılıyor…');
    try {
      const opened: OpenBook = JSON.parse(
        await PdfEngine.openBook(annotationBook.id),
      );
      setAnnotationBook(null);
      navigation.navigate('Reader', {
        book: opened,
        annotation: { id: mark.id, pageIndex: mark.pageIndex },
      });
    } catch (issue) {
      setMessage(errorMessage(issue));
      throw issue;
    } finally {
      operation.current = false;
      setBusy('');
    }
  }
  const [renameError, setRenameError] = useState('');
  const operation = useRef(false);
  const loading = useRef(false);
  const generation = useRef(0);
  const loadedCount = useRef(0);
  const onVisible = useRef(
    ({ viewableItems }: { viewableItems: Array<{ item: Book }> }) => {
      setVisible(new Set(viewableItems.map(item => item.item.id)));
    },
  ).current;
  useEffect(() => {
    const timer = setTimeout(() => setQuery(text.trim()), 220);
    return () => clearTimeout(timer);
  }, [text]);
  useEffect(() => {
    let alive = true;
    PdfEngine.getLibraryPreferences()
      .then(value => {
        if (alive) {
          const prefs = JSON.parse(value);
          setSort(prefs.sort);
          setFavoritesOnly(prefs.favoritesOnly);
        }
      })
      .catch(() => {})
      .finally(() => {
        if (alive) setPreferencesReady(true);
      });
    return () => {
      alive = false;
    };
  }, []);

  const load = useCallback(
    async (reset: boolean) => {
      if (!reset && (loading.current || operation.current)) {
        return;
      }
      const token = reset ? ++generation.current : generation.current;
      const offset = reset ? 0 : loadedCount.current;
      loading.current = true;
      try {
        const batch: Book[] = JSON.parse(
          await PdfEngine.queryLibrary(
            query,
            sort,
            favoritesOnly,
            offset,
            PAGE_SIZE,
          ),
        );
        if (token !== generation.current) {
          return;
        }
        loadedCount.current = offset + batch.length;
        setBooks(previous => (reset ? batch : [...previous, ...batch]));
        setHasMore(batch.length === PAGE_SIZE);
        if (reset) {
          setMessage(
            batch.length
              ? ''
              : query
              ? 'Bu adda kitap bulunamadı.'
              : 'İlk kitabını ekleyerek başlayabilirsin.',
          );
          const last: Book | null = JSON.parse(
            await PdfEngine.mostRecentBook(),
          );
          if (token === generation.current) {
            setRecent(last);
          }
        }
      } catch (error) {
        if (token === generation.current) {
          setMessage(errorMessage(error));
        }
      } finally {
        if (token === generation.current) {
          loading.current = false;
        }
      }
    },
    [query, sort, favoritesOnly],
  );
  useFocusEffect(
    useCallback(() => {
      if (preferencesReady) load(true);
      return () => {
        generation.current++;
        loading.current = false;
      };
    }, [load, preferencesReady]),
  );

  useEffect(() => {
    if (!focused) return;
    let alive = true;
    async function receive() {
      if (operation.current) return;
      operation.current = true;
      try {
        const result = JSON.parse(await PdfEngine.importSharedPdf());
        if (result && alive) {
          await load(true);
          if (!result.cancelled) {
            const opened: OpenBook = JSON.parse(
              await PdfEngine.openBook(result.id),
            );
            setNotesBook(null);
            setAnnotationBook(null);
            setReaderSettings(null);
            setRenaming(null);
            navigation.navigate('Reader', { book: opened });
          } else setMessage('Paylaşılan dosyanın eklenmesi iptal edildi.');
        }
      } catch (error) {
        if (alive) setMessage(errorMessage(error));
      } finally {
        operation.current = false;
      }
    }
    receive();
    const listener = AppState.addEventListener('change', state => {
      if (state === 'active') receive();
    });
    const shareListener = DeviceEventEmitter.addListener(
      'KitapSharedPdf',
      receive,
    );
    return () => {
      alive = false;
      listener.remove();
      shareListener.remove();
    };
  }, [focused, load, navigation]);
  async function pick(relinkId = '') {
    if (operation.current) {
      return;
    }
    operation.current = true;
    setBusy('import');
    setCancelRequested(false);
    setMessage('Dosyayı seç. Ardından güvenli bir yerel kopya hazırlanacak…');
    try {
      const result = JSON.parse(await PdfEngine.pickBook(relinkId));
      await load(true);
      setMessage(
        result.cancelled
          ? 'Ekleme iptal edildi.'
          : result.duplicate
          ? 'Bu dosya zaten kitaplığında.'
          : relinkId
          ? 'Aynı PDF yeniden bağlandı. Konum korundu.'
          : 'Kitap eklendi. Okumak için adına dokun.',
      );
    } catch (error) {
      setMessage(errorMessage(error));
    } finally {
      operation.current = false;
      setBusy('');
    }
  }
  async function cancel() {
    setCancelRequested(true);
    setMessage('İptal ediliyor; yarım kopya temizlenecek…');
    try {
      if (!(await PdfEngine.cancelImport())) {
        setMessage('İşlem zaten tamamlandı; kitaplık yenileniyor…');
      }
    } catch (error) {
      setCancelRequested(false);
      setMessage(errorMessage(error));
    }
  }
  async function open(book: Book) {
    if (operation.current) {
      return;
    }
    operation.current = true;
    setBusy('open');
    setMessage('Kitap açılıyor…');
    try {
      const result: OpenBook = JSON.parse(await PdfEngine.openBook(book.id));
      navigation.navigate('Reader', { book: result });
      setMessage('');
    } catch (error) {
      setMessage(errorMessage(error));
    } finally {
      operation.current = false;
      setBusy('');
    }
  }
  function remove(book: Book) {
    Alert.alert(
      'Kitaplıktan kaldır',
      `“${book.title}”, uygulamadaki kopyası ve bu kitabın kayıtları kaldırılacak. Dışarıdaki orijinal PDF silinmez.`,
      [
        { text: 'Vazgeç', style: 'cancel' },
        {
          text: 'Kaldır',
          style: 'destructive',
          onPress: async () => {
            if (operation.current) {
              return;
            }
            operation.current = true;
            setBusy('remove');
            try {
              await PdfEngine.removeBook(book.id);
              await load(true);
            } catch (error) {
              setMessage(errorMessage(error));
            } finally {
              operation.current = false;
              setBusy('');
            }
          },
        },
      ],
    );
  }
  async function rename() {
    if (!renaming || operation.current) {
      return;
    }
    const title = name.trim();
    if (!title) {
      setRenameError('Kitap adı boş olamaz.');
      return;
    }
    operation.current = true;
    setBusy('rename');
    try {
      await PdfEngine.renameBook(renaming.id, title);
      setRenaming(null);
      await load(true);
    } catch (error) {
      setRenameError(errorMessage(error));
    } finally {
      operation.current = false;
      setBusy('');
    }
  }
  async function openSettings() {
    if (operation.current) return;
    operation.current = true;
    setBusy('settings');
    try {
      setReaderSettings(JSON.parse(await PdfEngine.getReaderDefaults()));
    } catch (error) {
      setMessage(errorMessage(error));
    } finally {
      operation.current = false;
      setBusy('');
    }
  }
  async function favorite(book: Book) {
    if (operation.current) return;
    operation.current = true;
    setBusy('favorite');
    try {
      await PdfEngine.setFavorite(book.id, !book.favorite);
      setMenuBook(null);
      await load(true);
    } catch (error) {
      setMessage(errorMessage(error));
    } finally {
      operation.current = false;
      setBusy('');
    }
  }
  async function choosePreferences(nextSort: string, favorites: boolean) {
    try {
      await PdfEngine.saveLibraryPreferences(nextSort, favorites);
      setSort(nextSort);
      setFavoritesOnly(favorites);
      setSortOpen(false);
    } catch (error) {
      setMessage(errorMessage(error));
    }
  }
  const usableWidth = window.width - insets.left - insets.right;
  const columns =
    usableWidth < 320
      ? 1
      : usableWidth < 600
      ? 2
      : usableWidth < 840
      ? 3
      : usableWidth < 1200
      ? 4
      : 5;
  const cardWidth = (usableWidth - 32 - (columns - 1) * 12) / columns;
  const menuWidth = Math.min(232, usableWidth - 32);
  const menuMaxHeight = Math.max(
    48,
    window.height - insets.top - insets.bottom - 32,
  );
  const menuHeight = Math.min(
    menuMaxHeight,
    5 * (48 + 24 * Math.max(0, window.fontScale - 1)) + 32,
  );
  const menuTop = Math.max(
    insets.top + 8,
    Math.min(menuPoint.y, window.height - insets.bottom - menuHeight - 16),
  );
  const menuAvailableHeight = Math.max(
    48,
    window.height - insets.bottom - menuTop - 16,
  );
  const header = (
    <View style={styles.header}>
      <View style={styles.heading}>
        <Text style={styles.title}>Kitaplığım</Text>
        <View style={styles.headerActions}>
          <Action label="PDF ekle" onPress={() => pick()} disabled={!!busy} />
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Ayarlar"
            onPress={openSettings}
            disabled={!!busy}
            style={styles.settingsButton}
          >
            <Text accessible={false} style={styles.settingsGlyph}>
              {'\u2699\uFE0E'}
            </Text>
          </Pressable>
        </View>
      </View>
      <>
        <TextInput
          accessibilityLabel="Kitap adında ara"
          placeholder="Kitap adında ara"
          value={text}
          onChangeText={setText}
          maxLength={200}
          style={styles.input}
        />
        <View style={styles.heading}>
          <View style={styles.filters}>
            {[
              { label: 'Tümü', value: false },
              { label: 'Favoriler', value: true },
            ].map(({ label, value }) => (
              <Pressable
                key={label}
                accessibilityRole="button"
                accessibilityState={{ selected: favoritesOnly === value }}
                onPress={() => choosePreferences(sort, value)}
                style={[
                  styles.filter,
                  favoritesOnly === value && styles.activeFilter,
                ]}
              >
                <Text style={styles.filterText}>{label}</Text>
              </Pressable>
            ))}
          </View>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Sıralama seçenekleri"
            onPress={() => setSortOpen(true)}
            style={styles.sort}
          >
            <Text style={styles.caption}>
              ↕ {sort === 'title' ? 'Kitap adı A–Z' : 'Son okunan'}
            </Text>
          </Pressable>
        </View>
        {recent && !query && !favoritesOnly && (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Son kitaba devam et"
            onPress={() => open(recent)}
            style={styles.recent}
          >
            <Text style={styles.caption}>Kaldığın yerden devam et</Text>
            <Text numberOfLines={1} style={styles.recentTitle}>
              {recent.title} · ({recent.pageIndex + 1}/{recent.pageCount}) →
            </Text>
          </Pressable>
        )}
      </>
      {busy === 'import' && (
        <Action
          label="Eklemeyi iptal et"
          onPress={cancel}
          disabled={cancelRequested}
        />
      )}
      {!!message && (
        <Text accessibilityLiveRegion="polite" style={styles.message}>
          {message}
        </Text>
      )}
    </View>
  );
  return (
    <View
      style={[
        styles.screen,
        {
          paddingTop: insets.top,
          paddingBottom: insets.bottom,
          paddingLeft: insets.left,
          paddingRight: insets.right,
        },
      ]}
    >
      <FlatList
        style={styles.scroller}
        key={columns}
        keyboardShouldPersistTaps="handled"
        data={books}
        numColumns={columns}
        keyExtractor={item => item.id}
        ListHeaderComponent={header}
        contentContainerStyle={styles.list}
        columnWrapperStyle={columns > 1 ? styles.row : undefined}
        initialNumToRender={6}
        maxToRenderPerBatch={6}
        windowSize={3}
        onViewableItemsChanged={onVisible}
        viewabilityConfig={{ itemVisiblePercentThreshold: 20 }}
        refreshing={false}
        onRefresh={() => load(true)}
        onEndReachedThreshold={0.4}
        onEndReached={() => {
          if (hasMore) load(false);
        }}
        ListFooterComponent={
          hasMore ? (
            <Action label="Daha fazla kitap" onPress={() => load(false)} />
          ) : undefined
        }
        renderItem={({ item }) => (
          <View style={[styles.book, { width: cardWidth }]}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={`${item.title}, ${item.pageIndex + 1} / ${
                item.pageCount
              }. Okumaya devam et`}
              disabled={!!busy}
              onPress={() => open(item)}
              style={styles.open}
            >
              <View style={styles.coverFrame}>
                <Thumbnail
                  style={styles.cover}
                  bookId={item.id}
                  pageIndex={0}
                  enabled={focused && visible.has(item.id)}
                />
              </View>
              <Text
                numberOfLines={2}
                style={[styles.bookTitle, { minHeight: 40 * window.fontScale }]}
              >
                {item.title}
              </Text>
              <Text style={styles.caption}>
                ({item.pageIndex + 1}/{item.pageCount})
                {item.favorite ? '  ★' : ''}
              </Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={`${item.title}, kitap menüsü`}
              disabled={!!busy}
              onPress={event => {
                setMenuPoint({
                  x: event.nativeEvent.pageX,
                  y: event.nativeEvent.pageY,
                });
                setMenuBook(item);
              }}
              style={styles.more}
            >
              <Text style={styles.dots}>⋮</Text>
            </Pressable>
          </View>
        )}
      />
      <Modal
        visible={!!menuBook}
        transparent
        animationType="fade"
        onRequestClose={() => setMenuBook(null)}
      >
        <View style={styles.modalRoot}>
          <Pressable
            accessibilityLabel="Menüyü kapat"
            style={StyleSheet.absoluteFill}
            onPress={() => setMenuBook(null)}
          />
          <View
            accessibilityViewIsModal
            style={[
              styles.menu,
              {
                left: Math.max(
                  insets.left + 16,
                  Math.min(
                    menuPoint.x - menuWidth + 16,
                    window.width - insets.right - menuWidth - 16,
                  ),
                ),
                top: menuTop,
              },
            ]}
          >
            <ScrollView
              style={[
                styles.menuScroller,
                { maxHeight: menuAvailableHeight, width: menuWidth },
              ]}
              contentContainerStyle={styles.menuContent}
              keyboardShouldPersistTaps="handled"
            >
              <Action
                label="Notlar"
                onPress={() => {
                  setNotesBook(menuBook);
                  setMenuBook(null);
                }}
                disabled={!!busy}
              />
              <Action
                label="Altı çizilenler"
                onPress={() => {
                  setAnnotationBook(menuBook);
                  setMenuBook(null);
                }}
                disabled={!!busy}
              />
              <Action
                label={menuBook?.favorite ? 'Favoriden çıkar' : 'Favoriye ekle'}
                onPress={() => {
                  if (menuBook) favorite(menuBook);
                }}
                disabled={!!busy}
              />
              <Action
                label="Adı düzenle"
                onPress={() => {
                  if (!menuBook) return;
                  setRenaming(menuBook);
                  setName(menuBook.title);
                  setRenameError('');
                  setMenuBook(null);
                }}
                disabled={!!busy}
              />
              <Action
                label="Kaldır"
                onPress={() => {
                  const book = menuBook;
                  setMenuBook(null);
                  if (book) remove(book);
                }}
                disabled={!!busy}
              />
            </ScrollView>
          </View>
        </View>
      </Modal>
      <Modal
        visible={sortOpen}
        transparent
        animationType="fade"
        onRequestClose={() => setSortOpen(false)}
      >
        <View style={styles.overlay}>
          <Pressable
            style={StyleSheet.absoluteFill}
            accessibilityLabel="Sıralamayı kapat"
            onPress={() => setSortOpen(false)}
          />
          <View accessibilityViewIsModal style={styles.dialog}>
            <Text style={styles.bookTitle}>Sırala</Text>
            <Action
              label="Son okunan"
              selected={sort === 'recent'}
              onPress={() => choosePreferences('recent', favoritesOnly)}
            />
            <Action
              label="Kitap adı A–Z"
              selected={sort === 'title'}
              onPress={() => choosePreferences('title', favoritesOnly)}
            />
          </View>
        </View>
      </Modal>
      {readerSettings && (
        <ReaderSettings
          options={readerSettings}
          close={() => setReaderSettings(null)}
          save={async value => {
            await PdfEngine.saveReaderDefaults(JSON.stringify(value));
            setReaderSettings(null);
            setMessage('Okuma tercihleri kaydedildi.');
          }}
        />
      )}
      {annotationBook && (
        <AnnotationsPanel
          bookId={annotationBook.id}
          close={() => setAnnotationBook(null)}
          jump={jumpAnnotation}
        />
      )}
      {notesBook && (
        <NotesPanel
          bookId={notesBook.id}
          current={notesBook.pageIndex}
          close={() => setNotesBook(null)}
          jump={jumpNote}
        />
      )}
      <Modal
        visible={!!renaming}
        transparent
        animationType="fade"
        onRequestClose={() => {
          if (!busy) setRenaming(null);
        }}
      >
        <KeyboardAvoidingView behavior="height" style={styles.overlay}>
          <ScrollView
            contentContainerStyle={styles.dialogContent}
            keyboardShouldPersistTaps="handled"
          >
            <View style={styles.dialog} accessibilityViewIsModal>
              <Text style={styles.bookTitle}>Kitap adı</Text>
              <TextInput
                accessibilityLabel="Yeni kitap adı"
                value={name}
                onChangeText={value => {
                  setName(value);
                  setRenameError('');
                }}
                editable={!busy}
                maxLength={300}
                autoFocus
                style={styles.input}
              />
              {!!renameError && (
                <Text accessibilityLiveRegion="polite" style={styles.message}>
                  {renameError}
                </Text>
              )}
              <View style={styles.actions}>
                <Action
                  label="Vazgeç"
                  onPress={() => setRenaming(null)}
                  disabled={!!busy}
                />
                <Action
                  label="Adı kaydet"
                  onPress={rename}
                  disabled={!!busy || !name.trim()}
                />
              </View>
            </View>
          </ScrollView>
        </KeyboardAvoidingView>
      </Modal>
    </View>
  );
}
const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#F5F3ED' },
  scroller: { flex: 1 },
  list: { paddingHorizontal: 16, paddingBottom: 24 },
  row: { gap: 12 },
  header: { paddingVertical: 20, gap: 16 },
  heading: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 12,
    flexWrap: 'wrap',
  },
  title: { fontSize: 28, fontWeight: '600', color: '#28362E' },
  caption: { fontSize: 13, color: '#677269', lineHeight: 20 },
  message: { fontSize: 14, color: '#435747', lineHeight: 21 },
  filters: { flexDirection: 'row', gap: 4 },
  filter: {
    minHeight: 48,
    justifyContent: 'center',
    paddingHorizontal: 14,
    borderRadius: 24,
  },
  activeFilter: { backgroundColor: '#DCE5D8' },
  filterText: { fontSize: 14, color: '#334C39' },
  sort: { minHeight: 48, justifyContent: 'center' },
  recent: { padding: 14, borderRadius: 12, backgroundColor: '#E9EDE4', gap: 4 },
  recentTitle: { fontSize: 15, color: '#334C39', fontWeight: '500' },
  book: { marginBottom: 20, gap: 12 },
  open: { gap: 8 },
  coverFrame: {
    width: '100%',
    aspectRatio: 0.7,
    backgroundColor: '#E4E3DB',
    borderRadius: 8,
    overflow: 'hidden',
  },
  cover: { width: '100%', height: '100%' },
  bookTitle: {
    fontSize: 15,
    lineHeight: 20,
    fontWeight: '500',
    color: '#28362E',
  },
  more: {
    position: 'absolute',
    top: 4,
    right: 4,
    width: 48,
    height: 48,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#F9F8F0ED',
    borderRadius: 24,
  },
  dots: { fontSize: 28, color: '#28362E' },
  input: {
    minHeight: 48,
    padding: 12,
    borderWidth: 1,
    borderColor: '#C9CEC3',
    borderRadius: 12,
    color: '#28362E',
    fontSize: 16,
    backgroundColor: '#FCFBF7',
  },
  headerActions: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    gap: 8,
  },
  settingsButton: {
    minWidth: 48,
    minHeight: 48,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#DCE3D7',
  },
  settingsGlyph: { fontSize: 26, color: '#334C39' },
  actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  modalRoot: { flex: 1, backgroundColor: '#00000022' },
  menu: {
    position: 'absolute',
    borderRadius: 14,
    overflow: 'hidden',
    backgroundColor: '#FAF9F4',
    elevation: 6,
  },
  menuScroller: { flexGrow: 0 },
  menuContent: { padding: 8, gap: 4 },
  overlay: {
    flex: 1,
    backgroundColor: '#00000055',
    padding: 24,
    justifyContent: 'center',
  },
  dialogContent: { flexGrow: 1, justifyContent: 'center' },
  dialog: {
    padding: 24,
    backgroundColor: '#F5F3ED',
    borderRadius: 16,
    gap: 16,
    width: '100%',
    maxWidth: 480,
    alignSelf: 'center',
  },
});
