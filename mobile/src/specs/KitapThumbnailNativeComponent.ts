import type { CodegenTypes, HostComponent, ViewProps } from 'react-native';
import { codegenNativeComponent } from 'react-native';
export interface NativeProps extends ViewProps {
  bookId: string;
  pageIndex?: CodegenTypes.WithDefault<CodegenTypes.Int32, 0>;
  enabled?: CodegenTypes.WithDefault<boolean, false>;
  dark?: CodegenTypes.WithDefault<boolean, false>;
}
export default codegenNativeComponent<NativeProps>(
  'KitapThumbnail',
) as HostComponent<NativeProps>;
