import React, { useEffect, useRef, useState } from 'react';
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

export type NoteCard = {
  id: string;
  scope: string;
  pageIndex: number;
  annotationId: string;
  quote: string;
  title: string;
  body: string;
  revision: number;
  updatedAt: number;
  hasDraft: boolean;
  draftOnly: boolean;
  annotationDeleted: boolean;
};
export function NoteList({
  bookId,
  dark = false,
  jump,
  current = -1,
  changed,
}: {
  bookId: string;
  dark?: boolean;
  jump: (note: NoteCard) => void | Promise<void>;
  current?: number;
  changed?: () => void;
}) {
  const [items, setItems] = useState<NoteCard[]>([]);
  const [text, setText] = useState('');
  const [query, setQuery] = useState('');
  const [scope, setScope] = useState('');
  const [sort, setSort] = useState('date');
  const [offset, setOffset] = useState(0);
  const [more, setMore] = useState(false);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const operation = useRef(false);
  const [error, setError] = useState('');
  const [refresh, setRefresh] = useState(0);
  const [editor, setEditor] = useState<NoteDraft | null>(null);
  const [undo, setUndo] = useState<NoteCard | null>(null);
  const alive = useRef(true);
  useEffect(() => { alive.current=true;return () => {alive.current=false;};},[]);
  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery(text.trim());
      setOffset(0);
    }, 220);
    return () => clearTimeout(timer);
  }, [text]);
  useEffect(() => {
    setOffset(0);
  }, [scope, sort]);
  useEffect(() => {
    let currentRequest = true;
    setLoading(true);
    setError('');
    PdfEngine.listNotes(bookId, query, scope, sort, offset, 40)
      .then(value => {
        if (currentRequest) {
          const batch: NoteCard[] = JSON.parse(value);
          setItems(batch);
          setMore(batch.length === 40);
        }
      })
      .catch(issue => {
        if (currentRequest) setError(errorMessage(issue));
      })
      .finally(() => {
        if (currentRequest) setLoading(false);
      });
    return () => {
      currentRequest = false;
    };
  }, [bookId, query, scope, sort, offset, refresh]);
  async function edit(kind: string, page = -1, annotationId = '', id = '') {
    if (operation.current) return;
    operation.current = true;
    setBusy(true);
    setError('');
    try {
      const draft = await openNote(bookId, kind, page, annotationId, id);
      if (alive.current) setEditor(draft);
    } catch (issue) {
      if (alive.current) setError(errorMessage(issue));
    } finally {
      operation.current = false;
      if (alive.current) setBusy(false);
    }
  }
  async function change(item: NoteCard, deleted: boolean) {
    if (operation.current) return;
    operation.current = true;
    setBusy(true);
    setError('');
    try {
      const ack: { revision: number } = JSON.parse(
        await PdfEngine.changeNoteDeleted(
          bookId,
          item.id,
          item.revision,
          item.draftOnly,
          deleted,
        ),
      );
      if (!alive.current) return;
      setUndo(deleted ? { ...item, revision: ack.revision } : null);
      setRefresh(value => value + 1);
      changed?.();
    } catch (issue) {
      if (alive.current) setError(errorMessage(issue));
    } finally {
      operation.current = false;
      if (alive.current) setBusy(false);
    }
  }
  const ink = dark ? styles.light : undefined;
  return (
    <ReaderToneContext.Provider value={dark}>
      <View style={styles.fill}>
        <ScrollView style={styles.filters} keyboardShouldPersistTaps="handled">
          <View style={styles.actions}>
            <Action
              label="Kitaba not ekle"
              disabled={busy}
              onPress={() => edit('book')}
            />
            {current >= 0 && (
              <Action
                label={`Sayfa ${current + 1} için not ekle`}
                disabled={busy}
                onPress={() => edit('page', current)}
              />
            )}
          </View>
          <TextInput
            accessibilityLabel="Notlarda ara"
            placeholder="Başlık ve notlarda ara"
            placeholderTextColor={dark ? '#aab0ae' : '#667064'}
            value={text}
            onChangeText={setText}
            maxLength={200}
            style={[styles.input, ink]}
          />
          <View style={styles.actions}>
            {[
              ['', 'Tüm notlar'],
              ['book', 'Kitap notları'],
              ['page', 'Sayfa notları'],
              ['passage', 'Pasaj notları'],
              ['draft', 'Taslaklar'],
            ].map(([value, label]) => (
              <Action
                key={label}
                label={label}
                selected={scope === value}
                onPress={() => setScope(value)}
              />
            ))}
          </View>
          <View style={styles.actions}>
            <Action
              label="Son değişen notlar"
              selected={sort === 'date'}
              onPress={() => setSort('date')}
            />
            <Action
              label="Notları sayfaya göre sırala"
              selected={sort === 'page'}
              onPress={() => setSort('page')}
            />
            <Action
              label="Notları yenile"
              disabled={busy}
              onPress={() => setRefresh(value => value + 1)}
            />
          </View>
          {!!error && (
            <Text accessibilityLiveRegion="polite" style={[styles.text, ink]}>
              {error}
            </Text>
          )}
          {undo && (
            <View style={styles.actions}>
              <Text style={[styles.text, ink]}>Not silindi.</Text>
              <Action
                label="Notu geri al"
                disabled={busy}
                onPress={() => change(undo, false)}
              />
            </View>
          )}
          {loading && (
            <Text style={[styles.text, ink]}>Notlar yükleniyor…</Text>
          )}
        </ScrollView>
        <FlatList
          style={styles.fill}
          data={items}
          keyExtractor={item => item.id}
          initialNumToRender={6}
          maxToRenderPerBatch={6}
          windowSize={3}
          ListEmptyComponent={
            !loading ? (
              <Text style={[styles.text, ink]}>Bu filtrede not yok.</Text>
            ) : undefined
          }
          renderItem={({ item }) => (
            <View style={styles.card}>
              <Text style={[styles.text, ink]}>
                {item.scope === 'book'
                  ? 'Kitap geneli'
                  : `Sayfa ${item.pageIndex + 1} · ${
                      item.scope === 'passage' ? 'Pasaj' : 'Sayfa'
                    } notu`}
                {item.hasDraft ? ' · Taslak var' : ''}
                {item.annotationDeleted ? ' · Bağlı işaret silinmiş' : ''}
              </Text>
              <Text style={[styles.title, ink]}>
                {item.title || 'Başlıksız not'}
              </Text>
              <Text numberOfLines={4} style={[styles.text, ink]}>
                {item.body}
              </Text>
              {!!item.quote && (
                <Text numberOfLines={3} style={[styles.quote, ink]}>
                  Alıntı: {item.quote}
                </Text>
              )}
              <Text style={[styles.text, ink]}>
                {new Date(item.updatedAt).toLocaleDateString('tr-TR')}
              </Text>
              <View style={styles.actions}>
                <Action
                  label={item.hasDraft ? 'Taslağı aç' : 'Notu aç'}
                  disabled={busy || loading}
                  onPress={() =>
                    edit(item.scope, item.pageIndex, item.annotationId, item.id)
                  }
                />
                {item.pageIndex >= 0 && (
                  <Action
                    label={`Notun konumuna git · Sayfa ${item.pageIndex + 1}`}
                    disabled={busy || loading}
                    onPress={async () => {
                      try {
                        await jump(item);
                      } catch (issue) {
                        if (alive.current) setError(errorMessage(issue));
                      }
                    }}
                  />
                )}
                <Action
                  label="Notu sil"
                  disabled={busy || loading}
                  onPress={() =>
                    Alert.alert(
                      'Notu sil',
                      'Bu not ve varsa taslağı listeden kaldırılacak. Geri al düğmesiyle geri getirebilirsiniz.',
                      [
                        { text: 'Vazgeç', style: 'cancel' },
                        {
                          text: 'Sil',
                          style: 'destructive',
                          onPress: () => change(item, true),
                        },
                      ],
                    )
                  }
                />
              </View>
            </View>
          )}
        />
        <View style={styles.actions}>
          <Action
            label="Önceki notlar"
            disabled={offset === 0 || busy || loading}
            onPress={() => setOffset(value => Math.max(0, value - 40))}
          />
          <Text style={[styles.text, ink]}>
            {items.length ? `${offset + 1}–${offset + items.length}` : '0 not'}
          </Text>
          <Action
            label="Sonraki notlar"
            disabled={!more || busy || loading}
            onPress={() => setOffset(value => value + 40)}
          />
        </View>
        {editor && (
          <NoteEditor
            bookId={bookId}
            initial={editor}
            dark={dark}
            closed={() => {
              setEditor(null);
              setRefresh(value => value + 1);
              changed?.();
            }}
          />
        )}
      </View>
    </ReaderToneContext.Provider>
  );
}
export function NotesPanel({
  bookId,
  dark = false,
  current = -1,
  close,
  jump,
  changed,
}: {
  bookId: string;
  dark?: boolean;
  current?: number;
  close: () => void;
  jump: (note: NoteCard) => void | Promise<void>;
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
              <Text style={[styles.heading, dark && styles.light]}>Notlar</Text>
              <Action label="Not panelini kapat" onPress={close} />
            </View>
            <NoteList
              bookId={bookId}
              dark={dark}
              current={current}
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
  text: { fontSize: 15, lineHeight: 23, color: '#354436' },
  title: { fontSize: 19, color: '#354436' },
  quote: { fontSize: 15, lineHeight: 23, color: '#586458' },
  card: { padding: 10, borderBottomWidth: 1, borderColor: '#7b8579', gap: 8 },
  overlay: { flex: 1, backgroundColor: '#00000066' },
  panel: {
    flex: 1,
    alignSelf: 'center',
    width: '100%',
    maxWidth: 600,
    backgroundColor: '#f0ede5',
    padding: 12,
  },
  heading: { fontSize: 24, color: '#354436' },
  dark: { backgroundColor: '#16191c' },
  light: { color: '#dadbd7' },
});
