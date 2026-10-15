import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Alert,
  FlatList,
  Modal,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import PdfEngine from '../specs/NativeKitapPdf';
import { Action, ReaderToneContext } from './controls';
import { errorMessage } from './types';
import { NoteEditor, openNote } from './NoteEditor';
import type { NoteDraft } from './NoteEditor';

export type Annotation = {
  id: string;
  pageIndex: number;
  quote: string;
  kind: string;
  color: string;
  lineCount: number;
  provenance: string;
  createdAt: number;
  revision: number;
  deleted: boolean;
  noteCount?: number;
};
export const MARK_COLORS = [{ name: 'Sarı', value: '#FFEB3B' }];

export function AnnotationList({
  bookId,
  dark = false,
  jump,
  changed,
}: {
  bookId: string;
  dark?: boolean;
  jump: (mark: Annotation) => void | Promise<void>;
  changed?: () => void;
}) {
  const [items, setItems] = useState<Annotation[]>([]);
  const [text, setText] = useState('');
  const [query, setQuery] = useState('');
  const [kind, setKind] = useState('');
  const color = '';
  const [sort, setSort] = useState('page');
  const [offset, setOffset] = useState(0);
  const [more, setMore] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [refresh, setRefresh] = useState(0);
  const [editing, setEditing] = useState<Annotation | null>(null);
  const [undo, setUndo] = useState<Annotation | null>(null);
  const [noteEditor, setNoteEditor] = useState<NoteDraft | null>(null);
  async function editNote(item: Annotation) {
    if (write.current) return;
    write.current = true;
    setBusy(true);
    setError('');
    try {
      const draft = await openNote(bookId, 'passage', item.pageIndex, item.id);
      if (alive.current) setNoteEditor(draft);
    } catch (issue) {
      if (alive.current) setError(errorMessage(issue));
    } finally {
      write.current = false;
      if (alive.current) setBusy(false);
    }
  }
  async function askDelete(item: Annotation) {
    if (write.current) return;
    write.current = true;
    setBusy(true);
    let detail: Annotation;
    try {
      detail = JSON.parse(await PdfEngine.getAnnotation(bookId, item.id));
    } catch (issue) {
      if (alive.current) setError(errorMessage(issue));
      return;
    } finally {
      write.current = false;
      if (alive.current) setBusy(false);
    }
    if (!alive.current) return;
    if (!detail.noteCount) {
      await change(item, item.kind, item.color, true);
      return;
    }
    Alert.alert(
      'Notlu işareti sil',
      `Bu pasajda ${detail.noteCount} not veya taslak var.`,
      [
        { text: 'Vazgeç', style: 'cancel' },
        {
          text: 'İşareti sil, notları koru',
          onPress: () => change(item, item.kind, item.color, true, false),
        },
        {
          text: 'İşaret ve notları sil',
          style: 'destructive',
          onPress: () => change(item, item.kind, item.color, true, true),
        },
      ],
    );
  }
  const [busy, setBusy] = useState(false);
  const write = useRef(false);
  const alive = useRef(true);
  useEffect(
    () => () => {
      alive.current = false;
    },
    [],
  );
  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery(text.trim());
      setOffset(0);
    }, 220);
    return () => clearTimeout(timer);
  }, [text]);
  useEffect(() => {
    setOffset(0);
  }, [kind, color, sort]);
  const load = useCallback(async () => {
    setRefresh(value => value + 1);
  }, []);
  useEffect(() => {
    let current = true;
    setLoading(true);
    setError('');
    setEditing(null);
    PdfEngine.listAnnotations(bookId, query, kind, color, sort, offset, 40)
      .then(value => {
        if (current) {
          const batch: Annotation[] = JSON.parse(value);
          setItems(batch);
          setMore(batch.length === 40);
        }
      })
      .catch(issue => {
        if (current) setError(errorMessage(issue));
      })
      .finally(() => {
        if (current) setLoading(false);
      });
    return () => {
      current = false;
    };
  }, [bookId, query, kind, color, sort, offset, refresh]);
  async function change(
    item: Annotation,
    nextKind: string,
    nextColor: string,
    deleted: boolean,
    deleteNotes?: boolean,
  ) {
    if (write.current) return;
    write.current = true;
    setBusy(true);
    setError('');
    try {
      const saved: Annotation = JSON.parse(
        deleted && deleteNotes !== undefined
          ? await PdfEngine.deleteAnnotation(
              bookId,
              item.id,
              item.revision,
              deleteNotes,
            )
          : await PdfEngine.changeAnnotation(
              bookId,
              item.id,
              item.revision,
              nextKind,
              nextColor,
              deleted,
            ),
      );
      if (!alive.current) return;
      if (deleted) setUndo(saved);
      else if (undo?.id === saved.id) setUndo(null);
      setEditing(null);
      changed?.();
      setRefresh(value => value + 1);
    } catch (issue) {
      if (alive.current) setError(errorMessage(issue));
    } finally {
      write.current = false;
      if (alive.current) setBusy(false);
    }
  }
  const ink = dark ? styles.light : undefined;
  return (
    <ReaderToneContext.Provider value={dark}>
      <View style={styles.fill}>
        <ScrollView style={styles.filters} keyboardShouldPersistTaps="handled">
          <TextInput
            accessibilityLabel="Kayıtlı alıntılarda ara"
            placeholder="Kayıtlı alıntılarda ara"
            placeholderTextColor={dark ? '#aab0ae' : '#667064'}
            value={text}
            maxLength={200}
            onChangeText={setText}
            style={[styles.input, ink]}
          />
          <View style={styles.actions}>
            {[
              ['', 'Tümü'],

              ['highlight', 'Sarı işaretler'],
              ['region', 'Bölgeler'],
            ].map(([value, label]) => (
              <Action
                key={label}
                label={label}
                selected={kind === value}
                onPress={() => setKind(value)}
              />
            ))}
          </View>
          <View style={styles.actions}>
            <Action
              label="Sayfa sırası"
              selected={sort === 'page'}
              onPress={() => setSort('page')}
            />
            <Action
              label="Yeni kayıtlar"
              selected={sort === 'date'}
              onPress={() => setSort('date')}
            />
            <Action label="Kayıtları yenile" onPress={load} disabled={busy} />
          </View>
          {!!error && (
            <Text accessibilityLiveRegion="polite" style={[styles.text, ink]}>
              {error}
            </Text>
          )}
          {undo && (
            <View style={styles.actions}>
              <Text style={[styles.text, ink]}>İşaret silindi.</Text>
              <Action
                label="Silmeyi geri al"
                disabled={busy}
                onPress={() => change(undo, undo.kind, undo.color, false)}
              />
            </View>
          )}
          {loading && (
            <Text style={[styles.text, ink]}>Kayıtlar yükleniyor…</Text>
          )}
        </ScrollView>
        <FlatList
          data={items}
          keyExtractor={item => item.id}
          initialNumToRender={6}
          maxToRenderPerBatch={6}
          windowSize={3}
          style={styles.fill}
          ListEmptyComponent={
            !loading ? (
              <Text style={[styles.text, ink]}>
                Bu filtrede kayıt yok. Metin seçerek sarı işaret ekleyebilirsin.
              </Text>
            ) : undefined
          }
          renderItem={({ item }) => (
            <View style={styles.card}>
              <Text style={[styles.text, ink]}>
                Sayfa {item.pageIndex + 1} ·{' '}
                {item.provenance === 'manual-region'
                  ? 'Elle seçilen bölge'
                  : `${item.lineCount} satır parçası`}{' '}
                ·{' '}
                {item.kind === 'underline'
                  ? 'Altı çizili'
                  : item.kind === 'highlight'
                  ? 'Sarı fosforlu'
                  : 'Bölge'}
              </Text>
              <Text style={[styles.text, ink]}>
                {MARK_COLORS.find(c => c.value === item.color)?.name ??
                  item.color}{' '}
                · {new Date(item.createdAt).toLocaleDateString('tr-TR')}
              </Text>
              <Text
                numberOfLines={editing?.id === item.id ? undefined : 5}
                style={[styles.quote, ink]}
              >
                {editing?.id === item.id
                  ? editing.quote || 'Metin içermeyen bölge işareti'
                  : item.quote || 'Metin içermeyen bölge işareti'}
              </Text>
              <View style={styles.actions}>
                <Action
                  label="Pasaj notunu aç"
                  disabled={busy || loading}
                  onPress={() => editNote(item)}
                />
                <Action
                  label={`Konuma git · Sayfa ${item.pageIndex + 1}`}
                  onPress={async () => {
                    try {
                      await jump(item);
                    } catch (issue) {
                      if (alive.current) setError(errorMessage(issue));
                    }
                  }}
                  disabled={busy || loading}
                />
                <Action
                  label="İşareti düzenle"
                  onPress={() => setEditing(item)}
                  disabled={busy}
                />
              </View>
              {editing?.id === item.id && (
                <View style={styles.edit}>
                  <Action
                    label="Alıntının tamamını göster"
                    disabled={busy}
                    onPress={async () => {
                      try {
                        const detail: Annotation = JSON.parse(
                          await PdfEngine.getAnnotation(bookId, item.id),
                        );
                        if (alive.current) setEditing(detail);
                      } catch (issue) {
                        if (alive.current) setError(errorMessage(issue));
                      }
                    }}
                  />
                  <View style={styles.actions}>
                    <Action
                      label="İşareti sil"
                      disabled={busy}
                      onPress={() => askDelete(item)}
                    />
                    <Action
                      label="Düzenlemeyi kapat"
                      disabled={busy}
                      onPress={() => setEditing(null)}
                    />
                  </View>
                  <Action
                    label="Alıntıyı kopyala"
                    disabled={busy || !item.quote}
                    onPress={async () => {
                      try {
                        const detail: Annotation = JSON.parse(
                          await PdfEngine.getAnnotation(bookId, item.id),
                        );
                        await PdfEngine.copyText(detail.quote);
                      } catch (issue) {
                        if (alive.current) setError(errorMessage(issue));
                      }
                    }}
                  />
                </View>
              )}
            </View>
          )}
        />
        {noteEditor && (
          <NoteEditor
            bookId={bookId}
            initial={noteEditor}
            dark={dark}
            closed={() => {
              setNoteEditor(null);
              setRefresh(value => value + 1);
              changed?.();
            }}
          />
        )}
        <View style={styles.actions}>
          <Action
            label="Önceki kayıtlar"
            disabled={offset === 0 || busy || loading}
            onPress={() => setOffset(value => Math.max(0, value - 40))}
          />
          <Text style={[styles.text, ink]}>
            {items.length
              ? `${offset + 1}–${offset + items.length}`
              : '0 kayıt'}
          </Text>
          <Action
            label="Sonraki kayıtlar"
            disabled={!more || busy || loading}
            onPress={() => setOffset(value => value + 40)}
          />
        </View>
      </View>
    </ReaderToneContext.Provider>
  );
}

