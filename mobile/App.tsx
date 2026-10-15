import React from 'react';
import { NavigationContainer, DefaultTheme } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { LibraryScreen } from './src/reader/LibraryScreen';
import { ReaderScreen } from './src/reader/ReaderScreen';
import type { Routes } from './src/reader/types';

const Stack = createNativeStackNavigator<Routes>();
const theme = {
  ...DefaultTheme,
  colors: {
    ...DefaultTheme.colors,
    background: '#f0ede5',
    card: '#f0ede5',
    text: '#303c33',
    primary: '#38533d',
  },
};
export default function App() {
  return (
    <SafeAreaProvider>
      <NavigationContainer theme={theme}>
        <Stack.Navigator
          screenOptions={{
            contentStyle: { backgroundColor: '#eeeae2' },
            animation: 'none',
            statusBarStyle: 'dark',
          }}
        >
          <Stack.Screen
            name="Library"
            component={LibraryScreen}
            options={{ headerShown: false }}
          />
          <Stack.Screen
            name="Reader"
            component={ReaderScreen}
            options={({ route }) => ({
              title: route.params.book.title,
              headerShown: false,
              headerBackAccessibilityLabel: 'Kitaplığa dön',
            })}
          />
        </Stack.Navigator>
      </NavigationContainer>
    </SafeAreaProvider>
  );
}
