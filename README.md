# بقالة العزي للمواد الغذائية — Native Android

تطبيق Android أصلي Kotlin + Gradle، بواجهة عربية RTL، قاعدة بيانات Room، تبويبات الفاتورة/السجل/العملاء، اقتراح الأصناف، مشاركة الفاتورة، وطباعة Bluetooth ESC/POS.

## البناء

- Java 17
- Gradle 8.2
- Android Gradle Plugin 8.2.2
- compileSdk 34 / minSdk 23

على GitHub يكفي رفع المشروع إلى المستودع ثم تشغيل GitHub Actions. سيتم إنشاء `app-debug.apk` كـ Artifact.

## الطباعة العربية

دعم ESC/POS العربي يختلف حسب لغة طابعة POS وخطها وترميزها. الكود يرسل UTF-8 مباشرة. إذا كانت الطابعة لا تدعم UTF-8/RTL، يلزم تحويل النص إلى صورة Raster قبل الإرسال حسب موديل الطابعة.
