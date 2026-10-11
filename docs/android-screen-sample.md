# Android app minimal screen

این فایل برای ساخت یک صفحه‌ی ساده اندروید است که قابلیت‌های گوشی را نشان می‌دهد.

```kotlin
package com.phoneflipper.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        val title = TextView(this)
        title.text = "Phone Flipper"

        val report = TextView(this)
        report.text = "در حال بررسی قابلیت‌های گوشی..."

        val button = Button(this)
        button.text = "به‌روزرسانی گزارش"
        button.setOnClickListener {
            report.text = "NFC: بررسی شد\nIR: بررسی شد\nBLE: بررسی شد\nRF: نیاز به ماژول خارجی دارد"
        }

        layout.addView(title)
        layout.addView(report)
        layout.addView(button)

        setContentView(layout)
    }
}
```

## توضیح

این نمونه اولیه است و قرار است بعداً به نسخه کامل و واقعی تبدیل شود.
