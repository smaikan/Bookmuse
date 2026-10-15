import React, { useRef, useState } from 'react';
import {
  KeyboardAvoidingView,
  Modal,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  View,
} from 'react-native';
import Slider from '../specs/KitapSliderNativeComponent';
import { Action, ReaderToneContext } from './controls';
import { validateOptions } from './options';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import type { ReaderOptions } from './options';

export function ReaderSettings({
  options,
  close,
  save,
}: {
  options: ReaderOptions;
  close: () => void;
  save: (options: ReaderOptions) => Promise<void>;
}) {
  const [draft, setDraft] = useState(options);
  const [error, setError] = useState('');
  const insets = useSafeAreaInsets();
  const [saving, setSaving] = useState(false);
  const saveInFlight = useRef(false);
  async function commit() {
    if (saveInFlight.current) return;
    const issue = validateOptions(draft);
    if (issue) {
      setError(issue);
      return;
    }
    saveInFlight.current = true;
    setSaving(true);
    setError('');
    try {
      await save(draft);
    } catch (problem) {
      setError(
        problem instanceof Error
          ? problem.message
          : 'Okuma tercihleri kaydedilemedi.',
      );
    } finally {
      saveInFlight.current = false;
      setSaving(false);
    }
  }
  function dismiss() {
    if (!saveInFlight.current) close();
  }
  function patch(value: Partial<ReaderOptions>) {
    setDraft(current => ({ ...current, ...value }));
  }
  function toggle(
    key:
      | 'screenOn'
      | 'reduceMotion'
      | 'leftHand'
      | 'doublePage'
      | 'coverSingle',
    label: string,
  ) {
    return (
      <View style={styles.toggle} key={key}>
        <Text style={styles.label}>{label}</Text>
        <Switch
          accessibilityLabel={label}
          disabled={saving}
          value={draft[key]}
          onValueChange={value => patch({ [key]: value })}
        />
      </View>
    );
  }
  function slider(
    label: string,
    value: number,
    low: number,
    high: number,
    change: (value: number) => void,
  ) {
    return (
      <View>
        <Text style={styles.label}>
          {label}: {value}
        </Text>
        <Slider
          style={styles.slider}
          accessibilityLabel={label}
          minimum={low}
          maximum={high}
          value={value}
          onValueChange={event => change(event.nativeEvent.value)}
        />
      </View>
    );
  }
  return (
    <Modal visible animationType="none" onRequestClose={dismiss}>
      <ReaderToneContext.Provider value={false}>
        <KeyboardAvoidingView
          behavior="height"
          style={[
            styles.screen,
            { paddingLeft: insets.left, paddingRight: insets.right },
          ]}
        >
          <ScrollView
            contentContainerStyle={[
              styles.content,
              {
                paddingTop: insets.top + 24,
                paddingBottom: insets.bottom + 24,
              },
            ]}
            keyboardShouldPersistTaps="handled"
          >
            <Text accessibilityRole="header" style={styles.title}>
              Ayarlar
            </Text>
            <Text style={styles.caption}>
              Bu ayarlar bütün kitaplarda kullanılır.
            </Text>
            <Text accessibilityRole="header" style={styles.subtitle}>
              Okuma Görünümü
            </Text>
            <View style={styles.actions}>
              {[
                { label: 'Orijinal', theme: 0 },
                { label: 'Sepya', theme: 2 },
                { label: 'Gece', theme: 3 },
                { label: 'Özel', theme: 4 },
              ].map(({ label, theme }) => (
                <Action
                  key={label}
                  disabled={saving}
                  label={label}
                  selected={draft.theme === theme}
                  onPress={() => patch({ theme })}
                />
              ))}
            </View>
            {draft.theme === 4 && (
              <>
                <Text style={styles.label}>Arka plan rengi</Text>
                <TextInput
                  accessibilityLabel="Arka plan rengi"
                  autoCapitalize="characters"
                  value={draft.paper}
                  onChangeText={paper => patch({ paper })}
                  maxLength={7}
                  editable={!saving}
                  style={styles.input}
                />
                <Text style={styles.label}>Yazı rengi</Text>
                <TextInput
                  accessibilityLabel="Yazı rengi"
                  autoCapitalize="characters"
                  value={draft.ink}
                  onChangeText={ink => patch({ ink })}
                  maxLength={7}
                  editable={!saving}
                  style={styles.input}
                />
              </>
            )}
            <Text style={styles.caption}>
              Renk ve kontrast ayarı resimleri de etkiler. Orijinal ile PDF’nin
              renklerine dönebilirsin.
            </Text>
            {slider(
              'Kontrast (%)',
              Math.round(draft.contrast * 100),
              60,
              180,
              value => patch({ contrast: value / 100 }),
            )}
            {toggle('doublePage', 'Yatay tablette çift sayfa')}
            {toggle('coverSingle', 'Kapak tek sayfa olsun')}
            <Text accessibilityRole="header" style={styles.subtitle}>
              Ekran
            </Text>
            <View style={styles.actions}>
              {(
                [
                  ['auto', 'Otomatik yön'],
                  ['portrait', 'Dikey yön'],
                  ['landscape', 'Yatay yön'],
                ] as const
              ).map(([orientation, label]) => (
                <Action
                  key={orientation}
                  disabled={saving}
                  label={label}
                  selected={draft.orientation === orientation}
                  onPress={() => patch({ orientation })}
                />
              ))}
            </View>
            <Action
              disabled={saving}
              label="Sistem parlaklığı"
              selected={draft.brightness === -1}
              onPress={() => patch({ brightness: -1 })}
            />
            {slider(
              'Okuma parlaklığı (%)',
              Math.round((draft.brightness < 0 ? 0.5 : draft.brightness) * 100),
              5,
              100,
              value => patch({ brightness: value / 100 }),
            )}
            {toggle('screenOn', 'Okurken ekran açık kalsın')}
            {toggle('reduceMotion', 'Hareketi azalt')}
            {toggle('leftHand', 'Sol el kullanımı')}
            {!!error && (
              <Text accessibilityLiveRegion="polite" style={styles.label}>
                {error}
              </Text>
            )}
            <View style={styles.actions}>
              <Action label="Vazgeç" onPress={dismiss} disabled={saving} />
              <Action
                label="Ayarları kaydet"
                disabled={saving}
                onPress={commit}
              />
            </View>
          </ScrollView>
        </KeyboardAvoidingView>
      </ReaderToneContext.Provider>
    </Modal>
  );
}
const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#F5F3ED' },
  content: {
    padding: 24,
    gap: 14,
    maxWidth: 680,
    width: '100%',
    alignSelf: 'center',
  },
  title: { fontSize: 26, color: '#303c33', fontWeight: '600' },
  subtitle: { fontSize: 21, color: '#303c33', marginTop: 10 },
  label: { fontSize: 16, color: '#354436', flexShrink: 1 },
  caption: { fontSize: 14, color: '#586458', lineHeight: 21 },
  actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  toggle: {
    minHeight: 48,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 16,
  },
  input: {
    minHeight: 48,
    borderWidth: 1,
    borderColor: '#8d9a89',
    borderRadius: 6,
    padding: 12,
    color: '#273c2d',
    fontSize: 20,
  },
  slider: { height: 48, width: '100%' },
});
