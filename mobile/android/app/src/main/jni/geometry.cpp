#include <jni.h>
#include <dlfcn.h>
#include <cmath>
#include <array>
#include <cstdint>

static jlong pointer(JNIEnv* env, jobject wrapper, const char* signature) {
  // Public JVM accessor in pinned 2.0.3, audited from its AAR. Never look up a private field.
  jclass owner=env->GetObjectClass(wrapper);
  jmethodID accessor=env->GetMethodID(owner,"getPage$pdfiumandroid",signature);
  env->DeleteLocalRef(owner);
  if(!accessor) { env->ExceptionClear();return 0; }
  jobject core=env->CallObjectMethod(wrapper,accessor);
  if(!core || env->ExceptionCheck()) return 0;
  jclass type=env->GetObjectClass(core);
  jmethodID getter=env->GetMethodID(type,"getPagePtr","()J");
  env->DeleteLocalRef(type);
  if(!getter) { env->ExceptionClear();env->DeleteLocalRef(core); return 0; }
  jlong result=env->CallLongMethod(core,getter);
  env->DeleteLocalRef(core);return result;
}

// Minimal public PDFium ABI. Resolve from the same already-loaded wrapper engine.
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_kitap_pdf_PdfGeometry_glyphs(JNIEnv* env, jobject, jobject pageWrapper, jobject textWrapper, jint count) {
  jlong pageAddress=pointer(env,pageWrapper,"()Lio/legere/pdfiumandroid/core/unlocked/PdfPageU;");
  if(env->ExceptionCheck()) return nullptr;
  jlong textAddress=pointer(env,textWrapper,"()Lio/legere/pdfiumandroid/core/unlocked/PdfTextPageU;");
  if(env->ExceptionCheck()) return nullptr;
  if (!pageAddress || !textAddress || count < 0 || count > 32000) return nullptr;
  static void* library = dlopen("libpdfium.so", RTLD_NOW | RTLD_LOCAL);
  if (!library) return nullptr;
  using Unicode = unsigned int(*)(void*,int);
  using Angle = float(*)(void*,int);
  using Box = int(*)(void*,int,double*,double*,double*,double*);
  using Map = int(*)(void*,int,int,int,int,int,double,double,int*,int*);
  using Error = int(*)(void*,int);
  static auto unicode = reinterpret_cast<Unicode>(dlsym(library,"FPDFText_GetUnicode"));
  static auto angle = reinterpret_cast<Angle>(dlsym(library,"FPDFText_GetCharAngle"));
  static auto box = reinterpret_cast<Box>(dlsym(library,"FPDFText_GetCharBox"));
  static auto map = reinterpret_cast<Map>(dlsym(library,"FPDF_PageToDevice"));
  static auto mappingError = reinterpret_cast<Error>(dlsym(library,"FPDFText_HasUnicodeMapError"));
  static auto generated = reinterpret_cast<Error>(dlsym(library,"FPDFText_IsGenerated"));
  if (!unicode || !angle || !box || !map || !mappingError || !generated) return nullptr;
  constexpr int stride = 19, chunkSize = 128, scale = 1000000;
  auto output = env->NewFloatArray(count * stride);
  if (!output) return nullptr;
  void* page = reinterpret_cast<void*>(static_cast<uintptr_t>(pageAddress));
  void* text = reinterpret_cast<void*>(static_cast<uintptr_t>(textAddress));
  // Small stack staging only: no full native vector beside the Java allocation.
  std::array<float, chunkSize * stride> batch{};
  for (int start=0; start<count; start+=chunkSize) {
    const int n = std::min(chunkSize, count-start);
    for (int j=0;j<n;++j) {
      float* value = batch.data()+j*stride;
      std::fill(value,value+stride,0.f);
      const int i = start+j;
      const unsigned int code = unicode(text,i);
      const float a = angle(text,i);
      value[0]=static_cast<float>(code); value[2]=a;
      double l=0,r=0,b=0,t=0;
      const int quarter = static_cast<int>(std::round(a/(M_PI/2)));
      if (mappingError(text,i)!=0 || code==0 || code>0x10ffff || !std::isfinite(a)) value[1]=1;
      if (!box(text,i,&l,&r,&b,&t) || !std::isfinite(l+r+b+t) || r<l || t<b) value[1]=1;
      if (std::abs(a-quarter*(M_PI/2))>0.0001) value[1]=2; // Reject arbitrary skew: AABB isn't an exact quad.
      if(code==0 && generated(text,i)==1) value[1]=4;
      double xs[4]={l,r,r,l}, ys[4]={b,b,t,t};
      int q=((quarter%4)+4)%4;
      for(int c=0;c<4;++c) {
        int source=(c+q)%4;
        value[3+c*2]=static_cast<float>(xs[source]);value[4+c*2]=static_cast<float>(ys[source]);
        int x=0,y=0;
        if(!map(page,0,0,scale,scale,0,xs[source],ys[source],&x,&y)) value[1]=1;
        value[11+c*2]=static_cast<float>(x)/scale;value[12+c*2]=static_cast<float>(y)/scale;
      }
    }
    env->SetFloatArrayRegion(output,start*stride,n*stride,batch.data());
    if(env->ExceptionCheck()) return nullptr;
  }
  return output;
}
