import type { CodegenTypes, HostComponent, ViewProps } from 'react-native';
import { codegenNativeComponent } from 'react-native';

export interface NativeProps extends ViewProps {
  overviewVisible?: CodegenTypes.WithDefault<boolean, false>;
  overviewEnabled?: CodegenTypes.WithDefault<boolean, false>;
  reduceMotion?: CodegenTypes.WithDefault<boolean, false>;
}
export default codegenNativeComponent<NativeProps>(
  'KitapGestureHost',
) as HostComponent<NativeProps>;
