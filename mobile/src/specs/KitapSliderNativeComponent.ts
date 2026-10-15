import type { CodegenTypes, HostComponent, ViewProps } from 'react-native';
import { codegenNativeComponent } from 'react-native';
type SliderEvent = Readonly<{ value: CodegenTypes.Int32 }>;
export interface NativeProps extends ViewProps {
  value?: CodegenTypes.WithDefault<CodegenTypes.Int32, 0>;
  minimum?: CodegenTypes.WithDefault<CodegenTypes.Int32, 0>;
  maximum?: CodegenTypes.WithDefault<CodegenTypes.Int32, 100>;
  onValueChange?: CodegenTypes.DirectEventHandler<SliderEvent>;
  onSlidingComplete?: CodegenTypes.DirectEventHandler<SliderEvent>;
}
export default codegenNativeComponent<NativeProps>(
  'KitapSlider',
) as HostComponent<NativeProps>;
