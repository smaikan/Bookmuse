import React, {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from 'react';
import {
  AccessibilityInfo,
  Animated,
  Pressable,
  AppState,
  BackHandler,
  DeviceEventEmitter,
  StyleSheet,
  Text,
  TextInput,
  useWindowDimensions,
  View,
} from 'react-native';
import { useIsFocused, usePreventRemove } from '@react-navigation/native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import PdfEngine from '../specs/NativeKitapPdf';
import PdfView from '../specs/KitapPdfViewNativeComponent';
import GestureHost from '../specs/KitapGestureHostNativeComponent';
import type { DiagnosticEvent } from '../specs/KitapPdfViewNativeComponent';
import { Action, ReaderToneContext } from './controls';
import type { Routes } from './types';
import { DEFAULT_OPTIONS, isDark, readerPaper } from './options';
import type { ReaderOptions } from './options';
import { PagePanel } from './PagePanel';
import { adjacentSpread, spreadPages } from './spread';
import { MARK_COLORS } from './Annotations';
import { errorMessage } from './types';
import { NoteEditor, openNote } from './NoteEditor';
import type { NoteDraft } from './NoteEditor';

type Selection = {
  id: string;
  pageIndex: number;
  quote: string;
  lineCount: number;
  region: boolean;
};

export function ReaderScreen({
  route,
  navigation,
}: NativeStackScreenProps<Routes, 'Reader'>) {
  const { book } = route.params;
  const insets = useSafeAreaInsets();
  const focused = useIsFocused();
  const window = useWindowDimensions();
  const [allowRemove, setAllowRemove] = useState(false);
  const exitAction = useRef<Parameters<typeof navigation.dispatch>[0] | null>(
    null,
  );
  const exiting = useRef(false);
  usePreventRemove(!allowRemove, ({ data }) => {
    if (exiting.current) return;
    exiting.current = true;
    PdfEngine.flushReadingPosition(book.sessionId)
      .then(() => {
        exitAction.current = data.action;
        setAllowRemove(true);
      })
      .catch(() => {
        exiting.current = false;
        setNotice('Son sayfa kaydedilemedi. Çıkmak için tekrar deneyin.');
        setTools(true);
      });
  });
  useEffect(() => {
    if (allowRemove && exitAction.current)
      navigation.dispatch(exitAction.current);
  }, [allowRemove, navigation]);
  const [active, setActive] = useState(AppState.currentState === 'active');
  const [target, setTarget] = useState(
    route.params.annotation?.pageIndex ??
      route.params.notePage ??
      book.pageIndex,
  );
  const [displayed, setDisplayed] = useState<number | null>(null);
  const [displayedPages, setDisplayedPages] = useState<number[]>([
    book.pageIndex,
  ]);
  const displayedRef = useRef<number | null>(null);
  const requested = useRef(target);
  const [pending, setPending] = useState(true);
  const [status, setStatus] = useState('Sayfa hazırlanıyor…');
  const [failed, setFailed] = useState(false);
  const [retry, setRetry] = useState(0);
  const [options] = useState<ReaderOptions>({
    ...DEFAULT_OPTIONS,
    ...book.options,
  });
  const [tools, setTools] = useState(false);
  const [reduceMotion, setReduceMotion] = useState(false);
  const [zoomCommand, setZoomCommand] = useState(0);
  const [zoomLevel, setZoomLevel] = useState(1);
  const [notice, setNotice] = useState('');
  const [panel, setPanel] = useState(false);
  const [overviewClosing, setOverviewClosing] = useState(false);
  const [bookmarked, setBookmarked] = useState<boolean | null>(null);
  const [bookmarkBusy, setBookmarkBusy] = useState(false);
  const bookmarkOperation = useRef(false);
  const [presentationId, setPresentationId] = useState(0);
  const prepared = useRef(new Set<number>());
  const ready = useRef(new Set<number>());
  const saved = useRef(new Set<number>());
  useEffect(() => {
    if (!focused) return;
    const listener = DeviceEventEmitter.addListener('KitapSharedPdf', () =>
      navigation.popToTop(),
    );
    return () => listener.remove();
  }, [focused, navigation]);
  const [selection, setSelection] = useState<Selection | null>(null);
  const selectionRef = useRef<Selection | null>(null);
  const selectionActive = useRef(false);
  const [selectionArmed, setSelectionArmed] = useState(false);
  const [selectionPending, setSelectionPending] = useState(false);
  const [selectionCommand, setSelectionCommand] = useState(0);
  const [markColor] = useState(MARK_COLORS[0].value);
  const [manualText, setManualText] = useState('');
  const [markBusy, setMarkBusy] = useState(false);
  const markWrite = useRef(false);
  const operationId = useRef<{
    selection: string;
    id: string;
    kind: string;
    color: string;
    text: string;
  } | null>(null);
  const [annotationRevision, setAnnotationRevision] = useState(0);
  const [noteEditor, setNoteEditor] = useState<NoteDraft | null>(null);
  const selectionNoteId = useRef<{
    selection: string;
    id: string;
    color: string;
    text: string;
  } | null>(null);
  const [focusCommand, setFocusCommand] = useState(
    route.params.annotation
      ? JSON.stringify({
          page: route.params.annotation.pageIndex,
          mark: route.params.annotation.id,
          sequence: 1,
        })
      : '',
  );
  const focus = useRef({ x: 0.5, y: 0.5, zoom: 1 });
  const [returnPoint, setReturnPoint] = useState<{
    page: number;
    x: number;
    y: number;
    zoom: number;
  } | null>(
    route.params.annotation || route.params.notePage !== undefined
      ? { page: book.pageIndex, x: 0.5, y: 0.5, zoom: 1 }
      : null,
  );
  const mounted = useRef(true);
  const selectionAction = useCallback((action: number) => {
    setSelectionCommand(value => (Math.floor(value / 10) + 1) * 10 + action);
  }, []);
  const clearSelection = useCallback(() => {
    selectionActive.current = false;
    setSelectionArmed(false);
    setSelectionPending(false);
    selectionRef.current = null;
    setSelection(null);
    operationId.current = null;
    selectionNoteId.current = null;
    selectionAction(3);
  }, [selectionAction]);
  useEffect(
    () => () => {
      mounted.current = false;
    },
    [],
  );
  const dark = isDark(options);
  const pair =
    options.doublePage &&
    window.width - insets.left - insets.right >= 720 &&
    window.width > window.height;
  const pages = pair
    ? spreadPages(target, book.pageCount, options.coverSingle)
    : [target];
  const wanted = useRef(pages);
  wanted.current = pages;

  useLayoutEffect(() => {
    navigation.setOptions({
      headerStyle: { backgroundColor: dark ? '#16191c' : '#f0ede5' },
      headerTintColor: dark ? '#dadbd7' : '#303c33',
      statusBarStyle: dark ? 'light' : 'dark',
      headerShown: false,
      statusBarHidden: true,
      navigationBarHidden: true,
    });
  }, [dark, navigation, options.fullscreen]);
  useLayoutEffect(() => {
    prepared.current.clear();
    ready.current.clear();
    saved.current.clear();
    setPending(true);
  }, [pair, window.width, window.height]);
  useEffect(() => {
    let motionMounted = true;
    AccessibilityInfo.isReduceMotionEnabled().then(value => {
      if (motionMounted) {
        setReduceMotion(value);
      }
    });
    const subscription = AccessibilityInfo.addEventListener(
      'reduceMotionChanged',
      setReduceMotion,
    );
    return () => {
      motionMounted = false;
      subscription.remove();
    };
  }, []);
  useEffect(() => {
    let current = true;
    PdfEngine.setWindowOptions(focused ? JSON.stringify(options) : '').catch(
      () => {
        if (current) {
          setNotice(
            'Ekran ayarı uygulanamadı. Okuma ayarlarından tekrar deneyebilirsin.',
          );
        }
      },
    );
    return () => {
      current = false;
      PdfEngine.setWindowOptions('').catch(() => {});
    };
  }, [focused, options]);
  useEffect(() => {
    const subscription = AppState.addEventListener('change', state =>
      setActive(state === 'active'),
    );
    return () => {
      subscription.remove();
      PdfEngine.closeSession(book.sessionId).catch(() => {});
    };
  }, [book.sessionId]);
  useEffect(() => {
    if (active && focused) {
      prepared.current.clear();
      ready.current.clear();
      saved.current.clear();
      setPending(true);
      setStatus('Sayfa hazırlanıyor…');
    }
  }, [active, focused]);
  useEffect(() => {
    if (!focused) {
      return;
    }
    const subscription = BackHandler.addEventListener(
      'hardwareBackPress',
      () => {
        if (panel || noteEditor) return false;
        if (selectionActive.current) {
          clearSelection();
          return true;
        }
        if (zoomLevel > 1.01) {
          zoom(3);
          setZoomLevel(1);
          return true;
        }
        if (tools) {
          setTools(false);
          return true;
        }
        return false;
      },
    );
    return () => subscription.remove();
  }, [clearSelection, focused, tools, zoomLevel, panel, noteEditor]);
  useEffect(() => {
    if (displayed === null) {
      return;
    }
    let current = true;
    setBookmarked(null);
    PdfEngine.isBookmarked(book.id, displayed)
      .then(value => {
        if (current && displayedRef.current === displayed) {
          setBookmarked(value);
        }
      })
      .catch(() => {
        if (current) {
          setNotice('Yer imi durumu alınamadı. Sayfayı tekrar açabilirsin.');
        }
      });
    return () => {
      current = false;
    };
  }, [book.id, displayed]);

  async function toggleBookmark() {
    if (
      displayed === null ||
      bookmarked === null ||
      bookmarkOperation.current
    ) {
      return;
    }
    const page = displayed;
    bookmarkOperation.current = true;
    setBookmarkBusy(true);
    try {
      const enabled = await PdfEngine.setBookmark(book.id, page, !bookmarked);
      if (displayedRef.current === page) {
        setBookmarked(enabled);
      }
    } catch {
      setNotice('Yer imi kaydedilemedi. Tekrar deneyin.');
    } finally {
      bookmarkOperation.current = false;
      setBookmarkBusy(false);
    }
  }
  function zoom(action: number) {
    setZoomCommand(value => (Math.floor(value / 10) + 1) * 10 + action);
  }
  async function saveMark(kind: string) {
    const chosen = selectionRef.current;
    if (!chosen || markWrite.current) return;
    markWrite.current = true;
    setMarkBusy(true);
    setNotice('İşaret kaydediliyor…');
    try {
      // Keep the exact command on retry; type/color/text cannot reuse an id for a different operation.
      if (!operationId.current || operationId.current.selection !== chosen.id) {
        const id = await PdfEngine.newOperationId();
        if (selectionRef.current?.id !== chosen.id) return;
        operationId.current = {
          selection: chosen.id,
          id,
          kind,
          color: markColor,
          text: manualText,
        };
      }
      const operation = operationId.current;
      await PdfEngine.saveSelection(
        book.sessionId,
        chosen.id,
        operation.kind,
        operation.color,
        operation.id,
        operation.text,
      );
      if (mounted.current) setAnnotationRevision(value => value + 1);
      if (mounted.current && selectionRef.current?.id === chosen.id) {
        clearSelection();
        setNotice('İşaret kaydedildi.');
      }
    } catch (issue) {
      if (mounted.current)
        setNotice(
          `${errorMessage(
            issue,
          )} Seçim korundu; aynı kayıt işlemini tekrar deneyebilirsin.`,
        );
    } finally {
      markWrite.current = false;
      if (mounted.current) setMarkBusy(false);
    }
  }
  async function createNote(scope: string) {
    if (markWrite.current) return;
    markWrite.current = true;
    setMarkBusy(true);
    try {
      const draft = await openNote(
        book.id,
        scope,
        scope === 'book' ? -1 : displayedRef.current ?? target,
      );
      if (mounted.current) setNoteEditor(draft);
    } catch (issue) {
      if (mounted.current) setNotice(errorMessage(issue));
    } finally {
      markWrite.current = false;
      if (mounted.current) setMarkBusy(false);
    }
  }
  async function noteFromSelection() {
    const chosen = selectionRef.current;
    if (!chosen || markWrite.current) return;
    markWrite.current = true;
    setMarkBusy(true);
    setNotice('Pasaj ve not hazırlanıyor…');
    try {
      if (selectionNoteId.current?.selection !== chosen.id)
        selectionNoteId.current = {
          selection: chosen.id,
          id: await PdfEngine.newOperationId(),
          color: markColor,
          text: manualText,
        };
      if (selectionRef.current?.id !== chosen.id) return;
      const draft: NoteDraft = JSON.parse(
        await PdfEngine.selectionNote(
          book.sessionId,
          chosen.id,
          selectionNoteId.current.id,
          selectionNoteId.current.color,
          selectionNoteId.current.text,
        ),
      );
      if (mounted.current) {
        setAnnotationRevision(value => value + 1);
        if (selectionRef.current?.id === chosen.id) {
          clearSelection();
          setNoteEditor(draft);
          setNotice('');
        }
      }
    } catch (issue) {
      if (mounted.current)
        setNotice(
          `${errorMessage(issue)} Seçim korundu; tekrar deneyebilirsiniz.`,
        );
    } finally {
      markWrite.current = false;
      if (mounted.current) setMarkBusy(false);
    }
  }
  function returnToReading() {
    if (!returnPoint) return;
    const point = returnPoint;
    setReturnPoint(null);
    go(point.page);
    setFocusCommand(JSON.stringify({ ...point, sequence: Date.now() }));
  }
  function go(page: number) {
    if (!Number.isInteger(page) || page < 0 || page >= book.pageCount) {
      return;
    }
    if (selectionActive.current) clearSelection();
    const next = pair
      ? spreadPages(page, book.pageCount, options.coverSingle)
      : [page];
    const alreadyShown =
      page !== target &&
      next.join(':') === pages.join(':') &&
      ready.current.size === pages.length;
    requested.current = page;
    wanted.current = next;
    if (!alreadyShown) {
      prepared.current.clear();
      ready.current.clear();
      saved.current.clear();
    }
    setTarget(page);
    setPending(!alreadyShown);
    setFailed(false);
    setStatus(alreadyShown ? 'Konum kaydediliyor…' : 'Sayfa hazırlanıyor…');
    if (alreadyShown) {
      displayedRef.current = page;
      setDisplayed(page);
      setDisplayedPages(next);
    }
    if (page === target) {
      setRetry(value => value + 1);
    }
  }
  function diagnostic(data: DiagnosticEvent, pane: number) {
    if (data.sessionId !== book.sessionId) {
      return;
    }
    if (data.status === 'activatePage') {
      go(data.pageIndex);
      return;
    }
    if (data.status === 'selectionArmed') {
      selectionActive.current = true;
      setSelectionArmed(true);
      setNotice(data.message);
      setTools(false);
      return;
    }
    if (data.status === 'annotationError') {
      setNotice(data.message);
      setTools(true);
      return;
    }
    if (data.status === 'selection' && data.pageIndex === requested.current) {
      try {
        const selected: Selection = JSON.parse(data.message);
        if (
          selected.pageIndex !== requested.current ||
          !selected.id ||
          typeof selected.quote !== 'string'
        )
          return;
        setSelectionArmed(false);
        selectionRef.current = selected;
        selectionActive.current = true;
        setSelectionPending(false);
        setSelection(selected);
        operationId.current = null;
        setManualText('');
        setTools(true);
        setNotice('');
      } catch {
        setNotice('Seçim bilgisi alınamadı; yeniden seçin.');
      }
      return;
    }
    if (data.status === 'selectionCleared' && pane === requested.current) {
      selectionActive.current = false;
      setSelectionArmed(false);
      setSelectionPending(false);
      selectionRef.current = null;
      setSelection(null);
      operationId.current = null;
      return;
    }
    if (
      (data.status === 'selectionLoading' ||
        data.status === 'selectionError') &&
      pane === requested.current
    ) {
      selectionRef.current = null;
      setSelection(null);
      operationId.current = null;
      setNotice(data.message);
      setSelectionArmed(false);
      if (data.status === 'selectionLoading') {
        selectionActive.current = true;
        setSelectionPending(true);
      } else {
        selectionActive.current = false;
        setSelectionPending(false);
      }
      setTools(true);
      return;
    }
    if (
      (data.status === 'ready' || data.status === 'zoom') &&
      pane === requested.current
    ) {
      focus.current = {
        x: data.focusX ?? 0.5,
        y: data.focusY ?? 0.5,
        zoom: data.zoom ?? 1,
      };
    }
    if (data.status === 'navigate') {
      const direction = data.direction || data.pageIndex - pane;
      go(
        pair
          ? adjacentSpread(
              displayedRef.current ?? requested.current,
              book.pageCount,
              options.coverSingle,
              direction,
            )
          : data.pageIndex,
      );
      return;
    }
    if (data.status === 'openPanel') {
      setTools(false);
      setPanel(true);
      return;
    }
    if (data.status === 'interaction') {
      setTools(false);
      return;
    }
    if (data.status === 'toggleTools') {
      setTools(value => !value);
      return;
    }
    if (data.status === 'zoom') {
      if (pane === requested.current) {
        setZoomLevel(data.zoom);
      }
      return;
    }
    if (!wanted.current.includes(data.pageIndex)) {
      return;
    }
    if (data.status === 'prepared') {
      prepared.current.add(data.pageIndex);
      if (wanted.current.every(page => prepared.current.has(page))) {
        setPresentationId(value => value + 1);
      }
    } else if (data.status === 'ready') {
      prepared.current.add(data.pageIndex);
      ready.current.add(data.pageIndex);
      if (
        wanted.current.every(page => prepared.current.has(page)) &&
        !wanted.current.every(page => ready.current.has(page))
      ) {
        setPresentationId(value => value + 1);
      }
      if (wanted.current.every(page => ready.current.has(page))) {
        displayedRef.current = requested.current;
        setDisplayed(requested.current);
        setDisplayedPages([...wanted.current]);
        setPending(false);
        setFailed(false);
        setStatus(
          saved.current.has(requested.current) ? '' : 'Konum kaydediliyor…',
        );
        if (pane === requested.current) {
          setZoomLevel(data.zoom ?? 1);
        }
      }
    } else if (data.status === 'saved') {
      saved.current.add(data.pageIndex);
      if (
        data.pageIndex === requested.current &&
        wanted.current.every(page => ready.current.has(page))
      ) {
        setStatus('');
      }
    } else if (data.status === 'error') {
      setPending(false);
      setFailed(true);
      setStatus(data.message);
    } else if (
      data.status === 'saveError' &&
      data.pageIndex === requested.current
    ) {
      setFailed(true);
      setStatus(data.message);
    }
  }
  const counter =
    displayed === null
      ? 'Sayfa hazırlanıyor'
      : displayedPages.length > 1
      ? `${displayedPages[0] + 1}–${
          displayedPages[displayedPages.length - 1] + 1
        } / ${book.pageCount}`
      : `${displayed + 1} / ${book.pageCount}`;

  return (
    <ReaderToneContext.Provider value={dark}>
      <View style={[styles.screen, { backgroundColor: readerPaper(options) }]}>
        <GestureHost
          style={styles.surface}
          overviewEnabled={true}
          overviewVisible={panel && !overviewClosing}
          pointerEvents={panel ? 'none' : 'auto'}
          importantForAccessibility={panel ? 'no-hide-descendants' : 'auto'}
          reduceMotion={options.reduceMotion || reduceMotion}
        >
          {active &&
            focused &&
            pages.map((pane, side) => (
              <PdfView
                key={`${side}-${retry}`}
                style={styles.pdf}
                sessionId={book.sessionId}
                pageIndex={pane}
                pageCount={book.pageCount}
                previousPageIndex={
                  pair
                    ? target === 0
                      ? -1
                      : spreadPages(
                          adjacentSpread(
                            target,
                            book.pageCount,
                            options.coverSingle,
                            -1,
                          ),
                          book.pageCount,
                          options.coverSingle,
                        )[side] ?? 0
                    : pane - 1
                }
                nextPageIndex={
                  pair
                    ? adjacentSpread(
                        target,
                        book.pageCount,
                        options.coverSingle,
                        1,
                      ) === target
                      ? -1
                      : spreadPages(
                          adjacentSpread(
                            target,
                            book.pageCount,
                            options.coverSingle,
                            1,
                          ),
                          book.pageCount,
                          options.coverSingle,
                        )[side] ?? -1
                    : pane + 1 < book.pageCount
                    ? pane + 1
                    : -1
                }
                readerOptions={JSON.stringify({
                  ...options,
                  fullscreen: true,
                  left: 0,
                  right: 0,
                  top: 0,
                  bottom: 0,
                  fitWidth: false,
                  reduceMotion: options.reduceMotion || reduceMotion,
                })}
                zoomCommand={zoomCommand}
                selectionCommand={pane === target ? selectionCommand : 0}
                annotationRevision={annotationRevision}
                focusCommand={pane === target ? focusCommand : ''}
                externalNavigation={pair}
                trackReading={pane === target}
                allowPrefetch={true}
                pixelLimit={pair ? 750000 : 1500000}
                deferPresentation={pair}
                presentationId={presentationId}
                showHandle={false}
                accessible
                accessibilityLabel={
                  pending
                    ? `${book.title}, PDF sayfası hazırlanıyor`
                    : `${book.title}, PDF sayfası ${pane + 1}`
                }
                accessibilityActions={[
                  { name: 'increment', label: 'Sonraki sayfa' },
                  { name: 'decrement', label: 'Önceki sayfa' },
                  { name: 'pages', label: 'Sayfalar' },
                  { name: 'tools', label: 'Okuma işlemleri' },
                ]}
                onAccessibilityAction={event => {
                  const action = event.nativeEvent.actionName;
                  if (action === 'pages') setPanel(true);
                  else if (action === 'tools') setTools(true);
                  else
                    go(
                      Math.max(
                        0,
                        Math.min(
                          book.pageCount - 1,
                          (displayed ?? target) +
                            (action === 'increment' ? 1 : -1),
                        ),
                      ),
                    );
                }}
                onDiagnostic={event => diagnostic(event.nativeEvent, pane)}
              />
            ))}
        </GestureHost>
        <ReaderOverlay
          visible={tools}
          reduced={options.reduceMotion || reduceMotion}
        >
          <View
            style={[
              styles.floating,
              {
                bottom: insets.bottom + 12,
                left:
                  insets.left +
                  (window.width -
                    insets.left -
                    insets.right -
                    Math.min(
                      640,
                      window.width - insets.left - insets.right - 24,
                    )) /
                    2,
                width: Math.min(
                  640,
                  window.width - insets.left - insets.right - 24,
                ),
              },
              dark && styles.darkFloating,
            ]}
          >
            {selectionPending && !selection ? (
              <Action label="Seçimi iptal et" onPress={clearSelection} />
            ) : selection ? (
              <View style={styles.context}>
                <Text
                  numberOfLines={2}
                  accessible
                  accessibilityActions={
                    selection.region
                      ? [
                          { name: 'expand', label: 'Bölgeyi genişlet' },
                          { name: 'shrink', label: 'Bölgeyi daralt' },
                        ]
                      : [
                          { name: 'start', label: 'Seçimi baştan genişlet' },
                          { name: 'end', label: 'Seçimi sondan genişlet' },
                          {
                            name: 'shrinkStart',
                            label: 'Seçimi baştan daralt',
                          },
                          { name: 'shrinkEnd', label: 'Seçimi sondan daralt' },
                        ]
                  }
                  onAccessibilityAction={event => {
                    const commands: Record<string, number> = {
                      expand: 6,
                      shrink: 9,
                      start: 4,
                      end: 5,
                      shrinkStart: 7,
                      shrinkEnd: 8,
                    };
                    const action = commands[event.nativeEvent.actionName];
                    if (action) selectionAction(action);
                  }}
                  style={[styles.status, dark && styles.light]}
                >
                  {selection.region ? 'Elle seçilen bölge' : selection.quote}
                </Text>
                {selection.region && (
                  <TextInput
                    accessibilityLabel="Bölge açıklaması"
                    value={manualText}
                    onChangeText={setManualText}
                    maxLength={1000}
                    editable={!markBusy}
                    style={[styles.input, dark && styles.light]}
                  />
                )}
                <View style={styles.actions}>
                  <Action
                    label={
                      selection.region ? 'Bölgeyi kaydet' : 'Sarıyla işaretle'
                    }
                    disabled={markBusy}
                    onPress={() =>
                      saveMark(selection.region ? 'region' : 'highlight')
                    }
                  />
                  {!selection.region && (
                    <Action
                      label="Kopyala"
                      disabled={markBusy}
                      onPress={() =>
                        PdfEngine.copyText(selection.quote).catch(issue =>
                          setNotice(errorMessage(issue)),
                        )
                      }
                    />
                  )}
                  <Action
                    label="Pasaja not ekle"
                    disabled={markBusy}
                    onPress={noteFromSelection}
                  />
                  <Action
                    label="Seçimi kapat"
                    disabled={markBusy}
                    onPress={clearSelection}
                  />
                </View>
              </View>
            ) : (
              <View
                style={[
                  styles.actions,
                  { flexDirection: options.leftHand ? 'row-reverse' : 'row' },
                ]}
              >
                {[
                  {
                    label: 'Sayfaya not ekle',
                    icon: '✎',
                    press: () => createNote('page'),
                    disabled: pending || displayed === null,
                  },
                  {
                    label: 'Kitaba not ekle',
                    icon: '＋',
                    press: () => createNote('book'),
                    disabled: false,
                  },
                  {
                    label: 'Metin seç / Altını çiz',
                    icon: '⌁',
                    press: () => selectionAction(1),
                    disabled: pending,
                  },
                ].map(item => (
                  <Pressable
                    key={item.label}
                    accessibilityRole="button"
                    accessibilityLabel={item.label}
                    disabled={markBusy || item.disabled}
                    onPress={item.press}
                    style={[
                      styles.mainAction,
                      (markBusy || item.disabled) && styles.disabled,
                    ]}
                  >
                    <Text style={[styles.symbol, dark && styles.light]}>
                      {item.icon}
                    </Text>
                    <Text style={[styles.actionText, dark && styles.light]}>
                      {item.label}
                    </Text>
                  </Pressable>
                ))}
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel="Sayfalar"
                  onPress={() => {
                    setTools(false);
                    setPanel(true);
                  }}
                  style={styles.pageAction}
                >
                  <Text style={[styles.symbol, dark && styles.light]}>▦</Text>
                </Pressable>
              </View>
            )}
            {!!notice && (
              <Text
                accessibilityLiveRegion="polite"
                style={[styles.status, dark && styles.light]}
              >
                {notice}
              </Text>
            )}
          </View>
        </ReaderOverlay>
        {selectionArmed && (
          <View
            style={[
              styles.feedback,
              { top: insets.top + 12 },
              dark && styles.darkFloating,
            ]}
          >
            <Text
              accessibilityLiveRegion="polite"
              style={[styles.status, dark && styles.light]}
            >
              {notice}
            </Text>
            <Action label="Seçimi iptal et" onPress={clearSelection} />
          </View>
        )}
        {(failed || (pending && displayed === null)) && (
          <View
            style={[
              styles.feedback,
              { top: insets.top + 12 },
              dark && styles.darkFloating,
            ]}
          >
            <Text
              accessibilityLiveRegion="polite"
              style={[styles.status, dark && styles.light]}
            >
              {status}
            </Text>
            {failed && (
              <Action label="Sayfayı tekrar aç" onPress={() => go(target)} />
            )}
          </View>
        )}
        {panel && (
          <PagePanel
            reduced={options.reduceMotion || reduceMotion}
            bookId={book.id}
            count={book.pageCount}
            current={displayed ?? target}
            options={options}
            closeStart={() => setOverviewClosing(true)}
            close={() => {
              setPanel(false);
              setOverviewClosing(false);
            }}
            select={go}
            counter={counter}
            bookmarked={bookmarked === true}
            bookmarkBusy={bookmarkBusy || pending}
            toggleBookmark={toggleBookmark}
            returnToReading={
              returnPoint
                ? () => {
                    returnToReading();
                    setPanel(false);
                  }
                : undefined
            }
          />
        )}
        {noteEditor && (
          <NoteEditor
            bookId={book.id}
            initial={noteEditor}
            dark={dark}
            closed={() => setNoteEditor(null)}
          />
        )}
      </View>
    </ReaderToneContext.Provider>
  );
}
function ReaderOverlay({
  visible,
  reduced,
  children,
}: {
  visible: boolean;
  reduced: boolean;
  children: React.ReactNode;
}) {
  const opacity = useRef(new Animated.Value(visible ? 1 : 0)).current;
  const [mounted, setMounted] = useState(visible);
  useEffect(() => {
    if (visible) setMounted(true);
    const animation = Animated.timing(opacity, {
      toValue: visible ? 1 : 0,
      duration: reduced ? 0 : 160,
      useNativeDriver: true,
    });
    animation.start(({ finished }) => {
      if (finished && !visible) setMounted(false);
    });
    return () => animation.stop();
  }, [visible, reduced, opacity]);
  if (!mounted) return null;
  return (
    <Animated.View
      pointerEvents={visible ? 'box-none' : 'none'}
      accessibilityElementsHidden={!visible}
      importantForAccessibility={visible ? 'auto' : 'no-hide-descendants'}
      style={[StyleSheet.absoluteFill, { opacity }]}
    >
      {children}
    </Animated.View>
  );
}
const styles = StyleSheet.create({
  screen: { flex: 1 },
  surface: {
    position: 'absolute',
    top: 0,
    bottom: 0,
    left: 0,
    right: 0,
    flexDirection: 'row',
  },
  pdf: { flex: 1 },
  floating: {
    position: 'absolute',
    maxWidth: 640,
    alignSelf: 'center',
    borderRadius: 18,
    padding: 10,
    gap: 8,
    backgroundColor: '#F7F6EDF0',
  },
  darkFloating: { backgroundColor: '#242B27F0' },
  actions: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
    flexWrap: 'wrap',
  },
  mainAction: {
    flex: 1,
    minWidth: 64,
    minHeight: 72,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 6,
    gap: 4,
  },
  pageAction: {
    minWidth: 48,
    minHeight: 72,
    alignItems: 'center',
    justifyContent: 'center',
  },
  symbol: { fontSize: 24, color: '#304936' },
  actionText: {
    fontSize: 12,
    lineHeight: 17,
    textAlign: 'center',
    color: '#304936',
  },
  disabled: { opacity: 0.4 },
  context: { gap: 8 },
  status: { fontSize: 14, lineHeight: 21, color: '#465B49' },
  light: { color: '#E1E5DB' },
  input: {
    minHeight: 48,
    borderWidth: 1,
    borderColor: '#9CA897',
    borderRadius: 8,
    padding: 10,
    color: '#304936',
  },
  feedback: {
    position: 'absolute',
    alignSelf: 'center',
    maxWidth: '90%',
    padding: 12,
    borderRadius: 12,
    backgroundColor: '#F7F6EDF0',
    gap: 8,
  },
});
