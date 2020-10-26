# MLKitScanner
MLKitScanner is a android librbary to scan QRCode, Barcode and etc.
Build with help of camerax codes with google MLKit which is using play services for model.

Library for Java and Kotlin both.

## Installation

```bash

allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

```
in build.gradle (Project)


```bash

implementation 'com.github.vidheyMB:MLKitScanner:2490a4b210'

```
in build.gradle (Module)

## Usage 

```xml

<androidx.camera.view.PreviewView
        android:id="@+id/scannerPreView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:visibility="visible" />
        
```

in activity_main.xml


## kotlin 

```kotlin

Scanner.startScanner(this, scannerPreView,this)

```

in MainActivity.kt

```kotlin

override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Scanner.REQUEST_CODE_PERMISSIONS) {
            if (Scanner.allPermissionsGranted(this)) {
                Scanner.startScanner(this, scannerPreView, this);
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show();
            }
        }
    }
    
```
request permission for camera access.


## Complete Kotlin code

```kotlin
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.vmb.scanner.Scanner
import com.vmb.scanner.ScannerListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ScannerListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Scanner.startScanner(this, scannerPreView,this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Scanner.REQUEST_CODE_PERMISSIONS) {
            if (Scanner.allPermissionsGranted(this)) {
                Scanner.startScanner(this, scannerPreView, this);
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    override fun onSuccess(scanCode: String) {
        TODO("Not yet implemented")
    }

    override fun onFailed(response: String) {
        TODO("Not yet implemented")
    }
}
```


## Java

```java

Scanner.INSTANCE.startScanner(this, scannerPreview, this);

```
in MainActivity.java

```java

@Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Scanner.REQUEST_CODE_PERMISSIONS) {
            if (Scanner.INSTANCE.allPermissionsGranted(MainActivity.this)) {
                Scanner.INSTANCE.startScanner(this, scannerPreview, this);
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
    
```

## Complete Java code

```java

public class MainActivity extends AppCompatActivity implements ScannerListener {

public PreviewView scannerPreView;

 @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scannerPreView = findViewById(R.id.scannerPreView);
       
        Scanner.INSTANCE.startScanner(this, scannerPreView, this);
    }

@Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Scanner.REQUEST_CODE_PERMISSIONS) {
            if (Scanner.INSTANCE.allPermissionsGranted(MainActivity.this)) {
                Scanner.INSTANCE.startScanner(this, scannerPreview, this);
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }


 @Override
    public void onSuccess(@NotNull String scanCode) {
        TODO("Not yet implemented")
     }

    @Override
    public void onFailed(@NotNull String response) {
       TODO("Not yet implemented")
    }
 }
  
```
