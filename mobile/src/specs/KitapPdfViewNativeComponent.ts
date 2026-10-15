import type { CodegenTypes, HostComponent, ViewProps } from 'react-native';
import { codegenNativeComponent } from 'react-native';

export type DiagnosticEvent = Readonly<{
  status: string;
  message: string;
  renderMs: CodegenTypes.Double;
  bitmapBytes: CodegenTypes.Double;
  sessionId: string;
  pageIndex: CodegenTypes.Int32;
  zoom: CodegenTypes.Double;
  direction: CodegenTypes.Int32;
  focusX?: CodegenTypes.Double;
  focusY?: CodegenTypes.Double;
}>;

export interface NativeProps extends ViewProps {
  sessionId: string;
  pageIndex?: CodegenTypes.WithDefault<CodegenTypes.Int32, 0>;
  previousPageIndex?: CodegenTypes.WithDefault<CodegenTypes.Int32, -1>;
  nextPageIndex?: CodegenTypes.WithDefault<CodegenTypes.Int32, -1>;
  pageCount?: CodegenTypes.WithDefault<CodegenTypes.Int32, 1>;
  readerOptions?: string;
  zoomCommand?: CodegenTypes.WithDefault<CodegenTypes.Int32, 0>;
  externalNavigation?: CodegenTypes.WithDefault<boolean, false>;
  trackReading?: CodegenTypes.WithDefault<boolean, true>;
  allowPrefetch?: CodegenTypes.WithDefault<boolean, true>;
  pixelLimit?: CodegenTypes.WithDefault<CodegenTypes.Int32, 1500000>;
  showHandle?: CodegenTypes.WithDefault<boolean, false>;
  deferPresentation?: CodegenTypes.WithDefault<boolean, false>;
  presentationId?: CodegenTypes.WithDefault<CodegenTypes.Int32, 0>;
  selectionCommand?: CodegenTypes.WithDefault<CodegenTypes.Int32, 0>;
  annotationRevision?: CodegenTypes.WithDefault<CodegenTypes.Int32, 0>;
  focusCommand?: string;
  onDiagnostic?: CodegenTypes.DirectEventHandler<DiagnosticEvent>;
}

export default codegenNativeComponent<NativeProps>(
  'KitapPdfView',
) as HostComponent<NativeProps>;
