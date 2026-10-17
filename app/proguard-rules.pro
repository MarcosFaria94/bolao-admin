# O código do próprio app fica inteiro (sem renomear), para não quebrar a
# leitura dos dados salvos e do backup. As bibliotecas continuam sendo reduzidas.
-keep class io.github.marcosfaria94.bolao.** { *; }
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-dontnote kotlinx.serialization.**

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