export function AnnotationsPanel({
  bookId,
  dark = false,
  close,
  jump,
  changed,
}: {
  bookId: string;
  dark?: boolean;
  close: () => void;
  jump: (mark: Annotation) => void | Promise<void>;
  changed?: () => void;
}) {
  const insets = useSafeAreaInsets();
  return (
    <Modal visible transparent animationType="none" onRequestClose={close}>
      <ReaderToneContext.Provider value={dark}>
        <View
          style={[
            styles.overlay,
            {
              paddingTop: insets.top,
              paddingBottom: insets.bottom,
              paddingLeft: insets.left,
              paddingRight: insets.right,
            },
          ]}
        >
          <View
            style={[styles.panel, dark && styles.dark]}
            accessibilityViewIsModal
          >
            <View style={styles.actions}>
              <Text style={[styles.title, dark && styles.light]}>
                Altı çizilenler
              </Text>
              <Action label="Kayıt panelini kapat" onPress={close} />
            </View>
            <AnnotationList
              bookId={bookId}
              dark={dark}
              jump={jump}
              changed={changed}
            />
          </View>
        </View>
      </ReaderToneContext.Provider>
    </Modal>
  );
}
const styles = StyleSheet.create({
  fill: { flex: 1 },
  filters: { flexGrow: 0, maxHeight: '42%' },
  actions: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
    alignItems: 'center',
  },
  input: {
    minHeight: 48,
    borderWidth: 1,
    borderColor: '#7b8579',
    borderRadius: 6,
    padding: 10,
    color: '#354436',
    marginVertical: 8,
  },
  text: { fontSize: 15, color: '#354436', paddingVertical: 6 },
  quote: { fontSize: 17, lineHeight: 25, color: '#354436' },
  card: { padding: 10, borderBottomWidth: 1, borderColor: '#7b8579', gap: 8 },
  edit: { gap: 8 },
  overlay: { flex: 1, backgroundColor: '#00000066' },
  panel: {
    flex: 1,
    alignSelf: 'center',
    width: '100%',
    maxWidth: 600,
    backgroundColor: '#f0ede5',
    padding: 12,
  },
  title: { fontSize: 24, color: '#354436' },
  dark: { backgroundColor: '#16191c' },
  light: { color: '#dadbd7' },
});
