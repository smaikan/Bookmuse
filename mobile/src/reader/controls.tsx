import React, { createContext, useContext } from 'react';
import { Pressable, StyleSheet, Text } from 'react-native';

export const ReaderToneContext = createContext(false);
export function Action({
  label,
  onPress,
  disabled = false,
  selected = false,
}: {
  label: string;
  onPress: () => void;
  disabled?: boolean;
  selected?: boolean;
}) {
  const dark = useContext(ReaderToneContext);
  return (
    <Pressable
      // Android Fabric can retain native enabled=false across an accessibility
      // state update. Recreate only the host button when availability changes.
      key={disabled ? 'disabled' : 'enabled'}
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ disabled, selected }}
      disabled={disabled}
      onPress={onPress}
      style={[
        styles.button,
        dark && styles.darkButton,
        selected && styles.selected,
        selected && dark && styles.darkSelected,
        disabled && styles.disabled,
      ]}
    >
      <Text style={[styles.text, dark && styles.darkText]}>{label}</Text>
    </Pressable>
  );
}
const styles = StyleSheet.create({
  darkButton: { backgroundColor: '#303c34' },
  darkSelected: { backgroundColor: '#466048' },
  darkText: { color: '#d8e2d8' },
  button: {
    minHeight: 48,
    minWidth: 48,
    justifyContent: 'center',
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: '#dce3d7',
    flexShrink: 1,
  },
  selected: { backgroundColor: '#bfceb8' },
  disabled: { opacity: 0.45 },
  text: { fontSize: 16, color: '#273c2d', textAlign: 'center' },
});
