import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  AppState,
  Keyboard,
  KeyboardAvoidingView,
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

export type NoteDraft = {
  id: string;
  scope: string;
  pageIndex: number;
  annotationId: string;
  quote: string;
  title: string;
  body: string;
  editorToken: string;
  contentRevision: number;
  baseRevision: number;
  dirty: boolean;
  recovered: boolean;
};
export async function openNote(
  bookId: string,
  scope: string,
  page = -1,
  annotationId = '',
  id = '',
): Promise<NoteDraft> {
  return JSON.parse(
    await PdfEngine.openNoteEditor(bookId, id, scope, page, annotationId),
  );
}

export function NoteEditor({
  bookId,
  initial,
  dark = false,
  closed,
}: {
  bookId: string;
  initial: NoteDraft;
  dark?: boolean;
  closed: (saved: boolean) => void;
}) {
  const insets = useSafeAreaInsets();
  const [title, setTitle] = useState(initial.title);
  const [body, setBody] = useState(initial.body);
  const [status, setStatus] = useState(
    initial.recovered ? 'Son kaydedilmiş taslak kurtarıldı.' : '',
  );
  const [busy, setBusy] = useState(false);
  const busyRef = useRef(false);
  const alive = useRef(true);
  const finished = useRef(false);
  const keyboard = useRef(false);
  const latest = useRef({
    title: initial.title,
    body: initial.body,
    revision: initial.contentRevision,
  });
  const acknowledged = useRef(initial.contentRevision);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const flush = useCallback(async () => {
    if (finished.current) return;
    const value = latest.current;
    if (value.revision <= acknowledged.current) return;
    try {
      const ack: { contentRevision: number } = JSON.parse(
        await PdfEngine.writeNoteDraft(
          bookId,
          initial.id,
          initial.editorToken,
          value.revision,
          value.title,
          value.body,
        ),
      );
      acknowledged.current = Math.max(
        acknowledged.current,
        ack.contentRevision,
      );
      if (
        alive.current &&
        !finished.current &&
        !busyRef.current &&
        latest.current.revision === ack.contentRevision
      )
        setStatus('Taslak kaydedildi.');
    } catch (issue) {
      if (
        alive.current &&
        !finished.current &&
        !busyRef.current &&
        latest.current.revision === value.revision
      )
        setStatus(
          `${errorMessage(issue)} Metin editörde korunuyor; yeniden deneyin.`,
        );
      throw issue;
    }
  }, [bookId, initial.id, initial.editorToken]);
  useEffect(() => {
    alive.current = true;
    const background = AppState.addEventListener('change', state => {
      if (state !== 'active') {
        if (timer.current) clearTimeout(timer.current);
        flush().catch(() => {});
      }
    });
    const show = Keyboard.addListener('keyboardDidShow', () => {
      keyboard.current = true;
    });
    const hide = Keyboard.addListener('keyboardDidHide', () => {
      keyboard.current = false;
    });
    return () => {
      alive.current = false;
      if (timer.current) clearTimeout(timer.current);
      // An abrupt OS kill can skip cleanup; only a completed database draft is guaranteed.
      flush().catch(() => {});
      background.remove();
      show.remove();
      hide.remove();
    };
  }, [flush]);
  function edit(field: 'title' | 'body', value: string) {
    if (busyRef.current || finished.current) return;
    latest.current = {
      ...latest.current,
      [field]: value,
      revision: latest.current.revision + 1,
    };
    if (field === 'title') setTitle(value);
    else setBody(value);
    setStatus('Taslak kaydı bekleniyor…');
    if (timer.current) clearTimeout(timer.current);
    timer.current = setTimeout(() => {
      flush().catch(() => {});
    }, 650);
  }
  async function exit(save: boolean) {
    if (busyRef.current || finished.current) return;
    busyRef.current = true;
    setBusy(true);
    if (timer.current) clearTimeout(timer.current);
    Keyboard.dismiss();
    const value = latest.current;
    setStatus(save ? 'Not kaydediliyor…' : 'Taslak saklanıyor…');
    try {
      if (save)
        await PdfEngine.saveNote(
          bookId,
          initial.id,
          initial.editorToken,
          value.revision,
          value.title,
          value.body,
        );
      else
        await PdfEngine.endNoteEditor(
          bookId,
          initial.id,
          initial.editorToken,
          value.revision,
          value.title,
          value.body,
        );
      finished.current = true;
      if (alive.current) closed(save);
    } catch (issue) {
      if (alive.current)
        setStatus(`${errorMessage(issue)} Metin korunuyor; tekrar deneyin.`);
    } finally {
      busyRef.current = false;
      if (alive.current && !finished.current) setBusy(false);
    }
  }
  function back() {
    if (keyboard.current || Keyboard.isVisible()) {
      Keyboard.dismiss();
      return;
    }
    exit(false);
  }
  const ink = dark ? styles.light : undefined;
  return (
    <Modal visible transparent animationType="none" onRequestClose={back}>
      <ReaderToneContext.Provider value={dark}>
        <KeyboardAvoidingView
          behavior="height"
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
            style={[styles.editor, dark && styles.dark]}
            accessibilityViewIsModal
          >
            <Text style={[styles.heading, ink]}>
              {initial.scope === 'book'
                ? 'Kitap notu'
                : initial.scope === 'page'
                ? `Sayfa ${initial.pageIndex + 1} notu`
                : `Pasaj notu · Sayfa ${initial.pageIndex + 1}`}
            </Text>
            <ScrollView
              style={styles.form}
              keyboardShouldPersistTaps="handled"
              contentContainerStyle={styles.formContent}
            >
              <View style={styles.actions}>
                <Action
                  label="Klavyeyi kapat"
                  onPress={() => Keyboard.dismiss()}
                />
                <Action
                  label="Taslağı tekrar kaydet"
                  disabled={busy}
                  onPress={() => flush().catch(() => {})}
                />
              </View>
              {initial.scope === 'passage' && (
                <View style={styles.quote}>
                  <Text style={[styles.label, ink]}>Bağlı alıntı / bölge</Text>
                  <Text selectable style={[styles.text, ink]}>
                    {initial.quote || 'Metin içermeyen bölge işareti'}
                  </Text>
                </View>
              )}
              <Text style={[styles.label, ink]}>Başlık</Text>
              <TextInput
                accessibilityLabel="Not başlığı"
                value={title}
                onChangeText={value => edit('title', value)}
                maxLength={200}
                editable={!busy}
                style={[styles.input, ink]}
              />
              <Text style={[styles.label, ink]}>Not</Text>
              <TextInput
                accessibilityLabel="Not metni"
                value={body}
                onChangeText={value => edit('body', value)}
                multiline
                autoFocus
                maxLength={50000}
                editable={!busy}
                textAlignVertical="top"
                style={[styles.input, styles.body, ink]}
              />
              <Text style={[styles.label, ink]}>
                {body.length} / 50.000 karakter · Otomatik kayıt taslağı saklar.
              </Text>
            </ScrollView>
            <Text accessibilityLiveRegion="polite" style={[styles.status, ink]}>
              {status}
            </Text>
            <View style={styles.actions}>
              <Action
                label="Taslağı sakla ve kapat"
                disabled={busy}
                onPress={() => exit(false)}
              />
              <Action
                label="Notu kaydet"
                disabled={busy || !(title.trim() || body.trim())}
                onPress={() => exit(true)}
              />
            </View>
          </View>
        </KeyboardAvoidingView>
      </ReaderToneContext.Provider>
    </Modal>
  );
}
const styles = StyleSheet.create({
  overlay: { flex: 1, backgroundColor: '#00000066' },
  editor: {
    flex: 1,
    alignSelf: 'center',
    width: '100%',
    maxWidth: 700,
    padding: 14,
    backgroundColor: '#f0ede5',
    gap: 8,
  },
  form: { flex: 1 },
  formContent: { gap: 8, paddingBottom: 12 },
  heading: { fontSize: 23, color: '#354436' },
  label: { fontSize: 14, color: '#586458' },
  text: { fontSize: 17, lineHeight: 25, color: '#354436' },
  input: {
    minHeight: 48,
    padding: 10,
    fontSize: 18,
    borderWidth: 1,
    borderColor: '#7b8579',
    borderRadius: 6,
    color: '#354436',
  },
  body: { height: 220 },
  quote: { gap: 6, padding: 10, borderLeftWidth: 3, borderColor: '#7b8579' },
  actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  status: { fontSize: 15, lineHeight: 21, color: '#354436' },
  dark: { backgroundColor: '#16191c' },
  light: { color: '#dadbd7' },
});
